# Changelog

Notable changes to Workout, newest first. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions are the
`versionName` from [`version.properties`](version.properties), with the
`versionCode` in brackets because that is what Android actually compares.

Each entry says what shipped and why it mattered. The reasoning behind a decision — the
alternatives rejected, the measurements, the argument — lives in
[DECISIONS.md](DECISIONS.md) and [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md), and is not
repeated here.

## [Unreleased]

### Added

- **The progression question at *Done* can be turned off** (N66). It has opened since N50 wherever a
  plan could answer it, and it writes the plan — so a lifter who would rather the app did not edit a
  template on the way out of an exercise had no way to say so. Settings now carries *Ask about
  progression*, on by default. Off, *Done* just finishes the exercise; nothing is written to the plan.

### Changed

- **A program's screen is its days, one ⋮ each, and what a day trains is a tap away** (N72). Each slot
  row carried an arrow for each direction and a delete icon, and the delete fired on the tap; they
  are one menu now, in the same shape the workout and the template editor already use — each
  direction offered only where it exists, the removal last, coloured, and behind a question that says
  what leaves and what stays. Tapping a day's **name** opens a read-only view of what its template
  actually trains: the exercises in order, each with the sets the plan wrote, drawn with the same line
  the template editor uses and in that exercise's own unit. There is deliberately no way to edit a
  plan from there — a program uses its template, and the template is edited in the template editor —
  so the view is one question and one Close.

### Added

- **Weights can be shown and typed in pounds, app-wide or per exercise** (N64). Storage is unchanged
  — every weight is still whole grams — so this is presentation and nothing else: a stored value
  survives a switch exactly, and switching back shows the number it always did. Settings carries
  *Weight unit* (kg by default, so an upgrade changes nothing), and an exercise can set its own on its
  detail screen, which is the point: a machine that jumps in pounds reads in pounds while everything
  else reads in kilograms. Every number belonging to that exercise follows it — its logged sets, its
  plan's targets, its review line, its history — and the ± steppers move by that unit's plate pair,
  2.5 kg or 5 lb. **Statistics and body measurements stay in kilograms**: their units are the metric's
  own, chosen per series, and are not a property of an exercise. The strings that baked in `kg` now
  take the unit as an argument, so a translation can place it.

### Changed

- **A program is a schedule over templates, and nothing else** (N73). A slot used to carry its own
  prescriptions — sets, rest, cue and target effort that overrode its template's — so two slots naming
  one workout could train it differently, edited behind a pencil on every program row. A program now
  **uses** the template: the workout is seeded from it, the next set is prefilled from it, and
  accepting a progression step writes **the template**. A slot is a template and a weekday. The two
  tables that held the overrides are dropped on upgrade and what they held is discarded, which is the
  point of the change rather than a side effect; templates, planned sets and every logged workout are
  untouched. Two slots naming one template now share its plan *and* its progression — the deliberate
  reversal of P3.8's "the slot wins where it speaks" — while the last-time prefill stays that slot's
  own history.
- **A warm-up records no RPE** (N67). The set editor offered the effort field for every role, so a
  ramp could be written with a number the plan never asked of it — and read back as though the set had
  been judged against one. The field is now absent while the role is *Warm-up*, what a save writes
  drops the number even if the draft still held it, and the set's row and History stop printing one.
  Migration 28→29 clears what was already stored: every logged warm-up's RPE, and the per-set target a
  plan written before N59 still carries as its fallback. The plan's exercise-level target is left
  alone, because that one belongs to the exercise's working sets.
- **The RPE stepper has its own row, above *Log set*** (N68). It shared a row with the button that
  commits the set, so the ± pair was squeezed into whatever the button left over and the control that
  states the effort sat a thumb-width from the one that writes it. The row is full width now, its
  number is the largest thing on it and keeps a fixed column, and *Log set* is the full-width row
  below — the values it writes are still the ones above it.
- **An exercise's *Done* moved to its foot, and is withheld until a set is logged** (N69). It sat in
  the header beside the name, where it read as part of the title rather than as the last thing about
  the exercise, and it was offered before there was any work to be done with. It now comes after the
  sets and the rating, and an exercise with nothing logged has no action at all — the way past a
  movement you did not do is the overflow's *Remove*, which asks first. *Reopen* keeps the same place,
  so the foot of the block is where its state is decided either way.
- **The plan's line states how many sets are left** (N70). It only spoke once the last planned set had
  landed, so the stretch of the session the count would have been useful in — the sets still to write
  — said nothing at all. It now reads *"2 planned sets left"* while one remains and falls back to
  N52's *"Planned work done — log extra sets if you want them."* at zero, which is the state that
  sentence was written for. The count and the *Log extra set* label share one rule, so they cannot
  disagree about when the plan's work is finished.
- **A template's exercise actions live behind one ⋮, and removing one asks first** (N71). The row
  carried a *Superset with above* text link, two arrows and a delete icon; the live workout had
  already moved the same actions into a per-exercise overflow (N53). The template now draws that same
  menu — order, then pairing, then the destructive entry last and coloured — and it is **one
  component** used by both screens rather than a second copy, so the two cannot drift. Removal keeps
  the workout's guard: the planned sets go with the row and there is no undo to reach for, so it is a
  question rather than a tap.

### Removed

- **The workout screen's "Last time" line is gone** (N65). It printed the *first* set of the previous
  session while the next-set fields prefill from the *last* one, so the line and the fields under it
  routinely disagreed about the same workout, and neither said which set it was quoting. The prefill
  already answers "what should this set be" (P1.3, N59) with the number the fields actually start from,
  which is where the answer belongs; a second, differently-chosen quote beside it was noise that could
  be wrong.

## [1.13] — 2026-10-05 (versionCode 14)

### Added

- **The joint rating is picked from the body's joints, left and right apart, each with its own score**
  (N63). "Which joints" was free text and the pain was one number: right for a note, wrong for a fact
  the trend is asked about. The per-exercise rating now offers the body's joints — shoulder, elbow,
  wrist, hip, knee, ankle, and the central neck and lower back — as separate left and right entries
  where the joint is paired, each picked one carrying its own 1–10 (a new pick starts at 1), and a save
  replaces the list. The single *Joint pain (1–10)* field and its note box are gone; a session rated
  before the change keeps its number and its free text, which history still reads **until it is rated
  again — a new rating retires them**, so a list the lifter cleared stays cleared rather than falling
  back to the number it replaced (the dialog states the old rating so that replacement is visible). The
  joint-pain trend reads the **worst** joint rather than an average of two sides. The picked rows ride
  in the backup with the exercise they describe, and the readiness note's sore-muscle list and this one
  now share a single editor.
- **The readiness note records which muscles are still sore, and how sore** (N62). The note was one
  free-text line — the right shape for "slept badly" and the wrong one for "quads 8, calves 3". The prompt
  a new workout opens with now offers the taxonomy's own muscle groups beside the note, each picked one
  carrying its own 1–10 (a new pick starts at 1), and a save replaces the list so a muscle taken off
  it is gone rather than merged with the old rows. The note itself is unchanged and the whole capture
  is still skippable; the rows ride in the backup with the session they describe, and history draws
  them under the readiness line.

### Changed

- **The next set is stated on the workout screen, not behind *Log set*** (N59). N51 had put the set
  editor in front of every set so a set that differed from the prefill was corrected before it was
  written; the cost was a dialog between every set and a button whose label could not describe what it
  wrote. The values the plan and history prefill are now the exercise block's own fields — weight,
  reps and the role picker — so they are read and changed before anything is committed, and *Log set*
  is a button beside them that writes exactly what is on screen. B7's display-agrees-with-storage is
  back on this path, and the editor is what correcting an already-logged set still opens. The fields
  are keyed on the logged-set count, so a write re-arms them from the plan at the position the next set
  occupies (B48) — N19's "a role is one set's decision" holding without a dialog.
- **The app's progression proposal is withdrawn** (N33, N22). With the next set's values visible and
  editable before the write, a separate proposal to accept had nothing left to add, so the *Use it*
  link and the double-progression rule that computed it are gone; what the fields start from is what
  was done last time, unchanged. N50 has since put a proposal back, at *Done* (the entry below), and the
  warm-up ramp keeps the loadable step the rule had defined (N28).
- **The rest field's "Empty for the default, 0 for none." hint is gone but for one field** (N60). It sat
  under the template editor's rest field and under a program slot's as well as under the exercise's, and
  the two plan editors *restate* a rest rather than define one, so there the sentence was noise read on
  every edit. It stays on the exercise's own rest field, which is where a rest is defined and where the
  rule belongs.
- **Home's start bar is one stack, and a program's next run is the last thing on it** (N61). The bar read
  *Next up*, the *Programs* / *Start from template* links, then *Start workout*, so the app's own
  suggestion came before the choice of how to begin and the next-up start was the smallest target on the
  screen. It now reads, top to bottom: *Programs* beside *Templates* (the template list the old link
  already opened), the empty start — renamed *Start empty workout* and drawn on the deep indigo
  container rather than the accent, so the two full-width pills do not read as the same action and the
  planned one is the app's suggestion — and last the next-up block at the bottom edge, set a size
  larger than before, whose *Start* grew from a text button into the same full-width pill and reads
  *Start planned workout*. N55's split is untouched: the field still opens the read-only planned-workout
  dialog and the pill beside it starts the workout, and one pill is drawn per next-up row because more
  than one program may be active (P3.12).
