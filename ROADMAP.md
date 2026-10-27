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

**The statistics page: summaries first, one row saved, and an axis that does not read as one.** Three items
from the same screen. The summaries sit behind the evidence, and the target rate — which its own comment already
calls "the family the average and the trend belong to" — below the thing that hides them (N221). The control that
sets the moving-average window takes a row of its own above the dates while *Expand* sits alone under them (N222).
And the figure's scale is drawn as a line at its top instead of against its top, middle and foot (B259).

| # | What it changes |
| --- | --- |
| N221 | **The statistics screen reads chart, summary, then evidence.** The two summaries are promoted out of the readings disclosure and drawn under the figure in that order — *Average* (the mean of the window, not the chart's moving average) and *Trend* (the fitted line, stated per week) — and the metric's target follows them: *Target rate* for weight, where the goal itself is set on Measurements, and *Target* for every other metric, since the target is the same fact read for a metric with no rate. Below them the readings keep their disclosure, now as the evidence rather than as the way in: header with the count, newest first, `rememberSaveable` as today. What the row has to decide is where *Overview*'s three numbers land — they follow the target rate now, and "readings last" leaves them between the two — and whether the readings header should announce its expanded state, which today it does not. It amends N36 ("the readings, newest first, with the average on top") and N208 ("the three numbers after the figure"), stays addressable by tag so the existing tests follow the content rather than the fold, and is the same kind of reorder as the one that moved the figure above the cards: the chart is what the screen is for, and the page should say what it shows before listing it. |
| N222 | **The window the average is taken over shares a row with the figure's *Expand*.** *Average of last: N* is the chart card's first row and *Expand* its last, so the control that sets the average costs a row above the dates while the control that enlarges the figure sits alone under them. Put the picker on the left and *Expand* on the right of one row beneath the dates and the plan lines, so the card reads figure, dates, actions. Neither control changes: the picker keeps its dropdown of the three windows (N40, N211) and its announced label, *Expand* keeps its tag and its target size, and the row has to survive the narrowest phone without the picker's label pushing *Expand* off the card. This is the chart's own *Expand* — the full-height view — not the readings header; the window is a property of the line it averages, and moving it into the evidence row would make a chart control look like a setting. |
| B259 | **The axis's three numbers are drawn in a line at the top of the figure.** On the emulator the weight/volume scale reads 722.5, 361.25 and 0 inside about a hundred pixels at the plot's top, while the plot itself runs the figure's full 180 dp and the dates row sits at its foot — so the scale is a caption rather than an axis, and a reader cannot tell which number belongs to which height. It reproduces on the phone and it is layout, not arithmetic: `ChartAxis` asks for `fillMaxHeight()`, but the page scrolls (`Column.verticalScroll`), so the constraint it fills is unbounded and the modifier resolves to wrap-content; the `Row` then takes its height from the canvas and the gutter's three labels stack at its top. Give the gutter the figure's height — already a parameter of `ChartFigure` — rather than the row's, or measure the row at `IntrinsicSize.Min`, and pin it with a test asserting the three labels' vertical bounds are spread across the figure instead of the top hundred pixels. The expanded dialog draws the same figure, so it inherits the fix and gets the same assertion. |

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
