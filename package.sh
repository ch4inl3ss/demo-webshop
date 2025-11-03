#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
ZIP_NAME="webshop-docker.zip"
cd "$ROOT_DIR"
rm -f "$ZIP_NAME"
zip -r "$ZIP_NAME" . -x '.git/*' -x 'target/*' -x "$ZIP_NAME"
echo "Archiv erstellt: $ZIP_NAME"