- **What the app asks when an exercise is done is progression, and the answer changes the plan** (N50).
  N59 withdrew the app's proposal but left two questions open — where a proposal belongs, and what earns
  one — and this answers both. *Done* opens a **progression prompt** that states what the plan asked and
  what was done and, where the plan was answered with room in hand, offers the next step as the lifter's
  choice: the smallest loadable step, or a rep — and **only a rep where the plan names no added
  weight**, because an assisted set's number is the machine's help and a bodyweight set's is zero, so
  neither has a load a step can raise. **Where there is no plan there is no prompt at all** —
  a next step is something only a plan can ask — so *Done* finishes the exercise and nothing else.
  **Earned** is narrow on purpose — the exercise came from a plan, every prescribed working set carried
  a target RPE, and each was performed with its reps
  met at or under that RPE; warm-ups are excluded, and an unrated session suggests nothing rather than
  guessing. "Every prescribed working set" is the plan the screen showed: a slot's prescription wins
  where it speaks and the template answers the rest, so a slot overriding one of three sets still has
  its other two checked and its own RPE applied to them. Accepting writes the **plan** — the slot's
  prescription for a program start, the template's planned set for a direct one — because the plan is
  what the next run reads, and the app still writes only what the lifter accepts; the set's own legacy
  RPE travels back with it rather than the exercise's number the rule read, so an effort cleared from
  the plan later is not resurrected.
- **Muscle feel is a stepped number, and it opens on 7** (N8). It was the last typed number on the
  rating dialog, and it is one value on a ten-point scale: it is now the same −/+ stepper the picked
  soreness and joint lists use, starting at 7 — a set worked hard without being taken to failure —
  rather than blank, so *Save* always carries the number the dialog shows. The ends are still named
  beneath it (N12).
- **A set's RPE is a stepped number, prefilled from the plan** (N6, N59). It was the workout screen's
  last typed field, and it was left blank on purpose — N59 showed the plan's target *beside* it rather
  than writing a target into a record of what happened. The lifter now reads the plan's own RPE in the
  field and changes it if the set felt different; with no plan the workout screen's stepper opens on 9.
  Halves step half a point at a time and stop at 1 and 10, so a set logged from that screen always
  carries an RPE — and the set's own line says it back, because effort is part of what a set was. The
  caption under the field names the plan's target where there is one, and is absent otherwise. The plan
  editors keep their text field: there the RPE is a target being authored, not a set being recorded.
  **Correcting an already-logged set invents nothing**: a set that recorded no effort shows *Not
  recorded*, keeps that on save, and states the default on the first tap of either button.
- **A plan's RPE is one number per exercise, not one per planned set** (N14, N59). Authoring a plan
  meant typing an effort into every set, and the plan was saying the same thing each time; the target
  now sits beside the rest and cue the exercise already carries, in both the template editor and a
  program slot's prescription, while a *logged* set keeps its own RPE — a plan asks for an effort, and
  the set records what it actually felt like. Migration 27→28 adds the columns and seeds each exercise
  from the last set that named one; the per-set columns stay, so a plan set through a set's dialog
  keeps its value and a backup written before the change still restores whole. A reader prefers the
  exercise's number and falls back to the set's, so an old file behaves as it did — and clearing the
  exercise's one field clears the legacy per-set values with it, or the fallback would resurrect the
  effort the lifter just removed; a plan whose effort only ever lived on its sets (an imported
  pre-change backup) is left alone.
- **The rating is opened by the lifter, never handed to them** (N8, N50). *Done* used to end in *How did
  that feel?* — with a plan, one action inside the progression prompt; without one, as the prompt
  itself. Both are gone: the exercise's own rating row is the only way in, so finishing an exercise
  never asks how it felt. The dialog has one title and one way out now, because there is no prompt
  wording left to vary.

### Fixed

- **A set logged from the screen keeps the RPE it was given** (no feature id). The inline fields are
  stated before *Log set*, but the write path had no parameter for the effort, so what the stepper
  showed never reached the row — and *Undo* on a deleted set dropped the effort and the comment as it
  put the row back. The log path now takes both, required rather than defaulted so a caller cannot lose
  them by forgetting. A comment is given from the set's own row rather than on this path, so "the
  comment it was given" is what the edit and the undo carry.

### Removed

- **The library's overflow menu is gone, and with it the *Workout history* link** (no feature id). The
  link was the menu's only entry, and History is the tab beside this screen — drawn in the bar on every
  library screen — so it was a second door to a room one tap away. B1 had already taken export and import
  out of the menu, which is why nothing was left to keep it for.

## [1.12] — 2026-10-04 (versionCode 13)

### Added

- **A program's *Next up* moved into the bottom bar and opens what is planned** (N55). The next-up card
  sat in the scrolling list between today's plans and *Recent*, so a program with nothing scheduled
  today was something to scroll to, and the only thing in it that responded was *Start*. It now sits in
  the bottom bar above the start pill — the edge of the screen the thumb is already at — and leaves the
  list, because one program's next run shown twice was two answers to one question. The field is its
  own target and *Start* is a separate one beside it: tapping the field opens what is planned, and
  tapping *Start* starts it, so looking and starting stopped being the same gesture. What it opens is a
  read-only dialog of the workout's ordered exercises rather than the template's editor, which is the
  only destination a template had: the question is "what is in this workout", and opening the editor to
  answer it would put every target one mis-tap from being rewritten on the way to reading it. The row
  stays compact rather than a card, because more than one active program (P3.12) means the bar may
  carry several.
- **A running workout's exercises can be reordered** (N54). Order matters mid-session — a rack taken,
  equipment moved — and the only way to change it was editing the template, which rewrote every future
  run for a reason that belonged to one afternoon. Each exercise's own ⋮ menu (N53) offers *Move up*
  or *Move down*, whichever direction exists — the list knows which row it is drawing, so the entry
  that would write nothing is simply not offered, the shape B28's row-0 exclusion already uses — and
  the order written is the **session's**: the template is never touched, which is N16's "a session
  reads it at the start" applied to order rather than to targets. The order is persisted as it changes,
  so it survives a process death and a repeat copies the session's own order rather than the plan's.
  The two rows' positions swap in one transaction (B27's rule about a write that must not half-land),
  and a move with nowhere to go is still a no-op reported as success rather than a failure the user
  cannot act on. A superset member moves as itself, which keeps a round adjacent because adjacency is
  what the grouping means. The plan follows the **exercise** rather than the slot it now occupies, so a
  moved row reads its own targets — and its own count for whether the planned work is done (N52) —
  instead of its new neighbour's.

### Changed

- **A history row leads with the weekday, and home's Recent rows with it** (N57). The headline is
  `Sun, Oct 4, 2026`: the short weekday and then the `MEDIUM` date the row already showed. The weekday
  is the part a lifter navigates by — which day of the week this was — while the month heading above
  the row already carries the month the date repeats, which is why the short form is enough. It is
  built from the locale's own names (`TextStyle.SHORT`, the way the month heading uses `TextStyle.FULL`)
  rather than a hard-coded English pattern, and read in the session's own zone (N25, B38). One
  formatter serves both surfaces, so a finished workout reads the same way wherever it is listed.
- **A workout started from a template says which one in history** (N58). The session already stored
  `templateId` — a program's slot included — but the history projection never selected it, so a
  finished *Push A* and a finished empty workout were indistinguishable in the list. The template's
  name now joins the supporting line beside the duration, sets and volume, and that line **wraps**
  rather than truncating — as it already did, which is what lets a fourth item be read in full: four
  items are more than the line holds on a phone, and an ellipsis on the one part that cannot be
  inferred from the workout would hide exactly what the change is for. The name is read
  **live** from the template row, so renaming a template relabels the past — accepted, because N16's
  template is living and the workout's own identity is when it happened, which the headline carries.
  That read also answers deletion: `deleteTemplate` is a soft delete, so the name is still there and a
  past workout goes on saying which workout it was. Snapshotting the name onto the session was rejected
  — it costs a column and a migration and changes only what a *rename* does. A **repeat** now carries
  its source's template id, so a repeated *Push A* is still a Push A in history rather than an unnamed
  session; the targets are still not copied with it, which is provenance rather than prescription.

- **One exercise's rare actions live in its own ⋮ menu** (N53). *Superset with above* was a text link in
  every exercise header and *Delete* an icon beside *Done*; both are rarely used and the header is read
  constantly mid-session, so the two of them cost more attention than they earned. They moved into a
  per-exercise overflow — the shape the workout-level actions used until N42 removed the one that no
  longer had a reason to exist. Nothing about either changed: Delete keeps its confirmation (B2), and
  pairing keeps its row-0 exclusion and its absence on a done exercise (B28, N7), because the exclusion
  is now the *entry* not being offered rather than a control that writes nothing.
