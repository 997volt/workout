# Import helper — specification

> **Status: implemented as [`docs/`](docs/), shipped as [N159](ROADMAP.md).** The durable rules behind it —
> why the conversion is outside the app, why the input is rows rather than the app's format, and why the ids
> are deterministic — are recorded in [DECISIONS.md](DECISIONS.md). Its output is decoded by the app's own
> reader: [`import-helper-backup.json`](docs/fixtures/import-helper-backup.json), regenerated with
> `node docs/fixtures/generate.js` ([N160](ROADMAP.md)).

A companion to the app for one job: turning old training spreadsheets into a
`workout-backup.json` the app accepts, with a review step that catches the errors the app cannot report.

The pipeline it serves:

```
spreadsheet → an LLM → rows of JSON → [this tool] → workout-backup.json → app: Settings → Import
```

## Why it exists

The app's import is deliberately strict and deliberately silent. It runs as **one transaction**
([`RoomBackupRepository`](app/src/main/java/com/example/androidapp/data/RoomBackupRepository.kt)), so a
single unresolvable reference rolls the whole file back. And the two messages a rejected file can produce
name no row: a body the decoder cannot read becomes **"That does not look like a backup file."**
([`BackupCodec`](app/src/main/java/com/example/androidapp/data/transfer/BackupCodec.kt)), and a constraint
failure becomes `DataError.Storage`, which the screen says as **"Couldn't read or write that file."**
([`DocumentMessages`](app/src/main/java/com/example/androidapp/ui/transfer/DocumentMessages.kt)).

Without a review step the loop is: import, generic failure, guess, repeat — over a file of hundreds of
rows. That loop is the whole product requirement.

There is a second reason, and it is the stronger one. The import **merges and never overwrites**: a row
whose id is already present is left alone
([`BackupRepository`](app/src/main/java/com/example/androidapp/domain/repository/BackupRepository.kt)).
So a wrong value that has already been imported cannot be corrected by importing again. The review step is
the only moment a bad value can be caught.

## Scope

In scope: **importing history** — sessions, their exercises, their sets, and the definitions of the exercises
they reference — **one movement's progress** drawn from the export the lifter loaded (N165), **daily body
data** from a Waistline backup (N166), and **a program document** from a table (N167).

Out of scope, each deliberately:

- **Editing a program.** The sheet creates one; the app inserts by id and never overwrites, so the app is
  where a program is changed. A full editor — substitutions, deload weeks, versions, skips — is not planned
  here. The document format itself is the app's
  ([`ProgramDocument`](app/src/main/java/com/example/androidapp/data/transfer/ProgramDocument.kt)).
- **`.xlsx` parsing.** Reading a zip container needs a dependency, which the constraints below forbid.
  Exporting the sheet as CSV is one menu item in every spreadsheet program.
- **A server, accounts, sync, telemetry, a service worker, or a build step.**
- **Editing the app's other collections.** The tool writes history, daily body data and a program
  document, and nothing else.
- **A statistics dashboard.** One movement's progress is a view of a file, not a surface that holds or syncs
  the record; family heads, multi-lift comparisons and every other trend stay in the app.

### This is not the "web dashboard" non-goal

