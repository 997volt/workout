# Workout — Roadmap

> **v1.18** is shipped, and **2.0's changes are on `main` with no release cut**. Last reviewed against the
> code: 2026-10-11 — the N87–N95 batch shipped as 1.17;
> **N96–N102 with B95 shipped as 1.18**, with the six defects a line-by-line reading of that batch found
> (B96–B101); and **N103 and N104 are implemented**, which is the taxonomy becoming nullable and the store
> break that let it happen without a migration — a new database file, the 1.x chain and its schemas deleted,
> both transfer formats refusing 1.x, and the four retired pattern names gone with the files that could carry
> them. The 1.x store is left on the device by design, and the 1.18 APK is still the way to read it.
> Checked against the changelog as well as the code: the unreleased work is under `[Unreleased]` there, every
> shipped id has its entry, and *Next* holds only the cleanup that break entitles 2.0 to (N105, N106). A
> line-by-line reading of the N87–N95 changes found sixteen defects (B79–B94); all are fixed and recorded in
> [CHANGELOG.md](CHANGELOG.md).
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
shipped, with its entry in the changelog: [CHANGELOG.md](CHANGELOG.md) for the current line,
[CHANGELOG-1.x.md](CHANGELOG-1.x.md) for the 1.x releases.

## Next

The taxonomy and the 2.0 break it rode on (N103, N104) are implemented on `main` and wait under
`[Unreleased]` in [CHANGELOG.md](CHANGELOG.md). What remains before the tag is the cleanup that break
entitles 2.0 to.

**N104's rule — a name nothing can reach is a name that should not exist — was applied to the four retired
movement-pattern names and to nothing else.** Four legacy things and two renamed transfer fields are now just
as unreachable, and two decisions a later id reversed still read as binding. **None of it is a migration
while the 2.0 store is unshipped**: schema 1 can be amended in place, by B16's own rule, and the exported
schema regenerated rather than chained — so cutting the tag first is what turns each drop into a migration.

- **The legacy values 2.0's store can no longer hold go** (N105): `MuscleGroup.BACK`, N63's
  `jointPain`/`jointPainNote`, N59's per-set `template_sets.targetRpeHalves` and N6's pre-half
  `rpe`/`targetRpe` transfer fields each exist only so a 1.x row or a 1.x file keeps reading — and 2.0 starts
  a store that never held one and refuses a 1.x file before the decoder. That takes the enum constant, its
  `SELECTABLE_MUSCLE_GROUPS` filter and the reads that still show a row carrying it; the two rating columns
  with the dialog, section and review paths that fall back to them; the per-set effort column with the `?:`
  fallbacks that prefer the exercise's own number; and the renamed DTO fields with their mapping and the
  tests that exercise them. The app-facing rules are N75, N63, N59 and N6; the argument is N104's, and each
  drop is safe for exactly its reason.
- **The decisions N73 and N56 reversed stop reading as binding** (N106). N73 reversed P3.8 and dropped the
  slot's prescription tables, but the P3.8 bullets still describe `program_slot_exercises` and a slot-level
  percentage as live, and N85, N47 and the program document's own doc repeat "what each slot prescribes".
  N56 deleted the template's weekday pin, but P3.3 still says home "falls back to the template pins it
  already reads" and the adherence entries still reason from the pins' missing skip record. The code is
  already right; the decisions, and the comments the two reversals never reached, are what move — and the
  review stamp at the top of [DECISIONS.md](DECISIONS.md) moves with them.

## Parked — deliberately not planned

Each row is a product in its own right, contradicts "local-only", or both. Parking is a
decision, not a backlog, and every row names what would change it. Parked is **not** the same
as the non-goals below: these become possible again the moment their trigger fires, while a
non-goal is a line this app does not cross.

