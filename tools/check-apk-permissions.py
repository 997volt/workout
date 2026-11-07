#!/usr/bin/env python3
"""The merged release must declare no permission a user can grant (B275).

The source manifest is empty, but AndroidX contributes one signature-level permission for dynamically
registered receivers. That is not a runtime permission, but "no permissions at all" is still too strong a
claim; this checks the actual APK and allows only that one.
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


def aapt2() -> str:
    found = shutil.which("aapt2")
    if found:
        return found
    for root in (os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")):
        if root:
            candidates = sorted(glob.glob(os.path.join(root, "build-tools", "*", "aapt2")))
            if candidates:
                return candidates[-1]
    local = Path(__file__).resolve().parent.parent / ".toolchain/android-sdk/build-tools"
    candidates = sorted(local.glob("*/aapt2"))
    if candidates:
        return str(candidates[-1])
    raise SystemExit("check-apk-permissions: aapt2 not found; pass --aapt2 or set ANDROID_HOME")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--aapt2")
    args = parser.parse_args()
    tool = args.aapt2 or aapt2()
    output = subprocess.run(
        [tool, "dump", "permissions", str(args.apk)],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    ).stdout
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
    return 0


if __name__ == "__main__":
    sys.exit(main())
