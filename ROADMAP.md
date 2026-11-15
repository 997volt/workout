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

## Next — the findings from the 2.30 review

The review of everything unreleased at `ffcf0320` (N304–N311 and their fixes B420–B444) found the findings
below: the local gate and the full 510-test instrumented suite were green on the reviewed tree, so each row
names a path the tests do not cover. The rows are ordered by severity.

- **B453 — The import preview's "and N more" crashes (pre-existing).** Found while checking the batch's format
  strings: [ImportPreviewDialog.kt](app/src/main/java/com/example/androidapp/ui/transfer/ImportPreviewDialog.kt)
  (:101) passes no format argument for a `%1$d` plural item, so a file with more than three problems throws
  `MissingFormatArgumentException` when the preview opens. Pass the count. It predates this batch.
- **N313 — The review's missing tests.** B438's promised race tests never landed; B439's weekly failures have
  none (`failStalls` is never flipped and no screen test carries `stallError`/`volumeError`); no Compose test
  renders the workout's volume snackbar, which is why B445 shipped. Add those, and the second-row dialog test
  B447 needs.
- **N314 — `makeWeightless` has no production caller.** [TemplateRepository.kt](app/src/main/java/com/example/androidapp/domain/repository/TemplateRepository.kt)
  (:250) and [RoomTemplateRepository.kt](app/src/main/java/com/example/androidapp/data/RoomTemplateRepository.kt)
  (:633) are reached only by tests and fakes: the plan-editor toggle and *Empty* use `setExercisePlan(findWeight)`
  and `replaceExercise(empty = true)`. Delete the port method and its overrides, or route the toggle through it.

The review also verified what these rows do not touch: schema 25 equals schema 24 plus the defaulted marker
(entity, JSON and migration agree), the backup carries it and the shared document drops it, B440's targeted
update preserves it, the new plan writes are each one transaction, and the N310 hand-off, the
calibration-before-stall order, the B422 close, the B442 filter, the B423 bound and the N306 signed-load delta
all hold.

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
