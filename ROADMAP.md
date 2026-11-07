# Workout — Roadmap

> **What is planned, not what happened.** *Next* is the batch picked up before anything else, *Later* is decided
> and waiting, *Parked* is decided against until a named trigger fires, and the non-goals are lines this app
> does not cross.
> Shipped work is [CHANGELOG.md](CHANGELOG.md), how a release is cut is [RELEASING.md](RELEASING.md), and the
> settled rules are [DECISIONS.md](DECISIONS.md) with their argument in
> [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).

A local-only training notebook: write a plan, log what you actually did against it, and let
the app show you the difference and what to do next.

Ids (`F#` foundations, `B#` defects, `N#` the next planned changes, `P#.#` the product
backlog, `R#.#` releases) are stable and go in commit messages. They were assigned when the
work was planned, so they do not always run in order — and an id this file does not list has
shipped, with its entry in [CHANGELOG.md](CHANGELOG.md) for the current line or
[CHANGELOG-1.x.md](CHANGELOG-1.x.md) for the 1.x releases.

## Next — 2.26: release and CI hardening

2.25 is released. This batch closes the release-integrity and CI gaps left in *Later* before another feature is
taken: pinning the toolchain, testing the privacy manifest, making the emulator timeout match the measured
boot, bounding artifact retention, using one Node major, and keeping the test-only provider out of the release
APK. N276 remains a separate immediate follow-up because it changes the release verifier itself.

- **N272 — Pin the supply chain.** The wrapper properties carry no `distributionSha256Sum` although the comment
  says the workflow validates it (it validates the JAR, not the distribution), and there is no dependency
  verification or locking. Fixed means the wrapper checksum is present and dependency verification metadata is
  committed and checked in CI.

- **N275 — Assert the manifest invariants the privacy claims rest on.** No test checks `allowBackup`,
  `dataExtractionRules`, exported components or permissions; the merged release manifest already carries
  androidx's signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, so "no permissions at all" is
  imprecise. Fixed means a source-manifest test plus a CI check of the merged permissions, and the README
  says "no dangerous/runtime permissions" (or explains the one).

- **N278 — Artifacts need a retention policy.** The reports and the R8 mapping are uploaded with no
  `retention-days`; on a nightly workflow the default retention compounds a large mapping file and test
  reports. Fixed means a retention window per artifact, with the mapping attached to the release asset for
  permanent retention.

- **N279 — The docs job must run the same pinned Node as the build.** The build job pins Node 22; the docs job
  only echoes `node --version` and runs the helper's tests on whatever the runner ships, so the two jobs can
  test the same generated `docs/shared` on different majors. Fixed means the same `setup-node` version in both
  jobs.

- **N281 — The test-only FileProvider should not ship.** The production manifest declares a `FileProvider`
  scoped to a cache directory only because `DocumentIoTest` needs a real content URI. Fixed means the provider
  moves to the instrumented manifest (or the test uses a test-local provider), and the release manifest no
  longer carries it.

## Later — the rest of the 2.22 review

The rows below are real and specified, but none is in flight: a defect here is low-frequency or a polish item,
an engineering row is not user-visible, and a feature is waiting for a decision. A row leaves this section when
it is picked up; its design argument moves to [DECISIONS.md](DECISIONS.md).

### Defects and polish

- **B358 — The shell and the screens must not both apply the navigation-bar inset.** The shell reserves the
  tab bar, whose height already includes the system bar, and each screen's default `Scaffold` adds the system
  inset again ([AppNavHost.kt](app/src/main/java/com/example/androidapp/ui/navigation/AppNavHost.kt)); the
  deload screen adds a third `navigationBarsPadding()`. Fixed means the shell or the screens own the bottom
  inset exactly once, verified on a gesture-navigation and a three-button device (this was reasoned from code,
  not measured).

### Build, CI, tests and docs

- **N276 — Verify release monotonicity and tag binding.** `verify-release.py` compares the APK to
  `version.properties` but not to the previous tag, so a typo that lowers `versionCode` passes; the "tag points
  at the commit that built the APK" rule is procedural. Fixed means the script reads the latest tag's version
  and requires a strictly greater `versionCode`, the release notes record the built commit, and the
  keystore password is read from the environment or a file rather than passed on `keytool`'s argv.

