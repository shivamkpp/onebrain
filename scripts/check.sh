#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
(cd "$repo_root/backend" && ./mvnw -B verify)
(cd "$repo_root/frontend" && npm test && npm run build)
