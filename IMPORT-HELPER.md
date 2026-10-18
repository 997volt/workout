# Import helper — specification

> **Status: a specification for work that is not built.** Nothing in this repository implements it yet,
> and no [ROADMAP.md](ROADMAP.md) id covers it — the first commit that starts it must say so rather than
> invent one, per [AGENTS.md](AGENTS.md). When the tool ships, its durable rules (the format conventions
> and the import semantics it relies on) move into [DECISIONS.md](DECISIONS.md) and this file shrinks to
> what a user needs to know.

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

In scope: history only — sessions, their exercises, their sets, and the definitions of the exercises they
reference.

Out of scope, each deliberately:

- **Programs and templates.** A separate tool may follow and share this shell; the app already has its own
  format for those ([`ProgramDocument`](app/src/main/java/com/example/androidapp/data/transfer/ProgramDocument.kt)).
- **`.xlsx` parsing.** Reading a zip container needs a dependency, which the constraints below forbid.
  Exporting the sheet as CSV is one menu item in every spreadsheet program.
- **A server, accounts, sync, telemetry, a service worker, or a build step.**
- **Editing the app's other collections.** The tool writes history and nothing else.

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
  index.html     markup and the CSP meta
  app.js         all logic
  style.css      presentation
  .nojekyll      stop Jekyll rewriting JavaScript that contains {{ }}
IMPORT-HELPER.md  this file, at the root, never served
```

Enabling Pages (branch `main`, folder `docs`) publishes the tool at
`https://997volt.github.io/workout/`. The three files also open from disk, which is the copy for anyone who
would rather not load it from a website. A single-file build is the same three files concatenated; it must
behave identically.

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
5. **Each row is appended as one set**, with `setIndex` counting from 1 **including warm-ups**, because that
   is how the app stores them; it numbers the work sets separately when it displays them.
6. **A blank type is `NORMAL`.** That is the only default, and a sheet with no warm-up marking therefore
   produces an inflated preview — which is why every session header shows its warm-up count.

Clearing the exercise on a date change is the rule that matters: without it, the last lift of Monday
inherits every set of Tuesday.

Row-level diagnostics, each naming the row rather than the file: *no date yet*, *no exercise yet*, *reps
missing*, *weight missing*, *unreadable date*, *unreadable number*, *type not recognised*.

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
id = "imp_" + fnv64_hex(seed | kind | sheet | row)
```

`kind` is `sess`, `ex` or `set`; the hash is two 32-bit lanes rendered as sixteen hex characters. Exercise
ids key on the **normalised name only**, so two files that both say *Cable Fly* produce one exercise.
Session ids key on the seed and their own source coordinates, and children derive from their parent and
index.

`seed` is the source name by default and is **editable in the UI as the revision**.

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
exercise with no sets; an unmatched exercise name with no mapping and no *create new* choice; a duplicate
id; a non-null `deletedAt`; a missing `finishedAt`; a reference that does not resolve; a mixed unit inside
one group; a `drop` or `cluster` role.

**Warns and exports anyway.** A weight between 1 and 999 grams on an exercise that is not bodyweight — the
kilogram-versus-gram mistake, and the single most valuable check here; a date in the future or before 1970;
a duration of zero or more than six hours; more than fifty reps or a weight above 500 kg; two sessions
overlapping in time; an exercise name that matches only a row the export holds as deleted — the tool
creates a new exercise under that name instead of resurrecting the deleted one, because a document never
brings back what the user deleted (the rule
[`ProgramRepository`](app/src/main/java/com/example/androidapp/domain/repository/ProgramRepository.kt)
states for a program document); a session with no work sets; more than eight warm-ups on one exercise;
unknown keys in the input.

A problem is `{path, message}`, where `path` addresses the model rather than the page, so the test page can
assert it.

## The editor

One page, four panels:

1. **Source** — a drop zone, a paste box, an optional *load your app export*, a CSV mode, and the prompt
   for the model with a copy button. Loading an export is optional but recommended: it supplies the exercise
   library, the dry-run comparison and the id-collision check.
2. **Sessions** — one collapsible row per session: date, exercise count, set count, work tonnage, warm-up
   count, a problem badge. Expanded, each exercise block is a table of sets with editable reps, weight and
   unit, RPE and role. The exercise name is a text input backed by a `<datalist>` built from the library, so
   matching needs no widget library. Inputs are created once and only the problem badges re-render, which is
   what keeps focus in the cell being typed.
3. **Problems** — counts and a list; selecting one scrolls to and focuses its row. A *show only problems*
   filter.
4. **Export** — *will add N sessions, M sets; K sessions already exist and will be skipped; J new
   exercises*; Download `workout-backup.json`, and a copy-to-clipboard fallback. Refused while a blocking
   problem exists, with the first three named.

Accessibility is part of the screen rather than a later pass: every input labelled, problems announced
through a status region, no colour-only signalling, full keyboard operation, and targets of at least 44 px.

The preview tonnage counts work sets and excludes warm-ups, matching `recordsEffort`, and warm-up volume is
reported beside it rather than folded in.

## Workflows

**First import.** Export from the app once — it carries the whole library, even with no history — load it,
paste the rows, fix the problems, download, then Settings → Import.

**A file that failed.** Nothing was written. Fix it and import again; the ids are unchanged, so the second
attempt is the first one that takes.

**A value already imported.** The merge never overwrites; see *Identifiers and revisions* for the sequence
that does work, which the UI states next to the revision field.

## Privacy

- No request of any kind. The CSP is `default-src 'none'; script-src 'self' 'unsafe-inline';
  style-src 'self' 'unsafe-inline'; img-src data:; connect-src 'none'`.
- A source scan asserts that `http://`, `https://`, `fetch(`, `XMLHttpRequest`, `WebSocket` and
  `sendBeacon` appear nowhere outside comments.
