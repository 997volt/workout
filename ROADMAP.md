# Workout — Roadmap

> **v1.16** is shipped. Last reviewed against the code: 2026-10-08 — the N87–N94 batch shipped and emptied
> *Next*; the two *Later* requests graduated into it as N95–N97; the *Later* section went with them; and N95
> has since shipped, leaving N96 here and N97 parked. A line-by-line reading of the unreleased N87–N95
> changes then found the defects now queued under *Next* beside N96.
>
> Forward-looking only. What shipped is [CHANGELOG.md](CHANGELOG.md), how a release is cut is
> [RELEASING.md](RELEASING.md), and settled decisions with the rules that apply to every
> change are [DECISIONS.md](DECISIONS.md) — their argument, where there is one, is
> [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).

A local-only training notebook: write a plan, log what you actually did against it, and let
the app show you the difference and what to do next.

Ids (`F#` foundations, `B#` defects, `N#` the next planned changes, `P#.#` the product
backlog, `R#.#` releases) are stable and go in commit messages. They were assigned when the
work was planned, so they do not always run in order — and an id this file does not list has
shipped, with its entry in [CHANGELOG.md](CHANGELOG.md).

## Next

**Requests from use**, each with the decision it settles. N95 shipped — the library's shape, its own entry in
[CHANGELOG.md](CHANGELOG.md) — and what is left here is the pattern that moves onto it, which is why it could
not land first. The third thing that job named, the number a category would sum to, is **parked by decision**
rather than queued: see N97 below.

- **N96 — movement patterns get fewer, and move onto the category.** `MovementPattern` holds eleven values —
  HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL, SQUAT, HINGE, LUNGE, CARRY, ISOLATION, CORE,
  OTHER ([ExerciseTaxonomy.kt](app/src/main/java/com/example/androidapp/domain/model/ExerciseTaxonomy.kt)) —
  and the request is fewer of them. **The mapping:** `HORIZONTAL_PUSH` and `VERTICAL_PUSH` become **PRESS**;
  `HORIZONTAL_PULL` and `VERTICAL_PULL` become **PULL**; `SQUAT`, `HINGE`, `LUNGE`, `CARRY`, `ISOLATION`,
  `CORE` and `OTHER` keep their names — nine values where there were eleven. The four directional ones are
  merged because the split buys nothing a lifter asks a question with: "how much pressing" is not two
  questions because one of them was overhead, while the cost is a decision per exercise that nothing checks,
  so a dip is filed one way by one lifter and the other way by the next. **LUNGE stays out of SQUAT**, the
  one judgement inside the set: both are knee-dominant and differ by stance rather than by joint action, but
  "have I been squatting?" is a question a lifter asks and lunges are not the answer to it.
  **The pattern belongs to the category, not the exercise.** Whether a movement is a press or a hinge is what
  its family decides, so declaring it once per category is what stops a movement and its variations
  disagreeing — the same reasoning that has a variation inherit its muscles rather than restate them — and it
  removes a per-exercise field the lifter had to answer. **An exercise in no category carries no pattern**,
  settled that way rather than left open: one fact should have one home, and a loose custom movement is
  exactly the case where "press or pull" says little.
  **What the work touches.** The enum's values; the migration that rewrites the stored names, which is a
  migration over **names, never ordinals**; the exercise editor, where the field leaves, and the category
  editor, where it arrives; the seed's placement of every exercise it ships; and both transfer formats,
  whose versions are read before the body (B62). A retired name must still **read** rather than crash when an
  older row is loaded by a build that no longer offers it — N75's retired Back, which is not offered and
  still loads. **The per-exercise column is dropped in the same migration**, so the category's is the one home
  the rule "one fact, one home" leaves. **N95 has landed, so the dependency this entry named is met** — categories exist, the library draws them, and
  a lifter can make one. Two things N95 settled that this builds on: a head that says nothing is **silent rather
  than authoritative**, which is the rule that lets a category without a pattern leave its movements' own
  answers alone; and the per-exercise column can only go in a migration that rebuilds the table, because SQLite
  has no way to drop a column or relax a `NOT NULL` in place.

### Defects found in review

The unreleased N87–N95 batch was read line by line against what [CHANGELOG.md](CHANGELOG.md) says it does,
and the reading found these. Two make a control do nothing or give an upgrading library the wrong shape; the
rest are smaller, and the last few are rules the batch states but nothing holds.

