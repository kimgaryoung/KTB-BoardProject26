"""기능 : 서비스별 결정적 빌드 컨텍스트 / 설명 : 선택한 서비스만 패키징하고 실제 입력 바이트로 이미지 키를 계산합니다."""
import argparse
import hashlib
import shutil
from pathlib import Path

INTRO_FILES = [
    'html/index.html', 'js/index.js', 'css/home.css',
    'css/common/layout.css', 'component/dialog/dialog.css',
    'component/header/header.css', 'component/header/header.js',
    'utils/function.js', 'utils/request.js', 'utils/authNavigation.js', 'utils/pageHeader.js',
]
NOISE = {'.git', '.gradle', 'node_modules', 'build', '.DS_Store', '__pycache__'}


def targets(value):
    choices = {'all': ['backend', 'frontend', 'intro'], 'frontend-backend': ['backend', 'frontend'],
               'backend': ['backend'], 'frontend': ['frontend'], 'intro': ['intro']}
    if value not in choices:
        raise ValueError('Unknown deployment target')
    return choices[value]


def excluded(path):
    return (path.name in NOISE or path.name.startswith('.env') or path.suffix == '.log'
            or path.name.startswith('application-local.'))


def copy_tree(source, dest):
    if excluded(source):
        return
    if source.is_symlink():
        raise ValueError(f'Symlinks are not accepted in a build context: {source}')
    if source.is_dir():
        for child in sorted(source.iterdir()):
            copy_tree(child, dest / child.name)
    else:
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, dest)


def content_hash(root):
    digest = hashlib.sha256()
    for path in sorted(p for p in root.rglob('*') if p.is_file()):
        digest.update(path.relative_to(root).as_posix().encode() + b'\0')
        digest.update(hashlib.sha256(path.read_bytes()).digest())
    return digest.hexdigest()


def prepare(root, component, variant, out):
    if component not in targets('all') or variant not in ('single', 'multi'):
        raise ValueError('Invalid component or Dockerfile variant')
    if out.exists():
        raise ValueError('Context output must be a new directory')
    out.mkdir(parents=True)
    copy_tree(root / f'deploy/docker/{component}.{variant}.Dockerfile', out / 'Dockerfile')
    if component == 'backend':
        for name in ['gradlew', 'gradle', 'build.gradle', 'settings.gradle', 'src']:
            copy_tree(root / name, out / 'src' / name)
    elif component == 'frontend':
        for name in ['package.json', 'package-lock.json', 'app.js', 'api', 'component', 'css', 'html', 'js', 'utils', 'public', 'test']:
            copy_tree(root / 'frontend' / name, out / 'src' / name)
        for name in ['html/index.html', 'js/index.js', 'css/home.css']:
            (out / 'src' / name).unlink()
    else:
        for name in INTRO_FILES + ['public']:
            copy_tree(root / 'frontend' / name, out / 'src' / name)
        copy_tree(root / 'scripts/ci/build_intro.py', out / 'build_intro.py')
        copy_tree(root / 'deploy/nginx/intro.conf', out / 'intro.conf')
    return content_hash(out)


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('component', choices=targets('all'))
    p.add_argument('variant', choices=['single', 'multi'])
    p.add_argument('output', type=Path)
    args = p.parse_args()
    print(prepare(Path(__file__).resolve().parents[2], args.component, args.variant, args.output))
