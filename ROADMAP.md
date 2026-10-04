# Workout — Roadmap

> **v1.10** is shipped and installed. Last reviewed against the code: 2026-10-04.
>
> Forward-looking only. What shipped is [CHANGELOG.md](CHANGELOG.md), how a release is cut is
> [RELEASING.md](RELEASING.md), and settled decisions with the rules that apply to every
> change are [DECISIONS.md](DECISIONS.md) — their argument, where there is one, is
> [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).

A local-only training notebook: write a plan, log what you actually did against it, and let
the app show you the difference and what to do next.

Ids (`F#` foundations, `B#` defects, `N#` the next planned changes, `P#.#` the product
backlog, `R#.#` releases) are stable and go in commit messages. They were assigned when the
work was planned, so they do not run in order — and an id this file does not list has
shipped, with its entry in [CHANGELOG.md](CHANGELOG.md).

## Next

Nothing is planned. The deferred scope of the last two rounds — P3.3's and P3.5's, programmed as
P3.8-P3.16 — was built and shipped at 1.10, with its entries in [CHANGELOG.md](CHANGELOG.md). A candidate
graduates to this section — gaining an id and a spelled-out decision rather than a wish — when it is
picked up, so an empty *Next* is a state rather than a gap: the two queues below are where unplanned
work lives, *Later* for what is self-contained and *Parked* for what is a product in its own right.

## Later (still self-contained)

Post-MVP on the same local-only premise, grouped by theme and ordered by value inside each: a
candidate graduates to *Next* — gaining an id and a spelled-out decision — when it is picked
up, and leaves for [CHANGELOG.md](CHANGELOG.md) when it ships.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (N22's "the app suggests; it never writes"), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed. What waits now came
from using the built app rather than from either queue.

### The workouts tab, cut back to the question it answers

Three changes to one screen, and they are one argument: the tab that starts a workout should
answer "what am I training today" and get out of the way. Everything else on it is a second path
to somewhere the app already goes.

- **"See all workouts" goes.** It is a text button on the *Recent* heading whose only job is to
  open History — the tab beside it, one tap away and always visible. A section heading that
  carries a way out of its own section duplicates the tab bar, and *Recent* is not a thing the
  user needs to leave; History keeps the entry points it already has.
- **"Repeat last workout" gives its slot to Programs.** That link and *Start from template* sit
  as a pair above the start pill, and Programs — the screen the whole scheduling half is edited
  from — is reachable only from the overflow menu today. A destination belongs in the action row;
  the menu is where it got lost. Repeat-last is not deleted with its button (see *History*),
  because dropping the entry point to a shipped feature is not the same as deciding against it.
- **The overflow menu goes, and its three data actions move to Settings.** With Programs out of it
  the menu holds only export, import and delete-everything — not actions on a workout at all, but
  on the whole database, which is what Settings is about. This settles a placement that has moved
  twice: the library held them, B1 moved them to the home overflow as "back where you start", and
  Settings is a third and better answer rather than a return to the first — B1's argument was that
  the library sat two menus from where the user starts, not that a data action belongs beside
  *Start workout*.

### History: what a finished workout can carry

- **Repeat-last becomes an action on a finished workout in History**, which is the entry point its
  home-screen button gives up. It is the same one-tap copy of the last workout's exercises and
  order, addressed to the workout the user is looking at rather than to "the last one" — which is
  all that a button on the home screen could ever mean. The row already opens the workout, so the
  second action needs a home that reads as one.

### Settings: the app's data, and the rest timer

- **A Data section: export, import, delete everything** — the destination of the move above, each
  keeping what it does today. Delete-everything stays last and coloured, because it is still the
  one entry that can cost the user something.
- **A rest-timer switch, with the rest still on screen.** A rest is a countdown today: the session
  holds an end instant, the workout screen counts it down with ±15s controls, a rest that ends
  chimes (N27), and the default is editable (N21). The ask is the opposite preference — someone who
  rests by feel and would rather not be counted at — as a switch rather than a removal, because the
  prescription is still worth reading: with the timer off the screen shows **that exercise's own
  rest as a fixed label**, the same value the timer would have counted, falling back to the default
  rest when the exercise has none, with no countdown, no ±15s and no chime. Off has to mean the
  timer is genuinely not running rather than a countdown hidden behind a static number, so the
  decision this needs when it is taken is whether the end instant is simply never written while the
  switch is off.

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
| P1.9 | kg/lb display setting | You start lifting in pounds. Storage is canonical grams, so this is display-only whenever it is wanted. |
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
