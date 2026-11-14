# Workout — Roadmap

> **What is planned, not what happened.** *Next* is the ordered batch, *Later* is decided and waiting, *Parked*
> is decided against until a named trigger fires, and the non-goals are lines this app does not cross.
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

## Next — the findings from the unreleased review

The review of the N304–N311 batch (unreleased at `6c4d9e0d`; the local unit/lint/detekt/assemble gate
green, the instrumented suite not run here) found defects that the batch's own rows did not cover. The
rows below are the fixes, ordered as the review ranked them: the must-fix group first, the smaller
findings after it, and the missing tests last as **N312**. Each row names the finding, the path and the
fix; when a row lands, its finding is gone from this file.

- **B421 — Find-the-weight cannot mark a plan with a drop run, and the deload can fail the same
  validation (must fix).** [RoomTemplateRepository.kt](app/src/main/java/com/example/androidapp/data/RoomTemplateRepository.kt)
  writes `makeWeightless` through `updateSetRow`, which re-runs `validateRun`; nulling the anchor makes
  `hasSomethingToTakeOff` false and the untouched rung reports "nothing to take off", rolling the whole
  write back. The plan editor's switch is unusable for any plan with a ladder. The same re-validation can
  refuse a deload whose three steps take a kept anchor to 0/below its drop value. Write the deliberate
  transition without the stranded-run check (or allow it there), and test both.
- **B422 — A calibration write failure traps a finished workout (must fix).**
  [CalibrationDialog.kt](app/src/main/java/com/example/androidapp/ui/workout/CalibrationDialog.kt) has
  `onDismissRequest = {}` and no confirm/dismiss buttons; both answers are writes, and a Failure only sets
  `lastError` ([ActiveWorkoutViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutViewModel.kt)).
  The session is already stored, so a persistent failure leaves a modal that system back cannot dismiss,
  with the error behind it. Show the error in the dialog and add a non-writing *Close* that advances
  `removeCalibration` (the marker stays pending).
- **B423 — History compares an older workout to a later one (must fix).**
  [WorkoutDetailViewModel.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailViewModel.kt)
  calls `previousPerformance`, whose query is `ORDER BY finishedAt DESC LIMIT 1` excluding only the viewed
  session ([WorkoutDao.kt](app/src/main/java/com/example/androidapp/data/local/WorkoutDao.kt)); any workout
  older than the movement's latest is compared to a *later* session. Bound the read by the viewed session's
  `finishedAt`; keep the prefill's unbounded read.
- **B424 — The history comparison ignores assistance (must fix).** `SetDelta`/`PastSet` carry only weight
  ([WorkoutDetailViewModel.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailViewModel.kt)),
  so an assisted set that got harder by dropping assistance reads "same as last time", and a
  weight↔assistance change is a wrong magnitude. Compare the signed load and carry assistance in `PastSet`.
- **B425 — Adopted/seeded assistance is stored as `0`, which disables load progression (must fix).**
  `calibrateExercise` ([RoomTemplateRepository.kt](app/src/main/java/com/example/androidapp/data/RoomTemplateRepository.kt))
  and the replace seed ([ExerciseReplace.kt](app/src/main/java/com/example/androidapp/domain/model/ExerciseReplace.kt))
  copy the session's non-null `0`; `ProgressionPlanSet.addedWeightGrams` requires the assistance to be
  null ([ProgressionOffer.kt](app/src/main/java/com/example/androidapp/domain/model/ProgressionOffer.kt)), so
  after Use or a seeded replace auto progression stops raising the load. Normalize `0` to null and assert it.
- **B426 — Find-the-weight behaves as a mode, so later plan saves wipe weights (must fix).**
  [TemplateEditorViewModel.kt](app/src/main/java/com/example/androidapp/ui/templates/TemplateEditorViewModel.kt)
  always re-runs `makeWeightless` while the switch is on, and `makeWeightless` clears every load: toggle on,
  save, add a weight, edit cue/rest/RPE, save — the weight is gone with no confirm or undo. The mark and the
  plan fields are two transactions (a second failure leaves the loads gone), and the `rememberSaveable`
  switch never re-syncs, so a stale ON can re-clear weights the finish adopted. Write the mark only when it
  changes, fold both writes into one transaction, and re-sync the draft to the stored flag.