- **Logging a set is the editor, prefilled and committed on Save** (N51). *Log set* wrote the offered
  set in one tap, so a set that differed from the prefill was logged and then edited — the same
  `SetEditorDialog`, one step later, with the first step having decided something the user did not
  mean. Logging *is* that dialog now, and the one-tap path is gone rather than kept beside it: the
  button no longer *writes* the values it names, though it still reads them and the dialog opens on
  them, its role picker moved inside the dialog with it, and the plan's next unlogged set is what the
  dialog opens on (B48). That retires B7 for this button — it no longer
  writes the set its label describes, because the label no longer describes one — and N19's rule is
  unchanged by the move: a role is still a decision about one set, made before the write, and it still
  clears itself.
- **An exercise says when its planned work is done** (N52). Past the last planned set the exercise
  kept accepting sets with nothing to say the work the plan asked for was finished —
  `comparePlanToActual` said so only in the review, after *Finish*. The plan is the template the
  workout was started from, so the moment its last set is written *Log set* becomes **Log extra
  set** and a notice says the planned work is done. Nothing closes: logging an extra set is what
  the control still does, the exercise stays open, and the way to end it is the **Done** already in
  its header (N7). It is per exercise and it is the control rather than a dialog, because N51
  already puts a dialog in front of every set and a second one would interrupt the next exercise's
  first set. An exercise with no plan behind it — an empty workout, or one added by hand — is never
  called done.
- **The palette's second filled role and its links were replaced, so both read** (N49). *Log set* is a
  filled tonal button, which draws its label in `onSecondaryContainer` on `secondaryContainer`; that
  container was the category Teal, and white on it measures 2.9:1 — the loudest thing on the workout
  screen carrying the least readable label. The links beside it are `TextButton`s, whose label is
  `primary` — Indigo at 4.07:1 against the page and 3.77:1 on a raised card, under the 4.5:1
  body-size text needs. The container changed in the palette: it is now the
  same hue taken down to a surface (`TealDeep`), so white on it reads at 5.94:1. The links did not —
  a link's colour is `primary`, which is also what *fills* the Start pill, the selected tab and the
  chips, and one role cannot be both — so every link in the app now draws through `AppTextButton`,
  which carries the link colour (`IndigoLink`: 8.85:1 on the page, 8.22:1 on a raised card). The rest
  bar draws its links on the filled container itself, so those use `onSecondaryContainer` instead.
  Teal itself stays the category accent a row's tile wears. `PaletteContrastTest` asserts the pairs
  this app draws, and scans the sources so a bare `TextButton` cannot put the fill colour back under
  a label, the way `TileAccentTest` already holds the tiles (B55).

### Removed

- **A template carries no weekday; the day belongs to a program's slot** (N56). A template is a
  reusable workout, and it also held an N16 weekday pin of its own — its editor offered a day picker,
  `templates.weekday` stored it, and *Today* on home fell back to those pins whenever no program was
  active. That was two places answering "what am I doing on Tuesday", and the template's copy was the
  weaker one: a template has no order, no next-up and no adherence to belong to. The pin goes, with its
  column (v25 rebuilds the table and copies the columns that survive), the `setWeekday` path through
  DAO, repository and editor, the picker, and the `pinnedFor` fallback — leaving a program's slots as
  the only source of a dated plan. With no active program there is then no *Today* list, which is the
  point rather than a regression: a day is a scheduling fact, and scheduling is what a program is for.
  The loss is accepted rather than mitigated — a template pinned to a day today comes out of the
  migration with no day at all, and getting the schedule back means putting it in a program — which is
  the rule being stated rather than a migration that failed. The backup carries the same field, and it
  goes with the column: the codec ignores keys this build does not know, so a file written before the
  change still decodes and the pin is simply not read.

## [1.11] — 2026-10-04 (versionCode 12)

### Changed

- **The app has one dark theme, and the five tabs are drawn in it.** The look is taken from
  M.E. Me, the patient-held health record the reference screenshots came from: a near-black
  page, cards separated by *tone* rather than by shadow, a solid coloured square leading each
  row, a wide violet pill for the primary action, and screen titles set as tracked small
  capitals above content that is left to be the loudest thing on the screen.
  `dynamicColor` is gone rather than defaulted — a scheme read from the launcher's wallpaper
  cannot produce this palette, and it hands the app's identity to a setting the user chose for
  a different reason. `AndroidAppTheme` now takes no arguments, because the `darkTheme` and
  `dynamicColor` pair picked between four schemes and there is now one.
- **The window moved with the theme.** `Theme.AndroidApp` is a dark parent with an explicit
  `windowBackground`, and `enableEdgeToEdge` is given dark system-bar styles. Both are load-
  bearing on a light-mode device: without the first, every cold start flashes white, and
  without the second the no-argument form reads the *system* light/dark setting and draws dark
  status-bar icons onto near-black bars, so the clock and the battery vanish.
- **Home's primary action is a full-width pill in a bottom bar**, not an extended floating
  button, and the tab bar separates from the page by tone with an accent wash behind the
  selected item.
- **A rest of zero is a valid, deliberate answer** (N45). The rest field had two states and needs
  three — empty inherits, a positive number is that rest, and zero means the exercise has none —
  where zero used to be refused in three repositories with two different sentences for one rule, and
  shown as a red field in the library editor, the template plan editor and the slot prescription
  dialog. The floor moves to zero, the three sentences collapse into one, and the hint under every
  rest field now carries both halves. Nothing downstream changed: `startRest` already computes an
  end instant at *now*, so a zero rest never runs. A stored zero now displays as **None** rather
  than `0:00`, which read as a rest that had run out. The **default** rest keeps its 5–3600 bound:
  an app-wide zero would take the rest out of every exercise at once, which is the new switch's job
  (N44).
- **Duplicate is gone, and Add set starts from the last planned set** (N46). The *Planned sets*
  dialog offered a blank Add set beside Duplicate, which appended a copy of every set the exercise
  already had — a loop wearing a button, since it doubles (1, 2, 4, 8) and leaves the odd counts to
  manual adds. Both authoring surfaces now take the prefill, so a plan is extended by confirming
  rather than retyping and any count is reachable: the template's plan dialog reads the last set in
  local scope, and the slot's prescription dialog looks it up through the exercise's prescription.
  The role prefills too, which is safe because Add warm-ups *prepends* — the last set is the last
  working set. The whole path goes with the button: the repository method, its DAO read, its tag and
  string, and the test that only exercised it.
- **The workouts tab answers one question and gets out of the way** (N42). "See all workouts"
  is gone from the *Recent* heading — History is the tab beside it, one tap away and always
  visible, so a section heading carrying a way out of its own section was a second path to
  somewhere the app already goes. "Repeat last workout" gives its slot in the action row to
  *Programs*, the screen the scheduling half is edited from, which the overflow menu had got
  lost in; and the overflow itself is gone, because with Programs out of it, it held only
  actions on the whole database rather than on a workout.

### Added

- **A program, with its templates, can be carried to another device as a file** (N47). Programs and
  their templates already rode in the backup, but there was no way to hand one to somebody or move
  one without moving the whole database — import merges a backup rather than accepting a program.
  The document is its own format with a **version of its own** (a new backup column does not make an
  old program file wrong), reusing the backup's DTOs because they are already the raw-storage shape
  the mappers speak. It carries the program's *definition* — slots, the templates they name, their
  planned exercises and sets, and what each slot prescribes — and deliberately not history: skips,
  deload weeks and substitutions belong to the device that trained them. Every exercise a template
  names travels **whole**, because a user's own exercise id means nothing elsewhere; the receiver
  creates whatever it does not already have. Loading merges and overwrites nothing, so the same file
  twice is a no-op, and the imported program arrives inactive at the end of the list, because
  following a program is a choice rather than something a file makes. A movement whose exercise is
  neither carried nor present is skipped, and the rest of the program still arrives. Export is per
  program in its editor; *Load* is on the Programs list, both through the Storage Access Framework,
  so the app still declares no permissions.
- **Settings has a Data section** (N43): export, import and delete-everything, moved from the
  home overflow, each keeping what it does today. They act on the whole database, which is what
  Settings is about; delete-everything stays last and coloured, behind the same typed
  confirmation, because it is still the one entry that can cost the user something.
- **The rest timer can be switched off, without losing the prescription** (N44). Settings gains
  *Count the rest down*: on it behaves as before, and off the workout screen shows each exercise's
  own rest as a fixed label — falling back to the default rest when it has none — with no
  countdown, no ±15s controls and no chime. Off is genuinely off rather than a countdown hidden
  behind a static number: the end instant is never written while the switch is off, and a rest
  already running is cleared when it goes off. It is a preference about the timer, not a fact about
  the exercise, which is why it is not the same thing as a rest of zero (N45).
- **A finished workout can be repeated from its History row** (N48). This is the entry point the
  home screen's "Repeat last workout" button gave up, addressed to the workout the row names rather
  than to "the last one": the copy is that workout's exercises and order (never its loads), and the
  action is offered only where there is still something to copy. A repeat is a start, so it goes
  through the program's missed-day question like every other one, and the repository names the
  workout it is given — a row deleted since is reported rather than opening a blank session.
