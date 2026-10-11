# Workout — Roadmap

> **v2.0** is shipped ([CHANGELOG.md](CHANGELOG.md)), **P1.1's library set, B103 and N110 are on `main` with no
> release cut**, and *Next* holds N107 beside the defects a reading of that batch found. Last reviewed against
> the code: 2026-10-11 — the N87–N95 batch shipped as 1.17; **N96–N102 with B95 shipped as 1.18**, with the six
> defects a line-by-line reading of that batch found (B96–B101); **N103–N106, N108, N109 and B102 shipped
> as 2.0**: the taxonomy becoming nullable, the store break that let it happen without a migration — a new
> database file, the 1.x chain and its schemas deleted, both transfer formats refusing 1.x, and the five
> retired enum names (four patterns and `BACK`) gone with the files that could carry them — the rest of the
> legacy the new store cannot hold, the decisions a later id reversed that were still reading as binding, the
> two columns and one transfer field nothing read while schema 1 was still unshipped, and the measurements
> screen's silent write failure; and **B103 with N110 is implemented** — the session and template reads no
> longer throw on an exercise that states no muscle, and the library became a category over movements, with a
> movement reading its family's pattern and muscles — **in the same unreleased batch as P1.1's strength and
> bodybuilding set**, 61 movements joining the 45 and the picker grouped exactly as the library is. The 1.x store
> is left on the device by design, and the 1.18 APK is still the way to read it.
> Checked against the changelog as well as the code: every shipped id has its entry, and *Next* holds N107,
> which shortens [DECISIONS.md](DECISIONS.md) without dropping a decision, **beside the five defects a
> line-by-line reading of the unreleased P1.1, B103 and N110 changes found (B104–B108)**. A line-by-line reading
> of the N87–N95 changes found sixteen defects (B79–B94); all are fixed and recorded in
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

One planned change, and it is text rather than code — with the five defects a reading of the unreleased batch
found queued beside it.

- **The longest evidence-backed entries state the rule and defer their argument to the evidence** (N107).
  The file's own contract is that an entry is the rule, the shortest honest reason and the rejected
  alternative, with the longer argument in [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md). Twelve entries
  that already have an evidence section run to 13–26 lines and carry the full argument anyway; enforcing
  the contract on those twelve moves it to the section that exists for it, losing no decision. It is a text
  change rather than a code one, so it can land before or after the tag.

### Defects found in review

The unreleased P1.1, B103 and N110 changes were read line by line against what [CHANGELOG.md](CHANGELOG.md) says
they do, and then run on a device, because the gate they were checked with does not execute the instrumented
suite the migration and the DAO projections live in. The reading found one defect that turns that suite red, one
branch the batch left with no production caller, and three places where the prose states something the code or
the data does not.

- **The program document's import flattens a movement under a movement, and its test still asserts the old link**
  (B104). N110's shape rule walks a movement filed under a movement up to the first category and folds what it
  read on the way ([ExerciseLibrary.kt](app/src/main/java/com/example/androidapp/domain/ExerciseLibrary.kt)),
  which is the rule [DECISIONS.md](DECISIONS.md) sets for a file an older build wrote; the import applies it to
  a program document's library
  ([BackupMappers.kt](app/src/main/java/com/example/androidapp/data/transfer/BackupMappers.kt),
  [RoomProgramRepository.kt](app/src/main/java/com/example/androidapp/data/RoomProgramRepository.kt)). But
  `aDocumentCarryingAVariation_carriesItsAncestors`
  ([ProgramDocumentTest.kt](app/src/androidTest/java/com/example/androidapp/data/ProgramDocumentTest.kt)) still
  asserts the N103 parent — `paused-bench` under `barbell-bench-press` — and fails with *expected
  `barbell-bench-press` but was `cat-bench`*. The production half is the intended shape and the test is the stale
  half, which is what makes it a defect: the batch leaves `connectedDebugAndroidTest` red. A full run on
  `pixel6_api36` reports 268 tests and this one failure, and `testDebugUnitTest` — the gate the batch was checked
  with — never compiles that source set, so nothing in the default loop notices.
