#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
for tool in java node npm docker curl; do
  command -v "$tool" >/dev/null || { echo "Missing prerequisite: $tool" >&2; exit 1; }
done
command -v javac >/dev/null || { echo 'Install a JDK (Java 21), not just a JRE.' >&2; exit 1; }
docker info >/dev/null
docker compose version >/dev/null
(cd "$repo_root/backend" && ./mvnw -B -DskipTests package)
(cd "$repo_root/frontend" && npm ci)
echo 'Dependencies installed. Run ./scripts/check.sh, then ./scripts/dev.sh.'
