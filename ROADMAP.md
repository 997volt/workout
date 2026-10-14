# Workout — Roadmap

> **v2.4** is shipped ([CHANGELOG.md](CHANGELOG.md)), and *Next* holds nine items — four the release rounds
> turned up (B128, B129, N128, N129), two candidates to choose between (N130, N131) and three about
> measurements (B130, N132, N133) — with four decided changes waiting in *Later* (N134–N137). Last reviewed
> against the code: 2026-10-14 — the N87–N95 batch shipped as 1.17; **N96–N102 with B95 shipped as 1.18**, with the six
> defects a line-by-line reading of that batch found (B96–B101); **N103–N106, N108, N109 and B102 shipped
> as 2.0**: the taxonomy becoming nullable, the store break that let it happen without a migration — a new
> database file, the 1.x chain and its schemas deleted, both transfer formats refusing 1.x, and the five
> retired enum names (four patterns and `BACK`) gone with the files that could carry them — the rest of the
> legacy the new store cannot hold, the decisions a later id reversed that were still reading as binding, the
> two columns and one transfer field nothing read while schema 1 was still unshipped, and the measurements
> screen's silent write failure; and **P1.1's library set, B103 and N110 shipped as 2.1**: the session and
> template reads no longer throw on an exercise that states no muscle, the library became a category over
> movements with a movement reading its family's pattern and muscles, 61 movements joined the 45, and the
> picker groups exactly as the library is; and **N107 with the N111–N121 batch shipped as 2.2**: the body's
> record split in two with soreness moved to the day and read back as a statistic, a set's number and the
> plan's remainder counting only the sets that stand on their own, the planned-set dialog's ± steppers and its
> one-line effort, rest and accept, today's workout at the foot of home beside the row that records the day,
> the library's long-press family details, a program read for the work it spreads where, and the three menu
> entries that reach what a row names — with the eight defects a reading of that batch found (B109–B116) fixed
> before the tag; and **N122–N124 with B117–B126 shipped as 2.3**: a movement's secondary muscles take half a
> set each, the seed gained the behind-the-back cable curl, where the next workout is became one surface in
> home's body, and the four defects a lifter found in use (B117–B120) with the six a review of the batch found
> (B121–B126) are fixed. **B127 shipped as 2.4** the same day, because the keyboard's black band was found in
> use on the released 2.3 and the window's soft input mode was the cause. The 1.x store is left on the device
> by design, and the 1.18 APK is still the way to read it.
> Checked against the changelog as well as the code: every shipped id has its entry, *Next* holds nine items
> (B128, B129, N128, N129, N130, N131, B130, N132, N133) and *Later* holds four (N134–N137). **Reading the N122–N124 batch line by line found five defects (B121–B125), and the race B125
> recorded rather than fixed became the sixth (B126); all six are fixed and recorded in
> [CHANGELOG.md](CHANGELOG.md).** **Reading the N107 and N111–N121 batch line by line — and running
> it on a device — found eight
> defects (B109–B116); all eight are fixed and recorded in [CHANGELOG.md](CHANGELOG.md).**
> Reading the P1.1, B103 and N110 batch line by line — and running it on a device — found five defects
> (B104–B108); all five are fixed and recorded in [CHANGELOG.md](CHANGELOG.md). A line-by-line reading of the
> N87–N95 changes found sixteen defects (B79–B94); all are fixed and recorded in
> [CHANGELOG.md](CHANGELOG.md).
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
shipped, with its entry in the changelog: [CHANGELOG.md](CHANGELOG.md) for the current line,
[CHANGELOG-1.x.md](CHANGELOG-1.x.md) for the 1.x releases.

## Next

Nine items in three groups. The first is what cutting 2.3 and 2.4 turned up: a release procedure that invites
its own mistake, the verification it leaves to a human, a race left in a comment, and the cheaper answer to
the formatting decision (B128, B129, N128, N129). The second is two candidates of one shape — a dimension the
app already records and reduces before reading it back — put here as **alternatives, one to be taken rather
than both** (N130, N131). The third is measurements: a defect the request turned up, and the two changes that
make a deletion deliberate (B130, N132, N133). Home's plan surface (N125–N127) left this section for the
changelog, and the decided-but-not-started work (N134–N137) sits in *Later*.

