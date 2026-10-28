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

**A program's identity is not its name.** 2.17 was reported with a second, different program built in the import
helper arriving under the first one's `pgm_…` id: the app answered *That program is already here.* and added
nothing. The table path mints the id from the program's name and nothing else, so a name **is** the identity — two
different tables that both say *Push Pull Legs* collide, and every table without a `Program` column collides on
*My program*. The editor path already mints a unique id, which is why the report only arrives from a dropped
table. B266 gives a dropped file an identity of its own; B267 stops the app calling a different program "already
here", which is the half pasted tables still need, because a paste has no file name to seed.

| # | What it changes |
| --- | --- |
| B266 | **A dropped table's program id is a hash of its name, so two programs cannot share one.** `buildProgramDocument` mints `pgm_` plus `fnv1a64Hex` of a `program`-prefixed seed and the program's name (`docs/app.js:5247`), and `programName` falls back to `My program` when the column is absent or blank (`5246`) — so a second table named *Push Pull Legs* carries the first one's id, every unnamed table carries `pgm_200c7fe71197adaf`, and `mergeProgramDocument` ends the merge on the live id (`RoomProgramRepository.kt:740`), reports a zero summary and says *That program is already here.* about a program the device has never seen. **Seed the id with the document's source**: `parseProgramSheet` already receives `options.fileName` (`docs/app.js:1033`), so the sheet records it and the seed carries the source as well as the name — a different file with the same program name is a different program, while re-dropping the same file stays a no-op. The accepted cost is stated rather than hidden: renaming or *Save as* makes a second program, and a **pasted** table has no file name to seed, so it keeps the name-only identity and needs B267. Rejected: hashing the content too, because every spreadsheet edit would then arrive as a new program and duplicate the plan the lifter is training; minting a fresh id per download, because re-downloading a draft would import it twice. Pin all three cases — same file twice, a second file with the same name, and a paste — in the page's own gate. |
| B267 | **"That program is already here" is said of a program the device has never seen.** With a name-derived id an id match *guarantees* a name match, so the early return has nothing to compare and cannot tell a re-import from a different program carrying that name; a pasted table therefore still loses its program silently, and so does any collision the page cannot see. Compare the document's shape against the stored program's — the slots' template ids are enough, and the rows are not yet written at that point — and when it differs say that a *different* program with that name is already here and nothing was added, instead of the idempotent sentence. Leave the true no-op silent. Rejected: reporting every id match as a clash, because re-importing the same document is the common case and must stay silent. |

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The timed set (N220) has shipped to the unreleased section.

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
