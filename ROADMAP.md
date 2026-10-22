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

**A program can be a finite block, not only a weekly rotation.** A program is a week that repeats: a slot is a
template plus a weekday, that weekday recurs, and every week reads the same templates. A peaking block — nine
sessions, each with its own loads, done on alternating Mondays and Fridays and then over — has nowhere to live:
its nine sessions are nine templates, but nothing can say that the ninth is the last, or that session 4 is due on
*that* Friday rather than every Friday. That is the next work, and its shape is agreed: a program says whether it
repeats, and in one that does not, a slot's weekday *places* a single occurrence instead of scheduling a recurring
one. The loads need no new layer, because in a sequence every session is already its own template. Nothing is
built yet, and the rows are in build order.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| P3.21 | medium | **A program can be a finite block, with a beginning and an end.** The app's schedule has one shape: a weekday recurs, so nine distinct sessions cannot be placed on alternating Mondays and Fridays, and a load prescribed for "week 3" has no week to belong to. A block is the ordinary way a peak is written down, and today it can only be done by starting templates by hand from the library, with no due date and no sign of where the block is. | A **program kind** — weekly (today's, and the default) or sequence — and an authored **start date**, so a block's first session has a date to be due on rather than an accident of when the program was created. In a sequence the weekday is a **placement**: the session is due on the first such weekday after the last thing done, and the sequence **ends** instead of wrapping. A sequence is still one program whose slots are templates, so each session's loads are prescribed once and never move. **Progression is untouched**, and it is harmless here for a structural reason: a step accepted on session 3 rewrites only session 3's template, which a finite block never runs again — so RPE keeps earning steps on the weekly program and the block's numbers do not drift. Not planned in this block: a week-indexed plan entity, loads computed by the app, adherence scoring for a sequence, a per-program progression mode, and a `Kind`/`Starts on` column on the helper's sheet, which the app's editor covers until the sheet grows one. |
| N174 | high | **The model says what kind a program is, and when it starts.** Nothing in `programs` distinguishes a schedule that repeats from one that is done, and there is no authored date to hang a block's first due date on: `createdAt` is when the row was written, which is not when the block begins. | `programs.kind` stores `WEEKLY` or `SEQUENCE` **by name**, and `programs.startDate` an epoch day with null meaning the day it was created — both in one migration 7→8, read by the code added beside them. The program document gains both as optional fields and the format stays 4, the way N171's `edit` did; the backup carries both and its `schemaVersion` moves to 6, because an older build reading a sequence as weekly would repeat the block forever — a wrong meaning rather than a dropped extra. No new tables. |
| N175 | high | **The schedule is a derived next session and due date, not a stored plan.** A due date that is written down is N16's dated instance by another route: it goes stale when a session is done late, when a slot moves, or when the block is edited, and every screen would read it. | A pure sequence schedule — next slot, due date, complete — beside `programRun`, over the slots, sessions, skips, start date and today, so the screens and the tests read one function. **Next** is the first slot with nothing done and no skip; **due** is the first matching weekday strictly after the last event, or at or after the start date when nothing has happened yet; a slot with no weekday means the next day, so "whenever" sessions still work. `ProgramRun` gains a finished state rather than wrapping to the first slot. A weekly program answers exactly as it does now. |
| N176 | high | **Home offers a block's next session, and can start it.** With no weekday matching a recurring rule, a sequence's sessions are otherwise reachable only from the library, one template at a time — the run is read in the editor and nothing acts on it (N125). | When the next session is due or overdue, home's plan surface shows it as a card — name, `Day k of N`, its due date — and the card starts it with the slot id it already carries, so the prefill history is the slot's own. Nothing is offered before the start date. **No missed-day question is asked**: nothing is dropped, because the session stays next until it is done or consciously skipped, and asking on the way in is the interrogation P3.3 refused. |
| N177 | medium | **A rest day names what is next, for both kinds.** The rest line says only that today is rest; on a block's off day, or a weekly program's, the useful sentence is which session is coming and when. | The rest line names the next session: a sequence's next slot and its due date, a weekly program's next slot in the run and the weekday it lands on. **Informational only** — N125 removed the startable next-up row on purpose, and naming Friday's workout is not the same as acting on it. |
| N178 | medium | **The editor authors the kind and the start date.** A block arrives by import, so the fields cannot live only in a create dialog; and a sequence slot's weekday picker labelled "weekday" would describe the wrong rule. | A weekly/sequence toggle and **Starts on** (default: the creation date) in the program editor, the weekday picker labelled **Due** for a sequence slot, and `Day k of N` beside the order. An imported program is edited the same way, which is the path a block arrives by; a create-time dialog is not planned. |

None of this is a rule until it is built: the decisions it needs — that a sequence is an order with an end rather
than a dated instance (N16), that a weekday can place one occurrence rather than schedule a recurring one (P3.3,
P3.9), and why the backup's schema version moves — are recorded in [DECISIONS.md](DECISIONS.md) with the row that
implements them.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.**

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
