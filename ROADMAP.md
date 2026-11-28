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

## Next — the live workout's feedback

Five changes to what the session screen says and does, asked for after training with it, in the order
asked. None adds a stored row or a backup field: a set's records still derive from the sets, and the
plan still moves exactly as N74 and N299 decide. The first two are the banners over the list, the next
two are lines an exercise block prints, and N377 is the gate behind the auto step and the earned-set
offer.

- **N373 — The auto progression banner says what moved, and how.** A batch of several steps collapses to
  *"3 targets adjusted"* and a lone step says only *"next time 82.5 kg"*; neither names the direction or
  the number each set left. The recorded step carries the whole target before and after
  (`AutoProgressionStep`), so the banner should state, per set, the direction — reps below the ceiling,
  the load at it, a hold's seconds — and the move itself: *"Set 2 · 100 → 102.5 kg"*, *"Set 3 · 8 → 9
  reps"*, *"Plank · 0:45 → 1:00"*. The step needs the offer's set place for the "Set 2" half, which it
  does not carry today. Keep the batch's Undo/Dismiss and the one-at-a-time queue; a batch that moved
  many sets may draw a compact list rather than a sentence.

- **N374 — A record banner is withheld when a stronger set already dominates it.** Records are one best per
  rep count, so 100 kg × 4 is a new best at four reps even when 100 kg × 5 stands, and the app announces
  it. A set is dominated when a record at **more reps** is at least as heavy; the banner then stays quiet,
  while the four-rep best is still stored — a record derives from the sets, not from the banner — and a
  same-rep, heavier set still announces. The check reads the merged view the banner already reads
  (history plus this session's earlier sets), so an earlier set here can dominate too.

- **N375 — The workout block stops printing the plan's progression sentence.** N227 put the plan editor's
  sentence under the exercise name. It is a plan-level fact restated on a logging screen, and it was asked
  for gone. Delete the line, its four strings and its tag from the workout block, with the screen tests
  that assert them; keep the plan editor's sentence and `progressionRuleFor`, which the auto preflight and
  the plan stall still read. Reverses N227's workout half only — the plan screen still states the rule.

- **N376 — The prefill's reason line names one source, not two.** It states both halves today — *"Last time
  8 × 60 kg, RPE 8 · Plan 5–8 reps @ 62.5 kg, RPE 8"* — so the lifter parses two origins for numbers that
  came from one. Where the planned set states a target, the line states the plan alone; where it states
  none (no plan, a planned set that names no number, or a rung whose targets are derived), it states the
  last performance alone. A plan that names reps but no load therefore leaves the field's load unlabelled:
  that is the cost of one source, accepted. This amends P3.17's "states its two sources", so DECISIONS is
  updated when it ships.

- **N377 — Auto progression and the earned set read soreness and joint pain.** The gate is per exercise
  and per session, from the lifter's own records. **Joint pain** ≥ 4 holds the auto step — whichever
  direction it earned, reps, load or a hold's seconds — and the earned-set offer; ≥ 7 also offers the
  existing deload/replace card, with the joint and its score as the reason. **Soreness** holds only the
  offer: a sore primary muscle > 4, or a sore secondary muscle ≥ 6, withholds the add and never the
  step. **Muscle feel** gates nothing; it stays recorded and charted, because 10 is *fully worked*, not
  *recovered*. The day's soreness is the daily record as it stands at the session's start — a record
  older than three days is stale — mapped through the exercise's `primaryMuscle` and
  `secondaryMuscles`. A session held by the add gate breaks the three-session pattern, so both the
  comfortable and extra-set offers need consecutive clear sessions and the Done snackbar and the plan
  card read one rule. **Nothing writes a set count**: a held step writes no target, a held add withholds
  an offer, and a trim stays the deload card's confirmed, undoable action, offered by pain alone. The
  Ask dialog is unchanged. The gate is derived from the stored ratings and days, so no schema or backup
  field is added; it needs the taxonomy on the workout row, a targeted daily-record read, ratings in the
  volume finder's session read, and the hold reason in the auto banner. Rejected: gating on muscle feel,
  letting soreness hold a step, writing the set count (N299's no down-adjustment and N311's lifter-adds
  rule), parsing the free-text readiness note, storing the hold as a row, and a soreness-triggered trim.

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
