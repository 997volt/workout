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
id a 1.x commit names is still looked up by it. This file carries the current release and what is
unreleased.

## [Unreleased]

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