- **The picker's flat branch is unreachable from production** (B105). `libraryRows`'s `grouped` parameter
  ([ExerciseLibrary.kt](app/src/main/java/com/example/androidapp/domain/ExerciseLibrary.kt)) exists, by its own
  KDoc, because "the pickers are why this exists", but the picker was moved to the grouped list in this batch
  ([ExercisePickerScreen.kt](app/src/main/java/com/example/androidapp/ui/workout/ExercisePickerScreen.kt),
  [ExercisePickerViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ExercisePickerViewModel.kt)),
  so nothing under `main` passes `grouped = false` and the branch's only caller is
  [ExerciseLibraryTest.kt](app/src/test/java/com/example/androidapp/domain/ExerciseLibraryTest.kt), whose
  comment still calls it "the picker's flat branch". AGENTS.md's rule is to delete an API the moment nothing
  calls it, so the parameter and the branch go together, and the test with them.
- **The lunge family is documented as stating no primary muscle, and the seed states one** (B106).
  [DECISIONS.md](DECISIONS.md), [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md) and
  [CHANGELOG.md](CHANGELOG.md) all say the lunge family "holds no single primary muscle, so each of its two
  movements states its own", and [SeedExercises.kt](app/src/main/java/com/example/androidapp/data/SeedExercises.kt)'s
  own KDoc repeats it. The seed gives the family `MuscleGroup.QUADS` and files **four** movements under it, of
  which only `walking-lunge` and `reverse-lunge` state a primary — `bulgarian-split-squat` and `step-up` read the
  family's. The sentence is wrong twice: the family does state a primary, and it holds four movements rather
  than two. Either the prose or the seed has to move, and the `SeedCategory` type cannot express an unstated
  primary, which is what the Olympic-lift note already records.
- **The seed's own doc says four where three of each value was added** (B107).
  [SeedExercises.kt](app/src/main/java/com/example/androidapp/data/SeedExercises.kt) says "the four equipment
  values that had no exercise of their own — `KETTLEBELL`, `BAND`, `OTHER`" and "the four muscles that had no
  movement of their own — `FOREARMS`, `ADDUCTORS`, and `LOWER_BACK`". Both lists hold three: 2.0's seed used
  five of the eight equipment values, and only those three muscles were no movement's primary.
  [CHANGELOG.md](CHANGELOG.md) has the count right, so the source KDoc and the entry it belongs to disagree.
- **The migration does not clear a cycle, though the evidence says it does** (B108).
  [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md) says a cycle is "the one case cleared rather than folded" and
  that "the device's own store is carried across the same way by a migration", but
  [LibraryMigrations.kt](app/src/main/java/com/example/androidapp/data/local/LibraryMigrations.kt) reparents in
  one hop with no `seen` guard, so a two-row cycle becomes two self-parents rather than two cleared links; only
  the import path's shape rule carries the guard. The app refuses to write such a store, so nothing is broken in
  use — the defect is the evidence claiming a guard the migration does not have, and
  [LibraryMigrationTest.kt](app/src/androidTest/java/com/example/androidapp/data/local/LibraryMigrationTest.kt)
  passing over the case it claims rather than covering it.

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
closed (B51-B58), the seven requests that were its last queue (N80-N86), the N87–N95 batch, the N96–N102
batch with B95 and the six defects a review of *it* found (B96-B101), and 2.0 — the taxonomy and the store
break (N103, N104), the legacy and dead weight it left behind (N105, N106, N108, N109) and the measurements
screen's silent write failure (B102) — each with its entry in [CHANGELOG.md](CHANGELOG.md).

***Next* holds N107 and the five defects the review recorded (B104–B108)**. **B103 and N110 landed together** —
the defect first, because the rework leans on what it fixed — and the reading that checked them found one that
turns the instrumented job red: a program document's variation now imports under the family rather than under the
movement it was a version of, so the test still asserts a link the shape no longer has (B104). 2.0 shipped with
the loudest note this project has written — the 1.x store stays on the device and 2.0 does not read it — and the
rule that allowed that is in [DECISIONS.md](DECISIONS.md), with the window it stays payable in.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
