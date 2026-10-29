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

Nothing is in flight. Seven of the eight rows this queue held have shipped — the two library-working rows
(N230–N232), the exercise's own screen (N233, N234), three schema steps (N235, N236, N242) and two rules that read
history (N237, N238). What is left is N239, which waits for one file.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and
simply not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it
forward**, for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a
graveyard. **It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for
the changelog.

**The order below is the build order**: the two library rows, because the merge has to settle which movements
exist before the ones that remain can be given a taxonomy. Each row says what it waits for where it waits for
anything. The numbers quoted here are the measurements the work was planned from, not facts to keep up to date: a
row that ships leaves this file.

### The lifter's own library joins the seed

Both rows are driven by the same export and the same rulings, and both are measured rather than assumed: the
library it was planned from holds 186 rows against the seed's 126, and of the 60 that the seed does not ship, 3 are
names the seed retired on purpose, 15 are **duplicates of a seeded movement by name**, and 42 are movements the
seed has never had.

**N239 — the lifter's own movements join the seeded library.** What is left after N238 — the rows the seed does not
ship and that are not a duplicate of anything — is the lifter's own training, and it is **bare**: an imported
movement states no muscle, no equipment and no pattern, so it is not found by "barbell", does not roll into a
family's muscle total, and cannot be sorted among its siblings. Each gains a taxonomy and a family, and joins the
seed **under the id it already has**, so no history has to move at all — the id is the link, which is the whole
reason this is the safe half of the work. The seed's catalog in `docs/` is generated from the seed and pinned by a
test, so it is regenerated in the same change. A retired name that survives N238 stays the lifter's row and does
**not** re-enter the seed: a retired name is a change to what a fresh install ships, and re-adding one would give
two installs two different libraries. Waits for N229, whose new families these rows are filed under, and for N238,
because a row merged away is a row not worth naming. **It also waits for the lifter's export**: the rows this
row names came from one device's library, and only 5 of them are pinned by this text — the rest cannot be derived
from the seed, the app or the repo, so a table written without the file would be invented training data. **The upright rows need one more family** — four implements of
one lift with no family to hold them today — and it ships with them rather than before they exist.

**A movement is per implement the lifter loads separately**, and the equipment variants of one lift stay separate
movements rather than one row with a note. The seed already ships them that way — three bench presses, three flies,
four calf raises, a barbell and a dumbbell skullcrusher — because four of the facts a row owns are the implement's:
its **load step**, since an EZ bar moves in 2.5 kg, a dumbbell pair per hand and a cable stack in 5 kg, and one
step is wrong for the other two; its **display unit**, because a pound-stack machine and a kilogram bar are read
differently; its **bodyweight and timed flags**, which are the movement's own; and its **history**, since a best
set at eight reps across a bar and a pair of dumbbells describes neither. What keeps them one lift is the
**family**: the variants file under one head, so the library folds them together, the statistics roll-up reads them
as one series and the adherence breakdown counts them once (N97, N110). So the four upright rows are four movements
under one new *Upright Row* family, and the barbell skullcrusher sits beside the seeded dumbbell one under *Triceps
Extension*. The rule is not only the implement, either: the seed ships a paused bench press and a speed day that
share their parent's numbers, because the lifter **prescribes** them separately. So a movement earns a row of its
own where it would stand as its own line in a plan, and a grip or a stance that changes nothing else is a note on
the movement rather than a movement. **A variant nobody trains is not kept**: one row per implement actually loaded
is how the merge list was ruled, and a fourth row for a bell that is never picked up is the clutter N238 is
removing. **The implement goes first in the name**, as every seeded variant does ("Dumbbell Bench Press", not
"Bench Press (Dumbbell)"), which means the repair that files a row the seed now ships must **rename** it where it
still carries the import's own name — or a device and a fresh install would ship one id under two names, the
divergence B148 was.

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
