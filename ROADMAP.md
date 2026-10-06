# Workout — Roadmap

> **v1.14** is shipped and installed. Last reviewed against the code: 2026-10-06.
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

**The defects a review of the unreleased N74–N79 batch found**, each with the rule it holds. They carry
`B#` ids because they are defects rather than scope, and they stand here because that batch has not
shipped yet: what it does is recorded in [CHANGELOG.md](CHANGELOG.md) under *Unreleased*, and these are
what stand between it and a release. A candidate still graduates to this section only by being picked up;
the two queues below are where unplanned work waits, *Later* for what is self-contained and *Parked* for
what is a product in its own right.

- **B59 — a plan holding a legacy or orphaned drop run cannot be written.**
  [RoomTemplateRepository.kt](app/src/main/java/com/example/androidapp/data/RoomTemplateRepository.kt)
  re-checks every rung rule over **all** of an exercise's stored rows on any add or edit, and one rule
  refuses a drop run whose first rung names no value. Migration 34→35 leaves that value null, so a plan
  written before the feature is refused the moment anything in it is touched — while the lifter is editing
  a different row and the error names the run. A run whose anchor was deleted fails the same way. The rules
  have to bind the row being written rather than the rows already on disk: backfill a value in a migration,
  or tolerate what is stored, so a pre-N79 plan stays editable and the read path's *null means no ladder*
  stays the one answer.
- **B60 — deleting a run's anchor leaves its rungs.** The N79 plan promised this rule in as many words and
  [DECISIONS.md](DECISIONS.md) records it as held;
  [RoomTemplateRepository.kt](app/src/main/java/com/example/androidapp/data/RoomTemplateRepository.kt)
  soft-deletes one row, so the rungs keep standing with nothing above them — which is the state B59 then
  refuses to write. Either delete a run with its anchor, as the decision says, or drop the claim: a rule the
  code does not hold is read as true by the next change that touches the shape.
- **B61 — the plan editor cannot round-trip a run's value.** `TemplateSet.toEdit()` in
  [TemplateEditorScreen.kt](app/src/main/java/com/example/androidapp/ui/templates/TemplateEditorScreen.kt)
  omits `dropValueGrams` — its N74 sibling `targetRepsCurrent` is carried — so opening an existing drop row
  builds an empty value field, the dialog's own guard disables Save, and a number the app already stores can
  be neither seen nor kept without retyping it. The dialog is the only place a value is authored, so the row
  is uneditable, note included, until it is typed again.
- **B62 — a newer file is reported as corrupt rather than as newer.**
  [BackupCodec.kt](app/src/main/java/com/example/androidapp/data/transfer/BackupCodec.kt) and
  [ProgramDocumentCodec.kt](app/src/main/java/com/example/androidapp/data/transfer/ProgramDocumentCodec.kt)
  both decode the whole document before checking its version, and a newer file fails inside the decoder on an
  enum name an older build cannot represent — where the version gate can no longer catch it. So a v2 backup,
  or a program holding a Cluster set, is answered with *"That does not look like a backup file"*: the exact
  confusion the version exists to prevent, and the CHANGELOG's claim that 1.14 rejects the export "with the
  message that the file is newer" is not true of the files that carry the new values. The version is read
  before the document is decoded. The program document also needs the decision the backup got: the new muscle
  names and `SetType.CLUSTER` travel in it, and its format version is still 1.
- **B63 — the live log accepts a warm-up as a rung's anchor.**
  [RoomWorkoutRepository.kt](app/src/main/java/com/example/androidapp/data/RoomWorkoutRepository.kt) asks
  whether any non-rung set exists, which a warm-up satisfies, while the run model requires the row above to
  record effort and the plan boundary refuses exactly that shape. A drop logged after only a warm-up is
  therefore stored as a rung the app reads no run for — and the method's own doc says a warm-up does not
  count. The log asks the domain's question, on the set immediately above.
- **B64 — a rung is offered where it cannot be saved, and the edit path never refuses it.**
  [SetRoleSelector.kt](app/src/main/java/com/example/androidapp/ui/components/SetRoleSelector.kt) lists every
  role unconditionally, so the first set of an exercise can be armed as a drop or a cluster in both the
  logger and the plan editor; the refusal arrives after the tap, and the comment claiming the picker is the
  real guard is false. Editing a logged set's role has no guard at all. The picker offers a rung only where
  an anchor exists, and every write path holds the same rule — the project's own *a control that cannot write
  is worse than no control*.