- **An ongoing workout can be discarded, behind a prompt** (N41). A workout holding logged sets
  previously had no exit but *Finish*, which files it in history. The workout screen's top bar now
  offers *Discard workout*, and the prompt says what goes — how many sets are logged — and, when
  the workout came from a program slot, the second consequence too: only a finished session settles
  a scheduled occurrence (P3.5), so dropping out is recorded as a miss rather than as no workout at
  all. The empty workout keeps its prompt-free discard, because there is nothing to lose.
- **`AppCard`, `AppRow`, `IconTile`, `SectionHeader`, `TopBarTitle` and `AppFilterChip`** are
  the shared pieces the restyle is built from. `AppRow` is deliberately built on Material's
  `ListItem` rather than a hand-rolled `Row`: the drawn result would be the same and the
  semantics would not, because `ListItem` is what tells a screen reader that a headline and
  its supporting line are one item.
- **Row-navigation now carries an `onClickLabel`** — "Open workout", "Open Back Squat". A row's
  headline names the *thing* and never the action, so without one the control announced what it
  was and not what it would do. This is the per-screen accessibility rule the project already
  states, applied to the rows the restyle created.

### Fixed

- **A template's planned warm-ups are armed as warm-ups when the workout is run** (B48). The
  pending set's role now follows the plan's next unlogged set, carried the way its reps and weight
  already were, so a template that opens with a ramp records warm-ups as warm-ups rather than as
  working sets. That mattered beyond tidiness: warm-ups are excluded from records and progression
  on purpose, so a ramp recorded as working could inflate volume and set a personal record against
  a bar nobody cleared. The picker beside the button still overrides the role for the one set it is
  about to log.
- **"Add warm-ups" writes the ramp it offers** (B49). The template editor's route never passed the
  action to the screen, so the button fell back to a default empty lambda and a tap wrote nothing,
  reported nothing and changed nothing — while the ViewModel method underneath was implemented and
  tested, which is why it shipped green. The callback's default is gone rather than kept, so the
  next forgotten wire-up is a compile error instead of a silent no-op.
- **"Add warm-ups" is offered exactly when a ramp can be built** (B50). One predicate now decides
  the guard and the action together, where the guard had asked only whether a weight was *typed*:
  an assisted set (stored as `0` kg) and a working weight too light to load a step below both
  offered a button that then returned success without writing. The shared predicate makes that
  silent path unreachable, and the repository's own failure is no longer swallowed.
- **A loaded program is honest about an exercise this device has deleted** (B51). The load asked
  whether an exercise was present with a query that filters soft-deleted rows, while the insert
  beside it ignored an id that already existed — so a lift the user deleted here was skipped *and*
  judged absent: its movements were dropped and reported as exercises "not on this device", which is
  untrue, because the device has the row and is hiding it. Dropping stays the answer, since
  un-deleting a lift is a write to the library that "overwrites nothing" refuses to make; the
  sentence now reads "not in your library", which is true whether the row is absent or hidden.
- **A loaded prescription must be one its template actually trains** (B56). The interactive writes
  enforce P3.8 through `requireExerciseInTemplate`, but the import writes raw rows — so a slot whose
  template id already existed on the receiving device, and had since diverged, could be given a
  prescription for a movement that template no longer holds, which no screen would ever show. The
  load now reads what each template trains, after the carried exercises have landed.
- **The program document's export and load are required callbacks** (B52). Both were
  `(() -> Unit)? = null`, so a route that forgot one drew no control and failed nothing — the shape
  B49 was recorded against, re-introduced by the change that recorded it. The two screens now
  require them, so the next forgotten wire-up is a compile error.
- **The Storage Access Framework's document IO is written once** (B53). The backup and the program
  document each carried their own read, write, MIME type and `DataError`-to-sentence mapping, which
  is two copies of a rule about cancellation, free to drift. They share `ui/transfer/DocumentFiles`.
- **The new shared components lost three parameters nothing passed** (B54): `SectionHeader`'s
  trailing slot and test tag, and `IconTile`'s content description. The slot was the vestige of
  "See all workouts", which N42 removed from the heading it sat on.
- **An icon tile's glyph reads on the accent behind it** (B55). Every glyph was white, which is
  2.5:1 on `Amber`, 2.9:1 on `Teal` and 3.0:1 on `Coral` — under the contrast a graphic needs, and a
  tile whose glyph cannot be read has stopped doing its one job. Each accent names an `onColor` now,
  asserted at 3:1 or better by a test rather than eyeballed.
- **A load that added only templates no longer says the program was already here** (B57). The
  sentence asked the program count alone, and a program whose id is already present can still bring
  templates the device did not have.
- **A negative rest is refused in one sentence, in one place** (B58). The refusal repeated the hint
  that sits under every rest field, so one instruction lived in two files; the constant now says
  only what is wrong and the hint says what to do.
- **The statistics title no longer collides with its actions.** `CenterAlignedTopAppBar` gives the
  centred title and its trailing actions the same pixels once the actions are word-length, so
  "STATISTICS" ran into "Adherence" and the title lost. Both actions are icon buttons now, which is
  what a bar with a centred title can hold; the words move into `contentDescription`, where a screen
  reader was already reading them, and both keep their test tags.

## [1.10] — 2026-10-03 (versionCode 11)

### Added

- **More than one program can be active at once, and a program's place is authored** (P3.12,
  amending P3.3; schema v21). `programs.isActive` is no longer exclusive, so a lifting block and
  a conditioning one are two schedules at once rather than one edited to hold both. Home's today
  plan is the **union** of every active program's slots for the day, the missed-day question
  walks every active program's pending occurrences, and adherence scores them together — the
  per-program breakdown is deliberately left to P3.14. `programs.position` gives the list and
  the union an order moved by hand (`programs` are reordered in the list), so which active
  program comes first is a decision rather than an accident of its name or when it was created.
  The upgrades are additive: the rows already there take their `rowid` as the position, and a
  program that was active stays active.
- **A program slot prescribes its own intensity, schema v22** (P3.8). A slot that names only a
  template is a schedule — the numbers come from the template, so two slots pointing at one
  template cannot train it differently. A slot now carries a prescription of its own: per exercise,
  sets × reps at a load, at a **percentage of the estimated 1RM**, or up to an RPE, with the rest
  and cue a plan already has. An empty prescription leaves the template's targets standing (N14)
  and every target stays nullable, because a target is not a claim.
- **A percentage is derived, never assumed.** The kilograms are N17's Epley estimate of the
  exercise's heaviest working set, rounded to the loadable step; an exercise with nothing estimable
  has **no number**, and history prefills the load rather than the app borrowing one. A weight the
  slot writes wins over its own percentage.
- **Progression is offered per slot, from that slot's own history** (N22, N33): a heavy Monday and a
  light Friday pointing at one template progress apart, because which slot a session settled is
  P3.3's occurrence matching. Nothing is written unless the lifter accepts it.
- **The slot travels as a start argument, not a stored fact.** Home passes the slot it is starting,
  which seeds the session's rest, cue and targets; the session still records only the template
  (P3.3), so provenance and matching are unchanged. The prescribed rows join the backup codec, with
  the round trip guarded by a field-for-field test.
- **A program's order is a run, and the app keeps the place** (P3.9). P3.3's order was real in the
  editor and nowhere else: home asked each slot for its weekday, so an order-only slot was never
  surfaced and "which one is next" had no answer on any screen. The run now follows the last slot
  **trained or consciously skipped**, derived from finished sessions and recorded skips rather than
  a stored cursor — so a day simply missed leaves it where it is (that is the missed-day question's
  job) and an edited program re-derives its place. A program with no weekdays runs A → B → C,
  because the cursor, not a day, attributes an order-only session to its slot. Home shows a
  **next up** row for a program with nothing scheduled today, and the editor marks the slot the run
  is at. Rotation is by what was done, never by load.
- **A deload is a week the lifter marks, schema v23** (P3.10). It is an event keyed by **program and
  week** — the shape of a skip — never a dated plan and never a week the app computes, because a
  deload is decided by how the block is going. **Exempt from the ratio, not from the calendar**: a
  deload week's scheduled occurrences count as neither done, skipped nor missed, so a deliberate
  back-off cannot read as a failure, while its sessions still mark their days and the missed-day
  question still asks. Nothing is scaled for you: what a deload week prescribes is what the slot
  prescribes (P3.8). Weeks are marked and unmarked on the Adherence screen, one chip per active
  program, and only a week that has started can be marked — the app has no forward view to hang a
  future week on. The new rows join the backup codec, guarded by a round trip.
- **A workout can be substituted for one occurrence, schema v24** (P3.11). "The rack is taken
  today, do the dumbbell version" cannot be answered by editing the program, which changes every
  week that references the template (N16, inherited by P3.3), so it is an **event keyed by slot and
  week** — the shape of a skip. The pick is made **at the point of starting**, from the slot's row
  in today's plan, and recorded before the session opens; no other week changes, which is the whole
  point, and the scheduled workout is offered to clear a mis-pick. A session started from the
  substitute **settles the slot's occurrence** — P3.3's matching extends to it, so the app stops
  asking about a day already trained — and adherence scores it against the slot: the day was
  scheduled and it was done, whatever it was done with. The new rows join the backup codec.

