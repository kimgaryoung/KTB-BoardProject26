#!/usr/bin/env bash
# 기능 : 검증된 호스트로 SSH 배포 / 설명 : 비밀키는 임시 파일에만 저장하고 미리 등록한 호스트 키로 서버를 검증합니다.
set -Eeuo pipefail
: "${DEPLOY_HOST:?Set component EC2 host}"
: "${DEPLOY_USER:?Set EC2 user}"
: "${EC2_SSH_KEY:?Missing EC2_SSH_KEY}"
: "${EC2_KNOWN_HOSTS:?Missing pinned EC2_KNOWN_HOSTS}"
[[ "$DEPLOY_HOST" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*$ && "$DEPLOY_USER" =~ ^[a-zA-Z0-9_-]+$ ]] || exit 2
ssh_dir="$(mktemp -d)"
trap 'rm -rf "$ssh_dir"' EXIT
chmod 700 "$ssh_dir"
printf '%s\n' "$EC2_SSH_KEY" > "$ssh_dir/key"
printf '%s\n' "$EC2_KNOWN_HOSTS" > "$ssh_dir/known_hosts"
chmod 600 "$ssh_dir/key" "$ssh_dir/known_hosts"
image="$(python3 -c 'import json; print(json.load(open("release/release.json"))["image_ref"])')"
printf -v command 'bash -s -- %q %q %q %q %q' "$COMPONENT" "$image" "$AWS_REGION" "$ECR_REGISTRY" "$DEPLOY_PORT"
ssh -i "$ssh_dir/key" -o BatchMode=yes -o ConnectTimeout=15 \
  -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$ssh_dir/known_hosts" \
  "$DEPLOY_USER@$DEPLOY_HOST" "$command" < scripts/ci/deploy-remote.sh
