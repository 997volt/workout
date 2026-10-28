#!/usr/bin/env python3
"""Verify the release APK against what the notes will claim about it (ROADMAP N128).

Four checks that were done by hand on every release, and twice now:

  1. the APK's `versionCode`/`versionName` are the ones `version.properties` states,
  2. its signer certificate is the one in the keystore the release is signed with,
  3. its SHA-256 — the number the notes carry — printed together with its size,
  4. with `--remote`, the asset published against the tag is the file just checked.

**The first three need no network and no token.** `aapt2` and `apksigner` come from the
SDK build-tools, the keystore from the gitignored `keystore.properties`, and the version
from the repo — so this runs on CI, on a machine that has never seen a release, and from a
fresh checkout. CI has no keystore and signs nothing, so there the certificate is reported
as unchecked rather than failed; everywhere else it is required. The remote half is the part
that needs credentials, so it is opt-in rather than the default, and it uses `gh` for the
same reason: the token already lives there.

The certificate is the check that matters most. Get it wrong and the APK is refused as an
update by every phone that has the app, and the only fix is an uninstall — which deletes the
workout history, because platform backup is off. Comparing against the keystore the build
signs *with* is stronger than comparing against the previous release's APK: the two are the
same question, and the keystore is always here.

Usage:
    tools/verify-release.py                                  # the built release APK
    tools/verify-release.py --apk path/to/workout-2.5.apk
    tools/verify-release.py --remote v2.5                    # …and the published asset
"""
from __future__ import annotations

import argparse
import hashlib
import os
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
# A release machine signs and writes app-release.apk; CI has no keystore and writes only the
# unsigned fallback, so the version and hash checks have to read either name (B131).
SIGNED_APK = ROOT / "app/build/outputs/apk/release/app-release.apk"
UNSIGNED_APK = ROOT / "app/build/outputs/apk/release/app-release-unsigned.apk"
VERSION_FILE = ROOT / "version.properties"
KEYSTORE_FILE = ROOT / "keystore.properties"

# Where a released APK is attached (RELEASING.md step 7 names it `workout-<version>.apk`).
ASSET = "workout-{version}.apk"


def default_apk() -> pathlib.Path:
    """The APK to check when `--apk` is not given (B131).

    Prefer the signed output a release machine writes, and fall back to CI's unsigned one, whose
    `versionCode`/`versionName` and SHA-256 are readable just the same — only the certificate needs a
    keystore. With neither present, name the signed one, which is what a release machine forgot.
    """
    if SIGNED_APK.is_file():
        return SIGNED_APK
    if UNSIGNED_APK.is_file():
        return UNSIGNED_APK
    return SIGNED_APK


class CheckFailed(Exception):
    """A verification that failed, with the sentence to print."""


def sdk_tool(name: str) -> str:
    """`aapt2`/`apksigner` from the workspace build-tools, else from PATH.

    The version is whatever is installed rather than pinned: a release may be cut with a newer
    build-tools than the one this file last saw, and the tools\' output is stable across them.
    """
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        build_tools = pathlib.Path(sdk) / "build-tools"
        if build_tools.is_dir():
            for version in sorted(build_tools.iterdir(), key=version_key, reverse=True):
                candidate = version / name
                if candidate.is_file():
                    return str(candidate)
    found = shutil.which(name)
    if found:
        return found
    raise CheckFailed(
        f"{name} not found: source tools/android-env.sh, or install the SDK build-tools",
    )


def read_properties(path: pathlib.Path) -> dict[str, str]:
    """The `key=value` files this repo keeps at the root, comments and blanks skipped."""
    properties: dict[str, str] = {}
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        properties[key.strip()] = value.strip()
    return properties


