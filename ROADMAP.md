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

## Next — the 2.27 review fixes

The review of `v2.26..HEAD` found five concrete gaps. **B390 is a release blocker**: the records timeline is
wired to a state field no production code fills, so it always renders empty.

- **B393 — Bound the program document's ancestor walk.** `withAncestors` still calls `findById` in a loop; the
  template/exercise/set reads are batched, but a deep library is still one query per ancestor. Fixed means one
  batch read per level (or one batch for the whole set) with a test proving the document still carries the chain.

- **B394 — Read trained counts only when the trained side is showing.** `ProgramBalanceViewModel` puts the logged
  sets query in `sources`, so opening the Plan tab also queries history. Fixed means the trained flow is selected
  by `view`, and the Plan side opens without a log read.

## Later — the rest of the 2.22 review

The rows below are real and specified, but none is in flight: a defect here is low-frequency or a polish item,
an engineering row is not user-visible, and a feature is waiting for a decision. A row leaves this section when
it is picked up; its design argument moves to [DECISIONS.md](DECISIONS.md).

### Defects and polish

- **B358 — The shell and the screens must not both apply the navigation-bar inset.** The shell reserves the
  tab bar, whose height already includes the system bar, and each screen's default `Scaffold` adds the system
  inset again ([AppNavHost.kt](app/src/main/java/com/example/androidapp/ui/navigation/AppNavHost.kt)); the
  deload screen adds a third `navigationBarsPadding()`. Fixed means the shell or the screens own the bottom
  inset exactly once, verified on a gesture-navigation and a three-button device (this was reasoned from code,
  not measured).

### Features

Each row below is a product candidate, not an agreed design; the constraints and the decision it touches are
named so the row can be picked up without re-arguing it from scratch. The id is the planning id; no row here is
agreed to ship.

- **N291 — Substitute an exercise inside a workout.** Weekly programs can substitute a workout for an
  occurrence, but mid-session the only options are remove and add, which loses the plan context. A session
  exercise's menu could offer "Substitute exercise", keeping its position and marking the stand-in so plan
  comparison says "substituted" rather than "missed". Medium; the plan-comparison rule is the open design
  question, and the app still suggests rather than writes.

- **N292 — Multi-week program phases.** A weekly program repeats forever and a sequence block happens once;
  there is no authored multi-week block (week 1/2/3 prescriptions). This is a real programming capability and
  the largest item on this list: new schema and program-document fields, a format-version bump, and an editor
  for the phases. Each week is authored by the lifter; the app does not auto-progress loads, which keeps N22's
  "no percentage/coach rule" decision intact.

- **N293 — Manual duration and distance for activities.** An activity is currently a marker with no numbers, so
  a swim or a ride records only "the day was trained". Manual duration/distance fields would extend the
  notebook without adding GPS or a new permission. Medium–large: schema, backup and helper format, and a
  decision about whether the values live on the session or a record of their own.

- **N294 — An encrypted backup file.** Encryption at rest is parked; an encrypted *export* is narrower: a
  password-protected file for a folder another app syncs, still user-chosen and local. It needs a new format
  version, Android Keystore/AES (no new dependency), and plaintext import kept for existing files. Only worth
  doing when a backup actually leaves the device.

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
