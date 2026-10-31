"""기능 : EC2 교체 실패 복구 검증 / 설명 : Docker와 curl을 가짜 실행 파일로 바꿔 실제 서버 없이 검사합니다."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('deploy-remote.sh')


class DeployTests(unittest.TestCase):
    def simulate(self, health_ok=True, pull_ok=True, start_ok=True, component='frontend'):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'frontend.env').write_text('BACKEND_BASE_URL=http://backend:3000\n')
            (root / 'backend.env').write_text('DB_URL=jdbc:mysql://test/board\n')
            for command in ['docker', 'aws', 'curl', 'flock', 'sleep']:
                p = root / command
                p.write_text('''#!/usr/bin/env python3
import json,os,sys
name=os.path.basename(sys.argv[0]); args=sys.argv[1:]
with open(os.environ['CALLS'],'a') as f: f.write(json.dumps([name]+args)+'\\n')
if name=='aws': print('fake-password')
if name=='docker' and args[0]=='inspect': sys.exit(0)
if name=='docker' and args[0]=='pull' and os.environ['PULL_OK']=='0': sys.exit(1)
if name=='docker' and args[0]=='start' and os.environ['START_OK']=='0': sys.exit(1)
if name=='curl' and os.environ['HEALTH_OK']=='0': sys.exit(1)
''')
                p.chmod(0o755)
            env = dict(os.environ, PATH=f'{root}:{os.environ["PATH"]}', CALLS=str(root / 'calls'),
                       HEALTH_OK=str(int(health_ok)), PULL_OK=str(int(pull_ok)),
                       START_OK=str(int(start_ok)),
                       DEPLOY_ROOT=str(root), HEALTH_ATTEMPTS='1', HEALTH_DELAY='0')
            result = subprocess.run(['bash', str(SCRIPT), component, '123.dkr.ecr.ap-northeast-2.amazonaws.com/repo@sha256:' + 'a' * 64,
                                     'ap-northeast-2', '123.dkr.ecr.ap-northeast-2.amazonaws.com', '3000' if component == 'backend' else '8080'], env=env, capture_output=True, text=True)
            calls = [json.loads(line) for line in (root / 'calls').read_text().splitlines()] if (root / 'calls').exists() else []
            return result, calls

    def test_healthy_candidate_replaces_only_selected_service(self):
        result, calls = self.simulate()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(['docker', 'rename', 'board-frontend', 'board-frontend-previous'], calls)
        self.assertFalse(any('prune' in c or 'board-backend' in c for c in calls))

    def test_failed_health_restores_existing_container(self):
        result, calls = self.simulate(health_ok=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(['docker', 'rename', 'board-frontend-previous', 'board-frontend'], calls)
        self.assertIn(['docker', 'start', 'board-frontend'], calls)

    def test_pull_failure_keeps_running_container(self):
        result, calls = self.simulate(pull_ok=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(c[:2] == ['docker', 'stop'] for c in calls))

    def test_recovery_failure_does_not_delete_restored_container(self):
        result, calls = self.simulate(health_ok=False, start_ok=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls.count(['docker', 'rm', '-f', 'board-frontend']), 1)

    def test_backend_readiness_uses_the_canonical_site_board(self):
        result, calls = self.simulate(component='backend')
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertTrue(any(c[0]=='curl' and c[-1]=='http://127.0.0.1:3000/v1/posts' for c in calls))


if __name__ == '__main__':
    unittest.main()