- Health data never persists. The exercise mapping may, in `localStorage`, because a name matched to a
  library slug is configuration; it has a button that forgets it.
- If a hosted model is used, the spreadsheet left the machine **before** this tool saw it. The page says so
  where the prompt is offered, because the tool's own guarantee does not cover that step.

## Testing, without a dependency

A `tests.html` page runs assertions in the browser and prints pass or fail: unit and date conversion, the
offset sign, id determinism, every blocking and warning rule, the grouping algorithm against the example
above, and a round trip through encode and decode. No framework, because a test harness would be the
largest dependency in the project.

Acceptance, by hand:

1. The example at the top of *Input* yields one session, two exercise blocks and five sets, two of them
   warm-ups.
2. A date appearing twice starts two sessions on that day; a blank exercise after a date change is a row
   problem rather than a carry-over.
3. Toggling the date order reinterprets every ambiguous date and updates the sessions it belongs to.
4. 225 lb on a pound-mapped exercise stores `102058` grams and displays back as 225.0 lb.
5. A warm-up imports as `WARMUP` and appears in no personal record.
6. A clean file imports into an empty install and History shows the expected dates, exercises and weights.
7. Importing the same file twice adds nothing the second time.
8. The same inputs in the same order produce the same rows, apart from `exportedAt`.

## What is deliberately left for later

- **A fixture the app's tests decode.** One synthetic `workout-backup.json` checked in and read by a JVM
  test is what makes a `schemaVersion` bump fail loudly instead of silently breaking the tool. It is the
  one app-side change this work wants, and it is not part of the first version.
- **Repairing an already-exported backup** as editable sessions, rather than only the row list.
- **A program and template editor**, which would be a second mode over the same shell.

## Assumptions to confirm before building

Stated so a wrong one is caught here rather than in a generated file:

1. The exercise name is written once per block and blank below, or repeated on every row. The grouping
   rules above handle both, but the second lets the tool warn when one exercise's rows interleave.
2. Warm-ups are marked per row, by a column rather than a separate block.
3. No sheet carries a time column; if one does, it is used and `12:00` is only the fallback.
4. A blank type means a work set. If a sheet's warm-ups are unmarked, the editor shows the count as zero
   rather than guessing which sets they were.
