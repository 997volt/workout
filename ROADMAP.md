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

## Next — running one program for years

The 2.29 work made one program usable indefinitely; what is left is what the app says *around* each
session. Finishing currently ends on a review that repeats what history already reads back, and the one
number it exists for compares the session to a plan that auto progression may already have moved — the
least reliable number at the least useful moment. The one thing a lifter cannot see from the session
itself is a lift that has stopped moving, so that is what the finish should say.

The rows are one shift of emphasis: the finish says less, history says more, and the stall names
the exercise rather than one of its sets.

- **N305 — The finish says only what stalled.** The review dialog goes. Totals, ratings, readiness and
  the comment are already read back in the workout detail, and the plan-vs-actual comparison it exists
  for is deleted with it rather than moved to history: keeping it honest would mean storing the
  prescription with the session, and that schema was judged not worth a number that changes nothing
  about the next session. The **note prompt stays** — it is the one input the finish owns. When the
  workout came from an **active weekly** program's template and that template has a stalled exercise
  after this session, a compact dialog takes the review's place: the stalled lifts in workout order,
  one line each, and a single *Done*. It appears when the run **just reaches three**, not after every
  session it stays stuck — the program screen and weekly review already carry it. Otherwise finishing
  closes the screen. [WorkoutReviewDialog.kt](app/src/main/java/com/example/androidapp/ui/workout/WorkoutReviewDialog.kt),
  `WorkoutReview`/`buildSummary`, [PlanComparison.kt](app/src/main/java/com/example/androidapp/domain/model/PlanComparison.kt)
  and `comparePlanToActual` are deleted with their tests, and DECISIONS' N20 ("the review is a moment")
  and N131 (the mean-effort comparison) are amended in this change rather than left describing code
  that no longer exists. The finish needs two reads it does not have: the active weekly program naming
  the finished template, and a template-scoped stall list instead of the program's first one.

- **N306 — History compares the session to the last one, not to the plan.** The workout detail keeps the
  performed sets and gains, per exercise, the **previous session that trained the movement** with a
  per-set delta against the same place in it — the by-place rule the workout prefill already uses
  (N168), read through
  [WorkoutRepository.previousPerformance](app/src/main/java/com/example/androidapp/domain/repository/WorkoutRepository.kt)
  with the viewed session excluded. No schema: the previous session is a read, not a snapshot. A
  movement with no earlier session says nothing rather than zero, a set with no counterpart in the
  previous session is not compared, and the delta is stated in the set's own unit and shape (a hold in
  seconds); because history is editable, it is re-derived when a set changes. It replaces the
  comparison N305 removes, and is the only comparison the app makes — against what was actually done,
  not against what a living plan says today.

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
