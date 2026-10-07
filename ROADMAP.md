# Workout — Roadmap

> **v1.14** is shipped and installed. Last reviewed against the code: 2026-10-07.
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

**Five items, in the order they are to be done.** They are what *Later* was holding, picked up
together, so each carries an id and the decision it needed spelled out rather than a wish. The order is
not importance; it is what the code and the open questions already fix, which is why it is argued at the
end of this section rather than left to be re-derived.

- **N82 — The workout's preamble scrolls away, and *Add exercise* moves under the last exercise.** The
  body is a fixed `Column` — the record banner, the elapsed header, the readiness row, the rest bar —
  above a `LazyColumn` of exercises, so scrolling the work down leaves the readiness note and the clock
  in place; and the way to add a movement is an `ExtendedFloatingActionButton` over the list's last rows,
  which is exactly why that list carries 96 dp of bottom padding. Both move into the flow: readiness
  becomes an item the list scrolls past, and *Add exercise* becomes a button below the last exercise
  rather than over it — *Log set*'s shape, full width and a `FilledTonalButton`, filled with what the
  FAB draws today (`primaryContainer`, white on it) and keeping the `+` the FAB carries.
  `PrimaryActionButton` is the near miss rather than the answer: it is already a full-width pill that
  "used to be" an extended floating button (P1.16, N1) and takes both colours as parameters, but at its
  52 dp with its own glyph spacing it is not *Log set*'s shape, so this borrows the colours and takes
  the form from the log-set button.
  **What is not settled is where the line falls, because the rest bar must not cross it.** The
  countdown is the one thing here that has to be readable while the list is scrolled somewhere else —
  you are resting *from* a set that is no longer in front of you — so it stays pinned, and the record
  banner and the error line stay with it for the same reason (N23 deliberately puts the record between
  sets rather than over them). That leaves the elapsed clock: it is part of the same preamble as
  readiness and could scroll with it, or stay beside the rest bar as the one fact that is true for the
  whole session. Either way the refactor is the same one — the body becomes a single `LazyColumn` and
  `ExerciseList`, which has this one caller, becomes the items inside it instead of a list of its own.
  **The empty session is the edge the move creates.** `EmptyWorkout` is drawn *instead of* the list and
  offers only *Discard*, so the FAB is the one way to put the first movement into a new workout; a
  button at the end of a list that is not drawn leaves it with nothing to press, and needs the same
  button. The FAB's 96 dp of bottom padding goes with the FAB.
- **N83 — The *Start planned workout* pill takes *Log set*'s colour.** The pill is a `PrimaryActionButton`
  on its defaults and so draws `primary`, the app's own Indigo; *Log set* is a `FilledTonalButton` on its
  defaults and draws `secondaryContainer`, which N49 already took down to `TealDeep` so a white label
  passes on it. The change is that one pair — `secondaryContainer` with `onSecondaryContainer` — and it
  needs no new contrast work, because `PaletteContrastTest` asserts exactly that pair and names *Log set*
  and the rest bar as what draws it.
  **It lands against N49 and N61, both of which are written down.** N49 is settled in
  [DECISIONS.md](DECISIONS.md) and opens with *the Start pill* as one of the things `primary` *fills*;
  the same sentence is in `PaletteContrastTest`, so the decision's wording and that test's comment have to
  move with this rather than be quietly contradicted. N61 built the ladder on purpose — the *empty* start
  recedes to `primaryContainer` "so the two full-width pills do not read as the same action and the planned
  one is the app's suggestion" — and this puts the planned pill on a container step too, a deep teal
  beside a deep indigo. Both then read as recessed: the screen loses the loud one, and `primary` fills
  nothing on home but *Resume*.
  **Today's card is the same action in the same colour** — a plain `Button` reading *Start*, also on
  `primary` — and it is not the control the request names, so whether this covers both planned-workout
  starts or only the next-up row is what is left to settle.
