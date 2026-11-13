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

## Next — a stalled lift gets a decision

The stall rule is exercise-level (N304) and the finish already says which lifts have just stopped moving
(N305), but the only answer on offer is the week-level deload creator (N147) — a whole lighter week for a
lift that may need a lighter *lift*, not a lighter week. These rows give the three answers a lifter
actually has: **leave it**, **deload that lift**, **replace it**. Each is a **plan edit**, so the stall
disappears because the plan changed rather than because the app hid it, and none of them stores a
"resolved" state. The three are offered on both surfaces the stall already has: the finish dialog and the
stall card the program editor and weekly review share
([PlanStallCard.kt](app/src/main/java/com/example/androidapp/ui/programs/PlanStallCard.kt)).

**The existing Auto progression switch, its baseline and *Reset to starting targets* are untouched.**
These actions never read or write `progression_baselines`; the reset stays the switch's memory of where the
plan started, not a log of plan edits. Its existing behaviour after one of these edits — a reset restores
its own captured targets on the sets that still exist — is unchanged and not special-cased here.

- **N307 — Deload this lift.** A plan edit, not a one-session override: the plan's targets change, the plan
  is the new baseline, and next week progresses from it. The set count follows one arithmetic, shared with
  the replace: N = the plan's stand-alone working sets; keep the first
  `min(N, max(2, N − round(0.3 × N)))` (round half up), which is `1, 2, 2, 3, 3, 4, 5, 6, 6, 7, 8, 8` for
  N = 1…12; drop the later working sets and the rungs that belong to them. Warm-ups are untouched — not
  removed, no target changes — and the rungs of kept anchors stay. Every kept working set loses **three of
  the movement's own load steps** (N77), clamped at zero; an assisted set instead gains **three steps of
  assistance** (more help is lighter), and a set that states both moves its assistance; a bodyweight set
  with neither is unchanged, a hold keeps its seconds, and a warm-up is unchanged. Reps are unchanged
  except a counted set whose plan has a range: its current target moves to the range's lowest. One
  transactional write, the before → after shown (e.g. *"Bench Press: 5×5 @100 kg → 3×5 @95 kg"*), snackbar
  undo. The stall clears because the old sessions now meet the lower target (B303); the week-level deload
  (N147) stays a separate, secondary answer. The per-lift deload action joins the current stall card in
  this row; the finish sheet and the all-stalls card are N310.

- **N308 — Replace this lift.** A plan **edit**, not a session substitution (N291): the template's
  exercise row changes for every future session, the old movement's history, trends and records stay, and
  the stall clears because the plan no longer names it. The set rows are trimmed by the same rule as N307
  — the first kept working sets survive, the later ones and their rungs are dropped, warm-ups stay with
  their targets cleared, kept rungs stay. The kept working sets are seeded **by place** from the new
  movement's last finished session — weight, assistance and fixed reps (or the duration for a timed
  movement), the same shape the picker's add-to-template fill writes
  ([ExercisePickerViewModel.kt](app/src/main/java/com/example/androidapp/ui/workout/ExercisePickerViewModel.kt))
  — and where there is no counterpart the target is cleared; the old plan's rep range is not carried on a
  seed. **Empty** (N309) keeps the plan's reps and target effort and clears the loads instead. The exercise
  id changes; name, muscle, equipment, unit and step come from the library; position, rest, superset group
  and the target effort stay; the cue is cleared, because it was the old movement's. The replace sheet
  suggests the other movements **filed under the same category** (the old movement's `parentId`, the
  library's own taxonomy, N95/N110), movements only, excluding the old movement and any already in the
  plan; a different family is one *Browse all movements* away; an unfiled movement has no suggestions, and
  no category is inferred. The confirm shows old → new, the set-count change, the seeded targets or
  "targets to be found", and the programs that also name the plan (`otherProgramNamesUsingTemplates`);
  choosing a movement already in the plan from browse warns. One transaction, snackbar undo (the inverse
  restores the dropped sets, the kept targets and the exercise/cue). The picker gains a replace intent and
  the action joins the stall card; the finish sheet and the all-stalls list are N310. No baseline work.

- **N309 — Find the weight.** When the load is not known, the plan can say so: **Empty** clears the working
  sets' weight and assistance (and the warm-ups' targets), keeps the plan's reps and target effort, and
  sets a **one-shot marker** on the plan's exercise — a column on `template_exercises`, a migration, an
  additive defaulted backup field, **not** the shared program document, because it is a one-session
  transition like the auto switch rather than a prescription. The next session for that exercise starts
  with the weight field **empty**, ignoring history and the 20 kg default, with the plan's rep range and
  target RPE as the guide ([SetSuggestion.kt](app/src/main/java/com/example/androidapp/ui/workout/SetSuggestion.kt));
  *Log set* waits for a number, and the following sets fill from "what you just did". The marker is
  consumed at the finish, not at the start, so an abandoned session tries again. At the finish, one line
  per calibration exercise — *"Use today's weights for Bench Press? 3 sets, top 65 kg"* — **Use** /
  **Not now**: Use writes **every logged working set's** weight and assistance into the plan's work sets by
  place, with no "answered the plan" gate, leaving reps, target effort and warm-ups alone and in one
  transaction; Not now leaves the plan weightless; either clears the marker. The plan then has a load, so
  the next session prefills from it and load progression can run. A plan-editor toggle offers the same
  weightless prescription outside the replace flow. No baseline work.

- **N310 — The decision on both surfaces.** The finish dialog
  ([WorkoutStallDialog.kt](app/src/main/java/com/example/androidapp/ui/workout/WorkoutStallDialog.kt))
  stops being information-only: for the workout's just-stalled lifts (the run just reached three) of an
  active weekly program, each gets **Deload** and **Replace**; *Done* is do nothing. The deload writes in
  place; the replace closes the workout, opens the plan's replace sheet, and returns to the plan. The stall
  card, shared by the program editor and the weekly review, lists **every** stalled lift of the program
  rather than the first — the finder gains a program-wide list — each with the same actions and its run
  length, with the week-level "lighter week" kept as a secondary link. The calibration prompt from N309
  runs before the stall sheet. This row also amends N305's "information, not a decision" rule, changes the
  screen states from one stall to a list, and carries the tags and the announcements.

**Docs for the batch.** DECISIONS records the deload rule (amending P3.10's "never computed" to "the lifter
chooses, the arithmetic is shown"), the replace rule (the shared trim, the seed, the category suggestions,
the cue, the shared-plan warning), the calibration (the marker, the blank first session, the prompt, the
20 kg default exception), and the surfaces (amending N305). N299 is unchanged: the per-lift deload is the
"separate rule" it names, and the auto switch, baseline and Reset are untouched. N16/B303 say why a plan
edit clears the stall; N95/N110 are the suggestion source. Each row lands with its CHANGELOG entry under
`[Unreleased]`, its tests (unit, and instrumented for the plan writes and the marker/migration), and its
roadmap row deleted.

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
