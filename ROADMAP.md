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

Every candidate that stood here has shipped — the defects found in use, the workout screen's
discard, the workouts tab cut back, repeat-last in History, Settings' data section and rest-timer
switch, a rest of zero, the planned-set prefill, and the program document — each with its entry in
[CHANGELOG.md](CHANGELOG.md). What stands here now is not another candidate from using the app: it
is a reading of that work, taken line by line after it was written rather than by using it. A
candidate graduates to *Next* — gaining an id and a spelled-out decision — when it is picked up,
so an empty queue is a state rather than a gap, and *Parked* below is where deliberate non-work
lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (N22's "the app suggests; it never writes"), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

### Defects found in review

The N41-N48 batch and the restyle were read line by line, and the reading found these. All but
the first are small; the first is the only one that loses anything, and it loses it while saying
something untrue about why.

- **A program document calls an exercise the device has deleted "not on this device"** (B51).
  `mergeProgramDocument` asks whether an exercise is present with `ExerciseDao.findById`, which
  filters `deletedAt IS NULL`, and the insert beside it is `INSERT OR IGNORE`. So a row the device
  holds but has *soft*-deleted is skipped by the insert *and* judged absent by the read: its
  movements are dropped and reported as exercises "not on this device", which is false. The backup's
  own import answers the same question the other way — `restoreSoftDeleted` un-deletes what the file
  has live — so the two paths disagree about what a hidden row means, and neither the disagreement
  nor the drop is written down. Whatever is chosen has to be true in the sentence and argued in
  DECISIONS: resurrecting a lift the user deleted is a write to their library, so the likely answer
  is that the movement is dropped and *said* accurately.
- **The document's export and load callbacks default to null** (B52). `ProgramEditorScreen`'s
  `onExportProgram` and `ProgramsScreen`'s `onLoadProgram` are `(() -> Unit)? = null`, so a route
  that forgets to wire one draws no control and fails nothing. That is the shape B49 was written
  against in this same batch — "a callback a screen cannot work without has no default" — and the
  screen tests pass the callback in rather than going through the route, so they cannot catch it.
- **The Storage Access Framework document IO is written twice** (B53). `ui/transfer`'s
  `DataTransferActions` and `ui/programs`' `ProgramFileActions` each carry their own
  `writeText`/`readText`, their own `JSON_MIME_TYPE` and their own `DataError`-to-sentence mapping.
  This is the second caller the rule says to extract at — the same rule the restyle applied to
  `FailureMessage` one file over.
- **The new shared components carry parameters nothing passes** (B54). `SectionHeader`'s `trailing`
  slot and `testTag`, and `IconTile`'s `contentDescription`, are referenced by no caller. `trailing`
  is the vestige of the "See all workouts" control N42 removed; `contentDescription` is documented
  as the exception case, and no tile is one.
- **A tile's glyph is white on accents where white does not read** (B55). `IconTile` tints every
  icon `Color.White`, which against `Amber` is 2.5:1, `Teal` 2.9:1 and `Coral` 3.0:1 — under the
  contrast a graphic needs, and less legible than the dark glyph the same tile would take.
- **A loaded prescription is never checked against what its template trains** (B56). The
  interactive writes enforce P3.8 through `requireExerciseInTemplate`; the import writes raw rows
  through the backup DAOs, so a slot whose template id already exists on the receiver — and has
  since diverged — can be given a prescription for a movement that template no longer holds.
- **"That program is already here" is decided from the program count alone** (B57).
  `importSentence` asks `summary.programs > 0`, so a load that added templates or exercises but not
  the program row itself — its id already present, its templates not — is announced as having added
  nothing.
- **A negative rest is refused with a sentence that repeats the field's hint** (B58).
  `RestTimer.NEGATIVE_REST_REFUSAL` carries "Leave it empty for the default, or 0 for none." while
  its own doc says the hint carries the construction, and `rest_edit_hint` says the same thing — one
  rule stated in two files, free to drift apart.

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
