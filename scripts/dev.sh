#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
for port in 8080 5173; do
  if curl --silent --max-time 2 "http://127.0.0.1:$port/" -o /dev/null; then
    echo "Port $port already has a server. Stop it before starting OneBrain." >&2
    exit 1
  fi
done
docker compose up -d --wait
(cd backend && ./mvnw -B -DskipTests package)
mkdir -p .local
run_dir="$(mktemp -d "$repo_root/.local/run.XXXXXX")"
backend_pid=''
frontend_pid=''
cleanup() {
  trap - EXIT INT TERM
  for pid in "$backend_pid" "$frontend_pid"; do
    if [ -n "$pid" ]; then kill "$pid" 2>/dev/null || true; fi
  done
  for pid in "$backend_pid" "$frontend_pid"; do
    if [ -n "$pid" ]; then wait "$pid" 2>/dev/null || true; fi
  done
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
java -jar backend/target/onebrain-0.1.0-SNAPSHOT.jar --spring.profiles.active=local >"$run_dir/backend.log" 2>&1 &
backend_pid=$!
(cd frontend && exec node node_modules/vite/bin/vite.js --host 127.0.0.1) >"$run_dir/frontend.log" 2>&1 &
frontend_pid=$!
ready=0
for ((attempt=1; attempt<=60; attempt++)); do
  if ! kill -0 "$backend_pid" 2>/dev/null || ! kill -0 "$frontend_pid" 2>/dev/null; then
    echo "A service stopped during startup. Logs: $run_dir" >&2
    exit 1
  fi
  backend_status="$(curl --silent --max-time 1 -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/v1/session || true)"
  if [ "$backend_status" = 401 ] && curl --fail --silent --max-time 1 http://127.0.0.1:5173/ -o /dev/null; then
    ready=1
    break
  fi
  sleep 1
done
if [ "$ready" != 1 ]; then echo "Services did not become ready. Logs: $run_dir" >&2; exit 1; fi
echo "OneBrain local services are running. Logs: $run_dir"
echo 'Use the frontend on port 5173. Ctrl-C stops the app processes; PostgreSQL data is retained.'
wait -n "$backend_pid" "$frontend_pid"
