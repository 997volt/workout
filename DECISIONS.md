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

> Reviewed against the code: 2026-10-04. Bump the date when this file is checked, the way
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
- **The muscles are one vocabulary, and a retired name stays readable** (N75). Adductors is a
  `MuscleGroup` entry and nothing else: the readiness note's sore list is derived from the taxonomy
  (`SORE_MUSCLE_GROUPS` is the selectable groups minus *Other*), so a second list of "muscles you can
  be sore in" would be two vocabularies for one idea — and the same entry makes it an exercise's
  primary muscle, which is where the taxonomy belongs. **`BACK` split into `LATS`, `UPPER_BACK` and
  `LOWER_BACK`, and `BACK` itself stays in the enum as a legacy value**: every muscle is stored by
  name and read back through `valueOf`, so deleting it would make every row that still says it — and
  every export written before the split — throw on read. It is excluded from every picker (through
  `SELECTABLE_MUSCLE_GROUPS`) and still shown where a row carries it, the way N59's legacy per-set
  effort and N63's legacy joint number are. **The seeded library is corrected by migration**, not by
  editing the seed: the seed is an `INSERT OR IGNORE` top-up that never updates a row, so a
  re-classified seed reaches fresh installs only. Each statement is guarded by the value the seed
  wrote and keyed to the seeded ids, so an exercise a lifter re-classified keeps their answer, a custom
  movement keeps its *Back*, and a secondary list is rewritten token by token so an added muscle
  survives. The deadlifts leave the back group for `HAMSTRINGS` — which the Romanian deadlift already
  had — with `LOWER_BACK` taking the retired tag's place in their secondaries, because the erectors
  holding a heavy hinge is a fact about the lift. The new constants sit where they belong in anatomy
  order rather than being appended, which is the one thing the enum's "append it, order is cosmetic"
  habit does not cover: the order *is* the pickers' map.
  ([evidence](DECISIONS-EVIDENCE.md#n75))
- **An exercise may name the step its weights move by** (N77). The ± buttons, the warm-up ramp and the
  progression offer all move a load by a step, and until this it came from the unit alone — 2.5 kg or
  5 lb — so a machine that jumps 5 kg, or 1 kg, was always edited against a step it did not have. The
  value is `stepGrams: Long?` beside `restSeconds` and `techniqueNote`, typed in the exercise's own
  unit to a tenth of it and stored in whole grams, with null meaning the unit's own; **every** call site
  reads `Weight.stepGramsFor` — the workout's fields, the warm-up ramp, the progression offer and the
  history editor, the last of which the feature's first pass missed (B65) — and the ramp follows it
  because a ramp rounded to a step the machine cannot load is the same defect one screen over. **It is a
  property of the movement, not presentation** — unlike the unit it is not cleared when the unit changes,
  and that change carries the typed number, re-expressed to the new unit's tenth, rather than reading
  "5" as the other unit (B66) — and it is deliberately *not* an ambient the way the unit is: it rides on
  the row the session and the plan already join from the library, and the pure functions take it as an
  argument. A step of zero is refused rather than stored: it is not a small step but no step, the ±
  buttons would do nothing and the ramp divides by it. ([evidence](DECISIONS-EVIDENCE.md#n77))


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
  are one *shape*: a contiguous run of sets hanging off a working set, which is the run's **anchor**.
  Adjacency is the parent link — the run is contiguous and its rows share a role — so no column ties a
  rung to its set, and the model needed none. **What separates the two roles is the load**: a cluster
  rung repeats the anchor's, assistance included, while a drop rung is the anchor's less the run's
  value, once per rung, so 100 kg with a 20 kg value is 80 and then 60. That is why the plan stores the
  **value**, once, on the run's first rung, and not a weight per rung: editing the anchor or accepting a
  progression step moves the whole ladder, one row is written, and no rung can drift. A rung therefore
  stores no reps either — the group's target is the anchor's — and its **reps prefill from that same
  set's last performance in a previous training**, matched by the set's place in the plan, because "what
  you just did" would answer with the anchor's reps. Four rules make the shape work at the write
  boundary: a value belongs to a drop and is above zero; **a rung needs an anchor** that stands on its
  own, so a warm-up cannot anchor a run and a plan cannot begin with one; a run names its value on its
  first rung and nowhere else; and every rung has to come out with something to load, which is false
  under an anchor with **no added weight** (an assisted or bodyweight set has no 20 kg to take off) and
  false once the ladder runs past zero — a **negative weight being assistance in this app, not a small
  weight** (N15), so a rung that would compute to −20 reads as carrying no
  derived weight rather than becoming an assisted set. **The anchor rule is `recordsEffort`, and every
  boundary asks it** (B63, B64): the plan's write, the live log, the re-role of a logged set, the
  prefill that reads which bar a drop comes off, and the picker — which offers a rung only where the
  boundary would accept one, because a control that cannot write is removed rather than shown. Deleting
  an anchor takes its rungs with it, so a run cannot be left hanging — and `removeSet` now does it, the
  rule having been recorded here before any code held it (B60). [runAt](app/src/main/java/com/example/androidapp/domain/model/RungRun.kt)
  is the one reader of the shape, and `isAnchoredAt` answers the same question for a position a row
  *would* take, which is what lets a picker and a boundary share one rule.
  ([evidence](DECISIONS-EVIDENCE.md#n79), [B59–B72](DECISIONS-EVIDENCE.md#b59-b72))
- **A template is living, and a session reads it at the start** (N16). Nothing links a
  session to its plan beyond the route that started it, so editing a plan changes the next
  prefill; writing targets onto the session would freeze them and make "living" false.
- **A plan is read by position, and position stays contiguous** (B72). A running session matches its
  logged sets to the plan by `setIndex` against the number logged, and a run's continuation is keyed by
  the same index, so a stored index *is* the plan's order rather than a durable id. Removing a planned set
  therefore renumbers the survivors, in the same transaction as the delete — the invariant `prependSets`
  already holds from the other direction — instead of leaving a gap that points the prefill and the rest
  rule at the row after the one they mean. ([evidence](DECISIONS-EVIDENCE.md#b59-b72))
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
- **A new planned set starts from the last one, and there is no Duplicate** (N46, narrowed by N81). The
  surface that authors a planned set is the template editor's *Add set*, which opens the set dialog on the
  last set, so a plan is extended by confirming rather than retyping and *any* count is reachable. N46 named
  two surfaces — a template's plan dialog and a slot's prescription dialog — and neither exists now: N81
  unfolded the plan dialog into the exercise block, and the slot's prescription dialog went with the program
  rework that made a slot point at a template. The prefill is the same rule either way, which is why only its
  name moved — the argument in [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md#n46) is about two because that
  is what there was.
  Duplicate doubled (1, 2, 4, 8), left the odd counts to manual adds, and appended the whole plan,
  ramp included, into an order `setIndex` exists to keep. The accepted cost is that prefilled values
  look like saved ones, so "Add set" then Save without touching a field is a plausible accidental
  double-add; the title says it is adding and the list behind it shows the count, and a set-*count*
  control would be the more honest affordance if that ever needs revisiting.
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
  because a rounded 9.3 would claim something never measured. The column is `rpeHalves`,
  and an older backup's whole-number `rpe` is read as halves rather than dropped.

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
  places answering "what am I doing on Tuesday", so it goes with its column, the `setWeekday` path
  through DAO, repository and editor, and the `pinnedFor` fallback. A program's slots are the only
  source of a dated plan; with no active program there is then **no *Today* list**, which is the point
  rather than a regression. The backup DTO's `weekday` goes with the column rather than staying for
  older files' sake: the codec already ignores keys this build does not know, so a file written before
  the change still decodes and the pin is simply not read. The loss is accepted rather than mitigated —
  a template pinned to a day comes out of the migration with no day, and getting the schedule back
  means putting it in a program, which is the rule being stated.
  ([evidence](DECISIONS-EVIDENCE.md#n56))
- **Looking at a planned workout and starting it are two gestures** (N55). A program's next-up row is
  the one place a workout can be seen before it is begun, and it used to offer only the start. The row
  moves to the bottom bar — the edge of the screen the thumb is already at, and off the list it shared
  with today's plans and recent history — with the field and *Start* as separate targets. What the field
  opens is a **read-only dialog of the workout's ordered exercises**, which settles the choice the
  roadmap left: a template's only destination was its editor, and reading a plan does not need the
  power to rewrite it. Reusing the editor with a read-only flag and previewing on a screen of its own
  were both rejected — the first for putting every target one mis-tap from a rewrite, the second for a
  destination holding a handful of names with nothing to do on it.
  ([evidence](DECISIONS-EVIDENCE.md#n55))
- **A session's exercise order is its own, and it is written as it changes** (N54). The template is
  never touched: N16's "a template is living, and a session reads it at the start" applies to order the
  same way it applies to targets, so a rack taken on one afternoon cannot rewrite every future run.
  Persisting the order with the session is the decision — a repeat (N48) copies the session's own
  order rather than the plan's, and a process death mid-session keeps the order that was arranged, which
  is the same "the session exists before anything is logged" rule P1.8 rests on. The alternative, an
  in-memory order that is written at *Finish*, was rejected: it would show one order and store another
  if the app died, and a repeat would silently disagree with the workout it repeats. Where the plan is
  read, it is paired by **movement** rather than by the slot a row occupies: the order was what the
  session was seeded from, and letting that order be edited is exactly what stopped that being true, so
  a moved exercise would otherwise read its new neighbour's targets and its neighbour's count for
  whether the planned work is done (N52).
  ([evidence](DECISIONS-EVIDENCE.md#n54))
- **The next set is stated on the workout screen, and *Log set* writes what is on it** (N59, superseding
  N51 and restoring B7 for this path). N51 put the set editor in front of every set so that a set
  differing from the prefill was corrected before it was written; the cost was a dialog between every
  set and a button whose label could not describe what it wrote. The values the plan and history
  prefill are now the exercise block's own fields — weight, reps, RPE and the role picker — so they are
  read and changed before anything is committed, the button beside them carries no values because the
  fields beside it *are* the values, and the editor is what correcting an already-logged set still
  opens. N19's role picker moved out of the dialog rather than away: it is still one set's decision
  made before the write, and it still clears itself, because the fields are keyed on the logged-set
  count and re-arm from the plan's next unlogged set (B48). The **correction dialog invents nothing**:
  it states a set that exists, so a set that recorded no effort shows *Not recorded* and keeps that on
  save — correcting a weight must not turn an absence into a 9.0 measurement — and the stepper's first
  tap states the default rather than stepping a number that was never there. ([evidence](DECISIONS-EVIDENCE.md#n59))
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

- **Progression was double progression, and it only ever suggested** (N22, withdrawn by N59). Add reps
  to the plan's rep ceiling, then the smallest loadable step (2.5 kg) and start the range again;
  a percentage rule needs a true one-rep max this app estimates rather than measures, and
  a linear weekly add ignores missed sessions. Assisted work inverts the direction, and
  with no plan there is no ceiling. **Withdrawn**: N59 states the next set on the screen and leaves the
  step to the lifter, so nothing computed a progression at the time; **N50 has since put one back at
  *Done*, earned from the plan's own target RPE rather than from the rep ceiling** (below).
  ([evidence](DECISIONS-EVIDENCE.md#n22))
- **A suggestion carried its reason, and null meant "nothing to explain"** (N22, withdrawn by N59).
  Only the three progression reasons drew a line; a line on a plain prefill would have trained the user
  to ignore the line that mattered. With the offer withdrawn the lines are gone: nothing on the screen
  is explained, because nothing on it was chosen by the app.
  ([evidence](DECISIONS-EVIDENCE.md#n22))
- **The app suggests; it never writes.** A silently applied suggestion is a programme
  decision taken without the person training, and this app is a log, not a coach.
- **Warm-ups are excluded from progression too** (N17, N20, N22) — a warm-up is not the
  work a target is measured against.
- **The progression is asked at *Done*, and the plan's own target RPE is what earns it** (N50). A plan's
  working set carries a target RPE (N14, P3.8), and where **every** prescribed working set was performed
  with its reps met at or under that target the session had room in hand, so *Done* offers the next step
  as the lifter's choice: the smallest loadable step, or a rep — and **only a rep where the plan names no
  added weight**, because an assisted set stores the machine's help as a magnitude with zero kilograms
  and a bodyweight set stores zero, so neither has a load a step can raise: offering `0 → 2.5 kg` there
  would write a weight that `Weight.display` then hides behind the assistance. *Every* prescribed set is
  **the plan the screen showed**, the slot's rows merged with the template's per index (P3.8), not the
  slot's alone — otherwise a slot overriding one of three sets made "every" mean one, and paired it with
  the wrong performance. **N74 amends both halves of that**: every set was judged as one, and the single
  step always landed on the last of them, so a set that answered the plan earned nothing when a sibling
  missed and the dialog named no set at all. It is not N22's rule restored — that computed from the rep
  ceiling alone and offered itself beside the next set, where this one is conditioned on the session's own
  effort and waits to be asked; **N74 brings N22's shape back inside the range, still gated on that
  effort**. An unrated session or a plan with no target RPE suggests nothing rather than guessing.
  **Where there is no plan there is no prompt at all**: a next step is something only a plan can ask, so
  *Done* simply finishes the exercise. **The rating is never on this path** — *How did that feel?* is
  opened from the exercise's own row, when the lifter reaches for it, because asking on the way out asks
  at the moment the answer is worth least.
  **Accepting writes the plan** — the slot's prescription for a program start (P3.8), the
  template's planned set for a direct one, and since N73 the template's alone, because a slot prescribes
  nothing of its own — because the plan is what the next run reads; a "next target"
  stored on the session was rejected as the forward view N16 removed. The write carries the **set's own**
  stored effort rather than the exercise's number the rule read, so a plan whose one field is cleared
  later is not resurrected by a copy the accept left behind.
- **Progression is offered per prescribed set, inside the plan's rep range** (N74, amending N50). *Done*
  states **one row per working set** — its number, the plan, what was done, and the step that set earned
  or why it earned none — and the lifter picks a step per row, written by **one *Done***; *Not now*, a
  dismiss and the back gesture all write nothing. **Earning is per set**: a set performed with its reps
  met at or under the exercise's one target RPE (N59) earns its own step whatever its siblings did, where
  N50 required every prescribed set and offered a single step on the last of them. **The range decides the
  direction.** "Reps from"/"Reps to" are the range the plan was authored with and progression never edits
  them; the number a session asks for is a third one, `targetRepsCurrent`, which starts on the range's
  floor (`from`, else `to`) and climbs. Below the ceiling a set earns the **rep** — one past what was
  actually done, capped at the ceiling, so asked 5 and did 7 of an 8 the plan follows the lifter to 8 —
  and the weight is **withheld**, because there are reps left to take; at the ceiling it earns the
  **weight**, which puts the climb back on the floor, so a range is walked 5→6→7→8 and started again a
  step heavier. A plan that wrote no ceiling keeps both directions, as it did under N50; a plan whose ends
  are equal, or that named only a ceiling, offers the weight alone and keeps its reps; and a set at the
  ceiling with no added weight to raise — bodyweight or assisted (N15) — is offered **nothing** and says
  so, because the app has neither a rep inside the range nor a weight to move. **The question is frozen
  when *Done* is tapped**: the offer is computed from the plan's target and the session's work, so the
  first step written would re-arm the very offer it answered, and a pick is not a write. **A finished
  exercise that is reopened is judged against the plan as it now stands**, because the target a set was
  answered against is not stored: accept a rep step, finish, undo the finish, and that same set can read
  as short of the target its own step created. Storing the answered target on the session is what N16
  refused, so the app states the plan it has rather than the one the set met. The setting (N66)
  and "no plan, no question" (N50) still hold. ([evidence](DECISIONS-EVIDENCE.md#n74))

- **A superset is a group of exercises performed in rounds** (N24). The model is a
  nullable `supersetGroup: Int?` ordinal on `session_exercises` and `template_exercises`; a
  separate `superset_groups` table would need its own ordering and a join on every read and
  expresses nothing more, while the ordinal survives reordering because `position` already
  carries the workout order. A group of one is not a group, so nothing is labelled.
- **Round semantics: the rest belongs to the round, not the set** (N24). After a set the
  app moves to the next member with no rest and starts the rest after the **last** one,
  otherwise pairing defeats itself; the rest is the group's longest member or the app
  default, and labels are giant-set notation: A1, A2, A3.
- **N24 is a schema change, done by the book**: migration 15→16, an exported `16.json`, a
  `MigrationTestHelper` case seeding a real superset, and registration in `ALL_MIGRATIONS`
  — columns land with the code that reads them. It is the first *structural* change since
  the features after N14, and those per-exercise features do not need revisiting.

- **A new column joins the backup codec in the same change, and the codec is guarded by a
  round trip** (N24's preparation). The hand-written codec omits a column it does not know
  without erroring — three silent losses so far (N9, N15, N16) — so
  `BackupCodecRoundTripTest` asserts every field of every backed-up entity survives
  entity → DTO → entity. A session's `restEndsAt` is the one deliberate exclusion.
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

- **A measurement is one entry per day, edited rather than added to** (N32). Several per
  day makes "what did I weigh today" a question with more than one answer, and a second
  reading is nearly always a correction of the first; the day is the local day it was
  taken, which is N25's rule for a session's time.
  ([evidence](DECISIONS-EVIDENCE.md#n32))
- **An unmeasured tape site stays blank** (N32) — carrying a value forward is
  indistinguishable from a measurement and draws a flat line through a site nobody
  measured.

- **One tap logs what happened; the app's idea of what should happen was an offer** (N33, withdrawn by
  N59). Prefill and progression proposal were separate fields, so a suggestion was not committed by
  the next tap; the rule was global, not program-only, because how a workout was started
  says nothing about whether its lifter progresses by hand. **Withdrawn**: N59 makes the next set's
  values fields the lifter reads and changes before committing, so a separate proposal had nothing left
  to add and there was no button left to accept one on.
  ([evidence](DECISIONS-EVIDENCE.md#n33))

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
  (N41). The empty workout keeps its prompt-free discard because there is nothing to lose; a workout
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
- **A group is rated, judged and rested once** (N79, extending N67). A drop or cluster **rung is work, so
  it is not excluded from the app the way a warm-up is — but it is not rated on its own**, because the
  group is one effort: the rating lives on the set the group hangs off, and `recordsEffort` is the one
  rule that hides the field, nulls the value at every write boundary and keeps it out of a backup. The
  reason is deliberately **not** N67's, and the two live on one property because they have one answer:
  a set that records no effort is not a record either, so a rung cannot set a personal record while its
  work still counts as **volume**, which sums every set and every rep. **The group is judged on its
  first set** — the plan's reps met and the effort inside the exercise's target, then N74's step applied
  once, with a rung stating that reason where it earned nothing rather than being measured against
  numbers that are not its own. A rung cut short therefore does **not** hold the group back: it has no
  target of its own to miss. **The rest waits for the run to close**, which needs no new field: the
  exercise's rest runs once after the last rung, the shape a superset round already has. And **the
  Done question pairs by class rather than by raw position** — prescribed work sets with performed work
  sets, prescribed rungs with performed rungs — because a drop logged after a working set used to shift
  every later pair, judging a prescribed set against a lighter drop's reps and offering it a heavier
  weight that accepting wrote into the plan.
  ([evidence](DECISIONS-EVIDENCE.md#n79))
- **The workout's overflow is the logger's own business, and the templates list withholds the start**
  (N78, narrowed by N87). The tab bar stays off the logger (N34): a bar under a live set logger invites
  losing the session, while an overflow entry is a deliberate step. N78 put Programs and Templates there
  and un-hid Home's two plan links for the same reason — a lifter checking what is next should not have to
  end a workout to look. **N87 keeps the second half and reverses the first**: mid-workout the overflow
  holds only *Discard*, because the two plans already live where the decision to train is taken, on home's
  start bar, which stays visible while a session is open — so the look is still one step back rather than
  the end of the session, and the logger stops carrying a way out it does not own. The ⋮ is then drawn
  **only when there is something to discard**: with the plan entries gone, an empty session's own discard
  is prompt-free and drawn in the body (N41), so the menu would open on nothing.
  **B43's withholding lives on in a narrower place**: starting is idempotent (`startOrResumeSession`
  hands back the open session), so the templates list asks the workout repository whether a session is
  open and disables *Start* when one is, with the row saying why. The button would not make a second
  workout — it would silently take a lifter who tapped *Start* on one template into the workout already
  running from another, and that is the lie it prevents. Making Templates a tab, or showing the bar
  mid-workout, was rejected for N34's reason.
  ([evidence](DECISIONS-EVIDENCE.md#n78))
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
  computed from **one** rule (every logged set against every planned one), so the line cannot say work
  is done while the button still promises the plan's next set; a null plan stays null rather than
  reading zero, because "nothing was planned" and "all of it is done" are different statements.
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
  the screen the whole scheduling half is edited from does not belong behind a menu. Keeping both
  entry points "just in case" was rejected — it is what made the tab a second path to everywhere
  else. ([evidence](DECISIONS-EVIDENCE.md#n42))
- **A repeat is addressed to a workout, and a row's second action is an icon** (N48, amending N29).
  The one-tap copy of exercises and order is the same; what changed is that it names its source, so
  a History row repeats *that* workout rather than whatever is newest, and the repository reports a
  row deleted since rather than opening a blank session. The action sits in the row's trailing slot
  beside the open, because a row is one thing you tap and an action on it is an icon; and because a
  repeat is a start, it still goes through P3.3's missed-day question.
  ([evidence](DECISIONS-EVIDENCE.md#n48))

## The app's data

- **An action on the whole database lives in Settings** (N43). Export, import and
  delete-everything had moved from the library to the home overflow (B1) on the argument that home
  is "where you start"; that placed them beside *Start workout*, which is an action on one workout
  rather than on the database. Settings is about the app, so they are rows there, in the order that
  matters: export, import, and the one that cannot be undone last and coloured.
  ([evidence](DECISIONS-EVIDENCE.md#n43))

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
- **A program orders templates; a slot is a template plus an optional weekday** (P3.3). It is
  the container N16's pins cannot be on their own: a pin says what happens on a Tuesday, but
  nothing orders the pins against each other, so "which one is next" has no answer. A
  weekday-less slot is order-only and is never "missed" — it has no day to miss. Today's plan
  comes from the slot pinned to today, and **with no program active home falls back to the
  template pins it already reads**.
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
- **A slot prescribes its own intensity, and an empty prescription leaves the template's targets
  standing** (P3.8, extending N14). The prescribed sets are rows of their own — a
  `program_slot_exercises` row for the rest, the **one RPE** and the cue, `program_slot_sets` for the
  sets — so two slots pointing at one template can train it differently, which naming a template alone
  could not. The vocabulary is the plan's (role, the split load, the rep range, a note) plus one a
  template's planned set cannot carry: a percentage of the estimated one-rep max. **The planned RPE is
  the exercise's, one number for every set it prescribes** — a plan asks for an effort, while the set
  logged against it records what that set actually felt like, one per set (N6, N59) — and a migration
  seeds it from the per-set values a plan already carried, which stay in their columns so an older
  backup still restores whole. **The fallback is for a plan that never named one at the exercise level,
  not for one that was cleared**: saving a blank field over an exercise that had a number clears the
  legacy per-set values in the same transaction, or the reader would resurrect the effort the lifter
  just removed. Every target is nullable and nothing verifies it, for N14's reason.
- **A percentage is derived, never assumed** (P3.8, from N17). The kilograms are Epley's estimate of
  the exercise's heaviest working set, rounded to the loadable step. An exercise with nothing
  estimable leaves the load open and history prefills it, which is "no number" said honestly rather
  than a borrowed one; a weight the slot writes wins over its own percentage, because the two are
  alternatives rather than a sum.
- **The slot travels as a start argument, never as a stored fact** (P3.8). Home already knows which
  slot it is starting, so the route carries the slot id and it seeds the rest, cue and targets; the
  session still records only the template (P3.3), so provenance and occurrence matching are
  unchanged. Storing it would make the session's provenance say two things and would go stale the
  moment the slot moved.
- **Progression is offered per slot, from that slot's own history** (P3.8, extending N22 and N33).
  A session names only the template, so which slot it belongs to is P3.3's occurrence matching —
  `sessionAssignments`, read where the session rather than the occurrence matters — and a heavy
  Monday and a light Friday progress apart. Keying the history by template alone was rejected: it is
  exactly what makes two slots progress together. Nothing is written unless the lifter accepts it.
- **A program's order is a run, derived from what was done rather than a stored cursor** (P3.9).
  The next slot follows the last slot trained or consciously skipped; a day simply missed leaves the
  run where it is, which is what the missed-day question is for, and an edited program re-derives
  its place rather than leaving a pointer at a slot that is gone. A program with **no weekdays**
  still runs A → B → C, because a cursor rather than a day attributes an order-only session to its
  slot. Home offers it as a next-up row for a program with nothing scheduled today, and the editor
  marks the place. **Rotation is by what was done, never by load**: choosing the next template from
  fatigue, soreness or accumulated load was rejected, because nothing the app records measures
  recovery and a rotation whose reason the lifter cannot see is a coach, not a log (N22's rule).
- **A deload is a week the lifter marks, never one the app computes** (P3.10). It is an event keyed
  by program and week, the shape of a skip, so it records a decision about how the block is going
  rather than being a dated plan (N16) or a derived week. **Exempt from the ratio, not from the
  calendar**: a deload week's scheduled occurrences are neither done, skipped nor missed, so a
  deliberate back-off cannot read as a failure, while its sessions still mark their days and the
  missed-day question still asks in that week. **Nothing is scaled for you** — what a deload week
  prescribes is what the slot prescribes (P3.8) — and a week that has not started cannot be marked,
  because the app has no forward view and a deload is decided by how the block is going.
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
- **A substitute trained from a next-up row is an ordinary session, and P3.9 advances the run past its
  template** (N85). A next-up row has no occurrence to key a substitution to — the run is calendar-free and
  `ProgramRun` carries no week — so the pick records nothing and the workout starts from the chosen template
  with the slot's prescription seeding it (P3.8). What it does **not** do is hold the run still, which an
  earlier version of the entry claimed. The session names the template it trained, and P3.9's run follows the
  last slot trained, so a pick naming another slot of the same program moves the run past that slot exactly
  as starting that template from the templates tab would; recording no substitution means only that history
  does not mark the session as a stand-in. The alternative — a session that names a template but settles no
  occurrence — was rejected: it needs the session to carry a second fact, and it contradicts P3.9's one rule.
  The picker offers *Restore the scheduled workout* only where the pick was written, because a control that
  cannot do anything is worse than no control (N53, N67).
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
- **A program document is its own format, and every exercise it names travels with it** (N47). The
  backup is a restore of a whole database, so it cannot be the way one program is handed over; this
  is a document with a **version of its own**, carrying the program's *definition* — slots, the
  templates they name, their planned work, what each slot prescribes — and not its history, because
  skips, deloads and substitutions belong to the device that trained them. Exercise ids are why it
  carries more than references: a seeded slug means the same thing everywhere, but a user's own
  exercise id means nothing on the receiving device, so the definition travels whole and the
  receiver creates what it lacks. Loading **merges by id and overwrites nothing**, so the same file
  twice is a no-op, and the program arrives **inactive and last**, because following one is a choice
  (P3.3) and the authored order is the user's (P3.12). A movement whose exercise is nowhere is
  dropped rather than failing the foreign key and rolling the document back — and **"nowhere"
  includes a row this device has deleted** (B51): a hidden row is not one the load may resurrect,
  because un-deleting a lift is a write to the library, so the movement is dropped and the count is
  worded "not in your library", which is true of both cases. A prescription is placed only where the
  slot's template actually trains its exercise (B56), the rule the interactive writes already
  enforced through `requireExerciseInTemplate` — the import writes raw rows, so it re-asks the
  question against what the template holds *after* the carried exercises land.
  ([evidence](DECISIONS-EVIDENCE.md#n47))

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
  (P3.5). A trained day needs no schedule; a ratio does. The N16 pins home falls back to carry no
  skip record, so a rest on a pinned day is indistinguishable from a miss, and scoring the pins
  was rejected for exactly that reason: it would turn every deliberate rest into a failure. A
  month with no elapsed scheduled day says so rather than reporting 0% or 100%.
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
  printing. It needs an active program, for the ratio's reason (P3.5): the pins carry no skip
  record to break down. No schema change — every row is the aggregate P3.5 already reads.

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
- **The app is dark, and its colours are its own.** One fixed scheme — the palette in
  [`Color.kt`](app/src/main/java/com/example/androidapp/ui/theme/Color.kt) reached through the
  roles in [`Theme.kt`](app/src/main/java/com/example/androidapp/ui/theme/Theme.kt) — and not
  `isSystemInDarkTheme`, not `dynamicColor`. Material You was rejected because a palette derived
  from the launcher's wallpaper cannot be *this* app's palette, and following the system would
  mean designing, testing and maintaining two schemes to say one thing. Separation is by
  **tone**, never by elevation: a shadow is invisible on a near-black page, which is why the
  scheme fills in Material's whole `surfaceContainer` ladder rather than three roles, the rest
  of which would otherwise stay at the baseline purples. Accents (`TileAccent`) are named by
  colour and handed out per row, never bound to a meaning — binding a hue to a concept means
  inventing one the day a new kind of row appears and reusing a colour, losing the distinction,
  every time one retires. The window theme and the system-bar styles are part of the rule rather
  than decoration on it: a dark app whose window is light flashes white on every cold start, and
  bars tinted from the *system's* light/dark setting draw dark icons onto a black bar on a
  light-mode device. A coloured surface also names the colour that reads *on* it rather than
  assuming white (B55): white is 2.5:1 on `Amber` and 2.9:1 on `Teal`, under the contrast a graphic
  needs, so each accent carries an `onColor` and a test holds every pair at 3:1 or better.
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
  **A planned workout's start fills that tonal container too** (N83). Starting a plan and logging its
  next set are one move — the plan stated, and then committed — so both planned-workout starts, the
  next-up pill and today's card, wear *Log set*'s colour, and `primary` no longer fills either of them.
  N61's ladder on home's start bar survives as a difference of hue rather than of emphasis: the empty
  start keeps the deep indigo container it was given, the planned pills are the teal container, and
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
- **Releases are manual**, and the tag must point at the commit that built the APK. The
  procedure is in [RELEASING.md](RELEASING.md); why it stays manual is in
  [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md#releases).

## Verified on device

Not rules — facts checked on hardware, recorded because they are the kind that quietly
stop being true:

- Process death mid-workout resumes the session with its set and rest intact.
- The library renders on the very first read after `pm clear`.
- v1.1 installed over v1.0 and kept the history — dated, and now historical. Every schema step
  since is covered by the instrumented migration tests, which upgrade a real database holding
  real rows; the next manual upgrade check belongs here with the two versions it compared.
- The three-way muscle split reaches a fresh install's pickers (N75): the readiness note offers
  *Adductors* and has no *Back*, and the library reads *Lats* for a pull-up and *Upper back* for
  a row. The migration that moves an existing library is the instrumented suite's job; a fresh
  seed proves only the list.
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
