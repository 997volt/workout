# Workout — Roadmap

> **v1.13** is shipped and installed. Last reviewed against the code: 2026-10-05.
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

### N74 — progression is offered per planned set

*Done* states **one row per working set** — what the plan asked, what was done, and the step that set
earned on its own — and the lifter chooses a direction per row, **committed by one *Done***; *Not now*
or a dismiss writes nothing, and a failed write reports the error, keeps the rows up and finishes
nothing. Today the rule is exercise-wide twice over: earning needs **every** prescribed working set met,
and the step always lands on the **last** working set, so the dialog names no set and cannot move a
back-off set differently from a top set (N50). **Earning becomes independent per set** — a set performed
with its reps met at or under the exercise's one target RPE (N59) earns its own step whatever its
siblings did — and **two bulk actions** (*raise every earned set's load*, *…reps*) cover the plan whose
sets share a target while a row stays individually changeable. The prompt is **frozen when *Done* is
tapped** and held as view-model state: a written target would otherwise re-arm the same offer on the
next read, and a selection is not a write. Unchanged: the RPE target stays one number for the exercise
(N59), the earning predicate and the warm-up exclusion (N17, N20, N22), the unit's own step (N64), and
that only what the lifter chose is written. **No migration and no backup-format change** — a planned
set's weight, assistance and rep targets are already its own columns. DECISIONS' N50 bullet is amended
where it fixes the exercise-wide rule, with the argument for this id beside it as N74's evidence.

A candidate graduates to this section — gaining an id and a spelled-out decision rather than a wish —
when it is picked up, so what stands here is committed work; the two queues below are where the rest
lives, *Later* for what is self-contained and *Parked* for what is a product in its own right.

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

### One request from use

Self-contained enough for this queue, and small enough that its decision is spelled out when it is
picked up rather than now — so it is a wish, and gains an id when it graduates.

- **An exercise's own default weight change.** The step is one global pair of constants rather than a
  property of the movement:
  [`Weight.stepGrams`](app/src/main/java/com/example/androidapp/domain/Weight.kt) answers 2.5 kg or 5 lb
  from the unit alone, steps the fields' +/− buttons, and is what the warm-up ramp and the progression
  offer round to — so a machine that jumps 5 kg (or 1 kg) is still always edited against a step it does
  not have. **N64 narrowed this rather than closing it**: the unit is answered for and the movement is
  not, and the step now travels as an argument to the ramp and the offer instead of being read from a
  constant at each call site, which is the seam this request wants. The request is a per-exercise value
  beside `restSeconds` and `techniqueNote` on
  [Exercise.kt](app/src/main/java/com/example/androidapp/domain/model/Exercise.kt), still the unit's own
  step unless it is set. Whether the ramp follows it too, or only the steppers, is the decision; the
  value is whole grams and its column is a migration numbered as it ships.

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