- **A skip can be corrected by hand** (P3.13). P3.3's prompt was the only thing that ever recorded
  a skip, and *Continue* silences a whole week in one tap, so a mis-tap was wrong forever and a
  week the app never asked about read as misses it did not earn. Tapping a **scheduled day** on the
  Adherence calendar now opens what that day scheduled — one row per occurrence, with its state —
  and each row can be marked skipped or unmarked, using the same `program_skips` row. Adding looks
  **backwards only** (a day passed over is behind you), removing is always allowed and soft-deletes,
  and the day returns to done, missed or pending by P3.5's same definitions. A finished session is
  not correctable — it is the record — and a deload week offers nothing to correct. The app never
  removes a skip itself: the correction is a second, explicit writer rather than a second opinion.
  No schema change.

- **The month is broken down, per day and per lift** (P3.14). A month's ratio says the program is at
  70%; it does not say that the squat day is what keeps being skipped. The same aggregate is now
  read **per slot** — each slot's own done / skipped / missed, accumulated in the same pass as the
  totals so the parts sum to the whole by construction — and **per lift**, over the join N14 already
  has: an exercise gets the occurrences of the slots whose template prescribes it, which answers "am
  I skipping this lift, or this day". A lift trained by two slots is counted in both on purpose.
  Counts, not a per-row percentage. No schema change.

- **A streak of scheduled work, with where it started** (P3.15). The obvious version — consecutive
  calendar days — is wrong here: it would break on every rest day, and by P3.5's rule an unscheduled
  day *is* rest. So the streak counts **scheduled occurrences, not days**, walking back from the
  most recent elapsed one while each was done. A skip and a miss both break it; an unscheduled day
  is invisible to it; a deload week neither extends nor breaks it, because it is not scored (P3.10);
  and an untrained today is stepped over rather than counted as a break. It is shown as a number
  **with its start** and never as a nudge. No schema change.

- **The ratio gains a history, a point per month** (P3.16). A month is the right grid and too short
  a judgement: a block is four to six weeks, so a change that took one reads as one flat month after
  another. Adherence now charts **twelve months** of the ratio beneath the month grid — the same
  aggregate, evaluated once per month over one wider read, so **a point and the grid it came from
  cannot disagree**. A month with nothing scored is a **gap in the line, not a point at zero**:
  nothing was scored there, it did not fail. The window is deliberately not the statistics screen's
  range (N21), which would make one number mean two things. No schema change.

### Fixed

- **A restore no longer fails on a program's slot** (no feature id). The import inserted the program
  tables before the templates they name, so a restore into a fresh install — the one case the backup
  exists for — hit `FOREIGN KEY constraint failed` on `program_slots.templateId` and rolled the
  **whole** import back, leaving an empty database. The two halves are now ordered parent-first
  across the boundary (`insertMissing` before `importPrograms`), and the round-trip test asserts the
  import succeeded rather than reading an empty table back. The ordering predates this round — it
  shipped with P3.3 at v1.9 — and this round is the first coverage of it.
- **A restored program list can be reordered again** (P3.12). A file written before the order existed
  carries no `position`, so every program came back at 0; `moveProgram` swapped two equal values and
  the up/down controls did nothing, permanently. A move now re-numbers the whole list from the
  displayed order, which repairs the ties on the first move.
- **A slot's prescribed set no longer erases the template's target with nulls** (P3.8). A set that
  wrote only a note, or only reps, left the template's numbers unreachable because the slot's target
  shadowed it. The slot now wins field by field, with the load still taken whole from one side or the
  other so a slot's kilograms cannot be added to a template's assistance (N15).
- **A substitution is recorded against the week the tapped row was drawn for** (P3.11), not against
  whatever day the clock had reached, so a tap on Sunday's plan can no longer be keyed to Monday's
  week.

## [1.9] — 2026-10-03 (versionCode 10)

### Added

- **Adherence: how often the days a program scheduled actually happened, and a month of days
  trained** (P3.5). A destination of its own, reached from Statistics the way Measurements is —
  N34's five surfaces stand, because one screen does not earn a sixth tab. **One window**: the
  month the grid draws is the month the ratio covers, so the number and the grid cannot disagree
  about what they are counting, and the calendar navigates back through history and forward no
  further than the current month. Days after today are drawn but never scored.
- **Done, skipped and missed come from one set of definitions, and the unit is the occurrence,
  not the day.** *Done* is an occurrence settled by a session that was **finished**, matched by
  template and date in a Monday-start week taken in the session's own zone — P3.3's matching,
  fed this window's finished sessions. *Skipped* is a `program_skips` row for that slot and week.
  *Missed* is elapsed and scheduled and neither of the others. Two slots can fall on one Tuesday,
  so the ratio counts two while the calendar marks the one day. Adherence is deliberately
  **stricter than the prompt**: an abandoned start is a miss here and marks no trained day.
- **What was scheduled is the active program's weekday slots**, so an unscheduled day is rest
  rather than a miss and a weekday-less slot has no day to miss. A **trained day is a finished
  session's own day in the zone it was performed in** (N25, B45), so a workout done abroad marks
  the day it happened. With **no active program** the calendar still marks the days trained —
  that needs no schedule — but there is **no ratio**, because the pins home falls back to carry
  no skip record, which would make every deliberate rest on a pinned day a failure. A month with
  no elapsed scheduled day says so rather than reporting 0% or 100%.
- **No schema change.** Everything it reads shipped with P3.3 at v20 — `program_slots`,
  `program_skips` and `workout_sessions`' `templateId`, `finishedAt` and `zoneOffsetMinutes` —
  and the aggregate is the pure, JVM-tested `ProgramSchedule` the prompt already uses.

  Known limits, inherited rather than new: skips exist only from P3.3 onward and only for a week
  whose prompt was answered, so an older or unanswered week reads as misses; the schedule is the
  program as it is *now*, so adding a slot writes misses into weeks already past.

- **Programs: an ordered list of templates, each with a weekday, schema v20** (P3.3). A program
  is the container the N16 pins could not be on their own — a pin answers "what happens on a
  Tuesday", but nothing ordered the pins against each other, so "which one is next" had no
  answer. A **slot** is a template plus an optional weekday; a weekday-less slot is order-only
  and can never be missed. **Today's plan comes from the slot pinned to today**; with no program
  active, home falls back to the template pins it already read.
- **The missed day is asked about at the point of starting, not assumed and not asked at
  launch.** Starting a workout with a scheduled day this week neither trained nor skipped asks
  *"You missed Paused Squat on Tuesday. Do it now, or continue with Bench?"* — **Do it now**
  starts that slot, **Continue** records a skip for **every** pending occurrence that week, so
  two misses are not two interrogations. Only days already behind you count: a Friday slot on a
  Wednesday has not been missed yet.
- **A skip is an event keyed by slot and week** (`program_skips`, storing the Monday as an epoch
  day), never a boolean on the slot — a flag would need resetting and would be wrong the moment
  two weeks in a row were missed. Those rows are also what P3.5's adherence needs, because a
  standing weekday pin carries no history.
- **A session records the template it was started from** — one nullable `templateId` on
  `workout_sessions`, written only when the session is created from a template, so a resumed
  session never rewrites it. It amends N16 deliberately: N16 rejected copying a plan's *targets*
  onto a session because that freezes what the plan prescribes, while recording *where a session
  came from* freezes nothing. An occurrence is matched by template and date, in a Monday-start
  week taken in the session's own zone (N25).
