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

## Next — auto progression for a never-ending weekly program

A weekly program already repeats for ever: [ProgramRun.kt](app/src/main/java/com/example/androidapp/domain/model/ProgramRun.kt)
wraps and nothing counts a cycle, so the only part of "run it for good" that is missing is that its
targets never move. N299 supplies that, and B395 removes the one pairing bug that would let a
mid-session substitution write the wrong plan.

### N299 — A followed weekly program adjusts its own targets

**The loop.** The lifter switches *Auto progression* on for one program. Home still shows the day and
the session still prefills from the plan. At *Done* the app computes the offer it already computes
([ProgressionOffer.kt](app/src/main/java/com/example/androidapp/domain/model/ProgressionOffer.kt)) and
takes every earned step without a dialog, writes the plan, records what it changed, and finishes the
exercise. The next occurrence prefills the new numbers. There is no end state, cycle counter or
per-week state: **the plan's targets are the whole of the progression.**

**What adjusts** — per set, settled:
- Below the range's ceiling, +1 rep for that set; at the ceiling, the movement's own load step and a
  restart on the range's floor (N74, unchanged). Every working set is judged on its own, so 5/5/4
  leaves 6/6/5 and each set follows what it did.
- **No down-adjustment.** A miss, an over-target RPE, a short load or a set nobody logged writes
  nothing and the target holds. A stall is still the N237 card's to suggest a deload; auto-back-off
  and auto-deload are a later decision, not this row.
- Warm-ups and rungs are excluded, holds move in seconds, and bodyweight or assisted sets can raise
  reps but not a load — the existing rules, not a second computation.
- The range is the throttle: 3×5–8 adds a load step about every fourth session, 3×5 every session.
  The consent states which the program is.
- The RPE stepper opens on the plan's target, so "met the plan" is the signal even when the lifter
  never touches it; raising the RPE is how a set that felt too hard holds the target.
- A plan with no load raises reps only, and the preflight says so. "Seat the first performed load"
  is not in this row.

**Ownership and resolution.** A boolean on the program, not a fourth global switch. At *Done* the
movement's own answer wins (N235); otherwise AUTO applies when the session's **template** is named by
a slot of a program with auto on; otherwise the device's Ask/Apply/Off. Resolving through the template
rather than a session→program column means a start from home, the template list or a repeat all count,
and the resolution survives a resume. The flag is device state: in the backup, **not** the program
document, so importing a program cannot start writing on the recipient's device. No document-format,
shared-model or helper change.

**Trust.** Enabling asks once, with a preflight built from
[ProgressionRule.kt](app/src/main/java/com/example/androidapp/domain/model/ProgressionRule.kt): what
will progress and how, what will not (no target RPE, no reps, nothing to move), and which movements
answer Ask/Off for themselves. Each write reads back as "next time: 102.5 kg" with undo while the plan
still holds the new value. Enabling snapshots the current targets as the baseline, with *Reset to
starting targets*; turning auto off leaves the current targets alone.

**A substitution never writes.** The plan belongs to the movement it prescribes, so a stand-in's sets
are not evidence for the original. B395 makes the live pairing say that.

**Deloads.** A built deload week is outside the program's plan — separate templates, no target RPE and
no slot — so AUTO writes nothing there and the program's targets are a pause, not a reset. An
**exempt** week is still the program's own training, so it does not suppress auto: if the lifter meets
the targets the plan moves, and if they back off the existing rule writes nothing. N300 is the naming
that makes the difference legible.

**Storage.** The write and its record must happen together: one applied-step record carrying the full
target before and after (a load step also resets reps, so the load alone cannot undo it), the
direction, the session and the time, plus a baseline keyed by program and planned set. A failure
leaves the exercise open and the next *Done* offers only what is left; the moved target makes a
second *Done* offer nothing, so a resume cannot write a step twice. The next database migration
carries the flag and the two tables; the backup grows with them, because the flag, records and
baseline are training data. Old backups restore with auto off and no records.

