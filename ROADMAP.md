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

## Next — the unreleased-changes review

Everything since 2.31 was read as one batch — the auto-progression fixes (B472–B477), the rep-range line
(B478), the session-plan snapshot (B471), N325, and the weight-goal/calorie batch (B479–B486, N327, N328) —
rather than as the separate reviews that produced them. The earlier rows were read in isolation; this read is
the seams between them. The auto-progression writes, the snapshot schema and the goal/calorie arithmetic
hold. The findings are the edges the fixes left: a block that can still be switched on, a secondary read
that can take away the off control, a double *Done* after *Not now*, a legacy boundary whose range lies, and
three backup/snapshot edges. Nothing was fixed during the read; the rows are ordered by what they cost.

- **B492 — A legacy clock-time boundary makes the history's range lie.** N328 subtracts a day from the next
  start's date, but `GoalOutcomes.readingsWithin`
  ([GoalOutcomes.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalOutcomes.kt)) clips by the
  *instant*: a period whose next boundary is 08:00 owns that morning's weigh-in, while the range in
  [GoalRow.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalRow.kt) says it ended the day
  before. The N328 test's own periods are 08:00, so it pins the shape without a boundary-morning reading.
  Fixed means the last day is derived from the instant (subtract only for a local-midnight boundary, or show
  the time), with a test whose reading sits on the boundary morning.
- **B489 — The backup preview does not count the plan snapshots.** The import writes `sessionPlanSets` and
  counts them in its summary (B471), but
  [BackupRepository.kt](app/src/main/java/com/example/androidapp/domain/repository/BackupRepository.kt)'s
  `BackupPreview` has no field for them and
  [RoomBackupRepository.kt](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt)'s
  `preview()` never reads them. A file whose only new rows are snapshots previews as no change, then the
  restore reports one added. The preview's four kinds are the question it answers, but a collection the merge
  now writes belongs in it. Fixed means a snapshot count (or the session count that contains them) and a
  snapshot-only test.
- **B490 — The switch is cleared without its baseline on the B475 paths.** B477 made disabling drop the
  baseline in `RoomProgressionRepository.setAutoProgression`, but converting to a block
  ([RoomProgramRepository.kt](app/src/main/java/com/example/androidapp/data/RoomProgramRepository.kt)),
  deleting ([ProgramDao.kt](app/src/main/java/com/example/androidapp/data/local/ProgramDao.kt)), restoring a
  document ([ProgramBackupDao.kt](app/src/main/java/com/example/androidapp/data/local/ProgramBackupDao.kt))
  and importing a block
  ([ProgramBackupMappers.kt](app/src/main/java/com/example/androidapp/data/transfer/ProgramBackupMappers.kt))
  clear only the flag. The rows stay hidden and exported, and "off drops it" is true on one of the four off
  paths. Fixed means those paths clear the baseline too (or go through the one repository method), with tests
  for each.
- **B493 — Duplicate plan snapshot rows are dropped in silence.** `session_plan_sets` is keyed by
  `(sessionExerciseId,setIndex)` and the backup import inserts with IGNORE, but
  [BackupValidator.kt](app/src/main/java/com/example/androidapp/data/transfer/BackupValidator.kt)'s
  `validateSessionPlanSets` never checks the pair for a duplicate. A hand-edited or corrupt file loses one
  row without a problem, and the one kept may be the wrong plan. The other tables' ids have the same class of
  check. Fixed means the validator refuses a duplicate pair, with the import tested.

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
