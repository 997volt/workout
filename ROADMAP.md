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

**A review of everything since 2.14.** The work after the tag is in [CHANGELOG.md](CHANGELOG.md); reading it
against the code turned up the rows below — the review's whole finding, no new feature, ordered by what each one
costs. The Kotlin gate passes: unit tests, `:shared:jvmTest`, lint, detekt, and the debug and R8 assemblies; the
changed-lines formatting check passes, as do the helper's node runner, single-file build and fixtures. Two things
were outside it. The **helper page is never rendered by a gate** — its node tests stop at the parser — which is
how B222 was missed, and the **instrumented suite was not run** here (no device or AVD), so the goal store's day
arithmetic and the template migration are unverified on this machine; B238 is that coverage gap. N207's package
split and N214's hidden filter behave as their plans say, and B221's weight line is right; the defects below are
what remains.

| # | What it changes |
| --- | --- |
| B222 | **The import helper page no longer opens.** `renderBodyPanel` reads `waistline.problems` while `state.waistline` is null; `renderAll()` calls it on page init and the *Clear body* control calls it after setting the state to null, so the page throws on load and when body data is removed. The node runner only exercises the pure parser and the single-file build only bundles the script, so neither renders it. Default the problems to an empty list (or return when there is no file) and add a smoke test that renders the panel and calls `init` with no Waistline state. |
| B223 | **A goal set on a day that already carries a boundary can fail to take effect.** `savePeriod` recognises an existing boundary with `startedAt == startMillis` (the new milliseconds are local midnight) and `startProblem` compares the same way, while every weight period written before N215 — by the old `setGoal` path and by any restored v2.14 export — carries a clock time. Setting a goal on such a day inserts a second, earlier boundary: the new goal owns only the hours before the old one and is usually Unjudged, and when the clock row is the newest, `observeCurrent` still returns it, so the card keeps the old goal and the save appears to do nothing. `editPeriod` then writes midnight for a goal-only change, moving the boundary earlier and handing it readings it did not own; a same-day predecessor makes the edit refused. Compare and order by the local date, and keep the stored instant when the picked day is unchanged. |
| B224 | **Saving the goal already in force opens a boundary.** `insertPeriod` has no equivalent of `openPeriod`'s same-kind/same-value guard, so a period that restates the open goal is written as a new row. The card's dialog opens prefilled, so opening the goal and confirming on a later day cuts the period in two; a long *Met* maintenance period can become two, one of them Unjudged. `onSaveGoalPeriod` still promises "a row that asks for what is already there writes nothing". Restore B140's guard for an open-ended new period. |
| B225 | **The goal editor can open on `LocalDate.MIN` and crash.** The measurements state defaults `today` to `LocalDate.MIN` until the first emission, the goal card is drawn during loading (only the `openDaily` shortcut is gated), and the editor formats the start through `atStartOfDay(...).toInstant()` — an `ArithmeticException` on `LocalDate.MIN`; the date picker would throw as well. The state's KDoc says the editor is only reachable after the list loads, which is not enforced. Gate the card or its *Set* control on `!isLoading`, or make `today` nullable and disable the editor. |
| B226 | **The recents row cannot restore a per-lift series.** The stored id is the metric's (`EXERCISE:<metric>` and `EXERCISE_JOINT:<site>`), with no lift; `onSelectExercise` never records; a chip only calls `onSelectMetric`. Bench and squat volume therefore share one chip and re-choosing the other lift does not even re-record it; from the default bodyweight series (no lift chosen yet) a chip leaves the lift null and lands on *Choose a lift*; from another lift it shows that lift's numbers under the same label. Store the selection (metric and lift), have the chip restore it, record a lift choice, and bound and dedupe the reader while there. |
| B227 | **A pinch zooms around the wrong day and by the wrong factor.** `pinchedWindow` maps `focus` over the range window's days, while the figure's x-axis spans the windowed series' first and last reading; with a seven-day window whose weigh-ins are only the last three, a centre pinch anchors two days before the finger and a 2× pinch produces a four-day window that can drop the reading under the finger and today's. At the left edge it can produce a window with no readings, and `SeriesChart` then returns, so the figure and the readings list disappear while the range row says *Custom*. Map the focus and the scale over the plotted span; keep the whole-series days for the clamps. |
| B228 | **A fast pinch loses events.** `calculateZoom` reports the scale since the previous motion event, but `onPinch` recomputes from `uiState.value`, which catches up only after `pendingRange` → `range` → `sources` → `uiState`; events delivered before that collector runs all compute from the same span, so all but one increment is lost and the window under-zooms. Compute from the pending window the gesture itself is building. |
| B229 | **A settled pinch can silently revert or flash.** `onPinch` drops the `setStatisticsRange` result and clears `pendingRange` unconditionally, so a failed commit reverts with no message; and because the preferences listener reaches the range on a later main-thread turn, a successful one can show the old window for a frame. Clear only when the stored flow reports the settled range, and surface a failure. |
| B230 | **Setting today's weight goal is titled "Add a past goal".** The dialog title is chosen by `editor.id == null`, and the card's editor is always a new period starting today, so the primary goal action is headed as a past one. Carry the title on the editor (the card's is *Weight goal*). |
| B231 | **Removing a goal period says "Goal removed."** The snackbar uses the `statistics_goal_period_removed` string, whose text is *Goal removed.*; removing a period leaves the previous one in force, and Undo restores a period, so the sentence is false. Say *Goal period removed.* |
| B232 | **The undated first period cannot be corrected.** The row the N137 migration creates has no start, `GoalPeriodDraft.valid` requires one, and `savePeriod` takes a non-null `LocalDate`, so changing only its kind or weight forces inventing a date — the invented history N137 refused. Let a goal-only change keep the start unknown (a nullable start on the write). |
| B233 | **The goal undo writes outside the timeline.** `restorePeriod` re-inserts by id with no day or boundary check; remove the newest period, save a new goal for today while the snackbar is up, then Undo, and two rows share today's midnight — `observeCurrent` breaks the tie on `createdAt DESC`, so the restored row wins and becomes the goal in force. Refuse or dedupe a restore whose day already carries a boundary, and clear the offer on a save. |
| B234 | **A refused goal save loses what was entered.** The dialog calls `onSavePeriod` and closes immediately; the repository's refusals (a taken day, a start outside its neighbours, an end past the next, an end on the open period) arrive asynchronously, so the typed weight and both picked days are gone with only a snackbar. Keep the dialog open until success, or pre-check the neighbours. |
| B235 | **"Last year" still rides the range row.** `chipLabelRes` falls back to `labelRes` for `LAST_YEAR`, so selecting a year puts *Last year* on the *More* control — the word the row dropped for six months — and the test covers only six months. Add a short *1 year* and test it. |
| B236 | **A pinch runs a record-count query per motion event.** `records` is derived from the live range, so every pinch event issues `countRecordsIn` and re-derives the overview and the series; one gesture can send dozens of queries while nothing on screen needs the count to move. Derive it from the settled range. |
| B237 | **The new tests assert the setup, not the claims.** The pinch tests use readings that fill the window, so the anchor bug passes; the screen pinch test accepts any scale above 1 and any focus in 0..1; the recents test uses a lift-less Exercise key and covers no unknown or over-long store; the goal same-day tests only write identical midnights; the add-a-past-goal screen test saves today's *No goal*; the removal snackbar is never composed; and `GoalMigrationTest`'s B140 case now passes because both writes are the same day, not because the value is the same. Pin each, and add a real past date and a removal-then-undo case. |
| B238 | **The goal store's day arithmetic stays outside the default gate.** `GoalPeriodsTest` is the only place a boundary's day is exercised, and it needs a device; the JVM tests drive a fake goal repository that does not enforce the day rules (and ignores the end). Mirror the pure day rule on the JVM — it is exactly the rule B223 and B224 break — or treat the instrumented job as part of the gate for this table. |

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.**

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
