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

One batch, taken from using the app rather than from either queue: the tab that starts a session, the
screen one runs on, how a set is written from it, where a weekday lives, and what history shows. A
candidate graduates to this section — gaining an id and a spelled-out decision rather than a wish —
when it is picked up, so what stands here is committed work; the two queues below are where the rest
lives, *Later* for what is self-contained and *Parked* for what is a product in its own right.

### The active workout's Log set control

- **N49, N52 — the Log set control's colour, and what it says once an exercise's plan is done.** *Log set*
  is a `FilledTonalButton` whose container is the palette's `secondaryContainer` (Teal): against the
  near-black page that is the loudest thing on the screen while being only the next step (6.8:1 against the
  page), and its label is worse than the problem — `onSecondaryContainer` is white, and white on Teal
  measures 2.9:1. The links are the opposite end: a `TextButton` draws in `primary` (Indigo), which measures
  4.07:1 against the page and 3.77:1 on a raised card — under the 4.5:1 that body-size text needs. Both
  entries are **replaced in the palette**, not worked around by pointing the two call sites at another
  existing token: the palette is where the colour is wrong, and a patch at the call site leaves the next
  control that reads the same role wrong again. `secondary` and `TileAccent.Teal` are separate entries and
  stay as they are, so the category colour does not move with the button, and *Superset with above* is not
  among the links this entry touches — N53 turns it into an overflow menu entry, so its colour is that
  change's to answer for. Two constraints on the replacement: `secondaryContainer` is also the rest timer's
  surface, so a quieter container moves that bar too (its label is white on the same Teal, the same 2.9:1),
  and `primary` is the app's own colour — the Start pill, the selected tab, the charts — so if the violet
  stays for filled surfaces, the links get a role of their own rather than `primary` being redefined under
  the pill.

  The same control carries the end of the exercise's plan. The plan is the template the workout was started
  from — a program's slot and a template chosen directly arrive as the same `templateId` — so a workout
  started from a template of either kind gets the notice, and an empty workout, with no plan behind it, has
  nothing to notice. Past the last planned set the exercise keeps accepting sets with nothing to say its
  work is done, and `comparePlanToActual` says so only in the review, after *Finish* — so the moment the
  last of that exercise's planned sets is written, *Log set* becomes **Log extra set** and a notice says the
  planned work is done. Nothing closes: logging an extra set is what the button still does. Accepting the
  notice is the **Done** button already in the exercise's header (N7), so there is no second "finish" and no
  dismissal of its own. It is per exercise, and it is the control rather than a dialog: N51 already puts a
  dialog in front of every set, and a second one would interrupt the next exercise's first set. The notice
  stays in-app: the app declares no notification permission and B37 removed the channel it used to create.
  The contrast is asserted rather than eyeballed, the way `TileAccent.onColor` already is (B55).

### Logging a set

- **N51 — logging a set opens the dialog that editing one opens.** *Log set* writes the offered set in one
  tap, so a set that differs from the prefill is logged and then edited — the same `SetEditorDialog`
  reached one step later, with the first step having decided something the user did not mean. Logging *is*
  that dialog, prefilled from the same offer and committed on Save, and the one-tap path goes: it is not
  wanted any more, which is the decision rather than a cost to weigh. The dialog already carries the role
  selector (N14), so the armed-role picker beside the button goes with the one-tap path, and the plan's
  next unlogged set becomes the dialog's initial role the way B48 already arms it. That inverts B7 for this
  button — it no longer writes the set its label describes, because the label no longer describes one.

### Running a workout

- **N53 — an exercise's rare actions move into its own overflow menu.** *Superset with above* is a text
  button in every exercise header and *Delete* an icon beside *Done*; both are rarely used, and the header
  is read constantly mid-session, so the two of them cost more attention than they earn. They move into a
  per-exercise ⋮ menu, the shape the workout-level actions used until N42 removed the one that no longer
  had a reason to exist. Delete keeps its confirmation (B2) and pairing with the exercise above keeps its
  row-0 exclusion (B28), because the action moved rather than changed. The moved superset item becomes a
  menu entry rather than a link, so its colour is this change's to answer for — N49 recolours only the
  links that stay on the screen.

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

### Templates and programs: where a day lives