def apk_version(apk: pathlib.Path, badging: str) -> tuple[str, str]:
    """The `versionName` and `versionCode` aapt2 reads out of the built APK itself."""
    if not apk.is_file():
        raise CheckFailed(f"no APK at {apk} — build it first (tools/build-apk.sh)")
    dump = subprocess.run(
        [badging, "dump", "badging", str(apk)],
        capture_output=True,
        text=True,
        check=False,
    )
    if dump.returncode != 0:
        raise CheckFailed(f"aapt2 could not read {apk}:\n{dump.stderr.strip()}")
    package = next(
        (line for line in dump.stdout.splitlines() if line.startswith("package:")),
        None,
    )
    if package is None:
        raise CheckFailed(f"no `package:` line in aapt2\'s output for {apk}")
    name = re.search(r"versionName=\'([^\']*)\'", package)
    code = re.search(r"versionCode=\'([^\']*)\'", package)
    if not name or not code:
        raise CheckFailed(f"no version in aapt2\'s package line: {package}")
    return name.group(1), code.group(1)


def apk_certificate(apk: pathlib.Path, apksigner: str) -> str:
    """The SHA-256 of the certificate the APK is signed with, uppercase hex, no separators."""
    verify = subprocess.run(
        [apksigner, "verify", "--print-certs", str(apk)],
        capture_output=True,
        text=True,
        check=False,
    )
    # A failure here is the unsigned-APK trap RELEASING.md lists: CI signs nothing, so a
    # CI-shaped build leaves an unsigned `app-release-unsigned.apk` in the output directory.
    if verify.returncode != 0:
        raise CheckFailed(
            f"apksigner refused {apk} — it is not signed:\n{verify.stderr.strip()}",
        )
    digest = re.search(
        r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]+)",
        verify.stdout,
    )
    if not digest:
        raise CheckFailed(f"no signer certificate in apksigner\'s output for {apk}")
    return digest.group(1).upper()


