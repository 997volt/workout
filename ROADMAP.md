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

## Next — the weight goal and calorie review

The weight goal (N135, N137, N149, N151, N215) and the energy adjustment it feeds (N98, N136) were read
against those decisions: the kind and the rate derived from it, the period store and its editor/history,
the target date and the reached state, the kcal sentence on Statistics and the prompt after a weigh-in, and
the backup. The model is sound — a kind rather than a second number, days rather than moments, a relative
adjustment against intake the app deliberately never stores — and the rate arithmetic is right. The
findings are at the edges: a reached target that still reads as a rate to chase, maintenance that speaks
below N98's floor, a zero weight accepted as a goal, an open period judged as missed, and a backup that can
restore rows the write path would refuse. Nothing was fixed during the read; the rows are ordered by what
they cost.

### Before 2.32

- **B479 — A reached target still reads as a rate to chase.** `isTargetReached` gives a target the goal
  history's 1 kg tolerance, but [WeightGoal.kt](app/src/main/java/com/example/androidapp/domain/model/WeightGoal.kt)'s
  `directionFrom` calls it met only when the two weights are equal. A target of 80.5 kg against 80.0 kg is
  therefore *reached* and *a gain goal* at once: `ratePerWeekGrams` returns 280 g a week,
  [StatisticsViewModel.kt](app/src/main/java/com/example/androidapp/ui/statistics/StatisticsViewModel.kt)
  passes the rate and band beside `targetReached = true`, and
  [EnergyAdjustmentSection.kt](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustmentSection.kt)
  prints "Target reached" over "On target between +0.2 and +0.4 kg per week" and an "eat more" sentence. The
  same rate reaches [MeasurementsViewModel.kt](app/src/main/java/com/example/androidapp/ui/measurements/MeasurementsViewModel.kt)'s
  weigh-in path, so the unsolicited prompt after a weigh-in at the target tells the lifter to eat more. The
  tests stop short of the contradiction: the domain case only checks the date, and
  [StatisticsScreenTest.kt](app/src/test/java/com/example/androidapp/ui/statistics/StatisticsScreenTest.kt)
  sets `targetReached` without the rate the view model really passes. Fixed means a reached target has no
  rate band and no calorie sentence or weigh-in prompt — the row says reached and stops — with tests for a
  target inside the tolerance, one exactly on it, the screen with its production state, and the weigh-in.
- **B480 — Maintenance quotes a calorie change below the floor, and judges a corridor from one weigh-in.**
  [EnergyAdjustmentSection.kt](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustmentSection.kt)'s
  maintenance branch calls `energyAdjustmentToCorridor` directly; that function never checks
  `hasEnoughToSpeak`, so one reading produces a kcal sentence. The weigh-in path in
  [EnergyAdjustment.kt](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustment.kt) checks the
  floor first and says nothing, so the two surfaces answer the same question differently.
  [GoalOutcomes.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalOutcomes.kt)'s
  `stayedInCorridor` likewise falls back to a one-reading check, so a maintenance period with one weigh-in is
  Met or Missed while `GoalOutcome.Unjudged` promises that too little recorded is not a verdict. The tests
  only cover three-reading corridors and the rate branch's floor. Fixed means the section applies N98's
  8-readings-over-21-days floor to maintenance, a corridor period with no fitted line is Unjudged, and tests
  cover one and two readings on both surfaces.
- **B481 — A weight goal of zero is accepted, and the calorie statement inherits it.** `MetricUnit.parse`
  accepts zero ("finite and not negative"), `weightGoalFor` in
  [GoalRow.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalRow.kt) asks only for a parse, and
  [RoomGoalRepository.kt](app/src/main/java/com/example/androidapp/data/RoomGoalRepository.kt) and
  [BackupValidator.kt](app/src/main/java/com/example/androidapp/data/transfer/BackupValidator.kt) accept any
  non-negative value. A target of 0 kg is an 80 kg lifter's goal 80 kg away with a multi-year date; a
  maintenance goal of 0 kg makes the corridor's point gap about 79 kg, and
  [EnergyAdjustment.kt](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustment.kt)'s corridor
  arithmetic quotes about 87,000 kcal a day. The body field on the same screen already treats zero as "not a
  body" (B336); the goal field does not. The tests cover the kind round trip and a missing value, never zero.
  Fixed means a weight goal must be positive at the editor, the repository and the backup, with tests; a
  typed 0 is refused and a file carrying one is refused rather than restored.
