#!/bin/sh
# Scoped Flutter verification for user_app. Run both checks even if one fails.
# Preserve strict analyzer failure reporting, including existing template lints.
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT_DIR/user_app"

command -v fvm >/dev/null 2>&1 || { echo "VERIFY FAILED: fvm is required" >&2; exit 1; }
[ -d "$APP_DIR" ] || { echo "VERIFY FAILED: $APP_DIR is missing" >&2; exit 1; }

cd "$APP_DIR"

analyze_status=0
test_status=0

echo "==> fvm flutter analyze --no-pub (user_app)"
fvm flutter analyze --no-pub || analyze_status=$?

echo "==> fvm flutter test --no-pub (user_app)"
fvm flutter test --no-pub || test_status=$?

if [ "$analyze_status" -ne 0 ] || [ "$test_status" -ne 0 ]; then
    echo "VERIFY FAILED: analyze=$analyze_status test=$test_status" >&2
    exit 1
fi

echo "VERIFY PASSED"
