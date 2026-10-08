# Workout — Roadmap

> **v1.17** is shipped. Last reviewed against the code: 2026-10-08 — the N87–N95 batch shipped as 1.17 and
> emptied *Next*; the two *Later* requests had graduated into it as N95–N97 and the *Later* section went with
> them, leaving N96 here. **N97 then came out of *Parked***, because the trigger it named has fired, and
> **fourteen parked ids became non-goals** — the platforms, services, sensors and shapes this app will not
> grow into — which is why that section is a table now. **N98–N103 and B95 were added from use**: body weight
> is kept in this app and what that lets the trend say; four smaller requests — the template editor's order, a
> history row's two numbers, an unrated exercise's prompt, and home's body in place of *Recent*; and, from
> reading the library's inheritance, one fact in one home — the taxonomy a child states rather than copies,
> and the equipment's own step that a variation was clearing. A line-by-line reading of those N87–N95 changes
> found sixteen defects; all sixteen are fixed and recorded in [CHANGELOG.md](CHANGELOG.md).
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
not land first. The third thing that job named, the number a head would sum to, is **queued as N97**, because
the trigger it named has fired — and reading that parked entry against the code widened its subject from a
category to any head. **N98 is a second, independent request**, from a decision about where body
weight lives rather than about the library: it touches the statistics screen, and it neither blocks nor is
blocked by N96. **N99–N102 are four smaller requests from the same round of use**, each confined to the screen
it names and independent of the others. **N103 and B95 come from reading the library's inheritance rather
than from a screen**: N103 removes the taxonomy a child copies by making it inherited, and B95 is the
equipment's own step that a variation clears alongside it.

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

- **N97 — a head reads as one number, whether it is a category or an exercise.** The grouping N95 landed is
  designed against this — "all bench press volume" — and the request that produced it wanted the aggregate, so
  it was parked rather than dropped: the shape had to be used before it could say which views should read the
  head. **The trigger it named has fired**, and reading the parked entry against the code widened its subject
  and found three things it had left open.
  **A head is any row that holds others, not only a category.** A category holds its movements and their
  variations; an exercise holds its own variations; a variation holds nothing and is no head. It is the same
  subtree either way, so one rule serves both — and the intermediate one is what fragments today, where three
  rows of one barbell bench (competition, speed day, paused) split a lift a lifter thinks of as one.
  **The subject is the subtree's ids**, which are distinct, so the union cannot double-count, and it
  **includes the head's own sets** where it has any: an exercise with variations can still be logged directly,
  while a category never can.
  **A head's series stays derived, never stored** — the child sets read together — so a re-filed or renamed
  child cannot leave a stale total. **Records and progression stay the exercise's**, because a PR belongs to
  the lift that was actually performed.
  **Which metrics take a head is decided per metric, not blanket**
  ([ExerciseTrendMetric](app/src/main/java/com/example/androidapp/domain/model/ExerciseTrendPoint.kt), line
  39). Volume, total reps, RPE, muscle feel and joint pain roll up: each is a sum or an average that means the
  same thing over a family. **Heaviest set and estimated 1RM do not** — a speed-day single at 60% or a paused
  triple merged into a competition bench's line reads as a decline that never happened, and it is the number a
  lifter is most likely to misread. Assistance rolls up only where every row under the head carries it, because
  it is a magnitude on assisted work, and a family mixing assisted and free rows has nothing to sum.
  **What the work touches.** The lift list offers movements only — `library.second.filter { it.rowKind.isLoggable }`
  ([StatisticsViewModel.kt](app/src/main/java/com/example/androidapp/ui/statistics/StatisticsViewModel.kt),
  line 161) — and must also offer heads. That is a **read-view exception to N95's rule, not a repeal of it**: a
  head is still never offered while logging and never named by a set, so the pickers that log keep their
  filter and the statistics list is the one that changes. Because a head is a whole family, its row has to read
  as one rather than as another lift. The query is exact today — `WHERE se.exerciseId = :exerciseId`
  ([TrendsDao.kt](app/src/main/java/com/example/androidapp/data/local/TrendsDao.kt), lines 133 and 141) — so a
  head resolves to the subtree's ids rather than to one. The per-lift breakdown groups by the exact id a slot
  names ([ProgramSchedule.kt](app/src/main/java/com/example/androidapp/domain/model/ProgramSchedule.kt), line
  552), so it rolls up by parent for the same reason, a head's count being the slots naming any row beneath it.
  **The views it exists for** are the ones that fragment when one movement is spread across rows: the lift's
  own trend, muscle-group volume, "how much pressing am I doing", and the per-lift breakdown of P3.14.

