"""기능 : 배포용 이미지 기록 / 설명 : 빌드·재사용 결과 모두 digest 주소와 실제 작업 시간을 같은 아티팩트로 남깁니다."""
import json
import os
import time
import subprocess
from pathlib import Path
from image_plan import DIGEST

p = Path('release.json')
data = json.loads(p.read_text())
digest = os.environ.get('BUILT_DIGEST') if data['build'] else data['digest']
if not digest or not DIGEST.fullmatch(digest):
    raise ValueError('A verified image digest is required for deployment')
details = json.loads(subprocess.check_output(['aws', 'ecr', 'describe-images', '--repository-name', data['repository'],
                                             '--image-ids', f'imageDigest={digest}', '--output', 'json'], text=True))
data.update(digest=digest, image_ref=f'{os.environ["ECR_REGISTRY"]}/{data["repository"]}@{digest}',
            variant=os.environ['VARIANT'], environment=os.environ['DEPLOY_ENVIRONMENT'],
            targets=os.environ['TARGETS'], cache=os.environ['CACHE_MODE'], experiment=os.environ['EXPERIMENT'],
            deploy_requested=os.environ['DEPLOY_REQUESTED'] == 'true',
            image_size_bytes=details['imageDetails'][0]['imageSizeInBytes'],
            started_at=int(os.environ['STARTED_AT']), finished_at=int(time.time()))
data['duration_seconds'] = data['finished_at'] - data['started_at']
p.write_text(json.dumps(data, indent=2))
with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as out:
    out.write(f'### {data["component"]}: {data["reason"]}\n\n`{data["image_ref"]}`\n\nContext: `{data["context_hash"]}`\n')
