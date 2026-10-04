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

One batch, taken from using the app rather than from either queue, and all of it the workouts half:
the tab that starts a session, the screen one runs on, and how a set is written from it. A candidate
graduates to this section — gaining an id and a spelled-out
decision rather than a wish — when it is picked up, so what stands here is committed work; the two
queues below are where the rest lives, *Later* for what is self-contained and *Parked* for what is a
product in its own right.

### The active workout's look

- **N49 — the Log set button and the screen's links are re-coloured against the one theme.** *Log set* is
  a `FilledTonalButton`, and on the dark page it reads as the loudest thing on a screen where it is only
  the next step; the clickable text beside and above it — *Done*, *Reopen*, *Superset with above*, *Use
  suggestion* — sits at the other end, too dim to read as tappable. The complaint is the pair, so both
  move: the log action to a quieter container than the primary pill, the links to a contrast that can be
  read without hunting. Rejected: restyling only one of the two, which would move the imbalance rather
  than settle it, and editing the palette itself, because the palette is the app's and the defect is
  which token these controls reach for.

### Logging a set

- **N50 — "one more rep than last time" is reworked.** The proposal and the sentence that carries it
  (N22, N33) are confusing as they stand. The replacement is deliberately not decided here and is spelled
  out when this is picked up; what is committed now is that the current shape does not survive it.

- **N51 — logging a set opens the dialog that editing one opens.** *Log set* writes the offered set in one
  tap, so a set that differs from the prefill is logged and then edited — the same `SetEditorDialog`
  reached one step later, with the first step having decided something the user did not mean. The button
  opens that dialog prefilled from the same offer and commits on Save. The one-tap path goes with it, and
  the cost is accepted rather than marked: every set takes a confirmation now, because the set is the
  record and the record is worth reading before it is written. That inverts B7 for this button — it no
  longer writes the set its label describes, because the label no longer describes one.

### Running a workout

- **N52 — the workout says when the planned work is done.** Starting from a program's slot seeds the
  plan's exercises and their planned sets, and logging has no ceiling: past the last planned set the user
  can keep logging with nothing to notice the day is complete, and `comparePlanToActual` says so only in
  the review, after *Finish*. The notice is a **popup** — the app's own in-app dialog, the shape *Done*'s
  rating prompt and the remove-exercise confirmation already use — raised the moment the last planned set
  is written, never a system notification: the app declares no notification permission and B37 removed the
  channel it used to create. It says the planned work is done, and what it offers — finish the workout,
  keep logging, or nothing but a dismissal — is settled when it is picked up, as is whether the rest
  chime's tone accompanies it.

- **N53 — an exercise's rare actions move into its own overflow menu.** *Superset with above* is a text
  button in every exercise header and *Delete* an icon beside *Done*; both are rarely used, and the header
  is read constantly mid-session, so the two of them cost more attention than they earn. They move into a
  per-exercise ⋮ menu, the shape the workout-level actions used until N42 removed the one that no longer
  had a reason to exist. Delete keeps its confirmation (B2) and pairing with the exercise above keeps its
  row-0 exclusion (B28), because the action moved rather than changed.

- **N54 — exercises can be reordered while the workout runs, without touching the template.** Order
  matters mid-session — a rack taken, equipment moved — and today the only way to change it is to edit the
  template, which rewrites every future run for a reason that belonged to one afternoon. The session's own
  order becomes editable from the workout screen and the template is never written, which is N16 ("a
  template is living, and a session reads it at the start") applied to order rather than to targets.
  Whether the new order is persisted with the session, and what it does to an exercise inside a superset
  group, is settled when it is picked up.

### The workouts tab

- **N55 — a program's *Next up* moves to the bottom, under *Start workout*, and the field itself opens
  what is planned.** The next-up card sits in the scrolling list between today's plans and *Recent*, so a
  program with nothing scheduled today is something the user has to scroll to, and the only thing in it
  that responds is the *Start* button. It moves into the bottom bar beneath the start pill — the edge of
  the screen the thumb is already at, where the primary action lives — and leaves the list, because one
  program's next run shown twice is two answers to one question. Tapping the field opens the planned
  workout; *Start* keeps starting it, so looking and starting stop being the same gesture. What the field
  opens is settled when it is picked up: a template's only destination today is its **editor**, so the
  change chooses between a read-only preview and opening that editor, and more than one active program
  (P3.12) means the bar may have several next-up rows to fit — which is why the row stays compact rather
  than a card.

## Later (still self-contained)

Empty. Every candidate that stood here has shipped — the defects found in use, the workout screen's
discard, the workouts tab cut back, repeat-last in History, Settings' data section and rest-timer
switch, a rest of zero, the planned-set prefill, the program document, and the eight defects a
review of that batch found and closed (B51-B58) — each with its entry in
[CHANGELOG.md](CHANGELOG.md). A candidate graduates to *Next* — gaining an id and a spelled-out
decision — when it is picked up, so an empty queue is a state rather than a gap, and *Parked* below
is where deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (N22's "the app suggests; it never writes"), a weekday-less slot that is never missed
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