- **N98 — the weight trend states an energy adjustment, and never a calorie target.** Body weight is recorded
  in this app, which makes one question answerable from what it already holds: *is the trend going where I
  meant, and how much should what I eat change?* **No food data is involved.** The inputs are the weight
  series ([BodyMeasurement.kt](app/src/main/java/com/example/androidapp/domain/model/BodyMeasurement.kt),
  N32), a target **rate**, and the fitted trend — which is already
  [TrendSlope.kt](app/src/main/java/com/example/androidapp/ui/statistics/TrendSlope.kt), per week rather than
  per reading for exactly this reason (N39). **The arithmetic is one line**: a kilogram of body mass is
  conventionally 7700 kcal, so the daily adjustment is `(targetPerWeek − observedPerWeek) × 7700 ÷ 7`. What
  the screen states is a **relative** figure — "about 200 kcal/day less than you are currently eating" — and
  never a number to eat. **The band is required, not decoration.** The 7700 coefficient is an approximation
  and weight moves on water, glycogen and sodium, so the statement carries the fit's own uncertainty — the
  scatter of the recorded readings around the line the chart already draws — rather than a point estimate; a
  single number there is the *plausible rather than true* that P2.8 is parked on. **It says nothing until
  there is enough to say.** Below a floor of readings over a window — a starting rule of **eight readings
  across at least twenty-one days**, to be checked against real data rather than assumed — the rows say *not
  enough yet* and draw no number, the shape N40's moving average takes when its window counts readings rather
  than days. **Nothing is written** (N22): the statement sets no setting, no plan and no target. **The
  absolute target is not this app's to give**: a number to eat needs intake, intake lives in whatever app
  records food, and its absence is a boundary rather than a gap to close with a formula — the lifter reads the
  relative adjustment here and sets the target there. **The app stores no food and no intake, and the
  non-goals below stand**: this states the energy implication of a weight series the app already owns — the
  family estimated 1RM and the trend slope belong to — and it does not become a diary.
  **What the work touches.** A target *rate* on the weight metric, beside the level target
  [GoalRow.kt](app/src/main/java/com/example/androidapp/ui/statistics/GoalRow.kt) already sets — a rate and
  not a target date, because the app keeps no forward view and no dated instances (N16). The derived statement
  itself, which belongs with the average and the trend line as a third thing *about* the series rather than a
  reading in it, plus the strings that say what the number is and is not. No new chart: the registry already
  draws the series
  ([MetricRegistry.kt](app/src/main/java/com/example/androidapp/ui/statistics/MetricRegistry.kt), N35) and the
  target line.
  **Weight history arrives once, through the import that already exists.** A one-off converter outside the app
  writes this app's own backup JSON from a foreign export, and the import is additive — it brings back what is
  gone and never overwrites what is there (P1.12) — so it cannot disturb the workouts already recorded.
  **No foreign format parser enters the app**: an importer for another app's export, or for a CSV, would be a
  permanent surface for a one-time need, and nothing would call it afterwards.
  **Rejected: reading the food app's export on an ongoing basis.** It would put a second weight series on the
  device (one fact, one home), park a parser for someone else's schema in this app, add a second health
  dataset to the one app that declares no permissions, and cross the nutrition non-goal — all to compute a
  figure the weight series already yields.

- **N99 — the template editor's exercise block reads in the order it is filled in: name, sets, add set, cue,
  rest, RPE.** Today the expanded block draws the sets, then the rest/RPE row with its save, then the cue, and
  *Add set* last ([TemplateEditorScreen.kt](app/src/main/java/com/example/androidapp/ui/templates/TemplateEditorScreen.kt),
  lines 482–493 for the block and 687–752 for the fields). **The change is the order and not the write**:
  *Add set* leaves the block's foot for the place directly after the sets — the control a new set grows from
  belongs beside the sets rather than past three fields — and the cue's full-width line (N80) moves above the
  numbers row, giving cue, rest, RPE. **The save stays on the numbers' row**, because one press still writes
  all three (N80) — but N80's stated reason for that position, that keeping it up there "leaves the block's
  foot to the *Add set* button", **is void once *Add set* moves up**, because the foot is then the rest/RPE
  row; the comment is rewritten to the reason that survives rather than inherited from a layout that no longer
  exists. The name is untouched: it stays the collapsed row the block folds into (N91).

