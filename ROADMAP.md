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

**Two things queued, neither started.** The statistics figure is drawn at a fixed 180 dp while its expanded view
takes 55 % of the window — the opposite of what N196 says the figure's height is (B261). And the bottom bar's
buttons do not do what they look like they do: tapping the tab you are already in changes nothing, so Library → an
exercise → Statistics → Library returns to the exercise and a second Library tap leaves you there (N223).

| # | What it changes |
| --- | --- |
| B261 | **The inline figure is a constant, not the room the screen has.** `CHART_FIGURE_HEIGHT` is 180 dp, passed by `SeriesChart` to a component that already takes its height as a parameter, and it was picked when the card also carried a row of window chips. N211 collapsed those chips into one control and N222 put that control on Expansion's line, so the figure is the only thing in the card that never changed — on a 914 dp phone it draws 19.7 % of the screen while the expanded view draws 55 %, and N196's own rule ("the height is the screen's … statistics' subject *is* the figure") is what the constant quietly breaks. Give it a share of the window the way the expanded view has one — a fraction clamped so a short phone still shows the card and a tall one does not get a second expanded view, e.g. 30 % between 220 and 340 dp: about 275 dp on that phone, which is +53 % — or, if the clamp is too much machinery, raise the constant to 240 dp and say so. Either way the expanded view must stay the larger of the two, the axis and the row of dates and actions move with the height, and the tests pin both sizes: the inline figure against the window, and expanded above inline. |
| N223 | **A tap on the current tab shows that tab's main view.** Selecting a *different* tab keeps today's behaviour — its saved back stack and its destinations' saved state come back, so an exercise looked up in Library, a workout opened in History or the Statistics lift you were comparing all survive the round trip. Selecting the tab that is **already active** pops to that tab's root instead, and at the root it is a no-op, which is what a tap on a button with nothing to undo should be. Today that tap does nothing at all, measured on the emulator: Library → Bench Press → an exercise → Statistics → Library restores the detail, and a second Library tap leaves it there — so the bar reads as a map while the button behaves as if you were already standing on it. **Not always-reset**, which is the tempting alternative: switching to another tab and back would then drop the Library search and scroll, the Statistics lift and metric, and any typed-but-unconfirmed editor field (the template rename is a `draft` in the composer, and an open set dialog lives in the same place), and one wrong tap would have no way back. The mechanics: `switchTab` branches when the current destination's tab — `AppTab.forRoute`, matched by prefix so a route with arguments counts — is the one tapped, and calls `popBackStack(tab.route, inclusive = false)` without touching `saveState`, so the other tabs are untouched; the bar is already hidden on the logger and the picker, so a workout cannot be interrupted this way. Pinned beside `switchingTabs_leavesOneBackEntry_andBackReturnsToWorkouts`: switching still restores, re-tapping floors to the root, back from the floored root still reaches Workouts, and a re-tap at the root changes nothing. |

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.** The timed set (N220) has shipped to the unreleased section.

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
