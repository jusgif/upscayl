#!/usr/bin/env bash
set -euo pipefail
VERSION="20260526"
URL="https://github.com/Tencent/ncnn/releases/download/${VERSION}/ncnn-${VERSION}-android-vulkan.zip"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="${ROOT}/.ncnn-download"
DEST="${ROOT}/app/src/main/prebuilt"
rm -rf "$TMP" "$DEST"
mkdir -p "$TMP" "$DEST"
curl -L --fail --retry 3 "$URL" -o "$TMP/ncnn.zip"
unzip -q "$TMP/ncnn.zip" -d "$TMP/unpacked"
cp -R "$TMP/unpacked"/* "$DEST/"
rm -rf "$TMP"
echo "ncnn ${VERSION} installed into app/src/main/prebuilt"