### What cutting 2.3 and 2.4 turned up (B128, B129, N128, N129)

- **The release procedure invites its own mistake** (B128). [RELEASING.md](RELEASING.md) step 3 is
  `git add -A && git commit -m "release: …"`, and `-A` takes whatever is in the tree. The 2.4 round proved
  it: a scratch directory of HTTP header and JSON dumps from checking the API went into the release commit,
  and only reading the commit's own `--stat` caught it before the push. **Name the three files** —
  `CHANGELOG.md ROADMAP.md version.properties` — which is also the complete set a release is allowed to
  touch, so the command becomes a check as well as a command. A release that has to carry a fix stages that
  file by name too; the invariant is that the release commit's content is *named* rather than swept.

- **A known race is filed in a comment and nowhere else** (B129). B126 put logging under a mutex so two
  sets cannot both compare against a session that has recorded neither. An **edit or a delete landing beside
  a log** can still move the picture that log compared against, because only logging is ordered. It is low
  severity and not the same defect: neither an edit nor a delete announces anything, so the end of it is a
  comparison that was briefly stale rather than a false banner. It is queued because this repository's rule
  is that a known gap gets an id rather than living in a KDoc — which is the only place it is today. The
  decision to make when it is taken up is *what* to order: the set-writing paths together, or the comparison
  against a snapshot the write path owns. What must not travel with the fix: the logging order itself, and
  the reason this is not B126.

- **The release verification is done by hand, twice now** (N128). Step 5's checks are
  `aapt2 dump badging` for the version, `apksigner verify --print-certs` against the **previous release's**
  certificate, `sha256sum`, and — after upload — re-downloading the published asset to confirm it is the
  file the notes describe. Every one is mechanical, and the certificate is the one where a slip means every
  user must uninstall and lose their history, because platform backup is off. A
  `tools/verify-release.py` beside [`ci-check-instrumented.py`](tools/ci-check-instrumented.py) would take
  the built APK and the previous tag and assert all four. It keeps the local half — badging, certificate,
  hash — runnable **without a token or network**, so it also works from CI and from a machine that has
  never seen the release before; the remote half (fetching the previous APK, the asset round-trip) is the
  part that needs credentials, and is the part worth making opt-in.

- **A diff-scoped formatting check answers B123 more cheaply than the reformat it rejected** (N129). B123
  settled that formatting stays a review rule, because loading `detekt-formatting` reports ~600 findings
  across 155 files and the alternatives were a repository-wide reformat or a set of `active: false` entries.
  Its **revisit trigger** is a layout defect that ships because a review missed it where a tool would have
  caught it. Running the ruleset over **only the files a change touches** meets that trigger without either
  rejected alternative: it would have caught the malformed `BalanceRow` line, which sits inside the
  120-column ceiling and is therefore silent to every rule wired today. What must not travel with it: the
  decision itself (no wholesale reformat, no rule switched off), and `MaxLineLength`, which stays the
  build's own ceiling. Rejected alternative: **new files only** — a rule that sees only additions misses the
  edit that breaks an old file, which is exactly how the malformed line arrived.

### Candidates, one to be taken rather than both (N130, N131)

Both are the same shape of question — a dimension the app already records, reduces, and then reads back —
and either is a real feature, so they are recorded as alternatives rather than as a batch. Neither should be
started to keep the section full.

- **The joint-pain trend averages away the joint and the side it is about** (N130). A rating records **which
  joint, which side, how much** —
  [`JointPain`](app/src/main/java/com/example/androidapp/domain/model/JointPain.kt) is a `Joint` with a
  `Side` and a score, and its own KDoc says "knee 6" is half a sentence — and what is read back is a single
  number: an average, as a workout trend and again as a per-exercise one (`TrendMetric.JOINT_PAIN`,
  `ExerciseTrendMetric.JOINT_PAIN`), plus one name-and-number line in the finish review that names the worst
  joint because it has room for one. So "left knee 6, right knee 2" charts as 4, and the question the
  recorded shape exists to answer — **which** joint keeps hurting, on which side, after which movements — is
  the one the average cannot. An earlier reading of this called it "no statistic at all"; that was **wrong**
  and is corrected here, because it nearly went into this file: the statistic exists, and the missing thing
  is the dimension. Shape deliberately left open — a series per joint, or a joint chosen beside the metric —
  since the recorded value already carries both halves the average drops.

