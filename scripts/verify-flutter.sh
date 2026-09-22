#!/bin/sh
# Scoped Flutter verification for user_app. Nonzero exit on analyze/test failure.
# Does not fix unrelated pre-existing template debt; records failures rather than papering over them.
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT_DIR/user_app"

command -v fvm >/dev/null 2>&1 || { echo "VERIFY FAILED: fvm is required" >&2; exit 1; }
[ -d "$APP_DIR" ] || { echo "VERIFY FAILED: $APP_DIR is missing" >&2; exit 1; }

cd "$APP_DIR"

echo "==> fvm flutter analyze --no-pub (user_app)"
fvm flutter analyze --no-pub

echo "==> fvm flutter test --no-pub (user_app)"
fvm flutter test --no-pub

echo "VERIFY PASSED"
