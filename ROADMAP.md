# Workout — Roadmap

> **What is planned, not what happened.** *Next* is the batch picked up before anything else, *Later* is decided and waiting,
> *Parked* is decided against until a named trigger fires, and the non-goals are lines this app does not cross.
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

## Next — 2.23: the 2.22 review's hardening batch

The whole-project review of 2.22 (4 November 2026, read-only: no source was changed) produced the items in
this section and in the three *Later* queues below. This section is the part that should be fixed before new
features are built; none of it is started. Each row states the current behaviour, why it matters, what
"fixed" means and what test proves it, so a reader does not need the review conversation. Ids continue the
existing run at **B339** and **N268**. When a row is picked up its design argument moves to
[DECISIONS.md](DECISIONS.md) as usual; when it ships it leaves for [CHANGELOG.md](CHANGELOG.md).

### Data correctness and recovery

- **B343 — "Delete everything" must not let the backup folder outlive the promise it makes.** The clear writes
  ([RoomBackupRepository.kt](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt)) empty the
  database, crash logs and the 1.x file, but leave the automatic-backup setting and folder untouched. The
  confirmation says there is "no server copy" and that an export is the only thing that can survive
  ([ClearEverythingDialog.kt](app/src/main/java/com/example/androidapp/ui/components/ClearEverythingDialog.kt)),
  while scheduled copies do survive. Worse, the still-enabled backup then writes an almost-empty copy each
  period and prunes to the newest five, so after five periods the pre-clear full backups are gone. Fixed means
  clear either disables automatic backup and says exactly what remains in the folder, or records a clear
  boundary that keeps earlier files out of the prune; the dialog must describe the real result.

- **B344 — The week in review must read each session's own zone.** `weekTotals` takes one `zone` and does
  `session.startedAt.atZone(zone)` ([WeeklyReview.kt](app/src/main/java/com/example/androidapp/domain/model/WeeklyReview.kt)),
  despite its own KDoc saying the session's own zone is used. Every `WorkoutSummary` carries
  `zoneOffsetMinutes`; history and adherence use it, the review does not. A workout performed abroad near a
  week boundary can therefore be counted in the wrong week and wrong totals. Fixed means
  `session.zoneIdOrNull() ?: zone`, with a test whose sessions carry a non-device offset.

- **B345 — The two multi-write paths that can lose a prior value must be one transaction.**
  `rateExercise` writes the muscle rating, soft-deletes the old joint list, then inserts the new one
  ([RoomWorkoutRepository.kt](app/src/main/java/com/example/androidapp/data/RoomWorkoutRepository.kt)); a
  failure in `insertAll` leaves the old ratings hidden and the new list empty, with no undo. `deleteSession`
  is four writes (sore rows, joint rows, session exercises, session); a failure between the last two leaves a
  live session whose exercises are all hidden. `finishSession` and `finishExercise` are the same shape at lower
  risk (an armed rest timer on a closed workout, live joints under a hidden exercise) and belong in the same
  pass. Fixed means `database.withTransaction` in each, with a test that makes the second write fail and asserts
  the first is rolled back.

- **B346 — A backup must be one snapshot, and import must not parse it on the main thread.** `export()` reads
  every table through separate DAO calls with no transaction
  ([RoomBackupRepository.kt](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt)). A set
  logged between the session-exercise read and the set read is written without its parent, and the file then
  fails the foreign key on import and rolls the whole restore back — the app's only escape hatch is
  unrestorable. Separately, `import` parses, shapes and inventories the whole file on the caller's thread while
  `export` and `preview` correctly switch to IO, and `visibleSetIds` reads outside the import transaction.
  Fixed means the read block is one `withTransaction`, the decode/shape work is on IO, `visibleSetIds` runs
  inside the transaction, and a test logs a set while an export is in flight and proves the file still
  restores.

