"""기능 : ECR 재사용·롤백 계획 / 설명 : 명시 이미지가 없으면 실패하며 태그를 배포 전에 불변 digest로 확정합니다."""
import argparse
import json
import os
import re
import subprocess
from pathlib import Path
from context import prepare

DIGEST = re.compile(r'sha256:[0-9a-f]{64}')
TAG = re.compile(r'[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}')


def plan(variant, source_hash, run_id, selector, force, lookup):
    if selector:
        if force or not (DIGEST.fullmatch(selector) or TAG.fullmatch(selector)) or selector.startswith('sha256:') and not DIGEST.fullmatch(selector):
            raise ValueError('Use a valid ECR tag/digest; explicit reuse cannot be forced to rebuild')
        digest = lookup(selector)
        if not digest:
            raise ValueError('Requested rollback/reuse image does not exist; refusing to rebuild')
        return {'tag': selector, 'digest': digest, 'build': False, 'reason': 'explicit-reuse'}
    tag = f'{variant}-src-{source_hash}'
    if force or variant == 'single':
        return {'tag': f'{tag}-{run_id}', 'digest': '', 'build': True, 'reason': 'fresh-build'}
    digest = lookup(tag)
    return {'tag': tag, 'digest': digest or '', 'build': not bool(digest), 'reason': 'content-reuse' if digest else 'new-content'}


def lookup(repository, selector):
    key = 'imageDigest' if DIGEST.fullmatch(selector) else 'imageTag'
    result = subprocess.run(['aws', 'ecr', 'describe-images', '--repository-name', repository,
                             '--image-ids', f'{key}={selector}', '--output', 'json'], capture_output=True, text=True)
    if result.returncode:
        if 'ImageNotFoundException' in result.stderr:
            return None
        raise RuntimeError('ECR query failed (check region, repository and IAM permissions): ' + result.stderr)
    digest = json.loads(result.stdout)['imageDetails'][0]['imageDigest']
    if not DIGEST.fullmatch(digest):
        raise ValueError('Invalid registry digest')
    return digest


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--component', required=True)
    p.add_argument('--variant', choices=['single', 'multi'], required=True)
    p.add_argument('--selector', default='')
    p.add_argument('--force', choices=['true', 'false'], default='false')
    p.add_argument('--repository', required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    if not re.fullmatch(r'[a-z0-9][a-z0-9._/-]*', args.repository):
        raise ValueError('Invalid ECR repository')
    source_hash = prepare(Path.cwd(), args.component, args.variant, args.output)
    result = plan(args.variant, source_hash, f'{os.environ["GITHUB_RUN_ID"]}-{os.environ["GITHUB_RUN_ATTEMPT"]}',
                  args.selector, args.force == 'true', lambda tag: lookup(args.repository, tag))
    checkout_sha = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    result.update(component=args.component, context_hash=source_hash, repository=args.repository,
                  checkout_sha=checkout_sha, source_sha=checkout_sha if result['build'] else None)
    Path('release.json').write_text(json.dumps(result, indent=2))
    with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
        out.write(f'tag={result["tag"]}\nbuild={str(result["build"]).lower()}\n')
