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

**A review of everything since 2.12 found these, and the gates hid almost all of them.** The range is the
finite-program block (P3.21, N174–N178), the six rows that preceded it (N179–N185), and the helper's authoring
of one (N186). One of the rows is already red in the instrumented suite (B193); the rest pass every gate, which
is the finding: each is a gap between what the changelog and [DECISIONS.md](DECISIONS.md) say and what the code
does, so nothing fails when it regresses. Defects first, then the tests and references that let them through.
Nothing here is fixed.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| B198 | high | **A block authored in the helper is written at the opened document's old format version.** `editorOpenSnapshot` deep-copies the opened document and the download writes it verbatim, so a file opened at v4 and turned into a block downloads at `formatVersion` 4 with `kind: SEQUENCE` inside. A 2.12 build's floor is 4, so it accepts that file, drops the name it does not know and repeats the block as a week forever — the hazard N185 moved the ceiling for, reintroduced by the page's own write. | Stamp the core's version on open (which also upgrades an old draft), and test that an opened v4 document edited into a block is written at 5. |
| B200 | medium | **The rest line can name a session the deload week dropped.** `nextWeeklySession` reads the program's raw slots, while the day's plan replaces a deload week with the built plan's days — so on a deload rest day the line can say *Next: Monday · Push* for a Monday the plan removed, and a deload day built on another weekday is never named. It also ignores this week's substitution when it names the workout. | Derive the line from the same plan the day card is built from (deload and substitution included) rather than from the slots, so the two surfaces cannot disagree about what comes next. |
| B201 | medium | **The block's new controls are unnamed.** The start-date row is a *Change* button beside a separate text, so a screen reader hears "Change, button" with no object; the picker's *Set* and *Cancel* carry no TestTags, which is why the test asserts the English "Set"; and a block's weekday chips repeat Mon–Sun per day under a bare "Due" with no group, so whose due day is being set is not exposed. This is the rule B188 applied to the helper, in the app's own editor. | `onClickLabel` on the change, and spoken names with tags of their own on the picker's answers; a block's weekday chips carry the day's row in their label, or sit in a list/row semantic that does. |
| B202 | medium | **The helper's preview summary states the opposite of the plan for a block.** The summary line passes only a change's *kind* to the sentence builder, while the two new sentences read its *detail*: making a program a block that starts on a day renders "1 × make the program weekly · 1 × start the block the day it was created" directly above the itemized list that says the right thing. | Pass the change itself into the summary, as the list already does, and cover it — the builder is not exported, which is why nothing caught it. |
| B203 | medium | **A block authored in the helper is shown as a weekly program.** The page has no *Due* and no *Day k of N* anywhere: the editor labels the placement control "Day" and the table preview prints the weekday, so the one thing that makes a block a block is invisible while it is authored. The table preview also never states the kind or the start, so a start a year off is invisible until the app refuses or the block is already overdue. | Label the control *Due* and the day *Day k of N* (the app has both strings), and put the kind and the start in the table's preview line beside its day and row counts. |
| B204 | medium | **The helper's start date rolls over instead of refusing, and formatting one can crash the editor.** `Date.UTC` normalises: `2026-02-31` becomes 3 March, `2026-13-01` becomes January 2027, `0000-01-01` becomes 1900 — so "Start must be a date" never fires for a well-shaped wrong day, and the block starts on a day its author never typed. In the other direction a millis-valued or huge `startDate` makes `toISOString()` throw out of the file reader, leaving the editor half built with no message. | Accept a date only when its year, month and day round-trip, and guard the formatter so a stored value it cannot render is a sentence rather than an exception. |
| B205 | medium | **The helper's kind and start disagree in two ways.** A table with a `Start` column and no `Kind` writes a start date onto a weekly program — a field the app's own editor hides for weekly and nothing reads. And switching a block back to *Every week* keeps its start date, hides the field that could clear it, and reports no start change, so the app keeps a date the lifter can no longer see. | Refuse (or drop and say so) a start without a kind, and clear the start when the kind becomes weekly — or keep the field visible so it can be cleared. |
| N187 | medium | **The block's behaviour is asserted where it cannot fail.** Nothing passes a `SEQUENCE` program to `todaysPlanFor`, which is how B194 stayed green; `SequenceTodayPlansTest` hands its schedule the `isDue` it wants, so "only when due" tests the fake; the editor's kind and start writes appear only as fakes that `error(…)`, and one test's comment claims the ViewModel and instrumented suite cover them; the "Day 1 of 2" case puts the run slot at index 0 and the weekly-chip test never asserts the block chip; the fixture pins change names but never their `detail`, which is what the page's sentences read; and the helper's new date and sentence helpers are not exported, so no runner can reach them. | One case per claim the changelog makes: a block is not a weekly plan, the skip prompt is not asked about it, a finished block says so, a kind and a start are written, *Due* and *Day k of N* appear, and the fixture compares each change's `detail`. |
| N188 | low | **Weekday names and date formatting have second homes again.** Home prints weekdays with `getDisplayName` rather than the app's `DayOfWeek.longLabel()/shortLabel()`, whose own doc says it exists because there were two sources and one went stale (F8); and the localized-date formatter is copied a third time (the edit dialog, the editor sections, the deload screen). `ProgramKind.fromName` is dead — the Room converter uses `valueOf`. | Use the label helpers home already has, extract the formatter at its second copy, and delete the unused `fromName` or use it where a stored name is read. |
| N189 | medium | **A block counts for nothing in the weekly record, and nothing else counts it.** The ratio, the grid, the breakdown and the streak exclude sequences — the right call, since a block's weekday is not a recurrence — but no surface replaces them: beyond the home card and the editor's label, a lifter on a block sees their streak and their month's ratio stop, with nothing saying the block itself is 4 of 9 done. | A block's own progress where the weekly record would otherwise be: the adherence screen says how far the block is, so training a block is not silent in the record. |
| N190 | low | **The docs still describe the table before its two new columns.** IMPORT-HELPER lists `Kind` and `Start` without saying what they accept, that they are said once for the whole table, or that a block's order is the rows' day order; its "a day with no weekday is a workout the lifter starts by hand" is false for a block (no weekday means the day after the last one); its "every part is editable" omits the kind and the start; and the page's own column hint and missing-column message still name the old set, so the new columns are undiscoverable from the page. | State the two columns where the table's columns are stated, fix the two sentences that are only true of a weekly program, and add them to the page's hint and its missing-column message. |

B194–B196 are one mistake on three surfaces: a block is still read as a week by the day's plan, by the
missed-day question, and by the schedule's own match. B193 and B199 are the suite around it — a red run and
seven checks that stopped running, both of which a green report hid.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.**

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