- **The schedule, its slots and its skips ride in the backup**, and the new column joined the
  codec in the same change with a round-trip test behind it (N24's rule).

  Known limits, stated rather than discovered: an occurrence is resolved only when the workout
  was *started from* that template, so a plan added by hand does not settle it; a second session
  from the same template in one week is unmatched; and editing a template changes every week
  that references it, which is N16's living-template decision inherited rather than new.

### Changed

- **The dead code the statistics round left behind is gone, and the test-tag backlog has a gate**
  (B47, D4). Three unused `HOME_*` constants went, along with the assertions that asserted the
  absence of things already absent; the tag rule now fails a build that adds a tag without the
  test asserting on it, so the backlog can only shrink rather than silently drift.
- **A pre-release dispatch runs the instrumented job at the highest published API** (N30). The
  nightly keeps the fast `aosp_atd` image at API 34 — a run nobody is watching should not be slow
  for a reason nobody is testing that night — while the release dispatch, the run that can catch an
  API 35+ behaviour change, runs at **36**. The shipping API is 37, but Google publishes no
  `platforms;android-37` and no `default` or `aosp_atd` system image at any 37.x, so 36 is the
  highest that exists; the first dispatch found that out. The `aosp_atd` images stop at 34.

### Fixed

- **Metric targets survive a backup** (N39). A target is a setting rather than a row, and the
  export did not carry settings — so restoring a backup dropped every target in silence, which
  is the hand-written codec's trap wearing a different hat. Targets now ride in the file, and
  "delete everything" clears them; the rest of settings (rest, cue, keep-screen-on, the
  statistics range) stays a device preference and is deliberately still not exported (N21).

## [1.8] — 2026-10-02 (versionCode 9)

### Added

- **An index on `workout_sessions.startedAt`, schema v19** (B46). The statistics range filter
  and the record count both scan by start time and neither could use the `finishedAt` index —
  one filters `IS NOT NULL`, the other wants a range on a different column — so the record query
  read every set ever logged. The migration test asserts the row survives *and* that the index
  exists, since a query cannot report a missing index.
- **A target for any metric, drawn on the chart.** Typed in the unit the screen shows and stored
  canonically, **dotted where the average is dashed**, clearable, and unreadable input sets
  nothing rather than a zero. The axis widens to include it — a target you cannot see is not a
  target (N39).
- **The readings, under the chart** — date and value, newest first, collapsed until asked for,
  with **Average** and **Trend** rows. It is the chart's accessible counterpart, a moment that
  recorded nothing stays a gap rather than a zero, and the average is over readings that exist.
- **The chart's x-axis is time**, labelled in words, so two workouts a day apart and two a month
  apart no longer draw the same distance apart; the line still breaks at a missing reading
  (N37).
- **Bars for quantities, a line for a scale** — volume as bars, ratings and measurements as
  lines, so a 1–10 rating is not flattened by an axis anchored at zero.
- **An average, a least-squares trend and its slope** in the metric's own unit per week, fitted
  only to readings that exist, with the sign in the text because the same number is progress on
  bodyweight and a warning on joint pain. *(Drawn in a theme colour invisible on a device at
  first; neutral now.)*
- **A trailing mean over a configurable period**, seven readings by default: it counts readings
  rather than days (N40), emits from the first reading, and ignores unmeasured days.
- **A Statistics tab with one picker over every series**, replacing a workout-trends screen and a
  per-lift one. Every series is described in **one registry** — label, unit, formatter, group,
  line-or-bars, axis-at-zero and better-direction (N35) — and the three enums stay, referenced by
  it. The two screens it replaced are gone, with their ViewModels.
- **The range is remembered as what it means**, so a saved "last 7 days" is still the last seven
  days tomorrow; a custom From–To asks for its dates before it applies and reads them in the
  calendar's own zone.
- **The overview is three numbers** — workouts, volume and records. A record needs a query of its
  own because nothing stores that a set was one, and a count that has not been asked for shows a
  dash rather than a zero.
- **Five tabs** — Workouts · History · Statistics · Library · Settings — with the overflow menu
  reduced to the data actions. Templates stayed under Workouts, and body measurements are pushed
  from Statistics.
- **Each tab keeps its own place**, back from a tab root goes to Workouts, the bar disappears
  during a workout, a detail pushed inside a tab keeps it highlighted, and each item carries a
  label and a selected state for TalkBack.
- **Body measurements**, on their own screen: a dated entry with a weight and *optionally* body
  fat, muscle and seven tape sites, every one nullable — weight is the only field an entry cannot
  be without, and the save says so before it is tapped.
- **A day has one entry, and saving onto it edits it** (N32); an unmeasured tape site stays
  **blank** rather than carrying a value forward.
- **Measurements are charted** through the same chart as the training trends, sharing the chart
  rather than the axis, and travel in the backup file with a round-trip guard test; the schema is
  at **v18** (the index above takes it to 19).
- **A finished workout can become a plan.** **Save as plan** copies the exercises in order with
  their performed sets as targets, keeps the rest, technique note and superset grouping, and
  offers to open it. It copies neither the readiness note, the ratings nor the workout comment,
  which describe that day rather than the plan.

### Changed

- **The instrumented job runs the emulator with VM acceleration** (N30). The `kvm` group existed
  on the runner but the runner user was not in it, so the emulator fell back to software
  emulation — which is what all four of its failure signatures were. A udev rule grants the group
  and `-accel auto` takes it. *(Signatures and probe output: DECISIONS-EVIDENCE.md.)*
- **CI sets the emulator's flags explicitly and runs nightly and before a release rather than on
  every push** (N30). Reading `android-emulator-runner`'s own `action.yml` showed the flags it was
  told to try *are* the action's defaults, so that experiment could not have been the fix; setting
  them means a bump cannot change how the emulator boots unnoticed. The concurrency group now
  includes the event name, after a push cancelled an instrumented run twenty minutes in.
- **The instrumented suite runs green on the hosted runner.** An **Automated Test Device**
  (`aosp_atd`, API 34) boots in three and a half minutes where the previous image took sixteen and
  spent the run offline, and the count guard is satisfied — 175 declared, 175 executed — on an
  image with no Google services. The cost stands: the suite is tested against API 34 rather than
  the 37 the app ships against.
- **Progression is offered rather than applied** (N33). In two places the suggestion *was* the
  value one tap logged, so a lifter who progresses by hand had to undo the app's arithmetic. One
  tap now logs the plan's target, then what you just did, then **last time unchanged**, then the
  default, with the proposal beside it and a **Use it** action.

### Fixed

- **The fitted line is inside the axis.** The axis was built from the readings alone, so a
  fast-rising line was clamped flat along the top edge exactly where it rose fastest.
- **A tape or body-fat field that is not a real number is no longer stored.** `Math.round` of a
  `NaN * 10` is `0`, so typing "NaN" wrote a real 0.0% reading and "Infinity" became −0.1%;
  negatives were accepted too. The guard is the one `Weight.parseKilograms` has always used.
- **"All" counts records instead of showing a dash.** The tile was permanently empty on the one
  range where a lifetime count means most, because "all" has no window and the count bailed
  without one.
- **A long range no longer loses its older sessions.** The training series were capped at 500
  *sessions* and then date-filtered, so a "last year" range silently omitted everything before the
  newest 500.
- **The Statistics screen can change the lift again.** The picker was drawn only while *no* lift
  was selected, and every entry point arrives with one already chosen.
- **Loading and failure no longer read as "0 workouts, 0 kg".** The overview and the chart were
  drawn from the default state before anything had been read, so a first frame — and any failed
  read, which had no message — made a claim about data the screen did not have.
- **The exported schema for v18 is regenerated.** It listed fifteen entities for eight tables,
  every pre-measurements table twice, and `app/schemas` is the baseline the migration tests
  validate against.
- **A target that is not a real number can no longer blank the chart.** `MetricUnit.parse` was a
  bare `toDoubleOrNull()` accepting `NaN` and `Infinity`, which reached the axis and made every
  coordinate `NaN` — `NaN.coerceIn(0f, 1f)` is still `NaN`; parsing now requires a finite,
  non-negative number checked after the unit conversion too, and `axisBounds` discards non-finite
  input.
- **A rating is drawn on its own scale again.** The chart promised a fixed 1–10 axis and the
  constants for it were referenced nowhere, so RPE readings of 7.1 to 7.3 produced a 0.24-wide
  axis; the scale lives beside the metric now and widens past 1–10 only for real readings or
  targets.
- **A rising trend no longer prints as "+0".** The rate used the reading formatter, which
  truncates, so 0.2 reps a week read as "+0 reps per week" beside a line going up.
- **A target below zero is visible on a metric anchored at zero** — the axis extends below zero
  only when something actually sits there, so an ordinary bar chart is unchanged.
- **Discarding a workout no longer leaves a white screen.** Two identical `LaunchedEffect(closed)`
  blocks both called `popBackStack`, popping the back stack twice and leaving the navigation host
  nothing to render; Finish had it too, one tap later. A single `LeaveWhenClosed` composable
  replaces both. *Verified on the emulator both ways: with the fix reverted, Discard reproduces an
  empty accessibility tree and a screenshot 94.6% one flat colour.*

## [1.7] — 2026-10-02 (versionCode 8)

### Added

- **Repeat the last workout in one tap**, copying the **exercises and their order — not the
  loads**, which progression and the "last time" prefill already answer; an exercise deleted since
  is skipped. *(Its branch had never been entered: the route carried only `templateId`, so
  `repeatLast` was always false.)*
- **A rest is heard and felt, and a workout keeps the screen awake** (N27): a tone and a tick when
  a rest ends, and screen-on while a workout is open, each with a settings switch and both on by
  default. **Neither asks for a permission** — an in-process tone and view-level haptics, and a
  window flag rather than a wake lock, where `Vibrator` would have cost
  `android.permission.VIBRATE`.
- **A warm-up ramp in one tap**: four sets at 40%, 60%, 75% and 85% for five, three, two and one
  rep, from the weight a plan already names. Offered only where there is a weight to take a
  fraction of.

### Changed

- **The app is called Workout, and so is the repository.** It had drifted into four names — the
  launcher said *Workout Log*, the roadmap heading *Workout Tracker*, the release assets
  `workout-log`, and the repository `android-app`, which named the platform rather than the app.
  `applicationId` and the signing key are untouched, so it updates in place with no uninstall and
  no lost history, and past release titles and `workout-log-1.x.apk` assets keep the old name
  because that is what they shipped as.
- **A workout remembers the timezone it was performed in** (N25). Every screen had formatted UTC
  timestamps in the zone you are reading in, so a session logged in Tokyo showed the wrong hour
  and, after a late flight, the wrong day; the offset is captured when the session opens, and
  sessions recorded before this show the current zone.
- **Unused imports are gated.** detekt's `UnusedImports` is off by default and the compiler does
  not run with `-Werror`, so an import stranded by an implementation that landed elsewhere passed
  every gate; turning it on found fifty-one across twenty-one files.
- **Two names that lied, corrected.** The finish review's `WorkoutSummary` — also the history
  row's name — is `WorkoutReview`, and `SettingsModule` moved out of `DatabaseModule.kt`, which
  said database while binding a `SharedPreferences` repository; the move also showed the new
  import gate flagging the three imports it left behind.

### Removed

- **The background rest alert, and with it the app's last permission.** The alarm, its receiver,
  the notification, the ask-on-first-set prompt and `POST_NOTIFICATIONS` and
  `SCHEDULE_EXACT_ALARM` are gone, so the app declares **no permissions at all**. The rest timer
  is untouched: the end instant still lives on the session row and survives process death.

### Fixed

- **The alert's notification channel is deleted on launch** (B37, B40). Android keeps one until
  uninstall, so a phone that ran an older build still listed "Rest timer" — for an app that posts
  nothing and declares no permissions. One idempotent call, no permission needed.
- **A date can no longer be rendered without saying which zone it is in.** The three formatters
  defaulted their zone to the phone's, which is what let the wrong-day bug ship; the zone is
  required and every call site names it.
- **Two documents that had stopped describing the code**, including a KDoc claiming an open
  session is *refused* when it is *resumed*, and comments the removed alert left in
  `AndroidManifest.xml` — the file the no-permissions promise rests on — and `AGENTS.md`.
- **Three tests that could not fail now can.** Deleting the rest alert took their assertions with
  it, leaving two empty-bodied tests and one that ran a whole finish flow to assert nothing; they
  now assert that skipping a rest clears it, that adjusting one reaches the repository with the
  step the button sends, and that a comment written on the way to finishing lands on the session.
- **A warm-up ramp is written in front of the work**, not appended, which had a plan reading the
  working set before the four warm-ups that prepare for it.
- **The repeat action is offered only when it would copy something**, and is absent while a
  workout is open. It had asked whether history was non-empty while the copy also requires
  exercises still in the library, so a last workout whose exercises were all deleted offered a
  button indistinguishable from *Start workout* that opened an empty session.
- **Repeating a workout keeps its rest, note and superset grouping.** Only the exercises were
  copied, so a repeated superset arrived ungrouped and its round logic short-circuited.
- **Home shows a past workout's date in its own zone.** It was the one screen that dropped the
  argument and fell back to the phone's, so the same workout read as two different dates on two
  screens.
- **Finishing a workout leaves the screen again, and the undo offers are back.** Removing the
  alert took the navigation effect and the call that draws the undo offer with it; neither is
  visible in a compile, and detekt's unused-parameter and unused-private-member rules caught both.
- **Pairing a superset is one write.** It wrote row by row and carried on after a failure, so the
  process dying between writes could leave half a group, which the screen shows as a superset of
  one.
- **The superset tap is no longer drawn on the first exercise**, where there is nothing above to
  pair with and the write matched every ungrouped row, churning `updatedAt` for no change.
- **Assisted work adds a rep before it takes help off.** The progression rule tested its assisted
  branch before its rep ceiling, so "add a rep first" was unreachable for assisted work (N22); the
  rep to add now carries the assistance rather than zeroing it.

## [1.6] — 2026-10-01 (versionCode 7)

### Added

- **Supersets and circuits.** **Superset with above** groups an exercise with the one before it,
  the pair is labelled **A1/A2**, and the **rest belongs to the round rather than the set** (N24)
  — a rest starts only once nothing else in the group is behind. Marking a member done counts as
  caught up, leaving takes the whole group apart, and a plan can prescribe one. Schema **v16**,
  with the grouping carried in the backup file; a `MigrationTestHelper` case seeds rows at v15 and
  asserts the amended `MIGRATION_15_16` keeps them.
- **Personal records, and noticing one when it happens.** A record is a **rep max** — the heaviest
  working set at each rep count — announced above the work with **what it beat**; the first time
  at a rep count says that instead. Warm-ups became excludable with N14's roles, which is why this
  could not have been built honestly before.
- **The app stops handing back the same number.** The prefill proposes the next step by **double
  progression** and says why — "One more rep than last time", "A step heavier", "Less help than
  last time" (N22). **It suggests; it never writes:** no plan or stored set changes, and with no
  plan there is no ceiling, so it proposes one more rep rather than inventing a weight.
- **A settings screen, and the default rest is editable** (N21). The app-wide rest was a hardcoded
  90 seconds; settings now holds a **Default rest** from a bounded set, an exercise's or plan's own
  rest still wins, and a change reaches a workout already open.
- **A review when a workout finishes, with plan versus actual** (N20). Finish used to be a dead
  end; the workout now ends with sets, reps and volume, the notes, each exercise's feel and joint
  pain, and — when it came from a plan — **the plan next to what was actually lifted**. It also
  says what was *not* done: a skipped prescribed exercise, and an exercise added mid-workout
  labelled as not in the plan.
- **Choosing a set's role where the set is logged** (N19), beside **Log set**, so three warm-ups
  no longer cost three log-then-edit round trips; the choice clears itself once the set is written.
- **Start over, with a way out first.** Deletes are soft, an import can only add, and the only
  true wipe was `adb shell pm clear`, which is not a phone feature. **Delete everything** removes
  every workout, set, template, planned set, custom exercise, rating and comment, plus the crash
  logs — the seeded library stays. Export is offered in the dialog, the
  confirmation is **typed** (`DELETE`), and it is **hard**, not soft: every other delete here sets
  `deletedAt`, and a soft-deleted row would survive an export taken afterwards.

### Changed

- **`personalRecords` and `setSupersetGroup` now run against a real database**, not hand-written
  repositories, which could not show that a heavier warm-up must not set a record or that a
  superset group survives its projection — the session projection had already shipped once
  without selecting the column.
- **A plan that names reps and no load now says why it proposes what it does** (N22): it explains
  a *load* only, since "one more rep than last time" beside a set the plan sized would be a
  sentence about the wrong number.
- **A route that is declared but never registered now fails the test suite.** Two bugs shipped
  from that gap — a route missing `@Serializable`, and one with no `composable<...>` registration,
  which made the settings screen crash on its first run; the route list is read out of `Routes.kt`
  and compared against what `AppNavHost.kt` registers, rather than named by hand.
- **A guard against the backup codec quietly losing a column** (N24's preparation). The
  hand-written codec had already dropped an unnamed column three times, each found by hand after
  shipping; a round trip with every field set now fails where the next column is added.

## [1.5] — 2026-09-30 (versionCode 6)

### Added

- **Trends for one exercise** — over the last ten finished sessions that recorded the lift:
  heaviest working set, estimated one-rep-max, volume, total reps, and that exercise's own average
  RPE, muscle feel and joint pain. **Warm-up sets are excluded from the load series** (N17), and
  nothing new is stored; the chart is shared with the overview.
- **Assisted work says which way is forward**: that series is the session's least assistance,
  labelled "less is more", rather than a climb that reads as improvement.
- **Tests for the gaps the review found** (B9–B11), the useful one being `ALL_MIGRATIONS` asserted
  against the runtime schema version — a migration written, tested and forgotten in the array had
  left the suite green while crashing every install with data.

### Changed

- **Tests assert with Truth, and Flow sequences with Turbine**, migrating as files are touched
  (DECISIONS.md).
- **The configuration cache is on for local builds too.** CI already passed
  `--configuration-cache` and `gradle.properties` now sets it, so an ordinary `./gradlew` gets the
  same reuse; the wrapper's `retries` went from 0 to 3, with the distribution still
  checksum-validated.
- **CI: one Gradle invocation where there were five**, because each separate call paid its own
  configuration; and the emulator job now fails unless the executed test count equals the count of
  `@Test` annotations in `app/src/androidTest`, naming the classes that did not report (B8, B12).
- **The review's rule violations are settled.** `HALVES_PER_POINT` is one constant; the two
  `DayOfWeek` formatters, the two `SetType` pickers and the three `CenteredMessage` copies are one
  component each (`SetRoleSelector` among them); the duplicate `ActiveWorkoutInfo` went with B13's
  dead state; and four files are named for what they hold (`DataErrorMessage.kt`, `NoteDialogs.kt`,
  `ExercisePickerRoute.kt`, and a `RestAlarmReceiver.kt` of its own, since Android instantiates it
  by name).

### Removed

- **Deleted what moved and left its shape behind** (B13). The exercise library's ViewModel still
  carried the whole "workout in progress" apparatus — a state field, two flows, a per-second
  ticker, and the `TimeSource` and `WorkoutRepository` dependencies that fed it — months after the
  resume button moved to home. With it went `@ApplicationScope`, the module binding a scope
  nothing injects, and four declarations nothing called (`TemplateDao.findTemplateSets`,
  `WorkoutSession.isActive`, `WorkoutSummary.hasVolume`, `PreviousPerformance.isEmpty`). Per D2,
  `Weight.step`, `DataResult.map` and `successUnit` went too.

### Fixed

- **The finish review's counts now agree with its reps.** "Prescribed 2×3" meant two sets, one a
  warm-up, totalling three reps; both counts and sums exclude warm-ups now, and a plan naming no
  reps no longer reads "prescribed 2×0".
- **The review tells two rows of the same exercise apart**, matching by exercise id rather than
  display name, which had collapsed a movement performed twice and made the totals contradict the
  rows.
- **A warm-up can no longer raise a personal best** — the record rule never saw the set's role,
  and now takes it, so a caller cannot forget it.
- **A record says what it actually beat.** The banner read history alone, so a bar set earlier in
  the same session was invisible: 20 kg then 22.5 kg at eight reps announced "the first time at
  this rep count" while claiming a record over that very 20 kg.
- **Editing a logged set no longer wipes its role or its assistance.** The editor was never told
  either, so saving a one-rep correction over a `-20 kg` assisted warm-up destroyed both;
  history now records the role.
- **A superset rests for the group's longest member** (N24), not for whichever exercise happened
  to close the round, which had the same pair resting differently depending on the order it was
  logged in.
- **Tapping a lift in a past workout showed no trends for it** (N17): the history screen passed
  the session's own row id where a series exists under the *library* exercise's id. Found by
  device verification against the published 1.4 upgrade.
- **A cancelled export no longer reports a failed one.** Both callbacks wrapped their file IO in
  `runCatching`, which swallows the `CancellationException` raised when the user leaves the screen
  mid-write; both now rethrow cancellation, the rule `dataResultOf` already stated.
- **An assisted set no longer loses its help in history** (B5) — the detail built the row without
  passing the column — and **a plan's target RPE reads as a lifter writes it** (B6), where 9.5
  rendered as "RPE 19".
- **One-tap "Log set" writes the set its button described** (B7, D3): the button read `-20 kg × 8`
  and wrote a set with no assistance, because the value was in the suggestion and not passed.

## [1.4] — 2026-09-30 (versionCode 5)

### Added

- **A plan can be pinned to a weekday, and home shows today's plan.** Several plans may share a
  day, and each is listed with a Start action; a plan with no day is one you start by hand. It is
  **a living template, not a dated instance** (N16). Migration **14→15** adds a nullable
  `templates.weekday`, stored by name.
- **Assisted load.** `-20` means the machine took 20 kg off, stored as a separate
  `assistanceGrams` — **a magnitude, never a signed weight**, so an assisted set contributes
  nothing to volume rather than subtracting (N15); a plan can prescribe it. Migration **12→13**
  adds `set_entries.assistanceGrams`, defaulting to 0 so every recorded set keeps its meaning, and
  a nullable `targetAssistanceGrams` on a plan's sets.
- **Templates are plans now.** A template exercise carries planned sets — a role, a target weight,
  a target rep range, a target RPE and a note — plus the rest and cue the plan prescribes, and
  starting a workout prefills each set from them; **Copy forward** duplicates an exercise's sets.
  **A set's role** gives planned and performed sets one vocabulary — working, warm-up, **top set**,
  drop and failure (N14). Migrations **10→11** add `template_sets` and the plan's
  `restSeconds`/`techniqueNote`, and **11→12** adds those to `session_exercises`.
- **Targets only:** nothing verifies a plan against what was lifted, and a logged set is a
  separate row expected to differ.
- **A comment on the workout itself.** Finishing asks once, and skippably, how it went; it needed
  no migration, because `workout_sessions.notes` has been in the schema since v1 and never had a
  UI.
- **How it felt can be recorded at any time**, per exercise, rather than only at Done, so the two
  ratings and the pain location are written down while the set is fresh. The Done prompt stays as
  the last chance rather than the only one.
- **Joint pain says where** — an optional free-text box beside the rating. Migration **9→10**.
- **Trends: what the app collects, read back.** RPE, muscle feel and joint pain over the last ten
  finished workouts, each on a fixed 1–10 axis with its latest value and average. A metric
  recorded once is a number rather than a line, a workout that did not record one leaves a gap,
  "not recorded" is never zero, and a workout with nine rated sets does not shout louder than one
  with a single rated set. No charting dependency: the lines are drawn on a `Canvas`.

### Changed

- **RPE takes half steps**, stored as *halves in an integer* (`19` is 9.5) (N6). The field accepts
  `9`, `9.5` and `9,5` and refuses anything finer than a half rather than rounding, while the
  muscle-feel and joint-pain ratings stay a whole-number scale. Migration **13→14** renames the
  columns to `rpeHalves` and `targetRpeHalves` and doubles existing values; the backup keeps
  reading the old whole-number field, since renaming it would have dropped the RPE out of every
  earlier export on restore.
- **The 1–10 scales now say what their ends mean** — "1 = barely worked, 10 = fully worked",
  "1 = none, 10 = severe" — while the number is being picked, and only there: the workout detail
  keeps showing a bare "Muscle feel 8" rather than repeating the vocabulary on every past workout.

### Fixed

- **Export and import are back where you start**, in the home overflow rather than two menus deep,
  and the library's copy is gone — one path, not two.
- **Removing an exercise asks first**, because it takes the exercise's sets with it and has no
  undo; the dialog says what goes rather than posing a bare question.
- **An Undo can no longer act on something that is gone.** The deleted-set and Done snackbars share
  one host, so a deleted set's Undo could outlive its exercise and fail the loggable-exercise
  guard without saying anything; it is offered only while its subject is in the session.
- **A library read that fails is a message, not a crash.** `observeExercises` and `getExercise`
  were the last calls that could throw out of a flow; both return the same `DataResult` the writes
  do, and "we could not read it" is no longer the same sentence as "it is not there".

## [1.3] — 2026-09-29 (versionCode 4)

### Added

- **Workout templates**, built once and started in one tap, through the same append path a manual
  add takes; resuming an open workout never seeds a second copy, and editing the session never
  touches the template. They ride along in the export file, soft-deleted ones included. Migration
  **8→9**.
- **How it felt: muscle feel and joint pain**, asked once and skippably per exercise, stored per
  session exercise and editable from the detail; deliberately unlabelled at first. Migration
  **7→8**.
- **Done, so an exercise stops taking sets by accident**: it hides **Log set**, dims and locks the
  sets, stops any running rest, and has an **Undo** and a **Reopen**, because accident protection
  must not become its own trap. The workout-level action is *Finish*, so this is *Done*, and it is
  a session state rather than a delete. Migration **6→7**.
- **An RPE and a comment on every set**, neither written by the one-tap path, so logging stays
  fast; an RPE outside 1–10 blocks Save rather than being clamped. Migration **5→6**.
- **A readiness note when a workout starts** — free text on the session, reachable from the
  workout header; a resumed workout is not asked again. Migration **4→5**.
- **Per-exercise rest and a technique cue**, falling back to the 90 s default, editable from the
  exercise detail — which now edits seeded exercises too, since the seeder tops up with
  `INSERT OR IGNORE` and never updates a row. Migration **3→4**.
- **Create a custom exercise from inside a workout**, stored `isCustom = true` with a UUID id and
  an unspecified taxonomy, and **editable afterwards** so it becomes pickable with real taxonomy.
- **Fifteen more exercises**, mostly competition, paused and accessory variants, topped up on
  every open without a migration.

### Changed

- **An exercise's subtitle no longer reads "Other · Other"**, since `Other` is the "not filled in
  yet" value a custom exercise is created with — the `Quads · Barbell` line drops it, so an
  unedited custom exercise shows its name alone.
- **The app opens on your workouts, not the exercise list.** Home is recent workouts with **Start
  workout** (or **Resume**) and a link to history; the library became a screen you navigate to,
  and stays the picker inside a workout.

## [1.2] — 2026-09-29 (versionCode 3)

### Added

- Set rows announce that they are editable, so a screen reader no longer reads a row and leaves
  the user to guess (`onClickLabel`).

### Fixed

- A duration of an hour or more rendered differently in the rest timer than in the workout clock —
  `90:00` against `1:30:00`. Both share one formatter now, and the rest timer gained the hours
  field.

## [1.1] — 2026-09-29 (versionCode 2)

The upgrade-test build: installed over 1.0 without uninstalling, and confirmed to keep the workout
history. **No user-visible changes** — it exists to prove the permanent signing key accepts an
upgrade rather than demanding an uninstall, which would have cost the history.

## [1.0] — 2026-09-28 (versionCode 1)

First release. Sideloaded as a signed APK; there is no Play Store listing.

### Added

- Exercise library: 30 seeded movements with primary and secondary muscles, equipment and movement
  pattern; search covers the display label *and* a locale-stable key, so it survives translation.
- Start a workout, log sets by reps and weight with prefill, tap to edit, and delete with undo.
- Rest timer with an in-app countdown and an optional background alert.
- Workout history: a chronological list grouped by month, plus a detail view with duration, volume
  and set count.
- Correct or delete a past set, and delete a whole workout behind a confirmation — so finishing a
  workout is no longer a one-way door.
- Resume affordance: the library button shows the running workout and its elapsed time.
- Export and import the whole database as JSON, through the Storage Access Framework, restoring
  anything missing or deleted and never overwriting what is still there.

### Notes

- Local only, as v1.0 shipped it: no `INTERNET` permission, no accounts, no ads, no analytics, and
  `allowBackup="false"` — the export file is the only way data leaves the device.
- Sets are `reps × weight`. Bodyweight work is reps at 0 kg; duration and distance are out of scope
  for this version.
