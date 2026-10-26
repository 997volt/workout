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

**The review of everything since 2.15.** N216/N217, B239, N218–N220 and the two migrations are in; reading them
against the code turned up the rows below. Two shipped features cannot be switched on at all (B240), and the new
"load" and "hold" rules reach the workout screen and the history but not the finish summary, the plan comparison or
the plan a hold session saves (B241–B244). The CI gate is green — unit tests, the shared JVM and JS tests, lint,
detekt, the debug and R8 assemblies — so none of this is a build failure: it is arithmetic and wiring that the tests
did not cover (B256).

| # | What it changes |
| --- | --- |
| B240 | **Neither exercise flag is ever saved.** `RoomExerciseRepository.updateExercise` rebuilds the row with `stored.copy(...)` and omits `isBodyweight` and `isTimed`; `@Update` writes every column, so each save puts the stored `false` back. The editor's only save path is that call, `createCustomExercise` takes a name, and no seeded movement carries either flag — so nothing can be marked bodyweight or timed, the switch shows it saved, and the next read disagrees. Add both fields to the copy and a read-back test; N219 and N220 are dead until this lands. |
| B241 | **The finish summary counts added weight as the load.** `WorkoutReview.totalVolumeGrams` is `weightGrams * reps` while the history screen and the workout's own SQL count `bodyweight + added − assistance`, so an all-bodyweight session finishes on "0 kg" and reads 600 kg in history — and the comment beside it claims the two agree. One line: `totalLoadGrams * reps`. |
| B242 | **The prefill invents 20 kg on a bodyweight or timed movement.** `SetSuggestion` falls back to `Weight.DEFAULT_GRAMS` with no plan and no history, and the field now means *added*: a first pull-up logs 95 kg for a 75 kg lifter, a plank opens loaded, and a set logged before the flag existed is re-read as added weight. Thread the flags into the suggestion and open at zero when there is no source. |
| B243 | **A hold is reported as zero reps.** The plan comparison sums reps, so the review reads `3×0 at 20 kg`, the totals read "reps 0", and progression calls a hold "24 reps short of the plan". Carry the hold into the comparison and skip the rep comparison for it — DECISIONS says a timed movement ignores its rep target, and this is the code that has to. |
| B244 | **Saving a hold session as a plan writes 0-rep targets.** `createTemplateFromSession` copies `set.reps` (0) into all three target columns, producing a plan the app's own validator refuses and a program document that cannot be imported. A hold contributes no rep target: copy the range only for a counted set. |
| B245 | **A bodyweight set's load is invisible where it is read.** The rows print the added weight, so a pull-up reads "0 kg × 8" beside a volume that counted 95×8, and the weight field is labelled "Weight" where the value is what was *added* — the one sentence that explains it lives in the library. Show the load or a "BW+" marker on the row, and name the field for what it holds. |
| B246 | **A bodyweight set can claim a record at its added weight.** The PR check reads `weightGrams` and `PersonalRecords.from` filters `> 0`, so a 20 kg belt is a "record: 20 kg" — against the rule the model states ("a bodyweight set has no weight to compare") and the N219 decision that N23's rule was not reopened. Skip bodyweight sets in the check, or reopen the rule in DECISIONS. |
| B247 | **A program document drops both flags.** The shared exercise DTO has no `isBodyweight`/`isTimed` and the mapping drops them, so a document recreates pull-ups as ordinary movements — the loss N47's "the definition travels whole" exists to prevent. Add the fields to the shared model (a plain addition, no version bump) and map them both ways. |
| B248 | **Automatic backup can overwrite the copy it just made.** Names are second-resolution and an existing name is reopened with `openOutputStream` (truncating); `onStart` starts a run without cancelling the last, so app→home→app inside one second rewrites the same document, and a failed write leaves the partial file among the five kept. Take a mutex around check and write, never reopen a backup in `"w"` (a fresh name, or a temp document renamed), and delete a failed document. |
| B249 | **A failing backup re-exports the database on every foregrounding.** A failure records only the flag and due-ness reads the last success, so a deleted folder or a revoked grant stays due forever — each resume runs a full export and fails at the write. Record the last attempt and retry at most once a period, or probe the folder before exporting. |
| B250 | **"Nothing leaves the device" is not true of a cloud-backed folder.** The hint promises it while the folder is any SAF tree; a Drive or Nextcloud provider uploads the periodic copy with nobody there. Say the app has no network access, or warn when the provider is not local. |
| B251 | **A stale weigh-in is used silently, and a failed read looks like none.** The newest weight ever is taken for the snapshot, so a six-month-old number loads every set until the lifter measures again, and a failed read (through `getOrNull`) prompts and writes a measurement over an error. Prompt, or say which day's weight is being used, when it is old; keep failure and absence apart. |
| B252 | **The two flags are unnamed to a screen reader, and a hold cannot be typed.** A bare `Switch` beside a `Text` announces "Switch, off" without saying which flag, the hold field is labelled "m:ss" on a numeric keyboard with no colon, and the value is spoken as "0:45". Name the switches, let the colon be typed, and say the hold in words. |
| B253 | **The backup format's version was not raised for a hold.** An older build accepts the file (the unknown key is ignored) and imports a hold as a `reps = 0` set its own UI calls invalid — the case BackupCodec's rule says to bump for, weighed against making older builds refuse the whole file. Decide it explicitly and record the answer. |
| B254 | **Schemas 10 and 11 are missing from the database's own account.** `WorkoutDatabase`'s KDoc explains every version through 9; the two migrations that shipped belong there, with what each column means for rows already on disk. |
| B255 | **The review's small print.** Pruning matches `workout-backup-*.json` by name while the changelog says "only files this app named"; a backward clock step stops backups until the clock catches up, with no reset; `saveTodayWeight` scans every measurement on the logging path the new DAO read exists to avoid; `@Suppress("LongParameterList", "TooManyFunctions")` on the workout logger bypasses the detekt ceiling notes every earlier move recorded; `SetEntry.totalLoadGrams` has only a test caller; `DurationFormat.parseSeconds` folds `1:75` to 2:15; the migration tests describe a shape they do not create; and the automatic backup's file is never asserted to hold the export. Each is a line or a note, gathered so they are not rediscovered one at a time. |
| B256 | **The new tests pin the plumbing, not the claims.** The volume "agreement" test recomputes the old formula in the test instead of calling the UI's `volumeGrams`; no test marks an exercise bodyweight or timed and reads it back; the trend, prefill and plan-comparison arithmetic is never exercised with a bodyweight or timed row; the hold boundary rule has no test for both-refused, zero and the day cap; and no test renders a hold as `0:45` rather than `0 × 45`. Pin the claims, and let B240's absence fail loudly. |

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