- **B347 — The backup must carry the movement-merge memory.** `retired_movements` records which merged id
  became which survivor so a program document exported before a merge resolves to the survivor instead of
  recreating the twin (N238, schema 16). It is absent from [BackupFile.kt](app/src/main/java/com/example/androidapp/data/transfer/BackupFile.kt)
  and from the export, so a restore loses that memory and an old document can mint the duplicate the table
  exists to prevent — including a merge the lifter made deliberately, which the automatic repair leaves alone.
  Fixed means a defaulted `retiredMovements` collection in the backup format, restored with the same additive
  rules as the other merge tables, and a test that exports, clears, restores and loads a pre-merge document.

### State and interaction

- **B349 — A rotation must not discard what was typed.** The measurements form is the clearest case: `adding`
  is `rememberSaveable`, so the dialog survives a rotation, while weight, body fat, muscle, tape and the
  picked day are plain `remember` ([MeasurementsScreen.kt](app/src/main/java/com/example/androidapp/ui/measurements/MeasurementsScreen.kt)),
  so the numbers reset and only the saved sore muscles come back. The set editors have the same shape: their
  host keeps the dialog open (`rememberSaveable` or a ViewModel flag) while the draft in
  [SetEditorDialog.kt](app/src/main/java/com/example/androidapp/ui/components/SetEditorDialog.kt) and the
  exercise-detail editor is plain `remember`. Fixed means a `Saver` or ViewModel field for each draft and the
  picked day, with a Robolectric test that rotates a half-filled form and asserts every value survives.

- **B350 — History's set editor must close on the write's answer, not the tap.** `onSave` in
  [WorkoutDetailScreen.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailScreen.kt) calls
  `onUpdateSet(...)` and then `onDismiss()` unconditionally. A refused write (a rung boundary, an invalid load)
  therefore closes the dialog and loses the correction, which is the exact inversion of B284's rule that the
  active workout enforces one screen away. Fixed means the history ViewModel exposes the same written/refused
  outcome and the dialog closes only on a success for its own set, with a test that a refusal keeps the typed
  values.

- **B351 — A failed trends read must not be reported as "no stall".** `PlanStallFinder` does
  `(trends.templateSessions(templateId) as? DataResult.Success)?.data.orEmpty()`
  ([PlanStallFinder.kt](app/src/main/java/com/example/androidapp/domain/PlanStallFinder.kt)), so a storage
  failure becomes an empty history and the weekly review and program editor say the plan is moving. That is
  the "a failed read looks like an empty result" shape B4 fixed for last-trained. Fixed means `firstStall`
  returns a `DataResult` (or otherwise carries the failure) and both callers show the read's error rather than
  inventing a verdict.

- **B352 — One template block's save outcome must not close another block's dialog.** The screen-global
  `setSaveOutcome == true && open != null` test in
  [TemplateEditorScreen.kt](app/src/main/java/com/example/androidapp/ui/templates/TemplateEditorScreen.kt)
  checks only that *some* dialog is open, so a write that finishes on block A can close the dialog just opened
  on block B. The effect is also keyed on a method reference, which can change on every recomposition. Fixed
  means the outcome carries the set it belongs to and the effect compares it with the open block's id.

- **B353 — The app's screen-reader structure must exist.** The settings and program-editor switches are a sibling `Text` plus an
  unnamed `Switch` ([SettingsScreen.kt](app/src/main/java/com/example/androidapp/ui/settings/SettingsScreen.kt)),
  so TalkBack says "On, switch"; `BodyweightField` already shows the `.semantics { contentDescription = label }`
  fix. `SectionHeader` and the top-bar titles never call `heading()`, so no long screen can be navigated by
  section. Fixed means the switch label is the switch's own semantics (or the row is `toggleable` with
  `Role.Switch`) and `SectionHeader`/top-bar titles carry `heading()`; a Compose semantics test proves both.

