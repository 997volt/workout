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

### Coverage and one scope question

- **N325 — The auto progression seams that let these through are uncovered.** No test asserts the
  banner's plural message for a batch of two steps (the per-step writes and batch undo now have tests),
  or a weekly program with the switch on next to a block that names the same template. The enablement
  text and the baseline line have screen tests only for their presence. Fixed means those cases are
  asserted, and B471's historical plan is pinned at whichever level the decision puts it.

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