- **The exercise picker's search does nothing** (B79). `ExercisePickerViewModel` passes the query to
  `libraryRows(exercises, currentQuery, grouped = false)`, and the flat branch of
  [ExerciseLibrary.kt](app/src/main/java/com/example/androidapp/domain/ExerciseLibrary.kt) never reads it:
  it returns every loggable row, sorted by name. v1.16 searched with `ExerciseSearch.filter`, which this
  refactor left with no caller. Typing "zzz" in the picker leaves the whole list on screen, and the picker's
  empty state hardcodes `libraryIsEmpty = false`, so an empty library reads as a failed search as well.
- **A real v1.16 upgrade leaves the three seeded bench variations unfiled** (B80). `MIGRATION_36_37`
  corrects the bench variations with `UPDATE … WHERE parentId = 'bench-press'`, but `MIGRATION_35_36` only
  adds the columns and backfills `rowKind` — every `parentId` is still NULL when that update runs, so it
  matches nothing, and the general loop then skips those three ids as already handled. A fresh install files
  them from `SeedExercises.parentOf`, so the two paths disagree: an upgraded library shows *Competition
  Bench Press*, *Bench Press — Speed Day* and *3-Second Paused Bench Press* as loose rows instead of under
  *Barbell Bench Press*. The instrumented test passes only because it hand-inserts a v36 row already
  pointing at the family head, a state no shipped build produces.
- **Searching a variation's own name finds nothing** (B81). In `libraryRows`, a variation is drawn only
  inside the loop over a category's matched children, and `looseRows` then drops any row whose parent is in
  the library. So a query that matches *Speed Day* but neither *Barbell Bench Press* nor *Bench Press*
  matches the row and then emits nothing: the family pass has no matched movement to hang it under, and the
  loose pass will not take it. Typing "speed" or "paused" in the library answers *No exercises match*,
  though the row is there and an empty query lists it — the opposite of what `matchesWithItsFamily`'s own
  doc promises ("filing never hides anything").
- **A variation is invisible whenever its exercise is not a drawn family child** (B82). `looseRows` draws an
  unfiled movement but not its variations, and `familyRows` draws variations only for category children, so
  a variation under an exercise in no category — a custom movement, or one of the six the seed leaves loose
  — never appears in the library while the flat picker still offers it. The same gap makes a *variation of a
  variation* invisible, and the screen offers exactly that: `canCreateVariation` asks only for
  `rowKind == MOVEMENT`, and `createVariationOf` refuses a category parent but not a variation, so the third
  level the shape forbids can be created and then cannot be found. "Two rules deep and no deeper" is stated
  and not enforced.
- **A new variation is stored already named after the exercise it hangs under** (B83). `createVariationOf`
  copies its parent and says in its comment that the name starts unset, but the copy keeps `name`, and the
  insert happens before the editor opens. Saving without typing leaves a second row with the parent's exact
  name; cancelling leaves it too, because a library row has no delete. The qualifier is meant to be "a name
  the lifter writes".
- **The detail screen keeps the old family after a save that moved the row** (B84). `loadCategoryOptions`
  runs once in `init`, and a successful save replaces only `exercise`; `head` and `headName` keep their
  pre-move values. Filing a loose movement under a category therefore returns to a page with no *Category*
  row and the old inherited muscle, until the screen is left and re-entered — the page contradicting the row
  it just wrote.
- **A variation's family field reads *Not in a category*** (B85). The edit form's `CategoryPicker` is
  offered categories only, while a variation's parent is a movement, so the row's own id is not among the
  options and the button falls back to the *none* label. Editing *3-Second Paused Bench Press* shows
  *Category: Barbell Bench Press* on the page and *Not in a category* in the form; accepting that value
  re-files the variation to the top level.
- **The effective-muscle resolvers look only one level up** (B86). `effectivePrimaryMuscle` reads the
  parent's stored field rather than the parent's effective value, so a muscle a category passes down reaches
  its movements but not their variations: with the category at Chest and a movement that stores `OTHER` and
  inherits it, the movement reads Chest and its new variation reads Other. `effectiveSecondaryMuscles` has
  the same one-level fallback, and a family disagreeing with itself is what the live inheritance exists to
  prevent.