- **B356 — A block's Skip must ask first or be undoable.** The one-tap `home_block_skip` button writes a skip
  that advances a sequence block permanently
  ([HomeRows.kt](app/src/main/java/com/example/androidapp/ui/home/HomeRows.kt)); there is no un-skip API, while
  removing a slot asks and deleting a measurement offers undo. Fixed means a confirmation that names the day
  and explains what advances (the shape of the slot-removal dialog), or an undo built on the row's existing
  `deletedAt`, plus the test that covers the chosen path.

### Backup lifecycle

- **B362 — Automatic backup needs a lifecycle, not only a schedule.** Old folder grants are never released
  when the folder changes ([BackupFolderPicker.kt](app/src/main/java/com/example/androidapp/ui/settings/BackupFolderPicker.kt)),
  so the app keeps access to every tree ever chosen until the system's persistable-grant limit is reached;
  changing folders does not reset the failure/period record, so a repaired backup can wait a full period; and
  there is no way to ask for one "now" after fixing a folder. Fixed means the previous grant is released, a
  folder change clears the failed-attempt state (or makes the next check due), and Settings offers a verified
  "Back up now". The write should be read back and parsed before its success is recorded, so "last backup" can
  mean "a restorable copy landed".

### Verification and tooling

- **N268 — The CI Gradle cache must not be redirected into the checkout.** `tools/android-env.sh` sets
  `GRADLE_USER_HOME=$ROOT/.toolchain/gradle-home` unconditionally, and `gradlew` sources the script whenever it
  exists, so on CI it bypasses the cache that `setup-gradle` restored; the README, workflow header and AGENTS
  all describe the script as a no-op without `.toolchain/`. Fixed means the variable is set only when
  `.toolchain/` exists or when it is unset, the no-op claim is made true, and a CI log shows the Gradle
  distribution coming from the restored cache.

- **N273 — The export/import ViewModel must be tested.** `DataTransferViewModel` is the only place that
  produces an export, confirms an import, clears everything and decides which outcome sentence the user sees;
  no test references it. Its collaborators are covered, but the orchestration B159 built to survive rotation
  is not. Fixed means a JVM test with fake `BackupRepository`/`DocumentIo` ports covering export-ready,
  export-failure, import-confirmed, import-cancelled, pending-import clearing and clear success/failure.

## Later — the rest of the 2.22 review

These rows are real and specified, but they are not the first batch: a defect here is low-frequency or a
polish item, an engineering row is not user-visible, and a feature is waiting for the hardening batch to land.
A row moves to *Next* when it is picked up; its design argument then moves to [DECISIONS.md](DECISIONS.md).
Nothing here is in flight.

### Defects and polish

- **B348 — The restore preview and the restore summary must agree about what became visible.** The summary
  counts a restored session-exercise or planned set whenever its own `deletedAt` is null, even when its parent
  stays hidden. B330 counts only rows the merge makes live, and N261 extended that honesty to sets the
  history cannot draw ([RoomBackupRepository.kt](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt));
  the summary still counts those children, and the preview does not count those kinds at all. Separately, the preview models measurement restores before
  inserts while the import runs inserts first, so a hand-made file with two live rows for one day+kind can be
  promised a restore the import will not perform. Fixed means one parent-aware counting rule used by both
  surfaces and one merge order modelled by both, with the same fixture read through preview and import.

- **B354–B355 — Messages the lifter can neither acknowledge nor see.** The library derives
  `lastTrainedError` from the read flow and `onErrorShown()` clears only `writeError`
  ([ExerciseLibraryViewModel.kt](app/src/main/java/com/example/androidapp/ui/exercises/ExerciseLibraryViewModel.kt)),
  so the snackbar cannot be dismissed and returns on every re-entry. Settings draws its write failure as the
  last child of a long scroll with no live region
  ([SettingsScreen.kt](app/src/main/java/com/example/androidapp/ui/settings/SettingsScreen.kt)), so a failure
  near the top is off-screen and unannounced. Fixed means an acknowledged-error state for the library and a
  snackbar or live-region-plus-scroll-to for Settings.

