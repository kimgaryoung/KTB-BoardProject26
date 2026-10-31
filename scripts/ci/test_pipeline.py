"""기능 : 배포 없는 파이프라인 회귀 검증 / 설명 : 빌드 범위·재사용·롤백 계약을 임시 파일과 가짜 레지스트리로 검사합니다."""
import tempfile
import unittest
import subprocess
import sys
import json
from pathlib import Path
import context
import image_plan
import build_intro
import shutil
import re


class ImagePlanTests(unittest.TestCase):
    digest = 'sha256:' + 'a' * 64

    def test_existing_multi_image_is_reused(self):
        p = image_plan.plan('multi', 'b' * 64, '12-1', '', False, lambda tag: self.digest)
        self.assertFalse(p['build'])
        self.assertEqual(p['digest'], self.digest)

    def test_new_multi_image_builds(self):
        self.assertTrue(image_plan.plan('multi', 'b' * 64, '12-1', '', False, lambda tag: None)['build'])

    def test_single_always_builds_without_explicit_override(self):
        self.assertTrue(image_plan.plan('single', 'b' * 64, '12-1', '', False, lambda tag: self.digest)['build'])

    def test_forced_build_has_unique_tag_for_immutable_ecr(self):
        p = image_plan.plan('multi', 'b' * 64, '12-2', '', True, lambda tag: self.digest)
        self.assertTrue(p['build'])
        self.assertTrue(p['tag'].endswith('-12-2'))

    def test_rollback_never_builds(self):
        p = image_plan.plan('multi', 'b' * 64, '12-1', 'release-previous', False, lambda tag: self.digest)
        self.assertFalse(p['build'])
        self.assertEqual(p['tag'], 'release-previous')

    def test_missing_rollback_fails_instead_of_rebuilding(self):
        with self.assertRaises(ValueError):
            image_plan.plan('multi', 'b' * 64, '12-1', 'missing-release', False, lambda tag: None)

    def test_force_and_rollback_are_mutually_exclusive(self):
        with self.assertRaises(ValueError):
            image_plan.plan('multi', 'b' * 64, '12-1', 'previous', True, lambda tag: self.digest)

    def test_untrusted_image_selector_is_rejected(self):
        for selector in ['$(touch /tmp/no)', '../latest', 'x\ny', 'https://other/image', 'sha256:bad']:
            with self.subTest(selector=selector), self.assertRaises(ValueError):
                image_plan.plan('multi', 'b' * 64, '12-1', selector, False, lambda tag: self.digest)

    def test_registry_error_does_not_become_a_cache_miss(self):
        def unavailable(tag):
            raise RuntimeError('registry unavailable')
        with self.assertRaises(RuntimeError):
            image_plan.plan('multi', 'b' * 64, '12-1', '', False, unavailable)


