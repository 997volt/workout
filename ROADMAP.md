# Workout — Roadmap

> **v1.14** is shipped and installed. Last reviewed against the code: 2026-10-07.
>
> Forward-looking only. What shipped is [CHANGELOG.md](CHANGELOG.md), how a release is cut is
> [RELEASING.md](RELEASING.md), and settled decisions with the rules that apply to every
> change are [DECISIONS.md](DECISIONS.md) — their argument, where there is one, is
> [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).

A local-only training notebook: write a plan, log what you actually did against it, and let
the app show you the difference and what to do next.

Ids (`F#` foundations, `B#` defects, `N#` the next planned changes, `P#.#` the product
backlog, `R#.#` releases) are stable and go in commit messages. They were assigned when the
work was planned, so they do not run in order — and an id this file does not list has
shipped, with its entry in [CHANGELOG.md](CHANGELOG.md).

## Next

**Nothing.** The fourteen defects a review of the unreleased N74–N79 batch found (B59–B72) are fixed, and
the batch itself is recorded in [CHANGELOG.md](CHANGELOG.md) under *Unreleased*. A candidate graduates to
this section — gaining an id and a spelled-out decision rather than a wish — when it is picked up, so what
stands here is committed work; the two queues below are where the rest lives, *Later* for what is
self-contained and *Parked* for what is a product in its own right.

## Later (still self-contained)

Everything that stood here has shipped — the defects found in use, the workout screen's discard, the
workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch, a rest of
zero, the planned-set prefill, the program document, and the eight defects a review of that batch found
and closed (B51-B58) — each with its entry in [CHANGELOG.md](CHANGELOG.md). A candidate graduates to
*Next* — gaining an id and a spelled-out decision — when it is picked up, so this queue is where
unplanned work waits, and *Parked* below is where deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (the app states what happened; it never writes what it decided), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

### Three requests from use

Each small and self-contained, and none has been picked up, so none has an id yet — picking one up
is what gives it a spelled-out decision and a place in *Next*. The last request that stood here — an
exercise's own weight change — became N77 that way and has shipped, with its entry in
[CHANGELOG.md](CHANGELOG.md).

- **More room around the next-up block.** A next-up row is drawn with 8 dp above it, inside the home
  start bar's own 12 dp of vertical padding, and its name sits against its *Start* button with nothing
  between them beyond the button's inset. With more than one program active (P3.12) the bar can carry
  several rows, and the block reads tighter than the cards it sits under. The decision is *where* the
  room goes — the block's own margin, the space between rows, or the gap between a row's text and its
  action, which are three different fixes — and it wants a device with two active programs rather than a
  guess.
- **Bring *Substitute* back to a next-up row.** Today's card offers it (P3.11) and the next-up rows do
  not, although a next-up row is the same `TodayPlan`: it already carries the slot's id and a *Start
  planned workout*, so `substituteOccurrence` fits it as it stands. **What is not settled is which
  occurrence the pick lands on.** A substitute is an event keyed by slot *and* week (P3.11), and
  [WorkoutsHomeViewModel.kt](app/src/main/java/com/example/androidapp/ui/home/WorkoutsHomeViewModel.kt)
  writes the week of *today* — right for today's card, which is that occurrence by construction, and
  questionable for a next-up row, which P3.9 makes deliberately **calendar-free**: a run advances when a
  slot is trained or skipped, never because a day passed. On a Sunday the next-up Monday falls in the
  *next* week, so the pick would be keyed to a week whose Monday has already gone. So the decision is
  whether a next-up pick is a week-bound substitution at all — and if it is, which week it lands in
  (`ProgramSchedule.occurrenceDate` is what turns a week and a weekday into the date in question) — or
  whether it only opens the session and records nothing.
- **A template exercise's planned sets stop being folded away, and the block takes the workout's
  shape.** The plan is one `ListItem` reading *Planned sets · 3* and everything about it is behind the
  tap: `TemplatePlanDialog` is the list, the only place a rung's derived load is shown, and where both
  *Add set* and *Add warm-ups* live. The sets become lines in the block itself, the way `ExerciseSets`
  draws a workout's logged ones, so the count and the loads read without a gesture; *Add set* becomes
  the block's foot, a full-width button where the workout's *Log set* is (N59); and *Add warm-ups*
  moves into the exercise's ⋮, which is N53's rule for the rare action — offered only where a ramp can
  be built, the `warmUpRampFor` predicate the button already reads (N28, B50), which is N53's own
  "what cannot be done is not offered". That menu is shared with the workout (N71), so the entry has
  to be optional and null there, the shape `supersetGrouped` already uses.
  **What is not settled is what *Add set* opens.** The workout could state its values on the screen
  because a logged set is the plan's prefill with the occasional correction (N59); a *planned* set is
  not that shape — its role, load, rep range, note and a run's value are all optional, and are authored
  once as a plan rather than once per set performed — so either the button opens today's
  `TemplateSetDialog` on N46's prefill from the last set, which is the smaller change and keeps every
  field the row can carry in one place (B61), or the block grows on-screen target fields and the dialog
  becomes correction only. That decides whether `TemplatePlanDialog` survives as anything — the list it
  draws is what is being unfolded, and a set line reachable by tap leaves it no job — and where the
  plan's rest, RPE and cue row sits relative to the unfolded sets and the new foot. It also touches
  durable content: [DECISIONS.md](DECISIONS.md) names *a template's plan dialog* as one of the two
  surfaces that author sets, and N46's accepted cost leans on the list behind the dialog showing the
  count.

