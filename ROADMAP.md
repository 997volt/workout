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

**The review of the batch since `v2.18` (2026-10-29).** One pass over the unreleased work — the library's controls
and its own data, the exercise's screen, the three schema steps, the two rules that read history, the lifter's
movements joining the seed, and the transfer and copy paths — read against the settled rules and checked on this
checkout. The Gradle gate, the unit freshness check, the docs node suite and the generated fixtures are green;
two checks are red, and both are why this is the next work: the page's copy of the shared core is stale, and the
instrumented report is older than the newest test source. The rows are in the order they should ship rather than
in id order: the three blockers first — a red CI step (B294), a feature that cannot be reached (B295), and an
upgrade path that hides movements (B296) — then the rows that lose or rewrite data, then the rules and the
reading of them, then the docs and the tests.

| # | What it changes |
| --- | --- |
| B294 | **The page's copy of the shared core is stale.** N242 added `targetDurationSeconds` to `TemplateSetDto`, but `docs/shared/workout-shared.js` was never regenerated: the committed file does not contain the field, and `:shared:syncSharedJs` rewrites it. The build job's "the page's copy of the shared core is current" step compares `git status --porcelain -- docs/shared` and would fail, so the helper loads a model older than the format it is asked to read. Run the task, commit the result, and decide whether the documented local gate must run it too — it does not today, which is how this got through. |
| B295 | **A hold can be neither seen nor taken, and taking it would erase it.** N242's seconds direction exists in the domain, but `ProgressionDialog`'s `ChoiceChips` draws only the reps and load offers, `BulkChoices` offers only `LOAD` and `REPS`, and `PendingProgression.ownDirection` never picks `SECONDS` — so a seconds-only offer is an empty row and *Apply at Done* never chooses it. The plan row and the question's plan and done lines also omit the hold, so a prescribed 45-second carry reads as its load and "0 reps". And `ProgressionPlanSet.toTemplateEdit` omits `targetDurationSeconds` while `RoomTemplateRepository.updateSet` writes the edit's value, so accepting a step would store null over the prescription. Add the seconds chip and the `ownDirection` branch, state the hold in both summaries, carry the duration in `toTemplateEdit`, and test a hold offer through the dialog and the write. |
| B296 | **The B292 repair can never re-file a family's children.** `releaseSeededMovementIds` runs `UPDATE exercises SET parentId = ? … WHERE parentId = ? AND parentId <> ?` bound to the same id twice, so it matches nothing; the impostor is then deleted and its children keep a `parentId` that the corrected seed has just turned into a movement. `libraryRows` only walks categories and sends a row to the top level only when its parent is absent, so those movements are drawn in neither the library nor the picker — they stay in the store and the backup but cannot be logged. `ExerciseSeederTest` plants the child already pointing at the seed's family, so it never exercises the repair. Drop the impossible predicate and plant the 2.6 shape (the child under `back-extension`) in the test. |
| B297 | **A backup loses the declined-step record.** N236 stores `declinedDirections` on `session_exercises`, but `SessionExerciseDto` has no field for it and no transfer mapper carries it, so every export drops it and every restore writes null — the one record the app cannot recompute. N235's and N242's new fields were added to their DTOs, which is what makes this an omission rather than a policy. Add it as a defaulted field (the schema version does not move), map it both ways, and pin it and `targetDurationSeconds` in the round-trip and old-file decode tests, where neither is covered now. |
| B298 | **A ruled merge can delete a movement the lifter made, and takes what only it held.** `mergedMovementPairs` guards the mechanical duplicates by shape but not the ruling table, whose `add` skips only seeded sources; the repair then deletes the source row unconditionally. This release also adds creation from the library, and `RoomExerciseRepository.create` writes exactly the bare row the mechanical rule calls an import's leftover, while a lifter-made *Cable Row*, *Calf Press* or *Machine Fly* is a ruled source whatever its shape. The source's own `restSeconds`, `techniqueNote`, `weightUnit`, `stepGrams` and `progressionMode` are dropped with it. Require "states nothing" for a ruled source too, or carry the fields the survivor lacks, and refuse a duplicate name at creation so the app cannot mint a mergeable twin. |
| B299 | **The flag repair undoes the lifter's answer.** `UPDATE_SEEDED_FLAGS` only raises (`isBodyweight < ? OR isTimed < ?`), but the editor writes both flags, so turning Bodyweight or Timed off on a seeded movement is re-raised on the next open — the reverse of "a flag a lifter set stays theirs". `UPDATE_SEEDED_FAMILY` has the same shape for a movement the lifter unfiled. Make the repairs one-time backfills keyed on what the seed shipped, not a per-open assertion of the seed, and test the off direction. |
| B300 | **The retired-id map misses the retirement this release knows statically.** `mergeRetiredMovements` re-points and deletes `deadlift` but writes no `retired_movements` row; the only writer is the duplicate repair, and the only reader is the program import. A document exported before the merge that names `deadlift` therefore resolves to nothing and re-mints the twin, which only the next open's ruling removes, and a fresh install — where the merge is a no-op — never has the mapping at all. Write the mapping in the retirement path too, and assert it in the seeder's test. |
| B301 | **A pre-merge export of the program on the device is offered as a copy.** `addPreview` compares the raw document with `plansTheSameWorkAs`, but the merge resolves retired movement ids first, and the comparison is on exercise ids. A document naming an id the device has since merged away therefore reads as different work, so the load stops at the rename-and-copy question instead of answering *already here*. Resolve retired ids in the preview too, or resolve once before both paths, and cover a document that names a retired id. |
| B302 | **A copy can attach its rows to the original's workout.** `programDocumentCopy` rewires a planned exercise's template with `templateIds[...] ?: planned.templateId`; when the document names a template it does not carry, the fallback is the original's live template, which the merge's `knownTemplates` accepts — so the "second program" writes rows into the first one's workout. The KDoc justifies the fallback only for a slot. Drop the planned rows whose template is not carried, mirroring the merge's own dropping rule, or state and test the pass-through. |
| B303 | **A stall outlives the session that answered it.** `PrescriptionHistory.stall` keeps the longest run it has seen and never clears it, so three misses followed by a session that met the plan still returns a stall of three — reproduced on this checkout. The card can therefore tell a lifter to deload a plan that has been working for as long as the run stays inside the read's twelve-session window, which is the wrong yes the rule's own test doc calls the failure to avoid. The test named for this case uses a two-miss streak, below the threshold, so it cannot catch it. Report only a run still open at the newest session, and add the three-miss-then-answer case. |
| B304 | **A set nobody logged breaks the streak.** In the same walk, a missing pair yields an empty kind set and falls into the reset branch, so a session that trained the movement but logged no set — or fewer sets than the plan prescribes — ends every run at once, though the rule says absence can neither start nor break one (reproduced). `toTemplateSessions` makes this reachable by emitting an entry with an empty set list for a session exercise that logged nothing. Skip a null pair rather than resetting on it, and test the gap-in-the-middle case. |
| B305 | **The stall pairs by raw position, not by class.** The KDoc says the walk pairs exactly as the offer pairs them, but `stall` strips warm-ups and then takes `performedWork.getOrNull(index)`, while `progressionPromptFor` deliberately pairs work sets with work sets and rungs with rungs (N79). A session that skipped a work set and logged a drop therefore compares the drop with the next prescription and can report a `LOAD_SHORT` the offer never states. Extract the offer's pairing and call it from both. |
| B306 | **A bodyweight hold never has a record.** `personalRecords` filters every row whose `bodyweightGrams` is set before `PersonalRecords.from`, which is where `longestHoldSeconds` comes from; a Plank or Side Plank — seeded bodyweight and timed — therefore reports no longest hold when the lifter's weight was recorded, though the changelog says a bodyweight carry still has a longest one. The new unit test only builds a spec with `weightGrams = 0`, which is not the bodyweight case. Keep the filter for `bestByReps` and compute the hold from the unfiltered rows. |
| B307 | **A hold-only edit says the program changed and writes nothing.** The shared `sameSet` compares every set field except `targetDurationSeconds`, so a document that changes only a hold plans no `SET_UPDATED`; the repository writes only the sets in that list, while the edit's confirmation sets its success flag regardless, and the page's preview has the same blind spot. Compare the duration and regenerate `docs/shared` with B294's fix. |
| B308 | **The helper accepts a hold the app refuses.** `validateSetTargets` checks the load, assistance, drop and reps but never `targetDurationSeconds`, so a zero or negative hold, or a set naming both reps and a hold, passes the shared validator and is written by the add merge and the edit writer — both of which bypass the app's own `validatePrescription`. That is B279's "the helper refuses what the app refuses" left open for N242. Add the range and exclusivity rules with their own `ProblemCode`, and pin them in the shared tests and a fixture. |
| B309 | **A failed decline record is swallowed.** N236 deliberately keeps finishing when the decline write fails and says the failure is said rather than swallowed, but `onFinishExercise` sets `lastError` and then calls `finishExercise`, which clears it on success, so the ordinary failure is erased; a finish failure replaces it. Set the decline error after the finish, or stop the finish clearing an error it did not set, and test a failure on the decline only. |
| B310 | **A decline survives the step being taken.** `declineProgression` writes the whole column and nothing clears it: decline, finish, Reopen, Done with the same plan reopens the question, and accepting the step leaves History and the review saying *step offered, not taken* about a step that was taken. Write the empty value when an exercise's steps are accepted, and after a decline whose finish then fails, so an open row does not keep the claim. |
| B311 | **Not trained can draw a blank page.** `ExerciseLibraryUiState.isEmpty` re-derives the list with the filter but without `lastTrained` and `notTrainedSince`, so under *Not trained* every movement counts as matching and the state says not empty while the rows the screen drew are empty — a blank list with no empty copy and no Create action. Branch on the rows the body already receives and delete the second derivation. |
| B312 | **One hold's shortfall is stated for all of them.** `sharedMiss` collapses sets that share a miss but whose stated number is null, and `statedNumber` has no `HOLD_SHORT` branch although the sentence states `asked − held`; two holds of 45 seconds that came up 15 and 5 short therefore collapse into one line reading "15 seconds short", with the rows' own reasons suppressed (reproduced). Add the hold's number to the check. The same file's KDoc says "three kinds" where the list has four since N242. |
| B313 | **The unreleased changelog has two *Added* sections.** The N243 entry sits under a second `### Added` after `### Fixed`, N232 appears in both *Added* and *Changed*, N241's bullet has no blank line before it, and several entries are separated by two. Merge to one section per heading, one entry per shipped row, and single blank lines. |
| B314 | **The schema story stops at 13.** `WorkoutDatabase`'s KDoc documents schemas 1–13 and `AllMigrations` points there for what each step did, while 14 (`declinedDirections`), 15 (`targetDurationSeconds`) and 16 (`retired_movements`) have no paragraph. Add them. |
| B315 | **The batch's rules are not in DECISIONS.** No entry covers N224–N243, while the batch introduces rules that bind later work: a movement's progression answer is training data rather than a device setting, and a merged row is deleted with its id remembered in `retired_movements`. The latter is an exception to the "rows are sync-shaped … soft deletes" rule, which still reads as absolute. Record both, and the exception, where the next change will read them. |
| B316 | **Dead weight.** Four of `MovementMergeDao`'s five methods — `insertRetired`, `repointSessionExercises`, `repointTemplateExercises`, `deleteMergedExercise` — are called by nothing, because the repair uses raw SQL, so the re-point statement exists twice; and `retired_movements.at` is written and never read. `STALL_SESSION_WINDOW` has no caller (the repository hardcodes twelve), and `Programs.PROGRAM_STALL` and `PROGRAM_STALL_DELOAD` duplicate `STALL` and `STALL_DELOAD` under the same values with no user. Delete them, keep the retired-id read (or move it onto `ExerciseDao`), and give `at` a reader or drop it before the schema ships. |
| B317 | **The history section resolves its unit its own way.** `ExerciseDetailScreen` passes `exercise.weightUnit ?: LocalWeightUnit.current` to the new section instead of `exerciseWeightUnit`, whose KDoc says every exercise-scoped display goes through it, and the same screen uses the helper for its weight rows. Use the helper and add a pounds case to the section's test. |
| B318 | **The schema and transfer tests check less than they claim.** `MigrationSchemaTest` plants a row in every table but counts only the `exercises` row, so a step that dropped all `session_exercises` or `template_sets` rows would pass; `DeclinedStepMigrationTest` never reads a row back — it inserts after the ALTER and asserts the pure codec — so the column's round trip is untested; and `withResolvedExercises`, the only reader of schema 16, has no test at all. Count every planted row, read the declined value back from a pre-ALTER store, and test resolution (the retired definition dropped, the planned exercise moved to the survivor). |
| B319 | **The library's tests leave gaps the review closed by hand.** The ruled-table test pins the count and source uniqueness but not that every survivor is still a seeded movement, so a rename silently makes a ruling never fire; five alias entries — `cgbp`, `bss`, `hlr`, `tgu` and *seated cable row*'s "cable row" — are already answered by the initials or substring rule, against the table's own rule; and `SeedCatalogTest`'s regeneration comment names `app/build/…` where the test writes the repo-root `build/…`. Close each, or delete what is redundant. |
| B320 | **The rename question words the wrong refusal.** `NameTaken` fires on the held program id but carries the document's name, so after the device's program is renamed the sentence says a free name is taken; the error is also a sibling `Text` where the repo's fields use `supportingText`, and the copy path re-implements `requireName` with a different message and reads the names outside its transaction. Word the sentence on the id clash (or carry the held name), move the error into the field's supporting text, and reuse the one name check. |
| B321 | **The instrumented half of the gate has not run against the final sources.** `tools/ci-check-tests.py instrumented` fails because the newest report predates `SeededTaxonomyRepairTest` by seconds; the report does hold the class's final test names, so the edit was likely a comment, but the checker's rule is the rule, and no device is attached here to re-run it. Run `connectedDebugAndroidTest` and the checker on a device before the tag, and treat a stale report as a release blocker rather than a green one. |

## Later — decided, and not being started

Nothing is queued. This section is where agreed work waits its turn: a row here has its design settled so it does
not have to be re-argued, and a row in *Next* is what is actually being worked on. Both drain the same way — a row
picked up moves to *Next*, and one that ships leaves for the changelog — which is what just happened to the last
eight.

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
| — | **A *Removed* view of the exercise library** | The app gains a way to remove a movement. Nothing can remove one today — the library's soft delete has no caller — so only a restored file can hide a row, and a group of them would draw a list nobody on this device could have made. |
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
