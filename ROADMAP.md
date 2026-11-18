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