- **B65 — the history editor's ± moves by the unit's step.**
  [WorkoutDetailScreen.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailScreen.kt) never
  passes the exercise's step to the set editor and
  [WorkoutDetailViewModel.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailViewModel.kt)
  drops it from the row it builds, although the query selects the column already. Correcting a past set
  therefore steps by 2.5 kg / 5 lb where the live logger steps by the movement's own step: a fourth ± surface
  the N77 entry's "three places" does not name.
- **B66 — changing an exercise's unit reinterprets its typed step.**
  [ExerciseDetailScreen.kt](app/src/main/java/com/example/androidapp/ui/exercises/ExerciseDetailScreen.kt)
  keeps the step text as typed and parses it against whichever unit is selected when Save is tapped, so "5"
  typed in kilograms and then switched to pounds stores 2268 g rather than 5000 g — with no signal, and no
  test for a unit changed mid-form.
- **B67 — the Back split's rewrite is not token-boundary safe.**
  [Migrations.kt](app/src/main/java/com/example/androidapp/data/local/Migrations.kt) rewrites a secondary
  list with `replace(…, 'BACK', …)` and a `LIKE '%BACK%'` guard, and `LOWER_BACK` contains `BACK`, so a row
  holding one would become `LOWER_UPPER_BACK` and throw on read. No legitimately written v32 row can hold
  that name, which is why this is hardening rather than a live defect — but the guard costs a comma on each
  side of the token.
- **B68 — the "ask about progression = off" path has no test.** Its only one was deleted with the N74
  rewrite and nothing replaced it: the screen test that remains passes a null question and never touches the
  setting, and no ViewModel test flips it. The decision now lives in
  [ActiveWorkoutViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutViewModel.kt),
  so the promise N66 makes — the app asks only when asked — is unguarded end to end.
- **B69 — the rules no test reaches.** N79's refusal of a value that is not above zero, and of a value on a
  later rung, are never exercised; the pairing test pins the pairing but not the offer it exists to protect
  (its planned sets end where they start, so both pairings offer the same load step); and the claims that a
  rung cut short does not hold its group back, that a cluster counts towards volume, and that the ViewModel
  matches a rung's reps by the set's place in the plan are asserted nowhere. A rule with no test is a rule
  the next change may quietly invert.
- **B70 — the new columns' round trip is only half covered.** `dropValueGrams` is read back through the
  repository; `stepGrams` and `dropValueGrams` have no backup round-trip assertion, and `stepGrams` and
  `targetRepsCurrent` are never written through a DAO and read back at all — so a mapper or projection that
  dropped one stays green, which is the trap those suites exist for. One instrumented fixture already seeds
  `targetRepsCurrent` without asserting it.
- **B71 — the small regressions the batch left.** The planned-set note box lost its two-line minimum;
  [WorkoutsHomeScreen.kt](app/src/main/java/com/example/androidapp/ui/home/WorkoutsHomeScreen.kt) still
  describes the Programs and Templates links as shown only while no workout is open, which N78 reversed; the
  muscle dropdown's options carry no test tag, so the N75 test addresses them by English label against the
  repo's own rule; [ExerciseTaxonomy.kt](app/src/main/java/com/example/androidapp/domain/model/ExerciseTaxonomy.kt)
  names a secondary-muscle editor that does not exist; and a rung whose load cannot be derived shows the
  anchor's number — or nothing, in the plan — with no reason, so an assisted-anchor drop is indistinguishable
  from a cluster.
- **B72 — the rest rule leans on an index space a deletion desynchronizes.** The rest waits on a plan
  position while the log counts logged sets, so removing a *middle* planned set can test the wrong row —
  starting the rest between a run's rungs, or skipping it after the last. The same positional assumption
  already underlies the prefill, so this is inherited rather than new; it is recorded because N79 is the first
  rule where it changes what the screen does.

## Later (still self-contained)

Everything that stood here has shipped — the defects found in use, the workout screen's discard, the
workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch, a rest of
zero, the planned-set prefill, the program document, and the eight defects a review of that batch found
and closed (B51-B58) — each with its entry in [CHANGELOG.md](CHANGELOG.md). A candidate graduates to
*Next* — gaining an id and a spelled-out decision — when it is picked up, so this queue is where
unplanned work waits, and *Parked* below is where deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (the app states what happened; it never writes what it decided), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

### Nothing waiting

The queue is empty rather than closed: it is where a candidate waits as a wish until it is picked
up, and picking one up is what gives it an id and a spelled-out decision. The last request that
stood here — an exercise's own weight change — became N77 that way and has shipped, with its entry
in [CHANGELOG.md](CHANGELOG.md).

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
