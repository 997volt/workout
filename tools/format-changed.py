#!/usr/bin/env python3
"""Run detekt-formatting over the files a change touches, and fail on their changed lines (N129).

B123 settled that the formatting ruleset stays out of the gate: loading it reports ~600 findings
across 155 files, and the only ways to wire it were a repository-wide reformat or a set of
`active: false` entries — the silent gate detekt.yml's header forbids. Its revisit trigger is a
layout defect that ships because a review missed it. This is that trigger answered without either
rejected alternative.

The files are the diff's, and so are the lines:

  * the files, so the repository is not reformatted and no rule is switched off;
  * the lines, because "every finding in a changed file" is decided by the file's own history
    rather than by the change. 150 files carry an ImportOrdering finding today, so a whole-file
    check would fail an edit that never went near an import, and the first thing anyone would do
    is stop running it.

A finding is reported when it lands on a line the change added or rewrote; a pre-existing defect on
an untouched line is left alone. This is a gate on what you wrote, not a reformat of what someone
else did. --all drops that restriction for the changed files, which is how to see what turning a
rule on in detekt-formatting.yml would cost.

**One rule needs more than the line.** detekt's `ImportOrdering` reports at the file's *first* import
line, not at the import that is out of order — so a change that adds the first import would carry a
misordering it never touched, which is the whole class the line filter exists to leave alone (B132).
For that rule the file's base version is read instead, and the finding is kept only when the base
block was already ordered (this change introduced it) or the file is new.

Usage:
    tools/format-changed.py                 # HEAD against the merge base with origin/main
    tools/format-changed.py --base main     # against another ref
    tools/format-changed.py --all           # every finding in the changed files
    tools/format-changed.py --fix           # let the rules rewrite the changed files, then report
"""
from __future__ import annotations

import argparse
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
REPORT = ROOT / "build/reports/detekt-formatting/report.txt"

# The report line: `Rule - message at /abs/path.kt:line:col - Signature=...`.
FINDING = re.compile(
    r"^(?P<rule>[A-Za-z]+) - (?P<message>.*?) at (?P<file>[^ ]+):(?P<line>\d+):(?P<col>\d+) - ",
)

# The detekt-formatting ruleset's own rule names, from its bundled config. The Compose ruleset is a
# plugin too, and buildUponDefaultConfig loads it alongside the formatting one -- so a rule the gate
# already reports could appear here, and a finding reported twice is how a check earns being
# ignored. Anything not in this set is dropped.
FORMATTING_RULES = {
    "AnnotationOnSeparateLine", "AnnotationSpacing", "ArgumentListWrapping",
    "BlockCommentInitialStarAlignment", "ChainWrapping", "ClassName", "CommentSpacing",
    "CommentWrapping", "ContextReceiverMapping", "DiscouragedCommentLocation", "EnumEntryNameCase",
    "EnumWrapping", "Filename", "FinalNewline", "FunKeywordSpacing", "FunctionName",
    "FunctionReturnTypeSpacing", "FunctionSignature", "FunctionStartOfBodySpacing",
    "FunctionTypeReferenceSpacing", "IfElseBracing", "IfElseWrapping", "ImportOrdering",
    "Indentation", "KdocWrapping", "MaximumLineLength", "ModifierListSpacing", "ModifierOrdering",
    "MultiLineIfElse", "MultilineExpressionWrapping", "NoBlankLineBeforeRbrace",
    "NoBlankLineInList", "NoBlankLinesInChainedMethodCalls", "NoConsecutiveBlankLines",
    "NoConsecutiveComments", "NoEmptyClassBody", "NoEmptyFirstLineInClassBody",
    "NoEmptyFirstLineInMethodBlock", "NoLineBreakAfterElse", "NoLineBreakBeforeAssignment",
    "NoMultipleSpaces", "NoSemicolons", "NoSingleLineBlockComment", "NoTrailingSpaces",
    "NoUnitReturn", "NoUnusedImports", "NoWildcardImports", "NullableTypeSpacing", "PackageName",
    "ParameterListSpacing", "ParameterListWrapping", "ParameterWrapping", "PropertyName",
    "PropertyWrapping", "SpacingAroundAngleBrackets", "SpacingAroundColon", "SpacingAroundComma",
    "SpacingAroundCurly", "SpacingAroundDot", "SpacingAroundDoubleColon", "SpacingAroundKeyword",
    "SpacingAroundOperators", "SpacingAroundParens", "SpacingAroundRangeOperator",
    "SpacingAroundUnaryOperator", "SpacingBetweenDeclarationsWithAnnotations",
    "SpacingBetweenDeclarationsWithComments", "SpacingBetweenFunctionNameAndOpeningParenthesis",
    "StringTemplate", "StringTemplateIndent", "TrailingCommaOnCallSite",
    "TrailingCommaOnDeclarationSite", "TryCatchFinallySpacing", "TypeArgumentListSpacing",
    "TypeParameterListSpacing", "UnnecessaryParenthesesBeforeTrailingLambda", "Wrapping",
}


