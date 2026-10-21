# Decisions

Settled choices for Workout, kept out of [ROADMAP.md](ROADMAP.md) so that file stays a
queue. Nothing here is a task: each entry is a decision already taken, recorded so it is
not relitigated by accident. A decision that constrains **unshipped** work stays with that
work in the roadmap — N39's parked plan target is the one left — and moves here once the
feature ships.

Each entry states the rule, the shortest honest reason, and the alternative that was
rejected. The argument behind it — measurements, observed history, verbatim errors, the
longer case against the rejected option — is in
[DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md), one entry per feature id. This file states
the rule; that one argues it.

> Reviewed against the code: 2026-10-15. Bump the date when this file is checked, the way
> [ROADMAP.md](ROADMAP.md) does, so the next reader knows how old these rules are.

## Data model

- **A weight's unit is presentation, and an exercise may choose its own** (N64). Storage stays whole
  grams, which is what makes the switch free and reversible: nothing on disk changes, and a value
  typed in pounds reads back as the pounds that were typed — pounds round to the nearest tenth *from
  the stored grams*, once, rather than truncating to `220.4`. The app-wide unit lives in Settings and
  is kilograms until it is changed, so an upgrade moves no number. An exercise's own unit is a
  nullable column holding the enum's name: null is "follow the app", which is a third answer and not
  a missing one, which is why the edit form offers three chips rather than two. **Statistics and body
  measurements are out of scope by decision**: their units belong to the metric, chosen per series in
  a registry that knows nothing about exercises, and making a chart follow an exercise's unit would
  mean a series that reads differently depending on which lift was picked last.
- **The unit reaches the screen as an ambient, and reaches every function as a parameter** (N64).
  `LocalWeightUnit` is provided once at the shell and read where a number is drawn, because threading
  it through every composable between the root and the field did not fit the length ceilings this
  project enforces — the batch that added it had already split three functions for exceeding them.
  Nothing outside composition depends on it: `Weight.format`, `parse` and `stepGrams` take the unit as
  an argument, so the pure layer stays pure and testable, and the local's default is the unit the app
  shipped in so a preview or a test behaves as it always did. The allowance is named in
  [detekt.yml](config/detekt/detekt.yml) rather than left to the rule's default, so the next ambient
  is a decision rather than a habit.
