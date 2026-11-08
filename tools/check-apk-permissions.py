#!/usr/bin/env python3
"""The merged release's permission and exported-component invariants (B275, B387).

The source manifest is empty, but AndroidX contributes one signature-level permission for dynamically
registered receivers and an exported profile-installer receiver guarded by `android.permission.DUMP`.
"Only the launcher is exported" is therefore a claim about the artifact, not the source file; this checks
both the permission list and every exported component of the actual APK.
"""
from __future__ import annotations

import argparse
import glob
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

USES = re.compile(r"uses-permission: name='([^']+)'")
PACKAGE = re.compile(r"^package: (.+)$", re.MULTILINE)
COMPONENT = re.compile(r"^\s*E: (activity|service|receiver|provider)\b")
NAME = re.compile(r'A: .*:name\([^)]*\)="([^"]+)"')
EXPORTED = re.compile(r'A: .*:exported\([^)]*\)=(?:"([^"]+)"|([^\s]+))')
PERMISSION = re.compile(r'A: .*:permission\([^)]*\)=(?:"([^"]+)"|([^\s]+))')


def aapt2() -> str:
    """The project's pinned build-tools first; PATH's arbitrary aapt2 is the last resort (N297)."""
    roots = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    roots.append(str(Path(__file__).resolve().parent.parent / ".toolchain/android-sdk"))
    for root in roots:
        if not root:
            continue
        pinned = Path(root) / "build-tools" / "36.0.0" / "aapt2"
        if pinned.is_file():
            return str(pinned)
        candidates = sorted((Path(root) / "build-tools").glob("*/aapt2"))
        if candidates:
            return str(candidates[-1])
    found = shutil.which("aapt2")
    if found:
        return found
    raise SystemExit("check-apk-permissions: aapt2 not found; pass --aapt2 or set ANDROID_HOME")


def exported_components(tool: str, apk: Path) -> list[dict[str, str | None]]:
    result = subprocess.run(
        [tool, "dump", "xmltree", str(apk), "--file", "AndroidManifest.xml"],
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if result.returncode != 0:
        print(result.stdout, file=sys.stderr)
        raise SystemExit(f"check-apk-permissions: {tool} could not read {apk}")
    components: list[dict[str, str | None]] = []
    current: dict[str, str | None] | None = None
    for line in result.stdout.splitlines():
        component = COMPONENT.match(line)
        if component:
            current = {"type": component.group(1), "name": None, "exported": None, "permission": None}
            components.append(current)
            continue
        if current is None:
            continue
        name = NAME.search(line)
        if name and current["name"] is None:
            current["name"] = name.group(1)
        exported = EXPORTED.search(line)
        if exported and current["exported"] is None:
            current["exported"] = exported.group(1) or exported.group(2)
        permission = PERMISSION.search(line)
        if permission and current["permission"] is None:
            current["permission"] = permission.group(1) or permission.group(2)
    return components


def check_components(tool: str, apk: Path) -> int:
    components = exported_components(tool, apk)
    for component in components:
        name = component["name"] or "<unnamed>"
        exported = component["exported"] == "true"
        if not exported:
            continue
        if name.endswith(".MainActivity"):
            continue
        if (
            name == "androidx.profileinstaller.ProfileInstallReceiver"
            and component["permission"] == "android.permission.DUMP"
        ):
            continue
        print(f"unexpected exported {component['type']}: {name}", file=sys.stderr)
        return 1
    launcher = next(
        (component for component in components if (component["name"] or "").endswith(".MainActivity")),
        None,
    )
    if launcher is None or launcher["exported"] != "true":
        print("check-apk-permissions: MainActivity is missing or not exported", file=sys.stderr)
        return 1
    print(f"OK: {apk.name} exports only the launcher and the DUMP-guarded profile receiver")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--aapt2")
    args = parser.parse_args()
    tool = args.aapt2 or aapt2()
    result = subprocess.run(
        [tool, "dump", "permissions", str(args.apk)],
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if result.returncode != 0:
        print(result.stdout, file=sys.stderr)
        raise SystemExit(f"check-apk-permissions: {tool} failed on {args.apk}")
    output = result.stdout
    package = PACKAGE.search(output)
    if package is None:
        raise SystemExit("check-apk-permissions: the APK declares no package")
    allowed = {f"{package.group(1)}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
    used = set(USES.findall(output))
    unexpected = used - allowed
    if unexpected:
        print("unexpected uses-permission:", ", ".join(sorted(unexpected)), file=sys.stderr)
        return 1
    print(f"OK: {args.apk.name} uses only the signature-level AndroidX permission ({len(used)} declared)")
    return check_components(tool, args.apk)


if __name__ == "__main__":
    sys.exit(main())