- **N100 — a history row names the workout and stops there.** The supporting line is
  `templateName · duration · sets · volume`
  ([WorkoutHistoryScreen.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutHistoryScreen.kt),
  line 200). **It keeps the date, the name and the set count, and drops the duration and the volume**: a row in
  a month-grouped list is scanned for *which workout this was*, and two numbers answering a different question
  make every row wider than the one it is read for. **Nothing is lost** — the workout detail already draws both
  ([WorkoutDetailScreen.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailScreen.kt), lines
  536 and 542), which becomes the reason to open the row. The headline is unchanged — the weekday and date from
  `HistoryFormat.historyHeadline` (N57) — while this row's `duration`/`volume` locals and their weight-unit
  read go with the line; the `history_volume` string stays, because the detail still uses it.

- **N101 — an unrated exercise is silent in history rather than advertised.** The rating section draws its
  title and, where nothing was recorded, the prompt "Tap to rate"
  ([ExerciseRatingSection.kt](app/src/main/java/com/example/androidapp/ui/components/ExerciseRatingSection.kt),
  lines 72–81). **Where the section is read-only and nothing is rated it draws nothing at all** — no title and
  no prompt — because a title with nothing under it is a label for nothing. **The read-only case is already
  named**: `onRate == null` (N84). **Every editable caller keeps the prompt**, the active workout and the
  detail's *Edit* included. This does not touch N84: that rule says a rating which **exists** is readable
  before *Edit* is chosen, and its summary stays while the tap goes — it never required advertising the absence
  of one.

- **N102 — home's body is the ways in, and the start bar is what is next.** The *Recent* section leaves the
  Workouts home: `recentItems` and `RecentWorkoutRow`
  ([HomeRows.kt](app/src/main/java/com/example/androidapp/ui/home/HomeRows.kt), lines 195 and 212), the
  `TodayAndRecent` pairing, whose name goes with its recent half, and the `HomeContent` branches that exist
  only for it ([WorkoutsHomeScreen.kt](app/src/main/java/com/example/androidapp/ui/home/WorkoutsHomeScreen.kt),
  lines 396–415). **In its place, four rows in the shape the recent rows already carry** — an `AppRow` with an
  `IconTile`, no section header over them because they are the body rather than a category of content:
  **Programs**, **Templates**, **Measurements**, **Start empty workout**. **The start bar loses its Programs
  and Templates links and its empty-start pill** (settled by decision): with those three now rows, the bar
  keeps the next-up block and the **Resume** pill, which shows only while a workout is open. **The three
  destinations carry the disclosure chevron and the start does not** — a chevron promises a screen, and this
  row is the action the screen exists for, so it wears the empty start's own colour (N61) instead of reading
  as a fourth peer. Measurements gains this entry point and keeps the one from Statistics; the rule that
  recording and reading are different jobs is untouched, and the bottom **tab** bar is not this bar.
  **What goes with the section rather than being left behind**: `WorkoutsHomeUiState.recent` and the query
  behind it, which nothing else reads; `isFirstRun` with it, because the state can no longer answer "is this a
  first run" and does not need to — the body is now one list of today's plans when there are any, then the
  four rows — so `home_first_run`, `home_first_run_hint` and their tag go too; `onOpenWorkout`, threaded from
  the route to the recent row alone; the `home_recent` and `home_no_recent` strings and `HOME_RECENT_ROW` /
  `HOME_NO_RECENT`; and `HistoryFormat.historyHeadline`'s "shared by home's Recent row" sentence, because
  N100's list is then its only caller. The Compose tests asserting the removed tags and the no-recent state
  move to the four rows rather than being deleted with them
  ([WorkoutsHomeScreenTest.kt](app/src/test/java/com/example/androidapp/ui/home/WorkoutsHomeScreenTest.kt),
  [TestTagCoverageTest.kt](app/src/test/java/com/example/androidapp/TestTagCoverageTest.kt)).