- **B427 — The finish's volume gate cannot see the session that completes a stall (must fix).**
  [PlanVolumeFinder.kt](app/src/main/java/com/example/androidapp/domain/PlanVolumeFinder.kt) builds the
  Done history from finished sessions only and `isOpen` excludes the current session, so a third missed
  session can be offered "Add a set" while the stall sheet says it stalled; the card, read after finish,
  disagrees. Pass `previous + current` to `isOpen`. The stall gate test
  ([PlanVolumeFinderTest.kt](app/src/test/java/com/example/androidapp/domain/PlanVolumeFinderTest.kt)) is
  vacuous today (its all-missed case has no trigger); rebuild it with a trigger so deleting the gate fails.
- **B428 — A plan naming the same movement twice resolves to the wrong row (must fix).** The N54 duplicate
  shape is supported, but [ActiveWorkoutViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutViewModel.kt)
  matches a marked plan entry by `exerciseId` with no occurrence, and
  [PlanVolumeFinder.kt](app/src/main/java/com/example/androidapp/domain/PlanVolumeFinder.kt) returns the
  first plan row. The second occurrence's calibration can adopt the first row's performed numbers, and a
  volume Add can append to the first plan row. Resolve by occurrence like `planEntriesFor`, and pass the
  resolved entry id.
- **B429 — The volume finder rebuilds the stall history from prompt pairs (must fix).** The prompt pairs
  drop extra performed sets and compact missing ones before `PrescriptionHistory.stall()` re-pairs them,
  so the volume gate can disagree with the stall card about which set was answered. It also counts sessions
  that did not train the movement, so a vacation/substitution breaks both volume streaks (the stall rule's
  "absence is neither" rule). Use the raw `TemplateSession.performed` sets and skip sessions with no
  performed exercise, as the stall finder does.
- **B430 — Undoing Done leaves the volume suggestion live (must fix).** `onReopenExercise` clears only the
  finish undo, so after the lifter takes the Done back, the queued "Add a set?" still writes plan work for a
  retracted finish. Clear the suggestion on reopen, finish and close, and cover the pairing in a VM test.
- **B431 — The new migration test should fail the instrumented job (must fix, instrumented).**
  [CalibrationMigrationTest.kt](app/src/androidTest/java/com/example/androidapp/data/local/CalibrationMigrationTest.kt)
  inserts a `template_exercises` row with no `templates`/`exercises` parents while Room runs
  `PRAGMA foreign_keys = ON`, so the insert throws before the default is checked; its
  `ALTER TABLE … DROP COLUMN` also needs SQLite 3.35 (API 31+), so an API 26–30 device fails. Create the
  parents or use `MigrationTestHelper` with the committed schema 24; avoid `DROP COLUMN` for portability.
  The per-change gate never runs this; the nightly/dispatch instrumented job is where it bites.
- **B432 — The deload undo is not one transaction (minor).** The plan screen restores the removed sets and
  then writes each kept target separately, with the undo handle dropped first; a failure or process death
  between them leaves a partial undo. Add a repository `undoDeload(removedIds, before)` in one transaction.
- **B433 — A weighted hold is deloaded by the weight path (confirm).** `deloadTarget` does not exclude a
  timed set from the weight branch, while the [ExerciseDeload.kt](app/src/main/java/com/example/androidapp/domain/model/ExerciseDeload.kt)
  doc says a hold is unchanged (the CHANGELOG says only "a hold's seconds"). Decide which is intended and
  make code and docs agree.
- **B434 — A seeded replace bypasses the run validation (minor).** The new plan is inserted with
  `setDao.insertTemplateSet` without `validateRun`, so a new movement much lighter than the old drop value
  can leave a stranded rung. Validate the written plan or clamp/clear the ladder, and test a low seed.
- **B435 — A marked timed movement's blank weight becomes 0 kg (minor).** The blank prefill is turned into
  `Load(0,0)` for a timed set ([SetEntryFields.kt](app/src/main/java/com/example/androidapp/ui/components/SetEntryFields.kt)),
  so Log is enabled at 0 and Use calibrates 0 — for a loaded carry this defeats find-the-weight. Carry
  `weightKnown` into `values`/`isComplete`.
- **B436 — The appended-set lookup is a race and is triplicated (minor).** The three VMs read the plan back
  via `maxBy(setIndex)` after `addSet`; two Adds in flight can both resolve to the second set (undo removes
  the wrong one), a failed/null read silently drops the undo, and the same ten lines live in three places.
  Return the inserted id from `addSet` in its transaction and delete the lookups.
- **B437 — The Done's "current" prompt is rebuilt after the progression write (minor).** `suggestVolume`
  runs after the step/auto write and passes `row.progression` rebuilt from the moved plan; a load step makes
  the performed set LOAD_SHORT, so the comfortable trigger can be lost. Capture the prompt at Done and pass
  it through.
