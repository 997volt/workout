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

- **N294 — An encrypted backup file.** Encryption at rest is parked; an encrypted *export* is narrower: a
  password-protected file for a folder another app syncs, still user-chosen and local. It needs a new format
  version, Android Keystore/AES (no new dependency), and plaintext import kept for existing files. Only worth
  doing when a backup actually leaves the device.

## Later — the deload seam

Two different things carry one name today. P3.10's mark exempts a week from scoring and leaves its
workouts alone; N147's built plan writes lighter ones. The Adherence chip calls the mark a deload, the
creator lives in a program-editor overflow, and neither says what the other is. N300 separates the two
actions, the B rows are the draft's blind spots, and N301 is the follow-on for a program with no end.
None of it is in flight.

### N300 — An exempt week and a deload week are two actions

The mark is an **exemption**, not a lighter week: its slots still prescribe their full workouts, and
the only thing it changes is that the week is not scored. The built plan is the deload. The change is
to say that in the names and give each action its own way in.

- **Name them apart.** The Adherence mark becomes *Exempt week* — "not scored" — and *Deload week*
  stays the creator's plan. One entry on the program screen offers *Exempt this/next week* and *Build
  a lighter week*, so the choice is made where a lifter is looking at the program rather than in two
  screens that never mention each other.
- **Offer the prospective exemption.** Today a week can be exempted only after it has started
  (Adherence), and the creator only builds next week. A vacation, a sick week or a planned rest week
  cannot be pre-marked without writing workouts nobody asked for. The repository already stores a bare
  mark; add the action for next week, and let a later *Build* reuse it.
- **Do not ask the missed-day question about an exempt week.** A built week is already left out
  (B163); a marked week still asks. That contradicts the name: the week is exempt from judgement, and
  the prompt is a judgement. Amend P3.10's "the missed-day question is unaffected" — the calendar
  still draws the days, a session still marks a trained day, and the ratio still leaves the week out;
  only the question goes. The argument moves to DECISIONS when this is picked up.
- **Show it on home.** A marked-only week looks exactly like a normal one on the training surface. A
  banner saying the week is not scored is enough; the full workouts must not be labelled *Deload*.
- **Reach the creator.** Add it to the program screen, not only the overflow and the stall card. A
  program that runs for years should not hide its recovery week behind a menu.
- **Give the draft an effort.** It deliberately writes no target RPE, so the workout opens its effort
  field on the app default 9.0 — the opposite of a deload. Write a deload effort (6.0 would do) or show
  a genuinely blank field; the first is one decision, the second a wider change to the session's
  no-target shape.
- **Name the workout after its week.** *Deload A* is indistinguishable after a year of them; put the
  week in the default name or hide the templates once the week is over, keeping them resolvable for
  the history that names them.
- **Say what unmarking does.** The dialog says the plan will not be used; it does not say the week
  returns to the normal schedule and can read as missed, because the deload sessions used templates
  that settle no slot. Say both, and keep the templates.

### N301 — A program with no end suggests when a deload may be due

The stall card is reactive. A program that runs forever has no moment that says "you have been at this
for six weeks". A card after a user-chosen number of weeks since the last deload — or since the
program started — says a recovery week may be due and opens the creator. It is a **suggestion**, not a
computed deload: the cadence is the lifter's, the app only counts weeks it can see, and there is no
recovery model and no notification (P4.7 stays a non-goal). Later than N300, and only if a cadence is
actually wanted.

**Not planned in this seam.** An automatic deload (a scheduled write is a coach, and nothing the app
records measures recovery), a percentage of a one-rep max (the app estimates rather than measures one),
a current or partial week (the plan is whole-week), a second deload entity (ordinary templates plus
the week-keyed mark are the right shape), and a change to the 60% rule without a new argument.

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