- **B357 — A tab root needs one back story.** Statistics correctly has no back arrow ("a root that offered
  back would be offering to go somewhere the user did not come from",
  [StatisticsScreen.kt](app/src/main/java/com/example/androidapp/ui/statistics/StatisticsScreen.kt)); History,
  Settings and Library are equally top-level tabs but draw Up that calls `popBackStack()` and therefore goes
  Home ([WorkoutHistoryScreen.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutHistoryScreen.kt)).
  Fixed means one rule applied to all roots, and the comment that argues it moved to the shared component
  rather than living on one screen.

- **B358 — The shell and the screens must not both apply the navigation-bar inset.** The shell reserves the
  tab bar, whose height already includes the system bar, and each screen's default `Scaffold` adds the system
  inset again ([AppNavHost.kt](app/src/main/java/com/example/androidapp/ui/navigation/AppNavHost.kt)); the
  deload screen adds a third `navigationBarsPadding()`. Fixed means the shell or the screens own the bottom
  inset exactly once, verified on a gesture-navigation and a three-button device (this was reasoned from code,
  not measured).

- **B360 — The set write boundary needs upper bounds as well as a floor.** `updateSet`/`logSet` clamp weights,
  assistance and bodyweight at zero but place no ceiling on reps or grams, even though `Weight.MAX_GRAMS` and
  `MAX_SET_SECONDS` exist ([RoomWorkoutRepository.kt](app/src/main/java/com/example/androidapp/data/RoomWorkoutRepository.kt)).
  The editor's parser keeps today's UI safe; a new caller or an import does not. Fixed means the boundary
  refuses a load above the app's maximum and an absurd rep count, with a test that calls the repository
  directly.

- **B361 — Crash logs must be bounded.** [CrashLogStore.kt](app/src/main/java/com/example/androidapp/platform/CrashLogStore.kt)
  writes one file per crash and never prunes; `latest()` reads and parses every file, and every export carries
  them all. A launch-time crash loop grows the directory for as long as it repeats. Fixed means the store keeps
  a bounded newest set (and can answer `latest()` from the filenames), with a test that records more than the
  cap.

- **B363 — A program edit must not write plan rows for a locally hidden movement.** The edit snapshot reads
  only live exercises, so an exercise that exists locally but is soft-deleted looks new; `writeExercises`
  returns `added.size` without checking ignored inserts, the planned row is written against the hidden id, and
  every plan read filters it out — invisible and unremovable, while the summary says it was added
  ([RoomProgramRepository.kt](app/src/main/java/com/example/androidapp/data/RoomProgramRepository.kt)). The
  program-add path already drops such rows. Fixed means counting actual inserts and refusing/dropping planned
  rows whose exercise is not live afterwards, with a test for the hidden-id case.

- **B364 — `restoreSet` must return a set to the place it held, not to an index later edits moved.** The undo
  derives its splice point from the live rows' stored indexes, but a removal renumbers the survivors and a later
  ramp addition or removal changes what those indexes mean, so Undo can put the set before a ramp or after its
  neighbour. Fixed means the splice point is stored on the hidden block or re-derived from the live order at
  removal time, with a test that adds a ramp between a delete and its undo.

- **B365 — Re-roling an anchor must re-check the rungs below it.** `updateSet` runs the rung rule only for the
  edited row, so changing a working anchor into a warm-up is accepted although logging refuses a rung under a
  warm-up and the rows below lose their anchor. Fixed means the role change re-runs the rule for the rows whose
  anchor is this set, or refuses a role that leaves a rung unanchored, with a test for both.

- **B366 — `setDailyWeight` must apply the same future-day refusal as `save`.** The bodyweight-loading writer
  accepts any instant while the measurement save refuses a day after today (B212). Today's callers pass today,
  so this is an inconsistency rather than a reachable write, but the two writers should share one boundary.
  Fixed means both call the same `isMeasurementDayAfterToday` check, with a test that the targeted writer
  refuses tomorrow.

