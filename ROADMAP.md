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

The review of the unreleased batch (N248–N254, B329), grouped by the unit a fix touches so one group is one
sitting. The `B` rows are claims the code does not keep and the ones to take first; the rest is what the batch
left behind. One row is picked up at a time; a row that ships leaves this file for
[CHANGELOG.md](CHANGELOG.md).

### The week in review

| # | Change | Why it matters | Size |
| --- | --- | --- | --- |
| B331 | **What moved counts working sets.** Restrict the week aggregate to `SetType.recordsEffort` roles, passed as a parameter the way `StatisticsDao` passes them, instead of the raw top-and-count over every role; cover it with a DAO case where a warm-up is heavier than the work and is no gain. | B17's rule is that a heavy warm-up is not a best, and the new *What moved* section reintroduces the defect it fixed: a 120 kg ramp set over 100 kg of work produces a gain, and the same row inflates the top before the week and hides a real one. | Small |
| B332 | **What moved does not claim "was 0 kg".** A movement trained before only at bodyweight has prior sets and a zero top, so its first weighted week prints "(was 0 kg)". Treat an unbacked previous weight as no number, or name the bodyweight case; cover the return in the gain test. | The entry's own rule is that a bodyweight row "does not claim a number it cannot back"; this is that number, on the most common way a lifter starts loading a pull-up or a dip. | Small |
| N255 | **The week's reads do not lie on failure.** A failed `activePrograms()` becomes `hasActivePlan = false`, so the screen asserts "No program is being followed"; propagate the error like the other reads. In the same area, `WeeklyReviewDao`'s doc says "live exercises" but the query has no `deletedAt` filter — add it or drop the claim. | N252's point was one `DataResult` end to end; the one read still swallowed now feeds a factual sentence, and documentation that promises a filter no query applies is how the next reader adds the bug it describes. | Small |
| N256 | **The week's units and its duplicated sentence.** Pass `unit.label()` as a format argument instead of concatenating "number unit" in Kotlin (the volume and moved lines), give the volume unit the changelog line it needs, and drop one of the two "no weekday schedule" sentences that can show together. | Every other screen formats the unit through the string, so the concatenation is the one place a localization would have to find; the volume label is a user-visible change no entry mentions. | Small |

### Docs and the gate

| # | Change | Why it matters | Size |
| --- | --- | --- | --- |
| B334 | **One `### Changed` under `[Unreleased]`.** `97e49e3b` inserted N254 above the existing heading, so the section appears twice, with no blank line between the bullet and the second heading. | The changelog's structure is the release's outline, and `DocsConsistencyTest` checks links and task sets, not headings, so nothing else notices. | Small |
| N259 | **Keep `tools/gate.sh` in step, and survivable.** It repeats the task list `DocsConsistencyTest` compares only between AGENTS.md and the workflow, so a CI change leaves the script stale; and `set -e` aborts the gate when `adb devices` fails though stopping emulators is best-effort. | N253 exists to make the gate reliable, and a workaround that drifts from CI or refuses to run is the same flake wearing a different coat. | Small |

### The tests the batch left behind

| # | Change | Why it matters | Size |
| --- | --- | --- | --- |
| N260 | **The batch's test debts.** Truth in the new ViewModel test rather than JUnit assertions, and a gain assertion that checks the row rather than that the list is not empty; a DAO case for a deleted session, since "deleted session or set" deletes a set; a per-block test tag instead of one `WEEKLY_REVIEW_BLOCK` on every line. | These are the shapes the rows above slipped through: the batch's happy paths are covered and its claims are not, so a regression would pass the suite that is meant to guard them. | Small |

## Later — decided, and not being started

Nothing is queued. This section is where agreed work waits its turn: a row here has its design settled so it
does not have to be re-argued, and a row in *Next* is what is actually being worked on. Both drain the same way —
a row picked up moves to *Next*, and one that ships leaves for the changelog.

The batch that was here — the 2.19 review's three data decisions (B326–B328) and three product rows (N245–N247)
— shipped in [2.20](CHANGELOG.md). A row returns here only once its design is settled and it is agreed to be
next.

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
