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

**Nothing.** The batch that stood here — the palette's two roles and the end of an exercise's plan
(N49, N52), logging through the editor (N51), an exercise's own menu (N53), its order during a session
(N54), *Next up* in the bottom bar (N55), the template's weekday going to the program (N56), and the
weekday and template name in history (N57, N58) — is built, and each entry is in
[CHANGELOG.md](CHANGELOG.md). A candidate graduates to this section — gaining an id and a spelled-out
decision rather than a wish — when it is picked up, so what stands here is committed work; the two
queues below are where the rest lives, *Later* for what is self-contained and *Parked* for what is a
product in its own right.

## Later (still self-contained)

One candidate waits here with an id rather than as a bullet: the *one more rep than last time* rework
(N50). It was put back while the batch was still settling the machinery it touches, and N59 has since
withdrawn the offer it described along with the rule that computed it — so what is left is a proposal to
design rather than a wording to adjust. Everything else that stood here has shipped — the defects found in use, the
workout screen's discard, the workouts tab cut back, repeat-last in History, Settings' data section and
rest-timer switch, a rest of zero, the planned-set prefill, the program document, and the eight defects a
review of that batch found and closed (B51-B58) — each with its entry in
[CHANGELOG.md](CHANGELOG.md). A candidate graduates to *Next* — gaining an id and a spelled-out
decision — when it is picked up, so this queue is where unplanned work waits, and *Parked* below
is where deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (the app states what happened; it never writes what it decided), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

### The proposal to progress

- **N50 — "one more rep than last time" is reworked.** The proposal and the sentence that carried it
  (N22, N33) are gone: N59 withdrew the offer, its *Use it* link and the double-progression rule behind
  it, because the next set's values are now fields the lifter reads and edits before committing. What
  the app should propose instead — a number beside those fields, a chip, nothing at all — is
  deliberately not decided here and is spelled out when this is picked up; what is committed now is that
  the withdrawn shape is not the one to restore.

### Two requests from use

Self-contained enough for this queue, and each small enough that its decision is spelled out when it is
picked up rather than now — so each is a wish, and gains an id when it graduates.

- **The rest field stops explaining itself, except where the exercise is edited.** The sentence under
  every rest field — `rest_edit_hint` in [strings.xml](app/src/main/res/values/strings.xml), "Empty for
  the default, 0 for none." — reads as noise on the template's field
  ([TemplateEditorScreen.kt](app/src/main/java/com/example/androidapp/ui/templates/TemplateEditorScreen.kt))
  and on a program slot's
  ([SlotPrescriptionDialog.kt](app/src/main/java/com/example/androidapp/ui/programs/SlotPrescriptionDialog.kt)),
  so it comes off those two. It **stays on the exercise's own field**
  ([ExerciseDetailScreen.kt](app/src/main/java/com/example/androidapp/ui/exercises/ExerciseDetailScreen.kt)),
  which is where a rest is defined rather than restated, and that is the whole decision: one string under
  three fields becomes one string under one, and the comment in
  [RestTimer.kt](app/src/main/java/com/example/androidapp/domain/RestTimer.kt) that calls it the hint
  under *every* rest field is updated with it.

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