**Acceptance.** Resolution (movement override, template in an auto program, off); a mixed per-set
session; no write on a miss; no double write across a resume; undo and a refused stale undo; reset;
preflight for a program with no target RPE; a substituted row finishes with no write; a built deload
week writes nothing and an exempt week still progresses; migration, backup roundtrip, and one device
pass through a session and the next.

**Not in this row.** Auto-lowering, auto-scheduled deloads, phases or a block end, shipped starter
products or first-run seeding, a weekly step cap, seating a blank load from the first performance,
AUTO in the global settings, and any program-document change.

The three reversals this needs — "the app suggests; it never writes", N292's "the app never advances
loads by itself", and N174's "no per-program progression mode … exists" — are argued in DECISIONS
when the row is picked up; the consent, record and undo are the conditions on which the reversal is
accepted.

### B395 — A substituted row can take another plan's entry

The review already pairs a stand-in with the plan it replaced (N291), but the live plan pairs by the
row's **current** `exerciseId` and occurrence in
[ActiveWorkoutSnapshot.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutSnapshot.kt).
A substitute that is a movement the same template also plans takes that movement's entry when it
stands earlier in the session: it prefills and progresses another slot's prescription, and the real
row loses its plan. It is order-dependent, so it reads as a glitch, and it reaches the manual prompt
and the prefill as well as N299.

The fix counts occurrences by `substitutedFromExerciseId ?: exerciseId` and never pairs the substituted
row: the stand-in consumes the occurrence it replaced and gets no plan, while the real row keeps its
entry. Tests in
[PlanEntriesTest.kt](app/src/test/java/com/example/androidapp/ui/workout/PlanEntriesTest.kt) cover the
steal, the duplicate occurrence, substituting back and the unplanned substitute; one workout test
asserts an auto substitution writes no target.

## Later — the rest of the 2.22 review

The rows below are real and specified, but none is in flight: a defect here is low-frequency or a polish item,
an engineering row is not user-visible, and a feature is waiting for a decision. A row leaves this section when
it is picked up; its design argument moves to [DECISIONS.md](DECISIONS.md).

### Defects and polish

- **B358 — The shell and the screens must not both apply the navigation-bar inset.** The shell reserves the
  tab bar, whose height already includes the system bar, and each screen's default `Scaffold` adds the system
  inset again ([AppNavHost.kt](app/src/main/java/com/example/androidapp/ui/navigation/AppNavHost.kt)); the
  deload screen adds a third `navigationBarsPadding()`. Fixed means the shell or the screens own the bottom
  inset exactly once, verified on a gesture-navigation and a three-button device (this was reasoned from code,
  not measured).

### Features

Each row below is a product candidate, not an agreed design; the constraints and the decision it touches are
named so the row can be picked up without re-arguing it from scratch. The id is the planning id; no row here is
agreed to ship.

- **N294 — An encrypted backup file.** Encryption at rest is parked; an encrypted *export* is narrower: a
  password-protected file for a folder another app syncs, still user-chosen and local. It needs a new format
  version, Android Keystore/AES (no new dependency), and plaintext import kept for existing files. Only worth
  doing when a backup actually leaves the device.

## Later — the deload seam

Two different things carry one name today. P3.10's mark exempts a week from scoring and leaves its
workouts alone; N147's built plan writes lighter ones. The Adherence chip calls the mark a deload, the
creator lives in a program-editor overflow, and neither says what the other is. N300 separates the two
actions, the B rows are the draft's blind spots, and N301 is the follow-on for a program with no end.
None of it is in flight.

### N300 — An exempt week and a deload week are two actions

The mark is an **exemption**, not a lighter week: its slots still prescribe their full workouts, and
the only thing it changes is that the week is not scored. The built plan is the deload. The change is
to say that in the names and give each action its own way in.