## Parked — deliberately not planned

Each row is a product in its own right, contradicts "local-only", or both. Parking is a
decision, not a backlog, and every row names what would change it. Parked is **not** the same
as the non-goals below: these become possible again the moment their trigger fires, while a
non-goal is a line this app does not cross.

| # | Feature | Revisit only if |
| --- | --- | --- |
| P4.1 | Health Connect read/write | A user asks to share with a platform health graph. It is a sharing integration; this app stores data for its user. |
| P4.2 | Foreground service | The rest timer needs to survive something the alarm and the in-app timer cannot. |
| P4.3 | Home-screen widget | The glance it would give turns out to be the missing thing. |
| P4.4 | Quick Settings / launcher shortcuts | Starting a routine becomes frequent enough to deserve a second entry point. |
| P4.5 | Wear OS companion | Wrist logging is genuinely wanted — and you accept `play-services-wearable`, which breaks the no-GMS line. |
| P4.6 | Bluetooth heart-rate straps | The product becomes heart-rate training rather than logging. |
| P4.7 | WorkManager reminders | Nudges demonstrably improve adherence. |
| P4.8 | Large-screen layouts | Tablet or foldable users actually appear. |
| P4.9 | Offline-first sync | There is a real multi-device story. It needs a backend, accounts and conflict resolution — the largest irreversible commitment on this list. |
| P3.7, P5.2 | Friends, shared routines | Accounts, servers and moderation become worth owning. |
| P5.3 | Monetization / Play Billing | There is a concrete reason to charge, and a willingness to take the Play-services dependency. |
| P5.4 | Localization | A non-English user appears. |
| P1.11 | Onboarding: goal, experience level, weekly target | This stops being a single-user local tool with one obvious user. It personalises defaults, and there are no defaults to personalise. |
| P1.17 | Accessibility audit | The per-screen rule stops being enough — a real complaint on a device, or a screen that grew past ad-hoc tagging. The rule still applies to every change; only the sweep is parked. |
| P2.5 | Progress photos | A visual record is actually wanted, and an encrypted-storage design for it is acceptable. |
| P2.8 | Muscle-group balance warnings | Enough history exists for a rolling window to say something true rather than something plausible. |
| P2.6 | Plate calculator | Loading from a plan's target is frequent enough that the arithmetic gets in the way, and you would rather it were done for you. |
| — | **Play Store listing** | You want distribution beyond `adb install`. Self-install works today, and Play App Signing would change who holds the signing key. |
| — | **Encryption at rest / app lock** | You start carrying the phone somewhere you would not carry the data. |
| F6 | Module split into `:core:*` / `:feature:*` | **A named goal, not a refactor**: a measured build-time problem, working on one feature without compiling the rest, or a second surface (Wear, a widget). |
| F11b | Product analytics | Almost certainly never: on a single-user local tool it buys nothing, and it would breach the no-`INTERNET` line. |

### N39 — the plan's target, parked by decision

The per-metric target shipped; **the plan's target was scoped out** when the feature was
built, so it is parked rather than planned. It is a different shape of work: the training plan
is not one of the statistics screen's sources, so it means making the plan available to a
screen that knows nothing about it, plus a rule for a lift that is in two plans at once or in
none.

## Explicit non-goals

Permanent, unlike *Parked* above: nutrition / calorie tracking, social feeds, live GPS route
tracking, and a web dashboard. Each is a product in its own right and would dilute the logging
core.

## Keeping this true

Four rules, written after the drift they prevent had happened four times — stale test counts,
a dependency inventory, an enumerated feature list that v1.3 quietly outgrew, and a review
stamp still reading v1.3 while *Next* said "Nothing" after v1.4 had shipped with defects
unfound. Two of those four were this file describing itself wrongly.

1. **Nothing marked done lives here.** Shipped work goes to [CHANGELOG.md](CHANGELOG.md), and a
   finished row is deleted from this file.
2. **No hand-maintained facts.** No test counts, no dependency lists, no inventory of which
   files exist. Those are commands (`./gradlew …`) or links.
3. **Every parked row names its revisit trigger**, so parking reads as a decision rather than a
   forgotten item.
4. **Durable content lives in [DECISIONS.md](DECISIONS.md).** Settled decisions and the rules
   that apply to every change are a reference, not a queue, and the two age differently. A
   section here that accumulates rather than drains belongs there.

Bump the review stamp at the top whenever this file is checked against the code.
