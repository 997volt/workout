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

## Next — the 2.33 whole-app review fixes

A whole-app review of 2.33 covered every module, the shared core, the import helper, CI and the
documentation. It found no security or privacy defect and the gates pass, so this batch is correctness
and contract work rather than a broken build. The entries are ordered by severity; each leaves this list
in the commit that fixes it, so an entry still here is open. N355 is a 2.32 regression and the only
data-loss defect: the export is the app's only escape hatch, and a fresh restore of a file taken after
any template-started workout currently fails. The review did not run the instrumented suite (its report
is stale on this checkout) and Robolectric still pins SDK 35 against targetSdk 37; those are verification
gaps, not planned work.

- **N361 — A restored plan keeps one live set per index.** Un-hiding a planned set writes its old
  `setIndex` by raw `@Update`, while removing a set renumbers the survivors: export, remove the middle
  set, import, and two live rows share an index (a session snapshot's `REPLACE` then drops one). Restore
  through the same splice and renumber the set undo uses, with a round-trip test.

- **N362 — The goal-period editor keeps its draft through a rotation.** It is held in `remember` (with its
  draft keyed on it and its `saving` guard), so a rotation closes the dialog and discards the typed weight
  and both days. Hold it in a saver as the target dialog does, and make `GoalRowTest` type a value and
  record the callback — it currently does neither.

- **N363 — History detail's delete, save-as-plan and change-day dialogs survive a rotation.** Their
  flags are `remember` beside a `rememberSaveable` menu and edit mode, in an activity with no
  `configChanges`, so a rotation dismisses all three and loses a typed plan name. Save the flags.

- **N364 — A failed previous-performance read is not "never trained".** The detail stores a failure as
  `null` and never retries it, removing every "vs last time" delta for that movement for the life of the
  screen. Cache only a success, retry a failure, and surface it, as the field's own KDoc promises.

- **N365 — A failed attempt is not a warm-up at the write boundary, and not a plan place in the rest
  timer.** N346 fixed the picker's rung refusal but not the repository twin, which still says "warm-up"
  when the anchor is an attempt; and `runContinues` counts every row, so an attempt starts a rest between
  an anchor and its drop. Add the failed-attempt cause to the boundary's rule (or share the picker's
  helper), read the place rule (`occupiesAPlanPlace`) in the timer, and test both.

- **N366 — The rest choices wrap at a large font.** Five fixed chips sit in a non-wrapping `Row`, so the
  last values clip instead of moving to a second line; the same screen's progression chips already use
  `FlowRow` for this reason. Wrap them.

- **N367 — A deload's workouts are named in the order they are written.** The name comes from the source
  day's index, so unticking the first day writes "Deload B" as the week's first workout, contradicting the
  builder's own KDoc and pinning the odd name in a test. Number from the emitted position and keep the
  weekday from the source day.

- **N368 — The helper enforces the document floor, and its catalog status is true for a current export.**
  The page checks only the ceiling and then stamps the working copy at the current version, so a v1–3
  document the app refuses is opened and re-badged (and a program taken from a newer backup is laundered
  the same way); its catalog status also compares a loaded export to the 5 the page writes, calling a
  current export unreadable. Export the floor beside the ceiling, refuse below it, and phrase the status as
  newer-than-this-page rather than wrong.

- **N369 — The documentation states today's numbers and commands.** DECISIONS carries stale backup,
  schema, document-version and formatting-findings facts; re-measure the formatting count with the command
  it already names and correct the others against the code. AGENTS' local fixture proof uses `git diff`,
  which cannot see a new untracked fixture (CI uses `git status`), and the helper page still says the only
  file it writes is the backup.

- **N370 — The stale and orphaned KDocs the review found are removed.** An orphan block before
  `weightGoalOutcomes` links a `TARGET_TOLERANCE_GRAMS` that does not exist, two KDoc blocks sit on one
  property in `StatisticsViewModel`, `RecordTimelineTags` still calls itself temporary after the split
  landed, and `StatisticsDao` describes a live-record divergence that `recordsAgainst` already merged.
  Delete or correct them.

- **N371 — The review's small app fixes.** A failed calibration read with no marked exercises stores an
  empty, non-null notice list and draws a button-less modal (make it null); a failed `recentBlocks` read is
  shown as "no blocks" (surface it); `planned_not_done` omits the plan of an untrained day (rename or count
  it); `historyGroups` normalizes the query with its locale but the row words with the machine default
  (pass it through). One commit can carry the four, with the comment that names each.

- **N372 — The review's tooling cleanups.** The single-file build splices raw text between script tags and
  never loads the page, so a `</script>` inside an inlined file would truncate it and still pass; the
  formatting plugin is still declared on the main detekt classpath beside the B289 comment that says only
  the formatting task loads it; and Turbine is declared, documented as used, and imported nowhere. Refuse
  the sequence and smoke-load the result, drop the duplicate declaration, and use or remove Turbine with
  its coroutines exception.

## Later

- **N330 — A 1RM test has its own view.** Once N329 exists, a test's attempts can read as one story:
  warm-ups, the made single, the failed attempts above it, and the tested result. A guided flow — rest
  between attempts, an attempt ladder, the result written once — can follow later. It is not a second data
  model: a 1RM test is an ordinary session whose sets carry N329's roles.

- **N332 — A custom range for the training block export.** N331's **Change** offers the recent sequence blocks
  and **Last 8 weeks**; a custom start and end date is the one design option the sheet does not surface yet.
  The repository's range read is already in place. Add the two date prompts to the sheet, keep the same
  all-in-one CSV and warning, and test the chosen dates and an empty range.

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
