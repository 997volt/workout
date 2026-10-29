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

N237 is the row in hand: the rule that notices one plan's prescription stalling, so the app can suggest a deload
instead of saying nothing about a pattern it can already measure set by set.

**N237 — three misses on one plan suggest a deload, and the lifter decides.** The app can already say what each set
missed and why, and it says nothing about the pattern. The rule: for one template **inside one program**, take the
sessions in order and count, per movement, the prescribed sets that earned no step; **three consecutive sessions in
which the same kind repeats** is a stall, and the app says so where progression already speaks. Only
`REPS_SHORT`, `OVER_TARGET_RPE` and `LOAD_SHORT` count: `NOT_DONE` is absence, and a session that was skipped is
not a session that was failed. The scope is the template in the program — the same movement trained on another day
of the same program does not count, because the two days prescribe different work, and a session started outside
any program counts for nothing, because a deload is a program's week. The counting is per prescription rather than
per exercise, so a plan whose second set is always the one that fails says that; and because the offer reports one
miss per set by precedence, the counter records the kinds it can still measure rather than only the reported one,
or "over the target" would silently become "short". **It only suggests**: nothing is written, and the lifter
decides. The suggestion opens the existing deload creator with the program already chosen, because a deload is
already a marked week with days and a kept percentage (N147) and a second deload concept would be a second place
for one idea. Waits for N226, which is where the sentences live.

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

**N238 — a movement the library holds twice is merged.** There are two sources of duplicates and one write that
settles both. **The mechanical ones** are the import's: it creates a movement whenever a name resolves to neither
the library nor the seed and mints its own id, so a document that says *Back Squat* leaves a second *Back Squat*
beside the seeded one — two rows of one name in one list, with the history split across them. Such a row is
recognised by its shape rather than by a hand-written list: its name equals a seeded movement's, it states **no
taxonomy**, and it has **no family** — nothing an edit produces, because the app writes whatever it is given and an
imported row arrives blank. **The ruled ones** are the author's: a retired seed row whose twin is the same lift
(*Deadlift* is *Conventional Deadlift*), and rows with different names that are one movement (*Machine Fly*, *Chest
Fly (Machine)* and *Pec Deck*), which no rule can see and which the lifter's own list settles — that list is what
this row waits on. The seed's note already calls `two-arm-cable-pushdown` and `machine-row` second names for
movements it still ships, which is why both appear in the list. The write itself: the merged row's
session exercises and planned exercises are re-pointed onto the survivor — **two columns**, because a plan's sets
and a session's sets hang off those rows rather than off the movement — and the other row then goes, **deleted**
rather than soft-deleted: after the re-point it holds nothing, and its id is one the seed will never ship, so a
tombstone would only be a row the library must filter and every backup must carry. Where both rows hold history
they merge into one movement rather than one being dropped. **The name is not copied into history**: a session
exercise stores the movement's id and reads its name from the library, so a merged set reads the survivor's name
rather than the one it was logged under — the sets, their loads and their records are untouched. **A re-pointed
plan moves its own version**, so a program document exported before the merge is refused as stale rather than
merged into a plan that changed (N171). **A merged-away id is not forgotten**: the repair keeps a retired-id to
survivor map the document import resolves through, because a document naming an id the device no longer holds would
otherwise create the duplicate again; and the repair runs where the seeder runs — every open, and after a restore —
so an old backup is re-merged rather than left as a twin. Idempotent for the same reason.

**The merge list**, as ruled: the name before the arrow goes, the one after it is the survivor, and `(seed)` marks a
movement the seed already ships. Every line is a ruling about one gym's equipment, so each carries the condition it
rests on.

| Merge away | Into | Holds because |
| --- | --- | --- |
| `Deadlift` | `Conventional Deadlift` (seed) | decided: one deadlift, named for the way it is performed |
| `Calf Press` | `Calf Press (Machine)` | one machine, two names |
| `Single Leg Standing Calf Raise (Dumbbell)` | `Single-Leg Calf Raise` | the same lift, and the shorter name is the clearer |
| `Bicep Curl (Dumbbell)` | `Dumbbell Curl` | the seed ships no dumbbell curl, and this is the one |
| `Machine Arm Extension`, `Triceps Extension (Machine)` | `Machine Triceps Extension` | one machine under three names |
| `Single Arm Tricep Extension (Dumbbell)` | `Dumbbell Single-Arm Extension` | the same lift, one phrasing |
| `Dumbbell Flat Bench Press` | `Dumbbell Bench Press` (seed) | flat is the default press — there is no other to name |
| `One-Arm Dumbbell Row` | `Single-Arm Dumbbell Row` (seed) | wording only |
| `Rope Crunch` | `Cable Crunch` (seed) | a rope crunch is the cable crunch, on a rope |
| `Dumbbell Overhead Triceps Extension` | `Overhead Triceps Extension` (seed) | the cable version has a row of its own, so the generic one is the dumbbell |
| `Machine Fly`, `Chest Fly (Machine)` | `Pec Deck` (seed) | a pec deck is a machine fly, and the gym has one |
| `Two-Arm Cable Pushdown` | `Triceps Pushdown` (seed) | the seed's own note calls it a second name for a movement it still ships |
| `Machine Row` | `Seated Row (Machine)` | the lifter's own name is the clearer, so the retired seed row folds into it |
| `Cable Row` | `Seated Cable Row` (seed) | the seated stack, confirmed |
| `Calf Raise` | `Standing Calf Raise` (seed) | the standing machine, confirmed |
| `Dumbbell Shoulder Press` | `Seated Dumbbell Press` (seed) | performed seated, confirmed |
| `Forearm Flexion` | `Wrist Curl` (seed) | the seed's own wrist curl is a **dumbbell** movement, so this is the same lift rather than a bar one |

**Two rows are kept rather than merged.** `Cable Seal Row` is its own setup — chest-supported on a bench, however
close the name reads to *Cable Row* — and `EZ-Bar Forearm Extension` is a different implement from the seeded
`Reverse Wrist Curl`, which is a dumbbell movement: it joins the seed as its own row under *Forearm*. The rows this
table does not name are N239's, because they are new rather than duplicated.

**N239 — the lifter's own movements join the seeded library.** What is left after N238 — the rows the seed does not
ship and that are not a duplicate of anything — is the lifter's own training, and it is **bare**: an imported
movement states no muscle, no equipment and no pattern, so it is not found by "barbell", does not roll into a
family's muscle total, and cannot be sorted among its siblings. Each gains a taxonomy and a family, and joins the
seed **under the id it already has**, so no history has to move at all — the id is the link, which is the whole
reason this is the safe half of the work. The seed's catalog in `docs/` is generated from the seed and pinned by a
test, so it is regenerated in the same change. A retired name that survives N238 stays the lifter's row and does
**not** re-enter the seed: a retired name is a change to what a fresh install ships, and re-adding one would give
two installs two different libraries. Waits for N229, whose new families these rows are filed under, and for N238,
because a row merged away is a row not worth naming. **The upright rows need one more family** — four implements of
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
