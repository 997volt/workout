# Changelog

Notable changes to Workout, newest first. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions are the
`versionName` from [`version.properties`](version.properties), with the
`versionCode` in brackets because that is what Android actually compares.

Each entry says what shipped and why it mattered. The reasoning behind a decision — the
alternatives rejected, the measurements, the argument — lives in
[DECISIONS.md](DECISIONS.md) and [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md), and is not
repeated here.

**1.0–1.18 live in [CHANGELOG-1.x.md](CHANGELOG-1.x.md)**, kept whole rather than deleted because the
id a 1.x commit names is still looked up by it. This file carries the current release.

## [Unreleased]

### Added

- **A refused program load can arrive under a name of its own** (N243). A program document whose id is already
  here carrying a *different* plan could only be refused: the merge is insert-only and never edits a program, so
  the lifter's ways out were renaming the program already on the device or editing the file by hand. The load now
  stops at a question instead — a name the list does not hold, offered as "Push Pull Legs (2)" and editable, with
  a name another program holds refused while the lifter is looking at the field. Confirming writes a **copy**: the
  plan as the document describes it, with an id of its own for the program and new ids for its workouts, planned
  exercises and sets, because a copy that shared its days with the original would not be a separate program —
  editing one day would rewrite both, and a document exported before the copy would then be refused as stale. The
  movements keep their library ids, which name rows rather than copying them.

- **The lifter's own movements join the seeded library** (N239). Twenty-seven rows this library trained — the ones
  the seed never shipped and that N238's merge does not take away — were **bare**: no muscle, no equipment, no
  pattern, so they were not found by "barbell", did not roll into a family's muscle total and could not be sorted
  among their siblings. Each now states its equipment and is filed under a family, **under the id it already has**,
  so no history moved: the id was the link. They ship with the seed too, which is what makes a fresh install and
  this device read the same library. The run needed one new family — *Upright Row*, four implements of one lift
  that had no head to hold them — and it ships with them rather than before they exist. A repair writes the
  taxonomy onto a row that states none, guarded by the same shape rule the merge reads, so a movement a lifter has
  filled in is theirs; the generated seed catalogue in `docs/` is regenerated with it.

- **A movement the library holds twice is merged** (N238, schema 16). A program document that names a lift the
  device does not have creates a movement of its own, so loading one leaves a second *Back Squat* beside the seeded
  one with the history split across the two — and the same movement can be two rows under two of the author's names
  (a *pec deck* and a *machine fly*). Both are settled by one write: the mechanical duplicates are recognised by
  **shape** — a seeded movement's name, no taxonomy, no family, which nothing an edit produces — and the ruled ones
  by the nineteen lines the roadmap's table states. A merge re-points the session's and the plan's rows onto the
  survivor, so the sets, their loads and their records are untouched, then **deletes** the merged row rather than
  soft-deleting it: after the re-point it holds nothing. The retired id is remembered, so a document exported
  before the merge joins the survivor instead of creating the twin again, and the repair runs where the seed runs —
  so an old backup is re-merged rather than left as a duplicate. Idempotent: a store with nothing to merge writes
  nothing.

- **Three misses on one plan suggest a deload** (N237). The app could already say what each set missed and why, and
  said nothing about the pattern. For one plan, the sessions are taken in order and each prescribed set is paired
  with the session's own, so a plan whose *second* set is always the one that fails says that rather than blaming
  the exercise. **Three sessions in which the same measurable kind repeats** is a stall, and the program's screen
  suggests a deload with the creator it already has. Only the kinds the rule can measure count — short reps, over
  the target effort, under the plan's load, a hold shorter than prescribed — because the offer states one reason by
  precedence and counting the *reported* one would let "over the target" silently become "short" between two
  sessions failing the same way. `NOT_DONE` is absence, not failure: a set nobody logged cannot start or break a
  streak. It only suggests, and nothing is written.

- **A plan can prescribe a hold, and a hold can progress** (N242, schema 15). A timed set already recorded seconds
  and wrote no reps, but a plan prescribed **reps**: a hold inside a plan ignored its target, a session saved as a
  plan wrote no rep target at all, and the next-step question answered a hold with "a hold is not measured in reps",
  so a lifter who trains carries and planks had a logged history and no prescription. A plan's set now names an
  optional **hold** beside its reps — one or the other and never both, which the write boundary refuses — the
  session's prefill opens a planned 45-second carry on 45, and the offer gains a **seconds** direction moved by the
  hold's own step, under the same effort gate as the other two. A hold earns seconds or the set earns reps, never
  both, which is the exclusivity the rep and weight directions already have. The plan editor's sentence states it,
  the program document carries it (a document describes a plan), and a deload that copies a plan keeps it.

- **A step that was offered and not taken is recorded** (N236, schema 14). *Not now* dropped the question and
  wrote nothing, so a week later History could not say a step was there to take — and it cannot be recomputed,
  because the offer is frozen from the plan as it stood and the plan has moved on since. The session exercise now
  records the directions it declined, written the moment the lifter declines, and both the workout's review and
  History's detail state it as *step offered, not taken*. **A declined step is not a miss**: the sets failed
  nothing, the lifter declined the answer, which is why the deload rule counts the work rather than this column.

- **A movement can answer *Done* its own way** (N235, schema 13). The progression mode was one device preference
  for the whole app, so a 5/3/1 squat and a movement being gently worked back were told the same thing. A movement
  now has its own answer — *App default*, *Ask*, *Apply at Done* or *Off* — drawn in its editor as a four-way
  choice, while the settings control keeps its three and says it is the default. It is stored on the movement
  rather than in the settings because it **decides what the app writes into a plan**, and this project's rule is
  that a setting is a device preference while a metric target is training data: a restore must not silently return
  a movement to *Ask*. The Done flow resolves it in the one place it reads the mode, and the plan editor's sentence
  gains a clause when the movement's answer differs from the setting — otherwise it promises a question that will
  not be asked. The backup carries it as a defaulted field, so the transfer format's version does not move, and a
  program document does not carry it, because a document describes a plan while this is the receiving library's
  view of a movement.

- **The workout's ⋮ opens the exercise** (N234, reversing N121). The shared menu already carried an *Open exercise*
  entry that the template editor supplied, and the live workout withheld it on purpose — with a test that asserted
  its absence. The alternative was left unstated: to look at a lift mid-session you switched to Library, found the
  movement, opened it and switched back, a larger interruption than two deliberate taps in an overflow that sits at
  the block's corner, away from the set fields. Opening it **pushes**, so the session's ViewModel, its running rest
  timer and the draft in the next set's fields are untouched and one back tap returns to them.

- **The exercise's own screen says when it was last trained and what its best is** (N233). The screen carried an
  honest apology instead of history. It now has the section: the date of the newest finished session that trained
  the movement, the working sets that session holds, and the best at each rep count — or the longest hold, because
  a timed movement has no rep count to be best at (N220). Both halves come from reads the app already had: the
  prefill's *previous performance* (which now also says when that session was) and the record rule the workout
  screen checks against. No schema change, and the record rule now sees a hold's seconds: a warm-up hold is not a
  record, and a bodyweight carry still has a longest one.

- **The library can be sorted by when a lift was last trained** (N230). Order was alphabetical by family and then
  by movement, and nothing offered another, so the one question the list could not answer was "what have I not
  done". A control names the order it is — *Name* or *Last trained* — and the default stays alphabetical, so a
  lifter who never opens it sees the library they have. Last trained is one new read for the whole library: the
  newest **finished** session naming each movement, because a session opened and abandoned is not training. A
  family takes its most recently trained member's place, a lift never trained sorts last, and a tie falls back to
  the name so the order does not depend on what the database returned. A read that failed is a message rather
  than the page — but it is never shown as "never trained", which would be a claim about the training.

- **Filters narrow the library inside its search** (N231). A row of chips over the list: *All*, *Not trained in 30
  days*, *Bodyweight*, *Timed*, *Unfiled*, each carrying its group's size from the whole library so the number does
  not move while a query is typed. The search applies **inside** the chosen group, because "bench" inside
  *Bodyweight* is the question a lifter actually has, and the two controls narrow one list rather than replacing
  one another. The group applies to movements, so a family is a row when the movements under it matched — a family
  is not bodyweight and is never trained. A chip whose group is empty is not drawn, and a group that empties while
  selected falls back to *All* rather than leaving a blank list under a chosen chip.

- **A movement can be made from the library** (N232). Creation lived in the workout picker alone, on the rule that
  the gap is felt mid-session — but planning a lift you have never trained is the other place it is felt, and there
  the library is the screen in front of you. The library's action is now a menu: a movement, and the family it has
  always made. A search that found nothing offers to create exactly what was typed, so "no exercises match *front
  squat*" is one tap from the movement. The dialog also files it while it is made, because an unfiled movement is
  the shape the library's filters exist to clean up. It reverses B94's rule that creation belongs to the workout
  picker alone.

- **A lift is found by how it is said, not only by its name** (N224). Search was substring containment over the name
  and the taxonomy, so *Barbell Bench Press* answered "bench" and not "bench pr", "bbp" or "pullup" — and the words a
  lifter types are the ones that come out of a session. Three rules now share one normalised query: the name compared
  with its separators removed (so "pullup" and "pull up" are one movement), the initials of its words ("bbp", "cgbp"),
  and an alias table for the words a name cannot carry ("ohp", "rdl", "bent over row"). The aliases live beside the
  search rather than on a row, so nothing is stored per movement and a movement a lifter makes has none.

- **A family says how much of itself a search is showing** (N225). A head's count was the number of children the query
  matched, so a family of six read "1 exercise" beside the one row it found — a claim about the library that the
  library does not support. The row carries the family's own size and the matched count, and the head states both only
  where they differ: "1 of 6 exercises", while a list nothing is narrowing keeps its plain count. The hidden-spot
  detail is the plural's quantity, which is the family's size: "1 of 6 exercises", never "1 of 6 exercise".

- **The next step's question states one shared reason once** (N226). When every prescribed set missed for the same
  cause the dialog repeated the same sentence per row: a plan with no target RPE printed it three times, and one whose
  sets all came up the same two reps short printed that line three times. The question now names the reason above the
  rows, which keep the plan and what was done. Two conditions make the collapsed line true of every set it replaces:
  only a reason about the sets themselves counts — `NOT_DONE`, `NOT_IN_PLAN` and `RUNG_OF_A_GROUP` describe work that
  is missing or extra, so a plan nobody logged is not one refusal — and the number has to agree, because two reps
  short and one are two facts.

- **The exercise states what its plan will do with the set** (N227). The workout block's small print carried the
  taxonomy, the cue and the rest prescription, and nothing about the step the plan's own numbers can earn. It gains a
  line reading the same rule the plan editor prints — the rep below the range's ceiling, the weight at it, or the
  weight alone where the reps are fixed — and a plan that cannot progress says nothing here, because the plan editor
  is where that sentence belongs. It sits with the small print and never on the prefill's reason line, which is where
  the numbers came from rather than the app's opinion of what to do next (N33). The rule moved to the model for this,
  so the session and the plan editor are one function reading one mapping of the same sets.

- **A family a movement joined or left opens once** (N228). The library folds every family on load and remembers the
  folds, so filing a movement from its own editor changed a count on a folded row and nothing else: the one place the
  change happened was the one place it could not be seen. The screen compares each movement's family with the one it
  had on the previous composition and opens what changed — the family joined, the one left, both when a movement
  moves between them. The library arriving is not the library changing, so the first composition opens nothing, and a
  movement that arrived unfiled names no family at all.

- **Plyometric movements** (N241). Seven arrive — box jump, jump squat, broad jump, depth jump, pogo hop, skater bound
  and a medicine-ball throw — as body-loaded movements, so a set prefills as added weight and one at its rep ceiling
  earns nothing rather than a heavier bar it does not load. The movement pattern enum gained `JUMP` for the family to
  state: a family holds one pattern and one primary muscle, and a box jump has an honest claim on the squat pattern
  and on the calves, quads and glutes. The throw states its own taxonomy, because a medicine-ball throw is upper-body
  power and the family's quads would be a fact invented about the movement.

### Changed

- **The seeded library files its movements, and one deadlift means one lift** (N229). Eight of the seed's movements sat
  at the top level, so a fresh install drew a flat alphabetical pile of loose lifts under its families. A face pull is
  rear-delt work and a dip is the compound its family already holds; the knee isolation needed two families, because a
  leg extension is quad work that *Squat* would name wrongly and a leg curl is knee flexion rather than the hinge
  *Deadlift* holds. The push-up stays unfiled on purpose: every press family is a *lift's* family. The plain
  *Deadlift* row goes too — the family is named after the lift, so a movement of that name sat beside it, and the bar
  lift a lifter means is *Conventional Deadlift*. Retiring a name is not deleting a row: a device that logged it keeps
  every set, plan target and program slot, moved onto the survivor, and the plan row it changed moves its own
  timestamp so a document exported before this is refused as stale rather than merged into a plan that changed.

### Fixed

One review pass over this unreleased batch — before any of it shipped — found the rows below, and each was
fixed in the same cycle.

- **The page's copy of the shared core is current** (B294). N242's field never reached the generated
  `docs/shared` build, so the build job's status check failed and the helper could not name a hold. The build is
  regenerated, and the gate paragraph names the command that regenerates it instead of implying the fixtures
  command does.
- **A hold can be taken, and the plan keeps it** (B295). N242's seconds offer had no control to take it: the
  question drew reps and load only, the plan and done lines dropped the hold, and the write would have stored
  null over it. A seconds chip, a bulk pick, the answer's branch, the hold in both lines and the duration in the
  write land together — fixing only the control would have made *Done* erase the prescription.
- **The id repair re-files a family's children** (B296). Its statement asked for a row that is its own parent
  and matched nothing, leaving the impostor's children filed under an id that then named a movement, drawn in
  neither the library nor the picker. The clause is gone and the test plants the shape the repair exists for.
- **A backup carries the declined step** (B297). N236's column was not in the backup DTO, so every export
  dropped the one record the app cannot recompute. It and the plan's hold are pinned by codec tests.
- **A merge takes an import's leftover and keeps its settings** (B298). The ruled merges had no shape guard, so
  a movement the lifter configured was deleted on the next open. A ruled source must now be bare, its rest, cue,
  unit, step and answer travel to the survivor, and creation and rename refuse a duplicate name.
- **The seed's repairs stop undoing a lifter's answer** (B299). Turning *Bodyweight* or *Timed* off, or
  unfiling a movement, was re-asserted on every open. The repairs read the row's never-edited marker now, and
  the seed's own repairs no longer write that marker.
- **The retired map records the deadlift** (B300). The seed's static retirement wrote no mapping, so a
  pre-merge document re-minted the twin and a fresh install never had the line. The retirement is recorded on
  every open, idempotently.
- **The load preview reads a retired id as the row it became** (B301). Comparing the raw document made an old
  export of the program on the device look like a different plan and offered a copy. Both paths resolve through
  one helper now.
- **A copy drops planned work whose workout it does not carry** (B302). Its fallback wrote rows into the
  original's workout; the planned work and its sets are dropped instead.
- **A stall is the run still open, paired by class** (B303–B305). The rule kept the longest run it had seen,
  reset a run when a set was simply not logged, and paired by raw position. It reports only a run open at the
  newest session, skips absence, and reads the offer's own by-class pairing.
- **A bodyweight hold keeps its record** (B306). The record read dropped bodyweight rows before the hold rule,
  so a Plank had no longest hold. The rep table still excludes them, because their added weight is not a load.
- **An edit that moves only a hold is planned** (B307). The shared set comparison skipped the duration, so the
  app said the program was updated and wrote nothing. The hold is compared like the reps, and the page build is
  regenerated.
- **The shared validator refuses a hold the app refuses** (B308). A zero-second hold, or a set naming both reps
  and a hold, passed the page's validator. Two problem codes and the two rules close it.
- **A failed decline is said, and a taken step clears it** (B309, B310). The finish cleared the decline's error
  before it could be shown, and accepting a step left *step offered, not taken* behind. The error survives the
  finish, and an acceptance writes the empty value.
- **The library's empty state is the list the body drew** (B311). The state re-derived the list without the
  last-trained data, so *Not trained* could draw a blank page. The body asks its rows.
- **A hold's shortfall is part of the one-reason check** (B312). Two holds short by different amounts collapsed
  into one line stating the first one's number. The hold's measurement now has to agree.
- **The docs state what the code does** (B313–B315). The unreleased changelog has one section per heading and
  one entry per row, the schema story covers 14–16, and DECISIONS records the hard-delete exception with its
  retired-id map and the movement-level answer as training data.
- **The retired map loses what only its writer read** (B316). Four unused DAO methods, an `at` nothing read, an
  unused stall window and duplicate test tags are gone. The `at` is dropped by schema 17, which recreates and
  copies the table because the supported APIs have no `DROP COLUMN`.
- **The history section reads the exercise's own unit** (B317). A pound movement had its history and best in
  kilograms. The one helper every exercise-scoped display uses is the one it calls.
- **The schema and transfer tests check what they claim** (B318). The chain test counts every planted row, not
  only the exercises one; the declined column is written before the ALTER and read back through the DAO; and
  the retired-id resolution has a JVM test.
- **The library's and the seed's tests stop being blind spots** (B319). The ruled table pins its survivors and
  sources against the seed, the empty-group fallback is tested, five aliases the search already answered are
  gone, and the catalog's regeneration note names the right path.
- **The refused load names the program the device holds** (B320). After a rename the question named the file's
  old name. The payload carries the held program's name, the refusal is the field's supporting text, and the
  copy path's name check is read inside its transaction.
- **The instrumented suite ran against the batch** (B321). 433 declared, 433 executed, none skipped, on a
  device — the report it replaced predated the newest test source.

- **A seeded family no longer holds a movement's id, so *Back Extension* exists** (B292). A database written by an
  older seed holds the *category* under an id this seed uses for a movement, and `INSERT OR IGNORE` cannot take a
  taken id — so the movement never arrived and the lift could not be logged at all, while a second family of the same
  name was drawn beside the corrected one. B148's repair tried to move that row aside and asked for one that was still
  its own parent, which is not the shape on disk, so it never fired. It now matches what is actually there and what no
  write path can produce: a row the seed claims for a movement, under that id, of another kind, still carrying that
  name. Its children are re-filed onto the family the seed files that movement under, and the impostor is deleted
  rather than soft-deleted, because a soft-deleted row keeps its id and would block the movement exactly as before.

- **The seed says which movements are bodyweight and which are held** (B293). The seeder's insert wrote ten columns,
  and `isBodyweight` and `isTimed` were not among them, so every seeded movement arrived as an ordinary counted lift:
  a weighted pull-up was recorded, charted and record-checked as its belt alone, and a plank could not be logged as a
  hold. The seed declares both flags now — the twelve bodyweight movements, the assisted pull-up whose machine takes
  weight off, the ab wheel, the three planks and the two carries — and a device that already holds the row is raised
  to the seed's truth and never lowered, so a flag a lifter set stays theirs. The catalog the import helper reads
  carries them too, which is what stops a pull-up resolved from a name arriving as an ordinary lift.

- **The audited taxonomy names the lats and the upper back** (N240). The Row family named upper back and biceps and not
  the lats, which is half of what a row is for, so a program's balance figure counted no lat work for the rows that
  train them most; the Lat Pulldown family was the mirror image, missing the upper back that retracts the shoulder
  blades. Both corrections are written behind the insert, matched on the list the seed shipped before, so a lifter who
  set their own list keeps it — and because a family's muscles are what its members read live, the whole family moves
  and the balance numbers of plans already authored are reshaped.

## [2.18] — 2026-10-29 (versionCode 38)

### Fixed

- **A required action cannot be forgotten, and the dialogs survive a rotation** (B283). Every screen's required
  callbacks lost their empty-lambda defaults: a screen that calls an action the caller never wired used to compile
  into a silent no-op, the B49 failure the rule exists to prevent. Optional slots keep their defaults, and previews
  now pass explicit no-ops, which is what makes them look like the call sites they stand in for. The parameter
  order follows the platform's rule (required actions, then `modifier`, then optional ones), and the workout
  preview's sample state moved to a property so the preview stays inside the function length this project allows.
  The rotation half: the flags that open the set editor, the rating dialog, the note field, the readiness field and
  the bodyweight prompt were plain `remember` state while the fields inside them were `rememberSaveable`, so a
  rotation closed the dialog and threw the draft away; they are saveable now, and the set editor is held by its
  row's id because a `SetRow` is not.

