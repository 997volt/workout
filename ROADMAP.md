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