- **B482 — A large maintenance gap is quoted as one week's calorie change.**
  `energyAdjustmentToCorridor` multiplies the whole gap by the same 1.1 kcal a day per gram a week (B335).
  1.2 kg outside the corridor is 1,320 kcal — the test currently asserts that — and 10 kg is 11,000, in a
  sentence that says "about X kcal a day less than you are eating now". The rate branch cannot reach those
  figures because the band bounds the target rate; the corridor branch has no bound and no second pace to
  say "this is more than a week's change". Fixed means the sentence stays useful for a multi-kilogram gap —
  a cap, a different wording that names the longer horizon, or both — with a test for the large gap.
- **B483 — The goal still in force is reported as "Missed".** `goalOutcomes` runs the last period to the end
  of the series and gives it the same Met/Missed verdict as a closed one;
  [GoalRow.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalRow.kt)'s history lists that open
  period, whose range says "From …". A target set two days ago and not yet reached reads "Missed", and a
  direction goal with enough history and one week off its band reads "Missed" while the lifter is still in
  it. `GoalOutcome` has no in-progress state, and the tests only judge a current period that is Met. Fixed
  means the open period has its own status ("so far" / "in progress") or is not given a final verdict; tests
  for a current target, direction and maintenance.
- **B484 — The backup checks a goal's kind and value separately, not against each other.**
  `validateGoals` in [BackupValidator.kt](app/src/main/java/com/example/androidapp/data/transfer/BackupValidator.kt)
  accepts any known kind and any finite non-negative value. A TARGET or MAINTAIN with no value is restored as
  a row that reads as no goal; a LOSE or GAIN with a value keeps the value in the row but drops it on read.
  When a file carries both `goalPeriods` and the older `goals`/`goalKinds` halves, the periods win and the
  halves are not checked against them. The app's own writes cannot produce those shapes, so this is a
  hand-edited file or a future build's row — exactly what a validator is for. Fixed means a value kind
  requires a positive value (B481), a direction kind requires none, and the two halves agree when both are
  present; tests for each refusal.
- **B485 — A restore can create a goal boundary the write path forbids.** `restoreGoalPeriods` in
  [RoomBackupRepository.kt](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt) inserts
  every `goalPeriods` row with IGNORE-by-id. It does not refuse a start in the future, a second undated row
  for one metric, or a second row on a day the device already has under another id.
  [RoomGoalRepository.kt](app/src/main/java/com/example/androidapp/data/RoomGoalRepository.kt) and
  [GoalTimeline.kt](app/src/main/java/com/example/androidapp/data/local/GoalTimeline.kt) assume at most one
  boundary per day: two rows on one day make `observeCurrent` choose by `createdAt`, leave a row the day
  rules cannot move, and show two overlapping stretches in the history; a future start becomes the current
  goal before its time. The round-trip test restores into a cleared database, so it never meets the existing
  day. Fixed means the validator refuses a future or duplicate start, the import merges a same-day row
  instead of laying a second, and tests cover a restore onto a device that already has that day.
- **B486 — The calorie section says "Set a goal" without a way to set one.** The weight goal is edited on
  Measurements; [StatisticsScreen.kt](app/src/main/java/com/example/androidapp/ui/statistics/StatisticsScreen.kt)
  hides `GoalRow` for the weight metric, and
  [EnergyAdjustmentSection.kt](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustmentSection.kt)
  renders the target as a plain `Text`. A lifter on the weight chart reads "Set a goal" as a control and
  cannot press it; the section's KDoc still says "the goal row above is where the number that sets it is
  chosen", and the row it names is not on this screen. Fixed means the target row opens the goal editor or
  navigates to Measurements, or the goal row returns here; a screen test that the control saves a goal, and
  copy that matches the screen.

### Coverage

- **N327 — The goal-history reader is weight-only under a generic name.**
  [GoalOutcomes.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalOutcomes.kt)'s `goalOutcomes`
  takes readings but always builds a weight `MetricSeries` and compares a target with the weight tolerance
  (1,000 grams) and a rate with the weight band. Its only caller is the weight's
  [MeasurementsViewModel.kt](app/src/main/java/com/example/androidapp/ui/measurements/MeasurementsViewModel.kt),
  so nothing is wrong on screen today; the repository and the non-weight `GoalRow` already support periods,
  and the first non-weight history would read a 1 m tolerance on a waist in millimetres. Fixed means the
  reader takes the metric's key and tolerance, or is named and tested as weight-only; a test for the
  non-weight case or an API that cannot be called with one.
- **N328 — The history prints the next period's start as this period's end.** A period runs to the next
  boundary, exclusive; [GoalRow.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalRow.kt)'s
  history row prints the next start as the second date (`"%1$s – %2$s"`), and the editor's "Until %1$s"
  names the same day. A weigh-in on that day belongs to the next goal, not the one whose range appears to
  include it. Fixed means the labels say "until" or show the last day the period owns, and a test with a
  reading on the boundary.

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
