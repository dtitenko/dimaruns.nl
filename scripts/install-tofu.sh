#!/usr/bin/env bash
# Installs the OpenTofu version pinned in infra/.opentofu-version into .tools/ (git-ignored),
# verifying the release checksum. Usage: scripts/install-tofu.sh   then   .tools/tofu -version
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
version="$(tr -d '[:space:]' < "$root/infra/.opentofu-version")"
dest="$root/.tools"

case "$(uname -s)" in
  Linux*) os=linux ;;
  Darwin*) os=darwin ;;
  MINGW* | MSYS* | CYGWIN*) os=windows ;;
  *) echo "unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac
case "$(uname -m)" in
  x86_64 | amd64) arch=amd64 ;;
  arm64 | aarch64) arch=arm64 ;;
  *) echo "unsupported arch: $(uname -m)" >&2; exit 1 ;;
esac

exe=tofu; [ "$os" = windows ] && exe=tofu.exe
if [ -x "$dest/$exe" ] && "$dest/$exe" -version | grep -q "v$version"; then
  echo "OpenTofu $version already installed in .tools/"; exit 0
fi

zip="tofu_${version}_${os}_${arch}.zip"
base="https://github.com/opentofu/opentofu/releases/download/v${version}"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT

curl -fsSL -o "$tmp/$zip" "$base/$zip"
curl -fsSL -o "$tmp/SHA256SUMS" "$base/tofu_${version}_SHA256SUMS"
(cd "$tmp" && grep " $zip\$" SHA256SUMS | sha256sum -c -)

mkdir -p "$dest"
unzip -oq "$tmp/$zip" "$exe" -d "$dest"
echo "Installed OpenTofu $version to .tools/$exe"