- **B367 — A cancelled trends collection must not become a storage failure.** `observeExerciseTrends` catches
  `Throwable` and emits `DataResult.Failure` without rethrowing `CancellationException`, while its sibling flow
  does rethrow. A cancelled collection can therefore be reported as a failed read. Fixed means the catch
  rethrows cancellation first, with a test that cancels the collector.

- **B368 — A disabled primary control must say why it is disabled.** Finish with no set, and Log set with an
  incomplete field, are disabled and silent ([ActiveWorkoutScreen.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutScreen.kt));
  Templates already shows a reason line under a disabled Start. Fixed means the reason is visible (and
  announced) beside the control, or the control is absent until it can act, with a UI test for each state.

- **B369 — Every top-bar title must go through the one-line title component.** Several bars draw a raw `Text`
  with no `maxLines`/ellipsis, so a long user-authored exercise, template or program name can overflow the
  fixed-height bar and compete with up to three actions
  ([ExerciseDetailTopBar.kt](app/src/main/java/com/example/androidapp/ui/exercises/ExerciseDetailTopBar.kt)).
  `TopBarTitle` already exists for this. Fixed means every top bar uses it (or the same max-lines/ellipsis), with
  a UI test at a long name.

- **B370 — The adherence grid and small accent text must meet the app's own accessibility bar.** Seven weighted
  cells at 360 dp are about 43 dp wide, below the 48 dp target, and several 12–15 sp labels are drawn in
  `primary`, which the palette's own contrast test says is below 4.5:1 on the raised surface. Fixed means a
  minimum cell size (or horizontal scrolling) and the accessible accent used for small text, with the contrast
  test extended to the actual call sites.

- **B371 — The measurements deep link must keep its screen frame, and messages must centre inside a scroll.**
  When the daily form is requested before the list loads, the route returns a bare `CenteredMessage` with no
  Scaffold, title or back path; and `CenteredMessage` uses `fillMaxSize`, which does not centre under an
  infinite height constraint, so it sits at the top of a scrolling parent. Fixed means the loading state is the
  Scaffold's body and the message takes the parent's constraints (`fillParentMaxSize` in a lazy item, or out of
  the scroll), with a screen test for the loading state.

- **B372 — Search, filter and sort must survive process death.** The library, history and picker keep their
  query state in ViewModels, which survives rotation but not process death, while the rest of the screen is
  restored. Fixed means those scalars live in `SavedStateHandle`, with a test that recreates the ViewModel from a
  saved state.

- **B373 — The last hardcoded date and English strings must follow the app's formatting.** The statistics custom
  range builds a raw ISO date with a hardcoded separator, and a few user-facing sentences/fragments remain as
  literals in Kotlin. Localization is parked, but the date is visible next to `HistoryFormat` everywhere else.
  Fixed means the date goes through `HistoryFormat` and the literals become resources (or are justified as
  punctuation), with the existing string tests extended.

### Build, CI, tests and docs

- **N269 — The fast half of CI should run on pull requests.** The workflow is scheduled/dispatch-only, which is
  documented as deliberate, but it leaves Dependabot PRs and ordinary changes unverified until the nightly and
  makes branch protection impossible. Fixed means `build` + `docs` run on `pull_request` (and on `push` to
  main) while `instrumented` stays on the emulator schedule; the "Two jobs"/"always run" prose moves with it.

- **N270 — The R8 release must be executed by a test, not only built.** R8 shrinking is enabled and tested only
  by assembling an unsigned release; a release-only Hilt/serialization/navigation crash passes every gate.
  Fixed means CI builds a debug-signed release (ephemeral keystore), installs it on the emulator and runs a
  smoke navigation, or runs the instrumented suite against a release build type.

- **N271 — Test the shipping and minimum APIs, and correct the release note.** The app targets SDK 37, the
  emulator dispatch runs 36, Robolectric simulates 35, and `RELEASING.md` calls the dispatch "the shipping
  API". Fixed means a periodic API 26 and API 37 run (or a documented equivalent) and the sentence says which
  API each run actually covers.