- **N84 — A past workout's name goes on top of its detail, and its edits move behind one ⋮.** The title is
  the date and nothing else — the name N58 added appears only in a list row's supporting line — and all
  three ways to write sit live on arrival: a set row opens its editor on tap, every row carries a delete
  icon, and the rating row is editable in place. The request is the name as the title and one overflow
  holding **Edit**, **Save as plan** and **Delete**, which is the shape `WorkoutMenu` already uses on the
  workout screen: the destructive entry last and coloured, still asking first (B2).
  **The name is the template's, read live and never snapshotted** (N58), so nothing is written to the
  session and no column arrives: the title becomes that name with the workout's own date kept under it, and
  the date stays the title where a session has no template behind it, because a free workout has no name to
  show. Giving the workout a name of its own was the snapshot N58 rejected — a column and a migration for
  what only a *rename* changes — and it would put a rename and a set edit behind one *Edit* label.
  **What is not settled is how far Edit reaches, because the rating row is both a reading and a write.**
  Gating the tap-to-edit rows and their delete icons needs a way back out — a toggle whose label states the
  state it is in, the shape *Reopen*/*Done* already uses (N7, N69). What that mode has to decide is the
  ratings: N8 and N50 went out of their way to make them something the lifter opens, so hiding the *reading*
  of them behind Edit would take back what that bought, and the choice is a read-only rating while reading
  or one that stays editable throughout. The exercise *name* is not an edit — it opens that movement's
  trends — so it stays. *Save as plan* is N31's entry, offered only when there is something to copy, so the
  menu holds three actions on a workout with sets and two on one without: N53's "what cannot be done is not
  offered", rather than a disabled third entry.
- **N85 — Bring *Substitute* back to a next-up row.** Today's card offers it (P3.11) and the next-up rows do
  not, although a next-up row is the same `TodayPlan`: it already carries the slot's id and a *Start
  planned workout*, so `substituteOccurrence` fits it as it stands. **What is not settled is which
  occurrence the pick lands on.** A substitute is an event keyed by slot *and* week (P3.11), and
  [WorkoutsHomeViewModel.kt](app/src/main/java/com/example/androidapp/ui/home/WorkoutsHomeViewModel.kt)
  writes the week of *today* — right for today's card, which is that occurrence by construction, and
  questionable for a next-up row, which P3.9 makes deliberately **calendar-free**: a run advances when a
  slot is trained or skipped, never because a day passed. On a Sunday the next-up Monday falls in the
  *next* week, so the pick would be keyed to a week whose Monday has already gone. So the decision is
  whether a next-up pick is a week-bound substitution at all — and if it is, which week it lands in
  (`ProgramSchedule.occurrenceDate` is what turns a week and a weekday into the date in question) — or
  whether it only opens the session and records nothing.
- **N86 — More room around the next-up block.** A next-up row is drawn with 8 dp above it, inside the home
  start bar's own 12 dp of vertical padding, and its name sits against its *Start* button with nothing
  between them beyond the button's inset. With more than one program active (P3.12) the bar can carry
  several rows, and the block reads tighter than the cards it sits under. The decision is *where* the
  room goes — the block's own margin, the space between rows, or the gap between a row's text and its
  action, which are three different fixes — and it wants a device with two active programs rather than a
  guess. It is last for both reasons: it needs that device, and it measures a bar N83 and N85 have
  already changed — a pill whose colour moved, and a row that gained an action to space around.

**Why this order.** N82 repeats the foot-of-list action N81 introduced on the workout screen, so the two
screens are made to match while the shape is fresh. N83 rewrites the rule N49 and N61 hold; it comes after
N81 and N82 because both raise a container-filled action the rewritten rule should cover, and before N86
because N86 measures the very start bar N83 changes. N84 shares nothing with the rest and is the only item
that adds an interaction mode, so it waits for the layout work rather than interrupting it. N85 and N86 are
last because neither can be finished from the code alone — N85 needs a product answer about which week a
next-up pick lands in, and N86 wants a device with two active programs — and N85 comes before N86 because a
row that gains an action changes the spacing under it.

## Later (still self-contained)

**Nothing.** Everything that stood here has shipped — the defects found in use, the workout screen's
discard, the workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch,
a rest of zero, the planned-set prefill, the program document, and the eight defects a review of that
batch found and closed (B51-B58) — each with its entry in [CHANGELOG.md](CHANGELOG.md), and the seven
requests that were its last queue are picked up in *Next*. A candidate graduates there — gaining an id
and a spelled-out decision — when it is picked up, so this queue is where unplanned work waits, and
*Parked* below is where deliberate non-work lives.

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
