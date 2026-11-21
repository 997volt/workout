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

## Later

- **N330 — A 1RM test has its own view.** Once N329 exists, a test's attempts can read as one story:
  warm-ups, the made single, the failed attempts above it, and the tested result. A guided flow — rest
  between attempts, an attempt ladder, the result written once — can follow later. It is not a second data
  model: a 1RM test is an ordinary session whose sets carry N329's roles.

- **N331 — Share the last training block as one CSV.** History gains a **Share training block** action with
  one sheet, not a report builder. It shows the block, its window, sessions, sets and missed planned sets,
  offers **Change** (the last few blocks, last 8 weeks or a custom range), and a read-only **Includes** list;
  there are no include toggles and no format picker, and **Download CSV** / **Share CSV** are the explicit
  consent. Because everything is on by default the sheet also says, in one line, that the file includes
  readiness, soreness, joint pain and body measurements, and to share it only with someone who should have
  that data. The app stays local-only: the file leaves only through the user's Download or Share action, the
  app never learns the recipient, and a shared temp file is deleted afterwards.

  **The block.** The most recent **sequence program**, from its start date to its last session when the block
  is complete, or to today while it is in progress; every finished session in that window is included,
  including extra workouts, labelled **Unplanned**. Sessions are labelled by the block's days through the
  existing slot assignment. With no sequence program the sheet does not invent a block: it offers **Last 8
  weeks** or a custom range and says "No block found".

  **Everything by default.** Every finished session and activity; every set including warm-ups, working,
  top, drops, clusters, to-failure and failed attempts; loads, units, reps, holds, RPE, assistance and the
  set's bodyweight; the plan's targets and whether each planned set was done; set and session comments; the
  session's readiness note, sore muscles and joint pain; body measurements in the block; the block's
  scheduled days with done / skipped / missed / upcoming. The report is scoped to the block's training and
  body — not app settings, the library catalogue, deleted rows, workouts outside the block, or the full
  backup.

  **The file.** One CSV, UTF-8, RFC 4180 quoting, ISO dates and dot decimals, with a `record_type` column and
  stable `D1` / `S1` keys instead of internal ids. One file is the point of "download the block"; the record
  types let a coach filter in a spreadsheet:

  - `block` — one row: name, start, end, status, exported-at and the block's totals.
  - `day` — one scheduled block day: date, weekday, block day, done / skipped / missed / upcoming, session key.
  - `session` — one finished workout or activity: start, finish, duration, workout, block day, comment,
    readiness note.
  - `set` — one planned or performed set: session/day, exercise, set number, role, planned reps/load/RPE,
    actual reps or hold, load, unit, assistance, bodyweight, RPE, comment, `performed`. A planned set that was
    not done is `performed=false`; a failed attempt is a performed row with `role=Failed attempt`, `reps=0`
    and the attempted load, so a coach sees it without it entering volume.
  - `activity` — session, activity, duration, distance, comment.
  - `measurement` — date/time, metric, value and unit: scale weight, body fat, tape sites.
  - `sore_muscle` — session, muscle, score.
  - `joint_pain` — session, site, side, score.

  The set's bodyweight snapshot and a `measurement` bodyweight row are deliberately both present: they are
  different facts. The file name is `workout-<block>-<start>-to-<end>.csv`; **Download** uses the existing
  SAF `CreateDocument("text/csv")` path, and **Share** uses `ACTION_SEND` with a `FileProvider` URI.

  **The data path.** A repository selects the block, its planned and performed rows, measurements and
  recovery rows; a pure report model and a single CSV writer turn them into the file; a ViewModel owns the
  sheet, the preview and the write outcome; a `FileProvider` carries the share. Tests: block selection
  (complete, active, no block, extra sessions), planned-but-not-done rows, failed attempts, measurements and
  recovery, CSV escaping, ISO dates and units, the share intent and the sheet's warning and contents.

  **Rejected.** HTML or PDF (CSV was chosen), reusing the backup JSON (machine-shaped, all health data, not the
  block), include toggles (everything by default, with a read-only review), and a server or email integration
  (local-only). A zip of separate CSVs is the one alternative kept if a coach specifically wants four clean
  tables; the default is one attachment.

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
