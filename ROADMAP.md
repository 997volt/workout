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

**The N157–N160 review's findings (B173–B175, N161–N164).** A reading of the unreleased batch and the new import
helper. Two are substantive: the merge the specification calls v1 is not built, and a paste with no source name
can silently share session ids with an earlier one. The rest are one refused sheet shape and four small
deviations from the spec.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| N161 | **medium** | **The tool cannot merge several sheets, which the spec calls v1.** Dropping more than one file reads only the first and silently ignores the rest; a second paste or drop replaces the rows; there is no sources list and no same-date combine. The workaround is one import per sheet, which does work because a file's name is the seed. | Add a sources list: each load appends, with its file name and row count, and the seed becomes per source. Show same-date sessions with a **Combine** action that unions their exercises and renumbers positions. Refuse a duplicate file rather than double it. |
| B173 | **medium** | **Session ids collide across separately loaded lists.** The id keys on the seed, the sheet and the source row, and a paste with no `source` field defaults both seed and sheet to `pasted-rows`; two sheets pasted separately, each with a session at row 1, produce the **same id**, so the app's merge silently skips the second sheet's sessions. | Include the session's date in the id, so two sessions at different dates cannot collide while a re-import of the same row still does not duplicate. |
| N162 | **medium** | **A row with a date or an exercise but no set is refused.** A notes-only day header or an exercise heading — ordinary notebook furniture — becomes an incomplete set and blocks the export with three messages. The spec's rule that every row is a set is what the tool implements, so the rule needs a definition rather than a patch. | A row with no reps and no weight carries the date, the exercise and the note and adds **no set**; warn when it carries nothing usable. Say so in the spec, and cover a notes-only row and an exercise heading. |
| N163 | low | **The session's zone offset is derived and cannot be corrected.** A `zoneOffsetMinutes` in the input is ignored and the offset comes from the browser's zone at that date, so a sheet trained abroad can put an evening session on the wrong day. | Either an offset control per session, which the spec already promises, or an explicit statement that the browser's zone is assumed. |
| B174 | low | **The export is not ordered by date.** Sessions are written in row order, so a sheet whose dates run out of order produces a file in that order; the spec says output is ordered by `startedAt`. The app sorts on read, so nothing breaks today. | Sort sessions by `startedAt`, ties broken by source order then row, before writing. |
| B175 | low | **Epoch-millis dates are refused.** The spec accepts a timestamp as an escape hatch for a model that emits one; the parser requires text, so such a row blocks with "a date must be written as text". | Accept a number as epoch millis, or drop the promise from the spec. |
| N164 | low | **The single-file build the spec describes does not exist.** The spec says the three files concatenated must behave identically; there is no concatenation. | Add the one-command inline build, or drop the claim from the spec. |

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

**Nothing.** The app-side fixture (N160) moved to *Next*, and the section drains the same way *Next* does.

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
- A web dashboard — a surface that **reads** the training record. The import helper (N159) only
  authors a file and never sees the database, so it is not one.
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