- **Name them apart.** The Adherence mark becomes *Exempt week* — "not scored" — and *Deload week*
  stays the creator's plan. One entry on the program screen offers *Exempt this/next week* and *Build
  a lighter week*, so the choice is made where a lifter is looking at the program rather than in two
  screens that never mention each other.
- **Offer the prospective exemption.** Today a week can be exempted only after it has started
  (Adherence), and the creator only builds next week. A vacation, a sick week or a planned rest week
  cannot be pre-marked without writing workouts nobody asked for. The repository already stores a bare
  mark; add the action for next week, and let a later *Build* reuse it.
- **Do not ask the missed-day question about an exempt week.** A built week is already left out
  (B163); a marked week still asks. That contradicts the name: the week is exempt from judgement, and
  the prompt is a judgement. Amend P3.10's "the missed-day question is unaffected" — the calendar
  still draws the days, a session still marks a trained day, and the ratio still leaves the week out;
  only the question goes. The argument moves to DECISIONS when this is picked up.
- **Show it on home.** A marked-only week looks exactly like a normal one on the training surface. A
  banner saying the week is not scored is enough; the full workouts must not be labelled *Deload*.
- **Reach the creator.** Add it to the program screen, not only the overflow and the stall card. A
  program that runs for years should not hide its recovery week behind a menu.
- **Give the draft an effort.** It deliberately writes no target RPE, so the workout opens its effort
  field on the app default 9.0 — the opposite of a deload. Write a deload effort (6.0 would do) or show
  a genuinely blank field; the first is one decision, the second a wider change to the session's
  no-target shape.
- **Name the workout after its week.** *Deload A* is indistinguishable after a year of them; put the
  week in the default name or hide the templates once the week is over, keeping them resolvable for
  the history that names them.
- **Say what unmarking does.** The dialog says the plan will not be used; it does not say the week
  returns to the normal schedule and can read as missed, because the deload sessions used templates
  that settle no slot. Say both, and keep the templates.

### B396 — A timed movement is drafted as a counted set

`deloadSetTargets` reads reps and weight only. A hold's plan names seconds and no reps, so the draft
falls back to the hold's zero-rep history, floors it at five, and writes a counted set with no
duration — or, with no history, no target at all. The week's hold is not scaled. The draft needs a
duration target and a rule for it: keep the hold and let the reduced set count be the deload, or apply
the same share to the seconds and round to the 15-second step. Either way the preview and the write
read the same function, and a timed movement never becomes a counted one.

### B397 — A previous deload can feed the next draft's load fallback

When the plan states no load, the draft falls back to the exercise's last finished session. A built
deload is an ordinary finished session of that movement, so the next draft can derive 60% of an
already-deloaded load and compound the reduction. Read the source **slot's** own last performance — a
deload session names no slot — or exclude the templates the deload days own, and test a second deload
after a first.

### B398 — The Adherence deload chip is offered for a sequence

The creator is hidden for a sequence because a block has no week-keyed schedule, but the Adherence
chip is drawn per active program and is not filtered by kind. An active sequence can be marked, the
mark is stored, and nothing in the sequence reads it. Hide the chip for `ProgramKind.SEQUENCE`, or make
the action say why it does not apply; the mark writer can keep accepting a week for the weekly caller.

### N301 — A program with no end suggests when a deload may be due

The stall card is reactive. A program that runs forever has no moment that says "you have been at this
for six weeks". A card after a user-chosen number of weeks since the last deload — or since the
program started — says a recovery week may be due and opens the creator. It is a **suggestion**, not a
computed deload: the cadence is the lifter's, the app only counts weeks it can see, and there is no
recovery model and no notification (P4.7 stays a non-goal). Later than N300, and only if a cadence is
actually wanted.

**Not planned in this seam.** An automatic deload (a scheduled write is a coach, and nothing the app
records measures recovery), a percentage of a one-rep max (the app estimates rather than measures one),
a current or partial week (the plan is whole-week), a second deload entity (ordinary templates plus
the week-keyed mark are the right shape), and a change to the 60% rule without a new argument.

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