- **The muscles are one vocabulary, and the split is the list** (N75, completed by N105). Adductors is a
  `MuscleGroup` entry and nothing else, because the daily sore list is derived from the taxonomy, so a
  second list of "muscles you can be sore in" would be two vocabularies for one idea. `BACK` became
  `LATS`, `UPPER_BACK` and `LOWER_BACK`, and the retired name is gone from the enum (N104, N105).
  ([evidence](DECISIONS-EVIDENCE.md#n75))
- **An exercise may name the step its weights move by** (N77). The ± buttons, the warm-up ramp and the
  progression offer all move a load by a step, and a machine that jumps 5 kg — or 1 kg — was always
  edited against the unit's, so the value is the movement's own: `stepGrams: Long?`, with null meaning
  the unit's. It is a property of the movement rather than presentation, so it is not cleared when the
  unit changes, and zero is refused rather than stored. Rejected: making it a setting, since the
  machine varies by movement, and deriving it from the equipment enum, which does not say how a stack
  jumps. ([evidence](DECISIONS-EVIDENCE.md#n77))

- **Weights are whole grams in a `Long`**
  ([Weight.kt](app/src/main/java/com/example/androidapp/domain/Weight.kt)) — exact 0.5 kg
  and 1.25 kg steps, no floating-point drift; units are presentational.
- **The v1 set row is `reps × weight`.** Bodyweight is reps at 0 kg — accepted, clamped,
  asserted by a test. Duration and distance are out of scope; **N15 extends this** with
  `assistanceGrams` beside the weight rather than letting a signed weight carry two
  meanings.
- **Enums are stored by name**, never ordinal, so reordering cannot reinterpret rows on
  disk.
- **Rows are sync-shaped** — UUID ids, `createdAt`/`updatedAt`/`deletedAt` soft deletes —
  so a future sync stays a decision, not a migration. A session's zone offset, the piece this
  entry used to call missing, landed with N25 (`zoneOffsetMinutes`); the design beyond the
  row shape is still parked (ROADMAP P4.9).

## Templates and plans

- **A plan's sets are targets, and nothing verifies them** (N14). The logged set is a
  separate `set_entries` row and is expected to differ; every target is nullable, because
  "work up to a heavy single" has no weight to write down and a zero is a claim the app
  cannot check.
- **One role vocabulary for planned and performed sets.** `SetType` gained `TOP_SET`
  rather than a parallel plan enum, so one idea keeps one name; storing enums by name meant
  no row changed.
- **A drop or a cluster is a group, and its rungs carry no targets of their own** (N79). The two roles
  are one shape — a contiguous run hanging off a working set, the run's **anchor**, found by adjacency
  rather than by a column — and they differ in the load: a cluster rung repeats the anchor's, a drop
  rung is the anchor's less the run's value, once per rung. The plan therefore stores the **value** once,
  on the run's first rung, so a weight per rung — rejected — cannot drift from the anchor, and a rung
  names no reps because the group's target is the anchor's. Deleting an anchor takes its rungs with it, and
  **the first rung does too** (B152): it is the row the run's value is stored on, so removing it alone
  stranded the survivors with nothing to derive from and no way to put the ladder back — re-typing the value
  made the survivor rung 1 and re-valued the plan. A cluster repeats the anchor's load **as a whole
  `Load`** (B153): an anchor giving 20 kg of assistance derives assistance and no added weight, so carrying
  only the weight half read it as a stated 0 kg and logged bodyweight while the machine was helping.
  ([evidence](DECISIONS-EVIDENCE.md#n79), [B59–B72](DECISIONS-EVIDENCE.md#b59-b72))
- **A plan's numbers are stated with the same ± steppers as a logged set's, and stay typeable** (N111).
  A target and a performed set are the same two numbers (N59), so the plan is authored with the control
  the workout already has rather than with a second one: the load moves by the exercise's own
  `Weight.stepGramsFor` (N77) in its own unit and keeps the one signed field assistance is written in
  (N15), reps by one with a floor of 1, and the effort by half a point clamped to the scale. **The field
  is not replaced by the stepper**, which is where the plan and the log deliberately differ: every
  planned target is optional (N14), blank means "the plan asks for nothing", and a control that could
  only step could never put a target back to none — the workout's RPE is step-only because a set being
  logged always states one. A plan's reps are a range where a logged set is one number, so each end
  keeps its own pair and a range stepped backwards stays the invalid state the dialog already refused
  rather than being swapped.
- **A template is living, and a session reads it at the start** (N16). Nothing links a
  session to its plan beyond the route that started it, so editing a plan changes the next
  prefill; writing targets onto the session would freeze them and make "living" false.
- **A template can keep a version, and a version is a template row of its own** (N134). A version is a plan
  with exercises and sets, so it lives in the tables they already live in, flagged by a nullable `versionOf`
  naming the template it was kept from; the templates list adds one predicate, `versionOf IS NULL` beside
  `deletedAt IS NULL`, so a version never appears among them. **Never automatic** — the lifter presses *Keep a
  version* at the moment they decide to change something — and **never edited nor restored**: it opens as a
  read-only plan, because the request is reference and a restore turns a record into an editing path whose
  overwritten state would then need keeping in turn. **It stores its own name**, because history reads the
  live template's name live (P3.3), so marking a version by renaming would rewrite how past sessions read.
  **The export carries versions and the link with them**, since the file is the only path off the device.
  Rejected: three revision tables and a migration — a version is a plan, and one nullable column is the whole
  of the difference. ([evidence](DECISIONS-EVIDENCE.md#n134))
- **A plan is read by position, and position stays contiguous** (B72). A running session matches its
  logged sets to the plan by `setIndex` against the number logged, and a run's continuation is keyed by
  the same index, so a stored index *is* the plan's order rather than a durable id. Removing a planned set
  therefore renumbers the survivors, in the same transaction as the delete — the invariant `prependSets`
  already holds from the other direction — instead of leaving a gap that points the prefill and the rest
  rule at the row after the one they mean. **A logged row is the exception, and it is why the two spaces are
  read differently** (B157): a set's stored `setIndex` survives a soft delete and re-logging appends at
  `max + 1`, so the app's own Undo leaves a permanent hole — log three sets, delete the third, undo it, and
  the live indices are 0, 1, 3. Anything matching a logged row to a plan row therefore matches by **place in
  the ordered list**, never by the stored index, which is a record of where the row was first put and not
  where it now sits. ([evidence](DECISIONS-EVIDENCE.md#b59-b72))
- **The plan's rest and cue win over the library's; null means "the library's"** (N14,
  extending N5) — copied onto the session exercise when it is seeded from a plan.
- **A prescribed rest of zero is a value — "no rest" — and only a negative is refused** (N45,
  amending N5). The field has three states: empty inherits, a positive number is that rest, and
  zero means the exercise has none; downstream `startRest` already computes an end instant at *now*,
  so a zero rest simply never runs. It is deliberately not the rest-timer switch (N44): one is a
  fact about the exercise, the other a preference about the timer. The **default** rest keeps its
  5–3600 bound (N21) — an app-wide zero would take the rest out of every exercise at once, which is
  the switch's job — and a stored zero is displayed as a word rather than `0:00`, which reads as a
  rest that has run out. ([evidence](DECISIONS-EVIDENCE.md#n45))
- **A new planned set starts from the last one, and there is no Duplicate** (N46, narrowed by N81). *Add
  set* in the template editor opens the set dialog on the last set, so a plan is extended by confirming
  rather than retyping and *any* count is reachable. **Duplicate** doubled (1, 2, 4, 8), left the odd
  counts to manual adds, and appended the whole plan, ramp included, into an order `setIndex` exists to
  keep. The accepted cost is that prefilled values look like saved ones, so "Add set" then Save without
  touching a field is a plausible accidental double-add; the title says it is adding and the list behind
  it shows the count.
  ([evidence](DECISIONS-EVIDENCE.md#n46))

- **Assistance is a magnitude in its own column, never a signed weight** (N15).
  `weightGrams` stays non-negative so volume stays `weight * reps` and contributes nothing
  rather than subtracting; the editor shows one signed number and steps that, so the weight
  column cannot go below zero. ([evidence](DECISIONS-EVIDENCE.md#n15))
- **`Load` stays a two-property `data class`; it cannot be a `value class`.** Inlining
  means storing one signed number, which N15 rejects, and it would buy nothing because
  `parseLoad` returns `Load?` and is boxed anyway. ([evidence](DECISIONS-EVIDENCE.md#n15))

- **RPE is half-points in an `Int`; the feel and pain ratings are not** (N6, extended for
  9.5). `19` is exactly 9.5; it refuses anything finer than a half rather than rounding,
  because a rounded 9.3 would claim something never measured. The column is `rpeHalves`.

- **A scheduled plan is a living template, not a dated instance** (N16). Dated instances
  would add a plan-per-date entity, plan generation and skipped-week handling for a
  comparison the logged sets already allow; several plans may share a day. Its *scheduled thing* is a
  program's slot, and the template's own weekday pin is gone (N56) — a template has no order, no
  next-up and no adherence to belong to, so the day lives where the schedule does.
  ([evidence](DECISIONS-EVIDENCE.md#n16))
- **A history row is headed by the weekday and the date, and says which plan it was** (N57, N58). The
  headline is the locale's short weekday plus the `MEDIUM` date, read in the session's own zone: the
  weekday is what a lifter navigates by, and the month heading above already carries the month. The
  template's name joins the supporting line, read **live** from the template row rather than
  snapshotted onto the session — so a rename relabels the past and a soft-deleted template still names
  the workout it was. A column on the session was rejected: it costs a migration and changes only the
  half (a rename) that is already accepted. The line therefore **wraps** rather than truncating, since
  the name is the part that cannot be inferred from the workout.
  ([evidence](DECISIONS-EVIDENCE.md#n57))
- **A day is a scheduling fact, and a template carries none** (N56). The N16 pin was the weaker of two
  places answering "what am I doing on Tuesday", so the column, the `setWeekday` path through DAO,
  repository and editor, the `pinnedFor` fallback and the backup DTO's `weekday` went with it. A
  program's slots are the only source of a dated plan; with no active program there is then **no *Today*
  list**, which is the point rather than a regression. Getting a day back means putting the template in a
  program, which is the rule being stated.
  ([evidence](DECISIONS-EVIDENCE.md#n56))
- **Looking at a planned workout and starting it are two gestures** (N55, N127). A card's own name is
  the one place a workout can be seen before it is begun, and a card used to offer only the start. The
  field and *Start* are separate targets. What the name
  opens is a **read-only dialog of the workout's ordered exercises**, which settles the choice the
  roadmap left: a template's only destination was its editor, and reading a plan does not need the
  power to rewrite it. Reusing the editor with a read-only flag and previewing on a screen of its own
  were both rejected — the first for putting every target one mis-tap from a rewrite, the second for a
  destination holding a handful of names with nothing to do on it.
  ([evidence](DECISIONS-EVIDENCE.md#n55))
- **A session's exercise order is its own, and it is written as it changes** (N54). The template is
  never touched, so a rack taken on one afternoon cannot rewrite every future run. Persisting the order
  with the session is the decision — a repeat (N48) copies the session's own order, and a process death
  mid-session keeps the order that was arranged. Rejected: an in-memory order written at *Finish*, which
  would show one order and store another if the app died.
  ([evidence](DECISIONS-EVIDENCE.md#n54))
- **The next set is stated on the workout screen, and *Log set* writes what is on it** (N59, superseding
  N51 and restoring B7 for this path). The values the plan and history prefill are the exercise block's
  own fields, so they are read and changed before anything is committed, and the button beside them
  carries no values because the fields beside it *are* the values. Rejected: N51's editor in front of
  every set, which put a dialog between every set and a button whose label could not describe what it
  wrote. ([evidence](DECISIONS-EVIDENCE.md#n59))
- **An exercise's rare actions live behind its own overflow, and what cannot be done is not offered**
  (N53). *Superset with above* was a text link in every exercise header and *Delete* an icon beside
  *Done*; both are rarely used, and the header is read constantly mid-session, so the two of them cost
  more attention than they earned. The action moved rather than changed: Delete keeps its confirmation
  (B2), because removing an exercise takes its sets with it and has no undo, and pairing keeps its
  row-0 exclusion (B28) and its absence on a done exercise (N7) — the exclusion is now the *entry* not
  being offered rather than a control that writes nothing, which is the same rule N54's move entries
  follow at either end of the list.
- **An exercise says when the plan's work is done, and the control goes on logging** (N52). Past the
  last planned set the exercise kept accepting sets with nothing to say the plan had been answered;
  `comparePlanToActual` said so only in the review, after *Finish*. The plan is the template the
  workout was started from, so the moment its last set is written *Log set* becomes **Log extra set**
  beside a notice. Nothing closes — logging another set is still what the control does, and ending the
  exercise is the **Done** already in its header (N7) — because the extra set is stated by the same
  fields as any other, and a modal would interrupt the next exercise's first set for something the
  lifter may simply read and walk past. The rule is "no plan
  never says done": an empty workout, or an exercise added by hand, has no plan to have finished, which
  is why the count is null rather than zero, and why it is read from the plan the session was seeded
  from rather than from whatever the row now sits beside (N54).
- **"No dead weight" is strict about APIs that exist to be tested, lenient about tests'
  instruments** (D2). `Weight.step`, `DataResult.map` and `successUnit` went, with the
  tests that only exercised them; `ExerciseDao.insertAll`, `softDelete` and
  `CrashLogStore.latest` stay, because a test calling a method to arrange or read its
  subject is a caller. An accessor whose only reader is the test asserting on *it* is not:
  the line is what the API is for, not where it is called from.
  ([evidence](DECISIONS-EVIDENCE.md#d2))

- **A per-exercise trend plots the number that moves, and says which way is forward**
  (N17). Load series come from *working* sets only; a set with no added weight is not a
  load; an assisted series is the session's *least* assistance, labelled "less is more".
  The one-rep-max estimate is Epley's, from the heaviest working set, refused beyond twelve
  reps and rounded to the nearest half-kilo. ([evidence](DECISIONS-EVIDENCE.md#n17))

- **The JVM tests are not forked across JVMs** (D1). Measured slower at roughly six times
  the CPU, and most of the task is compilation and Robolectric merging, which forking
  cannot overlap. ([evidence](DECISIONS-EVIDENCE.md#d1))

- **A test tag arrives with the test that asserts on it** (D4). Tags already applied in
  production and asserted by nothing are kept rather than swept, and the change that next
  touches one pays it off. A new tag without its test is a finding; a tag mapping to no
  control is deleted on sight. The backlog's size is deliberately not recorded: it drifts.
  ([evidence](DECISIONS-EVIDENCE.md#d4))

- **The role for the next set is armed in the fields that state it, and clears itself** (N19, moved by
  N51 and again by N59). The picker used to sit beside the button that wrote the set in one tap; with
  logging stated by the next set's own fields, the same choice is made among them, before the write —
  so N19's rule (a role is a decision about one set, and nothing lingers past it) survives the move
  unchanged. The pending-role-per-exercise
  alternative stays rejected for the same reason it was: it makes transient UI state part of a
  database-driven flow, and a composable holding the armed role is smaller than a store would be.
  ([evidence](DECISIONS-EVIDENCE.md#n19))
- **The pending set's role follows the plan, and the picker overrides it for one set** (B48,
  amending N19). The plan's next unlogged set arms the role the way its reps and weight already
  prefill, so a template that opens with a ramp records warm-ups as warm-ups; N19's "clears
  itself" still holds, with the plan rather than a hard-coded working set as the resting value.
  A second pass that rewrote the logged sets from the plan was rejected, because N14 holds that
  a logged set is the record of what happened and may differ.
  ([evidence](DECISIONS-EVIDENCE.md#b48))
- **One predicate decides whether a ramp can be built, and the guard and the action both read it**
  (B50, N28). A weight merely *typed* is not a weight a ramp can be taken from — an assisted set
  stores `0` kg, and a working weight at or below one loadable step has no lighter warm-up — so one
  `warmUpRampFor` call hides the button and refuses the write together. A guard predicate beside an
  action predicate was rejected: it is what produced a control that reported success over no change.
  ([evidence](DECISIONS-EVIDENCE.md#b50))

- **A plan is compared against the work, not the warm-ups** (N20). Warm-ups are excluded
  from both sides for N17's reason; a plan naming no weight leaves the delta *null*, not
  zero, because zero claims the lifter matched a plan that never said.
- **The review is a moment, not a screen** (N20). It is built from state the workout
  screen already holds, so no reads and no schema; the accepted cost is that it is not
  revisitable from history, and the honest fix for that is storing the plan with the
  session rather than re-deriving it.
- **The plan's effort is compared to the working sets' mean** (N131). The plan names **one effort per
  exercise** (N59) and the log holds **one rating per set** (N6), so unlike weight and reps this
  comparison has to reduce the sets to one number — and the reduction is the decision. It is the
  **mean of the working sets that recorded one**, because N59's target is carried onto *every*
  prescribed set: the question is whether the work landed where the plan put it. The **heaviest set's
  rating was rejected**: it answers what the top set felt like, and a session with one on-target top
  set and three off-target back-offs would read as matched. A mean is **rounded to half a point**,
  the app's own RPE resolution — `8.3` is not a rating anybody gave, the same reason
  `estimatedOneRepMax` rounds to the nearest 500 g. **Null stays null**: a plan naming no effort says
  nothing about it, and nothing rated reads "no RPE recorded" rather than silence, because silence
  there reads as *matched*. What did not travel: the prefill and the progression use of the target.
  ([evidence](DECISIONS-EVIDENCE.md#n131))

- **Settings live in `SharedPreferences`, not DataStore** (N21) — integers, booleans and two
  small maps owned by one process; writes are **committed**, so the screen never reports a
  save that did not reach disk. The move is warranted when a setting needs a schema or a
  migration, and the goal map — one line per metric — is still short of that.
  ([evidence](DECISIONS-EVIDENCE.md#n21))
- **A setting is a device preference; a metric target is training data** (N21, N39). The rest,
  the cue, keep-screen-on and the statistics range are not exported, so a fresh install returns
  the default rest to 90 s. A target is authored rather than chosen, so it rides in the backup
  and "delete everything" clears it; leaving it out was losing it on every restore, in silence.
- **The default rest is a bounded choice, not a number field** (N21) — zero or a negative is
  not a rest and longer than the session is a mistake, so the repository refuses anything
  outside 5–3600 seconds as `DataError.Invalid`. (It used to argue from the value becoming an
  alarm; the alert is gone with N26, and the bounds are still right.)

- **Progression is not double progression** (N22, withdrawn by N59; N50/N74 put a gated version back).
  Adding reps to the plan's rep ceiling and then the smallest loadable step was rejected: a percentage
  rule needs a true one-rep max this app estimates rather than measures, a linear weekly add ignores
  missed sessions, and assisted work inverts the direction. The offer that exists is earned from the
  plan's own target RPE and asked at *Done* (below).
  ([evidence](DECISIONS-EVIDENCE.md#n22))
- **The app suggests; it never writes.** A silently applied suggestion is a programme
  decision taken without the person training, and this app is a log, not a coach.
- **Warm-ups are excluded from progression too** (N17, N20, N22) — a warm-up is not the
  work a target is measured against.
- **The progression is asked at *Done*, and the plan's own target RPE is what earns it** (N50). A plan's
  working set carries a target RPE (N14, N59), and where **every** prescribed working set was performed
  with its reps met at or under that target the session had room in hand, so *Done* offers the next step
  as the lifter's choice: the smallest loadable step, or a rep — and **only a rep where the plan names no
  added weight**, because an assisted set stores the machine's help as a magnitude with zero kilograms
  and a bodyweight set stores zero, so neither has a load a step can raise: offering `0 → 2.5 kg` there
  would write a weight that `Weight.display` then hides behind the assistance. *Every* prescribed set is
  **the plan the screen showed** — the template's own rows, since N73 removed the slot's — where a slot
  that overrode one of three sets used to make "every" mean one and pair it with the wrong performance. **N74 amends both halves of that**: every set was judged as one, and the single
  step always landed on the last of them, so a set that answered the plan earned nothing when a sibling
  missed and the dialog named no set at all. It is not N22's rule restored — that computed from the rep
  ceiling alone and offered itself beside the next set, where this one is conditioned on the session's own
  effort and waits to be asked; **N74 brings N22's shape back inside the range, still gated on that
  effort**. An unrated session or a plan with no target RPE suggests nothing rather than guessing.
  **Where there is no plan there is no prompt at all**: a next step is something only a plan can ask, so
  *Done* simply finishes the exercise. **The rating is never on this path** — *How did that feel?* is
  opened from the exercise's own row, when the lifter reaches for it, because asking on the way out asks
  at the moment the answer is worth least.
  **Accepting writes the plan** — the template's planned set, for a program start and a direct one
  alike, because a slot prescribes nothing of its own (N73) — because the plan is what the next run
  reads; a "next target" stored on the session was rejected as the forward view N16 removed. The write carries the **set's own**
  stored effort rather than the exercise's number the rule read, so a plan whose one field is cleared
  later is not resurrected by a copy the accept left behind.
- **Progression is offered per prescribed set, inside the plan's rep range** (N74, amending N50). *Done*
  states one row per working set, and a set performed with its reps met at or under the exercise's one
  target RPE earns its own step whatever its siblings did. The range decides the direction: below the
  ceiling a set earns the **rep**, at the ceiling the **weight** and the climb restarts on the floor.
  Rejected: N50's single exercise-wide step on the last set, because a set that answered the plan earned
  nothing when a sibling missed. ([evidence](DECISIONS-EVIDENCE.md#n74))

- **A superset is a group of exercises performed in rounds** (N24). The model is a
  nullable `supersetGroup: Int?` ordinal on `session_exercises` and `template_exercises`; a
  separate `superset_groups` table would need its own ordering and a join on every read and
  expresses nothing more, while the ordinal survives reordering because `position` already
  carries the workout order. A group of one is not a group, so nothing is labelled.
- **Round semantics: the rest belongs to the round, not the set** (N24). After a set the
  app moves to the next member with no rest and starts the rest after the **last** one,
  otherwise pairing defeats itself; the rest is the group's longest member or the app
  default, and labels are giant-set notation: A1, A2, A3. **A member whose plan is used up is caught up**
  (B154): two members need not prescribe the same number of sets, and once the shorter one logged its last
  the round could never close again — so the rest silently stopped being armed for the longer member's
  remaining sets, with nothing on screen saying a round was holding it back.
- **A major release may start a new store instead of migrating one** (N104). 2.0 opens a **new database
  file** at schema 1 and never opens 1.x's, so there is nothing to migrate and nothing to truncate:
  `fallbackToDestructiveMigration` stays absent, because the old store is left **alone** rather than
  dropped, and the 1.18 build remains the way to read it. The migration chain, its exported schemas and its
  suite go with the store they belonged to — a schema file nobody migrates to is a contract with no
  counterparty. **The price is stated rather than hidden**: the app cannot read its own 1.x history, both
  transfer formats set a floor at 2.0 and refuse an older file *before* the decoder so the refusal reads
  "written by 1.x" rather than "corrupt", and **"delete everything" deletes the 1.x file too**, because user
  data must not survive a clear in a file no screen mentions. It is affordable **only** while the only log in
  existence is the author's; the first time another person's history exists, the same change is a migration
  by the book.

- **A new column joins the backup codec in the same change, and the codec is guarded by a
  round trip** (N24's preparation). The hand-written codec omits a column it does not know
  without erroring — three silent losses so far (N9, N15, N16) — so
  `BackupCodecRoundTripTest` asserts every field of every backed-up entity survives
  entity → DTO → entity, with no exclusion left: a session's runtime `restEndsAt` is not a field
  of the file at all (N109).
  ([evidence](DECISIONS-EVIDENCE.md#n24-codec))

- **A transfer file's version is read before its body is decoded** (B62). Both codecs used to decode the
  document and only then compare its version, so a file from a newer build failed *inside* the decoder —
  on an enum name this build has no constant for, which is exactly the data a version gate exists for —
  and the lifter was told their file was corrupt when its only fault was being new. The gate reads the
  version out of the JSON tree first, then hands the element to the decoder, so "newer" and "corrupt" are
  two answers rather than one. The rule that a bump is needed when a newer file could carry data an older
  build cannot represent applies to **every** format, not only the backup: the program document's version
  moved to 2 in the same change, having been left at 1 when the split muscle names and `SetType.CLUSTER`
  began travelling in it.
  ([evidence](DECISIONS-EVIDENCE.md#b59-b72))

- **A transfer file's version moves for a change of meaning, not for a defaulted addition** (N134, N137,
  N141). The codec tolerates unknown keys only because the version is read first (B62), so what protects a
  downlevel build is the version and the shims, not the decoder. `versionOf`, `goalKinds` and `activity` were
  all added without moving it, and a null default reads as "not a version", "no kind" and "a workout" — what
  those rows already meant. `goalPeriods` is read by an older build through the `goals`/`goalKinds` maps the
  file still writes, so nothing is lost there. **`activity` is the one with no shim**: an older build has no
  constant for the name, and `ignoreUnknownKeys` means it decodes a swim as an empty workout rather than
  refusing the file. That is the accepted cost — refusing a whole backup to protect one defaulted field is
  worse, and a file is restored into the same build or a newer one — but a field whose absence would
  **mislead** rather than merely be absent moves the version instead.
  ([evidence](DECISIONS-EVIDENCE.md#n141))

- **A migration is amended only while its version has never shipped — and the cost is
  real** (B16). Room refuses a database whose stored identity hash no longer matches, so a
  device that ran the amended version must be wiped; the alternative, a 16→17 migration, is
  correct but adds a version step to prove for data that exists only on developer machines.
  ([evidence](DECISIONS-EVIDENCE.md#b16))

- **A write is judged on what it creates, not on what is already stored** (B59). The plan boundary
  re-checked every rung rule over all of an exercise's stored rows, so a plan written before those rules
  existed — a drop run whose value column arrived null — was refused the moment anything in it was
  touched, and could be neither read nor corrected. The comparison is per set id and per problem: an
  untouched row is left as wrong as it was, a new row is judged on its own, and a write that makes an
  existing row worse, or moves a problem to another row, is still refused. The alternative — a migration
  that backfills a value — invents a number the lifter never gave and silently moves the whole ladder, so
  **a nullable column with a defined reading beats a plausible default**.
  ([evidence](DECISIONS-EVIDENCE.md#b59-b72))

- **The rest cue stays inside the permission-free envelope** (N27). `Vibrator` needs
  `VIBRATE`, so the cue is view-level haptics plus an in-process tone, and keep-screen-on
  is a window flag; a stronger cue is a permission, to be taken deliberately rather than as
  a side effect. ([evidence](DECISIONS-EVIDENCE.md#n27))

- **A removed feature still cleans up after itself** (B37, B40). The pre-N26 notification
  channel is deleted on every launch by one idempotent, permission-free call; documenting
  it as accepted instead leaves a trace of a deleted feature on a user's device to save
  four lines. ([evidence](DECISIONS-EVIDENCE.md#b37-and-b40))

- **A measurement is one entry per day, edited rather than added to** (N32, amended by N113 to one per
  **kind**). Several per day makes "what did I weigh today" a question with more than one answer, and a
  second reading is nearly always a correction of the first; the day is the local day it was taken, which
  is N25's rule for a session's time. Since N113 the record is two records — a daily one and a tape one —
  so a day holds one of each, and a tape day edits its own row rather than the one the scale wrote.
  ([evidence](DECISIONS-EVIDENCE.md#n32))
- **The body's record is two records, and soreness belongs to the day** (N113). A *daily* entry holds what
  a day says about the body — soreness, weight, body fat, muscle — and a *tape* entry holds the sites. They
  are separate because they are taken on different days and ask different questions, which is what one form
  could not be: **the weight stopped being required**, because a soreness-only morning is a real entry, and
  the form asks for what its own kind states rather than for everything (`statesSomething` is the rule
  *Save* reads). **Soreness is here rather than on the session** (N62's list, moved): it is a fact about the
  body's recovery, and a session could carry it only on a day that was trained — the rest day, when it is
  the thing worth writing down, had nowhere to put it. The rows an older session recorded stay on it and
  keep reading in history. **Both kinds ride one table and one export list**, each row carrying only its own
  kind's numbers, so the statistics screen's body and tape series keep reading one list — and the transfer
  format moved to 5 with its floor, because a weight that may be absent is a field whose meaning changed
  rather than one that was added.
- **An unmeasured tape site stays blank** (N32) — carrying a value forward is
  indistinguishable from a measurement and draws a flat line through a site nobody
  measured.

- **A rating is read back one site at a time, and the site is the joint *and* the side** (N130). The trend
  used to be one number per workout, so a rating that records **which joint, which side, how much** (N63)
  was reduced to an average about no particular joint: "left knee 6, right knee 2" charted as a 4, which is
  neither knee. The picker now offers a series **per site** — *Left knee* and *Right knee* are two, and a
  central joint is one — which is N118's answer for soreness and for the same two reasons: a per-metric
  filter would have to be answered twice, at the workout level and again per lift, and the stored choice
  stays one id. **Both of those levels get it**, because the average dropped the joint in both: the workout
  series reads a whole session's ratings for that site, and the per-lift one reads that lift's own points
  (N17). **Two series can now share a label**, so the picker carries a heading per group — the trigger shows
  the label alone, and without headings the two *Left knee* entries were indistinguishable before and after
  choosing. A site a session did not name is **null, not zero and not the overall worst**: a knee nobody
  mentioned has no reading, and a zero would draw one. Each site reads the session's **worst** score for it
  rather than a mean over the movements that reported it — N63's rule one level up, because three exercises
  that hurt the same knee are one sore knee, and averaging reports it as milder than any rating given. The
  overall *Joint pain* series stays: it answers "did this session hurt", and the sites answer *where*.
  ([evidence](DECISIONS-EVIDENCE.md#n130))

- **CI runs nightly and before a release, not on every push** (N30). The emulator is the
  one piece of infrastructure that has failed without a test running, so a per-push run
  would mostly report on the runner; the per-change guard is the local gate set in
  AGENTS.md, and the nightly run catches drift a local run cannot see. A release dispatches
  the pipeline first ([RELEASING.md](RELEASING.md) step 5), and the concurrency group
  carries the event name so a release dispatch and the nightly run cannot cancel each
  other. ([evidence](DECISIONS-EVIDENCE.md#n30-ci))

- **The emulator runs with VM acceleration, and that is what the flakiness was** (N30).
  Four different infrastructure failures shared one cause: the runner user is not in the
  `kvm` group, so the emulator fell back to software emulation. A udev rule grants the
  group access and `-accel auto` takes it. The API-level trade is split by trigger rather
  than paid every run: the nightly keeps the fast `aosp_atd` image at API 34, and the
  pre-release dispatch runs the shipping API, where an API 35+ behaviour change can show up.
  ([evidence](DECISIONS-EVIDENCE.md#n30-emulator))

- **A series is described once, in the metric registry** (N35). Labels, units, formatters,
  groups, bars-or-line, axis-at-zero and better-direction all live in one place, because
  twenty-one series across three screens and three query shapes is what made "show
  everything" expensive. The existing enums stay and the registry references them —
  `TrendMetric` is what a workout's ratings are read through — since replacing them would
  be a rename dressed as a refactor.
- **Arriving at Statistics with a lift selects estimated 1RM** (N35). An Exercise series
  means nothing until a lift is chosen, and of the eight exercise metrics 1RM is the one
  that answers "how is my bench going" — the heaviest set ignores reps, and volume rewards
  a long session over a strong one.

- **A gap is distance, and the line breaks at it** (N37). On a time axis a straight
  segment across missing weeks is a claim about weeks nobody measured; this is where the
  app deliberately differs from the tool whose shape it borrows.
- **The moving average's period counts readings, not days** (N40). A seven-day window for
  a lift trained twice a week would often average one reading, and a window that is
  sometimes empty is worse than one whose unit is stated on the control.

- **Test tags are exposed as resource ids** (N38–N39). A device-side tool addresses a
  control by *identity* rather than coordinates, so a tap lands on the control or fails
  instead of hitting its neighbour; the cost is that tags become visible to accessibility
  tooling, which is what an annotation meant for tooling is for.
  ([evidence](DECISIONS-EVIDENCE.md#n38))

## The workout screen

- **A workout with something in it is discarded behind a prompt, and the prompt says what it costs**
  (N41). **The empty workout keeps its prompt-free discard** because there is nothing to lose, and it lives
  in the same overflow as every other session's: the way out is in one place whatever the workout holds (see
  N87). **An activity is never "the empty workout"** (N143): it holds no sets by construction, so `isEmpty` is
  true for exactly the session a lifter deliberately recorded — and an unconfirmed tap deleted the day with no
  undo while the same action on a workout with one set asked first. The prompt-free branch asks *did nothing
  happen*, not *does it hold sets*, and an activity is the case that tells the two apart. A workout
  holding sets asks first and names the count, and when it was started from a program slot it says
  the second consequence: only a *finished* session settles an occurrence (P3.5), so dropping out is
  a miss rather than no workout at all. The action sits behind the top bar's overflow rather than
  beside *Finish*, which is the control a lifter reaches for mid-set. An undo was rejected: the
  session is soft-deleted, but restoring one would have to un-settle nothing and re-open a workout
  the user asked to be gone. ([evidence](DECISIONS-EVIDENCE.md#n41))
- **The rest timer can be switched off, and off means it never runs** (N44, extending N21 and N27).
  A rest is still shown — the exercise's own prescription as a fixed label, falling back to the
  default rest — with no countdown, no ±15s and no chime. The end instant is simply never written
  while the switch is off, and a rest already running is cleared when it goes off; hiding a live
  countdown behind a static number would be the timer still running, which is the reading this
  rejects. It is a preference about the timer, not a fact about the exercise, so it stays distinct
  from a prescribed rest of zero. ([evidence](DECISIONS-EVIDENCE.md#n44))
- **The progression question is a preference, and it is on until it is turned off** (N66, extending
  N50). *Done* still offers the next step a plan earned, because that is what the app has done since
  N50 and a preference that arrives off would silently remove a feature. It is off when the lifter
  says so, and off means the exercise simply finishes: the prompt exists to change the *plan*, so
  somebody who does not want the app editing the plan is not asked about it. Hiding the prompt but
  still writing the step was rejected — the write is the only thing the question is for.
- **A warm-up carries no effort** (N67). It is preparation rather than work, so an RPE on it measures
  nothing the plan asked of it and reads as a number the set was judged against. The rule is a
  property of the role (`SetType.recordsEffort`) rather than a check repeated at each caller, because
  the editor, the write boundary and the row all have to agree about what a warm-up is. The field is
  **absent** for a warm-up rather than disabled: a control that cannot write invites a tap that does
  nothing. Migration 28→29 clears the numbers already stored — logged and planned alike — while the
  plan's exercise-level target stays, because it belongs to the exercise's working sets.
- **A group is rated, judged and rested once** (N79, extending N67). A rung is work, so it is not
  excluded from the app the way a warm-up is, but it is **not rated on its own**: the rating lives on
  the anchor, and `recordsEffort` is the one rule that hides the field, nulls the value at every write
  boundary and keeps it out of a backup. The group is judged on its first set, a rung cut short does not
  hold it back, and the rest waits for the run to close. Rejected: a per-rung rating, because the group
  is one effort. ([evidence](DECISIONS-EVIDENCE.md#n79))
- **The workout's overflow is the logger's own business, and the templates list withholds the start**
  (N78, narrowed by N87). The tab bar stays off the logger (N34), and N87 leaves the overflow holding
  only *Discard* mid-workout because the two plan screens already live on home's start bar, which stays
  visible while a session is open. Starting is idempotent, so the templates list asks whether a session
  is open and disables *Start* when one is. Rejected: making Templates a tab, or showing the bar
  mid-workout. ([evidence](DECISIONS-EVIDENCE.md#n78); N87's reversal is argued at
  [its own section](DECISIONS-EVIDENCE.md#n87))
- **A start with a workout already running asks rather than resumes** (N89). N78 stopped the silent
  hand-back in the templates list by disabling *Start*; home's own start actions had no guard, so tapping
  *Start planned workout* on a next-up row dropped the plan and opened the running workout without a word.
  The question is asked instead — *Continue workout*, or *Discard and start new* — because home is where the
  lifter is choosing what to do, and disabling the control would take the choice away rather than offer it.
  Discarding is the workout screen's own discard, the same soft delete, after which a scheduled occurrence
  reads as a miss (P3.5); the question is asked **after** the missed-day one (P3.3), so dismissing that
  cannot leave a lifter with a discarded session and nothing started, and dismissing this one cancels the
  start rather than choosing for them. The templates list keeps its disabled *Start*: a different screen,
  and a dialog behind a control that cannot be pressed would never be reached.
- **An exercise is finished at its foot, and only once it has a set** (N69). *Done* says the work is
  over, so it is withheld until there is work: an exercise with nothing logged offers no action at all,
  and the way past a movement you did not do is the overflow's *Remove*, which keeps its confirmation.
  The action sits below the sets and the rating rather than in the header, where it read as part of the
  title instead of the last thing about the exercise. *Reopen* keeps the same place on a done one, so
  one position answers "what is this exercise's state" rather than two. **N76 puts the action at the
  end of the rating row** rather than under it — see the bullet below — and everything else here stands.
- **The action and the rating are one closing row** (N76). "The exercise is over" and "how did that
  feel" answer one question, so *Done* sits at the end of the rating row and inside its band instead of
  eight points under it: two controls read as a field with a button near it, which is what the foot
  used to say. The action keeps N69's place and its tags, and the rating row gives up the half of the
  width the action needs — its click target covered the whole row, and a full-width one would swallow
  the button beside it. Moving the action up beside the header was rejected for N53's reason: the
  header is read constantly mid-session.
- **The plan's line states the remainder, and the finished sentence only at zero** (N70). N52's notice
  could speak only once the plan was complete, which left the sets still to write — the part of the
  session a count is for — with nothing to read. The remainder and the *Log extra set* label are
  computed from **one** rule, so the line cannot say work is done while the button still promises the
  plan's next set; a null plan stays null rather than reading zero, because "nothing was planned" and
  "all of it is done" are different statements. **What that rule counts is the working sets on both
  sides** (N112): a plan's ramp is preparation the plan did not ask to be finished, so four warm-ups and
  three working sets are a plan of three — which is how the plan is compared with the work everywhere
  else too (N20).
- **A set's number, and the plan's remainder, count only the sets that stand on their own** (N112). The
  three lists that number sets — the plan's own, the workout's logged rows and the history detail's —
  read one rule, because they are three readings of one sequence: a **warm-up takes no number**, so an
  exercise's first working set is 1 whatever its ramp did, and a ramp of five no longer pushes the work
  to 6. The remainder follows the same rule on both sides, and a **rung does take a number**, because a
  run is work the plan prescribes and the lifter performs even though it is not rated on its own (N79).
  The rule lives on the role, beside `recordsEffort` and deliberately not the same one: that asks what
  may be recorded, this asks what the plan asked for.
- **An exercise's rare actions are one menu, and removing one always asks** (N71, extending N53 and
  B2). The workout and the template editor draw the same component, because a second screen carrying
  a near-copy of a menu is how two vocabularies for one idea start. Each entry is offered only where
  it exists — a direction with nowhere to go is absent rather than present-and-inert, and the first
  row has no pairing entry (B28) — and the destructive one is last, coloured, and behind a
  confirmation whose sentence the screen supplies: what the sets leave is a workout or a template, but
  the guard is the same one, because a soft delete with no undo reachable from the UI is not a tap.

- **A destination belongs in the action row, and a heading does not carry a way out of itself**
  (N42). Programs moved from the home overflow into the pair above the start pill, and "See all
  workouts" left the *Recent* heading: a control that leaves a section duplicates the tab bar, and
  keeping both entry points "just in case" is what made the tab a second path to everywhere else.
  **The heading itself has since gone with the list it headed** (N102), which is this rule taken to its
  end: a section that no longer exists cannot carry a way out of itself.
  ([evidence](DECISIONS-EVIDENCE.md#n42))
- **A repeat is addressed to a workout, and a row's second action is an icon** (N48, amending N29).
  The one-tap copy of exercises and order is the same; what changed is that it names its source, so
  a History row repeats *that* workout rather than whatever is newest, and the repository reports a
  row deleted since rather than opening a blank session. The action sits in the row's trailing slot
  beside the open, because a row is one thing you tap and an action on it is an icon; and because a
  repeat is a start, it still goes through P3.3's missed-day question.
  ([evidence](DECISIONS-EVIDENCE.md#n48))

- **Home's body is the ways in, and the bottom bar is what is next** (N102, narrowed by N124 and N125). A body
  of four ways in is never blank, which is why the first-run message went with the list it explained. The bar
  carries the *Resume* pill — only while a workout is open, and nothing else: the next-up block it once held
  went into the body (N124) and then out of the app altogether (N125) — so **the bar is never a place to
  start**. Starting empty moved into the body rather than being duplicated there, because the same action
  twice on one screen is what N42 removed the last time it happened.

- **The day's block leads when it holds no workout, and the day's record row is never absent** (B182). The
  ways-in sit above a *scheduled* day because that day's work belongs under the thumb (N115, N124) — but with
  nothing scheduled there is no work to put there, and four full-width cards pushed the day's line and its
  record row off a compact screen, which read as the row going missing. So the block leads when the plan is
  empty and stays at the foot when it is not. The record row itself no longer disappears once the day is
  written: it opens **that entry** for editing, because the blank daily form's save replaces the day through
  `findOnDay` — hiding the row was the safer rule while a blank form was the only thing it could open.

## The app's data

- **An action on the whole database lives in Settings** (N43). Export, import and
  delete-everything had moved from the library to the home overflow (B1) on the argument that home
  is "where you start"; that placed them beside *Start workout*, which is an action on one workout
  rather than on the database. Settings is about the app, so they are rows there, in the order that
  matters: export, import, and the one that cannot be undone last and coloured.
  ([evidence](DECISIONS-EVIDENCE.md#n43))

- **A statistic may state the energy implication of a weight trend, and never a calorie target** (N98). Body
  weight is recorded here, so the statistics screen can answer what a scale implies: a target **rate**, the
  fitted trend, and the gap between them as a daily figure. **It stores no food and no intake, and it never
  will** — the nutrition non-goal is a line rather than a backlog — so what it gives is a *relative* change
  ("about 200 kcal a day less than you are eating now") and the absolute target stays where the food is logged.
  **The band is measured, never assumed**: it is the fitted line's own standard error, null below three
  readings because a line through two points passes through both and every residual is zero. **The statement
  reads the whole weight series**: the chart's range is a display setting and its seven-day default is below the
  floor, so a verdict that used it could never speak (B141's rule). **Nothing is said until there is enough to
  say it** — eight readings across twenty-one days — and below that the screen says so
  rather than drawing a figure from noise, which is P2.8's bar. **Nothing is written**: the statement sets no
  setting, no plan and no target, which is the app's oldest rule reaching a second surface.

- **A weight goal sets the rate, instead of the rate being typed** (N135, revising N98's stored rate). "−0.35 kg
  a week" used to be a number the lifter authored beside a target, which is two answers to one question — and the
  pair could disagree, which is what the change fixes. Now the goal states a **kind**: a target weight, a
  direction with no target, or a weight to hold. The kind is what is stored, in a **sibling map** to the goals
  under the same key (`MetricKey.id`), because the goals map is one number per metric and is already in files
  this app has written; the kind travels in the export beside it. A goal written before kinds existed — a value
  with no kind — reads as a **target**, because that is exactly what a level goal was; a kind this build does not
  know is no goal rather than a guess (the enum-by-name rule). A maintenance goal asks for **no rate at all**,
  because its answer is a corridor. ([evidence](DECISIONS-EVIDENCE.md#n135))

- **Each direction has its own band and target, and a direction-only goal invents no target** (N135, amended by
  N149). The bands are **shares** rather than numbers, so the same rule reads correctly for a 50 kg lifter and a
  120 kg one: a **gain** is held to **0.25–0.5% of bodyweight a week** and asks **0.35%**; a **loss** to
  **0.5–0.8%** and asks **0.65%**, because gaining lean mass is the slower direction. The target is **clamped into
  its own band**, so no future change to how it is derived can leave the range that says neither too fast nor too
  slow. A **target** takes its direction from where the lifter is now; a **direction** takes the direction the
  lifter stated and never manufactures an end weight; **maintenance is a corridor**, ±1 kg around the stated
  weight, because a literal 0.0 kg/week is broken by the first glass of water and "hold within ±X" is what makes
  it a goal. A rate is **never derived from a deadline**: if a date is wanted, the band's rate gives the *estimate*
  of the date, not the other way round — and the statistics row now shows that estimate for a target (N151),
  saying *Target reached* once the weight is within the goal history's own tolerance. The rate is **derived,
  never typed**, so the statistics row shows it and
  the band it is judged against rather than offering it for editing — rejected: keeping the two side by side,
  which is two sources that can disagree, the thing being fixed.

- **A weigh-in answers whether the rate is the goal's, where the weighing happens** (N136). The energy sentence
  already existed under the statistics chart, and a lifter had to go and look for it; the same arithmetic is now
  asked at the moment a weight is saved. **The prompt is unsolicited, so it is held to a higher bar than the
  chart**: it checks N98's floor itself — 8 recorded readings across 21 days — rather than leaning on the
  statement's arithmetic, because a prompt that fires on water weight is worse than no prompt, and below the
  floor it says **nothing at all** rather than the chart's "not enough weigh-ins yet", which is an answer to a
  question the lifter asked. **No goal means no line**, which is the one prompt this app must never grow: the
  direction is the lifter's, and stating one they did not set is a diagnosis rather than arithmetic against what
  they asked for. It is **a line after the save, not a dialog**: it states, it is dismissible, it does not
  pursue (P4.7), and it is never a number to eat — the relative sentence is the only honest one without the
  intake store (N98). It is set by the save rather than derived from the data, because a value read from the
  flows would be drawn again on every return to the screen, which is the pursuit it must not have.
  ([evidence](DECISIONS-EVIDENCE.md#n136))

- **A goal is rows with periods, not a setting** (N137). The store was a `metricId=value` string rewritten on
  every change, which meant the past did not exist: changing a goal re-judged every earlier range, and the
  chart drew the current target across it. A goal is now a **row per period** in `metric_goals`, the newest row
  per metric being the one that is open — so a change opens a period, and **clearing opens a period of no goal**
  rather than deleting a row, because the answer to "what was I asking for until today" is the thing being kept.
  A row that asks for exactly what the open one asks for opens nothing, so the history is changes rather than
  button presses. **The first row's start is unknown, never invented**: the migration cannot know when the goal
  already on the device was set, and stamping it with the upgrade's own moment would be inventing history, which
  is what periods exist to stop. Only goals move into the database — the other settings are preferences about
  how the app behaves, not facts about a body — and with them goes the import path's old seam, since a goal is
  now a row that merges like every other. The **derived rate is still not stored** (N135): a period re-derives
  under today's rule, and the honest reading of an old period is "by today's rule, March was X".

  **Each period says whether it was met**, and what "met" means is the kind's (N137): a **target** is met when a
  reading in the period reached it within the corridor's tolerance — a fact about a reading, so it needs no
  trend at all; a **rate** is met when the fitted trend sat in N135's band *and* on the side the lifter asked for,
  which needs a fit and therefore N98's floor, because a verdict about the past must not be a rate from three
  weigh-ins either; **maintenance** is met when the weight stayed in its corridor, judged on the **fitted line**
  so a day of water is absorbed rather than counted against the period. A period nobody weighed in during is
  **unjudged**, which is its own answer rather than a miss — the app does not invent a verdict out of no data,
  and a period of no goal is neither met nor missed. **The chart draws one line per period**, each across the
  stretch it applied to, because one line across the whole width is the claim about ranges it never applied to
  that this feature exists to stop; a direction-only goal draws no line, since it states no weight and a level
  invented from the band would be the end weight N135 refuses to manufacture.


- **A plan's target is drawn on a lift's chart, and the plan is named** (N39). A plan already states what it asks
  for — a weight, an assistance, a rep range and an effort per set — and the chart for a lift now draws it beside
  what was actually done. **Every live plan that names the lift draws its own line**, each named under the chart,
  rather than one plan winning: a lift can sit in two plans at once and more than one program may be active, so a
  rule that picked one would be choosing a prescription the lifter did not — the quiet error this feature exists
  to avoid. Rejected: the active program's plan, which needs a second rule for two active programs disagreeing,
  and the newest plan, for which "newest" is not "the one I am following". **Live** means what the Templates list
  shows: a **kept version** is history rather than a prescription (N134), and a soft-deleted set was taken out of
  the plan, so neither draws. **Only the level-shaped targets draw** — a weight, an estimated one-rep max from
  that weight and its rep target, an assistance, an effort — because a rep range has no single level and a total
  is a sum of a prescription rather than a line in it; inventing one would be the "plausible rather than true"
  this app keeps refusing. The lifter's **own goal stays a different line**, dotted where a plan's is dashed and
  told apart by name under the chart, because the two mean different things: what they asked themselves for, and
  what their plan asks of them.

- **An activity is a session that takes no sets** (N141). Cycling, a swim, a climb: it answers "was the day
  trained" without claiming any work was done, so it is **the session row with an `activity` name on it** rather
  than a record type of its own — the day, the history, the adherence calendar and the export all already read a
  session, and a second kind of record would be a second path through every one of them. That costs one nullable
  column and a migration, and it is why an activity needs no new rule in the statistics: volume, reps and records
  read the sets an activity does not have. **The list is short and closed** — five activities, stored by name
  like every enum — because the request was "don't put too many options", and an open field would be a second
  vocabulary for the same few things. **The activity is offered only while the session holds nothing**, and once
  it is set the screen draws no exercise list and no add rows: an activity is not a workout with a different
  label, and the two are never mixed. A day whose only work was an activity is drawn in its own colour in the
  calendar (N142), and a day with **a workout and** an activity is still a workout day — the calendar's question
  is whether the day was trained. **History names it on both surfaces** (B167): the list row takes the activity
  as its label and drops the meaningless set count, and the detail's title is the activity with its duration,
  not *0 sets · 0 kg*.

## Measurements

- **The measurements list scrolls, and its empty sentence is an item of it** (B130). It was a `Column` with
  `fillMaxSize()` and a `forEach`, which is the one list in the app that predates the pattern every other
  screen has: the first screenful was all there was, so a lifter with a few weeks of daily entries could not
  see, edit or delete anything older, and the add button floated over whichever row happened to be last. It
  is a `LazyColumn` with the empty sentence as a **keyed item** rather than a sibling above the rows — an
  empty list has no rows to hang a sentence off, and the two states are mutually exclusive anyway.
  ([evidence](DECISIONS-EVIDENCE.md#b130-n132-n133))

- **Changing or deleting a measurement is a mode, not a button on every row** (N132; the correcting half is a
  request in use, later than the id). A measurement is the one thing here that cannot be re-derived: a set can
  be logged again and a plan re-authored, but a weight on a morning is a fact about the body. So the default
  view is what the screen is opened for — **scroll and add** — and the writes live behind one explicit toggle
  that **says which mode the list is in** and leaves again, while the add button is out of the way for as long
  as it is on. What did not travel with it: the add path with its two kinds (N113) and the empty sentence.
  **Inside the mode a row opens its form on the record it names**, so a number that was left out can be added
  and one that is wrong can be changed — the question this id had put aside as a different one, answered where
  the writes already live rather than behind a second mode. The draft keeps the entry's **own day and id**,
  because the record being corrected is the one that was tapped, so an edit cannot land as a second entry on
  the day. Rejected: a confirmation dialog on the existing per-row button. That is the guard for an act with
  **no way back at all** (deleting a workout, a template, an exercise), and it answers the wrong problem — the
  complaint is a delete button on every row of a reading screen, and a dialog leaves it exactly there while
  adding an interruption to every deliberate one.

- **A deleted measurement comes back through the soft delete, unless the day was re-entered** (N133). The
  delete already set `deletedAt` on the entry and on its sore rows, so the way back is clearing that field
  rather than re-inserting anything — N90's mechanism, and its correlation rule with it: `deletedAt` is the
  identity of the write that hid the row, so an undo reaches **its own** removal and cannot resurrect an
  entry deleted deliberately a moment earlier. The shape on screen is N7's: a snackbar naming what went, with
  *Undo*, held by the ViewModel until the message is taken and offered one at a time. What is *not* N90's
  shape is the collision, because a measurement's identity is **its day and its kind** (N32, N113): if the
  day has been re-entered since, the restore would have to overwrite a newer reading the lifter wrote, so it
  is **refused with a sentence** (`DataError.Invalid`) instead. "Re-entered" is read from `createdAt`, not
  `updatedAt` — an edit moves the latter without the day ever being re-entered.
  ([evidence](DECISIONS-EVIDENCE.md#b130-n132-n133))

## Programs

- **A slot's row acts through one menu, and its template is read-only from there** (N72, extending
  N53 and N71). The row carried two arrows and a delete icon, and the delete fired on the tap; one ⋮
  is the shape the workout and the template editor already draw, so the third screen is the same
  control rather than a fourth arrangement of it. Each direction is offered only where it exists, and
  a removal asks first and says both halves — the day leaves the schedule, the workout it pointed at
  and everything logged with it stay. Tapping the day's **name** opens what the template trains,
  read-only: a program uses its template (N73), so the question "what does this day do" is worth
  answering in place, but a second place to **edit** a plan is what "uses the template" rules out.
  The sets are drawn with the template editor's own line, promoted to `internal` rather than written
  again, because two formatters for one plan is how two readings of it start.
- **A program is a schedule over templates; a slot is a template plus an optional weekday, and it
  prescribes nothing of its own** (N73, reversing P3.8). P3.8 let a slot override its template — its
  sets, its rest, its cue and its target effort — so two slots naming one workout could train it
  differently, and progression wrote *the slot* for a program start. The lifter's decision is that a
  program always **uses** what the template says: there is one plan, it is the template's, and
  accepting a progression step writes it. Two slots naming one template therefore share its plan
  **and** its progression, which is the point rather than a loss. The two override tables are dropped
  and their contents discarded on upgrade; templates, planned sets and every logged workout stay. The
  **prefill history** is still the slot's own (`slotPreviousPerformance`), because that is what the
  lifter did rather than what the plan says.
- **A program orders templates; a slot is a template plus an optional weekday** (P3.3). A weekday says
  what happens on a Tuesday, but nothing orders the weekdays against each other, so "which one is next"
  would have no answer; the slot is that order. A weekday-less slot is order-only and is never "missed"
  — it has no day to miss. Today's plan comes from the slot pinned to today, and **with no active
  program there is no *Today* list** (N56), because a template carries no day.
- **A skipped occurrence is an event keyed by slot and week** (P3.3). `program_skips` stores the
  week's Monday as an epoch day, never a boolean on the slot: the same weekday recurs, so a flag
  would need resetting and would be wrong the moment two weeks in a row were missed. The
  rejected alternative — a `skipped` column — cannot answer P3.5 either, because a standing
  weekday pin carries no history. ([evidence](DECISIONS-EVIDENCE.md#p33))
- **The missed-day question is asked at the point of starting, never at launch** (P3.3). An app
  that questions you when you open it is one you stop opening. *Do it now* starts the missed
  slot; *Continue* records a skip for **every** pending occurrence this week in one write,
  because asking again for the next one turns two misses into two interrogations. Only days
  strictly before today are pending: recording a skip for a Friday on a Wednesday would be the
  app inventing a decision the user has not taken.
- **A session records the template it was started from, and only when it is created** (P3.3).
  One nullable `templateId` on `workout_sessions`, written at the moment the session opens, so a
  resumed session never rewrites it. This **amends N16 deliberately**: N16 rejected copying a
  plan's *targets* onto a session because that freezes what the plan prescribes; recording
  *where a session came from* freezes nothing, so the template stays living. It is provenance,
  not prescription. ([evidence](DECISIONS-EVIDENCE.md#p33))
- **An occurrence is matched by template and date, in a Monday-start week taken in the session's
  own zone** (P3.3, extending N25). With one candidate slot it resolves; with several, an exact
  weekday match wins, then the latest slot earlier in the week (done late), then the earliest
  after it (done early), then the earliest unresolved. A session resolves at most one
  occurrence, and an occurrence is resolved by at most one session — the first. Matching by
  exercises was rejected: it breaks the moment a template is edited and cannot tell two slots
  apart. ([evidence](DECISIONS-EVIDENCE.md#p33))
- **More than one program may be active, and the flag is a plain flag on the row** (P3.12,
  amending P3.3). Home's today plan, the missed-day question and adherence all read the **union**
  of the active programs' slots, so a lifting block and a conditioning one run at once. P3.3 set
  `isActive` in one transaction that cleared the others, which made "at most one" true by
  construction; that rule is deliberately gone, because forbidding two schedules left editing one
  program to hold both as the only alternative, and the union is the smaller change. A program is
  still training data and rides in the backup, so the answer stays on the row rather than
  splitting into a preference.
- **A program's place is authored** (P3.12). `programs.position` orders the list and the union,
  moved by hand rather than derived from a name or a creation time, so which active program comes
  first is a decision the lifter made. The rows already on disk take their `rowid` as the
  position, which is the only order they carried; a name tie-break would have reshuffled them
  the moment one was moved.
- **The slot travels as a start argument, never as a stored fact** (P3.8, narrowed by N73). Home knows
  which slot it is starting, so the route carries the slot id and the session's own history reads it for
  the prefill (N73); the session still records only the template (P3.3), so provenance and occurrence
  matching are unchanged. Storing it would make the session's provenance say two things and would go
  stale the moment the slot moved.
- **A program's order is a run, derived from what was done rather than a stored cursor** (P3.9).
  The next slot follows the last slot trained or consciously skipped; a day simply missed leaves the
  run where it is, which is what the missed-day question is for, and an edited program re-derives
  its place rather than leaving a pointer at a slot that is gone. A program with **no weekdays**
  still runs A → B → C, because a cursor rather than a day attributes an order-only session to its
  slot. **Home does not offer the run** — the next-up row that used to is gone (N125), because a day's
  workout is the question home is for and the run is an answer no part of that screen can act on; the
  Programs screen is where a run is read, and the editor
  marks the place. **Rotation is by what was done, never by load**: choosing the next template from
  fatigue, soreness or accumulated load was rejected, because nothing the app records measures
  recovery and a rotation whose reason the lifter cannot see is a coach, not a log (N22's rule).
- **A deload is a week the lifter marks, never one the app computes** (P3.10). It is an event keyed
  by program and week, the shape of a skip, so it records a decision about how the block is going
  rather than being a dated plan (N16) or a derived week. **Exempt from the ratio, not from the
  calendar**: a deload week's scheduled occurrences are neither done, skipped nor missed, so a
  deliberate back-off cannot read as a failure, while its sessions still mark their days. A built plan's own
  days are the calendar's schedule (B161), and the missed-day question does not ask about a built week (B163):
  a deload day has no slot a skip could record, so the week is left out of that question; a marked week with no
  plan keeps its slots and still asks. **Nothing is scaled for you by the app** — a week marked
  with no plan prescribes what the slot's template prescribes (N73), and N147's creator writes a draft only
  when the lifter asks for it — and **the creator plans the next week** (N158): the mark is still the lifter's,
  but the one-week forward window replaces the old no-forward-view rule, because on a Sunday the old rule built a
  week that had already passed. The repository accepts the current or next week and refuses anything further; a
  built plan's days are all future, and the calendar and home read it when the week arrives.
- **A deload week can prescribe its own workouts, and the app drafts them only when asked** (N147,
  narrowing P3.10). The mark is still the lifter's; what the creator adds is a **plan for that week**: one
  ordinary template per day and a `program_deload_days` row keyed to the mark, so a two-day deload can be
  two days while the program has four. The draft is one visible rule — the plan's stand-alone working sets
  kept up to three, its highest rep target floored at five, its heaviest stated load kept at 60% (50/70
  offered) or the last finished session's where the plan states none, rounded to the exercise's step; no
  warm-up, no rung, no effort target — and the screen's preview and the write read the same pure function.
  **Nothing scales by itself**: the creator writes only when called and the saved plan never recalculates;
  anything finer is a template edit. It applies to the **next week** (N158), so no deload day is in the past;
  the current week is still accepted for older plans, and the mark is written in the same transaction. Home's
  today plan and adherence's
  calendar and day dialog read the week's days instead of the slots; the missed-day question leaves a built week
  out (B163). A deload session names no slot, so the run pauses (P3.9), the week stays exempt from the ratio, and
  unmarking a built week asks first, leaving the templates in the library but not the week's plan (B162). The
  success card links to the workouts it wrote, and the adherence chip says a week is built (N148). **Reuse is the
  choices, not the numbers** (N150): a later plan can preselect the exercises the last one kept, matched by
  library id, and take its day count, then re-derives every load from the current plan; the **load share is not
  stored with a plan**, so reuse leaves it at the creator's current choice rather than guessing it; the last plan's
  day count and weekdays are taken by index (N154), and its exercises are read from the plan's current templates
  (N16, N155); a library id maps to the first current instance on reuse, because the stored plan keeps library ids.
  **The days are
  composed from the program's exercises, not copied from its templates** (N152): the days are headings and the
  program-wide pool is one list below them — one row per **source instance**, with that day's plan and name, and
  a chip per deload day on each row (N156) — and the day count is the
  deload's, not a prefix of the program; a movement on two source days is two rows, so the same exercise can be
  kept more than once (N157). The workout is named **Deload A, Deload B, …** in day order, so the built week reads as a week
  rather than as copies of the templates; the weekday still starts from the source day at that position, and the
  template editor renames a workout that now mixes upper and lower work. The store is
  **schema 7**; the **backup's `schemaVersion` stays at 5**, so an older build reading a newer file keeps the mark
  and the workouts but not the plan (B165). The plan travels in the backup as authored setup; N47's program
  document still carries neither it nor its workouts, because a week-keyed plan is history.
  ([evidence](DECISIONS-EVIDENCE.md#n147))
- **A substitute is an event keyed by slot and week, and it stands in for matching** (P3.11). The
  pick is made at the point of starting, like the missed-day question, and recorded for that slot
  and week before the session opens; editing the program instead would change every week that
  references the template (N16, inherited by P3.3). A session started from the substitute settles
  the slot's occurrence, so P3.3's matching is read with the substitution and the app stops asking
  about a day already trained, and adherence scores it against the slot — the day was scheduled and
  it was done, whatever it was done with. Clearing is always allowed, because a mis-pick would
  otherwise be permanent and the record is the lifter's statement rather than the app's.
- **A program's known limits are inherited, not new** (P3.3): an occurrence resolves only when
  the workout was started from that template, a second session from the same template in a week
  is unmatched, and editing a template changes every week that references it — N16's
  living-template decision, unchanged. P3.12 adds one more the union makes reachable: a session
  names only its template, so when two active programs put one template in a slot, that session
  settles an occurrence in **both** and advances both runs. Telling them apart would need the
  session to record the slot, which P3.8 deliberately did not do; it is recorded here so the
  overlap is a known limit rather than a surprise.
- **Superseded by N125: a substitute trained from a next-up row recorded nothing** (N85). The row is gone,
  and with it the whole path — `startSubstitute`, the second substitute callback, and the picker's
  conditional *Restore the scheduled workout* — so nothing offers an unrecorded substitute now: every
  *Substitute* belongs to a card, and a card *is* today's occurrence by construction. The entry is kept
  because the rules it settled are still P3.9's: a run advances past the template a session **trained**,
  and nothing here ever held it still, which an earlier version of this entry had claimed. A next-up row
  had no occurrence to key a substitution to — the run is calendar-free and `ProgramRun` carries no week —
  and the alternative then rejected (a session naming a template but settling no occurrence, needing the
  session to carry a second fact) stays rejected for the same reason.
- **A template exercise's block is folded until its name is opened** (N91). A plan's controls are the
  editor's job, but the *names* are what the screen is scanned by, and by N81 every block drew its plan
  lines, its rest/cue/RPE fields and *Add set* — five exercises made a long scroll of controls with the
  names lost among them. The row is therefore the whole of a block until its own name is tapped, and
  everything below it folds together. **The name is the control**, not a chevron beside it, because the
  name is what the lifter is looking for; the row's ⋮ is untouched, so opening a block is never a tap on a
  menu, and the label names the *action* while the state is announced beside it — the same name behaving
  differently depending on unseen state is the thing a screen reader cannot report for itself (B21's rule,
  applied to a fold). **The state is per exercise and `rememberSaveable`**, for N84's reason: this activity
  declares no `configChanges`, so a rotation would otherwise fold what the lifter opened. **A newly added
  exercise opens**, so its first set can be taken without a second tap, and only the blocks the plan already
  held start folded — the one exception being a first composition while the plan is still loading, which has
  no ids to tell apart and so opens them all. The state lives in `ui/components/RowFold.kt` rather than in
  the screen: it is a list of ids and a per-row flag, and the editor's own file and longest function are both
  at the length this project allows.
- **A removed planned set comes back at the position it held** (N90). The editor's set delete used to
  soft-delete the row and say nothing, so a mis-tap was unrecoverable; the workout's own answer is the shape
  it takes (N7, B3) — a snackbar naming what went, with *Undo*, held by the ViewModel until the message is
  taken — but **where it comes back is deliberately not the workout's answer**. The workout *appends*,
  because a log is read by what was done and "the values come back, the position may not" is the honest
  limit there. A plan is the other way round: **its order is the plan**. A set's `setIndex` is its position
  and a run's ladder is read by position (`runAt`, `rungWeightAt`, N79), so appending a restored rung would
  silently rewrite a drop ladder — a plan the lifter never wrote, produced by an undo. The soft delete is
  what makes the honest restore possible: the row and its index are still on disk, so the restore clears
  `deletedAt` and shifts the survivors down to make room, renumbering the exercise from there. **A run comes
  back whole**, because the rungs are not separate sets — a run whose anchor is gone reads as nothing — and
  `deletedAt` doubles as the identity of the one write that hid it, so an undo reaches its own removal and
  nothing else. That last part is load-bearing: without it, a restore that put back "every hidden row" would
  resurrect a set the lifter had deleted deliberately a moment earlier. Only one undo is offered at a time,
  as the workout's is, and it goes with the exercise that owns the set, so an Undo can never outlive the
  block it would restore into. **The box sits at the scaffold's foot**, not over the list, so the block's
  *Add set* — the control the delete lives beside — is never covered.
- **The active program rides the programs screen's bottom bar** (N92). The list is top-aligned and its only
  foot clearance was the *New program* FAB, so with a single program the row to open sat under the app bar —
  the least reachable place on a modern phone — while the action that makes *another* program was the thing
  in reach. The **active** program is drawn as a card in the scaffold's own `bottomBar`, so it is fixed at
  the foot and the list scrolls above it. **It is added to the list, never swapped with a row**: the list
  stays the reference it is, every program in the authored order (P3.12), and the card is the one being
  acted on. With several active programs it shows the first and the list carries the rest; with none there
  is no card, so neither case is rearranged to suit the single-program one. The card repeats what the row
  says — name, slot count, *In use* — because the two are the same fact read in two places rather than two
  facts; it offers no *Use*, since activating the program it is already showing would do nothing.
  **The FAB and the bar cannot collide**, which the scaffold settles by placing a floating action *above* a
  bottom bar. Putting the button into the bar beside the card was rejected — the two would have to share a
  row on the narrowest phones — and drawing the card *beside* the floating button was rejected too: it makes
  the card's width a function of the button's label and the font scale, and a number that drifts hides
  content rather than crowding it. The list keeps clearance for the button, now measured from one bar higher
  than before.
- **Programs and Templates link to each other from their own bars** (N93). They are siblings — home's action
  row opens both (N42) — and the only path from one to the other was back home and in, which is a trip
  between two views of the same thing: a plan and the schedule that orders it. Each bar carries the entry,
  sitting with the actions it already has (Programs' *Load*, N47) rather than floating over the list, because
  it is a place to go rather than the one action that screen makes. **It is a way across and not a third way
  to start anything**: it asks nothing about a session being open, and a running workout is still answered by
  the templates list's own disabled *Start* (N78, B43) rather than by this entry being hidden.
  ([evidence](DECISIONS-EVIDENCE.md#n93))
- **Superseded by N125: a next-up row stated the program and the count on lines of their own** (N88). The
  row is gone, so the shape it argued for has no control left to describe — and the *today* card, which the
  entry deliberately left alone, is the only card standing. Kept for the reason it was written: the split
  was **the row's shape, not a wrap**, and a count that shares a line with a name is at the mercy of how long
  that name happens to be. If a card ever states two such facts again, that is the argument to reuse.
  ([evidence](DECISIONS-EVIDENCE.md#n88))
- **The library's rows are a two-rule shape: a category holds movements, and nothing holds a movement**
  (N95, cut back by N110). The grouping is a **link**, so every movement stays loggable and keeps its own
  records, step and prefill, while the views that fragment when one movement is scattered across rows can
  be read as one thing. The head is a **category**: it is **never offered and never logged**, and it is a
  *statistic* rather than a classification. Rejected: merging a family into one row, because a paused
  bench is a different lift with its own records. ([evidence](DECISIONS-EVIDENCE.md#n95))
- **A movement reads its family's pattern, primary muscle and secondary muscles, and states its own
  equipment** (N110, on N103's rule). Those three are nullable, **null is *unstated*, not `OTHER`**, and
  the winner is the same for all three, the secondary list included: **own-if-stated, else the
  category's**. **Equipment is the one fact that never inherits**, because a family spans equipment, so a
  category cannot state one for its members. Rejected: copying a category's values onto its movements,
  which fails the first time a lifter renames a family. ([evidence](DECISIONS-EVIDENCE.md#n110))
- **The movement pattern is a row's own when it states one, and its family's when it does not** (N96,
  amended by N103 and N110). Whether a family presses or hinges is the family's to decide, so the seed states it once
  on the category and no movement under it states one — but the **rule** is own-if-stated, the same one the
  muscle follows, because a rule that held only while no row stated what its head also
  stated would diverge the first time a lifter set one by hand. **A row that states none, with nothing above
  it that does, has no pattern** — one fact, one home, and a loose custom movement is exactly the case where
  "press or pull" says little. The merge took the vocabulary from eleven to nine: `HORIZONTAL_PUSH` and
  `VERTICAL_PUSH` became `PRESS`, the two pulls became `PULL`, and **LUNGE stays out of SQUAT** because "have
  I been squatting?" has an answer and lunges are not it. **The four retired names are gone from the enum**
  (N104): nothing can reach them, because 2.0 starts a store that never held one and refuses a 1.x file before
  the decoder, which is what a legacy value was for while a 1.x row could still be read.
- **A head reads as one number, and which metrics take one is decided per metric** (N97, narrowed by N110). A
  *head* is a row that holds others, and **only a category does** — a movement heads nothing — so its series
  reads its own id and the ids of the movements filed under it, which are distinct, so the union cannot
  double-count. The head's own row is included, which costs a category nothing because it never names a set.
  **The statistics lift list is the one list where a head is offered — and only for the
  metrics that read one** (B98), a read-view exception to N95's rule rather than a repeal of it: a head is
  still never offered while logging and never named by a set, the pickers that log keep their filter, and a
  metric that reads a single id is offered movements only, because choosing a category there would draw an
  empty chart under a label promising a family. Volume, total reps, RPE, muscle feel and joint pain roll up,
  because each is a sum or an average that means the same thing over a family — **the ratings counted once per
  logged exercise, never once per set or from whichever row the query returned first** (B97); **a heaviest set
  or an estimated 1RM does not**, because a paused single merged into a competition bench reads as a
  decline that never happened. **Records and progression stay the exercise's**, and so does assistance: a
  record belongs to the lift that was performed, and whether a family is uniformly assisted is a fact about
  its *sets*, which the picker cannot know before any set is read.
- **A movement filed under a movement is flattened onto its category, never cleared** (N110, on B92). A file is
  the one place a link this app would never write can arrive, and **every variation in a file written before
  this change is exactly that link**, so the shape rule walks up to the first category and files the row there.
  What the row used to read from each movement above it is **folded onto the row first** — muscle, secondary
  list, pattern and equipment — because a variation has become an ordinary movement and a movement states its
  own facts, so this is the one moment a formerly inherited value has no other home; equipment travels with
  them even though it is never inherited *live*, or a paused bench would arrive with no bar. A **cycle** is the
  one case cleared rather than folded, because there is no top to reach. **The device's own store is carried
  across the same way** by a migration, which also fills a seeded category's pattern and muscles from the seed
  itself — but **only while it states nothing**: a value a lifter gave is never overwritten, and what a movement
  already states is never stripped.
- **A program document is its own format, and every exercise it names travels with it** (N47). It has a
  **version of its own** and carries the program's *definition* — the program, its slots, the templates
  those slots name and their planned work — not its history, because skips, deloads and substitutions
  belong to the device that trained them. A user's own exercise id means nothing on the receiving
  device, so the definition travels whole and the receiver creates what it lacks. Loading **merges by id
  and overwrites nothing**, and the program arrives **inactive and last**. Rejected: restoring a row
  this device has deleted, as the backup does, because this is an additive share (B51).
  ([evidence](DECISIONS-EVIDENCE.md#n47))
- **History that predates the app is converted outside it, by a page the app never talks to** (N159). The app
  reads only its own formats, and it must: the backup's import is one transaction that names no row when it
  refuses, and its merge never overwrites, so a generated file either fails opaquely or lands unchecked and
  cannot be corrected. The conversion therefore lives in [`docs/`](docs/) — a zero-dependency page with no
  build, no request and no stored training data, which runs from Pages and from disk — and writes the four
  collections a backup requires at the backup's own `schemaVersion`. **The input is a flat row list, not the
  app's format**: a model transcribes the sheet and the page does every conversion (whole grams, half-points,
  epoch millis, enum names, ids), so the error-prone half is code with tests. **Ids are deterministic**, keyed
  on the source coordinate rather than the values, so the same file twice adds nothing and a file whose first
  import failed can be fixed and loaded again; correcting a row that already landed means deleting it in the
  app and raising the revision, because the merge never overwrites. **The file carries nothing but history** —
  goals, measurements and programs are omitted rather than defaulted, because a generated block would be data
  the tool has no business writing. Rejected: a CSV import inside the app, which would be a second parser with
  no review step, and `.xlsx` reading, which needs a dependency and is one *Save as CSV* away.
  ([evidence](DECISIONS-EVIDENCE.md#n159))
- **A lift's progress may be drawn from a loaded file, and the numbers stay the app's** (N165). The web helper
  charts one movement from the export the lifter loaded: the heaviest working set and Epley's estimate from
  it, movements only, kilograms only, nothing stored. That is not the *web dashboard* the roadmap refuses —
  that line is about a surface that holds, syncs or accounts for the record, and this one holds a file it can
  close. The fence is explicit: no persistence, no fetching, no account, and **no second definition of a
  metric**. The estimate keeps the app's rules (0.5 kg rounding, a single rep is the max, nothing above 12
  reps), the warm-up exclusion is the app's own, and so is the row filter: a deleted session, exercise or set
  and an unfinished session are excluded exactly as `TrendsDao` excludes them, because a chart that counted
  them would show a session the phone does not (B179). A generated fixture runs the app's
  `toExerciseTrendPoints()` and the tool's series over the same rows. A family head is not offered, because
  aggregating one is the app's roll-up rule re-implemented.
  ([evidence](DECISIONS-EVIDENCE.md#n165))
- **Body data may be written from a Waistline backup, DAILY records only** (N166). The helper may append
  `measurements` to the backup it writes: one DAILY row per day — weight in grams, body fat and muscle in
  tenths of a percent — and no tape sites. Waistline's numbers are in the unit its own `settings` names, and
  the panel shows that unit and lets it be corrected; a day becomes local noon, so a zone change cannot move
  it; the row's id is a hash of its day and kind, so a re-import adds nothing and restores a row the app hid.
  **A day the device already holds wins over the file** (B178): the app keeps one entry of each kind per day
  (N113) and an import adds rather than overwrites, so a file row for a day already recorded is skipped
  instead of becoming a second live entry — which is the state the app's own `findOnDay` exists to prevent.
  A stat the app cannot store is named in the panel, never silently dropped — refusing the whole file would
  refuse every Waistline backup, which always holds tape sites. The app needs no new format: it already
  carries measurements and restores them, and a fixture proves its codec decodes the output.
  ([evidence](DECISIONS-EVIDENCE.md#n166))
- **A program may be written from a table, as a document, and the app stays the editor** (N167). The helper
  writes a `ProgramDocument` (format version 4) from a sheet: one row per planned exercise, days become
  templates, weekdays become slots, names resolve against the lifter's library and then the seed, a name in
  neither is created as a movement, and loads are kilograms. It creates and never edits — the app inserts by
  id and never overwrites, and a live program under the document's id ends the merge rather than letting the
  document's new day attach to it (B180), so a re-imported edit is a no-op — and the page says which case a
  same-named program is: unchanged, restored, or a second one. The document is the same format the seeded
  examples use (P3.19), so one codec and one fixture cover both.
  ([evidence](DECISIONS-EVIDENCE.md#n167))
- **The app offers three example programs, and they are documents** (P3.19). A new install's Programs screen
  offers *Start from an example* beside *Load a program file* and *Start empty*: full body × 3, upper/lower
  × 4, push/pull/legs × 3, shipped as program documents in assets and inserted through the same codec and
  merge a picked file uses. They are structure only — seeded movements, no loads, a target RPE of 8 so N50
  has a gate — and arrive inactive, so the lifter renames and activates one rather than the app choosing.
  Nothing is installed at startup: an example appears only when its card is tapped, and a deleted one comes
  back the same way — the document un-hides its own program row and nothing else (B177).
  ([evidence](DECISIONS-EVIDENCE.md#p319))
- **A saved plan offers the schedule, and lands in the program it was added to** (P3.18). *Save as plan*'s
  confirmation offers *Schedule it* beside *Open the plan*: pick a program and a weekday (or none, for an
  order-only slot), the slot is written, and the offer moves to that program's editor, where the week is
  finished. The copy stays the repository's transaction (N31); this adds only the second question — when —
  at the moment the lifter has decided to keep the workout, and it never chooses the program or the day.
- **A workout can be born in the program that needs it** (P3.18). *Add workout* offers *New workout* before
  the plans that exist: the template is created, its slot appended, and the plan opened for its exercises.
  The two writes stay separate repository calls, and the slot is added only after the template exists, so a
  slot never points at nothing. It is the same rule the schedule offer follows: the screen the lifter is on
  is where the missing row is made.
- **A plan exercise is prefilled from the last session, and the warm-ups stay out** (P3.18). Adding an
  exercise to a template reads the last time it was trained and writes one target per working set: the reps
  performed become the range's both ends, the load becomes the target, the assistance carries only when it
  was used. A warm-up is the lifter's ramp rather than the plan's work, so it is not copied. The numbers are
  ordinary targets — accept by leaving them, disagree by editing — and a lift with no history, or a failed
  history read, adds an empty plan rather than failing the append that already happened. **A failure while
  filling them is reported, though** (N168): the append stands and the picker says the targets could not be
  written, because a storage error that vanishes leaves a plan the lifter tops up without knowing why. The
  picker remembers the exercise that landed, so a second tap answers as that add rather than duplicating it.
- **A plan's quick entry writes a block, and the per-set dialog stays for what differs** (P3.18). A count,
  reps and a load in one row append that many planned sets with the same targets, starting from the last
  planned set; the single-set dialog remains for a top set and its back-offs, and a half-parsed row writes
  nothing. The rows it writes are ordinary targets: the quick entry is a way to type a plan, not a second
  kind of plan, so nothing downstream knows which entry made a set.
  ([evidence](DECISIONS-EVIDENCE.md#p318))
- **A bulk answer takes each set's own step, and the rep wins when a row offers both** (P3.17). The question
  offers one control per direction where two sets share it, and — when the directions are mixed — one that
  takes each row's own: the rep below a ceiling, the load at it. A plan with no ceiling offers both on the
  same row, and the rep is taken there because it is the smaller step at the same load. Picking is still not
  writing: only *Done* reaches the plan.
- **The prefill states its two sources, and neither is labelled a recommendation** (P3.17). Under the next
  set's fields a line names what the lift did last time and what the plan asks, each labelled; a set with
  neither source shows nothing. The set named is the one the prefill read — the last set for an ordinary set,
  and a **rung's own place last time** for a rung, which is where its reps come from (N168) — not the
  session's best, so the line explains the number on screen. It is not the app's opinion of what to do next
  (N33): it is where the numbers already on screen came from.
- **The progression prompt is a three-way choice, and the old switch still answers** (P3.17). Settings carries
  *Ask*, *Apply at Done* and *Off* instead of on/off: the middle is the lifter who wants the steps taken
  without tapping each one, and a boolean could not express it. Apply opens the same question with every
  earned step already picked, so the plan is still read before it is written and one *Done* is the consent
  (N33). A device that never opens the control reads its old switch — on means Ask, off means Off — and the
  old key is never written again.
- **The progression step is the movement's own, and the plan states the rule instead of offering a second
  place to set it** (P3.17, correcting the row's premise). N77 and N111 already give every movement a weight
  step, authored in its editor and carried in the backup, and the progression offer uses exactly it; N74
  already decides the direction from where the set sits in its range. A nullable field on the *plan's*
  exercise would be a second place for one number, and a device setting would put an authored training
  decision outside the file that is the app's only escape hatch. So the plan gains the sentence it lacked —
  the rep below the ceiling, the weight at it, the gate, and which one is missing (B181 corrected the first
  wording, which named the weight step as the only one) — and adds no storage.
  ([evidence](DECISIONS-EVIDENCE.md#p317))

## Adherence

- **Adherence counts occurrences; the calendar marks days. They are two questions** (P3.5). The
  ratio is done over done + skipped + missed, one per scheduled occurrence, while the grid draws
  the days a finished session happened on. Counting *days* in the ratio was rejected: two slots
  may fall on one Tuesday and P3.3 settles them one at a time, so a doubled schedule would hide a
  miss behind the one day it shares with the session that was done.
- **Done means finished, not started — deliberately stricter than the prompt** (P3.5, amending
  P3.3's rule for a different question). P3.3 asks "should I nag you about Tuesday?", so a start
  silences it; adherence asks whether the training happened, so an abandoned start is a miss and
  marks no trained day. Reusing the prompt's started-session rule was rejected because it would
  record a session opened and abandoned as training. ([evidence](DECISIONS-EVIDENCE.md#p35))
- **One window: the month the grid shows is the month the ratio covers** (P3.5). The calendar
  navigates back through history and forward no further than the current month, and days after
  today are drawn but never scored — only a day strictly before today can have been missed.
  A rolling window drawn beside the grid was rejected because the number and the grid would then
  be counting different things.
- **With no active program there is no ratio, and the calendar still marks the days trained**
  (P3.5). A trained day needs no schedule; a ratio does, and without an active program there is no
  schedule at all (N56). The template pins that used to stand in carried no skip record — a deliberate
  rest on one was indistinguishable from a miss — so scoring them was rejected. A month with no elapsed
  scheduled day says so rather than reporting 0% or 100%.
- **A trained day is the finished session's own day, and a week is taken in that session's own
  zone** (P3.5, extending N25 and B45). A workout performed abroad marks the day it happened even
  when the device's month has moved on, and weeks stay Monday-start in that same zone because
  that is what a `program_skips` row is keyed by.
- **No schema change** (P3.5). Everything it reads shipped with P3.3 at v20 — `program_slots`,
  `program_skips`, and `workout_sessions`' `templateId`, `finishedAt` and `zoneOffsetMinutes` —
  and the aggregate is the same pure, JVM-tested `ProgramSchedule` the prompt uses, fed the
  window's finished sessions. P3.3's inherited limits stand, except where P3.13 lifts one: skips
  exist only from P3.3 onward and only for a week whose prompt was answered **or one the lifter
  corrected by hand**, an unanswered past week still reads as misses until it is corrected, and the
  schedule is the program as it is *now*, so editing a slot rewrites past weeks.
- **A skip is the lifter's statement, so they can add and remove one by hand** (P3.13, amending
  P3.5's stated limit). Tapping a scheduled day opens what that day scheduled — one row per
  occurrence, because a day can schedule two, exactly as the ratio counts (P3.5) — and each row can
  be marked skipped or unmarked, using the same `program_skips` row. **Adding looks backwards
  only**: a day passed over is behind you, so only today or earlier is offered. Removing is always
  allowed and soft-deletes, returning the day to done, missed or pending by P3.5's same
  definitions. The app never removes a skip itself and *Continue* keeps writing a whole week: the
  correction is a second, explicit writer rather than a second opinion. A finished session is not
  correctable — it is the record — and a deload week offers nothing to correct, because it is not
  scored (P3.10).

- **The breakdown is the same aggregate, read two ways, and the parts are the whole** (P3.14).
  The per-slot counts are accumulated in the same pass as the totals rather than recomputed, so
  there is one definition of done, skipped and missed and the rows cannot drift from the ratio
  above them; a slot with nothing scored is left out, because a row of zeros answers nothing. The
  per-lift rows are a second grouping over N14's join — a slot's template names the exercises it
  trains — which is what answers "am I skipping *this lift*, or this day": a lift trained by two
  slots is counted in both, so those rows deliberately do **not** sum to the month's total the way
  the per-slot rows do. Counts, not a per-row percentage: two of three is not 67% of anything worth
  printing. It needs an active program, for the ratio's reason (P3.5): with no schedule there is
  nothing to break down. No schema change — every row is the aggregate P3.5 already reads.

- **A streak counts scheduled occurrences, not days, and is shown with its start** (P3.15).
  Consecutive calendar days was rejected because it would break on every rest day, and by P3.5's
  rule an unscheduled day *is* rest, so it is invisible here rather than a gap. A skip and a miss
  both break the run — the occurrence was scheduled and it was not done — while a deload week
  neither extends nor breaks it, because it is not scored (P3.10). Today is the one case that is
  neither: an untrained today is stepped over rather than counted as a break, or the number would
  be wrong for most of the day. The walk is bounded by the Monday the earliest active program was
  created, because a slot day before the program existed was not an occurrence; an order-only
  program and no program at all both report no run, the second for the ratio's reason (P3.5). It is
  a number with its start and never a nudge: the app has no notifications (P4.7 is parked), and a
  streak that pushes is a coach.

- **The ratio's history is the same aggregate per month, so a point cannot disagree with the
  grid** (P3.16). A month is the right grid and too short a judgement — a block is four to six weeks,
  so a change that took one reads as one flat month after another — so the ratio gains a chart of
  one point per month while the grid keeps its month. It is deliberately the *same* `monthAdherence`
  evaluated once per month over one wider read rather than a second ratio: P3.5's one-window rule
  holds, and a point and the grid it came from cannot drift. A month with nothing scored is a gap
  rather than a zero, which is N37's rule for a trend line said again — the chart breaks the line
  instead of drawing through a month nobody scored. The window is twelve months and is **not** N21's
  statistics range: that setting is another screen's chart window, and reusing it would make one
  number mean two things. Because it has to agree with the grid, the history inherits P3.5's limits
  instead of inventing a second answer — including that the schedule is the program as it is now.

## History and transfer

- **A query searches what a row says, not what is behind it** (N146). The history list is a SQL projection of
  totals and a template name, chosen so a year of training costs one query; reaching an exercise name would mean
  loading every set of every session, which is the cost the projection exists to avoid. So a query matches the
  month a row is grouped under, the plan it was started from, the activity it was, and a workout's set count —
  an activity's row prints none (B167), so the query does not reach one either — and the field's own hint names
  those four, because a search that silently cannot do what its placeholder suggests is
  worse than a shorter list of things it can. Both the query and the row are compared with the whitespace and
  underscores removed: `ROCK_CLIMBING` is what a lifter calls *rock climbing*, and the row's label is a string
  resource the ViewModel may not read.
- **An empty result is not an empty log** (N146, N143's lesson in a different place). A filter that shows "you
  have not trained yet" while a query is hiding rows is a lie about the lifter's own data, so the two states are
  separate values (`isEmpty` and `isFilteredEmpty`), and the filtered one carries the control that undoes it.
  The same rule decides the branch order in the screen: the *filtered* case is tested before the empty one, or
  the general condition swallows the specific.
- **A transfer runs where a rotation cannot cancel it** (B158, B159). The Storage Access Framework has already
  created the document by the time the picker's result arrives, so a coroutine that dies with the composition
  leaves a 0-byte file behind and reports nothing — the lifter finds out when a later import calls it unreadable.
  The work and its report therefore live in the ViewModel, and the file IO is a **`DocumentIo` seam** that takes
  the caller's `Context`: a ViewModel may not hold one, and the dispatcher switch belongs in one place so neither
  caller can forget it. The exception that keeps this honest is *any* work whose failure has no user to tell.
- **An import may only un-hide what a delete hid** (B149). A soft-hidden child row has two causes the ids cannot
  tell apart — the parent was deleted, or its list was replaced by a save — and correlating the timestamp the
  undo uses is not available to an import, which acts on a file written before either write. So the question
  asked is the one a delete and a replacement answer differently: is the parent coming back too. A restored
  session is the same shape at the top: it comes back **closed at its own start** (B150), because
  `finishedAt IS NULL` is the app's whole definition of the workout in progress and a file may not decide what
  this device is doing.

- **One program-edit core is compiled into both hosts, and neither calls the other** (N170). The
  document's model, codec, version gate, validation and edit planner live in a Kotlin Multiplatform
  `:shared` module: its `jvm` target is what `:app` links against, and its `js` target is what the
  import helper loads as five plain scripts. The page calls `planJson` and `validateJson`; the app
  calls `plan` and `validate` directly. There is **no localhost service**: a socket needs `INTERNET`
  — loopback included — and a foreground service to stay alive, and the page runs on another
  machine, so the two hosts exchange the document file and nothing else. The shared model carries a
  `Weekday` enum and string taxonomy, because `java.time` and the domain enums cannot compile to JS;
  the JSON keeps the names it always had, and the app maps at its transfer edge. The bridge is
  mechanical and total: every field travels both ways, including the ones the page does not edit.
  Rejected: a WebView hosting the page inside the app (a bridge, a second UI model and a screen
  Compose cannot test), and two implementations of the rules pinned only by fixtures — the drift
  B178 and B180 already arrived by. ([evidence](DECISIONS-EVIDENCE.md#n170))

## Rules that apply to every change

- **A range is a window in the current zone; a session's date is where it happened**
  (B45). A window is one interval and has to be measured somewhere, so the current zone is
  the only answer that makes "today" mean today; a session's date is a fact about the past,
  shown in the zone it was performed in. The accepted consequence is that a workout near
  midnight after a long flight can be listed under one date and fall outside a window that
  appears to include it — a window per session is not a window. It is recorded rather than
  patched so the next person finds a decision, not a bug report.
  ([evidence](DECISIONS-EVIDENCE.md#b45))
- **The current zone is read when it is needed, never captured** (B45). A `val` in a
  companion object freezes the zone at class load, so a process outliving a timezone change
  keeps computing "today" in a zone the device no longer has.
- **Accessibility accompanies each screen**; it is not a later phase. Name what a control
  does (`onClickLabel`), *announce* state changes rather than only drawing them, and tag
  things so tests do not assert on English literals.
- **One mechanism makes room for the keyboard, and it is the app's** (B119, B122, B127).
  The activity's window is pinned to `android:windowSoftInputMode="adjustResize"` — which
  `enableEdgeToEdge()` does not set, and which the platform otherwise resolves to `adjustPan` —
  so the system never pans a window the inset has already padded, and each screen's scrolling
  body applies `imePadding()` once. Two mechanisms leaving space for one keyboard is not a
  layout that looks slightly wrong: it is a black band whose size changes with the focused
  field, because a pan is measured from that field.
- **The app is dark, and its colours are its own.** One fixed scheme — the palette in
  [`Color.kt`](app/src/main/java/com/example/androidapp/ui/theme/Color.kt) reached through the
  roles in [`Theme.kt`](app/src/main/java/com/example/androidapp/ui/theme/Theme.kt) — and not
  `isSystemInDarkTheme`, not `dynamicColor`. Separation is by **tone**, never by elevation, and
  accents (`TileAccent`) are named by colour and handed out per row, never bound to a meaning.
  Rejected: Material You, because a palette derived from the launcher's wallpaper cannot be *this*
  app's palette, and following the system, which would mean maintaining two schemes to say one thing.
  ([evidence](DECISIONS-EVIDENCE.md#b55))
- **The app's colour fills; a link has a role of its own** (N49, extended by N83). `primary` is the app's
  own colour and it *fills* — the selected tab, a chip that is on, and home's *Resume* — and as a label it is
  Indigo at 4.07:1 on the page and 3.77:1 on a raised card, under the 4.5:1 body-size text needs. So a text
  action draws `IndigoLink` through `AppTextButton` rather than inheriting the component's default, and
  the one surface whose links are drawn *on* a filled container — the rest bar — uses that container's
  own content colour instead. The filled tonal role moved for the same reason: `secondaryContainer` was
  the category Teal, which cannot carry a white label at all (2.9:1), and is now the same hue taken
  down to a surface (`TealDeep`, 5.94:1). What holds this is a source scan, not a ratio: a contrast
  assertion on a constant no control reads is how the first attempt shipped the role while every label
  went on drawing the fill colour.
  **A planned workout's start fills that tonal container too** (N83, narrowed by N125). Starting a plan and
  logging its next set are one move — the plan stated, and then committed — so a planned-workout start wears
  *Log set*'s colour, and `primary` no longer fills it. **One control now, not two**: the next-up pill that
  shared the rule went with its row, and the palette scan holds the pair on the card's start alone. N61's
  ladder on home's start bar survives as a difference of hue rather than of emphasis: the empty
  start keeps the deep indigo container it was given, the planned pill is the teal container, and
  *Resume* is the only one left carrying the accent.
- **Privacy: local-only.** No `INTERNET` permission, no ads, no analytics. Crash logs stay
  in app-private storage and leave only in an export the user chose to make.
- **No Google Play services at runtime.** Firebase, `play-services-*`, Play Billing and
  Play Integrity are out by default; a future integration has to argue past this line.
- **Errors are values.** Reads and writes return `DataResult`, and `dataResultOf` rethrows
  `CancellationException` rather than swallowing it — `runCatching` catches `Throwable`, so
  a `suspend` function does not use it. The last two hold-outs, `ExerciseRepository`'s two
  reads, were closed by B4.
- **Measure before optimizing.** The one known hot spot was found by reading the code, and
  any further performance claim comes with a measurement.
- **Testing:** pure logic gets JVM tests, persistence gets DAO and migration tests,
  composables get Robolectric tests with no device; there is no coverage target, and the
  active-workout Compose gap closed with N7.
- **New and touched tests assert with Truth, and Flow sequences with Turbine.** JUnit's
  `assertEquals(expected, actual)` puts two bare values side by side and is easy to write
  the wrong way round; both libraries are JVM-only and the core Truth artifact, not
  `truth-android`. Most files still use JUnit and migrate **as they are touched**, never in
  a sweep and never left half-converted — a preference about failure messages, not a
  correctness gate. ([evidence](DECISIONS-EVIDENCE.md#truth-turbine))
- **No dead weight** (B53, B54). Extract a shared component at its second caller, not its first;
  delete an API the moment nothing calls it — a component's parameter nothing passes included,
  which is how `SectionHeader`'s trailing slot and `IconTile`'s content description survived the
  restyle that created them.
- **A callback a screen cannot work without has no default** (B49, B52). A default empty lambda turns a
  forgotten wire-up into a silent no-op instead of a compile error, which is how "Add warm-ups"
  shipped dead while its ViewModel method passed every test; optional callbacks may default,
  required ones may not. The program document's export and load are the second telling — both were
  `(() -> Unit)? = null`, so the same omission would have hidden a control rather than failed the
  build, and only the screens' own tests, which pass the callback in, could have caught it.
  ([evidence](DECISIONS-EVIDENCE.md#b49))
- **Schema changes are migration-numbered as they ship.** Do not add columns or tables
  ahead of the code that reads them; copy the SQL from Room's generated `createSql`,
  register the migration in `ALL_MIGRATIONS`, and never edit one that has shipped. Every
  migration gets an exported schema under [app/schemas](app/schemas) and a
  `MigrationTestHelper` test that upgrades a database with real rows.

## Process

- **The lint baseline is unwired on purpose.** Accepting a warning is a two-step, reviewed
  act, not a side effect of running the build.
- **Formatting is checked on the lines a change touched, not on the tree** (B123, answered by N129).
  The ruleset reports **435 findings across 121 files** on today's code — 106 of them `ImportOrdering`
  alone, in **106** files, measured with
  `./gradlew :app:detektFormattingCheck --no-configuration-cache --rerun-tasks` (642 across 155 when N129
  took it; the fall is drift, not a different ruleset, and the ratio is what the rule rests on) — so
  putting it in the gate means either the repository-wide reformat B123
  rejected or a set of `active: false` entries, which is the silent gate
  [detekt.yml](config/detekt/detekt.yml) refuses by its own contract. B123 named its own revisit
  trigger: a layout defect that ships because a review missed it. **`tools/format-changed.py` answers
  that trigger without either rejected alternative.** The rules load from a second config file
  ([detekt-formatting.yml](config/detekt/detekt-formatting.yml)) over the files the diff names, and a
  finding is reported only when it lands on a line the change **added or rewrote** — because a
  whole-file check is decided by the file's own history rather than by the change, and a gate that
  fails an edit which never went near an import is a gate nobody runs twice. Nothing is reformatted
  and no rule is switched off: `detekt.yml` keeps every rule and excludes this ruleset **explicitly**
  (`formatting: active: false`), because loading the plugin registers its rules as defaults for every
  detekt task and the exclusion is what makes the gate's own run unchanged.
  ([evidence](DECISIONS-EVIDENCE.md#b123))
- **A release's mechanical checks are one script, and its commit is named rather than swept** (N128, B128).
  Two of the release's four mechanical checks are about the APK an update is installed over, and one of
  those — the certificate — is the only defect here whose repair **deletes a user's history**, because
  platform backup is off. So the certificate is compared against the *keystore the build signs with*
  rather than against the previous release's APK: the two answer the same question, and the keystore is
  always on the machine. **Nothing in that path needs a token or the network**, so it runs on CI, on a
  release machine and from a checkout that has never seen a release; only the published-asset round trip
  is opt-in, and it is there because it catches a wrong file on the right release and a right file under
  the wrong name, neither of which any other check can see. **The release commit names its three files**
  and never `git add -A`: the 2.4 round put a scratch directory of HTTP dumps into a release commit and
  only `--stat` caught it. ([evidence](DECISIONS-EVIDENCE.md#n128))
- **Releases are manual**, and the tag must point at the commit that built the APK. The
  procedure is in [RELEASING.md](RELEASING.md); why it stays manual is in
  [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md#releases).

## Verified on device

Not rules — facts checked on hardware, recorded because they are the kind that quietly
stop being true:

- Process death mid-workout resumes the session with its set and rest intact.
- The library renders on the very first read after `pm clear`.
- The three-way muscle split reaches a fresh install's pickers (N75): the readiness note offers
  *Adductors* and has no *Back*, and the library reads *Lats* for a pull-up and *Upper back* for
  a row. The seed is what classifies a fresh store (N105); there is no migration, because 2.0 does
  not read the library a 1.x row came from.
- An exercise's own weight step reaches a live workout (N77): with a 5 kg step on the Assisted
  Pull-Up, the weight field's **+** took it from 20 to 25 — the 2.5 kg the unit alone would have
  given was not used. The detail screen showed *Unit default (2.5 kg)* before the edit and
  *5 kg (this exercise)* after, which is the read-back that proves the column was written.
- The plans are reachable mid-workout (N78, narrowed by N87): the workout's ⋮ holds only *Discard* and is
  not drawn from an **empty** session at all, while Home still shows *Templates* and *Programs* above
  *Resume workout* with a live session — so the look is one step back rather than a menu inside the
  session. The templates list's *Start* was disabled with "Finish the workout you are in to start this
  one." under the row.
- The *Done* action sits at the rating row's height once a set is logged (N76).
- N87–N94 were checked together on a Pixel 6 / API 36 (density 420, so 2.625 px per dp). Statistics led the
  tab bar with Workouts third while the app opened on Workouts; a planned exercise's block drew its row
  alone until the name was tapped, then the plan note, the rest/RPE/cue fields and *Add set*; deleting a
  planned set offered *Set deleted* / *Undo* in a box clear of *Add set*, and Undo brought the **same set id**
  back; the active program sat in the programs screen's foot bar with *New program* floating above it; and
  Programs' and Templates' bars each carried the entry to the other. The next-up row's four lines ended 21 px
  above its start pill — **exactly the 8 dp N86 asked for** after N88 gave the row a fourth line, which is
  the re-measure that entry required.
- A live workout's role picker offers **Cluster** beside **Drop** (N79), which is the fact that stops
  being true if the enum and the pickers drift. The derived ladder, the one-rating rule and the pairing
  are covered by the JVM and instrumented suites — the arithmetic is a pure function, so tapping it
  would prove less than the tests do.
- **A running workout's body takes the keyboard's height** (B119). On pixel6_api36 with a set's weight field
  focused, the body's `exercise_list` viewport measured **1859 px** with the IME hidden and **958 px** with it
  up — the 901 px the keyboard occupies — so a field behind the keyboard can be scrolled clear of it rather
  than staying under it. Measured rather than eyeballed because the inset is the kind of layout that quietly
  stops applying: `enableEdgeToEdge` and a target SDK are what removed it in the first place.
- **The same reading, corrected** (B127). That measurement was taken while the window was also free to pan for
  the keyboard, and a viewport measurement cannot see a *window* translation: the same `pixel6_api36` window
  reported `sim={adjust=pan}`, and the pan it applied depends on which field has focus — which is a black band
  between the content and the keyboard whose size changes between attempts. Pinning the window to
  `adjustResize` leaves the inset as the only mechanism; re-measured after it, the viewport is still 958 px
  with the keyboard up and 1859 px with it down, and the window reports `sim={adjust=resize}`. The lesson is
  kept because it is the trap: **a viewport measurement proves the inset landed, not that nothing else moved
  the window**.