| # | Feature | Revisit only if |
| --- | --- | --- |
| P5.4 | Localization | A non-English user appears. |
| P1.11 | Onboarding: goal, experience level, weekly target | This stops being a single-user local tool with one obvious user. It personalises defaults, and there are no defaults to personalise. |
| P1.17 | Accessibility audit | The per-screen rule stops being enough — a real complaint on a device, or a screen that grew past ad-hoc tagging. The rule still applies to every change; only the sweep is parked. |
| P2.5 | Progress photos | A visual record is actually wanted, and an encrypted-storage design for it is acceptable. |
| P2.8 | Muscle-group balance warnings | Enough history exists for a rolling window to say something true rather than something plausible. |
| P2.6 | Plate calculator | Loading from a plan's target is frequent enough that the arithmetic gets in the way, and you would rather it were done for you. |
| — | **Play Store listing** | You want distribution beyond `adb install`. Self-install works today, and Play App Signing would change who holds the signing key. |
| — | **Encryption at rest / app lock** | You start carrying the phone somewhere you would not carry the data. |

### N39 — the plan's target, parked by decision

The per-metric target shipped; **the plan's target was scoped out** when the feature was
built, so it is parked rather than planned. It is a different shape of work: the training plan
is not one of the statistics screen's sources, so it means making the plan available to a
screen that knows nothing about it, plus a rule for a lift that is in two plans at once or in
none.

## Explicit non-goals

Permanent, unlike *Parked* above: a parked row becomes possible again the moment its trigger fires, while a
non-goal is a line this app does not cross. They are of three kinds — a product in its own right that would
dilute the logging core, a line the hard constraints already draw, and a shape this repository has decided
against — and each row says which, because a refusal whose reason is lost is the one the next person
overturns.

| # | Non-goal | Why it is permanent |
| --- | --- | --- |
| — | Nutrition / calorie tracking | A product in its own right. The app states the energy implication of a weight trend it already owns (N98) and stores no food and no intake. |
| P3.7, P5.2 | Social feeds, friends and shared routines | Accounts, servers and moderation would have to be owned, and a feed needs a public. |
| — | Live GPS route tracking | A different product: this one logs sets, not distance. |
| — | A web dashboard | Statistics is where the log becomes useful; sending health data to a second place to read your own trend is worse privacy and worse UX, not a division of labour. |
| P4.1 | Health Connect read/write | A sharing integration. This app stores data for its user, and the export file is the only path off the device. |
| P4.2 | Foreground service | The app declares no permissions and holds no background work; a rest timer that needs a service to survive is the service's product. |
| P4.3 | Home-screen widget | A second surface is a second product to keep true. The app is opened and used. |
| P4.4 | Quick Settings / launcher shortcuts | One entry point is the app; a shortcut is the launcher's job rather than a second way in. |
| P4.5 | Wear OS companion | It needs `play-services-wearable`, and the no-Google-Play-services line is permanent. |
| P4.6 | Bluetooth heart-rate straps | That is heart-rate training rather than logging, with a sensor between the lifter and the log. |
| P4.7 | WorkManager reminders | The app never pursues its user: no notification permission, and nothing that asks to be opened. |
| P4.8 | Large-screen layouts | One layout, deliberately — a second is a second surface to design, test and hold to the per-screen accessibility rule. |
| P4.9 | Offline-first sync | It needs a backend and accounts, and the no-`INTERNET` line forbids the first; the largest irreversible commitment this app could make. |
| P5.3 | Monetization / Play Billing | Play Billing is a Play-services dependency, which the hard constraints forbid. |
| F6 | Module split into `:core:*` / `:feature:*` | One module is the shape, and the trigger the row named — a second surface such as Wear or a widget — is ruled out above, so the named goal it was waiting to become cannot arrive. |
| F11b | Product analytics | It buys nothing on a single-user local tool and would breach the no-`INTERNET` line. |

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

Everything that has stood in *Next* has shipped — the defects found in use, the workout screen's discard, the
workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch, a rest of
zero, the planned-set prefill, the program document, the eight defects a review of that batch found and
closed (B51-B58), the seven requests that were its last queue (N80-N86), the N87–N95 batch, and this round's
N96–N102 with B95, the six defects a review of *it* found (B96-B101), and the taxonomy with the 2.0 break it
rode on (N103, N104) — each with its entry in [CHANGELOG.md](CHANGELOG.md).

**The only thing left in *Next* is the cleanup above.** The release that carries 2.0 will need the loudest
note this project has written — the 1.x store stays on the device and 2.0 does not read it — and the rule
that allows that is in [DECISIONS.md](DECISIONS.md), with the window it is payable in.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
