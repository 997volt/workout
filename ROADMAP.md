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

## Next — the second unreleased-changes review

The unreleased batch was read again after the first review's fixes (B487–B493) landed. The earlier rows are
shipped and the batch now includes them; this read is the whole set with their seams in view. The fixes hold
on their own paths, but four edges remain where they meet older data: a block file can clear the baseline of a
live weekly program with the same id, an old file's weekly baseline still imports while its switch is off, a
block's legacy flag has no way to be turned off from the screen, and the preview counts snapshots the rest of
the preview hides. Nothing was fixed during the read.

- **B495 — An old file's weekly baseline imports while its switch is off.** The B490 filter drops a block's file
  baselines, but a pre-B477 file can carry a baseline for a **weekly** program whose `autoProgression` is false;
  [RoomBackupRepository.kt](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt) inserts it,
  recreating the hidden rows B477 says disabling drops. The switch and its baseline are one story only for the
  paths that write; the import still has two. Fixed means a baseline is imported only for a weekly program whose
  file switch is on, with a test for the old weekly auto-off row.
- **B496 — A block's legacy switch has no way to be turned off.** B487 hides the switch for a block and refuses
  enabling in [RoomProgressionRepository.kt](app/src/main/java/com/example/androidapp/data/RoomProgressionRepository.kt),
  but the repository still allows turning it **off** and
  [ProgramProgressionScreen.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramProgressionScreen.kt)
  now shows a message with no control at all. A block row from before B475 (or a hand-edited database) with the
  flag on therefore keeps it for ever, ignored by the resolver; the B475/B490 paths cannot reach a row that is
  already live. Fixed means the block state offers a turn-off action when the flag is on, or a one-time cleanup,
  with a test.
- **B497 — The preview counts snapshots the rest of it hides.** The B489 `planSnapshots` count includes every
  file snapshot, while the sessions/sets counts in the same preview filter rows whose session exercise or
  exercise is hidden (`visibleSetIds`). A file can preview a "Plan snapshots" row for a session the rest of the
  preview excludes; the import restores the snapshot under a hidden parent. Fixed means the snapshot count uses
  the same visibility rule as the sets, with a hidden-parent test.

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
