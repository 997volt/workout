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

**The rest of the post-2.16 review.** The statistics reorder (N221/N222), the figure's size, scale and clip
(B259–B261), the tab button that floors its own tab (N223) and the plan and progression fixes (B257, B258) are in;
reading them against the code turned up the rows below. The figure's new size crosses the expanded one on a short
window (B263), and the tab button has wiring and a test gap behind it (B264, B265). The gate is green — unit tests,
the shared JVM and JS tests, lint, detekt, the debug and R8 assemblies — so these are wiring and arithmetic the
tests do not reach, not build failures.

| # | What it changes |
| --- | --- |
| B263 | **The inline figure is the bigger one on a short window.** `inlineFigureHeight` floats at a 220 dp floor while `expandedFigureHeight` is 55 % of the window, so below 400 dp the expanded view is the *smaller* read — 198 dp against 220 at 360 dp — and *Expand* shrinks the figure, the opposite of what the B261 entry and `ChartAxis`'s own KDoc promise ("the expanded view stays the larger of the two, which is what makes *Expand* mean something"). The activity locks no orientation, so landscape on a 360 dp-wide phone and split-screen both reach it, and there the floor also takes 61 % of the window — the squeeze the floor exists to prevent. Bound the floor against the expanded height rather than against a constant, and start the test at 320–400 dp instead of 480: its comment that "the smallest window here is already larger than any phone this app runs on" is not true. |
| B264 | **N223's host wiring has no test.** `AppNavHost` reconciles the optimistically-updated `lastTab` with `LaunchedEffect(tabRoot)` — the comment calls the click-handler write load-bearing, because a tab restored at a detail leaves `tabRoot` null and the route would keep the previous tab — while `AppTabTest` drives `switchTab` only on a bare graph. Pin both ends at the host: a bar tap that switches, and a tab restored at a detail where the next tap must read as a re-tap rather than a switch. The rule is argued in three KDoc blocks and exercised in none of them. |
| B265 | **A whole-route question is answered by any non-null argument.** `switchTab` decides "this is a lift, not the tab's front page" with `currentEntryCarriesArguments`, which asks whether *any* key holds a non-null value. That is right only while `Statistics(exerciseId = null)` is the one tab root carrying an argument: a root with a non-null default — the shape `Measurements(openDaily = false)` already has — would make every re-tap on that tab take the switch branch instead of the no-op one. Ask the tab's own route what it carries, or assert the answer per `AppTab.route` so a new root argument cannot quietly change what the button means. |

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The timed set (N220) has shipped to the unreleased section.

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