- **The library's search stopped trimming its query** (B87). The trim lived in `ExerciseSearch.filter`,
  which is off this path now, so `matches` receives the raw text: a query of only spaces, which v1.16
  treated as blank and answered with the whole library, now matches nothing, and "squat " with the trailing
  space a half-typed second word leaves behind answers *No exercises match* where v1.16 matched.
- **A failed *New category* is drawn as a failed read and hides the library** (B88). `onCreateCategory`
  writes its failure into the same `error` field the read uses, and the body tests `error != null` before
  the list, so a write that did not land replaces the library with the read-failure page. The naming dialog
  is already closed when the write fails and the route passes no message, so nothing clears it until the
  dialog is opened and dismissed or a later create succeeds.
- **The first exercise added to an empty template starts folded** (B89). `rememberRowFold` seeds "what was
  already there" from the first non-empty id list, and its `remember` key is `ids.isEmpty()` — so when an
  empty editor gains its first exercise the key flips, the seed is rebuilt to include that id, and the row
  is judged already-there and folded. N91's *a newly added exercise opens expanded* holds only while the
  list was already non-empty.
- **The transfer formats can carry `RowKind.CATEGORY` without a version bump** (B90). Both codecs say to
  bump when a newer file could carry data an older build cannot represent — N75's added muscle names are the
  precedent — and a file written now carries `"rowKind": "CATEGORY"` and `parentId` that a v1.16 build has
  no field for. With the version still 2 and `ignoreUnknownKeys = true`, that build accepts the file and
  silently flattens every family instead of refusing it as newer.
- **The new question's discard has no in-flight guard** (B91). `activeWorkoutGate` clears its pending start
  only after the suspend discard returns, so the dialog and both answers stay live for the whole write: a
  second tap on *Discard and start new* (or *Continue workout* after it) reaches `onStart` again — a second
  logger entry, so Back appears dead — or, when both taps read the old session, the loser's delete reports
  `NotFound` and raises a failure snackbar over a start that did succeed. The repo's own pattern guards this
  one screen over in `ProgramStartGateViewModel`.
- **The library's shape is not enforced at the write or import boundary** (B92). `updateExercise` and the
  importers store any `parentId`, so a file with a cycle or a category under a category is accepted. A
  two-row cycle makes both rows vanish from the grouped library, each excluded from `looseRows` because its
  parent exists, and a category under a category is drawn twice, once as its own head and once as a child.
  The import path is the only way in today, which is why this is small.
- **A substitute's start is named as the scheduled plan** (B93). `startSubstitute` and
  `substituteOccurrence` carry `label = plan.name`, the row that was scheduled, while the start seeds the
  template that was picked. N89's new dialog is the surface that now shows the wrong name back to the
  lifter.
- **The library's list-item conversion and `ExerciseSearch.filter` are left with no caller** (B94).
  `ExerciseListItem`/`toListItem` and the filter function are still declared after the refactor that
  replaced both, which is the API the rule says to delete the moment nothing calls it.

## Parked — deliberately not planned

Each row is a product in its own right, contradicts "local-only", or both. Parking is a
decision, not a backlog, and every row names what would change it. Parked is **not** the same
as the non-goals below: these become possible again the moment their trigger fires, while a
non-goal is a line this app does not cross.