class Failure(Exception):
    """A step that could not run, with the sentence to print."""


def run(*command: str) -> str:
    result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise Failure(f"{' '.join(command)} failed:\n{result.stderr.strip()}")
    return result.stdout


def merge_base(base: str) -> str:
    """What this branch started from, so the diff is the change rather than everything since."""
    for candidate in (f"origin/{base}", base):
        exists = subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", candidate],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
        if exists.returncode == 0:
            return run("git", "merge-base", candidate, "HEAD").strip()
    raise Failure(f"no ref named {base} (or origin/{base}) to diff against")


def changed_files(base: str) -> tuple[str, dict[str, set[int]]]:
    """The base commit and the changed Kotlin files, each with the lines the change added or rewrote.

    Renames, additions and modifications are kept and deletions skipped (--diff-filter=ACMR): a
    file that is gone has no lines to check. git diff -U0 gives the hunks with no context, so every
    line a hunk names is a line the change actually touched.
    """
    head = merge_base(base)
    diff = run("git", "diff", "--unified=0", "--diff-filter=ACMR", f"{head}...HEAD", "--", "*.kt")
    # Work that is not committed yet is checked just as often as a branch is — and `HEAD` rather than
    # no revision, because a new file is untracked and `git diff` (index against working tree) does
    # not mention it at all. This one range covers staged and unstaged changes both.
    diff += run("git", "diff", "--unified=0", "--diff-filter=ACMR", "HEAD", "--", "*.kt")

    files: dict[str, set[int]] = {}
    current: str | None = None
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current = line[len("+++ b/"):]
            files.setdefault(current, set())
        elif line.startswith("@@") and current is not None:
            hunk = re.match(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", line)
            if not hunk:
                continue
            start = int(hunk.group(1))
            length = int(hunk.group(2) or 1)
            files[current].update(range(start, start + length))
    # A brand-new file is untracked, and no form of `git diff` mentions it — so it is asked for by
    # name. Every line of it is the change, which is what the whole-file case reduces to.
    for name in run("git", "ls-files", "--others", "--exclude-standard", "--", "*.kt").splitlines():
        path = ROOT / name
        if path.is_file():
            files.setdefault(name, set()).update(range(1, len(path.read_text().splitlines()) + 1))

    # A file with no hunks (a pure mode change) has nothing to check.
    return head, {name: lines for name, lines in files.items() if lines}


def base_imports_are_ordered(head: str, path: str) -> bool:
    """Whether [path]'s import block was already in order at [head] (B132).

    The check behind the ImportOrdering rule's line filter: detekt reports that rule at the file's
    first import, so a changed first line carries whatever misordering the block already had. This
    reads the base version and applies ktlint's own rule — the imports in ASCII order with `java`,
    `javax`, `kotlin` and aliases last — so a finding is kept only when the change itself broke the
    order. A file the base does not have (an addition) counts as ordered: everything in it is new.
    """
    result = subprocess.run(
        ["git", "show", f"{head}:{path}"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        return True
    imports = [
        line.strip()
        for line in result.stdout.splitlines()
        if line.strip().startswith("import ")
    ]
    return _imports_ordered(imports)


def _imports_ordered(imports: list[str]) -> bool:
    """ktlint's default ImportOrdering: `*`, then `java`, `javax`, `kotlin`, then aliases last."""

    def group(statement: str) -> int:
        if " as " in statement:
            return 4
        package = statement[len("import "):]
        for index, prefix in enumerate(("java.", "javax.", "kotlin."), start=1):
            if package.startswith(prefix):
                return index
        return 0

    order = [(group(statement), statement) for statement in imports]
    return order == sorted(order)


def analyse(files: list[str], fix: bool) -> None:
    """Runs the Gradle task over [files], which is where the ruleset is configured."""
    # A task that is UP-TO-DATE replays its previous report, so the file this run reads is proved to
    # be this run's by deleting it first (B289).
    REPORT.unlink(missing_ok=True)
    command = [
        "./gradlew",
        ":app:detektFormattingCheck",
        f"-PdetektFormattingFiles={','.join(files)}",
        "--console=plain",
    ]
    if fix:
        command.append("-PdetektFormattingAutoCorrect=true")
    result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise Failure(
            f"detekt-formatting could not run:\n{result.stdout.strip()}\n{result.stderr.strip()}",
        )


def findings() -> tuple[list[tuple[str, str, str, int, str]], set[str]]:
    """The report as (rule, file, message, line, where), plus any rule this file does not know.

    A rule outside [FORMATTING_RULES] is not dropped silently: the report is supposed to contain
    formatting findings only, and a name that is not in the set means either a new rule (the set
    needs it) or a ruleset that loaded where it should not have (B289). Both are worth failing on.
    """
    if not REPORT.is_file():
        raise Failure(f"no report at {REPORT}: the task did not run")
    parsed = []
    unknown = set()
    for line in REPORT.read_text().splitlines():
        match = FINDING.match(line)
        if not match:
            continue
        if match.group("rule") not in FORMATTING_RULES:
            unknown.add(match.group("rule"))
            continue
        number = int(match.group("line"))
        parsed.append(
            (
                match.group("rule"),
                match.group("file"),
                match.group("message"),
                number,
                "{number}:{column}".format(number=number, column=match.group("col")),
            ),
        )
    return parsed, unknown


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="main", help="the ref this change is against")
    parser.add_argument("--all", action="store_true", help="report every finding in the changed files")
    parser.add_argument("--fix", action="store_true", help="let the rules rewrite the changed files")
    args = parser.parse_args()

    try:
        head, touched = changed_files(args.base)
        if not touched:
            print("no Kotlin files changed -- nothing to check")
            return 0
        analyse(sorted(touched), fix=args.fix)
        reported, unknown = findings()
        if unknown:
            raise Failure(
                "the report contains rules this script does not know: "
                + ", ".join(sorted(unknown))
                + " — a new formatting rule (add it to FORMATTING_RULES) or a ruleset that loaded "
                "where it should not have",
            )
    except Failure as failure:
        print(f"\nFAIL: {failure}", file=sys.stderr)
        return 1

    if args.fix:
        # The rules rewrote the files, so the report describes what it read, not what is there now.
        print("the formatting rules rewrote the changed files; re-run to see what is left")
        return 0

    kept = []
    for rule, path, message, line, where in reported:
        relative = str(pathlib.Path(path).resolve().relative_to(ROOT))
        lines = touched.get(relative)
        if lines is None:
            # The ruleset reads comments and KDoc too, so a file can carry a finding on a line the
            # diff never named even when the file is in the list.
            continue
        # ImportOrdering anchors at the file's first import, not at the import that is out of order,
        # so it cannot go through the changed-line test at all: the line it reports is usually one the
        # change never touched. It is decided by the base block instead (B132, B289).
        imports = rule == "ImportOrdering"
        if not args.all and imports and not base_imports_are_ordered(head, relative):
            continue
        if args.all or imports or line in lines:
            kept.append((relative, where, rule, message))

    if not kept:
        print(f"no formatting findings on {len(touched)} changed Kotlin file(s)")
        return 0

    for relative, where, rule, message in sorted(kept):
        print(f"{relative}:{where}: {rule}: {message.strip()}")
    print(
        f"\nFAIL: {len(kept)} formatting finding(s) on changed lines. "
        "Fix them, or run tools/format-changed.py --fix.",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