- **N282 — Reads should stay cheap as the history grows.** `workout_sessions` has no index on `templateId`,
  `measurements` has none on `measuredAt`, the program-document merge does an N+1 read per template/exercise,
  and `buildProgramDocument` queries the program's slots twice. Fixed means a new migration adds the two
  indices (if measurement/plan sizes justify it) and the merge reads its ids in one query; a query-plan or
  timing test proves the change.

- **N283 — Test the orchestration ViewModels that have no direct tests.** `WorkoutsHomeViewModel` (discard,
  skip, substitute), `WorkoutHistoryViewModel` (query/filter), `ProgramEditorViewModel` (write/export),
  `DiagnosticsViewModel` (retry), `ProgramBalanceViewModel`, `HousekeepingViewModel` and
  `WeightUnitViewModel` are covered only through helpers and screens. Fixed means focused JVM tests with fake
  ports for their state transitions, starting with the destructive Home handlers.

- **N284 — One write-outcome abstraction for every editor.** B284's "close on the answer, not the tap" rule is
  implemented three ways (workout `SetWrite`, template `Boolean`, history a bare callback), which is how B340
  and B352 happened. Fixed means one `WriteOutcome<T>`/consume-once pattern used by the set, template and
  history editors, with the sticky-state and cross-dialog regressions covered by shared tests.

- **N285 — Split the largest files.** `ActiveWorkoutViewModel`, `TestTags`, `RoomProgramRepository`,
  `TemplateEditorScreen` and `ActiveWorkoutScreen` are the files where review misses happen and where detekt
  suppressions accumulate. Fixed means each is split by responsibility without changing behaviour, with the
  existing tests as the safety net.

- **N286 — Inject "today" and "now", and refresh the day when a screen resumes.** Most screens capture
  `ZoneId.systemDefault()` and `todayDate` when the ViewModel is built, so a screen left open across midnight
  (or a flight) keeps yesterday's plan and measurement row; this is documented as accepted staleness, but it
  is also why the date-boundary tests are hard to write. Fixed means a `ZoneSource`/clock seam beside
  `TimeSource` and a resume check that re-reads the day on Home, Measurements and the weekly review.

### Features

Each row below is a product candidate, not an agreed design; the constraints and the decision it touches are
named so the row can be picked up without re-arguing it from scratch. The id is the planning id; no row here is
agreed to ship.

- **N287 — Lean mass, derived from the body the app already records.** `weight − fat mass` is a series of its
  own, exactly as N265 made fat and muscle mass derived series; the two inputs are already stored and nothing
  new is persisted. `null` when either input is missing, the name says what it was made of, and it reads in the
  same mass unit as fat mass. Small; the only open question is whether the Statistics unit follower (N295)
  lands first.

- **N288 — Search history by what was written, not only by plan/activity/month.** N146 reaches the session's
  plan, activity and month; notes and exercise names are in the store but unsearchable. A query over session
  notes and the names of logged exercises (optionally set notes) would answer "when did I last write down the
  shoulder tweak" without loading every set for every keystroke. Small–medium; the result row must say which
  field matched.

- **N289 — Show what the log actually trained, per muscle, as counts only.** `muscleBalance` already defines
  the counting rule for a *plan* (primary 1, each secondary 0.5, warm-ups excluded); the same rule over logged
  sets in a 7/28-day window is a factual read. It must present counts, never a target range or a warning, which
  keeps P2.8 ("muscle-group balance warnings") parked. Medium; a Statistics group or a Plan/Trained toggle on
  the balance screen.

- **N290 — A records timeline.** The app celebrates a record at the moment it happens and per-lift bests exist,
  but the story — first time at this rep count, a best beaten, a hold extended — is not readable. It is
  derivable from the existing sets with `PersonalRecords`' rules; no schema. Medium; a windowed list keeps the
  query cheap.

- **N291 — Substitute an exercise inside a workout.** Weekly programs can substitute a workout for an
  occurrence, but mid-session the only options are remove and add, which loses the plan context. A session
  exercise's menu could offer "Substitute exercise", keeping its position and marking the stand-in so plan
  comparison says "substituted" rather than "missed". Medium; the plan-comparison rule is the open design
  question, and the app still suggests rather than writes.

- **N292 — Multi-week program phases.** A weekly program repeats forever and a sequence block happens once;
  there is no authored multi-week block (week 1/2/3 prescriptions). This is a real programming capability and
  the largest item on this list: new schema and program-document fields, a format-version bump, and an editor
  for the phases. Each week is authored by the lifter; the app does not auto-progress loads, which keeps N22's
  "no percentage/coach rule" decision intact.