[ROADMAP.md](ROADMAP.md#explicit-non-goals) lists *a web dashboard* as a permanent non-goal. This tool is
not one, and the distinction is the line it must not cross: a dashboard **reads** the training record and
implies a server that holds it; this tool **authors a file** and never sees the database. It has no
storage, no account, no request of any kind. The moment a feature here wants to read the app's data
directly, or keep it, it has become the thing the non-goal forbids.

## Constraints

| Constraint | Consequence |
| --- | --- |
| Zero dependencies | Vanilla JavaScript in a classic `<script>` (not a module, so it runs from `file://`), hand-written CSS, system font stack, no icon or web font |
| No build step | The source is the deployed artifact; nothing is generated |
| No network | No `fetch`, `XMLHttpRequest`, `WebSocket` or beacon; no third-party origin; a CSP as defence in depth |
| No persistence of health data | Everything lives in memory; the one thing that may persist is the exercise-name mapping, which is configuration rather than data |
| Runs from disk | File input plus `FileReader` and a Blob download only; the three files open from `file://` with no server |

## Files and deployment

The tool is deployed by GitHub Pages from `docs/`, which is the only folder a branch deployment can serve
besides the repository root. Three files and a marker, and no prose in that folder:

```
docs/
  index.html            markup and the CSP meta
  app.js                all logic
  style.css             presentation
  .nojekyll             stop Jekyll rewriting JavaScript that contains {{ }}
  build-single-file.js  writes one self-contained page to dist/ (N164)
  seed-catalog.js       the app's seeded library, generated and kept equal by a JVM test
  fixtures/             the synthetic backup the app's test decodes, its generator, the progress
                        fixture the app's trend generates for the tool's test page, the body-data
                        fixture, and the program document the app's codec decodes
IMPORT-HELPER.md        this file, at the root, never served
```

Enabling Pages (branch `main`, folder `docs`) publishes the tool at
`https://997volt.github.io/workout/`. The three source files also open from disk, which is the copy for anyone
who would rather not load it from a website; `node docs/build-single-file.js` writes
`dist/workout-import.html` — the same page with its stylesheet and script inlined — for a copy that needs no
neighbours at all.

## Input: rows, not sessions

The sheet is already flat and the grouping is deterministic, so the model that reads it should **transcribe
and not organise**. The canonical input is a list of rows, exactly as the sheet shows them:

```json
{
  "source": "2019-notebook.xlsx / October",
  "rows": [
    { "date": "24-10-2025", "exercise": "Bench Press", "reps": 10, "weight": 40, "unit": "kg", "type": "W" },
    { "date": null, "exercise": null, "reps": 8, "weight": 60, "rpe": 7, "type": "W" },
    { "date": null, "exercise": null, "reps": 5, "weight": 80, "rpe": 8, "type": "N" },
    { "date": null, "exercise": "Squat", "reps": 10, "weight": 50, "type": "W" },
    { "date": null, "exercise": null, "reps": 5, "weight": 100, "rpe": 8 }
  ]
}
```

A blank value carries down. That is the natural reading of a notebook, where a day is written once and an
exercise once, and every line under it is another set.

**Rejected: the LLM writes the app's backup format.** Asking a model for epoch millis, grams, half-points,
enum names and unique ids in one step is asking it to be wrong in five independent ways. Rows keep its job
to reading four columns; every mechanical conversion happens in code that has tests.

A nested form — `sessions: [{ exercises: [{ sets: [...] }] }]` — is also accepted and flattened, so a
document already produced that way is not wasted. It is not the recommended shape.

### Dates

Accepted: `24-10-2025`, `24/10/2025`, `24.10.2025`, and ISO `2025-10-24`. Two-digit years are refused
rather than guessed, and the parse is hand-written rather than `Date.parse`, whose handling of ambiguous
input differs between browsers.

**The ambiguity guard is mandatory.** `05-03-2025` is valid read either way, and a silent month swap
corrupts every chronological reading in the app — adherence, trends, the record history. So the tool parses
as day-month, counts the dates where day and month are both at most twelve, and says so above the list:
*"1,842 of 2,301 dates are ambiguous; read as DD-MM-YYYY"*, with a toggle that reinterprets the whole set as
month-day and revalidates. Under each reading it reports how many dates would fall in the future, which is
usually enough to see that one of the two is nonsense.

A date and the row's `time` (default `12:00`) become epoch millis in the browser's zone, and the stored
offset is

```js
zoneOffsetMinutes = -date.getTimezoneOffset()   // getTimezoneOffset counts minutes BEHIND UTC
```

which gives the app's convention — minutes east of UTC, so UTC+2 is `+120` — and follows daylight saving
date by date.

`durationMinutes` defaults to 60 and sets `finishedAt`, which is not optional: `finishedAt IS NULL` is the
app's definition of *a workout in progress*
([`WorkoutDao`](app/src/main/java/com/example/androidapp/data/local/WorkoutDao.kt)), so a session without
one would be picked up as the open workout.

### Grouping

Walk the rows in order, keeping the current date and the current exercise:

1. **A fully blank row is skipped.** It separates nothing and resets nothing.
2. **A non-blank date that differs from the current one starts a session** and clears the carried exercise.
   A repeated explicit date starts a second session on the same day.
3. **A non-blank exercise starts an exercise block** in the current session.
4. **A blank date means the current session; a blank exercise means the current block.** A non-blank value
   always wins.
5. **A row with reps or weight is a set**, with `setIndex` counting from 1 **including warm-ups**, because
   that is how the app stores them; it numbers the work sets separately when it displays them.
6. **A row with neither is context, not a set.** A date-only row opens the session and an exercise heading
   opens the block; the row's note joins the session's, and no set is written (N162). A row with a date and
   nothing else therefore neither blocks the file nor becomes an empty workout — it is left out with a
   warning if nothing follows it.
7. **A blank type is `NORMAL`.** That is the only default, and a sheet with no warm-up marking therefore
   produces an inflated preview — which is why every session header shows its warm-up count.

Clearing the exercise on a date change is the rule that matters: without it, the last lift of Monday
inherits every set of Tuesday.

Row-level diagnostics, each naming the row rather than the file: *no date yet*, *no exercise yet*, *reps
missing*, *weight missing*, *unreadable date*, *unreadable number*, *type not recognised*.

### Several lists

A year is more than one sheet, so the tool loads **any number of lists** and keeps each one's identity:

- every load appends; the sources panel shows each list with its row count and its own **revision**, and the
  same rows twice are refused rather than doubled;
- a list boundary is a session boundary — no date, exercise or set carries from one file into the next;
- an id names the list a row came from, so two lists that both start at row 1 cannot collide (B173);
- a date that two lists both trained is offered as **Combine**, which joins them into one workout, because a
  notebook often splits one day across two sheets.

### Units

Most rows are kilograms, and some exercises are recorded in pounds. The unit is therefore a property of the
**exercise**, not of the row:

- the mapping table carries a `kg | lb` control per distinct exercise name, remembered with the mapping;
- a session-exercise group may override it, and a mixed unit inside one group is an error;
- the intermediate may also carry `unit` per row, or `weightKg` / `weightLb` per set, for a sheet that says
  so;
- everything is stored in whole grams: `grams = round(lb × 453.59237)`.

A **new** exercise is created with `weightUnit: "POUNDS"` or `"KILOGRAMS"` — those exact strings, which is
what [`WeightUnit.fromName`](app/src/main/java/com/example/androidapp/domain/WeightUnit.kt) reads. A
**library** exercise is copied verbatim and keeps its own unit, so a pound weight logged against a
kilogram-display exercise stores 102,058 g and shows as about 102.1 kg. The data is right; the display
follows the exercise. The tool says so once per affected name and leaves the exercise's unit alone by
default.

### Only a movement is linkable

A schema-5 export carries the family heads beside the movements — `RowKind.CATEGORY` beside
`RowKind.MOVEMENT` — and it is ordered by name, so five names exist twice: *Deadlift*, *Hip Thrust*,
*Lat Pulldown*, *Lateral Raise*, *Overhead Press*. A logged set may only name a movement (the app says so
on `RowKind.isLoggable`), so a head is never matched, never offered in the mapping and never counted as a
deleted name that blocks a match: a name that matches only a head is **unmatched**, exactly like a name
the library does not have. An export written before that field carries no `rowKind` at all, and every row
in one is a movement, so the older files keep linking.

### The seeded library loads first

An export used to be the only way the tool could know the library, so without one every name became a new
exercise — and importing the app's own movements split their history from the seeded rows the app matches them
to. The seed is app content rather than the lifter's data, so the page ships a **generated copy** of it:
`seed-catalog.js` holds every category and movement with its id, name, stated taxonomy and parent, produced
from `SeedExercises.kt` and covered by `SeedCatalogTest`. A seed change fails that test, which writes the
expected file to `build/` so regenerating is a copy rather than hand-editing 126 rows. Loading an export still
adds the lifter's own exercises and the dry-run comparison; it no longer decides whether a name can be linked
at all.

### Roles

A row's `type` normalises to the roles the app stores, matched case-insensitively:

| The sheet says | Stored |
| --- | --- |
| `W`, `U`, `warmup`, `warm-up`, `warm` | `WARMUP` |
| `N`, `work`, `working`, `normal`, blank | `NORMAL` |
| `top`, `top set`, `1rm` | `TOP_SET` |
| `failure`, `F`, `AMRAP` | `FAILURE` |
| `drop`, `cluster` | **refused** |

`DROP` and `CLUSTER` are refused because they are rungs: their load is derived from the set above them
([`SetType`](app/src/main/java/com/example/androidapp/domain/model/SetEntry.kt)), so a spreadsheet number
for one would be read as a load the app then ignores or contradicts.

Warm-ups matter beyond ordering: `recordsEffort` is false for a warm-up, so a mis-typed warm-up becomes a
personal best. The editor therefore marks them in bulk rather than one at a time — a type control per row,
*first N rows are warm-up* per block, and a range selection that applies to every selected row.

### CSV

The same rows, as `date, exercise, reps, weight, unit, rpe, type, notes, time`, with the header matched
case-insensitively and a few aliases (`set type`, `warmup`, `w`). One line per set, blank cells carried
down exactly as JSON blanks are. CSV is the path that removes the model entirely, and it is the more
reliable one for a sheet whose columns do not change.

## Output: the backup file

`schemaVersion: 5`, `exportedAt`, and exactly four collections — the four the format requires, and
nothing else:

```json
{
  "schemaVersion": 5,
  "exportedAt": 1730000000000,
  "exercises": [ /* copied from the loaded export, or created for a new name */ ],
  "sessions": [ { "id": "imp_…", "startedAt": 1552586400000, "finishedAt": 1552590900000,
                  "zoneOffsetMinutes": 120, "notes": null,
                  "createdAt": 1552586400000, "updatedAt": 1552586400000 } ],
  "sessionExercises": [ { "id": "imp_…", "sessionId": "imp_…", "exerciseId": "…",
                          "position": 1, "createdAt": 1552586400000, "updatedAt": 1552586400000 } ],
  "sets": [ { "id": "imp_…", "sessionExerciseId": "imp_…", "setIndex": 1,
              "reps": 5, "weightGrams": 100000, "setType": "NORMAL",
              "rpeHalves": 16, "createdAt": 1552586400000, "updatedAt": 1552586400000 } ]
}
```

Every other collection is omitted, so it defaults to empty. Omitting them is not a convenience: a generated
`goals` or `measurements` block would be data the tool has no business writing. The file is pretty-printed
with two spaces.

### Conventions the output must honour

| Field | Convention |
| --- | --- |
| `weightGrams` | whole grams, a `Long`; kg × 1000 |
| `rpeHalves` | half-points: RPE 8 is `16` |
| `setIndex` | 1-based, contiguous within its exercise |
| `sessionExercises.position` | 1-based, contiguous within its session |
| `sessions.zoneOffsetMinutes` | minutes **east** of UTC |
| `startedAt`, `finishedAt` | epoch millis; `finishedAt` always present |
| `deletedAt` | omitted, or `null`; a value imports the row invisible |
| `createdAt`, `updatedAt` | the session's `startedAt`, so the same input produces the same rows |
| `templateId`, `activity` | omitted; a plain workout is the default |
| new exercises | `primaryMuscle: null`, `secondaryMuscles: []`, `equipment: null`, `movementPattern: null`; everything else defaults |

The tool never spells a taxonomy enum. It copies a row from the loaded export or writes `null`, so a change
to the seeded library cannot break it. New exercises state nothing because a row is allowed to state only
what it knows.

## Identifiers and revisions

```
id = "imp_" + fnv64_hex(seed | kind | sheet | row | date)
```

`kind` is `sess`, `ex` or `set`; the hash is two 32-bit lanes rendered as sixteen hex characters. Exercise
ids key on the **normalised name only**, so two files that both say *Cable Fly* produce one exercise.
Session ids key on the seed, the list, their own row and their date, and children derive from their parent
and index.

**The date is part of the identity** because two lists loaded under one revision both start at row 1: without
it, the second list's session would have the first's id and the app's merge would skip it as already present
(B173). A row whose date has not changed keeps its id, so re-importing is still a no-op.

`seed` is the list's name by default and is **editable per list in the UI as its revision**.

That gives three behaviours worth stating:

- **Re-importing the same file is a no-op**, because the merge skips ids it already holds.
- **A file that failed to import can simply be fixed and imported again** — nothing was written.
- **A correction needs a new revision.** The merge never overwrites, and deleting a session in the app
  soft-deletes its exercises but **not its sets**, so a re-import with the same ids restores the session
  and leaves the set values as they were. The honest sequence is: delete the session in the app, raise the
  revision, import. The new copy is live and the deleted one stays hidden.

Content is deliberately not part of the id: hashing the values would re-key every row whenever a weight was
corrected, which is the opposite of what the merge needs.

## Validation

**Blocks the export.** A root that is not an object; neither `rows` nor `sessions`; a session with no date; an unreadable
date; a set with no `reps` or no `weight`; `reps` below 1; a negative weight; `rpe` outside 1–10; an
offset that is not whole minutes within ±840; an exercise with no sets; an unmatched exercise name with no mapping and no *create new* choice; a duplicate
id; a non-null `deletedAt`; a missing `finishedAt`; a reference that does not resolve; a mixed unit inside
one group; a `drop` or `cluster` role.

**Warns and exports anyway.** A weight between 1 and 999 grams on an exercise that is not bodyweight — the
kilogram-versus-gram mistake, and the single most valuable check here; a date in the future or before 1970;
a duration of zero or more than six hours; more than fifty reps or a weight above 500 kg; two sessions
overlapping in time; an exercise name that matches only a row the export holds as deleted — the tool
creates a new exercise under that name instead of resurrecting the deleted one, because a document never
brings back what the user deleted (the rule
[`ProgramRepository`](app/src/main/java/com/example/androidapp/domain/repository/ProgramRepository.kt)
states for a program document); a session with no work sets; more than eight warm-ups on one exercise; a
session with no sets at all, which is left out of the file rather than refused (N162);
unknown keys in the input.

A problem is `{path, message}`, where `path` addresses the model rather than the page, so the test page can
assert it.

## The editor

One page, four panels:

1. **Source** — a drop zone for any number of files, a paste box, an optional *load your app export*, a CSV
   mode, and the prompt for the model with a copy button. The seeded library is already loaded from
   `seed-catalog.js`; an export adds the lifter's own exercises, the dry-run comparison and the id check.
   Below it, the **loaded lists**: one row each, with its row count, its own revision, a Remove, and a Combine line for a date two
   lists share.
2. **Sessions** — one collapsible row per session: date, exercise count, set count, work tonnage, warm-up
   count, a problem badge. Expanded, the header carries date, time, duration and the **UTC offset in
   minutes** — empty derives it from the browser, a number is the lifter's own — and each exercise block is a
   table of sets with editable reps, weight and unit, RPE and role. The exercise name is a text input backed
   by a `<datalist>` of the library's names, **sorted alphabetically**, so matching needs no widget library
   and typing filters. Inputs are created once and
   only the problem badges re-render, which is what keeps focus in the cell being typed.
   The **exercise mapping** above them is the same idea: its *Linked to* cell is a searchable field over the
   sorted library rather than a select, *Create new exercise* keeps the sheet's own name, and a typed name the
   library does not have is refused and the previous choice restored, so a typo cannot become a duplicate row.
3. **Problems** — counts and a list; selecting one scrolls to and focuses its row. A *show only problems*
   filter.
4. **Export** — *will add N sessions, M sets; K sessions already exist and will be skipped; J new
   exercises*; Download `workout-backup.json`, and a copy-to-clipboard fallback. Refused while a blocking
   problem exists, with the first three named.

Accessibility is part of the screen rather than a later pass: every input labelled, problems announced
through a status region, no colour-only signalling, full keyboard operation, and targets of at least 44 px.

The preview tonnage counts work sets and excludes warm-ups, matching `recordsEffort`, and warm-up volume is
reported beside it rather than folded in.

## Lift progress

One movement at a time, from the export already loaded, answering *how is my bench press going* on a screen
that is not phone-sized (N165). The movements that have history sit in a **list beside the chart**, with a
filter above it, so changing lift is one click rather than a search in a dropdown. Two series, both the app's
own definitions rather than a second opinion:

- **the heaviest working set** in each session, in kilograms — warm-ups excluded, and a set with no added
  weight is not a load;
- **Epley's estimate** from that set, `weight × (1 + reps/30)`, rounded to 0.5 kg because it is an estimate,
  returning the weight itself for a single rep and **nothing above 12 reps**, where the formula would be
  extrapolation dressed as arithmetic.

**The rows are the app's.** A soft-deleted session, exercise or set, and a session that was never finished, are
carried by an export but absent from the app's trend query; the page filters them the same way
(`deletedAt` on each, `finishedAt` on the session), so a deleted typo cannot become the chart's best session
and a lift whose sets were all deleted is not offered (B179). The picker's counts and *In your export* use the
same rule, which is why a removed exercise no longer shows its sets under a live session.

**Movements only.** A family head aggregates its children, which is the app's roll-up rule; offering one here
would re-implement it. **Kilograms only**, because the app stores grams and this is a progression view rather
than a unit converter. A range narrows to the last year or 90 days measured back from the newest session, so
an export from years ago still reads. **Nothing is stored**: the chart is built from the file in the tab and
dies with it.

Below the chart, **In your export** reads the loaded file back: every live session, newest first, with its
exercises and sets in kilograms, and the rows the app holds as deleted counted rather than shown. It is
**read-only**, and deliberately not the Sessions panel: that one is the import draft, built on a flat row list
which cannot carry readiness, template, superset, assistance or the ratings a backup does — a round trip through
it would silently drop them, and the app's merge would then refuse to overwrite the rows anyway.

The numbers are pinned to the app's rather than trusted to agree: `ProgressSeriesFixtureTest` runs the app's
own `toExerciseTrendPoints()` over a synthetic four-session fixture — a warm-up that must not win, a set that
rounds, one past the ceiling, and a single — and writes the whole input and expected series to
`fixtures/progress-series.js`. The tool's test page runs its series over the same rows and asserts the same
points.

## Program sheet

A table dropped into the page becomes a **program document** (`formatVersion` 4) the app imports from
Programs → Load program (N167). Columns: `Program` (optional), `Day`, `Weekday` (optional), `Exercise`,
`Sets`, `Reps`, `Load`, `Rest`, `RPE` (optional), `Note` (optional). A tab-separated paste from a spreadsheet
is the intended input.

- **One row is one planned exercise**: `Sets` of the same rep target, at one load, with an optional rest and
  RPE. `Reps` is `5` or a range like `6-8`.
- **Days become templates, weekdays become slots.** A day named on two rows with different weekdays is
  scheduled on both; a day with no weekday is a workout the lifter starts by hand.
- **Names resolve against the lifter's library, then the seeded library.** A name in neither is created as a
  movement, carried in the document with no taxonomy and listed in the preview.
- **Loads are kilograms**, converted to whole grams.
- **Create, not edit.** A sheet-derived program's id is a slug of its name, and the app ends the merge when a
  live program already holds that id — so re-importing the sheet adds nothing, and a sheet changed after it
  was imported (a new day, a different load) changes nothing either. The app is where a program is edited
  (B180). A program the lifter deleted comes back when its sheet is loaded again, un-hidden but not
  re-followed.
- **A same name is not always the same program.** With an export loaded, the preview says which case applies:
  this program is already there (nothing to add), it is there as deleted (the load restores it), or a
  *different* program carries the name (the load adds a second one, because it never renames or overwrites).

The fixture `fixtures/program-document.json` is what the sheet writes for a two-day example, and
`ProgramDocumentFixtureTest` decodes it with the app's own codec and checks every cross-reference the import
would otherwise drop.

## Body data (Waistline)

Dropping a Waistline backup (`waistline_backup_*.json`) beside the rows adds **DAILY** measurements to the
same output file (N166). Waistline keeps a day's numbers in `diary[].stats`; this reads `weight`, `body fat`
and `muscle`, and writes weight in grams and the percentages in tenths of a percent.

- **The unit is the file's.** Waistline stores each number in whichever unit the lifter chose and its
  `settings` names it; the panel shows the detected unit and lets it be corrected.
- **A day becomes local noon**, so a timezone change cannot move a weigh-in to the day before.
- **The id is a hash of the day and the kind**, so a second import of the same file adds nothing and restores
  a row the app hid.
- **A day the app already holds wins** (B178). The app keeps one entry of each kind per day, so its own row
  is not joined by a second one for a day entered by hand; the preview counts those days separately, and
  deleting the device's entry and importing again is how the file's numbers are taken.
- **Tape sites and custom stats are not written.** The app's daily record holds weight, body fat and
  muscle; the panel names everything else it left behind, so nothing disappears silently.
- Nothing is stored, and the output is the same backup the tool always writes — with history, or with
  body data alone.

The app needs no new format for this: `measurements` is already part of its backup format and its importer
restores it — with the day rule above applied at that boundary, where the device's own timezone is the one
that answers it. `WaistlineImportFixtureTest` decodes the tool's output with the app's own codec and pins the
numbers.

## Workflows

**First import.** Export from the app once — it carries the whole library, even with no history — load it,
paste the rows, fix the problems, download, then Settings → Import.

**A file that failed.** Nothing was written. Fix it and import again; the ids are unchanged, so the second
attempt is the first one that takes.

**A value already imported.** The merge never overwrites; see *Identifiers and revisions* for the sequence
that does work, which the UI states beside the list's revision.

## Privacy

- No request of any kind. The CSP is `default-src 'none'; script-src 'self' 'unsafe-inline' file:;
  style-src 'self' 'unsafe-inline' file:; img-src data:; connect-src 'none'`. The bare `file:` in the two
  source lists is what lets the page load its own script and stylesheet when opened from disk, where a
  browser may treat the document's origin as opaque and refuse `'self'`; over HTTPS it matches nothing.
- A source scan asserts that `http://`, `https://`, `fetch(`, `XMLHttpRequest`, `WebSocket` and
  `sendBeacon` appear nowhere outside comments.
- Health data never persists. The exercise mapping may, in `localStorage`, because a name matched to a
  library slug is configuration; it has a button that forgets it.
- If a hosted model is used, the spreadsheet left the machine **before** this tool saw it. The page says so
  where the prompt is offered, because the tool's own guarantee does not cover that step.

## Testing, without a dependency

A `tests.html` page runs assertions in the browser and prints pass or fail: unit and date conversion, the
offset sign, id determinism, every blocking and warning rule, the grouping algorithm against the example
above, the several-lists boundary and its combine, and a round trip through encode and decode. No framework,
because a test harness would be the largest dependency in the project.

Acceptance, by hand:

1. The example at the top of *Input* yields one session, two exercise blocks and five sets, three of them
   warm-ups.
2. A date appearing twice starts two sessions on that day; a blank exercise after a date change is a row
   problem rather than a carry-over.
3. Toggling the date order reinterprets every ambiguous date and updates the sessions it belongs to.
4. 225 lb on a pound-mapped exercise stores `102058` grams and displays back as 225.0 lb.
5. A warm-up imports as `WARMUP` and appears in no personal record.
6. A clean file imports into an empty install and History shows the expected dates, exercises and weights.
7. Importing the same file twice adds nothing the second time.
8. The same inputs in the same order produce the same rows, apart from `exportedAt`.
9. Two lists loaded together keep separate sessions, and two lists whose first session is at row 1 do not
   share an id; a date both trained is offered as Combine and joins into one workout.
10. A date-only row and an exercise heading add no set; their notes join the session's.
11. A stated UTC offset survives a date or time edit; clearing it goes back to the browser's zone.
12. An epoch-millisecond date is read as that day.

## What is deliberately left for later

- **Repairing an already-exported backup** as editable sessions, rather than only the row list.
- **A program and template editor**, which would be a second mode over the same shell.

## Assumptions the tool makes

Stated so a wrong one is visible rather than silent:

1. The exercise name is written once per block and blank below, or repeated on every row. The grouping
   rules handle both, but the second lets the tool warn when one exercise's rows interleave.
2. Warm-ups are marked per row, by a column rather than a separate block.
3. No sheet carries a time column; if one does, it is used and `12:00` is only the fallback.
4. A blank type means a work set. If a sheet's warm-ups are unmarked, the editor shows the count as zero
   rather than guessing which sets they were.
