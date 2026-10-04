# Workout — Roadmap

> **v1.10** is shipped and installed. Last reviewed against the code: 2026-10-04.
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

Nothing is planned. The deferred scope of the last two rounds — P3.3's and P3.5's, programmed as
P3.8-P3.16 — was built and shipped at 1.10, with its entries in [CHANGELOG.md](CHANGELOG.md). A candidate
graduates to this section — gaining an id and a spelled-out decision rather than a wish — when it is
picked up, so an empty *Next* is a state rather than a gap: the two queues below are where unplanned
work lives, *Later* for what is self-contained and *Parked* for what is a product in its own right.

## Later (still self-contained)

Post-MVP on the same local-only premise, grouped by theme and ordered by value inside each: a
candidate graduates to *Next* — gaining an id and a spelled-out decision — when it is picked
up, and leaves for [CHANGELOG.md](CHANGELOG.md) when it ships.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (N22's "the app suggests; it never writes"), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed. What waits now came
from using the built app rather than from either queue.

### Rest: making "no rest" expressible

- **A rest of zero becomes a valid, deliberate answer.** The field has two states today and needs
  three: empty means *inherit* (an exercise takes the app default, a template takes the library's,
  a slot takes the template's), a positive number is that rest, and zero is **refused** — in three
  repositories, with two different sentences for one rule ("Rest must be a positive number of
  seconds." twice, "A prescribed rest must be at least a second." once), and as a red field in the
  library editor, the template plan editor and the slot prescription dialog. So "this exercise
  needs no rest" cannot be written down at all, and a zero reads as an error when it is a perfectly
  ordinary intention. The ask is that zero is a value: the field stops calling it one, and it keeps
  meaning what it already means downstream — `startRest` computes an end instant at *now*, so a zero
  rest simply never runs, and nothing collapses zero into "absent", because both `?:` and the DAO's
  `COALESCE` test null rather than falsiness. That makes this a validation-and-wording change rather
  than plumbing, which is also why it is small enough to be worth doing properly.
- **What it has to settle when it is taken.** The rule it reverses is written down — N5 says "a rest
  of zero is not a rest; leaving it unset is how 'use the default' is expressed" — so that decision
  moves rather than being quietly broken, and the three sentences above collapse into one. The hint
  under the field has to carry both halves ("leave empty for the default, 0 for none"), and the
  *display* needs a word rather than a number: a stored zero renders as `0:00` today, which reads as
  a rest that has run out instead of one that was never wanted.
- **It is not the rest-timer switch, and the two should stay distinct.** The switch above is "stop
  counting me down, but show me the prescription"; a zero is "this exercise has no rest to count".
  One is a preference about the timer, the other a fact about the exercise. For the same reason zero
  stays out of the *default* rest's choices, where the Settings screen already refuses to offer it
  and says why in a comment — a typed zero there would be an alarm that fires instantly, and an
  app-wide zero would take the rest out of every exercise at once, which is the switch's job.

### Templates and plans: how a planned set gets written

One screen's problem, and this candidate removes a feature rather than adding one.

- **The Duplicate button goes; Add set prefills from the exercise's last planned set.** The
  *Planned sets* dialog offers Add set — a blank form — beside Duplicate, which appends a copy of
  every set that exercise already has. Duplicate is a loop wearing a button: it doubles (1, 2, 4,
  8), so three sets or five sets always end in manual adds, and what it stands in for is a
  *prefill* rather than an operation of its own. It also appends the whole plan, ramp included,
  into an order this app went out of its way to make reliable — `setIndex` is stored rather than
  inferred, and B34 exists because a generated ramp once landed after the work it was written to
  prepare for. So it is deleted, and Add set starts from the last set instead of from nothing,
  blank only while the exercise has no sets. That is one expression at the call site, because the
  dialog already takes an initial value and the conversion already exists; and it is strictly
  better than what it replaces — any count including the odd ones, appended in place after the
  work, with the ramp left where Add warm-ups put it. The role prefills too, which is safe
  precisely because Add warm-ups *prepends*: the last set is the last working set, and a user who
  authored a warm-up last is asking for continuity rather than being surprised by it. The removal
  is a whole path rather than one button — the repository method, its DAO read, the tag, the
  string, and the test that only exercises it — and the empty-plan sentence that advertises the
  button has to be reworded with it.
- **The same rule on the prescription side, or the inconsistency simply moves.** Sets are authored
  in two places and they disagree today: a template's plan dialog has the blank form *and* the
  button, while a slot's prescription dialog has the blank form and no button at all. Removing the
  button and prefilling only the template side leaves the worse half of that — neither surface can
  bulk-copy, and one of them still makes the user retype. Both take the prefill, which makes it one
  rule: a new set starts from the last set. That side needs a lookup rather than a one-liner,
  because its sets hang off the editor instead of sitting in local scope.
- **The cost, accepted.** Prefilled values look exactly like saved ones, so "Add set" then Save
  without touching a field is a plausible accidental double-add. The title already says it is
  adding and the list behind it shows the count, so this is accepted rather than marked; a
  set-*count* control would be the more honest affordance if it ever needs revisiting.

### Programs: loading one from a file

- **Load a program, with its templates, from a file.** Programs and their templates already ride in
  the backup, and import already merges rather than overwrites — so what is missing is not a reader
  but a *document*: there is no way to carry one program to another device, or to accept one
  somebody else wrote, without moving the whole database. Two things make it more than a file
  picker bolted to the Programs screen. The format has to survive being partial — four collections
  in the backup are required rather than defaulted, so a program-only file cannot simply be a small
  backup — and it has to name its exercises portably: a template references exercise ids, the
  seeded library's ids are permanent slugs that are the same everywhere, and an exercise the *user*
  created carries a generated id that means nothing on the receiving device. So the candidate
  carries two decisions: a program document with a version of its own, reusing the backup's DTOs
  and mappers where they fit, and a rule for an exercise the receiving device does not have. Both
  are why this is the largest of the three, and both are worth paying: a program is the part of
  this app a lifter would actually want to hand to someone.

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
