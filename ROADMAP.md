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

**The N147 review's seven findings (B161–B166, N148), plus two from use (B167, N149).** The review checked
every claim against the code and re-ran the gates to a green baseline; the last two came from using the app — an
activity whose history page shows *0 sets*, and a gain and a loss rate that share one default. None is a crash or
a data loss on the normal path: the first three are the built week's own lifecycle, the next four are robustness,
release notes and reachability, and the last two are what use turned up.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| B161 | **medium** | **Adherence still reads the program's slots.** The month calendar and the day dialog never read `program_deload_days`, so a built week's day names the slot's full template — or, if the day was moved, says nothing was scheduled — while home and the start prompt offer the deload workout. The ratio is unaffected (the week is exempt), but the screen that answers *what was scheduled* disagrees with the two that answer *what is planned*. | Read the week's plan in `RoomAdherenceRepository` for the calendar and the day dialog (a day with no slot, not correctable), or state the limit in DECISIONS; a moved-day test pins it. |
| B162 | **medium** | **Unmarking a built week silently loses the plan.** `setDeloadWeek(false)` soft-deletes the mark, and re-marking inserts a new one, leaving the old days attached to the deleted row: the week's workouts never come back. The templates survive, but the mapping is unreachable and a rebuild leaves duplicates; the adherence chip is one tap with no warning, where it used to be harmless. | Confirm unmarking a week that has live days — the app's pattern for a destructive action — and say what stays. |
| B163 | **medium** | **The start prompt cannot settle a deload day.** `onContinue` drops null slot ids, so a missed deload day stays pending and asks again on every start that week, and the dialog's *marks every missed day as skipped* is then false. | Stop asking about a built week's days (not scored, no skip — the smallest consistent rule, P3.10 amended), or give them a skip event. |
| B164 | low | **The creator's one-shot read has no `DataResult` boundary.** `DeloadSourceLoader` calls `first()` on Room flows, so a storage failure escapes the coroutine instead of the screen's error channel, and a failed read falls through to *this program has no workouts*. | Wrap the load in `dataResultOf`, give the state a distinct failure view, and test a failing read. |
| B165 | low | **The release notes omit schema 7 and the downlevel restore.** The backup deliberately stays at `schemaVersion 5`, so a 2.7 build reads a new file and drops `programDeloadDays`: the mark and the templates survive, the week's plan does not. | Say both in the changelog and the N147 evidence; the codec's addition policy is unchanged. |
| B166 | low | **Two prose defects.** `RoomDeloadRepository` still says *a future week is refused* where the code refuses any week but this one; the N147 evidence calls the load share *the screen's one control* when the screen has several. | Correct both sentences. |
| N148 | low | **The creator's result is not reachable from the screens it changed.** The success card offers only *Done*; the program editor and the adherence screen do not say a built plan exists; a rebuild replaces the week's days and leaves the previous templates in the library without saying so. | Link the built workouts from the success card, mark a built week in the program editor or the deload chip, and say what a rebuild leaves behind. |
| B167 | **medium** | **An activity has no page of its own in history.** The list row names it and then still appends *0 sets* (N146 added the name, not the shape); opening it shows the workout detail's header — *0 sets*, *0 kg* — and never says what the session was, so a swim or a ride reads as an empty workout. | Carry the session's activity into the detail (the state already holds it), make it the title/header, and replace the set and volume totals with the activity and the duration; drop the 0-set line from the list row. Test both surfaces. |
| N149 | low | **Gain and loss share one default rate.** `ratePerWeekGrams` derives both directions from N135's single band, so a Lose goal and a Gain goal at the same bodyweight read the same magnitude — the band's centre, 0.375% of bodyweight a week. The request is a **gain** default of about **0.25% of bodyweight a week** and a **loss that is faster**: the band's high edge, **0.5%**. | Amend N135: gain takes the band's low edge (0.25% weekly), loss the high edge (0.5% weekly), for a direction goal and for a target's own side alike; derive and clamp each into the band, state the two numbers and why loss is the faster edge in DECISIONS, and show the goal's own rate in its row; test both directions. |

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
