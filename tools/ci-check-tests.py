#!/usr/bin/env python3
"""Fail when a test run did not execute what its sources declare (ROADMAP B8, B268, B275).

Two suites, one rule. The JVM unit suite and the instrumented suite both declare `@Test`
methods in Kotlin sources and both write JUnit XML — and a report with no failures looks
exactly like a complete one, which is how a green artifact with well under half its tests
was produced (B8). A Gradle task that is UP-TO-DATE replays the previous report, so the
report's age matters as much as its contents (B275).

What is checked, per suite:

  * every `@Test` sits inside a class — a **local** function annotated `@Test` compiles,
    is invisible to JUnit and is how B268's two superset tests never ran;
  * every declared class reported, and reported the tests it declares;
  * no test was skipped (`tests=` counts a skipped case, so it cannot be read alone);
  * no test failed (B193: a run that executed everything and failed is not a green suite);
  * the newest report is not older than the newest source file.

Usage:
    tools/ci-check-tests.py unit
    tools/ci-check-tests.py instrumented

Exit status is 0 only when every check passes.
"""
from __future__ import annotations

import argparse
import pathlib
import re
import sys
from dataclasses import dataclass


@dataclass(frozen=True)
class Suite:
    """Where one suite's sources are, where its report lands, and what to call it."""

    sources: pathlib.Path
    results: pathlib.Path
    noun: str


SUITES = {
    "unit": Suite(
        sources=pathlib.Path("app/src/test"),
        results=pathlib.Path("app/build/test-results/testDebugUnitTest"),
        noun="unit",
    ),
    "instrumented": Suite(
        sources=pathlib.Path("app/src/androidTest"),
        results=pathlib.Path("app/build/outputs/androidTest-results"),
        noun="instrumented",
    ),
}

# A class declaration, and the annotation the runner reads. Matched on the stripped source,
# so neither can be found inside a comment or a string literal.
CLASS = re.compile(r"\bclass\s+([A-Za-z_][A-Za-z0-9_]*)")
TEST = re.compile(r"@Test\b")
SUITE_TAG = re.compile(r"<testsuite\b([^>]*)>")
ATTRIBUTE = re.compile(r'([A-Za-z][A-Za-z0-9_]*)="([^"]*)"')


def strip_source(text: str) -> str:
    """The source with comments and string bodies blanked, and newlines kept.

    Blanking rather than deleting is what lets a match carry the line the check reports.
    Raw strings, escapes and nested block comments are handled because Kotlin sources use
    all three; a brace inside any of them would otherwise move the class stack.
    """
    out: list[str] = []
    i, n = 0, len(text)
    while i < n:
        if text.startswith("//", i):
            end = text.find("\n", i)
            end = n if end < 0 else end
            out.append(" " * (end - i))
            i = end
        elif text.startswith("/*", i):
            depth, end = 1, i + 2
            while end < n and depth:
                if text.startswith("/*", end):
                    depth, end = depth + 1, end + 2
                elif text.startswith("*/", end):
                    depth, end = depth - 1, end + 2
                else:
                    end += 1
            out.append("".join(ch if ch == "\n" else " " for ch in text[i:end]))
            i = end
        elif text.startswith('"""', i):
            end = i + 3
            while end < n and not text.startswith('"""', end):
                end += 1
            end = min(n, end + 3)
            out.append("".join(ch if ch == "\n" else " " for ch in text[i:end]))
            i = end
        elif text[i] in "\"'":
            quote = text[i]
            end = i + 1
            while end < n:
                if text[end] == "\\":
                    end += 2
                elif text[end] == quote:
                    end += 1
                    break
                else:
                    end += 1
            out.append("".join(ch if ch == "\n" else " " for ch in text[i:end]))
            i = end
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def declared_tests(sources: pathlib.Path) -> tuple[dict[str, int], list[str]]:
    """Every `@Test`, per class, plus the ones JUnit cannot see.

    The second list is the B268 shape: an annotation declared inside a function or a lambda
    instead of at the class's own body depth, which compiles and is invisible to the runner.
    A file may declare several classes (three test files do), so the count cannot be per file.
    """
    declared: dict[str, int] = {}
    unreachable: list[str] = []
    for path in sorted(sources.rglob("*.kt")):
        text = strip_source(path.read_text())
        stack: list[tuple[str, int]] = []
        pending: str | None = None
        depth = 0
        line = 1
        i = 0
        while i < len(text):
            char = text[i]
            if char == "\n":
                line += 1
                i += 1
            elif char == "{":
                depth += 1
                if pending is not None:
                    stack.append((pending, depth))
                    pending = None
                i += 1
            elif char == "}":
                depth -= 1
                while stack and stack[-1][1] > depth:
                    stack.pop()
                i += 1
            elif text.startswith("@Test", i):
                # A test is reachable only at its class's own body depth. One indented inside
                # a function body or a `runTest` lambda is a local function to the compiler.
                if not stack:
                    unreachable.append(f"{path}:{line} (outside any class)")
                elif stack[-1][1] != depth:
                    unreachable.append(f"{path}:{line} (inside {stack[-1][0]}'s body, not at its level)")
                else:
                    name = stack[-1][0]
                    declared[name] = declared.get(name, 0) + 1
                i += len("@Test")
            elif (match := CLASS.match(text, i)) and (i == 0 or not text[i - 1].isalnum()):
                pending = match.group(1)
                i = match.end()
            else:
                i += 1
    return declared, unreachable


