# Workout — Roadmap

> **v1.16** is shipped. Last reviewed against the code: 2026-10-08 — the N87–N94 batch shipped
> together, which emptied *Next*.
>
> Forward-looking only. What shipped is [CHANGELOG.md](CHANGELOG.md), how a release is cut is
> [RELEASING.md](RELEASING.md), and settled decisions with the rules that apply to every
> change are [DECISIONS.md](DECISIONS.md) — their argument, where there is one, is
> [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).

A local-only training notebook: write a plan, log what you actually did against it, and let
the app show you the difference and what to do next.

Ids (`F#` foundations, `B#` defects, `N#` the next planned changes, `P#.#` the product
backlog, `R#.#` releases) are stable and go in commit messages. They were assigned when the
work was planned, so they do not always run in order — and an id this file does not list has
shipped, with its entry in [CHANGELOG.md](CHANGELOG.md).

## Next

**Nothing is queued.** A candidate graduates to this section — gaining an id and a spelled-out decision
rather than a wish — when it is picked up, and the queues below are where the rest lives: *Later* for what
is self-contained and *Parked* for what is a product in its own right.

## Later (still self-contained)

### Two requests from use

No id yet: picking one up is what gives it a spelled-out decision and a place in *Next*. They are recorded
together for two reasons: the second cannot be built before the first, since a pattern that lives on a
category needs categories to exist, and both are one job — giving the library a shape that statistics can
read. The first is a design before it is a change, so what follows is mostly the argument for one shape over
the others, and each entry ends by naming what the work touches — plus what is still open, where anything
is.

