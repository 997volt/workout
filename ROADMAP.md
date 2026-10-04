# Workout — Roadmap

> **v1.11** is shipped and installed. Last reviewed against the code: 2026-10-04.
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

One batch, taken from using the app: what a session records about the body it was trained with, and what
the app says when an exercise is done. A candidate graduates to this section — gaining an id and a
spelled-out decision rather than a wish — when it is picked up, so what stands here is committed work;
the two queues below are where the rest lives, *Later* for what is self-contained and *Parked* for what
is a product in its own right.

### What the body reported

- **N62 — the readiness note grows a sore-muscle list, each muscle with its own score.** The note (N4)
  stays what it is — one free-text line for what a list cannot say, "slept badly", "travel day" — and the
  soreness is a *structured* addition beside it rather than a second prose box: the lifter picks from the
  taxonomy's own
  [`MuscleGroup`](app/src/main/java/com/example/androidapp/domain/model/ExerciseTaxonomy.kt) (the named
  groups; `OTHER` is the "not specified" value and is not offered), and each picked muscle carries a score
  on the existing
  [`TenPointScale`](app/src/main/java/com/example/androidapp/domain/model/TenPointScale.kt), because
  "quads 8, calves 3" is the fact and one number for the whole body is not. Several muscles, several
  scores, added and removed one at a time. It is stored as rows keyed to the session rather than a
  serialized column, so one muscle's score can be read on its own later — the shape the per-exercise
  ratings already use — which means a migration numbered as it ships and the new rows joining the backup
  codec in the same change (N24's rule), with the readiness block in history (N4) rendering the list. A
  blank pick and a skipped prompt still write nothing.

- **N63 — the joint location is picked, not typed: more than one joint, left and right apart.** N9 made
  "which joints" free text on the argument that it is not a set of values the app can check; that was
  right for a note and wrong for a fact the charts are asked about, so the box becomes a pick from the
  body's joints — shoulder, elbow, wrist, hip, knee, ankle, and the central neck and lower back — with
  **left and right as separate entries** for the paired ones, because "knee 6" is half a sentence. More
  than one at a time, and each picked joint carries its own pain score on the same shape as N62's
  soreness list: one picked-list-of-sites-with-a-score component, extracted at this second caller rather
  than built twice. What this owes the rest of the app: the joint-pain trend reads one number per exercise
  today, so it reads the **worst** joint that session rather than an average of two sides, and a session
  rated before the change keeps reading the free text it wrote — history is not rewritten, and free text
  is not parsed into structure it never had. The single *Joint pain (1–10)* field and its note box go, the
  migration is numbered as it ships, and the new rows join the backup codec.

### What the app says when an exercise is done

- **N50 — the app's progression comes back as a decision at *Done*, earned by the plan and its target
  RPE.** N59 withdrew the offer rather than the question: the next set's values are now fields the lifter
  reads and edits, so a chip beside them had nothing to add. What it left was *where* a proposal could
  live and *what* would earn one, and this is both. *Done* (N7) stops opening *How did that feel?* first:
  it opens a **progression prompt** that states what the plan asked and what was done and — when it was
  earned — offers the next step as the lifter's choice: **a load increase** (the smallest loadable step,
  [`DEFAULT_PROGRESSION_STEP_GRAMS`](app/src/main/java/com/example/androidapp/domain/model/ProgressionSuggestion.kt))
  **or a rep**, with doing neither equally available. The rating is not lost: the prompt carries a *How did
  that feel?* action into the dialog N8 already ships, and the inline rating row (N10) stays where it is.
  **Earned** means the exercise came from a plan — a template's planned set or a program slot's
  prescription, both of which carry `targetRpeHalves` — and every prescribed working set was performed
  with its reps met at an RPE **at or under** the target, so the plan was fulfilled with room in hand;
  warm-ups are excluded (N17, N20, N22), and a session with no recorded RPE or no target RPE suggests
  nothing rather than guessing. **The accepted step is written to the plan**, not to the session that just
  happened — the slot's prescription for a program start (P3.8), so it stays per slot, and the
  template's planned set for a direct one — because the plan is what the next run reads and N16 already
  makes a template living. Reading the slot's *history* (P3.8's extension of N22) goes with the rule it
  extended. Rejected: a
  stored "next target" on the session, which is the forward view N16 removed; and restoring N22's rule,
  which computed from the plan's rep ceiling alone and offered itself beside the next set, where this one
  is conditioned on the session's own effort and waits to be asked. It suggests, and it never writes.

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

- **An exercise's own default weight change.** 2.5 kg is one global constant rather than a property of
  the movement: [`Weight.DEFAULT_STEP_GRAMS`](app/src/main/java/com/example/androidapp/domain/Weight.kt)
  steps the fields' +/− buttons, and
  [`DEFAULT_PROGRESSION_STEP_GRAMS`](app/src/main/java/com/example/androidapp/domain/model/ProgressionSuggestion.kt)
  is what the warm-up ramp rounds to, so a machine that jumps 5 kg (or 1 kg) is always edited against a
  step it does not have. The request is a per-exercise value beside `restSeconds` and `techniqueNote` on
  [Exercise.kt](app/src/main/java/com/example/androidapp/domain/model/Exercise.kt), still 2.5 kg unless
  it is set. Whether the ramp follows it too, or only the steppers, is the decision; the value is whole
  grams and its column is a migration numbered as it ships.

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
| P1.9 | kg/lb display setting | You start lifting in pounds. Storage is canonical grams, so this is display-only whenever it is wanted. |
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