def executed_tests(results: pathlib.Path) -> tuple[dict[str, int], int, int, float]:
    """Tests per class, skipped, failed, and the newest report's mtime."""
    executed: dict[str, int] = {}
    skipped = 0
    failed = 0
    newest = 0.0
    for path in results.rglob("TEST-*.xml"):
        newest = max(newest, path.stat().st_mtime)
        for tag in SUITE_TAG.finditer(path.read_text()):
            attributes = dict(ATTRIBUTE.findall(tag.group(1)))
            name = attributes.get("name", "").split(".")[-1].split("$")[-1]
            if not name:
                continue
            executed[name] = executed.get(name, 0) + int(attributes.get("tests", "0"))
            skipped += int(attributes.get("skipped", "0"))
            failed += int(attributes.get("failures", "0")) + int(attributes.get("errors", "0"))
    return executed, skipped, failed, newest


def check(suite: Suite) -> int:
    declared, unreachable = declared_tests(suite.sources)
    declared_total = sum(declared.values())
    if declared_total == 0 and not unreachable:
        print(f"no {suite.noun} tests declared — the source layout moved?", file=sys.stderr)
        return 1

    executed, skipped, failed, newest_report = executed_tests(suite.results)
    executed_total = sum(executed.values())
    if not executed:
        print(
            f"no {suite.noun} results found under {suite.results}: "
            "the run produced no JUnit XML at all",
            file=sys.stderr,
        )
        return 1

    print(f"declared {declared_total} {suite.noun} tests in {len(declared)} classes")
    print(f"executed {executed_total} across {len(executed)} classes")

    failed_check = False

    if unreachable:
        print(
            f"\n{len(unreachable)} `@Test` annotation(s) are where JUnit cannot reach them:",
            file=sys.stderr,
        )
        for where in unreachable:
            print(f"  {where}", file=sys.stderr)
        failed_check = True

    missing = {name: count for name, count in declared.items() if name not in executed}
    if missing:
        print("\nclasses that declared tests but reported nothing:", file=sys.stderr)
        for name, count in sorted(missing.items()):
            print(f"  {name}: {count} declared", file=sys.stderr)
        failed_check = True

    short = {
        name: (declared[name], executed[name])
        for name in declared
        if name in executed and executed[name] < declared[name]
    }
    if short:
        print("\nclasses that ran fewer tests than they declare:", file=sys.stderr)
        for name, (want, got) in sorted(short.items()):
            print(f"  {name}: {got} of {want}", file=sys.stderr)
        failed_check = True

    if skipped:
        print(f"\nFAIL: {skipped} {suite.noun} test(s) were skipped. A skipped test is not an "
              "executed one.", file=sys.stderr)
        failed_check = True

    # B193: everything ran and something failed is not a green suite, and "OK" beside a red
    # Gradle task is the sentence that once hid it.
    if failed:
        print(f"\nFAIL: {failed} {suite.noun} test(s) failed. Every declared test may have "
              "executed, and the suite is not green.", file=sys.stderr)
        failed_check = True

    newest_source = max((path.stat().st_mtime for path in suite.sources.rglob("*.kt")), default=0.0)
    if newest_report < newest_source:
        print(
            f"\nthe newest {suite.noun} report is older than the newest test source: this run "
            "replayed a previous report instead of executing the tests",
            file=sys.stderr,
        )
        failed_check = True

    if failed_check:
        return 1

    print(f"\nOK: every declared {suite.noun} test executed and passed")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("suite", choices=sorted(SUITES))
    parser.add_argument(
        "--sources",
        type=pathlib.Path,
        help="override the source directory (for checking the checker)",
    )
    parser.add_argument(
        "--results",
        type=pathlib.Path,
        help="override the results directory (for checking the checker)",
    )
    arguments = parser.parse_args()
    suite = SUITES[arguments.suite]
    if arguments.sources is not None or arguments.results is not None:
        suite = Suite(
            sources=arguments.sources or suite.sources,
            results=arguments.results or suite.results,
            noun=suite.noun,
        )
    return check(suite)


if __name__ == "__main__":
    sys.exit(main())
