# Workout — Roadmap

> **What is planned, not what happened.** *Next* is what is being worked on, *Later* is decided and waiting,
> *Parked* is decided against until a named trigger fires, and the non-goals are lines this app does not cross.
> Shipped work is [CHANGELOG.md](CHANGELOG.md), how a release is cut is [RELEASING.md](RELEASING.md), and the
> settled rules are [DECISIONS.md](DECISIONS.md) with their argument in
> [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).

A local-only training notebook: write a plan, log what you actually did against it, and let
the app show you the difference and what to do next.

Ids (`F#` foundations, `B#` defects, `N#` the next planned changes, `P#.#` the product
backlog, `R#.#` releases) are stable and go in commit messages. They were assigned when the
work was planned, so they do not always run in order — and an id this file does not list has
shipped, with its entry in [CHANGELOG.md](CHANGELOG.md) for the current line or
[CHANGELOG-1.x.md](CHANGELOG-1.x.md) for the 1.x releases.

## Next

**The whole-codebase review.** One pass over the tree on 2026-10-28 — the app, the shared program core, the
import helper's page and the tooling — read against the settled rules and exercised on this checkout. The gate is
green (unit tests, the shared JVM and JS tests, lint, detekt, the debug and R8 assemblies), so these are wiring,
arithmetic, format and coverage gaps the tests do not reach, not build failures. The rows are ordered by severity;
the two format defects (B276, B277) are the sharpest, because they make a legal plan unexportable and re-create
B247 on the helper's host.

