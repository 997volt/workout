# Workout — Roadmap

> **What is planned, not what happened.** *Next* is the ordered batch, *Parked* is decided against until a
> named trigger fires, and the non-goals are lines this app does not cross.
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

## Next — the template editor and two statistics fixes

Three changes asked for after using the plan editor and Statistics.

- **N378 — A template exercise can be replaced, with similar movements first.** The template editor's
  exercise row gains a **Replace** action, and it opens the **same replace sheet the stalled lift uses**
  (N308) rather than a second flow: the same confirm, the same *Empty* choice, the same new-plan-row
  semantics and the same undo. The sheet's first tier is the movement's own library family — the
  suggestions it already offers when a stalled lift is replaced — so a similar movement is one tap; the
  browse list remains beside it and reaches the **whole** library, so the suggestion is a shortcut, not a
  restriction. A movement with no family has no first tier and the browse list is the way out. Everything
  N308 already settles is unchanged: the new row takes the old position with new set ids, the old row is
  soft-deleted, the kept work sets are seeded by place from the new movement's last finished session (or
  cleared by *Empty*), the plan's targets, effort and rungs survive, and the undo takes the new row and
  sets back. Replacing with the same movement is not offered. **Tests**: the row's Replace opens the sheet,
  the family is shown before Browse, Browse reaches a movement outside the family, the kept sets and
  targets are the N308 rule, and the undo restores the old row.

- **N379 — Statistics' custom range opens with To = today.** Choosing the custom range in Statistics
  starts with **To already on today's date**, so the common "since X" window needs one pick rather than
  two; **From** keeps the range's current start when it has one and is empty otherwise. Today comes from
  the same source the range windows already use — the statistics ViewModel's `TimeSource` today, passed
  to the dialog — not a second clock inside the composable. When the kept From is after today, the shared
  dialog's order rule holds Apply and says which end to move (N35, N332). The block export's custom range
  is not Statistics and keeps its blank ends. **Tests**: opening Custom from a rolling range shows From
  empty and To today; a stored From is kept; a From after today is refused; the export dialog is
  unchanged.

- **N380 — A favourite series cannot also be in Latest.** The Latest row draws the recent selections
  **minus** the ones already in Favourites, using the same `StatisticsSelection` identity the pin stores,
  so one series has one home and the same chip cannot sit in both rows. The stored recents are filtered,
  not rewritten: unpinning a series brings it back to Latest if it is still a recent selection, and a
  favourite that was never recent does not enter it. The row's existing rule that it hides while fewer
  than two series are left still applies to the filtered list. **Tests**: pinning a recent removes it
  from Latest, unpinning returns it, a favourite pinned from the picker is not added to Latest, and the
  row hides when filtering leaves fewer than two.

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