- **The plan's target RPE never reaches the review** (N131).
  [`PlanComparison`](app/src/main/java/com/example/androidapp/domain/model/PlanComparison.kt) compares weight,
  assistance, reps and set counts; **effort is not in it**. The plan's `targetRpeHalves` is not dead — it
  prefills the field and drives the progression decision — and the logged RPE has trends of its own, so
  nothing here is unread either. What is missing is the *comparison*: "you prescribed RPE 8 and logged 9.5
  across the board" is invisible. That is **N119's question asked of the one column it did not cover**, and
  it is the last one. The difference to design around: the plan states **one effort per exercise** (N59)
  while the log records one per **set**, so unlike weight and reps this comparison has to choose what to line
  up against — the heaviest set's effort, the exercise's average, or the rating the lifter gave the exercise
  afterwards. What must not travel with it: the prefill and the progression use of the target, which work.

### Measurements: making a deletion deliberate (B130, N132, N133)

Asked for after using the app: a delete in the measurements view is one tap on a fact that cannot be
re-derived, and the list it sits in cannot even be scrolled. The defect came out of the request, and it has to
go first — an edit mode is no use for an entry that cannot be reached.

- **The measurements list does not scroll** (B130). `MeasurementList` is a
  [`Column`](app/src/main/java/com/example/androidapp/ui/measurements/MeasurementsScreen.kt) with
  `fillMaxSize()` that runs `forEach` over the entries — not a `LazyColumn`, not even `verticalScroll` — and
  the Scaffold hands it `innerPadding` and nothing else. So the first screenful is all there is: a lifter with
  a few weeks of daily entries cannot see, edit or delete anything older, and the add button floats over the
  last row that happens to fit. Every other list in the app scrolls (the library, history, the plan's own
  rows); this is the one that predates the pattern. Fixed by making it the list every other screen has, with
  the empty sentence as an item of it. Queued on its own rather than folded into N132, because it is a defect
  whatever is decided about deletes — and because N132's edit mode is unreachable for rows that cannot be
  scrolled to.

- **Deleting leaves the default view, behind an edit mode** (N132). Every row carries its own *Delete* today,
  so the destructive action is one tap away on a screen the lifter opened to **read or add** — and a
  measurement is the one thing here that cannot be reconstructed: a set can be logged again and a plan
  re-authored, but a weight on a morning is a fact about the body that no later write recovers. The default
  view becomes what it is for — **scroll and add** — and deletion moves behind an explicit edit toggle that
  the lifter enters and leaves, with the button saying which mode it is in. What must not travel with it: the
  add path with its two kinds (N113), the empty sentence, and the edit mode must not quietly become a way to
  *change* a reading — correcting a measurement's numbers is a different question, and this id does not ask
  it. Rejected alternative: a confirmation dialog on the existing per-row button. That is the guard this repo
  uses where an act is big and has no way back at all (deleting a workout, a template, an exercise), and it
  answers the wrong problem here — the complaint is a delete button on **every row of a reading screen**, and
  a dialog leaves it exactly there while adding an interruption to every deliberate one.

