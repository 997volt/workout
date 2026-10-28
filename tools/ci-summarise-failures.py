#!/usr/bin/env python3
"""Print failing tests as Markdown, for the GitHub Actions job summary.

F18. Gradle reports `There were failing tests` and nothing else with
`--console=plain`; the names live only in the JUnit XML. Twice now that meant
reading a red X with no idea which test failed, once while the logs and artifacts
were both inaccessible. This puts the names where they can be seen: the job
summary, and the log.

Usage:  python3 tools/ci-summarise-failures.py [results-dir ...]
Defaults to the unit-test and instrumented-test result directories.
"""
from __future__ import annotations

import glob
import os
import sys
import xml.etree.ElementTree as ET

DEFAULT_ROOTS = [
    "app/build/test-results",
    "app/build/outputs/androidTest-results",
]


def failing(path: str) -> tuple[list[str], str | None]:
    """The failing cases in one report, and why the file could not be read when it could not.

    A parse error is reported rather than swallowed: a corrupt report read as "no failures" is the
    same green-that-means-nothing this script exists to prevent (B289).
    """
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as error:
        return [], f"{path}: {error}"
    names = []
    for case in root.iter("testcase"):
        if list(case.iter("failure")) or list(case.iter("error")):
            klass = case.get("classname", "?")
            names.append(f"{klass}.{case.get('name')}")
    return names, None


def main(argv: list[str]) -> int:
    roots = argv[1:] or DEFAULT_ROOTS
    found: list[tuple[str, str]] = []
    broken: list[str] = []
    scanned = 0

    for root in roots:
        for path in sorted(glob.glob(os.path.join(root, "**", "*.xml"), recursive=True)):
            scanned += 1
            names, error = failing(path)
            if error is not None:
                broken.append(error)
            found.extend((path, name) for name in names)

    lines = ["### Failing tests"]
    if found:
        lines.extend(f"- `{name}`" for _, name in sorted(set(found)))
    elif scanned:
        lines.append(f"- No failures in {scanned} result file(s).")
    else:
        lines.append("- No JUnit XML found — the failure was probably earlier than the tests.")

    if broken:
        lines.append("")
        lines.append("### Reports that could not be read")
        lines.extend(f"- `{item}`" for item in broken)

    report = "\n".join(lines)
    print(report)

    # The job summary is where F18 wanted these names; stdout alone leaves them in the log (B289).
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(report + "\n")

    if broken:
        return 1
    return 0 if scanned else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
