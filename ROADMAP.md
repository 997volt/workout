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

Nothing is in flight. The 2.19.1 patch is in [CHANGELOG.md](CHANGELOG.md); the queued work below is the next
batch, and it starts with the resilience rows.

## Later — decided, and not being started

Nine rows are queued. This section is where agreed work waits its turn: a row here has its design settled so it
does not have to be re-argued, and a row in *Next* is what is actually being worked on. Both drain the same way —
a row picked up moves to *Next*, and one that ships leaves for the changelog.

The first three are the **resilience batch** the 2.19.1 crash earned, the next three are the data decisions the
2.19 review left open, and the last three are product. The table's order is the suggested order.

| # | Change | Why it matters | Size |
| --- | --- | --- | --- |
| B324 | **A failed housekeeping pass must not take the app down.** Catch a throw from the seed's repairs in the database-open callback, keep the app usable, record the failure and say it once with a retry. The seed writes in one transaction, so nothing is half-done; a silent skip is not the answer. | Every upgrade runs these repairs on data no test has, and one data-dependent foreign key made 2.19 unusable. | Small |
| B325 | **Exercise the upgrade path the app actually opens.** Run the seeder callback in the migration-chain test, and add an instrumented case with the real shape — a seeded row, an import's duplicate under a ruling's survivor name, and session/template references — asserting no constraint failure and no orphan. | 2.19's crash was invisible to both suites: the chain test opened Room without the callback and the merge test had no database. | Small |
| N244 | **Diagnostics.** Settings shows the last crash or failed repair, its stack and a *Copy details* action. | A local-only app with no reporting needs a way to hand over what went wrong; the 2.19 report cost a backup and a reproduction. | Small |
| B326 | **Decide the backup ceiling for a plan's hold.** Move `schemaVersion` to 8 with the helper's writer, or record the acceptance that a pre-hold build silently drops `targetDurationSeconds` on restore. The floor stays. | A new backup restored into an older build is the wrong-meaning case B322 fixed for documents. | Small |
| B327 | **Separate "the old seeder wrote 0" from "the lifter chose off".** Give the seed's flags a one-time provenance marker (or a migration), so B299's never-edited guard stops skipping a row the lifter edited for another reason. | A Plank whose cue was edited before the repair still reads as a counted lift, which is the bug the repair exists for. | Medium |
| B328 | **Validate a document where it is added.** Run the shared validation at the add boundary, or write down the trust rule and why; today the merge bypasses the write boundary. | The page refuses a reps-and-hold or zero-second hold the app will store. | Small |
| N245 | **Merge movements.** A screen to combine two library rows: pick the survivor, move the sessions and templates, record the retired mapping. | N238's auto-merge deliberately leaves a configured row alone, and deleting one is the only way to settle it — which takes its history. | Medium |
| N246 | **A restore preview.** Say what a backup will add (sessions, sets, measurements) and that nothing is overwritten, before the merge. | The import is the app's riskiest write and currently says nothing first. | Small |
| N247 | **A weekly review.** One screen of planned versus done for the week, what moved and what stalled. | The deload, stall and plan-review rules already compute the pieces; the app stops one step short of its promise. | Medium |

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

Everything that has **left** *Next* has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