- **A deleted measurement can be put back** (N133). There is no way back today, and there is no need to
  invent one: the delete is already a **soft** delete — `RoomMeasurementRepository.delete` sets `deletedAt`
  on the entry and on its sore rows — so the way back is clearing that field rather than re-inserting
  anything, the same mechanism N90's restore uses. The shape is settled too, and it is **a snackbar naming
  what went, with *Undo*** (N7, B3, N90), held by the ViewModel until the message is taken and offered one at
  a time — not a modal "are you sure?" before a reversible act, which is the interruption that shape exists to
  avoid. The wrinkle to settle while building it: a measurement's identity is **its day and its kind** (N32,
  N113), so an undo must not resurrect a row that would collide with a newer entry for the same day and kind
  — the restored row keeps `deletedAt` as the identity of the one write that hid it (N90's rule), and a day
  that has been re-entered since has to be refused with a sentence rather than overwritten.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

- **A template can keep a version, and the versions are attached to it** (N134). Asked for in use: *sometimes I want
  to update a template but keep its previous version for future reference*. **A version is a template row of its own**,
  flagged by a nullable `versionOf` naming the template it was kept from, with its exercises and sets in the tables they
  already live in. Nothing new is created for the plan's tree: the read paths, the copy machinery
  `createTemplateFromSession` already uses, and the soft-delete convention all apply unchanged, and the templates list
  gains one more predicate of the kind it already has — `versionOf IS NULL` beside `deletedAt IS NULL` — so a version
  never appears as a template. **That dissolves the hide button** a *duplicate* would have needed: the kept copies live
  inside the template they belong to rather than loose in the list, so there is nothing to hide and no count to hold
  down. The four decisions are settled, not open:

  - **Never automatic.** The lifter presses *Keep a version* at the moment they decide to change something, and what is
    kept is the state as it stands. Automatic versioning was rejected: a rename or a note tweak would become a
    "version", and it writes rows nobody asked for.
  - **A version is never edited.** It opens as a **read-only plan**, the rendering the planned-workout preview already
    uses, so it cannot be changed into something that looks live.
  - **No restore.** The request is future *reference*, and reading it back is the whole of it. Restoring was rejected
    rather than deferred: it turns a record into an editing path, and a restore is destructive in its own right — the
    state it overwrites would then need keeping, which is the problem this feature exists to solve, one level down.
  - **The export carries everything**, versions and the `versionOf` link with them, so an import re-attaches versions
    instead of flattening them into ordinary templates. An export that dropped them was rejected: the file is the only
    path off the device, and a silent loss found after a restore is the worst form of this feature.

  **A version stores its own name, and that is what makes this safe.** The live template's name is read **live** by
  history — a rename relabels the past (P3.3's provenance rule) — so anything that renamed the live row to mark a
  version would rewrite how past sessions read. Keeping a version renames nothing and repoints nothing, which is also
  why it needs no part of the slot-repointing a *duplicate* would have required.

  **The shape was revised while deciding it**, and the first sketch is kept as the rejected alternative: three revision
  tables and a migration, on the reading that a version is a different kind of thing from a template. It is not — it is
  a plan with exercises and sets, and the flag is the only thing missing. One nullable column against three tables and a
  second copy of the tree is the whole of the difference.

  **Brought forward when** a template is next edited with the thought "I wish I still had the old one" — which is the
  moment the feature pays for itself, and is why it sits here rather than in *Next*.

- **A weight goal sets the rate, instead of the rate being typed** (N135). Asked for in use: the lifter sets a goal —
  a **target weight**, a **direction** with no target, or **maintenance** — and the app derives the **rate** from the
  goal and the current weight. Half of it is already here, which is what makes this small: a per-metric **level goal**
  exists and exports (`goals: Map<String, Double>`, `SettingsRepository.setGoal`), and a **rate target** is its sibling
  that the energy adjustment is measured against
  ([`EnergyAdjustmentSection`](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustmentSection.kt), "it
  is the rate that the adjustment below is measured against", N98). So the lifter types two numbers that can disagree —
  a target weight and an unrelated rate — and the missing thing is the **kind** of goal and the derivation.

  The rule follows [health-center](https://github.com/997volt/health-center), read rather than assumed: it fits weight
  over the **last 10 days**, prunes iteratively any reading more than **1 kg** off the fit, and judges the slope against
  **0.025–0.05 kg/day** — "around 0.25% weekly" — calling anything above too fast and below too slow. What transfers is
  the **shape**: the rate is a share of bodyweight per week, judged from a fit rather than from a pair of readings.
  The reference is the citation for the numbers, not a spec copied by value, because its own inputs are not all here
  (below).

  - **Maintenance is a corridor, not a rate of zero.** "Hold within ±X of the goal weight" is what makes it a goal; a
    literal 0.0 kg/week is broken by the first glass of water.
  - **A direction-only goal invents no target.** The band decides the rate; the app never manufactures an end weight
    the lifter did not state.
  - **The rate is clamped to the band, and never derived from a deadline.** If a date is wanted, the band's rate gives
    the *estimate* of the date — the other way round demands exactly the rates the band exists to refuse.
  - **The typed rate goes.** The statistics row shows the derived rate rather than offering it for editing. Rejected:
    keeping the two side by side, which is two sources that can disagree — the thing being fixed.
  - **The goal kind travels.** The goals map already exports, so this is a transfer-format move with it: a goal the
    lifter set is body data, and the export remains the only path off the device.

  **Brought forward when** the goal is next changed and the rate has to be re-derived by hand — which is the friction
  that says the two numbers should never have been separate.

- **A weigh-in says whether the rate is on target** (N136). The sentence is already written: [`EnergyAdjustment`](app/src/main/java/com/example/androidapp/ui/statistics/EnergyAdjustment.kt)
  says how far current intake has to move for the trend to become the target — "about 200 kcal a day less than you are
  eating now" — but it lives **under a chart in Statistics**, so the lifter has to go and look for it. The request is to
  say it where the weighing happens: entering a daily weight answers whether the trend's rate is the goal's rate, and
  when it is not, the adjustment is what it prompts with.

  **The boundary that shapes it, stated rather than crossed.** This app stores **no food and no intake** — a non-goal,
  not an omission: "the app states the energy implication of a weight trend it already owns (N98) and stores no food and
  no intake." health-center's verdict compares the rate against **average calories from a diary**; that half cannot come
  across, so the prompt is **relative** — how far intake must move — and never "you ate X". Importing the calorie half
  was rejected because it needs the store this app refuses to keep.

  **And it must not fire on noise.** The reference judges on 10 days; this app refuses to speak before N98's floor —
  **8 recorded readings across 21 days** — on the ground that "a rate from three weigh-ins is noise". The floor is kept:
  a prompt that fires on water weight is worse than no prompt, and it is the standard P2.8 is parked on. The reference's
  shorter window is the one number to decide deliberately when this is built, not to adopt by copying.

  **Never a direction the lifter did not set.** The reference's "bodyfat > 20% → you should be cutting" is rejected: the
  goal is the lifter's and the app does arithmetic against it rather than diagnosing. A prompt telling somebody with no
  such goal to lose weight is the one prompt this app must never grow.

  **A line after the save, not a dialog.** It states; it is dismissible; it does not pursue (P4.7). And it is not P1.11
  — that parked row is about personalising defaults for a new user, while this is a target a lifter sets about their own
  body.

  **Brought forward when** N135 is built, since it is that goal's other half — or when the lifter next wants to know
  *this week* whether the rate is right and has to open Statistics to find out.

- **A goal's past is kept, and each period says whether it was met** (N137). Asked for in use: when a goal changes,
  the old one should be recoverable, so a later reading can say **what the goal was in a given period and whether it
  was achieved**. Today there is one value per metric and no past at all — goals are a newline-joined
  `metricId=value` map in a preferences string (`PreferencesSettingsRepository`, `KEY_GOALS`), **rewritten on every
  change**. So changing a goal silently **re-judges the whole past**: the chart's goal line is the current target drawn
  across a range it never applied to, and "did I hit the cut I set in March" has no answer, because March's goal is
  gone. The measurements are dated; the goal is the only half of the question missing.

  **Goals become rows** — one per metric per period, with the periods running from a row to the next and the last one
  open. A prefs string cannot hold this: the history is unbounded and every period needs a date range to answer
  anything, and the import path already says so out loud — `restoreMissingGoals` runs "outside the database transaction
  … because goals live outside the database", which is the seam this removes. Rejected: appending to the string, which
  keeps an unqueryable, ever-growing blob and keeps that seam.

  - **The migration cannot know when today's goal was set**, so the first row records its start as **unknown** rather
    than inventing the moment of the upgrade. Inventing that date would be inventing history, which is the thing this
    feature exists to stop.
  - **The derived rate is not stored.** N135 derives it from the goal and the weight series, so the goal is the input
    and arithmetic is not a fact. The trade is named rather than hidden: if the band rule ever changes, an old period
    re-derives under the new rule — the honest reading is "by today's rule, March was X". Storing each period's verdict
    instead was rejected as caching something fully derivable from rows the app already has.
  - **Achievement is a reader over the same rows**: clip the weight series to the period and compare it with the goal —
    a rate goal is met when the trend sat in its band, a target weight when it was reached within tolerance, maintenance
    when the weight stayed in the corridor. So the answer is only well defined once **N135's goal kinds** exist, and
    this is that trio's third part.
  - **The chart's goal line becomes segments**, one per period. That is the visible payoff, and it is the thing that is
    simply wrong today.
  - **Only goals move into the database.** The other settings — unit, rest timer, step defaults — are preferences about
    how the app behaves, not facts about a body, so they stay where they are.
  - **The export carries the rows** (the standing preference that it carries everything), and the goals' special
    missing-only import rule goes with the prefs store, because rows merge like every other table.

  **Brought forward when** a goal is changed a second time and the chart's line stops being true for the earlier range —
  or when N135 lands, since a goal's kinds without its past is half a feature.

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
closed (B51-B58), the seven requests that were its last queue (N80-N86), the N87–N95 batch, the N96–N102
batch with B95 and the six defects a review of *it* found (B96-B101), 2.0 — the taxonomy and the store
break (N103, N104), the legacy and dead weight it left behind (N105, N106, N108, N109) and the measurements
screen's silent write failure (B102) — and 2.1 — P1.1's strength and bodybuilding set, the library as a
category over movements (N110), the unstated-taxonomy read (B103) and the five defects the reading of that
batch found (B104–B108), the warm-ups out of the set numbering and the plan's remainder (N112), the body's
record split in two with soreness moved to the day (N113), and the long entries deferring their argument to
the evidence (N107), the four that followed it — today's workout to the foot of home (N115), the set dialog's
one-line effort, rest and accept (N116), the library's long-press family details (N117) and home's row that
records the day (N114), a program slot's ⋮ opening its template (N120), a planned row's ⋮ opening the lift
(N121), soreness becoming a statistic (N118) and a program's muscle balance with the bar's overflow (N119) —
each with its entry in [CHANGELOG.md](CHANGELOG.md), the eight defects a reading of that batch found
(B109–B116) and the four a lifter found in use (B117–B120) among them — and N122, the half-credit a movement's
secondary muscles now take, N123, the behind-the-back cable curl the seed gained, and N124, the next-up
surface moving into the body, with the five defects a review of that batch found (B121–B125) fixed before the
tag, and **N125–N127 shipped next** — today's plan as home's only answer, the line that says when there is
none, and the day's own workout readable before it is started. B126 is not in that list because it never stood in *Next*: it is the logging order that closes the race
B125 had recorded rather than fixed, and it landed with the batch after a release-readiness reading.

***Next* holds nine items (B128, B129, N128, N129, N130, N131, B130, N132, N133) and *Later* four
(N134–N137); home's plan surface (N125–N127) shipped.** N118
was found by asking what the app now records and never reads back, and N119
asked the same question of the *plan* — what it prescribes was read only slot by slot, so a week's shape was
nowhere. Both shipped, along with the three menu entries that reached a thing a row named. The section is filled
from what the log and the plan hold and from what a reading or a use turns up, never to keep itself full. **The reading that produced 2.1 found five defects in it, all fixed before the tag (B104–B108)** — the one
that mattered left the instrumented job red, because a program document's former variation imports under its
family now and the test still asserted the link the shape no longer has. 2.0 shipped with the loudest note this project has written — the 1.x store stays on the device and
2.0 does not read it — and the rule that allowed that is in [DECISIONS.md](DECISIONS.md), with the window it
stays payable in.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