| # | Feature | Revisit only if |
| --- | --- | --- |
| P4.1 | Health Connect read/write | A user asks to share with a platform health graph. It is a sharing integration; this app stores data for its user. |
| P4.2 | Foreground service | The rest timer needs to survive something the alarm and the in-app timer cannot. |
| P4.3 | Home-screen widget | The glance it would give turns out to be the missing thing. |
| P4.4 | Quick Settings / launcher shortcuts | Starting a routine becomes frequent enough to deserve a second entry point. |
| P4.5 | Wear OS companion | Wrist logging is genuinely wanted — and you accept `play-services-wearable`, which breaks the no-GMS line. |
| P4.6 | Bluetooth heart-rate straps | The product becomes heart-rate training rather than logging. |
| P4.7 | WorkManager reminders | Nudges demonstrably improve adherence. |
| P4.8 | Large-screen layouts | Tablet or foldable users actually appear. |
| P4.9 | Offline-first sync | There is a real multi-device story. It needs a backend, accounts and conflict resolution — the largest irreversible commitment on this list. |
| P3.7, P5.2 | Friends, shared routines | Accounts, servers and moderation become worth owning. |
| P5.3 | Monetization / Play Billing | There is a concrete reason to charge, and a willingness to take the Play-services dependency. |
| P5.4 | Localization | A non-English user appears. |
| P1.11 | Onboarding: goal, experience level, weekly target | This stops being a single-user local tool with one obvious user. It personalises defaults, and there are no defaults to personalise. |
| P1.17 | Accessibility audit | The per-screen rule stops being enough — a real complaint on a device, or a screen that grew past ad-hoc tagging. The rule still applies to every change; only the sweep is parked. |
| P2.5 | Progress photos | A visual record is actually wanted, and an encrypted-storage design for it is acceptable. |
| P2.8 | Muscle-group balance warnings | Enough history exists for a rolling window to say something true rather than something plausible. |
| P2.6 | Plate calculator | Loading from a plan's target is frequent enough that the arithmetic gets in the way, and you would rather it were done for you. |
| — | **Play Store listing** | You want distribution beyond `adb install`. Self-install works today, and Play App Signing would change who holds the signing key. |
| — | **Encryption at rest / app lock** | You start carrying the phone somewhere you would not carry the data. |
| F6 | Module split into `:core:*` / `:feature:*` | **A named goal, not a refactor**: a measured build-time problem, working on one feature without compiling the rest, or a second surface (Wear, a widget). |
| F11b | Product analytics | Almost certainly never: on a single-user local tool it buys nothing, and it would breach the no-`INTERNET` line. |

### N97 — a category reads as one number, parked by decision

The grouping in N95 is designed against this — "all bench press volume" — and the request that produced it
explicitly wants the aggregate, so parking it is a **deliberate scope cut** rather than a lost idea: the
library shape lands first, and the statistic waits until the shape has been used enough to know which views
should read it.

What it would take is already named, so reviving it is small: the per-exercise trend series
([MetricRegistry.kt](app/src/main/java/com/example/androidapp/ui/statistics/MetricRegistry.kt)) gains a
category as a valid subject, and the per-lift adherence breakdown's domain function (`exerciseAdherence`)
rolls up by parent. **A category's series is derived, never stored** — the child sets read together — so a
re-filed or renamed child cannot leave a stale total; **records and progression stay the exercise's**, because
a PR belongs to the lift that was actually performed. The views that fragment when one movement is spread
across rows (muscle-group volume, "how much pressing am I doing", the per-lift breakdown of P3.14) are the
ones it exists for.

**Revisit when** a lift's own series stops answering "how is my bench going" — in practice, once a lifter has
filed more than one variation or equipment variant under one category and the per-lift view fragments because
of it. That is the signal the shape has been used enough to say which views should read the head.

### N39 — the plan's target, parked by decision

The per-metric target shipped; **the plan's target was scoped out** when the feature was
built, so it is parked rather than planned. It is a different shape of work: the training plan
is not one of the statistics screen's sources, so it means making the plan available to a
screen that knows nothing about it, plus a rule for a lift that is in two plans at once or in
none.

## Explicit non-goals

Permanent, unlike *Parked* above: nutrition / calorie tracking, social feeds, live GPS route
tracking, and a web dashboard. Each is a product in its own right and would dilute the logging
core.

## Keeping this true

Four rules, written after the drift they prevent had happened four times — stale test counts,
a dependency inventory, an enumerated feature list that v1.3 quietly outgrew, and a review
stamp still reading v1.3 while *Next* said "Nothing" after v1.4 had shipped with defects
unfound. Two of those four were this file describing itself wrongly.

1. **Nothing marked done lives here.** Shipped work goes to [CHANGELOG.md](CHANGELOG.md), and a
   finished row is deleted from this file.
2. **No hand-maintained facts.** No test counts, no dependency lists, no inventory of which
   files exist. Those are commands (`./gradlew …`) or links.
3. **Every parked row names its revisit trigger**, so parking reads as a decision rather than a
   forgotten item.
4. **Durable content lives in [DECISIONS.md](DECISIONS.md).** Settled decisions and the rules
   that apply to every change are a reference, not a queue, and the two age differently. A
   section here that accumulates rather than drains belongs there.

Everything that has stood in *Next* has shipped — the defects found in use, the workout screen's discard, the
workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch, a rest of
zero, the planned-set prefill, the program document, the eight defects a review of that batch found and
closed (B51-B58), the seven requests that were its last queue (N80-N86), and the N87–N95 batch — each with
its entry in [CHANGELOG.md](CHANGELOG.md).

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
