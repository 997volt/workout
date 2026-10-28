# Working in this repository as an agent

This file is the router **and** the always-loaded digest: where truth lives, the facts
that bite, the gate to run, and each rule in one line. Reasoning, scope and rejected
alternatives are in [DECISIONS.md](DECISIONS.md) — its argument in
[DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md) — which **wins any disagreement with this
file**.

## Read, by question

- **What is planned** — [ROADMAP.md](ROADMAP.md); feature ids (`F#`, `B#`, `N#`, `P#.#`)
  are stable and go in commits. Forward-looking, not binding.
- **What the import helper is** — [IMPORT-HELPER.md](IMPORT-HELPER.md); the zero-dependency page in
  [`docs/`](docs/) that turns old spreadsheets into a backup file, charts one lift's progress from a
  loaded export, converts Waistline body data (N166), turns a program table into a program document
  (N167), and edits a program's document with the same core the app applies (N172). Its settled rules are in [DECISIONS.md](DECISIONS.md); its output is decoded by the app's own
  JVM test (N160), and its progress series is pinned to the app's trend (N165).
- **What is settled** — [DECISIONS.md](DECISIONS.md); the rules that bind every change,
  argued in [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).
- **What shipped** — [CHANGELOG.md](CHANGELOG.md); shipped work leaves the roadmap for here.
- **Cutting a release** — [RELEASING.md](RELEASING.md); binding while releasing.
- **Building, installing, the toolchain** — [README.md](README.md); its emulator
  invocation is the source of truth.

Three layers sit around those:

- **On demand:** the [`android-device-loop`](.dsh/skills/android-device-loop/SKILL.md)
  skill — load it before touching a device. (`android-ui-automation` ships with the
  `@zseven-w/dsh-android` plugin, not this repo.)
- **Cited sources:** [version.properties](version.properties) and
  [gradle/libs.versions.toml](gradle/libs.versions.toml); the generated
  [app/schemas](app/schemas) migration contract. `.env`, `keystore.properties` and
  `local.properties` are gitignored machine state — named, not linked.
- **Gates, not prose:** [detekt.yml](config/detekt/detekt.yml),
  [build.gradle.kts](app/build.gradle.kts) (`warningsAsErrors`),
  [.editorconfig](.editorconfig), [android.yml](.github/workflows/android.yml),
  [dependabot.yml](.github/dependabot.yml), [gradle.properties](gradle.properties),
  [ci-check-tests.py](tools/ci-check-tests.py),
  [proguard-rules.pro](app/proguard-rules.pro), [LICENSE](LICENSE).

Neither list is complete: the commit convention lives in git history, and an inventory of
files is the hand-maintained fact [ROADMAP.md](ROADMAP.md) refuses.

## Environment facts that bite

`$HOME` is not writable, so anything falling back to `~/.gradle` or `~/.android` fails
with `EACCES`. Source the workspace toolchain first (a no-op without `.toolchain/`):

```sh
. ./tools/android-env.sh
```

Use `tools/bin/adb`, not a system `adb`. Starting an emulator needs wider sandbox access
(it opens `/dev/kvm`); everything after that runs normally.

One write under `$HOME` fails harmlessly, even with the env sourced: the Kotlin daemon
logs `AccessDeniedException` for `~/.local/share/kotlin/daemon`, and compilation falls back
in-process. The build succeeds — do not chase it.

## Conventions that are easy to get wrong

Match these — they are the existing ones. Reasons, scope and rejected alternatives are in
[DECISIONS.md](DECISIONS.md#rules-that-apply-to-every-change).

- **Errors are values.** Reads and writes return
  [`DataResult`](app/src/main/java/com/example/androidapp/domain/DataResult.kt), never a
  thrown exception lost in a coroutine.
- **No mocking framework, and no new test dependency without asking.** Hand-written fakes,
  JUnit 4, Truth in new or touched tests. Compose UI tests run on the JVM under
  Robolectric; the instrumented suite is Room and DAOs.
- **Migrations are numbered as they ship.** Never add a column ahead of the code that
  reads it, and never edit a shipped migration — procedure in DECISIONS.md.
- **Enums by name, never ordinal**; weights are whole grams in a `Long`; RPE is
  half-points in an `Int`.
- **Delete an API the moment nothing calls it**; extract a shared component at its second
  caller.
- **Accessibility accompanies each screen, not a later pass:** name what a control does,
  announce state changes, address controls by `TestTags`, not English literals.

## Before you call it done

`lint` fails on warnings and detekt carries the Compose ruleset. Run what CI runs:

```sh
./gradlew testDebugUnitTest :shared:jvmTest :shared:jsNodeTest lint detekt :shared:detekt assembleDebug assembleRelease --console=plain
```

[`DocsConsistencyTest`](app/src/test/java/com/example/androidapp/DocsConsistencyTest.kt)
keeps that task set equal to CI's and these links resolving. The configuration cache is on
by default, so reruns are fast.

The import helper has its own gate, under node rather than Gradle (CI's `docs` job, N173):
`node docs/tests-node.js` runs the page's editor assertions against the committed `docs/shared`
build, `node docs/build-single-file.js` proves the portable copy still builds, and
`node docs/fixtures/generate.js` followed by `git diff --exit-code -- docs/fixtures docs/shared`
proves the generated files are current. Run them when touching `docs/`.

Formatting is not in that set: `detekt-formatting` reports a few hundred findings across the tree — the
current count and the command that produces it are in [DECISIONS.md](DECISIONS.md#process) — so
turning it on wholesale is the reformat B123 rejected. Run it over **what you changed**
instead — it reports a finding only when it lands on a line the diff added or rewrote:

```sh
tools/format-changed.py            # the branch against origin/main
tools/format-changed.py --fix      # let the rules rewrite those files
```

The instrumented job is not the default — `connectedDebugAndroidTest` needs a device — and
a green report is not proof it ran: no failures looks like a complete run, so
`tools/ci-check-tests.py instrumented` fails the job when the executed count disagrees with
the declared one, when anything was skipped, or when the report is older than the sources.
The same check runs over the unit suite in the build job (`tools/ci-check-tests.py unit`),
because a test JUnit cannot see is not an instrumented-only failure: B268's two superset
tests had never run at all.

## Hard constraints — do not break these

- **Local only.** No `INTERNET`, accounts, analytics or server; the export file is the
  only path off the device.
- **No Google Play services at runtime** — a degoogled device must run the app.
- **User data is health data.** It stays in app-private storage and leaves only in an
  export the user chose to make.

## Finishing a change

Commit subject `type: what changed (ids)`, reason in the body, including why a rejected
alternative was rejected. If nothing in [ROADMAP.md](ROADMAP.md) covers the change, say so
— do not invent an id.
