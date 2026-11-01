#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
./scripts/dev.sh &
dev_pid=$!
cleanup() {
  trap - EXIT INT TERM
  kill "$dev_pid" 2>/dev/null || true
  wait "$dev_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
ready=0
for ((attempt=1; attempt<=90; attempt++)); do
  if ! kill -0 "$dev_pid" 2>/dev/null; then echo 'Development services failed to start.' >&2; exit 1; fi
  backend_status="$(curl --silent --max-time 1 -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/v1/session || true)"
  if [ "$backend_status" = 401 ] && curl --fail --silent --max-time 1 http://127.0.0.1:5173/ -o /dev/null; then
    ready=1
    break
  fi
  sleep 1
done
if [ "$ready" != 1 ]; then echo 'Timed out starting development services.' >&2; exit 1; fi
(cd frontend && npm run test:e2e)