- **N103 — a library row states what it knows and inherits the rest.** A child's taxonomy is a *copy* today: a
  variation is `parent.copy(...)` with a few fields cleared
  ([RoomExerciseRepository.kt](app/src/main/java/com/example/androidapp/data/RoomExerciseRepository.kt),
  line 89), and the seed restates a movement's muscle, equipment and pattern on every variation it ships —
  two homes for one fact, which is what the shape exists to prevent, and why
  [ExerciseRepository.kt](app/src/main/java/com/example/androidapp/domain/repository/ExerciseRepository.kt)
  line 69 must say "inherits everything the exercise is" while only two fields resolve live.
  **The model becomes one rule: a row stores what it states, and anything unstated is read from the nearest
  row above that states it.** **Neither spare value can mean "inherit" today**: `OTHER` already means *other
  equipment* (a sled) **and** is the placeholder a category head is seeded with, because a family spans
  equipment ([ExerciseSeeder.kt](app/src/main/java/com/example/androidapp/data/local/ExerciseSeeder.kt),
  lines 47–53); `null` on the four settings means the app default, no cue, follow-app and the unit's own. The
  creation path says the quiet part out loud — the taxonomy "is stored as unspecified rather than left null,
  **because the columns are non-nullable**"
  ([RoomExerciseRepository.kt](app/src/main/java/com/example/androidapp/data/RoomExerciseRepository.kt),
  lines 138–143). **So the three columns become nullable**, `null` meaning *inherit*, and `OTHER` returns to
  meaning only *other*; `secondaryMuscles` is unchanged, because an empty list already is the unstated case.
  **The winner is the same for every one of them: own-if-stated, else inherit.** Primary muscle reads the
  other way today — `effectivePrimaryMuscle` returns the nearest stating **ancestor** and treats the row's
  own value as a fallback
  ([ExerciseLibrary.kt](app/src/main/java/com/example/androidapp/domain/ExerciseLibrary.kt), line 257) — so a
  filed exercise's own muscle is written, exported and read by nothing while a head states one, and the edit
  form offers a picker that cannot visibly change anything
  ([ExerciseDetailScreen.kt](app/src/main/java/com/example/androidapp/ui/exercises/ExerciseDetailScreen.kt),
  lines 592–603). One rule ends that trap with no special-casing, makes the Data model rule in
  [DECISIONS.md](DECISIONS.md) — "inheritance is live, never copied" — true rather than a caveat, and takes the
  copy out of `createVariationOf` and the restatement out of the seed, whose `barbell(...)` helper stops
  demanding a taxonomy for a variation.
  **The migration clears a stored value only where the nearest stating ancestor already states the identical
  value — never blanket-null**, and that care is the whole of it: a movement's equipment is genuinely its own,
  since `barbell-bench-press` states `BARBELL` while the head above it states nothing, so blanking the column
  would stop the library drawing "Barbell" on every bench. It is scoped to **seeded** rows, which carry one
  batch timestamp and so are recognisable as seed rather than as a lifter's answer, so a value the lifter
  stated is never unpicked. The rule is display-preserving by construction: it removes exactly the redundant
  copies and changes nothing on screen.
  **What the work touches**: the three columns with their converters and mappers; the resolvers — the two that
  exist plus the equipment one this adds, with movement pattern's home moving to the category under N96, which
  takes that field off a variation and its duplication with it; the edit form; the seed; and **both transfer
  formats, in the same change, or an import silently loses what the copy used to carry**.
  **A row stops being self-contained, and that is the price.** A document carrying a movement must carry its
  ancestors, because the program document's promise — "the definition of every exercise the templates name"
  ([ProgramDocument.kt](app/src/main/java/com/example/androidapp/data/transfer/ProgramDocument.kt), lines
  37–45) — does not reach a variation's parent when a template names only the variation. The document
  therefore closes over the parent chain, and the backup's `schemaVersion` **must bump**, not for tidiness but
  because a newer file carries `null` where an older build expects a value — the case B62's gate exists for.
  **A file written before this stays safe**, for the reason that makes the entry work: it carries full copies,
  and a stated value still wins under the one rule both builds share.
  **Rejected: `OTHER` as the inherit sentinel** — it would save the nullable columns and cost the meaning of a
  real answer, since *other equipment* is something a lifter picks and a head's placeholder must say nothing
  rather than "other". **Rejected: blanket-nulling the migration** — cheaper to write, and it would erase a
  movement's own equipment, the one fact a family deliberately does not share.

