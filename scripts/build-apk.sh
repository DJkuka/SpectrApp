#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"
if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: Gradle 8.13 is not installed or is not on PATH." >&2
  echo "Open this folder in Android Studio and run :android:assembleDebug." >&2
  exit 2
fi
./scripts/test-core.sh
gradle :android:assembleDebug
printf '\nAPK output is under android/build/outputs/apk/debug/\n'