- **N293 — Manual duration and distance for activities.** An activity is currently a marker with no numbers, so
  a swim or a ride records only "the day was trained". Manual duration/distance fields would extend the
  notebook without adding GPS or a new permission. Medium–large: schema, backup and helper format, and a
  decision about whether the values live on the session or a record of their own.

- **N294 — An encrypted backup file.** Encryption at rest is parked; an encrypted *export* is narrower: a
  password-protected file for a folder another app syncs, still user-chosen and local. It needs a new format
  version, Android Keystore/AES (no new dependency), and plaintext import kept for existing files. Only worth
  doing when a backup actually leaves the device.

- **N295 — Let weight-based Statistics follow the app's unit.** N64 deliberately excluded Statistics and body
  measurements from the kg/lb switch so a chart could not change with the lift picked last. A narrower rule
  — follow the app-wide unit for weight-based series, keep body metrics metric-owned — would fix the surprise
  a pounds user meets today, but it is a decision change and needs the same round-trip tests as N64.

## Parked — deliberately not planned

Each row is a product in its own right, contradicts "local-only", or both. Parking is a
decision, not a backlog, and every row names what would change it. Parked is **not** the same
as the non-goals below: these become possible again the moment their trigger fires, while a
non-goal is a line this app does not cross.

| # | Feature | Revisit only if |
| --- | --- | --- |
| P5.4 | Localization | A non-English user appears. |
| P1.11 | Onboarding: goal, experience level, weekly target | This stops being a single-user local tool with one obvious user. It personalises defaults, and there are no defaults to personalise. |
| P1.17 | Accessibility audit | The per-screen rule stops being enough — a real complaint on a device, or a screen that grew past ad-hoc tagging. The rule still applies to every change; only the sweep is parked. |
| P2.5 | Progress photos | A visual record is actually wanted, and an encrypted-storage design for it is acceptable. |
| P2.8 | Muscle-group balance warnings | Enough history exists for a rolling window to say something true rather than something plausible. |
| P2.6 | Plate calculator | Loading from a plan's target is frequent enough that the arithmetic gets in the way, and you would rather it were done for you. |
| — | **A *Removed* view of the exercise library** | The app gains a way to remove a movement. Nothing can remove one today — the library's soft delete has no caller — so only a restored file can hide a row, and a group of them would draw a list nobody on this device could have made. |
| — | **Play Store listing** | You want distribution beyond `adb install`. Self-install works today, and Play App Signing would change who holds the signing key. |
| — | **Encryption at rest / app lock** | You start carrying the phone somewhere you would not carry the data. |

## Explicit non-goals

Permanent, unlike *Parked*: no trigger brings one of these back.

- Nutrition / calorie tracking
- Social feeds, friends and shared routines (P3.7, P5.2)
- A web dashboard that **holds, syncs or accounts for** the training record — an account, a stored copy, a
  place your history lives. A computed view of a file you loaded and can close is not one (N159, N165).
- Live GPS route tracking
- Health Connect read/write (P4.1)
- A foreground service (P4.2)
- A home-screen widget (P4.3)
- Quick Settings / launcher shortcuts (P4.4)
- A Wear OS companion (P4.5)
- Bluetooth heart-rate straps (P4.6)
- WorkManager reminders (P4.7)
- Large-screen layouts (P4.8)
- Offline-first sync (P4.9)
- Monetization / Play Billing (P5.3)
- A module split into `:core:*` / `:feature:*` (F6)
- Product analytics (F11b)

## Keeping this true

Four rules, each answering drift this file has shown before.

1. **Nothing marked done lives here.** Shipped work goes to [CHANGELOG.md](CHANGELOG.md), and a
   finished row is deleted from this file.
2. **No hand-maintained facts.** No test counts, no dependency lists, no inventory of which
   files exist. Those are commands (`./gradlew …`) or links.
3. **Every parked row names its revisit trigger**, so parking reads as a decision rather than a
   forgotten item.
4. **Durable content lives in [DECISIONS.md](DECISIONS.md).** Settled decisions and the rules
   that apply to every change are a reference, not a queue, and the two age differently. A
   section here that accumulates rather than drains belongs there.

Everything that has **shipped** has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
