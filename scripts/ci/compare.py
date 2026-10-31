"""기능 : 여러 완료 실행 비교 / 설명 : 같은 실험 조건과 source SHA의 성공 실행만 그룹화해 실제 중앙값을 출력합니다."""
import argparse
import json
from pathlib import Path
import statistics

p = argparse.ArgumentParser()
p.add_argument('metrics', nargs='+', type=Path)
args = p.parse_args()
groups = {}
for path in args.metrics:
    data = json.loads(path.read_text())
    if data['conclusion'] != 'success' or not data.get('releases') or data.get('attempt', 1) != 1:
        print(f'Skipped incomplete/non-success run: {path}')
        continue
    releases = data['releases']
    key = (data['workflow'], tuple(sorted({r['checkout_sha'] for r in releases})),
           tuple(sorted(r['component'] for r in releases)), releases[0]['environment'],
           releases[0]['cache'], releases[0]['deploy_requested'],
           tuple(sorted((r['component'], r['reason'], r['digest'] if not r['build'] else '') for r in releases)),
           releases[0]['experiment'])
    groups.setdefault(key, []).append(data['wall_seconds'])
for key, durations in groups.items():
    print(json.dumps({'conditions':key,'runs':len(durations),'median_wall_seconds':statistics.median(durations),
                      'min_seconds':min(durations),'max_seconds':max(durations)},ensure_ascii=False))