- **Exercises get variations and families, and the library groups them.** One movement is performed several
  ways — a flat barbell bench press paused for three seconds, touch-and-go, as a speed day, or in competition
  style — and the library has nowhere to say so today: each is either its own unrelated row or one row the
  lifter keeps renaming, and the rest are lost. **The grouping is a link, not a rename and not a merge.**
  Every exercise stays loggable and its own, because that is the half that must not blur: a paused bench is
  a different lift and moves less weight, so its records, the weight it steps by (N77) and what a plan
  prefills from last time (P3.8) are its own. What the link buys is the other half — the views that fragment
  when one movement is scattered across rows: muscle-group volume, "how much pressing am I doing", and the
  per-lift adherence breakdown (P3.14). The head of a group is a **category**: a row that exists to hold
  others, is **never offered and never logged**, and carries a name. That is what makes it a *statistic*
  rather than a classification — "all bench press volume" can be read as one number — and it is why the
  category is not `movementPattern` plus `primaryMuscle`, which answer a fuzzier question ("horizontal chest
  pressing", taking in a fly or a dip) while a category cuts where the lifter cuts: a machine press is in or
  out of "bench press" because the lifter says so. **Dumbbell and machine bench belong in the same category**,
  as exercises rather than as variations of the barbell one — the number the category exists to produce is a
  lie about the training with the dumbbell work missing from it, and each stays its own exercise with its own
  records, step and prefill. So the shape is two rules deep and no deeper: a category holds **exercises**
  (barbell, dumbbell, machine), an exercise holds **its variations** (paused, touch-and-go, speed,
  competition), and the name of what a row hangs under is read live rather than copied, N58's rule for
  templates — a rename relabels its children and a soft-deleted head still names them. **What is inherited,
  level by level:** a variation inherits the *exercise* it hangs under — its muscles, its equipment, its
  pattern — so only what is performed differently is its own (its step, rest, cue and unit, and every
  record); an exercise inherits its *category's* **primary muscle** and, once the second request lands, its
  **pattern**, but sets its **own equipment**, which is the whole reason barbell, dumbbell and machine bench
  are three exercises under one category rather than one. Secondary muscles default from the category and are
  the exercise's to change. The qualifier stays a **name the lifter writes**: "three-second paused", "speed
  day", "beltless", "with chains" is not a closed set, an enum would need a migration each time the sport
  invents a technique, and grouping by parsing a name invents structure nobody stated that the next rename
  breaks.
  **Settled alongside the shape:** the seeded library **ships categories**, so the first statistic works out
  of the box; so does **the lifter creating their own**, because the seed's families are a starting set and
  not a closed one — otherwise a custom movement could never be filed; an **incline press is its own
  category**, a different movement rather than a different way of performing one; a deleted category still
  names its children, because the soft delete keeps the row for the export (P1.12); and because a category is
  a taxonomy somebody maintains — nothing derives it, and a mis-filed row skews the number silently — moving
  a row between categories has to be easy rather than a re-creation.
  **What the work touches.** `exercises` gains a nullable `parentId` and the mark that makes a row a
  category (stored by name, never an ordinal), added by a migration numbered as it ships, with every existing
  row reading as `parentId = null`. Note the columns: `primaryMuscle`, `equipment` and `movementPattern` are
  non-null today, so a category row carries its family's primary muscle and pattern — honest, since a family
  does have both — and an equipment value that is a placeholder, because a family deliberately spans
  equipment and every child sets its own. Making those columns nullable for the one row kind was rejected: it
  pushes a null check into every reader to spare one row a value it can honestly hold. The library screen grows
  the grouped, folded list, and search finds a family's exercises and variations under the family's name; the
  pickers (a workout's add-exercise and a plan's) must not offer a category; the exercise editor gains *new
  variation of this* and *move to category*; and the seed gains the common families, under the seeder's rule
  that a top-up never undoes what the lifter changed. Both transfer formats carry exercises, so both versions
  move, and B62's rule applies to each: the version is read before the body.
  **What a finished one looks like:** two variations of a barbell bench, a dumbbell bench and a machine press
  filed under one *Bench Press*, each logging its own sets and keeping its own records, with one number that
  sums them.
  **Still open:** how far a roll-up reaches — records and progression stay the exercise's, and which aggregate
  views read the category (muscle-group volume, "how much pressing", the per-lift adherence breakdown, P3.14)
  is the decision. "All bench press volume" is the one it exists for. Also open, and cheaper: **which families
  the seed ships and under what names** — a product choice rather than a code one, and the first cut of it
  decides how much of the library is grouped on day one.
- **Movement patterns get fewer, and move onto the category.** `MovementPattern` holds eleven values —
  HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL, SQUAT, HINGE, LUNGE, CARRY, ISOLATION, CORE,
  OTHER ([ExerciseTaxonomy.kt](app/src/main/java/com/example/androidapp/domain/model/ExerciseTaxonomy.kt)) —
  and the request is fewer of them. **The mapping:** `HORIZONTAL_PUSH` and `VERTICAL_PUSH` become **PRESS**;
  `HORIZONTAL_PULL` and `VERTICAL_PULL` become **PULL**; `SQUAT`, `HINGE`, `LUNGE`, `CARRY`, `ISOLATION`,
  `CORE` and `OTHER` keep their names — nine values where there were eleven. The four directional ones are
  merged because the split buys nothing a lifter asks a question with: "how much pressing" is not two
  questions because one of them was overhead, while the cost is a decision per exercise that nothing checks,
  so a dip is filed one way by one lifter and the other way by the next. **LUNGE stays out of SQUAT**, the
  one judgement inside the set: both are knee-dominant and differ by stance rather than by joint action, but
  "have I been squatting?" is a question a lifter asks and lunges are not the answer to it.
  **The pattern belongs to the category, not the exercise.** Whether a movement is a press or a hinge is what
  its family decides, so declaring it once per category is what stops a movement and its variations
  disagreeing — the same reasoning that has a variation inherit its muscles rather than restate them — and it
  removes a per-exercise field the lifter had to answer. **An exercise in no category carries no pattern**,
  settled that way rather than left open: one fact should have one home, and a loose custom movement is
  exactly the case where "press or pull" says little.
  **What the work touches.** The enum's values; the migration that rewrites the stored names, which is a
  migration over **names, never ordinals**; the exercise editor, where the field leaves, and the category
  editor, where it arrives; the seed's placement of every exercise it ships; and both transfer formats,
  whose versions are read before the body (B62). A retired name must still **read** rather than crash when an
  older row is loaded by a build that no longer offers it — N75's retired Back, which is not offered and
  still loads.
  **Dependency:** this cannot land before the first request, because until categories exist there is nothing
  to hang a pattern on.

Everything that has stood here has shipped — the defects found in use, the workout screen's discard, the
workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch, a rest of
zero, the planned-set prefill, the program document, the eight defects a review of that batch found and
closed (B51-B58), and the seven requests that were its last queue (N80-N86) — each with its entry in
[CHANGELOG.md](CHANGELOG.md). A candidate graduates to *Next* — gaining an id and a spelled-out decision —
when it is picked up, so this queue is where unplanned work waits, and *Parked* below is where deliberate
non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (the app states what happened; it never writes what it decided), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

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
