#!/usr/bin/env bash
# Runs the pinned OpenTofu against infra/ with credentials from .env (if present).
# Usage: scripts/tofu.sh plan | apply | output | ...
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
"$root/scripts/install-tofu.sh" > /dev/null

if [ -f "$root/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$root/.env"
  set +a
fi

tofu="$root/.tools/tofu"; [ -x "$tofu.exe" ] && tofu="$tofu.exe"
exec "$tofu" -chdir="$root/infra" "$@"
