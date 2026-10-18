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

**Nothing.** The pre-release review's twenty findings (B148–B160, N143–N146) are fixed and recorded in
[CHANGELOG.md](CHANGELOG.md): a seeded movement the database silently dropped, an import that un-hid rows a save
had replaced, a backup that could resume as the workout in progress, a save that read its day outside its
transaction, a rung removal that stranded its run, an assisted cluster that lost the machine's help, a superset
that stopped resting, a record count that disagreed with the rule, a plan read without the library's own filter, a
rung's reps matched by an index that is not a position, three transfers on the main thread, an export a rotation
could leave empty, an undo offered past its exercise, and four interface fixes: confirming an activity's discard,
stating a prescribed load with its unit and its assistance, telling a refused rung *why*, and searching the
history.

The review also found what the gates could not: `lint`, `detekt` and 1335 JVM tests were all green while every one
of those defects was live, and the two that mattered most were in seed data and in an import — the two places no
unit test was looking. Each row above shipped with a test that fails without its fix, and the instrumented suite
grew by four cases for the ones only real SQLite can show.

This section holds only work a **reading or a use** turns up — a defect found by using the app, a question the log or
the plan can answer and the screen cannot, a batch's own batch-reading — and it is never filled to keep itself
full. An empty section is a state, not a gap: a row stands here while it is being done and leaves for
[CHANGELOG.md](CHANGELOG.md) when it ships, which is how this section drains.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

- **A program can build its deload week, and the numbers come filled in** (N147). Asked for in use: a deload week
  is marked (P3.10) and nothing about the week changes — its slots still prescribe the full workouts, so the lifter
  either retypes every set to back off or trains the full plan and calls it a deload. The creator makes the mark and
  its prescriptions one act: choose how many days, tick the exercises each day keeps, and the app writes the
  workouts with sets, reps and loads taken from the program's own plan.

  **What it writes.** One ordinary template per day — *"Push — deload"* — and a plan for the week that names them.
  Nothing about a template is special: it is edited, versioned (N134), exported and backed up like any other. They
  are **copies, not references**, so a later edit to the program does not rewrite a deload already built — the
  draft is taken once, when the lifter asks for it. The week's days hang off the existing `program_deloads` row in a
  new `program_deload_days` table (`deloadId`, `templateId`, `position`, `weekday`), so a two-day deload is two days
  and the program's other slots do not appear that week. A day defaults to a program slot in order, takes that
  slot's weekday (an order-only slot's day stays order-only), the day count trims the last days, and the weekday is
  editable before anything is written. The backup gains one defaulted collection, for P3.10's reason: which workouts
  a marked week prescribed is authored setup the app cannot recompute, so a restore would otherwise keep the
  exemption and lose the work it excused. N47's program document still carries neither the plan nor its templates,
  because a week-keyed plan is history rather than a program's definition.

  **The draft's rule, stated so it is arithmetic rather than a coach.** The set count follows the plan, because the
  plan's count is the app's only evidence of how much work that lift normally does — a "compound" flag would be the
  app inferring a training intent the lifter already wrote down. **The plan's stand-alone working sets are kept,
  capped at three and floored at one**; a warm-up is preparation and a rung belongs to the set above it, so neither
  is a set of its own, and only the working sets are written — the session can still log its own ramp. A 3×5 squat
  keeps three lighter sets rather than losing one to a flat rule; a 5×5 keeps three rather than carrying the fifth,
  because past three the set count is the volume the week exists to remove; a 2-set accessory stays two. Reps are
  **one number, the highest target those sets name, floored at five**, so a 5/3/1 plan does 5s and a top-single plan
  does 5s rather than one; where the plan names none, the last finished session's highest working reps, and where
  neither does, none. Load is **the plan's heaviest stated working load — or the last finished session's heaviest
  working set, where the plan states none — kept at 60% by default** (50% and 70% offered), rounded to the
  exercise's own step (N77), and **no load at all** where neither states one, because zero is a claim (N14). One
  load per exercise: a 3×5 at a working 80% therefore reads 3×5 at about 48%, roughly 60% of the volume load it
  replaced. An assisted movement keeps its help, because assistance is a magnitude and reducing it would make the
  deload harder (N15). The plan's rest, technique cue and superset pairing travel; its effort target and its rungs
  do not. **One control**, the load kept at 60% by default (50% and 70% beside it), moves every added load
  together; anything finer is a template edit afterwards, and a second editor inside the creator was rejected.

  **Why this does not make the app a coach.** The week is still the lifter's mark, and the draft is written only
  when they ask: nothing scales a marked week by itself, no read infers a deload, and no number moves after the plan
  is saved. That is the narrow amendment to P3.10 — *nothing is scaled for you* becomes *nothing is scaled unless
  you ask, and what is written is an ordinary plan you can edit* — and [DECISIONS.md](DECISIONS.md) takes it when
  this ships.

  **It applies to the week it was built for.** The creator marks the current week, because a deload is decided by
  how the block is going and a week that has not started cannot be marked (P3.10). For that week, home's today plan
  and the missed-day question read the plan's days instead of the slots, so a reduced week offers the reduced days
  and no full workout on the others; a day already trained keeps its session. A deload session names no slot, so
  the run pauses rather than advancing (P3.9), and the week stays exempt from the ratio (P3.10): a deload is a break
  in the block, not a step through it. Unmarking the week leaves the templates in the library and puts the slots
  back; a week marked with no plan behaves exactly as P3.10 left it, which is what makes this additive.

  **Rejected:** a separate deload program to activate (two schedules and two histories for one block, and the run
  would move to it); writing the week as substitutions and inferring the plan from them (a reduced day count leaves
  the replaced slots' full workouts on home, and the read would have to guess that four substitutions mean a plan);
  scaling a slot's template in place (it rewrites every week that references it — N16 — and P3.10's slot
  prescription with it); and deriving the numbers from readiness or logged fatigue (N22's coach, and nothing the app
  records measures recovery).

  **Brought forward when** a deload week is next marked and its workouts then have to be retyped by hand — the
  friction that says the mark and its prescriptions are one decision.

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
- A web dashboard
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