- **A refusal is not a silent no-op, and an editor waits for its answer** (B284). The target dialog's Save was
  always enabled and did nothing at all when the text did not parse, so a typo looked like a dead button; it is
  disabled until the number reads, as the period editor beside it already does. The adherence day read reported its
  failure through the write channel while opening the dialog on an empty occurrence list — a claim that nothing was
  scheduled, when the truth was that the day could not be read; it says so and keeps the dialog closed. And both
  set editors — the workout's and the template editor's — fired their write and dismissed, so a refusal arrived
  after the values were gone and a correction had to be retyped. They close on the write's own answer now, the rule
  the goal editor already followed (B234): a written set closes, a refused one keeps the dialog and the values.

- **No dead weight, and two figures that could lie** (B291). What nothing calls is gone: a test tag with no
  control, an empty slot no caller passes, a plan field written and never read, a transfer outcome carrying a
  message nobody reads, a DAO read that duplicated the one production uses, and a goal-timeline helper only its
  test called. The one-rep estimate is floored at the weight that produced it, because the half-kilo rounding
  could take a sub-step load to zero; `higherIsBetter` is the model's answer for joint pain rather than a patch at
  each call site; the newest-weight read asks for a daily entry, which is what a weigh-in is; and adding a movement
  to a session reads and writes its position in one transaction.

- **The workout screen says what it draws, and the chart keeps its gesture** (B290). A failed write and a new
  record were drawn but never announced, while every other screen routes a failure through a snackbar — a polite
  live region; both are live regions now, and an edit or a delete clears the record banner instead of leaving a
  claim that is no longer true. The chart's gesture loop keyed on the zoom callback while reading the scrub one, so
  a scrub after a range change read a stale chart, or the loop restarted mid-gesture. The measurement fields ask for
  a decimal keyboard for values that are decimal, and the review's verdict is drawn in the label colour rather than
  the fill colour N49 measured below body-text contrast.

- **The docs state facts the code agrees with** (B288). One pass corrected the claims the review found stale: the
  formatting measurement (276 findings across 97 files now that the task's report is formatting-only), the backup
  ceiling (7, not 6), and the migration rule, which named a `MigrationTestHelper` this repository does not carry and
  now names the test that exists. README repeats the Gradle and Kotlin versions it points at the catalog for, and
  they were both behind. The program contract said a document's kind and start are ignored by an edit, which N186
  reversed. The Robolectric pin says what was measured — 36 and 37 fail on this toolchain — instead of "the newest
  it supports", which was not true. And the instrumented transfer test no longer claims the minified release runs.

- **The helper refuses what the app refuses** (B279). The same-list guard compared rows as stored, and the page
  writes `__source`/`__row` onto them, so its signature never matched again and re-dropping a file doubled the
  model instead of refusing it. The shared validator was also weaker than the app's own write boundary: the RPE
  floor was one half-point low, a rung could carry a current rep target, and the three run rules the template
  editor enforces were absent — a value on a set that is not a drop, a value on a later rung, and a drop under
  nothing to take off. Those are codes now, with the app's own sentences on the page. And a document newer than
  the page's core is refused when it is opened, instead of being refused as an edit and silently written as an
  older add.

- **A statistics write and the record count tell the truth** (B281). Two writes on the statistics screen dropped
  their result — a target and the recents row — while their own KDoc said a failure has to be visible; both report
  through the screen's settings-error channel now, which carries every write on that screen rather than only the
  range. And the record count filtered on the *added* weight being positive, so a weighted pull-up counted as a
  record the live announce and `personalRecords` both refuse: it asks `bodyweightGrams IS NULL` on both sides now.

- **A program's version and its export keep their promises** (B287). `keepVersion` read the plan outside the
  transaction its own comment says spans the reads and the copy, so an edit landing between them produced a version
  of a plan that never existed; the reads are inside it now. And the document export carried every slot but only live
  workouts, so a program using a deleted workout exported a document its own validator refuses — and the receiving
  merge dropped the day in silence. A day whose workout is hidden is not exported, which is what the screen shows.

- **The next set's fields take the history when it arrives** (B272). The draft was keyed on the row and its
  set count, and neither changes when the asynchronous previous-performance read returns — so the fields kept
  their 8 × 20 kg fallback while the reason line beside them read "Last time …", and *Log set* wrote the fallback.
  An untouched draft is re-seeded from the suggestion now, and one the lifter has edited is left alone. A read
  that fails is said rather than swallowed, so the fallback is not a silent claim about a lift.

- **The plan review pairs by occurrence and judges the load** (B274). A session may hold the same movement twice,
  and the review keyed performed rows by exercise id, so one row's sets vanished from the comparison while the
  totals counted both — the loss B20 fixed for the name key, still there under the id. It also compared only the
  added weight, which is zero on both sides of an assisted plan, so an assisted review always said *Matched*
  whatever the machine took. One planned row consumes one performed row in order now, the surplus is the
  improvisation it is, and the verdict is the signed load — which also stops the chart drawing a plan line at 0 kg
  beside an assistance series.

- **Three window bugs** (B282). `zoneIdOrNull` promised null and threw outside ±18:00, so one imported session
  with a nonsense offset killed the history flow instead of failing the import. The chart's date labels spanned the
  readings that recorded something while the plot projected every reading, so a series ending in a gap named a day
  that was not where the axis ended. And the next-session line read this week's deload plans for candidate days in
  the next week, so the Sunday of a deload week named the deload it was leaving.

- **A restore cannot refuse or lose a body measurement** (B269). Two defects in one path. The sore rows were
  inserted after the measurements passed a day filter, so a file entry that yielded to a day the device already held
  took its sore rows' foreign key with it and the whole import transaction rolled back — a valid file refused. And a
  file's own soft-deleted entry reserved its day before the live one was reached, so restoring a save/delete/re-enter
  history into an empty store inserted the hidden row and dropped the live one. The sore rows go in only for a
  measurement that is actually there, and only a live row reserves its day.

- **A backup prune cannot take the copy it just wrote** (B285). Files were ranked by their raw names, and a
  collision copy (`…-000-2.json`) sorts *below* the file it collided with, because `-` sorts before `.`. A clock
  moved backwards could therefore name a copy with an older stamp, put it past the five-file mark, and have it
  deleted in the same call that recorded the success. The prune ranks by the stamp and the suffix now, and never
  touches the name the run just wrote.

- **History's *Do it now* starts the missed day** (B271). The missed-day question returns one of two
  shapes — the repeat that was asked for, or the missed day's template — and the history host handled only the
  first, so answering *Do it now* closed the question and started nothing. Both shapes go to their own
  navigation now, through a pure branch the tests assert.

- **A second tap inside a write is not a second row** (B273). *Log set* stays enabled until the write returns, and
  the mutex only orders the turns it is handed, so two taps in one frame wrote two identical sets — the volume, the
  record comparison and the plan progress all counted both. The row is claimed before the write is queued now, and
  the picker's append does the same for the movement it is adding.

- **A rating's target is one scale** (B270). `MetricUnit.RATING` parsed a typed target by doubling it into the
  half-points the tables store an RPE in, while formatting and every series read the same number as a point on the
  1–10 scale. A target of 7 was stored as 14, printed as "14.0" and drawn at 14 on an axis that stops at 10; every
  rating trend was reported at half its slope. The unit is points throughout now, and **schema 12** halves the
  targets already stored that way — only the rating series, because a weight target in grams and a body-fat target
  in tenths must not move. The tests that pinned the old convention now assert the round trip instead.

- **A program document carries the whole movement** (B277). The page's sheet builder copied eleven of the seventeen
  fields the format holds, so a program authored there recreated a weighted pull-up as an ordinary lift, a
  pound-display movement as kilograms, and lost its cue and default rest — B247's bug, on the other host. It copies
  the row whole now, and a page test builds a movement carrying every field and asserts each one survives.

- **The helper's progress chart reads the load, not the added weight** (B278). It filtered and ordered on the added
  weight alone, so a weighted pull-up charted at the belt instead of the lifter plus the belt, and an assisted set
  charted nothing. It uses the app's `loadGrams` rule now — the lifter, plus what was added, less what the machine
  took — and the node suite pins both shapes to the numbers the app would read.

- **The page's browser tests assert the version the core writes** (B280). `docs/tests.html` still expected document
  version 4 after the format reached 5, so the file failed — and no gate ran it, because the docs job runs the node
  runner only. The assertion reads `programFormatVersion()` now, as the node checks already do, so the next bump
  cannot make it stale again; the sheet it checks is the one the node fixture covers, which is the gated copy.

- **A program can carry a failure set** (B276). The shared role enum listed five of the app's six roles, so a
  program whose plan prescribed a failure set could not be exported at all — the encode threw and the lifter was
  told a generic storage failure — and a document naming one was refused as corrupt by both hosts. The plan editor
  offers that role wherever a rung is not the answer, so this was a plan the app could make and the format could not
  write. The document's version moves to 6 with the role and its floor stays at 4 (N185's split: a v5 document still
  means what it says, and a v5 build reading a v6 one would have taken the failure set for an ordinary one). The
  page's editor gains the role, and its own tests read the version from the core rather than spelling a number that
  has now moved once.

- **The gates run what they claim to check** (B289). Five holes, each small and each in a check that exists to
  catch someone else's work. The docs gate read three documents Gradle was never told about, so editing
  `IMPORT-HELPER.md`, the 1.x changelog or the device-loop skill left it UP-TO-DATE. The formatting task loaded the
  Compose ruleset beside the formatting one, so its report carried a finding from the gate and the measurement
  DECISIONS quotes included it; it now has a classpath of its own and the report is formatting findings only.
  `tools/format-changed.py` applied its ImportOrdering exception *after* the changed-line filter, so the exception
  could never fire — detekt anchors that rule at the file's first import, which a change rarely touches — and it
  could read a report the task did not write; it decides imports from the base block now, deletes the report first,
  and fails on a rule name it does not know. `tools/verify-release.py` treated `CI=false` as CI, so the certificate
  check it exists for was skipped, and sorted build-tools names as strings, so `9.0.0` beat `36.0.0`. CI's
  generated-file checks now use `git status` rather than `git diff`, because a diff says nothing about an untracked
  file, and the failure summariser writes the job summary it has always claimed to write.

- **A migration is checked against the schema it ships** (B286). Each migration's test built the tables it touched
  by hand and called the migration object directly, so Room's post-migration validation never ran — a migration
  producing the wrong type, constraint or index would pass there and refuse to open on the first device after an
  upgrade, with no destructive fallback. `MigrationSchemaTest` builds each old store from its exported JSON, puts a
  row in every table, runs the whole chain and lets Room validate the result; a no-op 10 → 11 is refused, which is
  the proof that validation is live rather than assumed. The chain itself is one `ALL_MIGRATIONS` list now, and
  `AllMigrationsTest` asserts it is contiguous from 1 to the newest exported schema, so a forgotten migration is a
  red test rather than `A migration from 11 to 12 was required but not found` on every upgrade. Schema 10 is the one
  version the harness does not upgrade: it was current only while two changes shared an unreleased cycle, its
  `createAllTables` lacked a default that `MIGRATION_9_10` adds (B240), and no store at 10 ever left the repository.

- **The unit suite proves what it ran** (B268). The plan-side superset tests were local functions: a missing
  closing brace put both `@Test` methods inside the test above them, which compiles, is invisible to JUnit and
  meant they had never executed since the change that added them. Both run now, and the one written against the
  pre-B27 two-write pairing was corrected to the single write B27 shipped — a stale expectation the dead test had
  hidden. Nothing compared the JVM suite's declared tests with its result XML; the check existed for the
  instrumented job only, so it was generalised: `tools/ci-check-tests.py` reads both suites, and a `@Test` that
  sits inside a function rather than at its class's own body depth is a failure rather than a silent omission.

- **A green suite now has to prove it was complete** (B275). The instrumented count read `tests` alone, so a
  suite whose cases were all skipped still reported every test executed; it compared one grand total, so an old
  report could supply the number for a class that never ran; and its "replayed report" guard compared the XML
  against the top-level test directory's mtime, which a content edit never changes. The check now reads
  `skipped`, compares class by class, and takes the newest test source's mtime.

- **A dropped table is its own program** (B266). A program table's ids were a slug of the program's *name*, so a
  second, different spreadsheet that happened to say *Push Pull Legs* carried the first one's `pgm_…` id — the app
  answered *That program is already here.* and added nothing — and every table without a `Program` column collided
  on *My program*. The page seeds a table's ids with **the file it came from** as well as the name now: two
  spreadsheets are two programs, and re-dropping the same file is still a no-op. The cost is stated rather than
  hidden: renaming the file, or *Save as* under a new name, makes a second program. A **pasted** table has no file
  to name it, so its ids are exactly what they were — every program imported from a paste keeps its id — and a
  paste whose program name is already on the device is still answered by the app (B267).

- **A name taken by another plan is said, not called "already here"** (B267). The load's idempotent sentence was
  decided by the counts alone, and an id that is already here has two causes: the same document loaded twice, or a
  different document the seed happened to give the same id — a pasted table's id is a slug of its program name, so a
  second table that also says *Push Pull Legs* carries the first one's id. The second case answered *That program is
  already here.* about a program the device had never seen. The merge compares the document's planned movements —
  day, movement and place — with the plan it already holds under that id, and when they differ the card says *A
  different plan is already here under that name, so nothing was added.* The load still writes nothing either way
  (B180): what changed is only which true sentence it ends with. Loads, rep ranges, rests and notes are not part of
  the comparison, because the import would not have merged them over a live program either.

## [2.17] — 2026-10-27 (versionCode 37)

### Changed

- **The statistics page says what the series means before it lists what it is** (N221). The average and the trend
  lived inside the readings disclosure, so the one screen that exists to say what a series means hid that answer
  behind the rows it summarises — with the target rate below the fold those rows lived in. They are drawn under the
  figure now, Average then Trend, and the metric's target follows them: *Target rate* for weight, *Target* for a
  metric with no rate. The window totals keep their place after the figure and move above the readings, so the page
  ends on its evidence, and the disclosure states whether it is open — its label carries the count and its icon the
  direction, and TalkBack reads neither.

- **The averaging window shares a row with Expand** (N222). *Average of last: N* and *Expand* were two rows under
  the dates for two controls that both decide how much of the figure is read; they share the line, so the card
  reads figure, dates, actions.

- **Tapping the tab you are in shows that tab's front page** (N223). Selecting a different tab still restores it —
  the exercise you looked up, the workout you opened, the metric you were comparing — but the tab already showing
  now returns to its root, keeping its own state: the thing the bar's button never did. At the root the tap does
  nothing, and always-reset was not taken: it would drop the Library search and the Statistics selection with the
  rest of the tab.

### Fixed