- **N56 — a template carries no weekday; the day belongs to a program's slot.** A template is a reusable
  workout, and today it also holds an N16 weekday pin of its own: its editor offers a day picker,
  `templates.weekday` stores it, and *Today* on home falls back to those pins whenever no program is
  active. That is two places answering "what am I doing on Tuesday", and the template's copy is the
  weaker one — a template has no order, no next-up and no adherence to belong to. The pin goes, with its
  column, the `setWeekday` path through DAO, repository and editor, and the `pinnedFor` fallback in
  `todaysPlanFor`, leaving a program's slots as the only source of a dated plan. With no active program
  there is then no *Today* list, which is the point rather than a regression: a day is a scheduling fact,
  and scheduling is what a program is for. The loss is accepted rather than mitigated: a template pinned to
  a day today comes out of the migration with no day at all, and getting the schedule back means putting it
  in a program — which is the rule being stated, not a migration that failed. Nothing settled is
  contradicted — N16's "a scheduled plan is a living template" survives with the *slot* as the scheduled
  thing, and P3.3's weekday-less, order-only slot is untouched. Two edges are settled when it is taken:
  dropping the column is a migration that ships with the code that stops reading it (never ahead of it),
  and the backup DTO carries the same weekday, so whether that field stays for older files' sake or leaves
  with the column is decided there.

### History

- **N57 — a history row carries the weekday, not only the date, and home's recent rows with it.** The row's
  headline becomes **`Sun, Oct 4, 2026`**: the short weekday and then the `MEDIUM` date the row already
  shows, built from the locale's own names (`TextStyle.SHORT`, the way `HistoryFormat.month` uses
  `TextStyle.FULL`) rather than from a hard-coded English pattern, and read in the session's own zone (N25,
  B38). The weekday is the part a lifter navigates by — which day of the week this was — while the month
  header above already carries the month the date repeats, which is why the short form is enough. The
  formatter is shared with home's *Recent* row, so one change moves both surfaces and a finished workout
  reads the same way wherever it is listed.

- **N58 — a workout started from a template shows that template's name in history.** The session already
  stores `templateId` when it starts from a template — a program's slot included — but the history
  projection never selects it, so a finished *Push A* and a finished empty workout are indistinguishable in
  the list, and the name is the half of the row that says what the session *was*. The row keeps the weekday
  and date as its headline (N57), and the name joins the supporting line beside the duration, sets and
  volume. Four items is more than that line holds on a phone, so it **wraps** rather than truncating:
  `AppRow`'s supporting `Text` carries no `maxLines` today, and an ellipsis on the one part that cannot be
  inferred from the workout would hide exactly what the change is for. It is read **live** from the template
  row, so renaming a template relabels the past — accepted, because N16's template is living and the
  workout's own identity is when it happened, which the headline carries. Deletion is the softer case and
  the same read answers it: `deleteTemplate` is a soft delete, so
  the row and its name are still there and a past workout goes on saying which workout it was — the app
  already keeps a deleted template for the export (P1.12), and hiding it from history would take an
  explicit filter this change does not add. Snapshotting the name onto the session is rejected: it costs a
  column and a migration and changes only what a *rename* does, which is the half already accepted. One
  edge: a *repeat* (N48) starts a session with no `templateId` today, so a repeated workout shows no name —
  whether the repeat should carry its source's template id is settled with the change.

## Later (still self-contained)

One candidate waits here with an id rather than as a bullet: the *one more rep than last time* rework
(N50), picked up with the batch and put back once it turned out to touch the machinery the rest of the
batch was still settling. Everything else that stood here has shipped — the defects found in use, the
workout screen's discard, the workouts tab cut back, repeat-last in History, Settings' data section and
rest-timer switch, a rest of zero, the planned-set prefill, the program document, and the eight defects a
review of that batch found and closed (B51-B58) — each with its entry in
[CHANGELOG.md](CHANGELOG.md). A candidate graduates to *Next* — gaining an id and a spelled-out
decision — when it is picked up, so this queue is where unplanned work waits, and *Parked* below
is where deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (N22's "the app suggests; it never writes"), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

### The proposal to progress

- **N50 — "one more rep than last time" is reworked.** The proposal and the sentence that carries it (N22,
  N33) are confusing as they stand, and they are the machinery the batch's own entries name: the offer the
  links beside the button lead to, and the values the logging dialog prefills from (N51). So the
  replacement is deliberately not decided here and is spelled out when this is picked up; what is committed
  now is that the current shape does not survive it.

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
