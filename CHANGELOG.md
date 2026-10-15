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
  gate — it reports 642 findings across 155 files, and 150 of those files carry an `ImportOrdering`
  finding, so a whole-file check would fail an edit that never went near an import. `tools/format-changed.py`
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

- **A measurement's day is the local day, not 86 400 seconds** (B133). The repository ended a day at
  midnight plus a fixed span, which is the next local midnight only when the day is 24 hours long. On a
  23-hour day it ran an hour past, so a save at 00:30 found and overwrote the previous day's entry; on a
  25-hour day it stopped an hour short, so a save at 23:30 missed the day's own row and wrote a second —
  both breaking the rule that a day holds one entry of each kind. The restore's collision guard was
  computed from the same bounds, so the undo could be allowed onto an occupied day or refused on an empty
  one. `localDayBounds` now ends the day at `day.plusDays(1).atStartOfDay(zone)`, beside `localDay` so the
  repository and the screen cannot disagree about which day a moment is, and the DST bounds are pinned by a
  JVM test in `BodyMeasurementDayTest`.

- **The prose the batches left describing what they removed** (B136). N125 took the next-up rows and the
  read of a program's run with them, but left comments that still describe both: an orphaned KDoc above
  home's `ProgramsPart` promising "each active program's run", and a KDoc on `planEntriesFor` citing
  `nextUpFor`, a function that no longer exists. The same reading corrects the per-site joint series'
  description — in the registry and in [DECISIONS.md](DECISIONS.md)'s N130 entry — which had the
  workout-level series reading "a session's ratings for the movement the lifter chose" when it needs no
  lift and reads the whole session, and the `PrimaryActionButton` KDoc's account of a second caller that
  went with the rows. A reference to a function that is gone is how the next reader learns to distrust the
  rest, so they are deleted or corrected rather than left to age.

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
  than left implicit — **the formatting ruleset is not wired**, because it reports ~600 findings across 155
  files, which is a repository-wide reformat or a set of `active: false` entries. The decision, the rule-by-rule
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