- **B95 — a variation copies its parent's equipment and then clears the equipment's own step and unit.**
  `createVariationOf` copies the row and clears `restSeconds`, `techniqueNote`, `weightUnit` and `stepGrams`
  ([RoomExerciseRepository.kt](app/src/main/java/com/example/androidapp/data/RoomExerciseRepository.kt),
  lines 89–99). So a variation of a machine that jumps 5 kg arrives with the machine and the *unit's* step,
  and the ± buttons, the warm-up ramp and the progression offer all move a weight the machine does not have
  until the lifter retypes the number already typed on the parent; `weightUnit` is the same one step weaker on
  a machine that reads in pounds. Both are facts about the equipment that the same function has just copied,
  and `stepGrams` says so itself — "a fact about the equipment", "not presentation"
  ([Exercise.kt](app/src/main/java/com/example/androidapp/domain/model/Exercise.kt), lines 62–63). **The fix
  copies them with the equipment.** `name` and `techniqueNote` stay cleared, because they are what defines the
  variation, and `restSeconds` is left as the standing judgement: a paused variation is exactly the case that
  rests differently. Independent of N103 — that entry takes the taxonomy, these are settings — though the two
  edit the same field list in the same function.

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

### N39 — the plan's target, parked by decision

The per-metric target shipped; **the plan's target was scoped out** when the feature was
built, so it is parked rather than planned. It is a different shape of work: the training plan
is not one of the statistics screen's sources, so it means making the plan available to a
screen that knows nothing about it, plus a rule for a lift that is in two plans at once or in
none.

## Explicit non-goals

Permanent, unlike *Parked* above: a parked row becomes possible again the moment its trigger fires, while a
non-goal is a line this app does not cross. They are of three kinds — a product in its own right that would
dilute the logging core, a line the hard constraints already draw, and a shape this repository has decided
against — and each row says which, because a refusal whose reason is lost is the one the next person
overturns.

| # | Non-goal | Why it is permanent |
| --- | --- | --- |
| — | Nutrition / calorie tracking | A product in its own right. The app states the energy implication of a weight trend it already owns (N98) and stores no food and no intake. |
| P3.7, P5.2 | Social feeds, friends and shared routines | Accounts, servers and moderation would have to be owned, and a feed needs a public. |
| — | Live GPS route tracking | A different product: this one logs sets, not distance. |
| — | A web dashboard | Statistics is where the log becomes useful; sending health data to a second place to read your own trend is worse privacy and worse UX, not a division of labour. |
| P4.1 | Health Connect read/write | A sharing integration. This app stores data for its user, and the export file is the only path off the device. |
| P4.2 | Foreground service | The app declares no permissions and holds no background work; a rest timer that needs a service to survive is the service's product. |
| P4.3 | Home-screen widget | A second surface is a second product to keep true. The app is opened and used. |
| P4.4 | Quick Settings / launcher shortcuts | One entry point is the app; a shortcut is the launcher's job rather than a second way in. |
| P4.5 | Wear OS companion | It needs `play-services-wearable`, and the no-Google-Play-services line is permanent. |
| P4.6 | Bluetooth heart-rate straps | That is heart-rate training rather than logging, with a sensor between the lifter and the log. |
| P4.7 | WorkManager reminders | The app never pursues its user: no notification permission, and nothing that asks to be opened. |
| P4.8 | Large-screen layouts | One layout, deliberately — a second is a second surface to design, test and hold to the per-screen accessibility rule. |
| P4.9 | Offline-first sync | It needs a backend and accounts, and the no-`INTERNET` line forbids the first; the largest irreversible commitment this app could make. |
| P5.3 | Monetization / Play Billing | Play Billing is a Play-services dependency, which the hard constraints forbid. |
| F6 | Module split into `:core:*` / `:feature:*` | One module is the shape, and the trigger the row named — a second surface such as Wear or a widget — is ruled out above, so the named goal it was waiting to become cannot arrive. |
| F11b | Product analytics | It buys nothing on a single-user local tool and would breach the no-`INTERNET` line. |

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