- **A plan imported as a document starts on the right set** (B257). The import helper numbered a program
  table's planned sets from 1 while the app keeps them from 0, and the workout finds a plan's row by how
  many sets are logged — so a program loaded from the helper opened its first workout with the first set's
  target missing (no *Plan* line, the fields on history's fallback or the default) and every later set
  wearing the row above it: a ramp prescribed the load of the set before, a plan opening with a warm-up
  armed its first set as work, and its last row was never used. A plan is now read by its **order**, the
  place B72 already gave it, so a set is prefilled from the row that is actually next. The helper writes the
  app's indices now, and an already-imported plan reads correctly as it stands — nothing is renumbered
  under the lifter.

- **The next step is earned from the load actually lifted** (B258). *Done* judged a set on its reps and its
  effort and never read the weight, so a set answered at a lighter bar than the plan named was told to add
  weight — on top of a prescription it had not loaded — and a set done *above* the plan was offered a step
  below what it had just lifted (140 kg beaten by 150 offered 142.5). A set performed lighter than the plan
  now earns nothing and says how much lighter; at or above it, the step is the plan's next step above both,
  so 150 against a 140 plan becomes 152.5. A set that states assistance is not compared — the machine's help
  is a different quantity — and bodyweight plans, holds, warm-ups and rungs are unchanged.

- **The figure's scale reads along it, not across its top** (B259). The axis gutter asked for `fillMaxHeight()` on a
  page that scrolls, where the height constraint is infinite and the modifier resolves to wrap-content: the row took
  its height from the canvas, and the three numbers — max, middle, min — stacked in a line at the plot's top, so
  nothing said which height a value belonged to. The gutter is given the figure's height, which is the height it
  was always meant to read against, in the card and in the expanded view alike.

- **The statistics figure takes the room its window has** (B261). It was a fixed 180 dp — a fifth of a phone's
  screen, on the screen whose subject it is — because the constant was picked when the card still carried a row of
  window chips. It now takes 30 % of the window clamped to 220–340 dp: about 274 dp on a 914 dp phone, with a floor
  that keeps the dates and the actions and a ceiling that stops a tablet getting a second expanded view by default.
  The expanded view stays the larger of the two, which is what makes *Expand* mean something.

- **A bar stays inside the figure** (B260). A bar is centred on its reading, so the first one was drawn half outside
  the plot and, with nothing clipping the canvas, painted over the scale's numbers in the gutter beside it — which
  is how 722.5 and 361.25 came back partly hidden after the scale itself was fixed. The figure's frame clips now, so
  a bar is cut at the plot's edge instead of covering the axis; the adherence breakdown shares the frame and the
  same fix.

- **`./gradlew` needs nothing exported again** (B262). The wrapper regenerated by the 9.8.2 bump dropped the block
  that sourced `tools/android-env.sh` and scoped `HOME` to `.toolchain/home`, so the script the README describes —
  one that builds on a machine whose `$HOME` is not writable and whose `java` comes only from the workspace
  toolchain — did neither: `./gradlew` stopped at "JAVA_HOME is not set", and `connectedDebugAndroidTest` could not
  write the `adbkey` it needs. Nothing in the gate could see the loss, because CI has a writable `$HOME` and a JDK
  on `PATH` while `.toolchain/` is gitignored, so the deleted block was a no-op there and the job stayed green. The
  block is back, and a test pins it with the reason and the repair, so the next bump fails the pull request that
  deletes it rather than a build on someone's machine.

- ***Expand* is still the bigger read on a short window** (B263). The inline figure's 220 dp floor was unconditional
  and the expanded one is 55 % of the window, so below 400 dp the floor overtook it: a 360 dp landscape phone drew
  the card at 220 dp and *Expand* at 198, so the button that promises a larger figure made it smaller — and the
  floor's own reason failed with it, a figure taking 61 % of a short window being what pushes the dates and the
  actions out of it. The floor now bounds the *preferred* size only: the answer is capped below the expanded
  figure, so a window with room for 220 dp draws exactly what it drew before, and a shorter one gives the floor up
  instead of inverting the two reads. The test's range ran from 480 dp with a comment claiming that was smaller
  than any phone; it runs from 280 now.

- **A tab's front page is asked for by its own route** (B265). Returning to the tab you are in decided whether the
  entry on top was a pushed screen by asking whether *any* of its arguments held a non-null value, which was right
  only while `Statistics(exerciseId = null)` was the one tab root with an argument: a root whose parameter has a
  non-null default — the shape `Measurements(openDaily = false)` already has — would have read as a screen pushed
  on top of its own tab, so the tap took the switch branch instead of doing nothing. The question is whether an
  argument differs from its declared default now, which is the same as "is this the entry the tab's own route
  builds"; the lift reading is still not the Statistics front page, and both answers are pinned.

- **The bar's selected tab is a tested rule rather than a host detail** (B264). Two things write it — the route the
  stack settles on, and the tap itself — and the tap's write is the one that matters: a tab **restored at a
  detail** settles on a route that names no tab, so without it the next tap on that tab reads as a switch. Both
  writes lived in `AppNavHost`, where nothing could reach them without Hilt and every screen. They are
  `TabHighlight` now, saved by name: the route path, the restored-detail path, the switch and the re-tap, and the
  save-and-restore are each asserted, and every tab's own route is pinned as its front page so a sixth root with a
  parameter fails a test rather than a button.

## [2.16] — 2026-10-26 (versionCode 36)

### Added

- **A set can be a hold instead of a rep count** (N220). An exercise can be marked *timed*, and its sets then
  record how long they were held — the row reads `0:45` — with the weight field still there, because a carry is
  loaded as well as timed. A hold states **no reps at all**: reps go to zero rather than a forty-five-second
  plank reading as forty-five of something, which is exactly the lie the old shape told. Volume stays
  `reps × load` and records stay by reps, so a hold contributes to neither — the same answer N15 gave
  bodyweight before N219 reopened it, and the honest one here. The set floor and the ± pair follow the shape:
  a hold steps fifteen seconds, is refused at zero, and the correction dialog edits it as a hold rather than
  as reps. A plan can still only prescribe reps, so a timed movement inside one ignores its rep target; that
  is this change's boundary, stated rather than hidden.

- **A backup writes itself when the app is next opened** (N218). Periodic backup as a setting: pick a folder
  once, choose Daily, Weekly or Monthly, and a timestamped file is written there the next time the app comes
  to the front after the period has run out. It never runs in the background — no job, no alarm, no
  notification — so the app still declares no permissions and P4.7 stands; a window missed because the app
  stayed shut is paid at the next launch, and the settings row's date is how that becomes visible. The newest
  five files are kept and older ones pruned, and only files this app named are ever deleted: the folder is
  the user's. The run is ordered export, write, prune, record, so a crash anywhere leaves the previous copies
  intact and the period still due. A failure keeps the date of the last success beside it, because "the last
  backup failed" without "when one last worked" cannot tell a week from a minute.

- **A bodyweight set adds the lifter's weight to the load** (N219). An exercise can be marked bodyweight, and
  its sets then count bodyweight plus what was added, less machine assistance — so a 75 kg lifter with a 20 kg
  belt is lifting 95 kg, and an assisted pull-up taking 30 kg off is 45 kg. The weight is snapshotted onto the
  set when it is logged, so correcting a measurement later changes the record of the body without rewriting
  what was lifted. The load is one function, and the history list, the workout detail and the trend's volume,
  heaviest set and 1RM all read it, which is what stops them disagreeing. Records stay by added weight: N23's
  rule about bodyweight sets was written before this and was not reopened. With no weight on record, logging
  holds the set and asks; the answer is saved as today's daily measurement, keeping whatever else that day
  already says, and dismissing the prompt logs nothing rather than a set of zero kilograms.

- **A deleted workout can be restored** (N216). A delete was final in the app even though the plan was never
  really gone: the row and its exercises and sets stayed, which is what let a backup written *before* the
  delete bring it back, and nothing else could. The templates list now carries a *Deleted* chip beside the
  hidden one, holding every deleted workout with a *Restore* on its row. One write clears the delete flag;
  the plan returns with it, and a program day that still pointed at the workout reappears on its weekday,
  because that list only ever needed the workout to be live again. A workout deleted while hidden comes back
  hidden. A day *removed* from a program is still removed — restoring the workout brings back the plan, not
  the schedule you deleted separately — and nothing is restored automatically: a backup from before the
  delete is still the undo for a delete that was meant.

### Changed

- **The templates filter shows one group at a time** (N217). *Hidden* used to be a reveal: it added the
  hidden workouts to every live one, so the list a lifter opened to see what they had put away was longer
  than the list they left. The chips are modes now — *All*, *Hidden (n)* and *Deleted (n)* — and each draws
  its group alone, with the counts read from the whole library. A group that empties while it is selected
  (the last *Unhide* or *Restore*) falls back to *All* rather than leaving a blank list under a selected
  chip, and the hidden row no longer carries its own *Hidden* marker, because a hidden row never stands
  beside a live one any more.

### Fixed

- **The library keeps what a movement is** (B240). `updateExercise` rebuilt the row field by field and never
  mentioned `isBodyweight` or `isTimed`, while the write is a whole-row update — so every save put the stored
  *false* back. Neither flag could be switched on at all: the switch read as saved and the next open
  disagreed, which left the two features above unreachable until it was fixed.

- **The finish review counts the load the history counts** (B241). The review's volume was added weight ×
  reps while the history and the workout's own SQL counted the lifter as well, so an all-bodyweight session
  finished on "0 kg" and read 600 kg in the history. It uses the same rule now.

- **A prefill adds nothing it was not told to** (B242). With no plan and no history a bodyweight movement
  prefilled 20 kg, which is 20 kg *added*: a first pull-up logged at 95 kg for a 75 kg lifter, and a plank
  opened loaded. The fallback opens at zero when the field means what was added, while "what I did last time"
  still wins where there is one.

- **A hold is stated as a hold** (B243). The finish review read "3×0 at 20 kg" for a carry and the next-step
  dialog called it "24 reps short of the plan". A hold now reads `3×0:45`, the plan's rep target is not
  stated for a timed movement because it does not apply, a session of holds is counted in holds, and
  progression answers with its own sentence instead of a rep shortfall.

- **A plan saved from a hold session has no rep target** (B244). The copy wrote `0` into all three target
  columns — a plan the app's own validator refuses and a program document that cannot be imported. The plan
  names the set and its load, a shape it already allows.

- **A bodyweight set reads as the load it was** (B245, B246). Set rows printed the added weight beside a
  volume that counted the lifter, so a pull-up read "0 kg × 8"; both screens now share one sentence that
  reads `95 kg × 8 · BW`, the weight field says "Added weight" on such a movement, and a bodyweight set
  claims no personal record at its belt — N23's rule, restored.

- **A program document keeps both flags** (B247). The shared exercise model had no field for them, so a
  document recreated pull-ups as ordinary movements and planks as counted lifts. Both travel now, defaulted,
  and the page's copy of the core is regenerated.

- **An automatic backup never rewrites a copy** (B248). Names were second-resolution and an existing name
  was reopened with a truncating stream, so two foregroundings inside one second could destroy the copy just
  written; a failed write also left a partial file among the five kept. Runs take a mutex, names carry
  milliseconds, a taken name lands beside it with a `-2`, and a failed write deletes what it created.

- **A failing backup retries at the period's pace** (B249). Due-ness read only the last success, so a deleted
  folder ran a full export on every foregrounding and failed at the write. The attempt is recorded and is the
  retry's clock; a clock that went backwards is due rather than a silent stop.

- **The backup's own words are true** (B250). "Nothing leaves the device" is not true of a folder another app
  syncs; the hint says the app has no network access of its own instead. Pruning also matches the app's own
  name shape, so a `workout-backup-notes.json` in the folder survives.

- **A stale or unreadable weigh-in is asked about** (B251). The newest weight ever was written into every
  bodyweight set, so a six-month-old number loaded a set without a word, and a failed read became "nobody has
  weighed in", which asked and then wrote a measurement over the error. A month-old weigh-in now asks, with
  the age in the question; a failed read is said and nothing is written.

- **The two flags and a hold are named for a screen reader** (B252). The switches carried no accessible name,
  the hold field promised `m:ss` on a keyboard with no colon, and a hold was read as digits around a colon.
  Each switch is named by its label, the field accepts a colon, and a hold is spoken in words.

- **A hold raises the backup format to 7** (B253). A hold is `reps = 0` beside a duration an older build
  ignores, so it would import as an invalid zero-rep set and be clamped to one rep on the next save. The
  floor stays at 5: every older file still reads. **Schemas 10 and 11** join the database's own account
  (B254), and the rest of the review's small print is in (B255): the weigh-in answer is one targeted write,
  `1:75` is refused rather than read as 2:15, and the migration tests say what they actually prove.

- **The day's block keeps its place at the foot of the main screen** (B239). A day with nothing scheduled
  led with the no-plan line and *Record today's measurements*, while a day with a workout kept them under
  the ways in (B182) — so the two rows a lifter checks most moved from the top of the screen to the bottom
  as the calendar changed. The day's block now always comes last: the workout, the rest line and the record
  row in one place, under the ways in. What that costs is the compact-screen reachability B182 bought — on a
  short screen the day's rows are a scroll away — and the trade is deliberate, because a row that moves is
  worse than one that is consistently below the fold. The rest of B182 stands: the record row is never
  absent, and it opens the day's own entry rather than a blank form.

## [2.15] — 2026-10-25 (versionCode 35)

### Added

- **A weight goal's past can be added, edited and removed** (N215). The goal's history was read-only and could
  only grow at the clock's now, so a stretch that began in March could not be entered and one entered wrongly
  could not be corrected. A period is now a Start, an optional End and a kind — Target, Lose, Gain, Maintain or
  **No goal** — matched by the day, so a goal "from 1 March" owns that day, an older row written at a clock time
  is the same statement, and two changes on one day are one. A goal-only correction keeps the stored instant; the
  undated first period keeps its unknown start; a finished stretch writes the goal it replaced back as one
  restoring boundary, so the timeline has no gaps; a correction moves a boundary only between its neighbours;
  removing merges the stretch into the period before it, and the snackbar puts it back. The goal in force stays
  the newest boundary and keeps no end, a refused save keeps the editor open, and a correction re-reads that
  stretch's verdict, because the history is derived from what each period asked for.

- **A workout can be hidden from the templates list without being deleted** (N214). *Hide*, in the editor's
  overflow under *Versions*, takes a plan out of the choices while every program that names it still schedules
  and runs it; a *Hidden* filter in the list brings the hidden ones back in place, marked, each with its own
  *Unhide*. The picker a program's day chooses from stops offering them, which is the list's length dealt with
  where it actually hurt — while *Substitute* on home still offers a hidden plan, because a stand-in is what
  you train today rather than what a program is composed from. A new workout is always visible, and a backup
  still carries which ones were hidden.

- **A program opened in the editor can be downloaded as an add** (a request in use). *Download as new program*
  writes `program-add.json` — the same document with no `edit` marker — for a program the receiving device does
  not hold yet, where an edit is refused because the app plans an edit against a program it already has. The ids
  and timestamps travel unchanged, so the load adds it once and a second load of the same file adds nothing. There
  is no base to plan against, so the click is validated rather than planned: a refusal lists the app's problems
  and writes no file.

- **The statistics screen remembers the series you read** (N212). A short row above the series picker holds the
  three most recently chosen series, newest first, one tap from the chart — the current one reads as selected,
  and a per-lift series carries its lift, so bench volume and squat volume are two chips rather than one.
  Only an explicit choice is recorded, so opening the tab does not push the series actually read out of the row,
  and the row waits until there are two, because one chip repeating the picker's own label is not a shortcut.

- **Pinching the statistics figure changes the window** (N213). Fingers apart narrows it around the point
  pinched and fingers together widens it — never before the first reading or after the last, and never narrower
  than two days — and the result is a custom range, so the range row says *Custom* while it is on screen. The
  anchor and the scale follow the span the figure actually draws, a window that would leave a single reading is
  refused, and the window is written once after the fingers settle; a failed write says so.

### Changed

- **The *6 months* range is named that way on the row** (a request in use). The control read *Last 6 months* —
  the only one in the row carrying a word the other three drop — so the row says *6 months*, matching *7 days*,
  *1 month* and *3 months*. The menu behind *More* keeps the full name, where there is room for it.

- **The statistics chart lives in its own package** (N207). The overlay, the figure, the axis, the scrub and the
  series card moved out of the screen's file into `ui/statistics/chart`; the screen keeps the range, the picker,
  the readings and the summary. No behaviour change — the same composables, tags, strings and arithmetic.

### Fixed

- **The weight line no longer breaks on a tape day** (B221). Every measurement row became a reading, so a tape
  entry — which never states a weight — and a daily entry saved with the weight blank both inserted a null point
  into the weight series, and the chart stopped and restarted there although the weigh-ins either side were not
  in question. A body metric's series is now the entries that state it, so the line runs between actual
  weigh-ins and a skipped day is a longer segment rather than a hole. Tape sites, soreness scores and joint sites
  keep the break: there the entry *is* a moment of that series, and saying nothing about the site is the answer.

- **A real Waistline backup imports its body data** (B220). A real export dates every diary day with `dateTime`
  — an ISO instant at UTC midnight — while the helper parsed a `date` that only its own hand-written test ever
  wrote, so a real file produced **zero days** and the page still said it had loaded them. The day is now read
  from whichever field the file carries, the instant by its own date part so a western timezone cannot move a
  weigh-in to the day before, and a file that yields no daily record says why instead of claiming a load.

- **The import helper page opens again** (B222). The body panel read the reader's problems on every render,
  including the first one before a file is loaded and the one after *Clear body*, when there is no reader at
  all; the page threw during init and the helper was unusable. The panel's empty and broken-file plans are now
  pure and null-safe, and the node runner pins both, so a page that cannot render its first frame is a gate
  failure rather than a browser surprise.

- **A weight goal's past is corrected by the day** (B223, B224, B232, B233, B234). A boundary written before N215
  carries a clock time while the editor writes local midnight, so a goal set on that day could be inserted before
  it and never become the goal in force; a goal-only correction moved the boundary and took that day's earlier
  readings with it; a period restating the goal already in force opened a duplicate boundary; the undated first
  period could not change its goal without inventing a start; the removal's undo could write a day that a newer
  save had taken; and a refused save closed the editor and lost the typed values. Boundaries are matched and
  ordered by the local day, an unchanged day keeps its instant, a new open-ended period that restates the goal in
  force writes nothing, an existing undated row may keep its unknown start, the undo refuses a taken day, and the
  editor waits for the write's result.

- **The goal card waits for the measurements to load** (B225). The state's `today` is `LocalDate.MIN` until the
  first emission, and the card was drawn during that frame; opening its editor formatted the sentinel and threw.
  The card is drawn only after the list has loaded.

- **The recents row keeps the lift** (B226). The stored entry was only the metric's id, and an Exercise metric's
  id has no lift in it; two lifts' volume were one chip, a chip pressed from bodyweight left the lift empty, and
  a lift the library had lost was selected invisibly. The entry is the metric and its lift, the chip names the
  lift and restores it, choosing a lift records it, and the reader drops what it cannot offer, dedupes and keeps
  three.

- **A pinch follows the chart it is on** (B227, B228, B229, B236). The focus was mapped over the range window
  while the figure's axis spans the windowed readings, so a sparse window anchored the zoom days from the
  finger and a left-edge pinch could empty the figure; the scale was applied to a state that catches up after
  three flow hops, losing events; the settled write was dropped and cleared without a word; and each motion
  event ran a record-count query. Focus and scale use the plotted span, the gesture reads the pending window
  synchronously, a window with a single reading is refused, a failed write reaches a snackbar, and the count
  follows the settled range.

- **The goal editor and the range row say what they mean** (B230, B231, B235). The card's goal editor was titled
  *Add a past goal*; removing a period said *Goal removed.* although the previous goal comes into force; and the
  year range kept the *Last* the row had dropped for six months. The title travels with the editor, the snackbar
  says *Goal period removed.*, and the row says *1 year* while the menu keeps *Last year*.

## [2.14] — 2026-10-24 (versionCode 34)

### Added

- **A measurement's day can be corrected** (a request in use). A record's day was fixed when it was saved, so a
  morning typed onto the wrong date had no way back. The edit form now carries the day above the numbers, and
  changing it keeps the entry's **id and hour** — the same reading on another day, not a second one beside it.
  A day holds one record of each kind, so a move onto a day that already has one is refused rather than leaving
  two live entries; the form that adds a record still has no day field, because a new entry is today's.

- **The import helper draws a plan as its lines, and folds its days** (N172's form). Neighbouring sets that agree
  on every field are one line with a **Sets** count that grows or trims the rows behind it; *Split* breaks a line
  so one set can be written differently, *Merge* puts it back, and neither is written to the file. A program's
  days start folded, their heads saying how many exercises and sets they hold, and a day just added opens by
  itself.

### Changed

- **The weight goal is set where the body is recorded.** The goal's row — its kind, its number and the history
  behind it — sat under the statistics chart, which draws the past rather than where a reading is written. It is
  the measurements screen's first item now, and the chart keeps the goal's line; the energy statement stays under
  the chart, because it is a verdict about the trend. One open period per metric, closed by the next write, is
  unchanged.

- **Progression leads the workout settings.** It is the one switch there that decides whether the app edits a
  plan; the rest are about the timer the workout screen runs on.

- **The statistics screen is a figure you can read** (N196–N201). The chart drew at the height of the adherence
  sparkline, its range was seven chips wrapped three to a row, and its series menu listed every series the registry
  holds — the joint sites being the bulk of them. The figure is 180 dp now, with the **axis's numbers in a gutter**
  beside it (top, middle and bottom) instead of a row underneath, and it answers a touch: **the nearest recorded
  reading** under the finger is named below it, and **Expand** opens the same figure at the height of the window.
  The range is **one row** — a week, a month, three months, and one control for the rest including custom — and the
  menu opens on a **group** with a **search** that reaches **every group**, each hit naming its own, the twenty-four
  joint sites collapsed to **one row** at each level, grouped under their joint. Nothing about the series, the store
  or the goal's line on the chart moved.

- **The statistics figure says where you are pointing, and where you are in time** (N203, N205, N206, N208, N209).
  The scrub names the reading under the finger **and marks it on the figure** — a line down to the axis and an
  emphasised dot — and the readout is announced to a screen reader instead of only seen. The axis names a tick
  between its ends, the summary's three numbers follow the figure instead of preceding it, the joint sites are
  grouped under their joint, the expanded view says which series it shows and carries the card's own extras, and the
  moving-average window is one control naming the window on screen rather than three chips in the card.

### Fixed

- **An exercise's row keeps its actions beside its fields.** The program editor's name field takes the room the
  numbers do not need, so the move and remove buttons stay beside the exercise they act on; below the width where
  three numbers and three buttons stop fitting, the name takes its own line and the rest wrap as before.

- **The import helper counts a line without losing it** (B206, B207). Clearing a line's *Sets* count to zero used to
  clamp to one and delete the rest of its sets; zero, a blank, a fraction and a negative now all leave the line
  alone. Growing a line that was not the workout's last appended its copies after the last set, so the exercise's
  order became *2 · 2 · 2* where *4 · 2* was asked for and the line split in two on the next render; the copies are
  inserted inside the line now, and the sets after it shift along.

- **The statistics figure answers the finger and the screen** (B208, B210, B211, B213, B219). A drag on the figure
  scrolls the page again instead of dying on a scrub it did not want; the scrub holds the reading it names, so a
  range or metric changed under a held finger cannot name another series' value or crash; and the figure carries
  *Previous reading* / *Next reading* actions, with the expanded dialog named as a pane title, so a screen reader can
  move the readout without a touch. The series search reaches **every group** and each hit names its own, and the
  expanded view scrolls its body on a short window instead of clipping the extras.

- **A measurement cannot be dated after today** (B212). The calendar offers today and every day before it, and the
  save refuses a future day: a record is of the past, and a reading that has not happened would be plotted by the
  charts and read as the newest one by the rate verdict.

## [2.13] — 2026-10-22 (versionCode 33)

### Added

- **A block can be a program, not only a week** (P3.21, N174–N178). A program is no longer only a week
  that repeats: it says what kind it is — every week, or one block — and, for a block, the day it starts.
  A sequence's nine sessions are nine workouts, each with the loads it prescribes, done in order once and
  then over. Its weekday is a **placement**, not a recurrence: a session is due on the first such weekday
  after the last thing done, so a block trained late slides rather than counting a week as missed, and a
  start date in the future is not overdue. Home offers the next session as a card when it is due —
  *Day 4 of 9 · due Friday* — and starts it with the slot it was scheduled as, so the plan's own history
  still prefills it; a session that cannot be trained is skipped from its card, and a block's skip counts
  on the day it was taken; a rest day names what is coming, for a weekly program as well; and a block that is
  over says so, in the editor and on home, instead of marking its last session as next; and the adherence screen
  names each followed block's place, because that record is the one a block is naturally left out of. A block
  ends instead of wrapping, and the editor authors the kind and the start date, labels a sequence slot's weekday
  *Due*, and marks the run's place as *Day 4 of 9*. The document format moves to 5 and the backup's
  schema to 6, both with their floors left where they are: an older build refuses a block rather than
  repeating it forever, while every saved export, v4 starter program and older backup still reads.

- **The shared core passes the same static analysis as the app** (N179). `:shared` — the module that
  decides what an edit writes into the lifter's data — had no detekt at all. It now loads the app's
  config, minus the two rulesets only the app carries, and the eleven findings that first run reported
  were fixed rather than suppressed: the two longest functions became one small function per table they
  check, and the codec's swallowed serialization failures keep their cause.

- **The editor's tests run where the gate does, and the cross-host fixture is exact** (N180, N181). The
  program list's edit path — an add goes through, an edit stops at its question, the answer decides what
  is written — now runs as a JVM test over a hand-written fake rather than only on a device. The page's
  fixture asserts exactly which changes the planner names, in place of "each expected kind appears
  somewhere", and the node runner declares how many cases it expects and fails when it checks another
  number — which immediately found that a fixture case named "remove the only day" removed three.

- **A block can be authored entirely in the helper** (N186). The program table gains `Kind` — `weekly`, or
  `block` — and `Start`, a date like `2026-10-05`, said once for the whole table: two rows naming different
  kinds, or two starts, are refused rather than resolved by whichever came last. The program editor carries
  the same two controls, so a program already authored can be turned into a block, given a start and have its
  loads filled in before the app ever opens the file. The edit path applies them: a document that changes a
  program's kind or start names those changes in the preview, and the app writes them, because what a program
  is and when it starts are its definition rather than its placement (`isActive` and `position` stay the
  device's). A block authored in the helper now arrives as a block — no trip to the app's editor first.

### Fixed

- **The helper's block authoring writes what the app reads.** A program opened at an older format version
  and turned into a block is now written at the version this build reads (B198), so an app that does not
  know a sequence refuses it instead of repeating it as a week; a date is refused unless it survives the
  calendar round trip — February has no 31st — and a start the page cannot render is a message rather than
  a half-built editor (B204); a `Start` without `Kind` is refused and leaving a block clears its date
  (B205); and the preview says *one block, starting …* and words its summary from the change rather than
  from its kind, so it no longer calls a block weekly (B202, B203). The page's editor checks all run
  again: seven had been unreachable behind an early return, and the node runner had counted the fixture
  it was handed rather than the checks it ran (B199).

### Changed

- **The build job pins the Node it runs on** (N182), and the page takes the document's format version
  from the shared core instead of declaring its own (N183) — a version written in two places is the
  first thing to disagree — and has one download function rather than four copies (N184).

## [2.12] — 2026-10-22 (versionCode 32)

### Added

- **One program-edit core for the app and the page** (N170). The program document's model, its codec
  and version gate, and the rules an edit obeys now live in a Kotlin Multiplatform `:shared` module,
  compiled for the JVM the app links against and for the JavaScript the import helper loads. The app's
  codec delegates to it, so there is one reader and one writer for the document rather than two kept
  equal by hand, and `commonTest` runs the same cases on both targets. The document carries a shared
  `Weekday` enum serialized by name — the JSON does not change — and the app maps it at its transfer
  edge. Nothing user-visible changes yet: the edit path arrives with N171.

- **A program document can change the program it names, and the app says what it will do first** (N171).
  A document carrying an `edit` marker is no longer a proposal to add: the app plans it with the shared
  core, shows the diff — days, workouts, exercises, sets, the movements it would create, the workout a
  removed day leaves in the library, and any other program the edit also changes — and writes only on
  confirmation. A document exported before the program moved on the phone is refused and named for what
  it is, rather than merged into a state it was not made from. The format stays at version 4 (an older
  build ignores the marker and no-ops, exactly as it already does), there is no schema change, and a
  removal is still a soft delete: history and the library keep what the schedule stops using.

- **The helper edits a program, not just describes one** (N172). *Program editor* opens a program the app
  exported — or one taken from a backup, or a saved draft — and edits every part of it: days and weekdays,
  exercise order, rest, target RPE, supersets and cues, and each set's role, rep range, current rung, load or
  assistance, drop value and note. *Preview changes* is the app's own plan, so the page states exactly what
  the app will do and refuses what it would refuse; *Download edit* writes the document the app's **Load**
  applies after its confirmation, and a program that changed on the phone since the export is refused rather
  than merged. The table path still creates programs; this is how one is changed. A draft is a file, because
  the page still stores nothing and makes no request.

- **The helper's tests, fixtures and portable build are gated** (N173). The program editor's cases run in
  the browser and under node; CI runs the node copy against the committed `docs/shared` build, regenerates
  the program fixtures and the shared core and fails on a diff, and builds `dist/workout-import.html`. One
  fixture set (`docs/fixtures/program-edit.js`) holds the page's `planJson` and the app's Kotlin `plan` to
  the same answers, so a page whose preview drifted from the app is caught before a browser sees it.

- **The helper's tools are tabs.** Import, Programs, Progress and Body are four tabs rather than one long
  stack, so the program editor and the progress chart are one click away; each tab keeps its panels, Progress
  and Body say what to load before they have data, the selected tab is in the URL (`#programs`), a chosen
  tab starts at its top rather than at the old scroll position, and loading a program table switches to
  Programs. No roadmap id: a usability change, not a planned feature.

- **Programs and Body take their own files.** Each tab now has the same drop zone as Import: **Programs**
  opens a program export, a draft, a backup or a program table, and **Body** reads a Waistline backup and
  shows its daily rows. Neither imports anything else, and neither uploads. No roadmap id: the same
  usability change as the tabs.

- **A pasted history shows its progress before it is importable.** The chart read only the app export, so a
  rows file whose exercise names were not mapped — and which was therefore blocked from export — left Progress
  empty even though Import listed every session. The chart now falls back to the rows being imported, built
  without the import's mapping gate, and names its source; the export still refuses an unmapped name. No
  roadmap id: a fix to the helper.

- **A program can start from scratch in the helper.** Programs → *Start a new program* makes a blank document
  in the editor: add days, workouts, exercises and sets, check it with the app's own core, and download
  `program.json` for Programs → Load, which adds it as a new, inactive program. A new program has no edit
  marker; an opened export still downloads an edit with its base. No roadmap id: a gap in the helper's
  authoring flow.

## [2.11] — 2026-10-21 (versionCode 31)

### Added

- **The plan states its progression rule** (P3.17, corrected by B181). The block's foot now says what *Done*
  will do with the work: a set below its rep range's ceiling gains **one rep**, and a set at the ceiling gains
  the movement's **own weight step** (+2.5 kg by default, or whatever the exercise carries), both gated on the
  plan's target RPE — *Progresses by +1 rep below the rep ceiling, then by +2.5 kg at it, when a set meets the
  plan's RPE*. A plan with no target RPE, or no reps, says why it cannot progress at all; one with no weight
  says the reps are the only step rather than claiming one it cannot take. It records what the app already
  did; it does not add a second place to set a step.

- **The progression prompt is a choice, not a switch** (P3.17, part). Settings offers **Ask**, **Apply at
  Done** and **Off**. Apply opens the question with every earned step already picked — the lifter reads what
  the plan would become and one *Done* writes it — while Ask leaves it unpicked and Off finishes without
  asking. A device that never opens the new control keeps the old switch's answer: on reads as Ask, off as
  Off, and the old key is never written again.

- **The next set says where its numbers came from** (P3.17, part). Under the pending set's fields, a line
  names the two sources the prefill read: *Last time 5 × 100 kg, RPE 8.5 · Plan 5 reps, RPE 8*. It is a
  sentence, not a prescription — the remembered number and the planned one are labelled, and a set with
  neither source shows no line at all. The weight is read in the exercise's own unit.

- **A mixed plan takes every earned step in one tap** (P3.17, part). The progression question now offers
  *Take all N steps as offered* when the sets earned different directions — the rep below a ceiling, the
  load at it — so the one-tap answer is each row's own step rather than one direction chosen for the whole
  plan. It is still a pick, not a write: nothing reaches the plan until *Done*.

- **A plan's quick entry writes a whole block** (P3.18). Count, reps and load in one row — *4 × 6 @ 100* —
  appended as that many planned sets, starting from the last planned set. The per-set dialog stays for a top
  set and its back-offs, and the button is disabled rather than writing a half-parsed target; a blank load is
  allowed, since a plan may prescribe reps without a weight.

- **Adding an exercise to a plan prefills it from the last session** (P3.18, part). The last time the lift
  was trained becomes the plan's first targets — one set per working set, its reps as the range and the
  weight it was actually loaded with — while a warm-up is left out, because a plan prescribes the work. The
  values are ordinary editable targets: accepting them is doing nothing, and disagreeing is editing a field.
  A lift with no history, or a plan opened while the history read fails, still adds with no targets.

- **A workout can be created inside the program that needs it** (P3.18, part). *Add workout* now offers
  *New workout* before the plans that already exist: the template is created, its slot appended, and the new
  workout opened for the exercises it should hold. The two writes stay separate repository calls — a
  template is not a program's row — and the slot is added only after the template exists, so a slot never
  points at nothing.

- **A saved plan can be scheduled as it is saved** (P3.18, part). *Save as plan*'s confirmation now offers
  *Schedule it* beside *Open the plan*: choose a program and a weekday — or none, for an order-only slot —
  and the slot is written before the offer moves on to that program's editor, where the rest of the week is
  set. It is the second question the lifter already has an answer to at that moment: not only *keep this
  workout* but *train it on Wednesdays*.

- **Three examples on day zero** (P3.19). An empty Programs screen now offers *Start from an example* —
  full body, upper/lower, or push/pull/legs — beside the file picker and *New program*. Each is a real
  program document in the APK, inserted through the same codec and merge a picked file uses, so importing
  one twice adds nothing. They are structure only: seeded movements, **no loads**, a target RPE of 8 so
  progression has a gate, and a weekday per day. They arrive inactive, so the lifter renames and activates
  one rather than the app choosing; nothing is installed at startup, and a deleted example returns only by
  tapping its card again — the document's own program row is un-hidden (B177), while a deleted *exercise* it
  names is still dropped rather than resurrected (B51). `StarterProgramsTest` decodes all three and checks
  every reference, seeded id, the absent loads, and the RPE.

- **A program table becomes a program document** (N167). Drop a spreadsheet of `Program, Day, Weekday,
  Exercise, Sets, Reps, Load, Rest` into the page and it writes `program.json` for the app's Programs → Load:
  days become templates, weekdays become slots, a rep range stays a range, a seeded name keeps its slug, an
  unknown name is created as a movement, and a same-named program is reported for what it is — nothing to add,
  one to restore, or a second one. It creates and never edits (B180): a sheet-derived program's id is its
  name, a live program under that id ends the merge, and a changed sheet re-imported changes nothing. The app
  is where a program is changed. `ProgramDocumentFixtureTest` decodes the output with the app's own codec and
  checks the references the import would otherwise drop.

- **Waistline body data can be imported** (N166). Drop a Waistline backup (`waistline_backup_*.json`) into
  the page and it becomes daily measurements — weight, body fat and muscle — in the same backup file, with
  or without history. The unit is read from the file (kg or lb) and can be corrected, a day becomes local
  noon so a timezone cannot move it, and the row's id is a hash of its day and kind, so re-importing the same
  file adds nothing and restores a row the app hid. **A day the app already holds wins over the file**
  (B178): it keeps one entry of each kind per day, so importing a dated row never leaves a second live entry
  beside a day entered by hand — delete the device's entry and import again to take the file's numbers. Tape
  sites and custom stats have no place in a daily row: the panel lists exactly what it left behind rather
  than dropping it silently. The app needed no new format — its importer already restores measurements — and
  a fixture proves its codec accepts the output.

- **The import helper draws one lift's progress** (N165). Loading an export now shows a fifth panel: pick a
  movement and see the heaviest working set as dots and Epley's estimate from it as a line, in kilograms,
  over all sessions or the last year or 90 days. Movements only — a family head would need the app's roll-up
  rule — no unit toggle, and nothing stored: the chart is built from the file in the tab and dies with it.
  The numbers are the app's own: the estimate rounds to 0.5 kg, a single rep is the weight itself, and a set
  above 12 reps has no estimate. A generated fixture runs the app's `toExerciseTrendPoints()` and the tool's
  series over the same rows, so the two cannot disagree quietly.

- **The loaded export can be read back in the page** (N165). Below the progress chart, *In your export* lists
  every live session newest first — date, exercises, set count, working tonnage — and opens to its sets in
  kilograms, warm-ups marked and RPE beside the set. Deleted rows are counted rather than shown. It is
  **read-only on purpose**: the editable Sessions panel is the import draft, and a backup carries readiness,
  template, superset, assistance and ratings that a flat row list cannot, so a round trip would silently drop
  them; the app's merge would refuse to overwrite the rows anyway.

### Changed

- **The progress view lists its lifts instead of hiding them in a dropdown** (N165). The picker was a
  prefilled text field with a datalist, which showed only the name already in it and made changing lift a
  delete-then-type. Every movement with history now sits in a list beside the chart, with a filter above it:
  the current lift is highlighted, changing it is one click, and the filter narrows the list without touching
  the chart. It wraps above the chart on a narrow screen.

### Fixed

- **The day's measurements row is always on the main screen** (B182). The row sat at the foot of home behind
  four full-width way-in cards, so on a compact screen the first paint ended at *Start empty workout* and the
  day's line and *Record today's measurements* were only reachable by scrolling — reported as the row going
  missing when no program is followed. With nothing scheduled the day's block now leads the list; with a
  workout it keeps its place at the foot, under the card. The row also stops disappearing once today's entry
  exists: it reads *Today's measurements* and opens **that entry** for editing, because a blank daily form's
  save replaces the day through `findOnDay` — so the old rule (hide the prompt) was the safe one while a blank
  form was all it could open.

- **The program fixture is generated, and two sentences tell the truth** (N169). The program document the
  page writes is now produced by `docs/fixtures/generate.js` beside the sheet it came from, and the page's
  own test parses that sheet and rebuilds the document, so the committed fixture cannot quietly become
  something the writer no longer produces (`ProgramDocumentFixtureTest` still decodes the same file with the
  app's codec). The progress summary stops blaming "more than twelve reps" for a lift with no added weight,
  and the settings chips wrap instead of clipping *Apply at Done* at a large font.

- **A plan add that could not fill its targets says so** (N168). Adding a movement to a plan appends it and
  then copies the last session's working sets as its targets. The append is the action that matters, so a
  storage failure while copying used to be dropped: the movement arrived with no targets and nothing said
  why. It is reported now, the picker does not claim a clean add, and a second tap is answered as the add
  that already happened instead of adding the movement twice. Under the pending set, the why-line also names
  a rung's **own** previous set — where its reps came from — rather than the session's last set, which for a
  drop is a different row. The rest of the review's cleanups are internal: two misplaced doc comments, a
  dead test tag, one home for the quick-entry limit, and two pass-through composables removed.

- **The progress chart reads only the rows the app reads** (B179). A soft-deleted set, exercise or session,
  and a session still in progress, ride in an export but are absent from the app's trend query; the page
  counted them, so a deleted 999 kg typo could become the chart's best session and a lift whose only sets
  were deleted still appeared in the picker. The series, the picker and *In your export* now apply the app's
  own filters — `deletedAt` on the session, the exercise and the set, and a finished session — so a removed
  exercise no longer shows its sets under a live session. The fixture cannot catch this (the app's own
  function never sees a deleted row), so the rule is pinned by a case on the test page.

- **A year's import no longer freezes the tab** (B176). The Sessions panel built every set's controls when the
  rows loaded: a real 3,750-row file became about 127,000 DOM nodes in one synchronous render, and the tab
  stopped responding while it laid them out — which reads as a crash. A session's body is now built the first
  time it is opened and kept; the summary line stays, because it carries the badge and the problem filter, and
  focusing a problem builds its session first. The same load is **1,984 nodes**, and opening a session adds
  only that session's controls. The library status also says when an export carries sessions for the Progress
  chart, so a loaded export that leaves the Sessions panel empty says why instead of looking broken.

- **The exercise chooser is alphabetical and searchable** (N159). The mapping's *Linked to* was a select of
  every library row in seed order, so finding *Barbell Bench Press* meant scrolling past a hundred families and
  there was no way to type. It is a searchable field over the sorted library now: typing filters, *Create new
  exercise* keeps the sheet's own name, a name the library does not have is refused rather than invented, and
  the source-name suggestions are sorted the same way. The chooser still never offers a family head.

- **The import helper knows the app's seeded library without an export** (N159). A name could only be linked
  to a permanent slug once an app export was loaded; without one every name became a new exercise, so an
  import of the app's own movements split their history from the seeded rows the app matches them to. The page
  now ships `seed-catalog.js`, generated from `SeedExercises.kt` and kept equal by a JVM test, so *Barbell Bench
  Press* resolves to `barbell-bench-press` before any export is loaded. An export still adds the lifter's own
  exercises and the dry-run comparison, and a family head is still never linked.

- **The import helper never links a family head as an exercise** (N159). A schema-5 export carries
  `RowKind.CATEGORY` heads beside the movements and is ordered by name, so five names exist twice —
  *Deadlift*, *Hip Thrust*, *Lat Pulldown*, *Lateral Raise*, *Overhead Press*. The mapping looked a name
  up among every row, so "Lat Pulldown" could resolve to `lat-pulldown-family`: the note read
  "Library: Lat Pulldown" and the generated file attached the sets to a head that the app's own
  `RowKind.isLoggable` says a logged set may not name — silently, and differently depending on the
  export's row order. The head is now excluded from the library, from the mapping's list and from a
  deleted-name match; a name that matches only a head is unmatched, like a name the library lacks. An
  export written before that field carries no `rowKind` and still links every row. The mapping also says
  in the row itself when no export is loaded, which is the only state that makes every name read "new".

## [2.10] — 2026-10-18 (versionCode 30)

History that predates the app can be brought in: a zero-dependency page in [`docs/`](docs/), published at
[997volt.github.io/workout](https://997volt.github.io/workout/), turns old spreadsheets into a backup the app
imports — and the app's own decoder is now tested against the file it writes. The deload creator lists each
exercise once with a day chip, plans the coming week rather than the one that is ending, and a deload day can
keep the same exercise twice. The helper merges several sheets, no longer collides its ids, accepts a date-only
row, a stated UTC offset and an epoch-millisecond date, and can be built as one file. **No schema or backup
change**: 2.10 is additive on 2.9's store, and the backup stays at `schemaVersion` 5.

### Added

- **A browser helper turns old spreadsheets into an importable backup** (N159). History that predates the app
  had no way in: the app reads only its own formats, and a hand-written backup either fails as one transaction
  with no row named or lands checked by nothing and cannot be corrected, because the merge never overwrites.
  The helper in [`docs/`](docs/) is a zero-dependency page — no build, no request, no stored training data, and
  it opens from disk as well as from Pages — that reads a flat list of rows from a model's transcription or a
  CSV, groups it by the notebook's own rules (one date per day, one row per set, blanks carrying down), converts
  each weight to whole grams and each RPE to half-points, maps exercise names to the library with a per-exercise
  kg/lb choice, marks warm-ups per row, and writes the four collections a backup requires at `schemaVersion: 5`.
  Dates are read day-month with an ambiguity guard that counts the ones a month-day reading would also accept, so
  a `05-03` sheet cannot silently swap months. Deterministic `imp_` ids make a re-import of the same file add
  nothing, so a file whose first import failed can be fixed and loaded again; a row that already landed is
  replaced by deleting it in the app, raising the revision and importing, because the merge never overwrites.

- **The helper's output is decoded by the app's own reader** (N160). The tool and the app speak one format and
  nothing crossed the seam: the helper asserted against its own expectations, so a `schemaVersion` bump or a
  field whose meaning moved would break the import with the app's generic "that does not look like a backup
  file" and no build would fail. A synthetic backup produced by the tool itself now sits beside it
  (`node docs/fixtures/generate.js`) and a JVM test decodes it through `BackupCodec`, asserting the four
  collections and nothing else, finished sessions with their zone, whole grams and half-points, contiguous
  positions and set indices, resolving references, and `imp_` ids that do not repeat. Regenerating the fixture
  is now a deliberate format change rather than a silent one.

### Fixed

- **The import helper loads several sheets and merges them, and its ids no longer collide** (N161, B173). A
  drop of several files read only the first and a second load replaced the rows, so a year of monthly sheets
  had no way in. Each load now appends a list with its own revision, a list boundary is a session boundary, a
  date two lists trained can be combined into one workout, and the same rows twice are refused rather than
  doubled. Ids now name the date as well as the row: two lists loaded under one revision both start at row 1,
  and without it the app's merge silently skipped the second list's sessions.

- **A date-only row or an exercise heading no longer blocks the export** (N162). The rule that every row is a
  set turned ordinary notebook furniture into an incomplete set with three errors. A row with neither reps
  nor weight now carries its date, exercise and note and adds no set; a session left with no sets is warned
  about and left out of the file rather than refusing it.

- **The helper's UTC offset can be stated or corrected** (N163). It was derived from the browser and could
  not be changed, so a sheet trained abroad could put an evening session on the wrong day. A stated
  `zoneOffsetMinutes` is honoured, the session header carries a control for it, and clearing the control goes
  back to the browser's zone.

- **The helper writes sessions in date order and reads an epoch-millisecond date** (B174, B175). Sessions
  were written in row order, so a sheet whose dates ran out of order produced a file in that order; the
  export now sorts by start, ties by source. A numeric date inside the plausible millisecond range is read as
  that day rather than refused.

- **The helper can be built as one file** (N164). `node docs/build-single-file.js` writes
  `dist/workout-import.html` with the stylesheet and script inlined, and refuses to write a page that still
  names either. The page's CSP now names `file:` in its script and style sources, so `docs/index.html` opened
  from disk is not refused by a browser that treats a `file://` document's origin as opaque; over HTTPS that
  source matches nothing.

### Changed

- **The deload creator lists every exercise once, with a day chip on each row** (N156). The pool was repeated
  inside every day card, so a four-day program drew the same long list four times and an exercise had to be
  scrolled past once per card. The days are headings now — *Deload A/B* and the weekday — and the whole program's
  pool is one list below them, each row carrying a chip for every deload day it can be kept on. The chips write
  the same per-day selection the cards did, so the plan a week is built from is unchanged; a row no day keeps
  says so rather than looking selected.

- **The creator plans the next deload week, never the one that is ending** (N158). On Sunday, building a deload
  marked the week that had already passed: its days were in the past, home had nothing for the coming week, and
  the next Monday began a fresh full week. The creator now always targets the coming Monday, so every deload day
  is in the future. The repository accepts the current or next week — the current for older plans and the
  adherence's own mark — and refuses anything further, which narrows P3.10's no-forward-view rule to one week.
  The current week keeps its full workouts and ratio; home and the adherence calendar read the plan when the
  week starts. The future week's mark is not on the adherence chip until that week begins, so a plan created by
  mistake is rebuilt (the existing-plan note) or unmarked from the chip once the week starts.

- **The same exercise can be kept more than once in a deload** (N157). The pool kept one row per movement (and,
  after 2.9, the heaviest source's plan), so a program that scheduled a lift on two days could only keep it once.
  The pool now keeps one row per **source instance**, each with its own day's plan and source label; a deload day
  can keep the same library exercise more than once, and the template is written with both rows. The last-plan
  reuse maps a library id to the first current instance, so a duplicated old choice collapses to one on reuse.

## [2.9] — 2026-10-18 (versionCode 29)

The deload builder gets its program-wide pool and a reuse of the last plan's choices, and a target weight gets a
date estimate. A built week now reads as *Deload A, Deload B, …*; a movement scheduled on two source days comes
from the heaviest source, and *Use last deload* restores the exercises, day count and weekdays. The review of the
previous batch's fixes is in this build too — the last-plan read is program-scoped, a reached target says so once,
and the tolerance behind it has one definition. **No schema or backup change**: 2.9 is additive on 2.8's store, and
the backup carries no new fields.

### Added

- **A deload day draws from the whole program** (N152). The creator's day count was a prefix of the program's
  days, so a two-day deload could only use the first two source days: an exercise the program scheduled on the
  third or fourth day was unreachable, which is the opposite of what a deload week is for. Every deload day card
  now offers the **program-wide pool** — one row per movement, with the source days it appears on as context —
  and the lifter ticks what each deload day keeps. A movement scheduled on two source days is one pool row (the
  source day with the heaviest stated working load supplies the plan; a tie keeps the program's order), and a
  two-day deload can keep a lift the program only schedules on the third day. Each day's template is written from the chosen exercises and named **Deload A, Deload B, …** in day
  order, so the built week reads as a week rather than as copies of the program's templates; the source day is
  the pool row's context. The weekday still starts from the source day at that position, and the template editor
  renames the workout.

- **The creator remembers the last deload's choices** (N150). Building a deload started from scratch every
  block, so the exercises to keep and the number of days were chosen again even though the previous plan was
  already stored. *Use last deload* now preselects the exercises the last plan kept — matched by library id, so
  a movement the program still has is selected and one it dropped is not — and takes its day count and weekdays;
  every set, rep and load is then re-derived from the current plan and history, not copied from the old templates.
  The load share is not stored with a plan, so reuse leaves it at the creator's current choice rather than guessing
  it. Because the plan stores its templates, the exercises are read from their current contents (N16).

- **A target weight says around when it will be reached** (N151). The statistics row stated the rate and the band
  it is judged against, but a lifter with a target still had to divide to know what that meant in time. For a
  `Target` goal it now says *Around <date>* — derived from the current weight and the stated rate, never the other
  way round — and says *Target reached* once the weight is within the goal history's own tolerance. A
  direction-only or maintenance goal shows no date, because it has no level to reach.

### Fixed

- **A reached target says so once** (B172). The energy row printed *0 kg per week* under a goal whose rate is
  zero because the target is met, beside *Target reached*. The target text now says *Target reached* and the
  date line is reserved for an actual date. The tolerance behind that line and the goal history now has one
  definition (B171), so the two cannot disagree.

- **Use last deload no longer reads another program's week** (B168). The last-plan read found its mark for one
  program but then read every program's days for that week, so with two active programs that each built a deload
  in the same week the reuse could preselect the other program's exercises and day count. The read is now
  program-scoped, with a two-program test.

## [2.8] — 2026-10-18 (versionCode 28)

The deload week can now be built, and the screens around it say what is actually planned or done: adherence
reads a built week's own days, the start question leaves it alone, and the plan's workouts are one tap from the
success card. History names an activity on both its row and its page, and search follows what a row shows.
**One behavior moves on purpose**: a weight goal's rate now depends on its direction — loss asks 0.65% of
bodyweight a week, gain 0.35% — so an existing goal reads a different target after the update. The store is
schema 7; the backup stays at 5, so a 2.7 build reading a 2.8 file keeps the deload mark and workouts but not
the plan.

### Added

- **A program can build its deload week, and the numbers come filled in** (N147). A deload week used to be a mark
  and nothing else: its slots still prescribed the full workouts, so a lifter either retyped every set to back off
  or trained the plan and called it a deload. The program editor's *Deload week* now opens a creator: choose how
  many days, tick the exercises each keeps, and set one share of the load. The rule is stated on screen and applied
  once — the plan's stand-alone working sets up to three, its highest rep target floored at five, its heaviest
  stated load kept at 60% (or the last session's where the plan states none), rounded to the exercise's step. What
  it writes is an ordinary template per day and a plan for the current week, so the week's days are what home
  and adherence's calendar offer; the missed-day question does not ask about a built week (B163). A deload
  session names no slot, so the run pauses and the week stays exempt from the ratio. Unmarking a built week asks
  first (B162): the workouts stay in the library and the plan does not come back, and the plan rides in the
  backup. The store gains **schema 7** (`program_deload_days`), while the **backup's `schemaVersion` stays at
  5** — the collection is a defaulted addition, so a build older than this one reading a newer file keeps the
  mark and the workouts but not the week's plan (B165). **This is the narrow amendment to P3.10**: the app still
  never scales a week on its own, and the numbers are a draft the lifter can edit afterwards.

- **A built week is reachable from its success card, and says it is built** (N148). The card used to offer only
  *Done*, so the workouts the creator wrote were one navigation away with nothing pointing at them. It now offers
  *View workouts* beside *Done*, and the adherence deload chip labels a week that has a plan *built*, so the plan
  is visible where the week is read. A rebuild's note already says the older workouts stay in the library.

### Changed

- **Loss and gain have separate rate bands and stated targets** (N149). N135 gave both directions one 0.25–0.5%
  band and took its centre, so a Lose goal and a Gain goal at the same bodyweight asked for the same magnitude.
  Gaining is now held to **0.25–0.5%** of bodyweight a week and asks **0.35%**; losing to **0.5–0.8%** and asks
  **0.65%**, because gaining lean mass is the slower direction. The target is still derived from the goal and the
  weight the lifter is now, clamped into that direction's band, and the band is shown under the rate so the
  number has the range it is judged against. A target goal takes the rate for the side it is on.

### Fixed

- **A built deload week is read from its own days** (B161). The adherence calendar and the day dialog read the
  program's slots, so a week the creator built showed the slot's full template — or nothing, if the day was
  moved — while home and the start prompt offered the deload workout. They now read the plan's days: a plan's own
  days are the week's schedule, its slots are not scheduled that week, and the day dialog lists its days with the
  deload template. A marked week with no plan keeps the slots, exempt, as before.
- **Unmarking a built week asks first** (B162). The chip was one tap from making the week's plan unreachable. A
  marked week with a plan now labels itself *built* and asks, saying the workouts stay in the library but the plan
  will not be used; a mark with no plan still toggles off immediately.
- **The start question no longer asks about a built deload week** (B163). A deload day has no slot a skip could
  be recorded against, so *Continue* could not settle it and the prompt repeated every start that week. A program
  with a built plan is now left out of the question; a marked week with no plan keeps P3.10's shape.
- **The creator's failed read says so** (B164). A storage failure escaped the screen's error channel and fell
  through to *this program has no workouts*. The read now returns through `dataResultOf`, and a distinct state
  says the plan could not be read, with no create bar.
- **The energy statement reads the whole weight series.** It was fed the chart's range, so on the default
  last-seven-days it could not meet N98's eight-readings-over-21-days floor and always said *Not enough weigh-ins
  yet*, even when the weigh-in verdict — which reads the whole series — could speak. The verdict now uses the
  unwindowed series; the chart keeps its range. (No roadmap row: found while implementing N149.)
- **An activity is named on its history page** (B167). The list row named the activity and still appended *0 sets*;
  opening it showed *0 sets · 0 kg* and never said what the session was. The row now drops the set count for an
  activity, the detail's title is its name, and its totals are the duration and the activity rather than two
  zeroes. History search follows the same rule: it no longer matches the set count an activity's row does not
  show.

## [2.7] — 2026-10-18 (versionCode 27)

The pre-release review's batch: one defect visible on every fresh install, thirteen more found by reading, four
interface changes and one feature. **One number moves on purpose** — the Statistics *Records* tile stops counting
drop and cluster rungs as personal bests (B155), so it may read lower than it did on 2.6. That is the fix, not
lost data: a rung is work, but the app never announced it as a record.

### Added

- **History can be searched and filtered** (N146). A log long enough to need finding things in had only its month
  headings, so the question "where is that workout" had no answer but scrolling. A query matches what a row
  actually says — the month it is grouped under, the plan it was started from, the activity it was, its set
  count — which is deliberately not an exercise name: the list is a projection of totals, and reaching an exercise
  would mean loading every set of every session, the cost that projection exists to avoid. Two chips ask the other
  question a log gets asked, *show me the days I did something other than lift*, and the row now names its
  activity where it has one rather than reading as a workout somebody forgot to log. A query that matches nothing
  says so and offers the way back, which is the one distinction a filter has to get right.

### Changed

- **The document transfer has a device test of its own** (B158, B159). `DocumentIo` is the one piece of the
  transfer that is *about* Android — `openOutputStream`, `openInputStream`, and the dispatch they run on — and the
  coroutine that had to leave the composition's scope is only safe if that half behaves, so it is exercised
  against a real `ContentResolver`: a write and read round-trip, and the two failures a picker can hand over
  answering `false` and `null` rather than throwing. It needs a `content://` URI the test can write through, which
  is why the manifest gained a `FileProvider` scoped to one cache directory and nothing else.

- **The docs the batch left describing an older code** (B148–B160, N143–N146). A review of the review asked
  whether the documentation still described what shipped, and it did not, in three ways. **Four settled rules
  had been amended in the code and not in [DECISIONS.md](DECISIONS.md)**: N41's prompt-free discard is now the
  empty workout's alone, N79's rung rule gained removal and the cluster's whole load, N24's round gained the
  unequal-totals case, and B72's position rule gained the half the session taught it. **A measurement quoted in
  four places had drifted** — `detekt-formatting` was said to report 642 findings across 155 files and reports
  435 across 121 — so the figure was re-measured, the command that produces it written beside it, and the other
  copies made to point at that one rather than repeat a count nobody re-checks. **Two facts were simply wrong**:
  the APK sizes in [README.md](README.md) were ~13 MB and ~1.5 MB against ~22 MB and ~2.4 MB of the build in
  front of the reviewer, and a 1.x entry named a file B159 deleted. The `ImportOrdering` ratio the N129 rule
  rests on held flat at about nine files in ten, so the rule stands and only its numbers moved — which is why
  this is a correction rather than a decision.

- **A seeded movement is no longer dropped by an id it shares with a family** (B148). The seed declared *Back
  Extension* twice — once as the machine movement, once as the family over it — and a category and a movement are
  two **key spaces over one table**: the families are seeded first, and `INSERT OR IGNORE` (the rule that keeps a
  lifter's own rename) then discarded the movement without a word, so a lift the app ships could never be offered
  by a picker or named by a logged set. The `parentOf` entry was the tell: it pointed at itself, which no write
  path can produce. The family takes the `-family` suffix every other colliding name already carries, and
  `SeedExercisesTest` now asserts the two lists never intersect rather than trusting that the naming kept up. **A
  database already holding the collision is repaired on open**, inside the seeder's transaction and before the
  top-up, so the id is freed and the movement can land — guarded on the row still being a category, still called
  *Back Extension* and still its own parent, because a lifter's rename is exactly what `INSERT OR IGNORE` exists
  to protect.

### Fixed

- **An import no longer brings back rows a save had replaced** (B149). The restore was keyed on the id alone, and
  a soft-hidden child row has two possible causes the ids cannot tell apart: the parent was **deleted**, or its
  list was **replaced** by a save — a measurement's sore rows and an exercise's joint rows are both rewritten
  wholesale. So editing a day's soreness from quads 8 to quads 6 and then importing an older file gave the entry
  two live rows for one muscle, "quads 8" and "quads 6" both true, which is the state the editor's own rule
  forbids; the same measurement-level collision could leave one day and kind with two live entries, the state the
  undo path refuses outright. Correlating timestamps, the way the undo does, is not available to an import — it
  acts on a file written before either write — so the question asked is the one a delete and a replacement answer
  differently: **is the parent coming back too**. A child whose parent stays live is left hidden.

- **A restored session can no longer be the workout in progress** (B150). `finishedAt IS NULL` is the app's whole
  definition of the session you are in, and the mapper copied the file's value through — so importing a backup
  taken mid-workout (or one left open by a crash, which the app deliberately preserves) resumed a session that was
  never this device's, and importing *during* a workout wrote a second open row whose older half could never be
  reached: history reads only finished sessions and the active read returns the newest. A restored session is now
  closed at its own start, which keeps its day and its record without claiming anybody is in it.

- **A save reads the day it is writing to inside its own transaction** (B151). The check for the day's existing
  entry ran before `withTransaction`, so a save committing in that window was invisible to it: both found nothing,
  both inserted, and one day and kind held two live entries — which every read then lists twice and every chart
  then plots twice. B135 fixed the mirror half of this, the undo's guard; this is the save's own read, moved in
  beside the write and its timestamp.

- **Removing a run's first rung takes the run with it** (B152). A run's drop value is stored on its **opening**
  rung and every later rung inherits it, so deleting that row stranded the survivors with no value to derive from:
  the plan read "no load to drop", and re-typing the number did not put the ladder back — the survivor was now
  rung 1, so the plan was quietly re-valued rather than merely blanked. The anchor's removal already took the
  whole run for exactly this reason, and the shape left behind is the one the plan's own rules refuse to author.

- **A cluster rung under an assisted anchor keeps the machine's help** (B153). A cluster repeats its anchor's load
  *exactly*, assistance included, but only the weight half was handed on — so an anchor giving 20 kg of help
  derived a weight of 0, which the prefill reads as *the plan said 0 kg*: the help was discarded, the fields
  showed bodyweight, and the set was logged as one. The whole derived load travels now, in the session's prefill
  and on the plan's own row, which said "0 kg" for the same reason.

- **A superset whose members plan different set counts keeps resting** (B154). The round waited for every other
  member to reach the count just logged, and the shorter member can never reach it: from its last set onward the
  rest timer silently stopped being armed for the longer one — no countdown, no ±15 s, no end-of-rest cue — and
  nothing on screen said a round was holding it back. A member whose plan is used up is caught up, which is what
  the expression's existing "or has been marked done" was reaching for.

- **The Records count reads the app's own rule for what a record is** (B155). The query excluded `'WARMUP'` alone
  while the rule is `SetType.recordsEffort`, which a drop or cluster rung also fails — so a rung was counted as a
  record the lifter was never told about, and, on the other side of the comparison, an earlier rung's weight
  blocked a later honest set from a count that *was* announced. The roles are passed in from the one rule now, so
  the SQL cannot drift from the enum again.

- **A plan that names a removed movement no longer seeds a phantom row** (B156). The read that starts a workout
  from a template was the one query of its family without the library's soft-delete join, so a hidden movement
  arrived as a session row that the session's own reads then filtered out: invisible, unremovable, and counted by
  history's exercise total.

- **A rung's last-time reps are matched by place, not by a stored index** (B157). `setIndex` is not a position: a
  deleted set keeps its index and re-logging appends at the end, so the app's own Undo leaves a hole — log three
  sets, delete the third, undo it, and the live indices are 0, 1, 3 while the reading side counts 0, 1, 2. The
  comparison found no row, and the rung prefilled from the *anchor's* just-done reps instead of its own last
  performance, which is the one thing that prefill exists to provide.

- **The transfers' file IO, the export's encoding and the settings' commits run off the main thread** (B158).
  `suspend` alone bought nothing: `openInputStream`, `readBytes`, `write` and a synchronous `commit = true` all
  execute inline on the caller, and every caller was a UI scope — so choosing a backup on a cloud provider blocked
  the UI thread on a network round trip, a long history janked the export, and every Settings toggle and Statistics
  range chip was a disk write on the tap path. Each now takes `Dispatchers.IO`, in one place per kind of work.

- **A file transfer survives the screen that started it** (B159). The work ran in the composition's own scope,
  which a configuration change disposes — and the Storage Access Framework had already created the document by
  then, so a rotation mid-export left a 0-byte `workout-backup.json` behind and reported nothing at all; the lifter
  found out when a later import called the file unreadable. The transfer and its report live in the ViewModel now,
  for the backup and for the program document alike.

- **The delete undo is only offered while the exercise can still take the set** (B160). A finished exercise keeps
  its row in the list, so the offer outlived the write it promised: tapping *Done* and then the still-visible
  *Undo* answered "That workout is no longer open" in front of a workout that plainly was, and the set stayed
  lost. The offer asks the write's own question, and disappears with the exercise's ability to accept it.

- **Discarding a session whose only record is an activity asks first** (N143). An activity holds no sets by
  construction, so "nothing in it" was true for exactly the session a lifter had deliberately recorded — and one
  unconfirmed tap deleted the day's record with no undo, while the same action on a workout with a single logged
  set asked and named what went. The prompt-free branch is for a session that is empty *because nothing was done*,
  and an activity never is.

- **The workout review states the unit, and reads an assisted plan's help** (N144). A prescribed weight with no
  rep target was printed bare — the one number in the dialog with nothing saying whether it was kilograms or
  pounds — and an assisted plan, which is stored as 0 kg of added weight plus the help it names, was reported as
  "at 0 kg": a weight the plan never prescribed, with the assistance appearing nowhere. The comparison carries the
  help on both sides now, so `-20` reads as 20 kg of assistance.

- **A refused rung says what is wrong rather than what a rung is** (N145). One sentence covered three different
  faults — no set above at all, a warm-up above, another kind of rung above — so a lifter was told the rule
  instead of the fault, and the edit dialog offered Drop and Cluster at every position while the log path gated
  them. Both paths ask the same question and each fault has its own sentence; the editor's picker no longer offers
  what the write boundary would refuse after it had already closed.

## [2.6] — 2026-10-16 (versionCode 26)

*Corrected in place after release, and only in figures: two entries below quoted a formatting measurement
that had drifted. Nothing was added to this release's content, and the work that landed after the tag is the
2.7 entry above.*

### Added

- **An empty workout can be an activity instead** (N141). Under *Add exercise*, while the session holds
  nothing, a second row offers **Add activity** — cycling, running, swimming, rock climbing, volleyball — and the
  session then takes **no sets at all**: what is left is the readiness note and the comment asked for at the end,
  and *Not an activity* makes it a workout again if the wrong one was picked. A short **closed** list, stored by
  name like every enum, and one nullable column on the session (**schema 6**) rather than a record type of its
  own: the day, the history, the calendar and the export all already read a session, and a second kind of record
  would be a second path through every one of them. An activity **finishes with no sets**, which is the one rule
  that had to change, and it contributes no volume, no reps and no records — those read the sets it does not
  have.

- **An activity is its own colour in the adherence calendar** (N142). A trained day was drawn in one colour, so
  a day whose only work was a swim read as a workout that happened to log nothing. It has its own container now,
  and a day with **a workout and** an activity is still a workout day: the calendar's question is whether the
  day was trained.

### Changed

- **The row that records the day belongs to today** (N138). It was the last of home's *ways in* — the
  destinations — so "record what I did today" sat among the places to go. It is now **inside today's block,
  under the day's own workout** (and after the rest-day line when nothing is planned, because that is exactly
  when a weigh-in *is* the day's entry). Only its position changed: it is still drawn whenever today has no
  daily entry, a workout running or not (N114).

- **Home reads as two colour zones** (N139). Every row carried an accent of its own — Programs teal, Templates
  sky, Measurements coral, start-empty indigo, the day's record amber — which said "five different things" where
  the screen means two: **the ways in**, and **today**. Everything above today now takes the green Programs had,
  and everything inside today the blue Templates had, so the accent says which zone a row is in rather than
  which feature it belongs to.

- **A new workout no longer opens into the readiness note** (N140). The prompt was the shape N4 chose when the
  field was otherwise unfindable, and it had become the wrong bargain: opening a session is not the moment to
  write down how the body is — the lifter has not done anything yet — and what the prompt was really doing was
  making the field findable, which the row in the header already does. **The row is the only way in**, and
  pressing it is the only thing that opens the dialog. The prompt's state, its "skip", its two strings and the
  `isPrompt` flag the dialog carried all went with it.

### Added

- **A plan's target is drawn on a lift's chart** (N39, the half that was parked). The plan already states a
  target per set — a weight, an assistance, a rep range, an effort — and the chart for a lift now shows what the
  plan asks for beside what was actually done. **Every live plan that names the lift draws its own line**, each
  named under the chart, rather than one plan winning: a lift can be in two plans at once and more than one
  program may be active, and picking one silently is the quiet error this exists to avoid. The lifter's own goal
  stays dotted where a plan's target is dashed, and the text under the chart says which plan is which. Only the
  **level-shaped** targets draw — a weight, an estimated one-rep max from that weight and its rep target, an
  assistance, an effort — because a rep range has no level and a total is a sum of a prescription rather than a
  line in it. A kept version is history and a deleted set was taken out of the plan, so neither draws.

- **A weigh-in says whether the rate is on target** (N136). Asked for in use: the sentence already existed —
  *"about 200 kcal a day less than you are eating now"* — but it lived **under a chart in Statistics**, so a
  lifter had to go and look for it. Saving a daily weight now answers it where the weighing happens, as a
  **line after the save**: it states, it is dismissible and it does not pursue (P4.7). **It keeps N98's floor**
  — 8 recorded readings across 21 days — because an unsolicited prompt that fires on water weight is worse than
  no prompt, and it stays **relative** (how far intake must move), never a number to eat. **Nothing is said at
  all without a goal**: the direction is the lifter's, and a line telling somebody with no such goal to lose
  weight is a diagnosis rather than arithmetic against what they asked for. A **maintenance** goal is judged
  against its corridor, as the chart's section judges it (N135); a tape day and a soreness-only morning are not
  weigh-ins and answer nothing.

- **A measurement opens its own details** (no roadmap id — a request in use, which the roadmap does not
  cover). The list's supporting line is a summary: `82.4 kg · 18.3%` does not say which number is which,
  and the tape sites and the sore list read there as bare numbers and bare words. Tapping a row now opens
  the record read in full, each number under the name of what it measured, and **only what the record
  states is drawn** — an absent weight or an unmeasured site is not a zero, so it is not a row either
  (N113). It is a look and nothing else: correcting a reading is a different question (N132), so the one
  control is *Close*.

- **A template can keep a version, and the versions are attached to it** (N134). Asked for in use:
  *sometimes I want to update a template but keep its previous version for future reference*. A version is a
  **template row of its own** — its exercises and sets in the tables they already live in, flagged by a
  nullable `versionOf` naming the template it was kept from — so nothing new was created for the plan's tree:
  the read paths, the copy machinery and the soft-delete convention all apply unchanged, and the templates
  list gained one more predicate of the kind it already has, `versionOf IS NULL` beside `deletedAt IS NULL`,
  so a version never appears as a template. That also dissolves the hide button a *duplicate* would have
  needed: the kept copies live inside the template they belong to rather than loose in the list. *Keep a
  version* sits in the editor's overflow, because it is pressed at the moment the lifter decides to change
  something, and the versions open as a **read-only plan** — never edited, never restored, which is the
  request: future reference, not a way back. **A version stores its own name**, because the live template's
  name is read live by history and renaming anything to mark a version would rewrite how past sessions read.
  **The export carries versions and the `versionOf` link with them**, so an import re-attaches them instead
  of flattening them into ordinary templates. Rejected: three revision tables and a migration, on the reading
  that a version is a different kind of thing from a template — it is a plan with exercises and sets, and one
  nullable column is the whole of the difference.

### Changed

- **The measurements edit mode corrects a record as well as deleting it** (no roadmap id — a request in use,
  later than N132, whose revised rule is in [DECISIONS.md](DECISIONS.md)). N132 had put "correcting a
  measurement's numbers" aside as a different question; asked later, it needed no second mode. Inside the mode
  a row now opens its form **on the record it names**, holding what that entry already states, so a number that
  was left out can be added and one that is wrong can be changed. The draft keeps the entry's **own day and
  id**, so the write corrects that row rather than landing as a second entry on the day — the collision the
  undo's guard has to refuse cannot arise here. The default view is unchanged: its tap still opens the
  read-only details, which stays the one place a record is read where nothing can write.

- **A goal's past is kept, and each period says whether it was met** (N137). Asked for in use: when a goal
  changes, the old one should be recoverable, so a later reading can say what the goal was in a period and
  whether it was reached. Goals were a newline-joined `metricId=value` string in preferences, rewritten on every
  change, so changing one silently re-judged the whole past: the chart's line was the current target drawn
  across a range it never applied to, and "did I hit the cut I set in March" had no answer because March's goal
  was gone. A goal is now a row per **period** in a new `metric_goals` table (schema 5), the newest row per
  metric being the period that is open: a change opens one rather than overwriting one, clearing opens a period
  of *no goal* rather than deleting a row, and pressing *Set* twice with the same number opens nothing. **The
  goal already on the device moves in with an unknown start**, because stamping it with the upgrade's own moment
  would be inventing history. Two seams close with it: the kind and the value are one row, so a failure can no
  longer leave a value with no kind; and the goals' **missing-only** import rule goes, because a period is a row
  with an id. The export carries the periods — and still carries `goals`/`goalKinds` for a build that predates
  them, filled from the period that is open so the two cannot disagree.

  **The verdicts** are read from the same rows: a **target** is met when something in the period reached it
  within the corridor's tolerance — a fact about a reading, so it needs no trend; a **rate** is met when the
  fitted trend sat in N135's band *and* on the side the lifter asked for, which needs a fit and therefore
  N98's floor, because a verdict about the past must not be a rate from three weigh-ins either; **maintenance**
  is met when the weight stayed in its corridor, judged on the fitted line so a day of water is absorbed rather
  than counted. A period nobody weighed in during is **unjudged**, not a miss, and a period of no goal is
  neither. The goal row offers the history when there is a past to read, newest first.

  **The chart draws one line per period**, each across the stretch it applied to, so the chart never draws a
  target over a range it did not apply to. A direction-only goal draws no line at all, because it states no
  weight and inventing one from the band would be the end weight N135 refuses to manufacture.

- **A weight goal sets the rate, instead of the rate being typed** (N135). Asked for in use: the lifter set a
  target *and* a rate, two answers to one question that could disagree. The weight goal now states a **kind** —
  a target weight, a direction with no target, or a weight to hold — and the **rate is derived** from that kind
  and the weight the lifter is now, as the band's centre: **0.25–0.5% of bodyweight a week**, the reference's
  shape rather than its numbers, so the same rule reads correctly for a 50 kg lifter and a 120 kg one. A
  **direction-only** goal invents no end weight; **maintenance is a corridor** of ±1 kg rather than a rate of
  zero, which the first glass of water would break; a rate is never derived from a deadline. The **typed rate
  went**: the statistics row shows the derived rate and has nothing to press, and a goal with no weigh-in behind
  it says so rather than inventing a number. The **kind travels in the export** beside the goals, so a restore
  re-attaches it — and a value written before kinds existed reads as a target, which is what a level goal was.
  Rejected: keeping the two numbers side by side, which is the disagreement being fixed.

### Fixed

- **An activity can be finished with no sets** (B138). N141's one changed rule was not implemented where it is
  enforced: the top bar's *Finish* was gated on a logged set, and an activity holds **no** exercises at all, so
  it stayed disabled for the session's whole life. The only way out was *Discard workout*, which **deletes** the
  session — so `finishedAt` was never written, and because the adherence read filters on it the activity reached
  neither the history nor the calendar. N141 and N142 were unreachable end to end. The gate is now "an activity,
  or any logged set", and two Robolectric tests pin both halves: an activity finishes, an empty workout still
  cannot.

- **An activity is its own colour in the adherence calendar, and says so** (B139). `activity AS activity` had
  been added to the wrong query — the one whose row type has no such field — while the query that actually feeds
  the calendar was left without it, and its row type had gained the property. Room fills a nullable property a
  query does not return with `null` and only warns, so `isActivity` was always false, `activityDays` was always
  empty, and N142's colour was dead code. The column is on the read that feeds the calendar now, and an
  activity-only day is **announced** as an activity rather than as trained: a container colour says nothing to a
  screen reader.

- **A goal's pre-upgrade period leads, and the open one is read as open** (B140). `observePeriods` ordered
  `(startedAt IS NULL) ASC`, and SQLite reads that as 1 for the undated row — so the goal the one-time move
  writes, which is the **oldest** period, sorted last. Three consequences from one clause: the legacy target was
  drawn across the whole chart, its verdict read every reading in the series, and opening a period compared a new
  goal against the oldest row — so pressing *Set* twice with the same number opened a duplicate period, and a
  change back to a goal the lifter had already had read as no change at all, reporting success with nothing
  written. The ordering is fixed, and the open period is read by a `findOpen` query rather than assumed to be the
  last of a list.

- **A goal period is judged from the readings in it, not from the chart window** (B141). The history was computed
  from the series the range chips already clip, while the periods were all-time — so a verdict about the past
  moved with a display setting. It was not a corner: the default range is seven days, which holds at most seven
  daily weigh-ins, while a rate needs N98's floor of eight readings across 21 days, so every direction goal read
  "not enough weigh-ins" on the screen as it opened. The unwindowed series the ViewModel already reads now
  travels beside the windowed one.

- **A plan's target line reads the range's floor, and follows a chosen family** (B142, B143). Two ways the line
  N39 draws disagreed with the plan it is drawn from. It took a rep range's **top** where the session takes its
  floor — so a live 5–8 × 100 kg set with no progression was drawn at Epley(100 kg, 8) ≈ 126.5 kg instead of
  Epley(100 kg, 5) ≈ 116.5 kg — and it was queried for the single chosen lift, so choosing a category under RPE
  drew the family's readings with **no** plan line though live plans prescribe RPE for its movements. The
  fallback matches the session now, and the read follows the same family the series does.

- **The goal periods restore inside the import's transaction, and are counted** (B144). They still ran after it
  as the last trace of the seam the preferences store needed, and returned `Unit`, so their rows never reached
  the import summary. A file whose goal write failed left the sessions merged while the import reported failure —
  and a restore whose only new content was a goal said "everything in that file was already here" while it
  restored it.

- **The field-exhaustive round-trip guard covers the fields this line added** (B145). `BackupCodecRoundTripTest`
  exists because the hand-written codec has silently dropped a column three times, and its contract is a fixture
  with every field set to something distinctive. The batch added a session's `activity`, a template's
  `versionOf` and the whole `metric_goals` table, and extended none of it — the mappers were correct, but the
  guard that catches the next one was not. Dropping `activity` from the mapper now fails the suite, which was
  verified before the mutation was reverted.

- **Two migration tests assert something** (B146). `GoalMigrationTest` ended with
  `assertTrue("and the first move's timestamp is the one that stands", true)` — a literal, so the claim in its own
  message was never read, and `INSERT OR REPLACE` in place of `INSERT OR IGNORE` left the count at 1 and the test
  green. It asserts the row's `createdAt` now, which that mutation does fail. `ActivityMigrationTest` asserted a
  column index under a message about `startedAt`; it asserts the start the row was written with instead.

- **The prose the removals left behind, and a read-only imperative** (B147). The migration list said "three
  migrations" and named five; four declarations had lost their KDoc to the one inserted above them, one of them
  documenting `onSkipRest` as skipping a readiness prompt; and the readiness prose N140 removed the behaviour of
  still read as if the prompt existed. One was user-visible: the **read-only** version plan drew the editor's
  imperative "No planned sets yet. Add your first one." in a dialog whose only control is *Close*, where the other
  read-only plan preview uses the neutral "No planned sets".

## [2.5] — 2026-10-15 (versionCode 25)

### Changed

- **Today's plan is the only answer home gives** (N125). Home answered *where is the next workout* twice: a
  dated plan in the body, and a **next-up row per active program with nothing scheduled today** — the run it
  was up to, with its own *Start* and substitute (P3.9, N55, N124). The second is gone, because a day's
  workout is the question the screen exists for and a program's position on some other day is an answer
  nothing on it can act on. **That has a cost, named rather than discovered**: a program with no weekday, and
  every rest day of one that has them, is no longer reachable from home — the Programs screen is where a run
  is read now, and P3.9's home surface and the next-up half of N124 are superseded with it. Everything that
  only the rows called went with them: `nextUp`, `nextUpFor`, the `NextUp` model, the row and its label, its
  tags and strings, the program-runs read that fed it, and N85's whole unrecorded-substitute path — with no
  next-up row left, every *Substitute* belongs to a card, and a card **is** today's occurrence, so all of
  them are written. Rejected: keeping the rows behind a "show more" — the complaint is that the answer is the
  wrong one, and a disclosure hides it.

- **Home says when nothing is planned today, and which kind of nothing** (N126). An empty day drew **nothing
  at all**, so a rest day read as a screen that simply ended — survivable while a next-up row could be there
  instead, and not survivable once N125 removed them. The surface now says which empty it is, because the two
  are different facts: **no active program** (nothing is scheduled because nothing is being followed, so the
  line points at the way to plan one) and **an active program with no slot today** (*Rest day* — the answer
  on every day for a weekday-less program, never scheduled and never missed). Two quiet lines where a card
  would be, and no heading above them: an empty *Today · Monday* is the promise N115 removed, and the line
  names the day itself.

- **Today's planned workout can be read before it is started** (N127). A day's card offered *Start* and the
  substitute picker and nothing else, so the day's own workout was the one plan that could only be seen by
  beginning it. Its name is the field now, opening the same read-only
  [`PlannedWorkoutDialog`](app/src/main/java/com/example/androidapp/ui/home/PlannedWorkoutDialog.kt) a next-up
  row used to — with the program as a subtitle, read from the plan itself — so looking and starting stay two
  gestures (N55). **This is what kept that dialog**: the next-up rows were its only entry point, so N125
  without N127 would have deleted the preview rather than moved it.

- **Deleting a measurement is a mode, and it can be taken back** (N132, N133). Every row of the measurements
  list carried its own *Delete*, one tap from a screen opened to **read or add** — and a measurement is the
  one thing here that cannot be re-derived: a set can be logged again and a plan re-authored, but a weight on
  a morning is a fact about the body that no later write recovers. The default view is **scroll and add**
  again, and deletion lives behind one *Edit* toggle that becomes *Done* and takes the add button out of the
  way while it is on. **A delete now says what went and offers *Undo***, in the snackbar shape the workout and
  template screens already use (N7, B3, N90): the delete was always a soft one, so the way back is clearing
  `deletedAt` — on the entry and on the sore rows the same write hid — rather than re-inserting anything. The
  one case it refuses, because a measurement's identity is **its day and its kind** (N32, N113): if the day
  has been re-entered since, the undo would have to replace a newer reading the lifter wrote, so it says so
  instead. Rejected: a confirmation dialog on the per-row button — that is the guard for an act with no way
  back at all, and it would have left the delete on every row while interrupting every deliberate one.

### Added

- **The release's mechanical checks are one script** (N128). Badging, the signing certificate, the
  hash and — after upload — the published asset were all done by hand on every release, and the
  certificate is the one where a slip means every user has to uninstall and lose their history,
  because platform backup is off. `tools/verify-release.py` does all four. The first three need no
  token and no network, so the same script runs in CI on every change and from a checkout that has
  never seen a release; only the asset round-trip is opt-in, and it is the half that catches a wrong
  file on the right release, which nothing else here can see. **RELEASING.md step 3 names its three
  files instead of `git add -A`** (B128) — the 2.4 round put a scratch directory of HTTP dumps into a
  release commit, and only the commit's own `--stat` caught it.

- **The review compares the effort the plan asked for** (N131). A plan's target effort prefilled the
  fields and drove the progression, and the review — the one place a plan is held up against what was
  actually lifted — said nothing about it, so "you prescribed RPE 8 and logged 9.5" was invisible. The
  comparison now sits on the row: the prescribed line carries the target, the performed line the effort
  the working sets were actually taken to, and a verdict states the difference. **The reduction this
  needed is the decision**: the plan names *one* effort per exercise (N59) while the log holds one rating
  per *set* (N6), so the sets are averaged — the heaviest set's rating answers what the top set felt
  like, not whether the work landed where the plan put it, and a session with one on-target top set and
  three back-offs would read as matched. A mean is rounded to half a point, the resolution an RPE is
  logged in, so the row never prints a rating nobody gave. A plan that named no effort says nothing about
  it; one that named an effort and got no rating says so, because silence there would read as matched.

- **Joint pain is a series per joint, and per side** (N130). A rating already recorded **which joint,
  which side, how much** (N63), and what the trends screen read back was one number per workout — so
  "left knee 6, right knee 2" charted as a 4 that is about neither knee. The picker now offers a series
  per **site**, the joint and the side together, exactly as N118 offers one per muscle: *Left knee* and
  *Right knee* are two lines, and the record already carried both halves. **Both levels the ratings live
  at** get the dimension — a workout series read from a session's ratings for the lift chosen, and a
  per-lift one read from that lift's own points — because the average dropped the joint in both. Room for
  the second one was needed at the picker: two series can now share a label, so the list carries a heading
  per group and *Left knee* is no longer the same words in two places. A site a session did not name is a
  **gap, not a zero** — the rule the tape sites and the sore list already follow — so a knee
  nobody mentioned is not drawn as a measurement of nothing. Each site's number is that session's
  **worst** score for it, not an average over the movements that reported it: a knee aching through three
  exercises is one sore knee, and averaging would report it as milder than any rating the lifter gave.
  The overall *Joint pain* series stays beside them, because "this session hurt" is still a question it
  answers and the sites answer *where*.

- **A change's layout is checked where the change is** (N129). `detekt-formatting` stays out of the
  gate — it reports findings across a large part of the tree, about nine files in ten carrying an
  `ImportOrdering` one, so a whole-file check would fail an edit that never went near an import. `tools/format-changed.py`
  runs the whole ruleset over the files a diff names and reports a finding only when it lands on a
  line the diff added or rewrote; `--fix` lets the rules rewrite those files, and `--all` shows what
  turning a rule on would cost. This is B123's own revisit trigger — a layout defect a review missed —
  answered without either alternative it rejected: nothing is reformatted and no rule is switched off.

### Fixed

- **The measurements list scrolls** (B130). The list was a `Column` that ran `forEach` over the entries, so
  the first screenful was all there was: a lifter with a few weeks of daily entries could not see, edit or
  delete anything older, and the add button floated over whichever row happened to be last. It is the
  `LazyColumn` every other screen already has, with the empty sentence as an item of the list rather than a
  sibling above rows that may not exist.

- **The release verifier runs on the APK CI builds** (B131). `tools/verify-release.py` defaulted to the
  signed `app-release.apk`, but the CI `build` job has no keystore and writes only
  `app-release-unsigned.apk`, so the step N128 added failed on every run with `no APK at
  …/app-release.apk` and never reached the version check it exists for. It now prefers the signed output and
  falls back to the unsigned one: `versionCode`, `versionName` and the SHA-256 are readable on either, and
  only the certificate needs a keystore. The same pass stopped the CI branch returning before the hash, so
  an unsigned build is checked for the version **and** the hash the notes carry, with the certificate
  reported as unchecked.

- **The changed-lines gate no longer blames a change for a pre-existing import order** (B132). detekt's
  `ImportOrdering` reports at the file's *first* import line, not at the import that is out of order, so a
  change that added the first import to one of the ~150 files whose block was already misordered failed the
  gate on a defect it did not cause — exactly what the docstring and [AGENTS.md](AGENTS.md) promise will not
  happen. `tools/format-changed.py` now reads the base version of the file for that one rule and keeps the
  finding only when the base block was already in order: a change that breaks the order is still caught,
  and one that merely touches the block is not asked to fix what predates it.

- **A measurement's day is the local day, not 86 400 seconds** (B133). The repository ended a day at
  midnight plus a fixed span, which is the next local midnight only when the day is 24 hours long. On a
  23-hour day it ran an hour past, so a save at 00:30 found and overwrote the previous day's entry; on a
  25-hour day it stopped an hour short, so a save at 23:30 missed the day's own row and wrote a second —
  both breaking the rule that a day holds one entry of each kind. The restore's collision guard was
  computed from the same bounds, so the undo could be allowed onto an occupied day or refused on an empty
  one. `localDayBounds` now ends the day at `day.plusDays(1).atStartOfDay(zone)`, beside `localDay` so the
  repository and the screen cannot disagree about which day a moment is, and the DST bounds are pinned by a
  JVM test in `BodyMeasurementDayTest`.

- **The undo path is inside the write lock** (B134). B129 widened `writingASet` from logging to every set
  write, but `onUndoDelete` calls `logSet` to put back the row a soft delete hid and was left outside it.
  A log's comparison is two suspending reads, and the snackbar offering the undo is on screen while one
  can be in flight, so the restored set could land between them and be missed — or announced as beaten
  once it was back, the false banner B126 and B129 exist to close. The first cut's ground for omitting it
  (that it runs to its first suspension on the main dispatcher) does not hold: `logSet`'s first suspension
  **is** the write. It takes the lock now, and `onAddExercise` stays out because it adds an exercise row
  rather than a set.

- **The restore's guard and its sore-row correlation are closed** (B135). Two small holes in
  `RoomMeasurementRepository.restore`: the "day re-entered" check ran **outside** the transaction that
  does the write, so a `save` committing between them could leave two live rows for one day and kind —
  the collision the check exists to refuse — and the sore rows were correlated by `deletedAt = hiddenAt`,
  the same field the save path writes when it replaces a list, so a save and a delete in one millisecond
  shared a marker and the undo resurrected the soreness the lifter had replaced. The guard is now inside
  the transaction, and every write takes a timestamp strictly greater than the last, so the delete's
  marker can never be a save's.

- **The prose the batches left describing what they removed** (B136). N125 took the next-up rows and the
  read of a program's run with them, but left comments that still describe both: an orphaned KDoc above
  home's `ProgramsPart` promising "each active program's run", and a KDoc on `planEntriesFor` citing
  `nextUpFor`, a function that no longer exists. The same reading corrects the per-site joint series'
  description — in the registry and in [DECISIONS.md](DECISIONS.md)'s N130 entry — which had the
  workout-level series reading "a session's ratings for the movement the lifter chose" when it needs no
  lift and reads the whole session, and the `PrimaryActionButton` KDoc's account of a second caller that
  went with the rows. A reference to a function that is gone is how the next reader learns to distrust the
  rest, so they are deleted or corrected rather than left to age.

- **The picker tells its two *Chest* entries apart** (B137). The tape site `Chest` and the soreness muscle
  `Chest` rendered the same word inside the BODY group, so the headings added for *Left knee* could not
  separate them — and `withinAGroup_theLabelsAreDistinct` compared a label's *source*, which cannot see two
  sources rendering one word. The soreness series now sit in a **Soreness** group of their own, so every
  label stays the taxonomy's own word (a rename was rejected), and `MetricLabelRenderTest` resolves the
  resources and asserts the words a lifter actually reads are distinct.

## [2.4] — 2026-10-14 (versionCode 24)

### Fixed

- **The window no longer pans as well as reserving the keyboard's height** (B127). With a workout running and
  a set's field focused, a black band sat between the content and the keyboard — and **its size changed
  between attempts**. Two mechanisms were making room for one keyboard. `imePadding()` reserved the
  keyboard's height, which is B119's fix and is measured working (the body's viewport 1859 px → 958 px with
  the keyboard up, and back again with it down); the window itself was left at the platform's default soft
  input mode, which resolved to **`adjustPan`** — measured on `pixel6_api36`, the running workout's window
  reported `sim={adjust=pan}` — so the system *also* panned the window by an amount that depends on **which
  field is focused**. That is why the band had no fixed size, and why it was worst with a field low in the
  list. Android's own edge-to-edge guidance requires `android:windowSoftInputMode="adjustResize"` on every
  activity with a soft keyboard, and `enableEdgeToEdge()` does not set it; the manifest does now, so the
  window never pans and the inset is the app's alone to apply. Measured after the fix: the window reports
  `sim={adjust=resize}` and the viewport is unchanged at 958 px, so nothing was double-reserved by adding
  it. A test asserts the attribute, because nothing else in the build can notice it going missing. The B119
  entry in [DECISIONS.md](DECISIONS.md) said setting `windowSoftInputMode` "is not the fix on an
  edge-to-edge window", and is corrected with it: it is not the *whole* fix, and leaving it out is what put
  a second mechanism in the same place as the first.

## [2.3] — 2026-10-14 (versionCode 23)

### Added

- **A behind-the-back cable curl joins the seed** (N123). The curl family carried a barbell, a dumbbell, a
  machine and a plain cable curl, and not the single-arm curl taken behind the back — the one that holds
  tension on the biceps at the bottom of the range, which is what it is reached for. It states its own
  equipment (`Cable`) and reads *Biceps* and *Forearms* from the family by the own-if-stated-else-the-
  category's rule (N110), because equipment is the one fact a family never hands down. The seeder is an
  `INSERT OR IGNORE` top-up on every open, so a library that already exists gains the row on its next launch:
  **no migration and no transfer-format change**.

### Changed

- **A set's secondary muscles are credited half each** (N122). The balance view counted every planned working
  set once, for its movement's primary muscle, and credited the secondaries nothing — so a week of rows read as
  training lats alone, though the seed's own barbell row names biceps and forearms. A working set is now **one
  set for the movement's effective primary and half a set for each of its effective secondaries**, resolved by
  the same own-if-stated-else-the-category's rule the primary already reads (N110), so a family's muscles are
  inherited here too. **A muscle is counted once per set**: one named both primary and secondary, or twice in
  the secondary list, takes the primary's share and no more. Work that states a muscle nowhere keeps its own
  row rather than being dropped, and work that states secondaries but no primary credits them — no primary is
  invented for it. **The count is held in half-sets** — an `Int` and not a float, for the reason RPE is
  half-points — and the screen writes the halves back as sets (`1.5 sets`). The header still states the plan's
  own working-set count, so the screen says in words how a set is split, rather than letting the rows sum past
  the total unexplained; the bars divide by the credited work, which is what makes a lopsided week obvious.

- **Where the next workout is became one surface** (N124). Home said it twice — a dated *Today* card in the
  body and a *Next up* row in the start bar (P3.9, N55) — and the two were shown on **opposite conditions**:
  the card only when a program scheduled today, the row only when none did. The bar's block is gone and the
  body's section carries both, so one question has one answer in one place: today's plan, and the run a
  program with nothing scheduled today is up to. It stays at the **foot of the body**, where N115 put the
  day's plan, and is drawn whenever either answer exists — **not only on a scheduled day**, so a program with
  no weekday, and every rest day of one that has them, still says where its run is (P3.9). Looking at a
  planned workout and starting it stay **two gestures** (N55): a run's entry keeps the field that opens the
  read-only plan beside *Start*. The start bar keeps the *Resume* pill and is drawn only while a workout is
  open — it used to draw an empty column when nothing was scheduled. A program with a slot today is still left
  out of the next-up list, so a dated plan and an undated run never wear the same heading. Nothing stored
  changes: no schema, no transfer format, no program document.

- **The muscle balance's last filter is gone** (B124). `muscleBalance` ended `.filter { it.halfSets > 0 }`, left
  over from the `groupBy` shape N122 replaced — a group there could hold a row with no working sets, and the
  filter is what kept it off the screen. No credit can be zero now: a row with no working set is skipped before
  it credits anything, and every credit is one or two halves per working set. So the filter could never fire,
  and it sat where it read as the live guard that keeps empty rows out. What actually puts the unresolved
  bucket last is the ordering's `muscle == null` tie-break, and the counting's own doc says why it needs no
  filter, so the next reader does not put one back.

### Fixed

- **An ordinary set can be edited into the drop that opens a run** (B121). B120 taught the dialog to tell a
  run's first rung from its later ones, and asked `runAt` — which answers `null` for a row that is not a rung
  yet — with the row above as the fallback **for a row being added**. A row that already exists and is being
  changed into a drop was neither: the field was hidden, *Save* was enabled, and the write was refused with
  *"A drop run needs the value it takes off the set above it"*. That is B120's fault inverted, and a regression
  rather than a gap — the answer it replaced (`offers(SetType.DROP)`) showed the field here. The question is
  now asked of the **position**: a row already carrying a rung is answered by `runAt`, and one that is not — an
  ordinary set being changed, or a row not yet added — opens its run exactly when the row above is not a drop
  rung. The case B120 shipped without is pinned by a test beside the two it has.

- **The keyboard no longer covers the template editor or the exercise detail** (B122). B119 gave the running
  workout's body the IME inset and named the other screens with fields near the bottom; these are the two whose
  fields are the activity window's own — the editor's block fields and the detail's edit form both scroll in
  the Scaffold body — so both carry `imePadding()` for the same reason: the window is edge-to-edge and does not
  resize for the keyboard. **The measurements form is deliberately not touched**, which is a finding rather
  than an omission: its fields are inside an `AlertDialog`, and Compose leaves a dialog's window
  non-edge-to-edge (`DialogProperties.decorFitsSystemWindows` defaults to true), so the platform still resizes
  it and an inset there would do nothing. B119's own entry is corrected to name the two bodies. **The device
  measurement B119 set the pattern for was not made here** — no device was available — so the two insets are
  reasoned from the window's shape rather than measured, and the viewport reading is outstanding.

- **A malformed line, and the formatting gate that stays unwired** (B123). The balance row's signature shared
  its line with the `Column(` that should follow it, and nothing in the gate could see it: detekt loads the
  Compose ruleset alone, lint does not read Kotlin layout, and the line is inside the ceiling `.editorconfig`
  and detekt's `MaxLineLength` agree on. The line is rejoined, and the question behind it is settled rather
  than left implicit — **the formatting ruleset is not wired**, because it reports findings across a large
  part of the tree (the count at the time, and today's, are in [DECISIONS.md](DECISIONS.md#process)), which is
  a repository-wide reformat or a set of `active: false` entries. The decision, the rule-by-rule
  measurement and the trigger to revisit are in [DECISIONS.md](DECISIONS.md), and
  [detekt.yml](config/detekt/detekt.yml)'s header says so where the next person would wire it.

- **Two sets logged together no longer both announce a record** (B126). B118 moved the "what has this session
  already done" read off the screen's state and into the repository, which closed the window between a write
  committing and that state catching up — but **not** two logs whose reads both run before either write
  commits. Both then compared against history alone, so a lighter set logged beside a heavier one a moment
  earlier was announced as a personal best: the exact false banner B118 set out to remove, reached a different
  way. Logging is now **one set at a time** — the comparison, the write and the banner are a single step under
  a lock — so the second log reads a session that has recorded the first. The lock orders logging and nothing
  else: an edit or a delete landing beside a log can still move the picture that log compared against, and
  neither of those announces anything. Rejected alternative: accumulating this session's sets in the
  ViewModel, which would be a second record of what the store already holds. Pinned by a test whose fake
  suspends inside the write, so both logs' reads land before either write does — it fails on the code before
  this fix and passes after.

- **Two unreleased entries stated more than the code delivers** (B125). B120's said the run's value is read
  "from `runAt` for a row that exists", which is the case B121 contradicted; B118's promised that a set written
  a moment earlier "cannot be missed", where the read moving to the repository narrowed that window rather than
  closing it. Both sentences are corrected above — and the window the second one overstated is now closed by
  B126 rather than recorded as open.

- **Resuming a workout from home keeps its plan** (B117). The plan was read from the route's `templateId`, and
  the *Resume* pill builds the destination with no argument — so a workout started from a template showed every
  set as planned, and the same workout resumed from home showed **no plan at all**: no planned sets, no
  remainder, and nothing for the review to compare against. The route's argument says only how the workout was
  *asked to begin*; the template the session follows is recorded on the session itself, so the plan is read from
  there now, with the route still answering while the session is being created.

- **A second drop set can be added to a template** (B120). *Add set* after a drop rung prefilled the dialog from
  the last planned set — the run's value included — and saving it was refused with *"A run's drop value is
  written on its first rung"*, so a drop run could never grow past one rung. The dialog was asked the wrong
  question: whether a drop was **allowed** at that position, which is true of every rung of a run, instead of
  whether the row being authored **opens** the run — the only row a value is written on (N79). It is told now
  whether the **position** opens the run it would be part of: `runAt` answers that for a row already carrying a
  rung, and the row above for one that is not, so a rung that continues a run inherits the value instead of
  being asked for a second one. **Editing an existing later rung was broken the same way**, and is fixed with
  it. The first cut of this fix read "a row that exists" as "a row that is already a rung" and missed the
  ordinary set being changed into one, which B121 fixed.

- **A personal best is no longer announced over a better set logged in the same session** (B118). History is
  read with the live session excluded, and what the session had already done was merged back in from **the row
  being written to** alone — so a set on a second row of the same movement was on neither side, and a lighter
  set there was announced as a record. A plan may name a movement twice (N54), and a superset or a second block
  of the same lift reaches the same shape. The comparison now covers **every set this session has logged for
  the movement**, read from the repository rather than from the screen's state — so the read no longer depends
  on that state having caught up with a set just written. Two of those reads racing each other is a separate
  gap, closed with logging one set at a time (B126). Two look-alikes stay by
  design: a heavier **warm-up** or rung above is not a record (B17, N79), and a heavier set at a different rep
  count is a different record (N23).

- **The keyboard no longer covers the bottom of a running workout** (B119). The window is edge-to-edge
  (`enableEdgeToEdge`, `targetSdk 37`), so it does **not** resize for the keyboard, and nothing applied the IME
  inset — a set's fields below the fold stayed under the keyboard however far the list was scrolled. The
  workout's body carries the inset now, so its viewport and its bottom padding shrink by the keyboard's height
  and what is behind it can be reached. Setting `windowSoftInputMode` is not the fix on an edge-to-edge window.
  The other screens with fields near the bottom — the template editor and the exercise detail — share the gap
  and are **not** covered by this entry. The measurements form's fields are a dialog's, and a dialog's window
  is not edge-to-edge, so the platform still resizes it (B122).

## [2.2] — 2026-10-13 (versionCode 22)

### Changed

- **A planned set is stated with the same ± steppers as a logged one** (N111). The template editor's set
  dialog was the last place a load and a rep count were typed with no way to step them, and they are the
  same numbers the workout screen states. The target load steps by the exercise's own
  `Weight.stepGramsFor` (N77) in its own unit, keeping the one signed field a plan already wrote
  assistance in — so `+` on a `-20` target makes it **harder** rather than adding weight (N15); reps step
  by one with a floor of 1; and a drop run's value steps by that same amount. **A plan's reps are a
  range**, so each end keeps its own pair, and a range stepped backwards stays the invalid state the
  dialog already refused rather than being swapped. The exercise's target RPE steps by half a point
  clamped to 1–10 and **stays typeable** beside its buttons: every planned target is optional, and a
  control that could only step could never put one back to none (N14). The dialog grew a row, so its
  fields scroll rather than the note being clipped off the end, and the RPE took a row of its own for the
  reason N68 gave the workout's one — a label, a value and two buttons do not fit beside another number
  on a phone. The ± pair is not new: it is the one the workout's fields and the correction dialog already
  share, so this is its third caller rather than a third copy (B53).

- **A set's number, and the plan's remainder, count only the sets that stand on their own** (N112). A
  warm-up takes no number, so an exercise's first working set is **1** — on the plan, in the workout and
  in the history, which are the three lists that number sets and now read one rule for it. Four warm-ups
  and three working sets no longer number the work 5, 6, 7. *Planned sets left* follows the same rule on
  both sides, so that plan reports **three** left rather than seven, and the *Log extra set* notice
  arrives with the third working set — which is how the plan is compared with the work everywhere else
  (N20). A **rung keeps its number**, because a run is work the plan prescribes and the lifter performs,
  where a warm-up is preparation; the rule lives on the role beside `recordsEffort`, which answers the
  different question of what may be recorded (N79). The history row also gains the role label it never
  showed: with the number gone, a warm-up would otherwise be a row with nothing saying what it was.

- **The body's record is two records, and soreness belongs to the day rather than to the workout** (N113).
  Measurements offers **two** adds — a *daily* record (soreness, weight, body fat, muscle %) and a *tape*
  one — and each row says which it is, so a tape day stops inventing a weight to save and a daily entry
  stops being asked for seven sites nobody measured. **Soreness leaves the readiness note** for the daily
  record, because it is a fact about the body's recovery and a session could carry it only on a day that
  was trained: the rest day, when it is the thing worth writing down, had nowhere to put it. The day keeps
  **one entry of each kind** — N32's rule narrowed, not dropped — and an entry states only what it has: the
  weight is no longer required, so a soreness-only morning is a real entry, and *Save* is offered once an
  entry states something rather than once it states a weight. Both kinds ride one table and one export
  list, each row carrying only its own kind's numbers, so the statistics screen's body and tape series keep
  reading what they read. **The transfer format moves to 5 and the floor with it**: a weight that may be
  absent is a field whose meaning changed, so a 2.x build refuses the file rather than reading a missing
  weight as zero. Existing rows are carried across by a migration — a legacy entry becomes that day's daily
  record, and one that measured any site *also* becomes its tape record, so no measurement is dropped — and
  the sore rows an older session recorded stay on it, still read in history.

- **The longest evidence-backed decisions state the rule and defer their argument** (N107). [DECISIONS.md](DECISIONS.md)'s
  own contract is that an entry is the rule, the shortest honest reason and the rejected alternative, with the
  longer argument in [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md) — and twelve entries that already had an
  evidence section carried the full argument in the entry anyway, running to 13–28 lines. Enforcing the contract
  on those twelve moves the argument to the section that exists for it and leaves each entry four to eight lines:
  the rule in bold, why, and what was rejected. **No decision is lost** — every claim, name, measurement and
  reference in the moved prose is in the evidence section, and the two entries sharing the `#n79` anchor are kept
  in order there. The file is a reference, so this is a reading change rather than a code one: nothing in the app
  behaves differently.

- **Today's workout is the last thing on home, not the first** (N115). The *Today* block — its weekday heading
  and each scheduled plan's card — moved below the ways in, so the screen reads as the destinations first and
  the thing the app is telling you to do last. That is where the start bar's own next-up rows already sat
  (P3.9, N55), so the two ways of saying "here is today" no longer pointed at opposite ends of one screen; the
  heading travelled with the cards because both are drawn by one call. Nothing changes on a day with nothing
  scheduled, where the block draws nothing at all. The move needed an assertion of its own — two nodes' bounds,
  the idiom the workout screen uses for *Done* — because a tag-level assertion cannot see an order, and it left
  the body's LazyColumn with a tag (`HOME_LIST`): today's card is now below the fold on a short viewport, so a
  test that reaches it has to scroll the *list* to it, since a row the lazy list has not composed cannot be
  scrolled to by addressing the row.

- **A plan's effort, its rest and the accept are one line** (N116). The template editor's set dialog drew the
  rest and the check on one row with the RPE stepper on the next; the three are one statement about one set, so
  they now read across in the order effort, rest, accept. **This supersedes the row of its own N111 gave the
  RPE** — and N68's squeeze behind it — but only here: the workout screen's own RPE stepper is untouched. What
  N111 priced was a label, a value and two buttons beside another number, and the arithmetic had changed: the
  label is the field's own floating one rather than a leading word, and both numbers are one or two characters.
  So the stepper takes the flexible width and the rest field narrows from 110 dp to 96 dp, and the spacer that
  held the check at the right margin goes — with three controls it is last by construction. **The 48 dp step and
  check targets do not shrink**: they are the app's touch-target floor, so if a narrower device runs out of room
  a label gives way rather than a control. The layout is pinned by bounds rather than by tags, because the
  failure worth catching is the row quietly wrapping — the idiom the workout screen already uses for *Done*.

- **A long press on a library family shows what it states for its movements** (N117). A category head's tap
  already means fold, so its own facts — the pattern, the primary and secondary muscles — are reached by holding
  the row. Those are exactly the fields N110's own-if-stated rule resolves a movement against, which makes this
  the one place they can be read: a head is never offered and never logged, so it has no screen of its own. A
  value the row does not state reads as the detail screen's *None*, because null is **unstated** rather than
  *Other*; **equipment is deliberately absent**, since a family spans equipment and never inherits it (N110), so
  showing one would state an inheritance the model refuses.
  **The label is what carries it.** This is the app's first long press, and a hold has no control anyone can see,
  so `AppRow`'s new `onLongClickLabel` is the only thing that tells a screen reader the action exists — announced
  beside the fold action and the row's own state (B21). `AppRow` takes `combinedClickable` **only** when a long
  press is given, because that function's `onClick` is not nullable and a default would make every row without an
  action clickable: a ripple and a focus target that do nothing. Its test is the suite's first
  `performTouchInput { longClick() }`. Rejected: a trailing info icon, which is discoverable but puts a second
  control in a slot that already means fold.

- **Home offers to record the day's measurements, and only while today has none** (N114). A row below *Start
  empty workout* opens the **daily** form directly — soreness, weight, body fat and muscle — so the thing the app
  wants every day sits where the lifter already starts, rather than behind the measurements screen's FAB and its
  question about which of N113's two records was meant. It is drawn on what is stored rather than on whether the
  lifter was asked: today's entry makes it go, deleting today's entry brings it back, a tape day does not hide it
  (the two records are separate) and a soreness-only morning does, because an entry states what it has (N113).
  Recording is not starting, so B101's withholding of the start row while a workout is open does not reach it.
  **The rule is a pure domain predicate**, `BodyMeasurement.isDailyOn(day, zone)`, and the "which local day is
  this instant" helper moved out of `RoomMeasurementRepository` to sit beside it — `findOnDay` is its second
  caller, and the row's day and the query's day drifting apart is the one failure that would read as the data
  being wrong. The home ViewModel reads the day it already captured for the *Today* heading and the substitution
  write, in the zone it captured it in, and fills one field from `MeasurementRepository.observeAll()` — which is
  what makes the row live: saving makes it go, with nothing to refresh. The destination carries the intent
  (`Measurements(openDaily = true)`, the screen seeding the form it already opens from the FAB, in
  `rememberSaveable` so a dismiss stays dismissed across a rotation), so a dismiss lands on the list rather than
  back on home. Rejected: drawing the row always and letting it open today's entry for editing — a row that
  cannot say whether the day is answered is the one thing this row is for; a second destination or a
  ViewModel-held dialog, which duplicates one screen and puts transient UI state in a database-driven flow
  (N19); and a `COUNT` query of its own, cheap only by adding a port API and a second source of "today".
  Covered by `BodyMeasurementDayTest` (the day, the two kinds and the local-midnight boundary), the home
  screen's three row tests and the measurements screen's arrival test. **The ViewModel's own combine is not
  directly tested**: a home ViewModel test would need `TemplateRepository`'s 19 and `ProgramRepository`'s 21
  members stubbed, which neither has in shared form, and the one line it adds is the predicate above applied to
  the day it already holds.

- **A program slot's ⋮ opens the template that slot names** (N120). A program is a schedule *over* templates, and
  the only way from one of its slots to the workout it points at was the library, so the slot's menu gained the
  entry the Templates screen reaches — the template editor, which is where a template is edited. The slot's
  *name* keeps the in-place preview it already had ("See what %1$s trains"), so the two gestures stay distinct:
  the tap reads a day without leaving the program, the menu entry goes and changes it. It reads "Open %1$s" with
  the template's name, because every sibling in that menu names its target (*Move %1$s up*, *Remove %1$s*), and it
  sits after the two moves and before the removal, which is that menu's documented order — order first, the
  destructive entry last and coloured (N53). Nothing is stored and the route already existed, so this is one
  callback and one `DropdownMenuItem`.

- **A planned exercise's ⋮ opens the lift itself** (N121). From a template's exercise row the entry opens the
  exercise detail — its unit, step, rest, cue and taxonomy — which meant leaving the template, going to the
  library and finding the lift; planning is exactly where a lifter notices those facts matter. It reads "Open
  exercise", beside "Remove exercise", because this menu names no lift in any of its entries — the ⋮ sits inside
  one exercise's block — and its order puts it first among the optional ones: going to look at the lift changes
  nothing, so it comes after the entries that change the round and before those that write or take away (N53).
  **It is an optional entry of the shared menu**, in the shape N81's *Add warm-ups* established, and **the live
  workout deliberately does not carry it**: `ExerciseActionsMenu` is one menu for the workout's rows and a
  template's (N71), leaving a running session to look at one lift is its own decision, and a mis-tap mid-session
  costs more than the look is worth. A test asserts the absence by the words the workout's menu would show.
  The tag registry reached its function ceiling while adding this entry, so it carries a local
  `@Suppress("TooManyFunctions")` with the reason: the principled remedy is the nested-object grouping this file
  already uses for `Programs`, `Home` and `Measurements`, and moving the template tags there is a rename across
  ninety-odd call sites — its own change, not a rider on this one. The exercise row itself was one line over its
  ceiling, so its fold label is compacted rather than a helper added (which moved the *file* to its own ceiling,
  and then put `stringResource` inside a `SemanticsPropertyReceiver` lambda, where it cannot be called).

- **Soreness is a statistic, one series per muscle** (N118). N113 had the app start recording the muscles a day
  reports sore, each with its own score, and nothing read them back: the measurements list and a session's own
  history rows were the only places they appeared, so the one number the app asks for every day was the one it
  never showed. The statistics picker now offers a series per **selectable** muscle — the taxonomy minus *Other*,
  exactly what the daily form lets a lifter choose — read from the daily record's own list. A day that does not
  state a muscle is a **gap, not a zero**, which is the rule the tape sites already follow: a line at zero would
  claim a lifter was not sore on a day they simply did not answer. The scale is the `RATING` unit the workout's
  effort, muscle feel and joint pain already plot, on the same fixed 1–10 axis, and **more is worse** like pain —
  which is the one thing that differs from every other body series, and the change that made an existing test
  fail on purpose (it names the metrics where less is better, so soreness had to be added there rather than
  sliding in). **`MetricEntry` can now take its label from the domain**: a muscle's name is the taxonomy's own
  word, which the diary, the picker and the session rows already share, and inventing twenty string resources to
  say "Quads" a second time is how a vocabulary forks. The two sources are exclusive and one is always present,
  which the registry test asserts. Nothing is stored and no transfer format moves — the data was already there.

- **A program can be read for the work it spreads where** (N119). The editor's bar now carries an overflow with
  three entries — *Muscle balance*, *Export* and *Delete* — where it carried an export text button and a delete
  icon. That is N43's order for the database's own actions: three entries with one of them destructive puts the
  one that cannot be undone last and coloured, and it stops Delete sitting a thumb's width from a button a lifter
  presses often, at the cost of a tap each. *Muscle balance* is a destination of its own that counts the
  program's **planned working sets per muscle group**, so "does this week train what I think it does" is
  answerable while the week is still being written rather than after it is over. Two rules are inherited rather
  than restated: a **warm-up is not work** (N112), so a ramp does not inflate the muscle it warms up, and a
  movement's muscle is **own-if-stated, else its category's** (N110) — which is why the read includes the
  library. A set counts once, for its primary muscle; assisted work is not split off and secondaries are not
  credited, because a set is not half a set for a muscle that assists. **Work that states a muscle nowhere gets
  a bucket of its own rather than being dropped**, since dropping it would make the rest of the plan look more
  balanced than it is. **The read is new**: the editor loads one slot's template at a time for its preview (N72),
  so this is the first thing to read every template the program's slots name. Nothing is stored and no schema
  moves. The counting is a pure function (`muscleBalance`) with its own test; the screen states counts and shares
  rather than a verdict, because a range a lifter *should* hit is a claim this app has no evidence for — that is
  the parked P2.8, which stays parked with its trigger intact.

### Fixed

- **The N107 shortening had dropped a rung's reps prefill** (B109). The move's claim that no decision was lost
  was false: the N79 sentence saying a rung's reps prefill from that same set's last performance in a previous
  training — matched by the set's place in the plan, because "what you just did" would answer with the anchor's
  reps — left with the prose while the rule stayed in the code. It is back in the N79 evidence, where the
  contract sends the argument, with the `RungRun.kt` reference the same move dropped.

- **`MIGRATION_2_3` has the migration test the rules ask for** (B110). The migration that split the body's
  record in two shipped its exported schema and no test, though every migration gets one. A schema-2 store is
  rewound to its old shape and migrated with real rows: a weight-only entry becomes that day's daily record, a
  weight-and-waist entry becomes both records with the numbers split between them, a deleted one stays hidden
  on both, and the sore table is created and takes a row keyed to the daily record.

- **The blank-site rule and the split's keying are tested again** (B111). The test named for the blank-site
  rule was stamped `kind = DAILY` when the record was split, and a daily row drops every tape column by
  construction, so neither save wrote a site and the assertion held whatever the repository did. It is a
  **tape** correction now, and the split's own rules are covered with it: a day holds one entry of each kind,
  a save replaces the sore list, a score off the 1–10 scale is refused before anything is written, and a
  delete hides the rows that hang off the entry.

- **The session delete's sore rows are seeded before they are asserted on** (B112). N113 took the sore list
  off `setReadiness` and the test was reduced to a null note while its comment and its count still claimed to
  prove that deleting a workout hides an older session's soreness. With no row written the count was zero
  however the delete behaved. The row is written through the DAO now — a live session cannot write one, but
  the rows a pre-N113 workout left are still on disk — and a second assertion checks it is live *before* the
  delete.

- **The tag file lost six dead sore-picker helpers and got two comments back** (B113–B115). The readiness
  dialog's sore list moved to the daily form at N113, and six `TestTags.Readiness` helpers that named its
  picker, rows, score controls and remove were left with nothing calling them; they are deleted, with the
  object's KDoc and a dangling reference to the row helper gone too. The comments that describe
  `templateMenu` and `slotMenu` are back above the members they describe, rather than above the two entries
  N120 and N121 inserted between them and their declarations. And `TEMPLATE_EXERCISE_RPE`'s comment no longer
  gives the effort a row of its own — N116 put it on the line the rest and the accept share.

- **A measurement comes back from an import with its soreness** (B116). The daily record's sore rows gained a
  restore path the entry they hang off did not have, so an import could un-hide a record's soreness onto a
  record nothing could read. Writing the test found the larger half: the measurements insert was the one
  import that used `ABORT` rather than `IGNORE`, so a row the device already held — live, or hidden by a
  delete the import was meant to undo — **aborted the whole import and rolled it back** instead of being
  skipped. It is `IGNORE` now, the DAO has the `softDeletedIds`/`restore` pair every other table has, and the
  parent is restored before its child.

## [2.1] — 2026-10-11 (versionCode 21)

### Added

- **The library ships a strength and bodybuilding set** (P1.1). 61 movements join the 45, taking the seed to
  106 across six new families — **Rear Delt**, **Shrug**, **Back Extension**, **Forearm**, **Adductor** and
  **Carry**. The additions fill out the powerlifting bar lifts and the assistance work around them (front,
  box, hack and goblet squats; sumo, deficit and rack pulls; good mornings; close-grip, floor and decline
  presses; Pendlay and T-bar rows; chin-ups) and the machine, cable, dumbbell, band and kettlebell work a
  bodybuilding program is built from. **Every value the pickers offer now has an exercise behind it**:
  `KETTLEBELL`, `BAND` and `OTHER` were carried by no row, and `FOREARMS`, `ADDUCTORS` and `LOWER_BACK` were
  no movement's primary muscle. **No existing row changed** and the seeder tops up on every open, so an
  installed library gains the new entries on its next start without a migration. A new unit test holds the
  shape: unique ids, every file target a category, and no equipment, pattern or muscle left unanswered. Two
  of the oldest rows are retired in the same release, so the library ships **104** — see *Removed*.

### Changed

- **The exercise picker groups movements under their category, exactly as the library does** (P1.1). Adding an
  exercise to a workout — or to a template, which shares this picker — now shows the families a lifter filed,
  folded on arrival, with the movements indented under the head that holds them and the ones in no category at
  the top level. **A head still cannot be chosen**: its tap folds it, and only a movement reaches the add, so
  N95's "a category is never offered and never logged" now holds at the tap rather than by leaving the heads
  out of the list.

- **A search opens every family it reaches into** (P1.1). Both grouped lists — the library and the picker that
  logs from it — now show the movement a query matched rather than the folded head above it, so searching
  "back squat" answers with *Back Squat* instead of *Squat · 1 exercise*. The rule lives in the grouping
  rather than in either screen, because the fold is how a list is *browsed* and a search is not browsing; a
  blank query is not a search, so the folds a lifter set stand until they type.

- **The exercise library opens with every family folded** (P1.1). The list arrives as the families it is —
  the category heads, each with the count of what is inside, plus the movements filed under nothing — instead
  of as a wall of every movement. Tapping a head opens it, and that fold survives a rotation. The editor keeps
  N91's opposite rule, where a planned exercise's first set opens so it needs no second tap; the library
  cannot use that rule at all, because it loads asynchronously and nothing would look "already there", so
  every family would open.

- **The library is a category over movements, and a movement reads its family's taxonomy** (N110). The
  **variation level is gone**: it was one `parentId` carrying two meanings, told apart by the parent's
  `rowKind`, and the narrower roll-up it bought is one the category already offers — a category's series has
  always read its movements' variations too, so **no family's numbers move**. A movement's **movement
  pattern, primary muscle and secondary muscles are its category's until it states its own**, so a family
  states what its members share and the odd one out states its own — the lunge family states Quads, and the
  walking and reverse lunges state Glutes instead. **Equipment is never inherited**, because a family spans
  equipment: the lat pulldown family alone holds a cable, a bodyweight and a machine movement. The editor now
  offers those three fields rather than hiding the ones a category supplies, and shows what *not stated*
  resolves to, because under own-if-stated a movement may disagree with its family and a hidden picker cannot
  say so. A library written by the previous build is carried across by a **data migration** that fills the
  seeded categories from the seed itself, writes down what each variation used to read from the lift above
  it, and re-files it under the category — leaving any value a lifter set alone and clearing a link that is a
  cycle rather than turning it into a self-parent. An older backup or program
  document is **flattened the same way on import** rather than refused, so the family survives the trip.

### Removed

- **Two movements that were a second name for one already in the library are retired** (P1.1).
  `Two-Arm Cable Pushdown` was `Triceps Pushdown`, and `Machine Row` was `Chest-Supported Row` — one movement
  under two names, which the picker offers side by side and a lifter cannot tell apart. **A row dropped from
  the seed is not deleted from a device that already has it**: the seeder only ever inserts what is missing
  and there is no delete for a library row, so an installed library keeps both names, and a set already
  logged against a retired id still names its lift. A backup still carries the row and a program document
  still recreates it, so the id is orphaned nowhere it is already referenced — what changed is what a *fresh*
  install ships, which is one name per movement.

### Fixed

- **An empty workout can be discarded without adding a movement first** (N41, N87). The overflow was hidden
  for a session with nothing in it, on the reasoning that an empty session's discard was already drawn in the
  body — which left a workout started by mistake, or resumed by accident, with no way out but to put an
  exercise into it. **The overflow is now drawn whenever a session is open**, and it is where the discard
  lives for every state; the empty state's own copy of the button went with it, so one action has one home.
  The discard stays **prompt-free** for an empty workout: the dialog exists to name what a discard costs, and
  there is nothing to name.

- **A workout or template row reads an unstated muscle or equipment instead of throwing** (B103). The two
  joins that carry a library exercise onto a session and a plan projected `primaryMuscle` and `equipment`
  into non-null fields, while both columns have been nullable since N103 made *unstated* a real value — so
  Room emitted no null guard and the NULL reached a non-null converter. **Any custom exercise triggered
  it**, because one is created with a name and nothing else. The fields are nullable through the domain now
  and the one screen that drew them asks the shared `taxonomySubtitle` helper, so a row that states neither
  leads with its name rather than with *Other · Other*. Both joins also resolve the category a movement is
  filed under, so a movement that leaves the muscle to its family still reads it; equipment is not
  inherited, because a family spans equipment.

- **The program document's import is pinned to the shape it now writes** (B104). N110 re-files a movement
  filed under a movement onto its category on import, which is what a document written before the change
  carries, and the test still asserted the old link — so the instrumented suite was red while the default gate,
  which never compiles that source set, stayed green. The test now asserts the flattened parent **and** the
  facts the fold writes onto the row, so the behaviour and the test agree.

- **`libraryRows`' flat branch is gone** (B105). Its `grouped` parameter existed for the pickers, and the picker
  was moved to the grouped list in the same release, so nothing under `main` passed `grouped = false` — the
  branch, its parameter and the test that was its only caller are deleted, and the search's trim is documented
  as what it is rather than as the flat branch's reason.

- **The lunge family's documentation matches the seed** (B106). Four files said the family "holds no single
  primary muscle" and had two movements stating their own; the seed gives it `QUADS` and files four movements
  under it, of which the walking and reverse lunges state `GLUTES` and the other two read the family's. The
  sentence now says what own-if-stated actually demonstrates: a member disagreeing with its family.

- **The seed's own doc counts what was added** (B107). Its KDoc said "four" equipment values and "four" muscles
  where three of each had no exercise of their own — `KETTLEBELL`, `BAND` and `OTHER`, and `FOREARMS`,
  `ADDUCTORS` and `LOWER_BACK` as a primary. The counts now match the seed and this entry.

- **The library migration clears a cycle instead of leaving a self-parent** (B108). The re-file was one SQL hop,
  so a cycle — which a file can carry — became two rows pointing at themselves rather than the link going. The
  walk now carries the same `seen` guard the import's shape rule does, and reaches the first category however
  long the chain is. Two instrumented tests hold it.

## [2.0] — 2026-10-11 (versionCode 20)

**2.0 starts a new store, and it does not read the old one.** Upgrading leaves 1.x's file
(`workout.db`) on the device, untouched, and starts an empty library-backed store beside it: **2.0 cannot
read your 1.x history, and it refuses a backup or a program document written by 1.18 rather than
misreading it.** Export from the 1.18 build first if you want the log — that build is still the way to read
it afterwards — and note that "delete everything" in 2.0 removes it.

### Changed

- **A row states what it knows, and reads the rest from the head above it** (N103).
  `primaryMuscle`, `equipment` and `movementPattern` are nullable, and **null is *unstated*** — `OTHER` goes
  back to meaning only *other*, which is a real answer a lifter gives. One rule covers all of them, the
  secondary list included: **own-if-stated, else inherit**, walking all the way up, so a variation reads what
  its movement inherited from the category above both. A row nothing states a value for answers *none* rather
  than a guess, and the editor offers a field only where no head supplies it — a picker can no longer write a
  value the resolver would ignore, which is what the old head-wins rule did to a movement's own muscle. The
  seed follows: a movement states its own muscle, equipment and secondary list and no pattern, a variation
  states nothing at all, and a new exercise or category starts with a name.

- **The detail screen reads the whole chain, not the head alone** (N103, B96). A variation's pattern and a
  muscle stated two rows up were invisible on its page; the screen now draws the resolved answer — and the
  family's secondary list is shown once, where it belongs, instead of twice.

### Removed

- **1.x's store, and everything that carried it** (N104). The database is a new file at schema 1, so the
  migration chain, its exported schemas and its suite are gone — nothing in 2.0 can reach them. The four
  retired movement-pattern names and the shim that filtered them go with it: both transfer formats move to
  v4, and the version gate now refuses a **1.x file before the decoder**, which is what makes deleting a name
  a 1.x row could carry safe. `fallbackToDestructiveMigration` stays absent: the old store is left alone
  rather than dropped, which is the whole difference between this and losing data.

- **The rest of the legacy the new store cannot hold goes with it** (N105). N104's rule — a name nothing
  can reach should not exist — reached the four retired movement-pattern names and nothing else. The
  `BACK` group the N75 split retired is gone from the enum with its `SELECTABLE_MUSCLE_GROUPS` filter; a
  session exercise no longer carries the single `jointPain` number or its free text (N63), so the rating
  editors, the history row and the joint-pain trend read the picked list alone; and `template_sets` no
  longer carries a per-set target effort (N59), so a plan's one RPE is the exercise's. The transfer format
  also reads no pre-half `rpe` / `targetRpe` field (N6), which only a 1.x file ever wrote. Schema 1 is
  amended in place rather than migrated, which is legal because 2.0 has not shipped (B16).

- **Two columns and a transfer field nothing read are gone** (N108, N109). `exercises.isCustom` and
  `set_entries.completedAt` were written and round-tripped but read by no decision: the id scheme already
  says which rows are the lifter's (seeded slugs against UUIDs), and `createdAt` is the same instant as
  `completedAt`, because a set is never completed later than it is created. Schema 1 was amended in place
  while it is still unshipped (B16). The backup also stops carrying a session's `restEndsAt` —
  device-and-moment state the restore discarded — so the codec's one documented round-trip exclusion is
  gone with it.

### Fixed

- **A failed save or delete on Measurements is surfaced** (B102). The ViewModel reported the failure on its
  error channel, but the screen never read it and `onDismissError` was never called, so a write that did not
  land said nothing — the one screen that behaved that way while every other one shows the same message
  through a snackbar. The screen now reads the channel, and the always-null `error` field on its UI state
  went with the fix.

