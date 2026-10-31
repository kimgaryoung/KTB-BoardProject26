#!/usr/bin/env bash
# 기능 : EC2의 서비스별 독립 배포 / 설명 : digest 이미지 pull 후 해당 컨테이너만 교체하고 실패하면 이전 컨테이너를 복구합니다.
set -Eeuo pipefail
component="$1"; image="$2"; region="$3"; registry="$4"; port="$5"
case "$component" in
  backend) container_port=3000; health_path='/v1/posts' ;;
  frontend) container_port=8080; health_path='/html/login.html' ;;
  intro) container_port=8080; health_path='/healthz' ;;
  *) echo 'Invalid component' >&2; exit 2 ;;
esac
[[ "$image" =~ ^[a-z0-9./_-]+@sha256:[a-f0-9]{64}$ ]] || exit 2
[[ "$region" =~ ^[a-z0-9-]+$ && "$registry" =~ ^[a-z0-9.-]+$ && "$port" =~ ^[0-9]+$ ]] || exit 2
deploy_root="${DEPLOY_ROOT:-/opt/board}"
mkdir -p "$deploy_root/locks"
exec 9>"$deploy_root/locks/$component.lock"
flock -w 180 9
name="board-$component"
previous="$name-previous"
env_args=()
if [[ "$component" != intro ]]; then
  [[ -r "$deploy_root/$component.env" ]] || { echo "Missing runtime env file for $component" >&2; exit 1; }
  env_args=(--env-file "$deploy_root/$component.env")
fi
export DOCKER_CONFIG
DOCKER_CONFIG="$(mktemp -d)"
trap 'rm -rf "$DOCKER_CONFIG"' EXIT
aws ecr get-login-password --region "$region" | docker login --username AWS --password-stdin "$registry"
docker pull "$image"

# 기능 : 기존 컨테이너 보존 / 설명 : 이미지 다운로드와 설정 검사를 마친 뒤에만 기존 컨테이너를 멈춥니다.
had_previous=false
if docker inspect "$name" >/dev/null 2>&1; then
  docker rm -f "$previous" >/dev/null 2>&1 || true
  docker stop "$name"
  if ! docker rename "$name" "$previous"; then
    docker start "$name"
    exit 1
  fi
  had_previous=true
fi
recover() {
  docker rm -f "$name" >/dev/null 2>&1 || true
  if [[ "$had_previous" == true ]]; then
    docker rename "$previous" "$name"
    docker start "$name"
  fi
}
trap 'trap - ERR; recover; exit 1' ERR
docker run -d --name "$name" --restart unless-stopped \
  -p "$port:$container_port" "${env_args[@]}" "$image"

# 기능 : HTTP 준비 상태 확인 / 설명 : 컨테이너 실행 여부뿐 아니라 실제 HTTP 응답이 준비된 경우에만 배포 성공으로 판정합니다.
for ((attempt=0; attempt<${HEALTH_ATTEMPTS:-60}; attempt++)); do
  if curl --fail --silent --output /dev/null --max-time 5 "http://127.0.0.1:$port$health_path"; then
    trap - ERR
    echo "Healthy deployment: $component $image"
    exit 0
  fi
  sleep "${HEALTH_DELAY:-2}"
done
echo "Health check failed; restoring $component" >&2
trap - ERR
recover
exit 1