def keystore_certificate(properties: dict[str, str]) -> str:
    """The SHA-256 of the key the next release will be signed with.

    Read from the keystore rather than from the previous release\'s APK, because that is the
    same question and this file is always here — but it does mean `keytool` has to be on the
    JDK the build uses.
    """
    store = ROOT / properties.get("storeFile", "")
    if not store.is_file():
        raise CheckFailed(f"keystore {store} not found (see README: Install on your own phone)")
    keytool = pathlib.Path(os.environ.get("JAVA_HOME", "")) / "bin/keytool"
    keytool = str(keytool) if keytool.is_file() else "keytool"
    listing = subprocess.run(
        [
            keytool,
            "-list",
            "-v",
            "-keystore",
            str(store),
            "-storepass",
            properties.get("storePassword", ""),
            "-alias",
            properties.get("keyAlias", ""),
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    # keytool prints the password prompt to stdout on some builds, so the failure goes by return
    # code rather than by matching text — a mismatch here is never a wording change.
    if listing.returncode != 0:
        raise CheckFailed("keytool could not read the keystore; check keystore.properties")
    digest = re.search(r"SHA256:\s*([0-9A-Fa-f:]+)", listing.stdout)
    if not digest:
        raise CheckFailed("no SHA256 fingerprint in keytool\'s output")
    return digest.group(1).replace(":", "").upper()


def on_ci() -> bool:
    """Whether this is a CI run — a *truthy* CI, not merely a name in the environment (B289).

    GitHub sets CI=true. A shell may export CI=false to mean the opposite, and reading presence
    alone turned the one check that matters most into a silent pass there.
    """
    return os.environ.get("CI", "").strip().lower() not in ("", "0", "false")


def version_key(path: pathlib.Path) -> tuple[int, ...]:
    """A build-tools directory name as numbers, so 36.0.0 beats 9.0.0 (B289)."""
    return tuple(int(part) for part in re.findall(r"\d+", path.name)) or (0,)


def sha256(apk: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with apk.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def verify_asset(
    tag: str,
    expected_name: str,
    expected_code: str,
    expected: str,
    badging: str,
    apksigner: str,
    expected_certificate: str | None,
) -> None:
    """The published asset against the APK just checked (opt-in, needs `gh` and a token)."""
    if not shutil.which("gh"):
        raise CheckFailed("--remote needs `gh` on PATH")
    asset = ASSET.format(version=expected_name)
    with tempfile.TemporaryDirectory() as directory:
        download = subprocess.run(
            ["gh", "release", "download", tag, "--pattern", asset, "--dir", directory],
            capture_output=True,
            text=True,
            check=False,
        )
        if download.returncode != 0:
            raise CheckFailed(
                f"gh could not download {asset} from {tag}:\n{download.stderr.strip()}",
            )
        published = pathlib.Path(directory) / asset
        if not published.is_file():
            raise CheckFailed(f"{tag} has no asset named {asset}")
        if sha256(published) != expected:
            raise CheckFailed(
                f"the published {asset} is not this APK: its SHA-256 differs from the built one",
            )
        # The bytes match, which is the real check. These two say the asset is *installable* as
        # the release the notes describe rather than merely the same file under a different name.
        if apk_version(published, badging) != (expected_name, expected_code):
            raise CheckFailed(f"the published {asset} is not {expected_name} ({expected_code})")
        if expected_certificate is not None and apk_certificate(published, apksigner) != expected_certificate:
            raise CheckFailed(
                f"the published {asset} is signed by a different key than the one it was built with",
            )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--apk",
        default=None,
        help="the built release APK (default: the signed one, else CI's unsigned one)",
    )
    parser.add_argument(
        "--remote",
        metavar="TAG",
        help="also check the asset published against this tag (needs gh)",
    )
    args = parser.parse_args()
    apk = pathlib.Path(args.apk) if args.apk else default_apk()

    try:
        properties = read_properties(VERSION_FILE)
        expected_name = properties.get("versionName", "")
        expected_code = properties.get("versionCode", "")
        if not expected_name or not expected_code:
            raise CheckFailed(f"{VERSION_FILE} states no versionName/versionCode")

        badging = sdk_tool("aapt2")
        apksigner = sdk_tool("apksigner")

        name, code = apk_version(apk, badging)
        if (name, code) != (expected_name, expected_code):
            raise CheckFailed(
                f"the APK is {name} ({code}), but version.properties states "
                f"{expected_name} ({expected_code}) — rebuild after the bump",
            )
        print(f"version     {name} ({code}) — matches version.properties")

        verified_certificate: str | None = None
        signing = read_properties(KEYSTORE_FILE) if KEYSTORE_FILE.is_file() else {}
        if not signing:
            # CI has no keystore and signs nothing, so the *absence* is not a defect there — but it
            # is on the machine cutting the release, which is where this refuses rather than warns.
            if not on_ci():
                raise CheckFailed(
                    "keystore.properties is missing, so the certificate cannot be checked "
                    "(see README: Install on your own phone)",
                )
            print("certificate not checked: no keystore.properties (CI signs nothing)")
        else:
            built = apk_certificate(apk, apksigner)
            expected = keystore_certificate(signing)
            if built != expected:
                raise CheckFailed(
                    "the APK is signed with a different key than the keystore — every phone "
                    "would refuse it as an update, and an uninstall would delete the history",
                )
            verified_certificate = expected
            print("certificate matches the keystore")

        # CI reaches the hash too (B131): the version and the hash are the two things an unsigned
        # build can still be checked against, and the hash is the number the notes will carry.
        digest = sha256(apk)
        print(f"size        {apk.stat().st_size} bytes")
        print(f"sha256      {digest}")

        if args.remote:
            verify_asset(
                tag=args.remote,
                expected_name=expected_name,
                expected_code=expected_code,
                expected=digest,
                badging=badging,
                apksigner=apksigner,
                expected_certificate=verified_certificate,
            )
            print(f"published   {ASSET.format(version=expected_name)} on {args.remote} is this APK")
    except CheckFailed as failure:
        print(f"\nFAIL: {failure}", file=sys.stderr)
        return 1

    if signing:
        print("\nOK: the APK is the version, the key and the file the notes will describe")
    else:
        print("\nOK: the APK is the version and the file the notes will describe (no keystore)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
