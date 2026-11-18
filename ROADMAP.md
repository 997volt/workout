# Workout — Roadmap

> **What is planned, not what happened.** *Next* is the ordered batch, *Later* is decided and waiting, *Parked*
> is decided against until a named trigger fires, and the non-goals are lines this app does not cross.
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

## Next — the auto progression review

The auto progression feature (N299) was read against the decisions it claims to follow — N299 itself,
N303, N304, N305 and N311, with N74, N235, B400, B402 and B409 — with the switch on for a weekly
program that repeats: how a *Done* finds its program, what one press writes, what the banner can take
back, what the baseline resets, and what the plan's own movement does to the rules that compare history
to the plan. The rows are ordered by what they cost: the first needs a decision about storing the
prescription and should land before 2.32 cuts, the rest are independent. Nothing was fixed during the
read, because B471 is a design decision rather than a patch, and the last two reviews kept the read and
the write apart for the same reason.

### Before 2.32

- **B471 — Auto progression makes a progressing lift look stalled, and withholds its volume card.** The
  N304 stall rule compares every recent session to the plan **as it stands now**:
  [PlanStallFinder.kt](app/src/main/java/com/example/androidapp/domain/PlanStallFinder.kt) reads the live
  template and hands `exercise.progressionSets()` to `PrescriptionHistory.planned`, and
  `PrescriptionHistory.stall` measures each past session against it. With auto on, *Done* writes the next
  step **before** the finish reads the card, so every session that earned a step is now one step behind
  the plan: a rep step moves `targetRepsCurrent` past the reps just performed, a load step moves the weight
  past the load just lifted, and a hold step moves the seconds past the hold just taken. Three successful
  weeks therefore build a three-session run of "misses", and the finish says a lift that has moved every
  week has stalled; the program editor and the weekly review
  ([PlanStallFinder.stallsForProgram](app/src/main/java/com/example/androidapp/domain/PlanStallFinder.kt))
  carry the same run, which keeps growing while the plan keeps moving. The same living plan is judged by
  N311: [PlanVolumeFinder.kt](app/src/main/java/com/example/androidapp/domain/PlanVolumeFinder.kt)
  rebuilds previous prompts with `promptFor(..., exercise.progressionSets())`, so auto-progressed sessions
  never satisfy "every working set earned a step", and the false stall closes the `isOpen` gate that
  would still allow the extra-set card. A manually accepted step and a hand-edited target move the plan
  the same way, so this was latent before N299 — auto makes it certain. Fixed means the stall and volume
  rules read the prescription each session was trained under (N20's second rule: store the planned sets
  with the session), not today's plan; a stored verdict was rejected because history is editable (N306)
  and a corrected set must be judged against the plan it actually answered. N305 rejected that schema
  for a read-back number, but here the number decides a deload and a set-back, so the reason for
  rejecting it no longer holds. A write-log-only fix was rejected too: it would know the auto steps and
  not the manual ones, which is two rules for one fact.

- **B472 — A partial auto batch lands without its undo banner.** `applyAutoProgression` returns as soon
  as `writeAutoProgressionSteps` reports a failure
  ([ActiveWorkoutViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutViewModel.kt)),
  before `showAutoStep`, although the loop has already recorded every landed step in `steps`. Those rows
  no longer offer anything — the plan has moved past them — the exercise stays open for the rest, and the
  banner that is the only caller of `ProgressionRepository.undo` never shows them. A storage failure on
  the second of three sets therefore leaves the first two applied with no way back. B402's roadmap row
  named this half explicitly; the fix that shipped queued completed batches only. Fixed means the landed
  steps are shown with their undo before the failure is said (the exercise still stays open), plus a test
  whose second apply fails.

- **B473 — The banner's undo stops at the first refusal and can skip the next batch.** `onUndoAutoStep`
  walks the batch newest-first but wraps each call in `if (failure == null)`
  ([ActiveWorkoutViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutViewModel.kt)),
  so one "the plan has changed" refusal leaves the older steps applied while `advanceAutoStep` drops the
  banner anyway. The undo is safe to attempt per write and the lifter asked for the whole batch, so every
  step should be attempted with the first failure reported. The same method has no single-flight guard:
  two taps over a slow undo run the batch twice, the second run advances again, and the next queued moment
  is popped without being shown — which is the un-undoable batch B402 set out to prevent. Dismiss has the
  same advance. Fixed means every step is attempted, the first failure is said, and an advance only
  happens for the moment it was for (a guard or an identity check), with tests for the refused step and
  the double tap.

### The rest of the review