- **B438 — Add/undo state races (minor).** The volume suggestion is cleared unconditionally on completion (a
  suggestion that arrived meanwhile is dropped), with no in-flight guard (a config change re-arms the effect
  and a second Add appends twice while the undo tracks only the last), and a failed `addSet` leaves the
  suggestion live with no snackbar. Clear by identity, guard in flight, and clear on Failure.
- **B439 — Advisory failures are handled as fatal on the plan surfaces (minor).** The weekly review replaces
  the whole week with the error view on a failed stall/volume read and on a failed Add; the program editor's
  finder failure is re-derived from the state combine, so `onErrorShown()` re-emits the same Failure and the
  snackbar loops. Keep advisory read/Add failures beside their section, and deliver finder failures once.
- **B440 — A program-document edit silently clears the marker (minor).** The import applies edited rows with
  a full-row update from a DTO defaulting the marker to false
  ([ProgramImportWrite.kt](app/src/main/java/com/example/androidapp/data/ProgramImportWrite.kt)), so
  changing a plan through a document leaves the loads null but drops the marker — the next session prefills
  from history instead of asking. Exclude the column or merge the live value; test it.
- **B441 — Migration 24→25's SQL default disagrees with the entity and schema (minor).**
  [CalibrationMigrations.kt](app/src/main/java/com/example/androidapp/data/local/CalibrationMigrations.kt)
  adds `DEFAULT 0`; [TemplateExerciseEntity.kt](app/src/main/java/com/example/androidapp/data/local/TemplateExerciseEntity.kt)
  and `25.json` declare none, so fresh and upgraded installs differ (B193). Add
  `@ColumnInfo(defaultValue = "0")` and regenerate schema 25 (still unreleased).
- **B442 — The finish's "just reached three" notice can reappear (minor).** The filter is a count only;
  because the stall rule skips sessions that trained no prescribed set, a run of three survives a skipped
  lift and the next workout of that template (which does not train it) shows the notice again. Require the
  finished session to have logged the movement, or carry the run's newest session id.
- **B443 — The new controls miss the app's accessibility/UI conventions (minor).** The volume card prints
  the exercise name twice ([PlanVolumeCard.kt](app/src/main/java/com/example/androidapp/ui/programs/PlanVolumeCard.kt));
  the find-the-weight switch has no accessible name
  ([TemplateExerciseRow.kt](app/src/main/java/com/example/androidapp/ui/templates/TemplateExerciseRow.kt));
  and the multi-lift calibration/stall dialogs use one identical tag and label per action for every lift, so
  TalkBack repeats and a test cannot address one row. Name the exercise on the control and use per-row tags.
- **B444 — The docs/schema story lag the code (minor).** DECISIONS N305 still says "information, not a
  decision" without the N310 amendment, and `WorkoutStallDialogTest`'s KDoc repeats it while the test taps
  Deload/Replace; the `WorkoutDatabase` schema story stops at 24
  ([WorkoutDatabase.kt](app/src/main/java/com/example/androidapp/data/local/WorkoutDatabase.kt)). Note N305 as
  amended, fix the test KDoc, and add the 25 paragraph.
- **N312 — Cover the batch's untested seams.** No ViewModel test touches `calibrationNotices`,
  Use/Skip or the calibration→stall ordering; none touches `suggestVolume`/`onAddVolume`/`onUndoVolume` or
  the editor/weekly add+undo (the fakes' `addSet`/`removeSet` still throw); the two finders' card paths
  (`suggestionsForProgram`/`suggestionsForTemplate`, `stallsForProgram`, failure paths) are unasserted; the
  backup round-trip fixture never sets the marker true, and no test asserts the shared document omits it; the
  N310 hand-off's `AppNavHost` mapping is untested. Also missing: the two-lift screen test's second button,
  the find-the-weight switch's true path reaching `onSaveExercisePlan`, the weekly selection order, the
  marked-substitute/duplicate-movement/timed cases, undo-of-Done plus a pending suggestion, and the
  instrumented depth for deload/replace/calibrate (rungs, warm-ups, assistance, Empty, cues, hold/range,
  substituted/short/extra sets) and for `makeWeightless` on a drop run.

The review also verified the areas those rows do not touch: the finish ordering (calibration before the
stall), the by-place pairing and substitution rules, the N310 hand-off (one-shot args, `popUpTo<ActiveWorkout>`),
the volume cap/crossings, the schema 25 wiring and the shared-document exclusion, the dynamic CI test count,
and the CHANGELOG/DECISIONS/roadmap state. Those need no rows.

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
