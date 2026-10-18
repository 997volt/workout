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

**The N150/N151/N152 review's remaining findings (B169, B170, N153–N155).** The wrong-output reuse (B168), the
duplicated tolerance (B171) and the reached-target redundancy (B172) are fixed; the rest are a boundary, a test gap
and three product limits.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| B169 | low | **`findLastPlan` is outside the errors-as-values boundary.** It returns a bare `DeloadWeek?` and can throw; the loader's `dataResultOf` catches it today, so nothing reaches the screen, but a future caller can miss the boundary. | Return `DataResult<DeloadWeek?>` or document the read as loader-internal. |
| B170 | low | **The 2-of-4 pool path has no VM or screen test.** The pure builder proves a two-day deload can keep lifts from the third/fourth source days, but the `onToggleExercise(dayIndex, exerciseId)` wiring and the per-day state are untested for that case. | Add a VM test storing a third-day lift on the first deload day, and a screen test for both day cards. |
| N153 | low | **A movement on two source days takes the first source's plan.** The pool keeps the first instance, so the deload's sets, reps and load come from whichever source day the program lists first; the lifter cannot choose the heavier day's plan. The evidence records the choice; the UI does not. | Decide deliberately: the heaviest source, a source choice, or surface the first-source rule in the pool row. |
| N154 | low | **The reuse restores exercises and day count but not weekdays.** The last plan's day weekdays are not applied; the hint says exercises and day count, and the original N150 ask mentioned days/weekdays. | Apply the last plan's weekdays by day index, or leave it as the stated limit. |
| N155 | low | **The last plan's choices are read from the current deload templates.** Editing a deload template changes what a later *Use last deload* believes the last choices were; the plan stores the templates, not the choices. | Store the chosen exercise ids with the plan, or document the living-template behaviour (N16). |

This section holds only work a **reading or a use** turns up — a defect found by using the app, a question the log or
the plan can answer and the screen cannot, a batch's own batch-reading — and it is never filled to keep itself
full. An empty section is a state, not a gap: a row stands here while it is being done and leaves for
[CHANGELOG.md](CHANGELOG.md) when it ships, which is how this section drains.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The deload creator (N147) shipped, and the section drains the same way *Next* does.

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