| # | What it changes |
| --- | --- |
| B269 | **A restore can refuse or lose a body measurement.** Two defects in one path. The sore rows are inserted unfiltered after the measurements pass a day filter, so a file measurement that yields to a day the device already holds takes its sore rows' foreign key with it and the whole import transaction rolls back — the user's only escape hatch refusing a valid file. Separately, a file's own soft-deleted measurement reserves its day before the live one is reached, so restoring a save/delete/re-enter history into an empty store inserts the hidden row and drops the live one. Filter the sore rows by the measurements that were actually written, reserve days for live rows only, and cover both with a same-day import case that carries soreness. |
| B270 | **A rating's unit disagrees with itself.** `MetricUnit.RATING` formats a value as points, parses a typed number by doubling it into halves, and halves the rate before printing it — while every series it reads is already in points (`RoomTrendsRepository` divides the stored halves, and the chart's own range is 1–10). Typing a target of 7 on an RPE series stores 14, redraws the goal at 14 on a 1–10 axis and seeds the dialog with "14.0"; every rating trend is reported at half its magnitude. Pick points, delete the two conversions, migrate any stored half-point goals, and assert `format(parse(x)) == x` per unit instead of pinning one direction. |
| B271 | **History's *Do it now* starts nothing.** The history host handles only the repeat intent, and the missed-day question's *Do it now* replaces that intent with a template start, so the dialog closes and no workout begins. Home branches on both shapes; history handles one. Branch here too (or carry the repeat through the answer) and test the route's wiring, which `WorkoutHistoryScreenTest` never reaches because it drives the stateless screen. |
| B272 | **The next set's fields never take the history.** The draft is keyed on the row id and its set count, neither of which changes when the asynchronous previous-performance read lands, so the fields keep their 8 × 20 kg fallback while the reason line beside them reads "Last time …". A failed read is worse: it is dropped with no message and never retried, leaving a plausible number for the whole session — the shape B251 refuses for a stale weigh-in. Re-seed an untouched draft when the suggestion changes, and keep the failure so the row can say history is unavailable. |
| B273 | **A fast double tap on *Log set* writes the set twice.** Each tap launches its own write and the button stays enabled until the Room flow returns; the mutex orders the two writes rather than deduplicating them, so two identical rows land and the volume and the record comparison count both. Guard the row while a write is in flight (the picker's append has the same shape), and test two immediate calls against one set. |
| B274 | **The plan review collapses a movement and calls an assisted plan matched.** `comparePlanToActual` keys performed rows by exercise id, which re-creates B20's loss when a session holds the same movement twice — one row survives, the totals still count both — while the test at the same case uses two different ids and cannot see it. And both sides of the top-set verdict compare added weight only, so an assisted plan and an assisted set both read zero and always say "Matched" whatever help was used. Pair by occurrence rather than by key, compare the signed load, and return no plan level for an assisted target instead of 0 kg. |
| B276 | **The program format cannot carry a failure, and its version did not move for the last change of meaning.** The shared role enum lists five roles under the doc "the app's SetType, by name", but the app has six and the bridge is `valueOf`, so exporting a program whose plan holds a failure set throws and is reported as a generic storage failure, and a document naming one is refused as corrupt by both hosts. The same enum is where B247's bodyweight/timed flags landed without moving the ceiling, so a build that reads v5 accepts a newer document and drops them back to ordinary movements. Add the role and move the ceiling to 6 with the floor left at 4 (N185's split), in one change so the fixture set is regenerated once. |
| B277 | **The helper writes an exercise narrower than the app's.** The page's sheet builder copies eleven of the seventeen `ExerciseDto` fields — it drops the rest, the cue, the unit, the step and both B247 flags — so a program authored there recreates a pull-up as an ordinary lift and a pound movement as kilograms. The fixture uses seed rows that carry none of those fields, so neither host's fixture test can see the loss. Copy the row whole (or have the core copy it), add a fixture movement that carries a unit, a step, a rest, a cue and both flags, and assert on both hosts. |
| B278 | **The helper's Progress chart uses added weight, not load.** It filters on the added weight being positive and takes the heaviest by that column, while the app reads `loadGrams` — so a weighted pull-up charts at the added kilograms instead of the load, and an assisted set charts nothing where the app has a reading. The N165 fixture has no bodyweight or assisted rows, so the pin that exists to keep the two hosts' trends equal cannot catch it. Implement the same load function, fix the filter and the "no added weight" wording, add both shapes to the fixture, regenerate. |
| B279 | **The helper cannot refuse what it should.** The same-list guard compares against stored rows that `reconcileSources` has already annotated, so it can never match and re-dropping a file doubles the model instead of refusing it; the shared validator is weaker than the app's own save boundary (the RPE floor is one half-point low, `targetRepsCurrent` and `deletedAt` are unchecked, and the run rules the template editor enforces are absent, while the page's editor offers a drop value on every rung); and "download as new program" stamps the working copy at the current version whatever the source document declared, so a newer document is refused as an edit and silently written as an add. Fix the guard, align the validator with the write boundary, and gate the add on the source's version. |
| B280 | **The page's browser tests are stale and no gate runs them.** `docs/tests.html` still asserts the program document's version is 4 while the builder stamps the core's 5, so the file fails today — and the docs job runs only the node runner, which means the sheet and fixture assertions that live in that page are executed by no one. Assert the core's version, and either move those checks into the node runner or run the page headlessly as part of the docs job. A test file that is red and unrun is the same failure the declared-count gates exist to prevent. |
| B281 | **A write's failure is dropped, and a record the app refuses is counted.** The statistics ViewModel discards `setLevelGoal`'s and `setRecentSeries`' results under a KDoc that says a target's failure has to be visible, while the range write beside it reports; and the statistics record count filters on the added weight being positive, so a weighted bodyweight set counts as a record even though the live announce and `personalRecords` both exclude it. Route both writes into the screen's failure channel and add the bodyweight predicate to the count's two sides. |
| B282 | **Three window defects.** `zoneIdOrNull` promises null and throws outside a valid offset, and the import copies the file's value unvalidated, so one hostile backup kills the history flow instead of failing the import. The chart's date labels are built from the recorded readings while the axis is projected over every reading, so a series that ends in a gap names a day that is not where the plot ends. And the next-session line reads this week's deload plans for candidate days in next week, so on the last day of a deload week it names the week being left. Make the offset helper total or refuse the file, derive the ticks from the projected span, and scope the deload lookup to the candidate's own week. |
| B283 | **A callback nobody passes is silently a no-op.** Required actions still default to empty lambdas across the statistics, workout, template and program screens — including the one that dismisses a finished workout and the one that saves a goal period — so a forgotten wire-up compiles and does nothing, which is the B49 failure the rule exists to prevent; and a rotation throws away the goal-period editor, the measurement numbers and the dialog-open flags, while the fields inside them are `rememberSaveable`. Remove the defaults from required callbacks, save the open flags by id and the drafts by field, and make the goal-row test record what Save was handed rather than setting the outcome itself. |
| B284 | **A refusal closes the editor and loses what was typed.** The set editors in the workout and the template fire their write and dismiss, so a rejection arrives after the values are gone — the goal editor already decided the other way (B234); the target dialog's Save stays enabled and does nothing at all when the text does not parse; and the adherence day read reports its failure through the write channel while opening the dialog on an empty occurrence list, which claims the day had nothing scheduled. Close on success only, disable or mark an unreadable field, and give the day read its own failure. |
| B285 | **The backup prune can delete the copy it just wrote.** A taken name lands beside its original with a collision suffix, and that suffix sorts below the name it collided with, so a clock moved backwards can put the freshly written file past the five-file mark: the prune deletes it in the same call and the success is recorded as though a copy landed. Sort by the parsed stamp and suffix rather than by the raw name, and never prune the name just written. |
| B287 | **The program repository reads a version outside its transaction and can export a day it no longer shows.** `keepVersion` reads the plan before the transaction its own comment says spans the reads and the copy, so a concurrent edit produces a version that never existed; and the document export carries every slot but only live templates, so a program using a deleted workout exports a shape the shared validator refuses and the receiving device silently filters the day away. Move the reads inside and drop (or carry) a slot whose workout is hidden, so the app cannot write a document it would reject. |
| B288 | **The docs state facts the code contradicts.** The wiring comment says six migrations where ten are registered; DECISIONS carries the formatting measurement and a backup `CURRENT` that have both moved, and requires an `ALL_MIGRATIONS` registry and a `MigrationTestHelper` test that the 2.x code does not have; README repeats the Gradle and Kotlin versions it points at the catalog for; the program contract says a document's kind and start are ignored by an edit, which N186 reversed; the statistics DAO describes a live-path divergence B118 closed and proposes the fix that landed; `robolectric.properties` names the wrong Robolectric and an API below the one that tool supports; and a test comment claims the minified release is exercised by the debug build. Correct them, and decide whether the docs gate should parse versions and lint facts, since it currently checks links, anchors and the CI task set only. |
| B290 | **The workout screen's feedback is drawn but not announced, and a few controls mislead.** A failed write and the personal-record banner are plain text on the workout screen while every other screen routes a failure through a snackbar, which is a live region — so a screen-reader user hears neither the record nor the refusal. The review dialog's verdict is body-size text in `primary`, the role N49 measured below the ratio body text needs. An edit or delete that removes a record leaves the banner standing, the measurement fields ask for a digits-only keyboard for values that are decimal, and the chart's gesture is keyed on its zoom callback so the scrub callback it captured goes stale. The test side has the same shape: the snackbar test asserts Material3 rather than a screen, the tag-coverage list can grow because the check is name-presence, and the compiler's deprecations (the older Compose rule, `bundleOf`) are warnings nothing fails on. |
| B291 | **Dead weight and small arithmetic.** Delete what nothing calls (the unused test tag, the unused empty slot, the write-only plan field, the transfer outcome's message and its unreachable branch, the duplicated SAF query, the test-only model helpers), and remove Turbine if no test adopts it — it is declared, named in the testing rule, and the reason for a coroutines exception and a dependabot ignore. Then the arithmetic the review found: the one-rep estimate can round to zero below its step, the deload floor can prescribe more than the plan it deloads, `higherIsBetter` is wrong for joint pain and patched at two call sites, "target met" has two definitions (exact and tolerant), the maximum-plus-one inserts are not transactional, the newest-weight read has no kind predicate, and a "new" measurement is encoded as a blank id in a non-null field. |

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The timed set (N220) has shipped to the unreleased section.

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

## Explicit non-goals

Permanent, unlike *Parked*: no trigger brings one of these back.

- Nutrition / calorie tracking
- Social feeds, friends and shared routines (P3.7, P5.2)
- A web dashboard that **holds, syncs or accounts for** the training record — an account, a stored copy, a
  place your history lives. A computed view of a file you loaded and can close is not one (N159, N165).
- Live GPS route tracking
- Health Connect read/write (P4.1)
- A foreground service (P4.2)
- A home-screen widget (P4.3)
- Quick Settings / launcher shortcuts (P4.4)
- A Wear OS companion (P4.5)
- Bluetooth heart-rate straps (P4.6)
- WorkManager reminders (P4.7)
- Large-screen layouts (P4.8)
- Offline-first sync (P4.9)
- Monetization / Play Billing (P5.3)
- A module split into `:core:*` / `:feature:*` (F6)
- Product analytics (F11b)

## Keeping this true

Four rules, each answering drift this file has shown before.

1. **Nothing marked done lives here.** Shipped work goes to [CHANGELOG.md](CHANGELOG.md), and a
   finished row is deleted from this file.
2. **No hand-maintained facts.** No test counts, no dependency lists, no inventory of which
   files exist. Those are commands (`./gradlew …`) or links.
3. **Every parked row names its revisit trigger**, so parking reads as a decision rather than a
   forgotten item.
4. **Durable content lives in [DECISIONS.md](DECISIONS.md).** Settled decisions and the rules
   that apply to every change are a reference, not a queue, and the two age differently. A
   section here that accumulates rather than drains belongs there.

Everything that has **left** *Next* has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
