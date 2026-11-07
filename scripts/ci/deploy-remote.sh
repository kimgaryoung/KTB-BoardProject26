#!/usr/bin/env bash
# 기능 : EC2의 서비스별 독립 배포 / 설명 : digest 이미지 pull 후 해당 컨테이너만 교체하고 실패하면 이전 컨테이너를 복구합니다.
set -Eeuo pipefail
prepare_timeout="${PREPARE_TIMEOUT_SECONDS:-300}"
health_timeout="${HEALTH_TIMEOUT_SECONDS:-120}"
[[ "$prepare_timeout" =~ ^[0-9]{1,3}$ && "$health_timeout" =~ ^[0-9]{1,3}$ ]] || exit 2
(( prepare_timeout <= 300 && health_timeout <= 120 )) || exit 2
prepare_deadline=$((SECONDS + prepare_timeout))
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
if [[ -n "${EXPECTED_PREVIOUS_IMAGE:-}" ]]; then
  current_image="$(docker inspect --format '{{.Config.Image}}' "$name")"
  [[ "$current_image" == "$EXPECTED_PREVIOUS_IMAGE" ]] || { echo 'Current image changed; refusing to overwrite a newer release' >&2; exit 1; }
fi
env_args=()
if [[ "$component" != intro ]]; then
  [[ -r "$deploy_root/$component.env" ]] || { echo "Missing runtime env file for $component" >&2; exit 1; }
  env_args=(--env-file "$deploy_root/$component.env")
fi

# Host-managed runtime settings keep small EC2 limits and uploads across releases.
if [[ -r "$deploy_root/runtime.conf" ]]; then
  source "$deploy_root/runtime.conf"
fi
run_args=(--log-driver local --log-opt max-size=10m --log-opt max-file=3)
bind_ip="${DOCKER_BIND_IP:-0.0.0.0}"
[[ "$bind_ip" =~ ^[0-9.]+$ ]] || { echo 'Invalid bind address' >&2; exit 2; }
health_host="$bind_ip"
if [[ "$health_host" == 0.0.0.0 ]]; then health_host=127.0.0.1; fi
if [[ -n "${DOCKER_NETWORK:-}" ]]; then
  [[ "$DOCKER_NETWORK" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || exit 2
  docker network inspect "$DOCKER_NETWORK" >/dev/null
  run_args+=(--network "$DOCKER_NETWORK")
fi
case "$component" in
  backend) memory="${BACKEND_MEMORY:-}" ;;
  frontend) memory="${FRONTEND_MEMORY:-}" ;;
  intro) memory="${INTRO_MEMORY:-}" ;;
esac
if [[ -n "$memory" ]]; then
  [[ "$memory" =~ ^[1-9][0-9]*[mMgG]$ ]] || { echo 'Invalid container memory limit' >&2; exit 2; }
  run_args+=(--memory "$memory")
fi
if [[ "$component" == backend && -n "${BACKEND_UPLOAD_DIRECTORY:-}" ]]; then
  [[ "$BACKEND_UPLOAD_DIRECTORY" == /* && "$BACKEND_UPLOAD_DIRECTORY" != *,* && -d "$BACKEND_UPLOAD_DIRECTORY" ]] || exit 2
  run_args+=(--mount "type=bind,src=$BACKEND_UPLOAD_DIRECTORY,dst=/app/uploads")
fi
export DOCKER_CONFIG
DOCKER_CONFIG="$(mktemp -d)"
trap 'rm -rf "$DOCKER_CONFIG"' EXIT
aws ecr get-login-password --region "$region" | docker login --username AWS --password-stdin "$registry"
docker pull "$image"

# 기능 : 기존 컨테이너 보존 / 설명 : 이미지 다운로드와 설정 검사를 마친 뒤에만 기존 컨테이너를 멈춥니다.
had_previous=false
stopped_current=false
candidate_started=false
recover() {
  if [[ "$had_previous" == true ]] || { [[ "$stopped_current" == true ]] && timeout -k 5s 5s docker inspect "$previous" >/dev/null 2>&1; }; then
    timeout -k 5s 10s docker rm -f "$name" >/dev/null 2>&1 || true
    timeout -k 5s 10s docker rename "$previous" "$name"
    timeout -k 5s 10s docker start "$name"
  elif [[ "$stopped_current" == true ]]; then
    timeout -k 5s 10s docker start "$name"
  elif [[ "$candidate_started" == true ]]; then
    timeout -k 5s 10s docker rm -f "$name" >/dev/null 2>&1 || true
  fi
}
trap 'trap - ERR TERM INT; recover; exit 1' ERR TERM INT
if docker inspect "$name" >/dev/null 2>&1; then
  if docker inspect "$previous" >/dev/null 2>&1; then
    timeout -k 5s 10s docker rm -f "$previous" >/dev/null
  fi
  (( SECONDS < prepare_deadline )) || { echo 'Preparation deadline exceeded; keeping current container' >&2; exit 1; }
  stopped_current=true
  timeout -k 5s 20s docker stop "$name"
  timeout -k 5s 10s docker rename "$name" "$previous"
  had_previous=true
fi
if (( SECONDS >= prepare_deadline )); then
  trap - ERR TERM INT
  recover
  exit 1
fi
candidate_started=true
timeout -k 5s 20s docker run -d --name "$name" --restart unless-stopped \
  -p "$bind_ip:$port:$container_port" "${run_args[@]}" "${env_args[@]}" "$image"

# 기능 : HTTP 준비 상태 확인 / 설명 : 컨테이너 실행 여부뿐 아니라 실제 HTTP 응답이 준비된 경우에만 배포 성공으로 판정합니다.
health_deadline=$((SECONDS + health_timeout))
for ((attempt=0; attempt<${HEALTH_ATTEMPTS:-60} && SECONDS<health_deadline; attempt++)); do
  if curl --fail --silent --output /dev/null --max-time 5 "http://$health_host:$port$health_path"; then
    trap - ERR TERM INT
    echo "Healthy deployment: $component $image"
    exit 0
  fi
  sleep "${HEALTH_DELAY:-2}"
done
echo "Health check failed; restoring $component" >&2
trap - ERR TERM INT
recover
exit 1
