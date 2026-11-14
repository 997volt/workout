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

## Next — the stall decision, and the plan earning its volume

The stall rule is exercise-level (N304) and the finish already says which lifts have just stopped moving
(N305), but the only answer on offer is the week-level deload creator (N147) — a whole lighter week for a
lift that may need a lighter *lift*, not a lighter week. These rows give the three answers a lifter
actually has: **leave it**, **deload that lift**, **replace it**. Each is a **plan edit**, so the stall
disappears because the plan changed rather than because the app hid it, and none of them stores a
"resolved" state. The three are offered on both surfaces the stall already has: the finish dialog and the
stall card the program editor and weekly review share
([PlanStallCard.kt](app/src/main/java/com/example/androidapp/ui/programs/PlanStallCard.kt)).

The trim those answers apply needs a counterweight: a plan that has lost sets should be able to earn them
back, and a plan the lifter keeps over-performing deserves to catch up. **N311** is the mirror — the same
pattern-over-a-few-sessions evidence, the same one-tap plan edit, the same undo — and it is a suggestion,
never an automatic step: the Auto progression switch is unchanged.

**The existing Auto progression switch, its baseline and *Reset to starting targets* are untouched.**
These actions never read or write `progression_baselines`; the reset stays the switch's memory of where the
plan started, not a log of plan edits. The one interaction worth stating: the replace writes **new set
ids**, so the old baseline rows point at removed sets and are skipped by a Reset, while the new sets carry
no baseline — a Reset cannot write the old movement's numbers onto the new movement, with no baseline code.
The deload's surviving sets keep their baseline rows, so a Reset still restores the pre-deload targets on
them; that is the reset's literal meaning and it is unchanged.

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
  **Not now**: Use writes the **plan's own working sets**, by place, from the session's work sets — not
  every logged set: a row that was **substituted** (N291) is skipped, because it did not train the plan's
  movement, and a session with more work sets than the plan has its **extra sets truncated** (those are
  N311's business); reps, target effort and warm-ups stay, unmatched plan sets stay empty, in one
  transaction. Not now leaves the plan weightless; either clears the marker. The plan then has a load, so
  the next session prefills from it and load progression can run. A plan-editor toggle offers the same
  weightless prescription outside the replace flow. No baseline work.

- **N310 — The decision on both surfaces.** The finish dialog
  ([WorkoutStallDialog.kt](app/src/main/java/com/example/androidapp/ui/workout/WorkoutStallDialog.kt))
  stops being information-only: for the workout's just-stalled lifts (the run just reached three) of an
  active weekly program, each gets **Deload** and **Replace**; *Done* is do nothing. The actions **hand off
  to the plan**: the deload closes the workout and opens the program editor's deload confirm (the
  before → after, and the other programs only when there are any), and the replace opens the plan's replace
  sheet; the undo lives on the plan screen, so it is not lost when the workout closes. The stall card,
  shared by the program editor and the weekly review, lists **every** stalled lift of the program rather
  than the first — the finder gains a program-wide list — each with the same actions and its run length,
  with the week-level "lighter week" kept as a secondary link. The card's actions are offered on any
  program; the finish sheet stays active-weekly. The calibration prompt from N309 runs before the stall
  sheet. This row also amends N305's "information, not a decision" rule, changes the screen states from one
  stall to a list, and carries the tags and the announcements.

- **N311 — The plan earns a set back.** The counterweight to the trim: a plan edit that **adds** a working
  set, offered from evidence and never automatic. Two triggers, one action. **The extra set you keep
  logging**: the plan prescribes N working sets and the session logged N+1, in the session just done and
  in the previous finished one; the suggestion fires when the pattern **reaches** two, as a
  **Done snackbar** — *"Bench Press: a fourth set logged in each of the last 2 sessions. Add it to the
  plan?"* — after the step dialog or the auto-progression banner, with **Add** and the usual undo, and it
  stays on the plan card while it holds if the snackbar is missed. **The plan is comfortable**: in the
  session just done and the two finished before it, every working set **earned a step** — the progression
  offer's own gate, not a fresh reading of the effort field, which opens on the plan's target — with no
  stall open; that one is offered at Done too, as the same one-shot snackbar when the pattern **reaches**
  three, with the plan card and the weekly review behind it.
  The write appends one NORMAL working set after the exercise's last set: for the extra-set trigger, from the logged extra set's weight, assistance and reps;
  for the comfortable trigger, from the last kept set's target
  ([TemplateRepository.kt](app/src/main/java/com/example/androidapp/domain/repository/TemplateRepository.kt)
  `addSet`). The plan's effort stays; one transaction; snackbar undo; the plan takes effect the next
  session. The gates: no open stall, not a deload week's template, and the plan's work-set count below
  **six** — the suggestion's cap, while the editor can still add more by hand. One suggestion at a time —
  when both hold at Done, the extra set is offered first, since it is the lifter's own work — and no stored
  "dismissed" state: each snackbar fires once at its threshold and the card carries the rest. The
  read is the same `templateSessions` the stall uses
  ([TrendsRepository.kt](app/src/main/java/com/example/androidapp/domain/repository/TrendsRepository.kt))
  plus the current session's work sets, the plan's count, the cap and the stall check; no schema. **No auto
  volume**: the auto switch stays exactly as it is, and an automatic volume step would be its own opt-in
  later.

**Docs for the batch.** DECISIONS records the deload rule (amending P3.10's "never computed" to "the lifter
chooses, the arithmetic is shown"), the replace rule (the shared trim, the new row and set ids, the seed,
the category suggestions, the kept rung ladders, the cue, the conditional shared-plan warning), the
calibration (the marker, the blank first session, the prompt, the substituted row skipped and extra sets
truncated, the 20 kg default exception), the surfaces (the hand-off to the plan, amending N305 and N237's
"one stall"), and the volume rule (N311: the extra set after two sessions, the comfortable plan gated on
earned steps after three, both offered at Done, the six-set cap, the plan card behind them, and no automatic
volume). N299 is unchanged: the per-lift deload is the "separate rule" it names, and the auto switch,
baseline and Reset are untouched. N16/B303 say why a plan edit clears the stall; N95/N110 are the
suggestion source; N291 is why a replace says "in the plan". Each row lands with its CHANGELOG entry under
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
