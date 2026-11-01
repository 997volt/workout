#!/usr/bin/env bash
#
# The combined gate, with the one workaround the lint flake has earned (ROADMAP N253).
#
# `lintAnalyzeDebug*` sometimes fails with no findings while a booted emulator competes with the Gradle and
# Kotlin daemons for this sandbox's memory. The gate stops attached **emulators** first — never a physical
# phone, whose data is not ours to drop — and then runs exactly the task set CI runs. Pass --keep-emulator on
# a machine with room.
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

if [ -f "$ROOT/tools/android-env.sh" ]; then
    # shellcheck disable=SC1091
    . "$ROOT/tools/android-env.sh"
fi

keep=0
for arg in "$@"; do
    case "$arg" in
        --keep-emulator) keep=1 ;;
        *)
            echo "unknown argument: $arg" >&2
            echo "usage: tools/gate.sh [--keep-emulator]" >&2
            exit 2
            ;;
    esac
done

if [ "$keep" -eq 0 ] && command -v adb >/dev/null 2>&1; then
    # `adb emu kill` is only meaningful to an emulator; a USB serial is left attached and untouched.
    emulators=$(adb devices | awk '$2 == "device" && $1 ~ /^emulator-/ {print $1}')
    for serial in $emulators; do
        echo "stopping $serial: the combined gate flakes under its memory pressure (N253)"
        adb -s "$serial" emu kill >/dev/null 2>&1 || true
    done
    for _ in $(seq 1 10); do
        [ -z "$(adb devices | awk '$2 == "device" && $1 ~ /^emulator-/ {print $1}')" ] && break
        sleep 1
    done
fi

./gradlew testDebugUnitTest :shared:jvmTest :shared:jsNodeTest lint detekt :shared:detekt assembleDebug assembleRelease --console=plain