- **B474 — A double *Done* launches two auto writes.** `onFinishExercise` hands the auto path to
  `viewModelScope.launch` with no in-flight guard
  ([ActiveWorkoutViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutViewModel.kt)),
  and *Done* has no busy state either ([WorkoutExerciseSection.kt](app/src/main/java/com/example/androidapp/ui/workout/WorkoutExerciseSection.kt)).
  Two taps before the first write lands build the same prompt from the same pre-write plan; the second
  write is refused by `requireBeforeMatches` ("the recorded step does not start from the plan"), which is
  a caller refusal rather than a user error, and the race decides whether the finish's success clears it
  before the screen draws it. Fixed means a per-exercise finish in flight (or a single-flight write)
  ignores the second tap, with a test that presses *Done* twice without settling.

- **B475 — A program's auto switch can survive in a shape the editor cannot show.** A weekly program
  converted to a block keeps `autoProgression = 1` in its row —
  [RoomProgramRepository.setProgramKind](app/src/main/java/com/example/androidapp/data/RoomProgramRepository.kt)
  writes only the kind — while the overflow entry that opens the switch is hidden for a sequence
  ([ProgramEditorScreen.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramEditorScreen.kt)).
  The resolver ignores it because the kind is no longer weekly (N303), and converting back to weekly
  silently re-arms a switch the lifter has not seen since before the conversion. A block restored from a
  backup can arrive with the flag on and has no way to turn it off, because the backup restores the whole
  program row. Deleting a program has the same gap: `softDeleteProgram` clears `isActive` but not
  `autoProgression` ([ProgramDao.kt](app/src/main/java/com/example/androidapp/data/local/ProgramDao.kt)),
  and a program document that un-hides the row restores the device's flag, so the row writes again
  without a new consent. Fixed means the switch is cleared when the program leaves the weekly shape or is
  deleted (the same reason `isActive` is cleared), and a restored or converted row cannot carry a flag
  the resolver ignores; the document path must not re-arm it.

- **B476 — Reset writes more than "every planned set in this program".** `resetToBaseline` reads the
  program's baseline rows and rewrites every live set they name
  ([RoomProgressionRepository.kt](app/src/main/java/com/example/androidapp/data/RoomProgressionRepository.kt)),
  whether or not the program still schedules that set. Removing a day or repointing a slot leaves the old
  template's sets in the baseline, so a reset after a schedule edit rewinds a workout that is no longer
  part of the program — while the confirmation still says "Every planned set in this program"
  ([strings.xml](app/src/main/res/values/strings.xml)). The plan is also one plan: another program that
  schedules a shared template has its targets rewound by this program's baseline, and the dialog names
  neither the shared plan nor that program, although N307's deload confirm and N308's replace sheet
  already name another program when the plan is shared. Fixed means reset only touches sets the program
  currently schedules, and the confirmation names the sharing (or the other programs) when there is any.

- **B477 — The retained baseline for a disabled switch is unreachable, and the docs say it is not.**
  [ProgressionRepository.kt](app/src/main/java/com/example/androidapp/domain/repository/ProgressionRepository.kt)
  says disabling "leaves that snapshot, so a later reset still has the point the first switch was made
  from", and [RoomProgressionRepository.kt](app/src/main/java/com/example/androidapp/data/RoomProgressionRepository.kt)
  keeps it for that answer — but the screen offers Reset only while auto is on
  ([ProgramProgressionScreen.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramProgressionScreen.kt)),
  while re-enabling clears and recaptures the baseline. The kept rows therefore have no reader: the
  "Started from" line is hidden while off, and turning it on overwrites the date. The screen test's name
  says "only while auto is on" but it only asserts the on state, so the off state is unpinned as well.
  Fixed means one of the two stories: retain the first baseline across a re-enable (and say so), or
  drop the stale rows and correct both KDocs.

### Coverage and one scope question

- **N325 — The auto progression seams that let these through are uncovered.** No test presses *Done* on
  an exercise that earned two steps (the banner's plural message, the per-step writes and the batch
  undo), a second-write failure (B472), a refused or double undo (B473), a double *Done* (B474), a reset
  after a schedule change or over a shared plan (B476), the switch across a kind change or a
  delete/restore (B475), or a weekly program with the switch on next to a block that names the same
  template. The enablement text and the baseline line have screen tests only for their presence. Fixed
  means those cases are asserted, and B471's historical plan is pinned at whichever level the decision
  puts it.

- **N326 — Which sessions count as the program's?** N299 resolves a session to a program through its
  **template**, so the switch applies to (a) a weekly program's workout started from the library when
  that program is not active, and (b) a block's session whose template is also named by an auto weekly
  program — which the 2.29 entry's "its template never resolves to auto" says cannot happen, but the
  resolver filters the auto **program's** kind, not the session's origin. The app records only the
  template on a session, so neither boundary is visible at *Done*. Fixed means a decision: accept both
  (one plan, and the switch is the lifter's) and say so in N299 and N303, or persist the program/slot with
  the session so a block session can be excluded.

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

Everything that has **shipped** has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
