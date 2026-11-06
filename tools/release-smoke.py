#!/usr/bin/env python3
"""Launch the signed R8 release and walk its two primary tabs (N270).

A release APK can pass every unit test, compile and be signed, then crash at startup because R8 removed a
Hilt or serialization member. This script installs nothing — CI has already installed the APK — and only drives
the UI through the identifiers the app exposes: the home list and the tab bar. It fails on a crash or a missing
screen, so the release build is executed rather than merely assembled.

Usage: python3 tools/release-smoke.py [--package io.github.volt997.workout]
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def adb(*args: str) -> str:
    return subprocess.run(
        ["adb", *args],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    ).stdout


def dump() -> ET.Element:
    # uiautomator writes on the device; pull it rather than trying to parse stdout, which also carries
    # "UI hierchary dumped to" on some builds.
    adb("shell", "uiautomator", "dump", "/sdcard/release-smoke.xml")
    with tempfile.NamedTemporaryFile(suffix=".xml", delete=False) as tmp:
        path = Path(tmp.name)
    try:
        adb("pull", "/sdcard/release-smoke.xml", str(path))
        return ET.parse(path).getroot()
    finally:
        path.unlink(missing_ok=True)


def find(root: ET.Element, tag: str) -> ET.Element | None:
    for node in root.iter("node"):
        resource = node.get("resource-id", "")
        if resource == tag or resource.endswith(f":id/{tag}"):
            return node
    return None


def wait_for(tag: str, seconds: float = 15.0) -> ET.Element:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        root = dump()
        node = find(root, tag)
        if node is not None:
            return node
        time.sleep(0.5)
    raise SystemExit(f"release smoke: {tag} did not appear")


def tap(tag: str) -> None:
    node = wait_for(tag)
    match = BOUNDS.match(node.get("bounds", ""))
    if match is None:
        raise SystemExit(f"release smoke: {tag} has no bounds")
    x1, y1, x2, y2 = (int(part) for part in match.groups())
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--package", default="io.github.volt997.workout")
    parser.add_argument("--activity", default="com.example.androidapp.MainActivity")
    args = parser.parse_args()

    adb("wait-for-device")
    adb("shell", "am", "force-stop", args.package)
    # The application id and the Kotlin namespace differ on purpose: MainActivity is declared under
    # `com.example.androidapp`, so `.MainActivity` relative to the package would not resolve.
    started = subprocess.run(
        ["adb", "shell", "am", "start", "-W", "-n", f"{args.activity}"],
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if started.returncode != 0:
        print(started.stdout, file=sys.stderr)
        print(adb("logcat", "-d", "-t", "200", "AndroidRuntime:E", "*:S"), file=sys.stderr)
        raise SystemExit("release smoke: MainActivity did not start")
    # The home list is the first composition; a release-only crash shows here as a timeout or a dead process.
    wait_for("home_list")
    tap("tab_settings")
    wait_for("settings_screen")
    tap("tab_workouts")
    wait_for("home_list")
    print("OK: the signed R8 release launched and walked home -> settings -> home")
    return 0


if __name__ == "__main__":
    sys.exit(main())
