# Workout — Roadmap

> **What is planned, not what happened.** *Next* is what is being worked on, *Later* is decided and waiting,
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

## Next

**The ten defects a reading of the unreleased batches found (B138–B147).** Each is something a change left
rather than a missing feature, and each is queued here because fixing it is a decision: an activity that cannot
be finished, a colour that can never be drawn, a goal's past read in the wrong order, a verdict that depends on
the chart's own range, two ways the plan's target line disagrees with the plan, the goal periods left outside
the import's transaction, a round-trip guard that was not extended, two tests that assert nothing, and the prose
and one imperative the changes forgot. Everything since the `v2.5` tag was read line by line, the instrumented
suite was run on a device (every declared test executed, the three new migrations among them), and the goal
store's ordering was reproduced by executing its own SQL.

This section holds only work a **reading or a use** turns up — a defect found by using the app, a question the log or
the plan can answer and the screen cannot, a batch's own batch-reading — and it is never filled to keep itself
full. An empty section is a state, not a gap: a row stands here while it is being done and leaves for
[CHANGELOG.md](CHANGELOG.md) when it ships, which is how this section drains.

- **An activity can never be finished** (B138). N141's one changed rule — "an activity finishes with **no
  sets**" — is not implemented where the rule is enforced. The top bar takes
  `canFinish = state.exercises.any { it.sets.isNotEmpty() }`
  ([ActiveWorkoutScreen.kt:229](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutScreen.kt#L229)),
  and *Finish* is the only control that reaches `onFinish` → `finishSession` (`:465`). An activity holds **no
  exercises at all**: [ActivityAddRows](app/src/main/java/com/example/androidapp/ui/workout/ActivitySection.kt#L88)
  returns before drawing anything once `activity != null` (`:94`), and the activity body replaces the list — so
  `state.exercises` stays empty and `canFinish` is false for the whole life of the session. The only way out is
  *Discard workout*, which **deletes** it: `finishedAt` is never written, and because the adherence read filters
  on `finishedAt IS NOT NULL` the activity reaches neither the history nor the calendar. N141 and N142 are
  unreachable end to end. Verified by adding `assertIsEnabled()` on `TestTags.ACTIVE_WORKOUT_FINISH` to a state
  of `activity = SWIMMING, exercises = emptyList()`: it fails, and the batch's own activity test
  ([ActiveWorkoutScreenTest.kt:812](app/src/test/java/com/example/androidapp/ui/workout/ActiveWorkoutScreenTest.kt#L812))
  asserts the picker and the absent add rows and never a finish. Fix: `canFinish = state.activity != null ||
  state.exercises.any { it.sets.isNotEmpty() }`.

- **The activity colour can never be drawn, and its state is not announced** (B139). N142's container exists
  ([AdherenceScreen.kt:519](app/src/main/java/com/example/androidapp/ui/adherence/AdherenceScreen.kt#L519)
  `day.activityOnly -> secondaryContainer`) but nothing can set the flag. The column was added to the **wrong
  query**: `activity AS activity` went into `sessionsStartedBetween`
  ([ProgramDao.kt:326](app/src/main/java/com/example/androidapp/data/local/ProgramDao.kt#L326)), whose row type
  `ProgramSessionRow` has no such field, while `finishedSessionsBetween` (`:360`) — the one query whose rows
  become an `AdherenceSession`
  ([RoomAdherenceRepository.kt:76](app/src/main/java/com/example/androidapp/data/RoomAdherenceRepository.kt#L76))
  — was left without it, and `FinishedSessionRow` gained the property
  ([ProgramRows.kt:63](app/src/main/java/com/example/androidapp/data/local/ProgramRows.kt#L63)). Room passes
  `null` for a nullable property a query does not return, with no error: the generated `ProgramDao_Impl.kt`
  constructs `FinishedSessionRow(…, null)`, and a forced KSP rerun says so (`FinishedSessionRow has some
  properties [activity] which are not returned by the query`; `The query returns some columns [activity] which
  are not used by ProgramSessionRow`). So
  [ProgramMappers.kt:84](app/src/main/java/com/example/androidapp/data/local/ProgramMappers.kt#L84)'s
  `isActivity = activity != null` is always false and
  [ProgramSchedule.kt:264](app/src/main/java/com/example/androidapp/domain/model/ProgramSchedule.kt#L264)'s
  `activityDays` is always empty — a swim-only day still paints in the workout colour, the exact reading N142
  exists to remove. The state is also drawn without being announced: the cell's description branches on
  `day.trained` first ([AdherenceScreen.kt:502](app/src/main/java/com/example/androidapp/ui/adherence/AdherenceScreen.kt#L502)),
  and an activity day is inside `trainedDays`, so it reads "trained" like a workout — the rule that
  accessibility announces a state rather than only drawing it. No test sees any of it (`activityDays`,
  `isActivity` and `activityOnly` appear in none). Fix: move `activity AS activity` into
  `finishedSessionsBetween`, and give the cell its own description. Rejected alternative: leaving the column on
  both queries — the unused one is what hid the mistake from the compiler.

- **A goal's pre-upgrade period sorts last, not first** (B140). After an upgrade the goal already on the device
  is a row with `startedAt = NULL`
  ([PreferencesSettingsRepository.kt:303](app/src/main/java/com/example/androidapp/data/PreferencesSettingsRepository.kt#L303)),
  and the settled reading is that it "sorts **before** every dated period of that metric"
  ([DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md), the move's own paragraph); the query's own KDoc says "An
  undated row leads"
  ([MetricGoalDao.kt:37](app/src/main/java/com/example/androidapp/data/local/MetricGoalDao.kt#L37)). The SQL is
  `ORDER BY (startedAt IS NULL) ASC, startedAt ASC, createdAt ASC`
  ([MetricGoalDao.kt:42](app/src/main/java/com/example/androidapp/data/local/MetricGoalDao.kt#L42)). In SQLite
  that expression is 1 for the undated row, so ASC puts the dated rows first and the undated row **last** — the
  opposite, and exactly the clause `observeCurrent` gets right by ordering `startedAt DESC`. Every reader
  assumes the other order. `RoomGoalRepository.openPeriod` takes
  `observePeriods(...).lastOrNull()` as the open period
  ([RoomGoalRepository.kt:60](app/src/main/java/com/example/androidapp/data/RoomGoalRepository.kt#L60)), so it
  compares a new goal against the **oldest** row: pressing *Set* twice with the same number inserts a duplicate
  period — the changelog's "opens nothing" breaks — and setting a goal equal to the legacy one returns Success
  with **no write at all**. `ChartProjection.segmentOf` hands the last period `to = last`
  ([ChartProjection.kt:97](app/src/main/java/com/example/androidapp/ui/statistics/ChartProjection.kt#L97)), so
  the legacy target is drawn across the whole chart, the claim N137 exists to stop; and
  [GoalOutcomes.readingsWithin](app/src/main/java/com/example/androidapp/ui/statistics/GoalOutcomes.kt#L74)
  gives it every reading. Run against SQLite with a dated `LOSE` row and an undated legacy row, the query returns
  `new, legacy` and `lastOrNull()` is `legacy`, while `observeCurrent` correctly returns `new`. The same inverted
  clause is in `allForBackup`
  ([MetricGoalDao.kt:53](app/src/main/java/com/example/androidapp/data/local/MetricGoalDao.kt#L53)), whose KDoc
  says "dated rows after the undated one" (harmless there — the import re-derives order by id). Fix: `ORDER BY
  (startedAt IS NULL) DESC, startedAt ASC, createdAt ASC` in both, and a test that seeds an undated row beside a
  dated one and asserts the order **and** the same-value guard.

- **A period's verdict is computed from the chart's own window** (B141). The screen asks
  `goalOutcomes(state.goalPeriods, series.readings)`
  ([StatisticsScreen.kt:239](app/src/main/java/com/example/androidapp/ui/statistics/StatisticsScreen.kt#L239)),
  and `series` is the reading list already clipped to the range chips
  ([StatisticsViewModel.kt:341](app/src/main/java/com/example/androidapp/ui/statistics/StatisticsViewModel.kt#L341)
  `inWindow` → `:361`), while `goalPeriods` is the whole all-time list (`:282`). `goalOutcomes`' contract is to
  clip the series to the **period**, and the settled rule is that a target is met when "a reading **in the
  period** reached it" — so a verdict about the past is made a function of a display setting. It is not a
  corner: the default range is `LAST_7_DAYS`
  ([StatisticsRange.kt:34](app/src/main/java/com/example/androidapp/domain/model/StatisticsRange.kt#L34)), seven
  dates hold at most seven daily weigh-ins, and a **rate** needs N98's floor of 8 readings across 21 days
  ([EnergyAdjustment.kt:42](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustment.kt#L42)) —
  so on the screen as it opens every direction goal reads *"Not enough weigh-ins"* forever, and a target reached
  three weeks ago but not this week reads *Missed*. Changing the chip changes the verdict. The history test
  cannot see it because it hand-builds a two-reading state
  ([StatisticsScreenTest.kt:808](app/src/test/java/com/example/androidapp/ui/statistics/StatisticsScreenTest.kt#L808)).
  Fix: judge from the unwindowed series — the ViewModel already reads it as `allReadings` — while the chart's own
  segments stay clipped.

- **A plan's target line reads a rep range's top where the session reads its floor** (B142). The plan-target read
  added with N39's chart half takes `COALESCE(s.targetRepsCurrent, s.targetRepsMax, s.targetRepsMin)`
  ([StatisticsDao.kt:90](app/src/main/java/com/example/androidapp/data/local/StatisticsDao.kt#L90)) — current,
  then the **top**, then the floor — while the app's settled reading of the same stored row is current, then the
  **floor**, then the ceiling, stated three times:
  [ProgressionOffer.kt:60](app/src/main/java/com/example/androidapp/domain/model/ProgressionOffer.kt#L60),
  [SetSuggestion.kt:191](app/src/main/java/com/example/androidapp/ui/workout/SetSuggestion.kt#L191) and the
  `TemplateSet` KDoc. The new [PlanTarget.kt:6](app/src/main/java/com/example/androidapp/domain/model/PlanTarget.kt#L6)
  KDoc was written to match the query instead of the rule, which is how the disagreement went unnoticed. For a
  live set prescribing 5–8 reps at 100 kg with no progression yet, the session asks for 5 reps and the chart's
  estimated-1RM line is drawn at Epley(100 kg, 8) ≈ 126.5 kg rather than Epley(100 kg, 5) ≈ 116.5 kg — a line
  the plan never asked for, beside the lifter's real data. Neither test can see it: the DAO test writes
  min = max = current, and `PlanTargetTest` builds the target by hand. Fix:
  `COALESCE(s.targetRepsCurrent, s.targetRepsMin, s.targetRepsMax)`, and correct the KDoc.

- **A plan's target line is dropped when the chosen lift is a family** (B143). The series reads a whole family
  through `subjectIds` when the metric accepts a head
  ([StatisticsViewModel.kt:241](app/src/main/java/com/example/androidapp/ui/statistics/StatisticsViewModel.kt#L241)),
  but the plan targets are queried for the single chosen id (`:292`), and the query filters
  `e.exerciseId = :exerciseId`
  ([StatisticsDao.kt:97](app/src/main/java/com/example/androidapp/data/local/StatisticsDao.kt#L97)). `RPE` is
  both level-shaped ([PlanTarget.kt:75](app/src/main/java/com/example/androidapp/domain/model/PlanTarget.kt#L75))
  and head-accepting
  ([MetricRegistry.kt:417](app/src/main/java/com/example/androidapp/ui/statistics/MetricRegistry.kt#L417)), and
  the lift picker offers a category for exactly those metrics — so choosing a category draws the family's
  readings with **no** plan line, though live plans prescribe RPE for the movements in it. The changelog's
  "every live plan that names the lift draws its own line" is not delivered for that selection, and no test
  covers a family selection. Fix: read the plan targets for the same `subjectIds` list the series uses.

- **The goal periods are the one import write outside the transaction, and the one set of rows the count
  misses** (B144). Goals are database rows now, and the settled rule says the import path's old seam goes
  "since a goal is a row that merges like every other"
  ([DECISIONS.md](DECISIONS.md), the goal-as-rows entry) — but `restoreGoalPeriods(file)` is still called **after**
  `database.withTransaction { … }`
  ([RoomBackupRepository.kt:183](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt#L183)),
  as the only write that is not atomic with the rest, and it returns `Unit`, so its rows never reach
  `ImportSummary`. A file carrying a goal beside sessions whose goal write fails leaves the sessions merged while
  `import` reports failure — the user is told nothing changed after their data changed. On the ordinary path, a
  restore whose only new content is goal periods reports `wasAlreadyComplete`
  ([BackupRepository.kt:69](app/src/main/java/com/example/androidapp/domain/repository/BackupRepository.kt#L69))
  and says "everything in that file was already here" although the goal was in fact restored. Fix: call it
  inside the transaction and count what it inserted.

- **The field-exhaustive round-trip guard was not extended** (B145). `BackupCodecRoundTripTest` exists because
  the codec is hand-written and "has silently dropped an unnamed column three times", and its stated contract is
  a fixture with **every field set to something distinctive**, "so the next column this app adds fails here
  instead of turning up missing in someone's backup"
  ([BackupCodecRoundTripTest.kt:30](app/src/test/java/com/example/androidapp/data/transfer/BackupCodecRoundTripTest.kt#L30)).
  The batch added three stored things — the session's `activity`
  ([BackupMappers.kt:154](app/src/main/java/com/example/androidapp/data/transfer/BackupMappers.kt#L154)), a
  template's `versionOf` (`:255`) and the whole `metric_goals` table
  ([GoalPeriodMappers.kt](app/src/main/java/com/example/androidapp/data/transfer/GoalPeriodMappers.kt)) — and
  extended none of it: the session fixture never passes an activity, and there is no `TemplateEntity` or
  `MetricGoalEntity` fixture at all
  ([BackupCodecRoundTripTest.kt:205](app/src/test/java/com/example/androidapp/data/transfer/BackupCodecRoundTripTest.kt#L205)).
  The mappers as shipped are correct, so nothing is lost today; the guard the repo relies on is what no longer
  covers them, and a dropped `activity` would restore every swim as an empty workout with the suite green. Fix:
  set `activity` in the session fixture and add the two missing fixtures. (The same gap is why the N142 defect
  above had no test to fail.)

- **Two of the migration tests assert nothing** (B146). `GoalMigrationTest.movingThemTwice_addsNothing` ends
  `assertTrue("and the first move's timestamp is the one that stands", true)`
  ([GoalMigrationTest.kt:96](app/src/androidTest/java/com/example/androidapp/data/local/GoalMigrationTest.kt#L96))
  — a literal, so the timestamp its own message names is never read; `INSERT OR REPLACE` in place of
  `INSERT OR IGNORE` would leave the count at 1 and the test green while the claim was false.
  `ActivityMigrationTest` asserts `cursor.getColumnIndex("activity") == 0` under the message "the start is
  unknown rather than invented"
  ([ActivityMigrationTest.kt:52](app/src/androidTest/java/com/example/androidapp/data/local/ActivityMigrationTest.kt#L52))
  — a column index in a projection the test itself wrote, carrying a message copied from the goal test about a
  different column, while the fixture's own `startedAt` is never asserted. Both are the shape B8 exists for: a
  green report that proves less than it says. Fix: assert the row's `createdAt` in the first and drop the second
  line (the `assertNull(getString(0))` below it is the real check).

- **The removals and insertions left their prose, and one imperative, behind** (B147). The comment above
  `DatabaseModule`'s migrations now says "there are **three** migrations" and then names five — N110, N113,
  N134, N137 and N141, all five registered
  ([DatabaseModule.kt:38](app/src/main/java/com/example/androidapp/di/DatabaseModule.kt#L38)). `onSkipRest` is
  documented as "Skips the prompt without writing anything" — the KDoc of the deleted `onDismissReadinessPrompt`
  ([ActiveWorkoutViewModel.kt:1048](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutViewModel.kt#L1048)).
  In [BackupFile.kt](app/src/main/java/com/example/androidapp/data/transfer/BackupFile.kt#L115) the `goals`
  field lost its KDoc to `goalPeriods`, which now carries two. In
  [HomeRows.kt](app/src/main/java/com/example/androidapp/ui/home/HomeRows.kt#L381) `WayInActions` lost its KDoc
  to `TodayActions`, which carries "Five callbacks" for a class with four. In
  [ActiveWorkoutScreen.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutScreen.kt#L289) the
  deleted `AddExerciseButton` still has its KDoc above `SetEditorSection`, and the readiness prose N140 removed
  the behaviour of still reads as if the prompt exists (`:780`, `:809`;
  [NoteDialogs.kt:95](app/src/main/java/com/example/androidapp/ui/components/NoteDialogs.kt#L95)). One of these
  is user-visible: the **read-only** version plan draws the editor's imperative "No planned sets yet. Add your
  first one." ([TemplateVersionUi.kt:246](app/src/main/java/com/example/androidapp/ui/templates/TemplateVersionUi.kt#L246))
  in a dialog whose only control is *Close*, where the other read-only plan preview already uses the neutral "No
  planned sets" ([strings.xml:181](app/src/main/res/values/strings.xml#L181)). Fix: one pass over the five files
  and the readiness sentences, and the neutral string in the version plan.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The last row here — a goal's past, with each period saying whether it was met — shipped, and the
section drains the same way *Next* does.

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
| — | **Play Store listing** | You want distribution beyond `adb install`. Self-install works today, and Play App Signing would change who holds the signing key. |
| — | **Encryption at rest / app lock** | You start carrying the phone somewhere you would not carry the data. |

## Explicit non-goals

Permanent, unlike *Parked*: no trigger brings one of these back.

- Nutrition / calorie tracking
- Social feeds, friends and shared routines (P3.7, P5.2)
- A web dashboard
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
