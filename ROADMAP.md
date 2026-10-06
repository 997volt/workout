# Workout — Roadmap

> **v1.14** is shipped and installed. Last reviewed against the code: 2026-10-06.
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

### N75 — adductors join the muscles, and Back splits three ways

**Adductors** becomes a `MuscleGroup`, and that is the whole of its half: the readiness note's sore list
is derived — `SORE_MUSCLE_GROUPS` is the taxonomy minus *Other* — so one constant puts it in the picker
and in the exercise dropdown at once, and a second list of "muscles you can be sore in" would be two
vocabularies for one idea. **`BACK` splits into `LATS`, `UPPER_BACK` and `LOWER_BACK`**, and that half is
a data problem rather than an enum edit. Every muscle is stored by name and read back through
`MuscleGroup.valueOf`, so deleting `BACK` would make every row that still says it — and every export
written before this — throw on read. It therefore **stays in the enum as a legacy value**: excluded from
the pickers, still resolved wherever it is read, and shown as *Back* rather than dropped, the way N59's
legacy per-set effort and N63's legacy joint number are.

**The seeded library is corrected by a migration, not by editing the seed.** The seed is an
`INSERT OR IGNORE` top-up that runs on every open and never updates a row, so a re-classified seed
reaches fresh installs only. The app knows its own movements, so rewriting those ten rows (eight primary,
two inside a secondary list) by id is a correction rather than a guess — deadlift to `LOWER_BACK`, lat
pulldown to `LATS`, and so on. **A custom exercise a lifter tagged Back is left alone**: nothing in the
row says which of the three it is, and picking one would be the guess this project refuses. The new
constants sit in anatomy order in the pickers rather than being appended, which is the one thing the
enum's "append it, order is cosmetic" habit does not cover — the order *is* the picker's map, and
*Adductors* after *Other* is a worse one.

Unchanged, and worth saying so: the sore-muscle save validates the score and never the muscle, no
statistics or balance code aggregates a muscle group (P2.8 is still unbuilt), and there is no muscle
filter to widen. A migration is numbered as it ships; adductors alone needs none.

### N76 — *Done* sits beside *How it felt*

The exercise's foot stacks two controls: the rating row, then *Done* eight points below it. They answer
one question — the exercise is over, and how it felt is the last thing to say about it — so they become
**one row**: the rating row takes the width, the exercise's state action sits at its end and the same
height, and the pair reads as the exercise's closing line rather than as a field with a button near it.
The action keeps its place at the foot (N69's reason for putting it there has not changed) and its tags —
`EXERCISE_DONE`, `EXERCISE_REOPEN` — so nothing addresses it by its English; the rating row keeps its own
tag and its *"How it felt"* click label.

Two details the shape decides. The rating row's click target is the full width today, so it has to give
up the half the action needs or it would swallow the button. And the rating composable is shared with
History, so anything added for this stays optional at that call site. With no set to finish nothing is
drawn (N69), which leaves the row holding the rating alone.

Rejected: moving *Done* up beside the exercise's header. N53 moved these actions to the foot precisely
because the header is read constantly mid-session, and that reason stands.

### N77 — an exercise's own weight step (graduated from *Later*)

The ± steppers move by one global pair of constants —
[`Weight.stepGrams`](app/src/main/java/com/example/androidapp/domain/Weight.kt) — which answers 2.5 kg or
5 lb from the unit alone, so a machine that jumps 5 kg (or 1 kg) is still always
edited against a step it does not have. N64 left the seam for this by making the step travel as an
argument to the warm-up ramp and the progression offer instead of being read from a constant at each
call site. An exercise gains **`stepGrams: Long?`** beside `restSeconds` and `techniqueNote` on
[Exercise.kt](app/src/main/java/com/example/androidapp/domain/model/Exercise.kt) — whole grams, null
meaning the unit's own step — and every ± that moves its load reads that where it has one: the workout's
fields, the warm-up ramp and the progression offer. **The ramp follows it too**, because a ramp rounded
to a step the machine cannot load is the same defect one screen over, and the plan editors that step a
planned set's load follow it with them.

It is a per-exercise property and not a per-plan one, it travels in the backup file, and it is edited on
the exercise's own detail screen beside the rest and the cue. A migration numbered as it ships adds the
column. It is not the plate calculator (P2.6 stays parked), and it does not become a second
prescription anything has to keep in step with the plan.

### N78 — Programs and Templates while a workout is running

Both screens are unreachable mid-workout twice over: the tab bar is hidden on the logger — N34's reason,
that a tab bar under a live set logger is an invitation to lose the session — and Home hides its
Programs and Templates links whenever a session is open. A lifter who wants to look at what is next has
to end the workout to see it. **The workout's own overflow, which today holds only *Discard*, gains
*Programs* and *Templates*, and Home stops hiding its two links while a session is open.** The tab bar
stays off the logger: the reason it was off has not changed, and an overflow entry is a deliberate step
rather than a bar sitting under the thumb.

**The Start control in the Templates list is disabled while a session is open**, and says why rather than
only greying. Starting is already idempotent — `startOrResumeSession` returns the open session — so the
button would not open a second workout; it would silently take a lifter who tapped *Start* on one
template into the workout already running from another. That is the lie the disabled button prevents, and
the safety was never the missing part. Programs has no Start to disable (its trailing action is *Use*,
which activates), and Home's own launch points keep their meaning: the primary button already reads
*Resume* when a session is open, and the plan and next-up starts go through the same gate and resume the
same session.

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

### Nothing waiting

The one request that stood here — an exercise's own default weight change — graduated to *Next* as
N77, with its decision spelled out rather than deferred. The queue is empty rather than closed: it is
where a candidate waits as a wish until it is picked up, and picking one up is what gives it an id.

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
