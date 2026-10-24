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

**A review of everything since 2.13.** What the statistics pass shipped is in [CHANGELOG.md](CHANGELOG.md);
reading it, and the changes that landed beside it, against the release turned up the rows below — the review's
whole finding, no new feature, ordered by what each one costs. The gate set in [AGENTS.md](AGENTS.md) passes
(unit tests, lint, detekt, the debug and R8 assemblies), as do the page's node runner and the generated fixtures,
and nothing here adds a dependency, a permission or a migration. The changed-lines formatting check does not pass
(B218), and the instrumented suite was not run — it needs a device, which is part of B214's point.

| # | What it changes |
| --- | --- |
| B206 | **A set line's count of 0 deletes every set but one.** The rule the code states — blank, a fraction and zero leave the run alone, so typing never silently deletes work — holds for the first two; 0 parses, clamps to the floor of 1, and trims the rest out of the document with no undo. The editor's zero test uses a one-set line, which is why it passes. Treat 0 as "not a count" (or ask), and test a line of several sets. |
| B207 | **A set line grown in the middle of a workout reorders the plan.** The helper appends a grown line's copies at the exercise's next free index, and the app orders a plan's sets by `setIndex` (`TemplateDao`), so growing the first of two lines leaves *2 · 2 · 2* where *2 · 2* was asked for: the copies land after the later line, and the line splits in two on the next render. Every test grows the only line the fixture has. Grow within the line (shifting what follows), or let only a workout's last line take a count. |
| B208 | **The scrub reads the series by an index the series may no longer have.** `ChartFigure` holds `scrubbedIndex` across a series change and reads `series.readings[index]` unguarded at the readout, while the marker beside it guards with `getOrNull`; a second finger on a range chip while the first holds the figure changes the series under the index, and the read is then the wrong reading or a crash. Hold the reading (or its date) rather than its place. |
| B209 | **`[Unreleased]` covers only the statistics pass.** The day field, the goal row's move to measurements, the settings order and the helper's set lines and folded days all landed after v2.13 with no entry, while the statistics bullet ends "Nothing about the series, the store or the goal line moved" beside an undocumented move of the goal's row. [RELEASING.md](RELEASING.md)'s step 2 is where this is required; add the entries (or say why a change needs none) before the next release. |
| B210 | **The series search only searches the open group, and the group's own name is a match.** The menu filters the registry to the chosen group before matching, so searching for weight while *Per lift* is open answers "No series matches." with nothing saying the scope was the group — and because the group's label is matched too, a query that is a substring of it (a single `e` under *Per lift*) matches every series and narrows nothing. Search every group and show which one a hit is in, or say "No series in *Per lift* matches"; settle the label match, which [DECISIONS.md](DECISIONS.md) argues for. |
| B211 | **A drag that starts on the figure does not scroll the screen.** `scrubbable` consumes the down and every move, and a scrollable cancels its own drag when a change is consumed, so the figure is a dead zone for the screen's `verticalScroll` — new with N200, because the frame had no pointer input before it. Consume only the horizontal part of the gesture, or let a drag that begins vertically through. |
| B212 | **A measurement's day can be moved into the future.** The new field's calendar offers every day and nothing behind it refuses one: `save` validates soreness and the day's collision only, and `withDay` accepts any `LocalDate`. A record is of the past, so a day after today should be refused where it is picked and where it is written, and said why; a test pins that. |
| B213 | **The readout is announced, but only a finger can move it.** The polite live region changes only through `scrubbable`'s pointer stream, and the figure exposes no accessibility action, so the reader N206 named cannot make the value change; the readings list below is the accessible counterpart (N36) and is folded. Give the figure previous/next-reading actions, or stop claiming the readout is the accessible path and say the list is. The dialog's name is likewise a `Text` inside it, not a pane title. |
| B214 | **The day field's round trip and its refusal are outside the default gate.** The screen test stops once the calendar is displayed: nothing proves a picked day reaches the draft and the save. The collision refusal lives only in the Room test, and the instrumented job runs nightly rather than per change — the gap N180 closed for the editor. Add a JVM test for pick → save, and either mirror the refusal in the default gate or say why it has to stay on a device. |
| B215 | **The weight goal's move dropped the test that pins N135.** `aDirectionGoal_offersNoWeight_andInventsNone` went with the statistics row and was not re-added beside the row on measurements, so "the app never manufactures an end weight the lifter did not state" is untested; and no test asserts the new three-flow `combine` carries the goal and the history into `MeasurementsUiState` — the two added ViewModel tests check the writes. Move the lost test and cover the state that moved. |
| B216 | **Three new tests assert the setup where their names promise the claim.** `theWeightGoal_sitsAtTheTopOfTheList` checks that the row exists, not that it is above the entries; `aGapIsNeverTheAnswer` accepts either end when the midpoint is equidistant, so the tie-break is unpinned; the scrub test never lifts the finger, so nothing says the readout goes away. Pin each, or rename it to what it checks. |
| B217 | **The pass left its shared pieces on the wrong side of the packages.** `ui/components/TrendChart.kt` now imports `ui.statistics.scrubbable`, while `ChartMarker` lives in components; `ui/measurements` reads `WeightGoalRow`, the registry and `goalOutcomes` from `ui.statistics`; and the new measurement-day calendar is a second copy of the statistics range's `DatePrompt`, which "extract at the second caller" would have made one. Move the gesture beside the marker now, and the calendar into `ui/components` beside the other shared dialogs; the measurements side is what N207 has to answer. |
| B218 | **The pass leaves the changed-lines formatting gate red, and two doc blocks that no longer describe anything.** `tools/format-changed.py` over this change reports indentation in `ChartExtras`, import order in both new chart files, the blank-line runs a deleted block left behind, and comment spacing in `TestTags`; and the deleted weight-goal parameter's doc now sits above `onOpenMeasurements`, while the axis-end doc sits after `planLines` with no declaration under it. Run the tool with `--fix`, then re-attach or delete the two blocks — and the same tidy pass can drop the fully-qualified `MetricSeries` in `StatisticsBody`. |

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**What the statistics pass left open, and what it made visible.** N203–N206 and N208–N209 shipped and are in
[CHANGELOG.md](CHANGELOG.md); **N210 was struck** because the range has been remembered since N35. What remains is the
structural split, a recents row, pinch and the expanded view's fallback on a short window (B219).

| # | What it changes | Brought forward when |
| --- | --- | --- |
| N207 | **`ui/statistics` splits into a chart package.** The screen hit the 20-function ceiling three times in one pass, and the chart now spans four files; the menu, the figure and the axis already live in files of their own, so the next function that has to live on the screen is the one that starts the split. A refactor with no behaviour change, which is why it lands alone. | The next change that adds a function to `StatisticsScreen.kt`. |
| N212 | **A short "recently used" row above the metric trigger**, now that groups and search have made the menu navigable. | The same two or three series are what actually gets read. |
| N213 | **Pinch changes the window**, the gesture a chart on a phone already invites; it needs a policy for pinching past the data before it can land. | Re-choosing the range by hand to zoom is what people do. |
| B219 | **The expanded figure has no fallback for a short window.** The dialog draws the figure at a fraction of the window and stacks the title, the figure and the card's extras in a Column with no scroll, so a landscape or small window clips what is under the figure — the close button was moved above it for that reason and is the only part protected. Size the figure by what is left, or make the column scroll. | A window in which the expanded view clips its extras, or the next change to the dialog. |

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