- **N272 — Pin the supply chain.** The wrapper properties carry no `distributionSha256Sum` although the comment
  says the workflow validates it (it validates the JAR, not the distribution), and there is no dependency
  verification or locking. Fixed means the wrapper checksum is present and dependency verification metadata is
  committed and checked in CI.

- **N274 — Close the test gates that let a suite silently shrink.** `:shared` jvm/node tests are not
  count-gated; `app/schemas` has no CI freshness check (only `docs/shared` and `docs/fixtures` are checked);
  detekt scans main and unit tests but not the instrumented sources. Fixed means a `shared` suite in
  `ci-check-tests.py`, a `git status --porcelain -- app/schemas` step, and `androidTest` in the detekt source
  set. The same row pays down the deferred `TestTag` list (D4), which has no owner or deadline today.

- **N275 — Assert the manifest invariants the privacy claims rest on.** No test checks `allowBackup`,
  `dataExtractionRules`, exported components or permissions; the merged release manifest already carries
  androidx's signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, so "no permissions at all" is
  imprecise. Fixed means a source-manifest test plus a CI check of the merged permissions, and the README
  says "no dangerous/runtime permissions" (or explains the one).

- **N276 — Verify release monotonicity and tag binding.** `verify-release.py` compares the APK to
  `version.properties` but not to the previous tag, so a typo that lowers `versionCode` passes; the "tag points
  at the commit that built the APK" rule is procedural. Fixed means the script reads the latest tag's version
  and requires a strictly greater `versionCode`, the release notes record the built commit, and the
  keystore password is read from the environment or a file rather than passed on `keytool`'s argv.

- **N277 — The emulator timeout must exceed the boot time the workflow itself records.** The workflow's comment
  records a nightly boot of about sixteen and a half minutes while `emulator-boot-timeout` is 900 seconds, and
  another comment still says the hosted runner has no hardware acceleration although the KVM step below it
  exists. A recurrence of the recorded boot is killed before a test runs; the stale sentence misleads the next
  person debugging it. Fixed means the timeout is above the recorded worst case (or the post-KVM measurement is
  recorded and the value lowered deliberately), and the stale sentence is gone.

- **N278 — Artifacts need a retention policy.** The reports and the R8 mapping are uploaded with no
  `retention-days`; on a nightly workflow the default retention compounds a large mapping file and test
  reports. Fixed means a retention window per artifact, with the mapping attached to the release asset for
  permanent retention.

- **N279 — The docs job must run the same pinned Node as the build.** The build job pins Node 22; the docs job
  only echoes `node --version` and runs the helper's tests on whatever the runner ships, so the two jobs can
  test the same generated `docs/shared` on different majors. Fixed means the same `setup-node` version in both
  jobs.

- **N280 — Reconcile the prose with the code.** README's CI task list omits `:shared:detekt`; the workflow
  header says "Two jobs" while the file has three and says "always run" while nothing runs on push;
  `RELEASING.md` says "both jobs"; `.editorconfig` uses an inline comment EditorConfig does not parse;
  build-tools are not actually pinned in the build file; the privacy prose still says the export is the only
  path off the device (automatic backup writes a folder) and mentions progress photos the app does not hold;
  one XML comment still asks to revisit the platform-backup rules "when export/import lands", which it did.
  The docs test also still credits a removed `ci-check-instrumented.py`. Fixed means one docs pass, with
  `DocsConsistencyTest` extended to the README/RELEASING claims that drifted.

- **N281 — The test-only FileProvider should not ship.** The production manifest declares a `FileProvider`
  scoped to a cache directory only because `DocumentIoTest` needs a real content URI. Fixed means the provider
  moves to the instrumented manifest (or the test uses a test-local provider), and the release manifest no
  longer carries it.

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

Everything that has **left** *Next* has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
