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

Nothing is in flight. 2.21 shipped the 2.20 follow-up batch (N248–N254, B329) and the review's fixes, whose
settled rules are in [DECISIONS.md](DECISIONS.md). The unreleased [CHANGELOG.md](CHANGELOG.md) carries seven
entries: the two 2.21 follow-ups (N261, N262), the program table's repeated movement (N264), the energy
verdict's unit (B335), the body's two derived masses (N265), the zero that was a weigh-in (B336), and the day a
workout can now be moved to (N266). Three rows are queued from the review of that set; its fourth finding — this
summary, which named five of the seven entries — is fixed here rather than queued:

| # | Change | Why it matters | Size |
| --- | --- | --- | --- |
| B337 | **A refused day correction says which refusal it is.** `movedToDay` answers null both for an open session and for a day whose session would end in the future, and the repository turns both into "That day has not happened yet." The reachable case is the calendar's own bound: at 09:00, yesterday's 18:00 session moved to today — today *has* begun, 18:00 has not. Split the sentence, and give the open session its own ("Finish the workout first."), which is unreachable today because history lists finished sessions only but is not impossible. | The sentence is the only feedback a refused correction gives, and one that is wrong about *why* sends the lifter back to the calendar to try another day. The open-session branch is latent, and a latent branch whose message is wrong is the one that will be hit when a screen starts opening live sessions. | Small |
| B338 | **The measurement save refuses a non-positive weight, like the day's targeted write does.** `setDailyWeight` refuses `grams <= 0`; `save` validates sore-muscle scores but never looks at `weightGrams`. The form is `save`'s only caller today and it normalizes a typed zero to no weight, so nothing is broken — but the repository is where "an import may carry anything" is caught, and the rule B336 named now lives in one method only. Add the guard and the domain test `statedBodyWeightGrams`/`statesSomething` never had. | A backup or a future caller that hands `save` a zero is stored silently, and the readers are then the only thing keeping it out of the chart and the trend — the defence is a rule in every reader instead of one check at the boundary. The direct test is where the rule lives rather than where its effects show. | Small |
| N267 | **A corrected workout's day is announced.** The ⋮ → *Change date* write lands silently: the header's date changes and nothing says so. The per-screen rule is to announce state changes, and this write's whole effect *is* a date — so a snackbar naming it ("Moved to 26 September") is the confirmation the delete and its undo already use. | A lifter who picked the wrong day sees no evidence the tap landed, and TalkBack hears nothing at all. Every other write on this screen either changes the screen's shape (delete leaves) or is one row among many, while this one is a single date in a header line that is easy to have scrolled past. | Small |

## Later — decided, and not being started

Nothing is queued. This section is where agreed work waits its turn: a row here has its design settled so it
does not have to be re-argued, and a row in *Next* is what is actually being worked on. Both drain the same way —
a row picked up moves to *Next*, and one that ships leaves for the changelog.

The batch that was here — the 2.19 review's three data decisions (B326–B328) and three product rows (N245–N247)
— shipped in [2.20](CHANGELOG.md). A row returns here only once its design is settled and it is agreed to be
next.

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

Everything that has **left** *Next* has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
