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

**A progression policy the plan can hold.** The app already computes one step per set at Done, but the
lifter accepts each by hand and cannot say what a step is.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| P3.17 | low | **The progression is a suggestion per set, and the rule is invisible.** N50/N74 raise a set's reps below its ceiling and its load at the ceiling, but the step is a fixed 2.5 kg, the direction is the app's own rule, and the lifter must accept each set one at a time. Nothing in the plan says what progression means for *this* lift, so a squat and a lateral raise are governed the same, and the why-line is missing at prefill, where the number is first seen. | A **progression policy on the plan exercise**: which direction(s), the step in grams (2.5 kg default), and the gate (the plan's target RPE, as today), authored once and visible. **Apply at Done** accepts every offer for the session in one action, with a summary and an undo. A **why-line** beside the prefill ("last time 5×100 @8.5; plan 5 @8 → 102.5 kg"). The setting becomes three states — suggest, apply at Done, off — per template. Still never silent: the click is the consent (N33 stands). A policy on the plan is authored training data, so where it lives is the first decision: a nullable field on the plan's exercise means a schema 6 bump carried in the backup, while a device setting keeps it local and leaves the backup unchanged. Not planned here: choosing the programme itself — exercises, volume, deloads — which is coaching, not arithmetic. |

**A program you can create from what you already did.** Two taps from a finished session to a scheduled
weekly workout, instead of an hour in the template editor.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| P3.18 | medium | **A program is created from nothing, not from what you already did.** The model is right — a template is *what* you do, a program is *when* — but the on-ramp is inverted: *New program* is empty, *Add workout* only offers templates that already exist, so a first-time user must leave for Templates, author every set by hand, and come back. Nothing starts from a session already logged, which is where most people's program actually lives, and every target is blank until typed one by one. | **Use as a template** from a finished session: copy its exercises and sets, take the weights used as starting targets, then offer to schedule it (weekdays) into the active program or a new one. **One authoring surface**: *Add workout* offers *pick a template* or *new workout*, created inline and returned as a slot. **Prefill targets from the last session** when an exercise is added, accept-or-edit, as N33 already requires at the workout. **Fast set entry** — "3 × 5 @ 100" with per-set editing for a top set and its back-offs. Use stays as today: today's card, one tap, P3.11's way to start a different day. No schema change. Not planned: merging template and program into one object (P3.12 and N47 depend on the split), and the web sheet (N167) stays a second on-ramp, not the main one. |

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The app-side fixture (N160) moved to *Next*, and the section drains the same way *Next* does.

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
