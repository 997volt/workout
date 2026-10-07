# Workout — Roadmap

> **v1.16** is shipped. Last reviewed against the code: 2026-10-07.
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

**Two requests from use**, each with the decision it settles. A candidate graduates to this section — gaining
an id and a spelled-out decision rather than a wish — when it is picked up, so what stands here is committed
work; the two queues below are where the rest lives, *Later* for what is self-contained and *Parked* for what
is a product in its own right.

- **N87 — the plans leave the workout's overflow, and home becomes the only way in.** The workout's ⋮ carries
  *Templates* and *Programs*
  ([ActiveWorkoutScreen.kt](app/src/main/java/com/example/androidapp/ui/workout/ActiveWorkoutScreen.kt)),
  which N78 put there so that checking what is next would not mean ending the session. The request reverses
  that half: mid-workout the overflow is the logger's own business, and the two plans belong where the
  decision to train is taken — home's start bar
  ([WorkoutsHomeScreen.kt](app/src/main/java/com/example/androidapp/ui/home/WorkoutsHomeScreen.kt)), whose
  links above the Start/Resume pill already stay visible while a session is open. Keeping that half is what
  makes the removal safe: looking at what is next still does not require ending the workout, because home is
  one step back. Three things come with it. The overflow then holds only *Discard*, so the ⋮ must be drawn
  only when there is something to discard — today it is drawn always, because the two plan entries were
  reachable from an empty session, while an empty session's own discard is prompt-free and drawn in the body
  (N41). The two entries' tags go with them. And [DECISIONS.md](DECISIONS.md)'s N78 rule and its
  [evidence](DECISIONS-EVIDENCE.md#n78) record the placement as settled, so they are amended with the
  reversal's reason rather than left contradicting the code — the class of defect the N80–N86 review found
  twice. B43's withholding is untouched: the templates list still disables *Start* while a session is open,
  and it is still reachable from home.
- **N88 — a next-up row's exercise count gets a line of its own.** The row's supporting line joins the
  program's name and the count with a separator
  ([WorkoutsHomeScreen.kt](app/src/main/java/com/example/androidapp/ui/home/WorkoutsHomeScreen.kt)), so the
  two read as one sentence: *Upper/Lower · 5 exercises*. The count moves to its own line, and **always** —
  the point is the shape of the row, not a wrap that happens once the text is long, so a one-word program
  name must not pull the count back up beside it. It stays the plural string it is, so it still reads one
  exercise or five. Home's *today* card keeps its single supporting line: the request names the next-up row,
  and the card is a different layout (N16, N61). N86's gap guard measures the row's text against its start
  pill, and this changes how tall that text is, so it is re-measured with the change rather than left to
  pass by luck.
- **N89 — starting a planned workout while one is running asks which.** `startOrResumeSession` is
  find-or-create, so starting a template with a session open hands the open one back
  ([RoomWorkoutRepository.kt](app/src/main/java/com/example/androidapp/data/RoomWorkoutRepository.kt)): the
  new plan is never seeded, and the lifter is dropped into the running workout as if they had asked for it.
  N78 named exactly this and fixed it in one place — the templates list disables *Start* and says why — and
  home's start actions have no guard at all. The request is a better answer than disabling: a dialog at the
  point of starting that names the workout in progress and offers *Continue workout* or *Discard and start
  new*. *Continue* opens the running session, which is what home's own pill does; *Discard* ends it as the
  existing discard does — a soft delete, after which a scheduled occurrence reads as a miss (P3.5) — and
  then starts what was asked for. The question is asked **after** the missed-day question, never before, so
  cancelling that one cannot leave a lifter with a discarded session and no start. The templates list keeps
  its disabled *Start*: a different screen with its own rule, and a dialog behind a control that cannot be
  pressed would never be reached.

## Later (still self-contained)

**Nothing.** Everything that stood here has shipped — the defects found in use, the workout screen's
discard, the workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch,
a rest of zero, the planned-set prefill, the program document, the eight defects a review of that batch
found and closed (B51-B58), and the seven requests that were its last queue (N80-N86) — each with its
entry in [CHANGELOG.md](CHANGELOG.md). A candidate graduates there — gaining an id and a spelled-out
decision — when it is picked up, so this queue is where unplanned work waits, and *Parked* below is where
deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (the app states what happened; it never writes what it decided), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

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

Bump the review stamp at the top whenever this file is checked against the code.
