# Workout — Roadmap

> **What is planned, not what happened.** *Next* is what is being worked on, *Later* is decided and waiting,
> *Parked* is decided against until a named trigger fires, and the non-goals are lines this app does not cross.
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

## Next

**The pre-release review's twenty findings (B148–B160, N143–N146).** A senior review read all 200 sources before
2.6 went to a wider audience, checked every claim against the code, and reproduced the worst of them on a device.
One is visible on a fresh install, three corrupt or lose data, and none of them is caught by `lint`, `detekt` or
the 1320 JVM tests — which is the finding behind the findings.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| B148 | **high** | A seeded movement and a seeded category share the id `back-extension`, so `INSERT OR IGNORE` keeps the category and silently drops the movement: *Back Extension* can never be offered or logged, and the head reads "1 exercise" where the seed declares two. The `parentOf` entry is the self-parent the write boundary refuses. | Give the key spaces disjoint slugs, repair the row an upgrade already holds, and assert the two lists never intersect. |
| B149 | **high** | An import restores soft-deleted rows **by id alone**, so it un-hides a row a *save* replaced: two live measurements for one day and kind, or one measurement with quads twice. | Un-hide only what the parent's own delete hid, correlated by the timestamp the delete wrote. |
| B150 | medium | A backup's open session is imported with `finishedAt` null, so the file decides what the app is currently doing. Import during a workout and two open rows exist, the older unreachable. | Close a restored session at the boundary. |
| B151 | medium | `save` reads the day's existing entry **before** its transaction, so a save committing in the window leaves two live rows for one day and kind. B135 fixed the mirror half; this half is open. | Move the read inside the transaction. |
| B152 | medium | Removing a run's first rung strands its survivors: the run's value lives on the opening rung, so they read "no load to drop" and cannot be re-valued. | Take the whole run, as the anchor case already does. |
| B153 | medium | A cluster rung under an assisted anchor derives only the weight half of its load, so it prefills 0 kg, discards the machine's help, and the plan row agrees. | Carry both halves of the derived load. |
| B154 | medium | A superset whose members plan different set counts stops resting once the shorter member runs out, silently, for every remaining set. | Treat a member with nothing left to do as caught up. |
| B155 | medium | The Records count excludes `WARMUP` only, while the rule is `recordsEffort` — so a DROP or CLUSTER rung is counted as a record the app never announced, and blocks a later honest set from it. | Make the roles data, read from the one rule. |
| B156 | low | The plan read that seeds a workout omits the library's soft-delete filter its own twin has, so a plan naming a hidden movement seeds an invisible, unremovable row. | Join and filter like the twin. |
| B157 | low | A rung's last-time reps match a stored `setIndex` against a live position, and Undo appends rather than restores, so the index space drifts and the rung prefills from the anchor. | Match the previous session's row by position. |
| B158 | low | Three paths do their I/O on the main thread: document import/export, the export's crash-log read and encode, and the settings commit. | Move each to `Dispatchers.IO`. |
| B159 | low | A cancelled export leaves a 0-byte backup and says nothing, because the transfer runs in the composition's scope. | Run it where a rotation cannot cancel it. |
| B160 | low | The delete undo offer outlives its exercise being marked done, and the write is then refused with a sentence about a workout that is plainly open. | Ask the write's own question where the offer is made. |
| N143 | medium | Discarding a session whose only record is an **activity** asks nothing and destroys it: an activity holds no sets, so the prompt-free branch is exactly its case. | Confirm, and name what goes. |
| N144 | low | The review dialog prints a prescribed weight with no unit, and reports an assisted plan as "at 0 kg" because the assistance is stored and never read. | State the unit, and read the assistance. |
| N145 | low | A refused rung is told what a rung *is* rather than what is wrong, and the edit dialog offers Drop and Cluster where the log path gates them. | One sentence per cause, and gate both paths. |
| N146 | low | History has no search or filter; the month grouping is its only navigation. | A query and a filter over the list. |

This section holds only work a **reading or a use** turns up — a defect found by using the app, a question the log or
the plan can answer and the screen cannot, a batch's own batch-reading — and it is never filled to keep itself
full. An empty section is a state, not a gap: a row stands here while it is being done and leaves for
[CHANGELOG.md](CHANGELOG.md) when it ships, which is how this section drains.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The last row here — a goal's past, with each period saying whether it was met — shipped, and the
section drains the same way *Next* does.

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
| — | **Play Store listing** | You want distribution beyond `adb install`. Self-install works today, and Play App Signing would change who holds the signing key. |
| — | **Encryption at rest / app lock** | You start carrying the phone somewhere you would not carry the data. |

## Explicit non-goals

Permanent, unlike *Parked*: no trigger brings one of these back.

- Nutrition / calorie tracking
- Social feeds, friends and shared routines (P3.7, P5.2)
- A web dashboard
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

Everything that has **left** *Next* has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