class ContextTests(unittest.TestCase):
    def test_select_targets_without_paths(self):
        self.assertEqual(context.targets('frontend-backend'), ['backend', 'frontend'])
        self.assertEqual(context.targets('intro'), ['intro'])
        with self.assertRaises(ValueError):
            context.targets('arbitrary-command')

    def test_hash_ignores_timestamps_but_detects_contents(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / 'file'
            p.write_text('first')
            old = context.content_hash(Path(tmp))
            p.touch()
            self.assertEqual(old, context.content_hash(Path(tmp)))
            p.write_text('second')
            self.assertNotEqual(old, context.content_hash(Path(tmp)))

    def test_context_excludes_local_secrets_and_noise(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'source').mkdir()
            for name in ['app.js', '.env', 'application-local.yml', '.DS_Store']:
                (root / 'source' / name).write_text(name)
            context.copy_tree(root / 'source', root / 'out')
            self.assertEqual([p.name for p in (root / 'out').iterdir()], ['app.js'])

    def test_symlink_cannot_escape_context(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'source').mkdir()
            (root / 'secret').write_text('secret')
            (root / 'source' / 'link').symlink_to(root / 'secret')
            with self.assertRaises(ValueError):
                context.copy_tree(root / 'source', root / 'out')

    def test_real_contexts_separate_intro_and_service(self):
        root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            context.prepare(root, 'frontend', 'multi', out / 'frontend')
            context.prepare(root, 'intro', 'multi', out / 'intro')
            self.assertFalse((out / 'frontend' / 'src/html/index.html').exists())
            self.assertFalse((out / 'frontend' / 'src/js/index.js').exists())
            self.assertTrue((out / 'frontend' / 'src/html/login.html').exists())
            self.assertTrue((out / 'intro' / 'src/html/index.html').exists())
            self.assertFalse((out / 'intro' / 'src/js/board-write.js').exists())
            self.assertFalse((out / 'intro' / 'src/test').exists())

    def test_intro_and_service_changes_have_independent_image_keys(self):
        project = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / 'repo'
            shutil.copytree(project / 'frontend', root / 'frontend', ignore=shutil.ignore_patterns('node_modules', '.git', '.env*'))
            shutil.copytree(project / 'deploy', root / 'deploy')
            (root / 'scripts/ci').mkdir(parents=True)
            shutil.copyfile(project / 'scripts/ci/build_intro.py', root / 'scripts/ci/build_intro.py')
            frontend = context.prepare(root,'frontend','multi',Path(tmp)/'front1')
            intro = context.prepare(root,'intro','multi',Path(tmp)/'intro1')
            with (root/'frontend/html/index.html').open('a') as file:
                file.write('<!-- changed intro only -->')
            self.assertEqual(frontend,context.prepare(root,'frontend','multi',Path(tmp)/'front2'))
            intro2 = context.prepare(root,'intro','multi',Path(tmp)/'intro2')
            self.assertNotEqual(intro,intro2)
            with (root/'frontend/js/board.js').open('a') as file:
                file.write('\n// changed service only\n')
            self.assertNotEqual(frontend,context.prepare(root,'frontend','multi',Path(tmp)/'front3'))
            self.assertEqual(intro2,context.prepare(root,'intro','multi',Path(tmp)/'intro3'))

    def test_packaged_intro_has_complete_relative_module_and_style_dependencies(self):
        project = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            context.prepare(project,'intro','multi',root/'context')
            build_intro.build(root/'context/src',root/'site')
            html = (root/'site/index.html').read_text()
            self.assertIn('/intro/js/index.js',html)
            self.assertIn('/intro/css/home.css',html)
            for file in (root/'site').rglob('*'):
                if file.suffix == '.js':
                    for relative in re.findall(r"from\s+['\"](\.[^'\"]+)['\"]",file.read_text()):
                        self.assertTrue((file.parent/relative).is_file(),f'{file}: {relative}')
                if file.suffix == '.css':
                    for relative in re.findall(r"url\(['\"]?([^'\")]+)",file.read_text()):
                        target = root/'site'/relative.lstrip('/') if relative.startswith('/') else file.parent/relative
                        self.assertTrue(target.is_file(),f'{file}: {relative}')


class ComparisonTests(unittest.TestCase):
    def test_different_reused_digests_are_never_merged(self):
        with tempfile.TemporaryDirectory() as tmp:
            files = []
            for i, seconds in enumerate([10, 100]):
                file = Path(tmp) / f'{i}.json'
                file.write_text(json.dumps({'conclusion':'success', 'workflow':'optimized', 'attempt':1,
                    'wall_seconds':seconds, 'releases':[{'component':'backend', 'source_sha':'same-checkout',
                    'checkout_sha':'same-checkout', 'digest':'sha256:'+str(i)*64, 'build':False,
                    'environment':'staging', 'cache':'cold', 'deploy_requested':True,
                    'reason':'explicit-reuse', 'experiment':'rollback'}]}))
                files.append(str(file))
            result = subprocess.check_output([sys.executable,str(Path(__file__).with_name('compare.py')),*files],text=True)
            self.assertEqual(len(result.strip().splitlines()), 2)


if __name__ == '__main__':
    unittest.main()
