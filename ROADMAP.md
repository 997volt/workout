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

**Nothing.** The statistics pass this queue held (N196–N201) is in [CHANGELOG.md](CHANGELOG.md); N202 left the
plan before it started, because the readings list has been folded behind its count since N36.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**What the statistics pass left open, and what it made visible.** Rows N203–N206 finish the pass's own loose ends; N207 is
the structural debt it exposed; N208–N213 are improvements found while building it. Nothing here is started.

| # | What it changes | Brought forward when |
| --- | --- | --- |
| N203 | **A marker on the figure for the scrub** — a vertical line and an emphasised dot at the reading the readout names, instead of naming it below the figure and leaving the line unpointed. | Reading the value means looking away from the point it belongs to often enough to grate. |
| N204 | **The expanded figure carries what the card carries** — the dates, the plan targets' names and the moving-average picker — so the two readings of one series cannot disagree. | The expanded view is opened to read a target rather than the line. |
| N205 | **The joint sites group under their joint** (eight headings, three sides each) instead of one list of twenty-four. | A site is picked from the list rather than found by searching its name. |
| N206 | **The readout is announced and the expanded view named** — a live region on the scrub's text, a title on the dialog — so the pass's one touch-only affordance has an accessible counterpart. | The next accessibility pass over statistics, or a TalkBack user on it first. |
| N207 | **`ui/statistics` splits into a chart package.** The screen is 877 lines, hit the 20-function ceiling three times in one pass, and the chart now spans four files. A refactor with no behaviour change, which is why it lands alone. | The next change that adds a function to `StatisticsScreen.kt`. |
| N208 | **The overview's three numbers leave the row above the figure** — one line under the title, or below the chart — giving the figure back the space it occupies. | The figure is opened on a small phone and is still the smallest thing on the screen. |
| N209 | **The x axis carries ticks between its ends** — month or week, whatever the range is — rather than only the first and last date. | A range is long enough that its two end dates are the only time on the figure. |
| N210 | **The range is remembered across visits**, as the weight unit is. | A visit's first act is re-choosing the range it was left at. |
| N211 | **The moving-average picker folds into a menu**, like the range's *More*, leaving the card to the figure and its axis. | The 7/14/30 row costs the figure height it needs on a phone. |
| N212 | **A short "recently used" row above the metric trigger**, now that groups and search have made the menu navigable. | The same two or three series are what actually gets read. |
| N213 | **Pinch changes the window**, the gesture a chart on a phone already invites; it needs a policy for pinching past the data before it can land. | Re-choosing the range by hand to zoom is what people do. |

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
