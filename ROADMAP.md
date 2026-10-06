# Workout — Roadmap

> **v1.14** is shipped and installed. Last reviewed against the code: 2026-10-06.
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

### N79 — a drop or a cluster is one group, judged on its first set

A **drop** and a **cluster** are one shape: a contiguous run of sets hanging off a single working set. The
anchor is a set that records effort (Working, Top set or Failure — never a warm-up), and adjacency is the
parent link, so **nothing ties a rung to its set**. `SetType` gains **`CLUSTER`** beside `DROP`, and both
come to mean *a member of the group above me* rather than a set of its own.

**A rung carries no target of its own**: no reps, no rating, no weight written down. Its weight is
**derived** from the anchor — the anchor's own for a cluster, `anchor − k × dropValue` for a drop, so a
20 kg value under 100 kg is **80, then 60**. The value is **held by the run's first rung** and inherited by
the rest, so the plan edits one number, accepting a step writes one row, and no rung can drift. It is
validated where every other weight is — above zero, below the anchor's, and with the **whole ladder**
checked so the last rung cannot compute to nothing — and **refused under an anchor with no added weight**:
an assisted or bodyweight set has no 20 kg to take off, which is the absence the progression rule already
refuses to step, so the picker does not offer a drop there. A rung that would compute to zero or below
reads as carrying no derived weight, rather than turning into assistance. A cluster needs no
guard: it copies whatever load the anchor has, assistance included.

**The prefill splits accordingly.** A rung's weight is computed **from what the anchor actually did** in
this session — the plan's weight standing in before it has been done, so stripping 20 kg off a bar you
loaded 5 kg heavy gives 85 and not 80 — and its **reps come from the same set's last performance in a
previous training** — matched by the set's place in the plan, the only identity a logged row keeps
between sessions — falling back to the set just done.

**One rating for the group**, which is how `recordsEffort` already works: a rung records none, so the
rating sits on the anchor. The reason is deliberately **not N67's** — a rung is work, just not work rated on
its own — and a rating already stored on a drop row stays on disk and stops being displayed. **A rung
cannot set a personal record** either; the record belongs to the anchor, the set that carries the rating.
Volume is unchanged, since it already sums every set and every rep.

**The group is judged on its first set**, both kinds alike — the plan's reps met and the effort inside the
exercise's target, then N74's step applied **once to the group**. So a rung cut short does not hold the
group back: it has no target to miss. The question states the group **once**, the anchor with its step or
its reason and its rungs beneath it as recorded work. **Rest needs no new field** — the exercise's rest runs
once, after the run's last rung, which is what a superset round already does.

**The pairing becomes role-aware, or a run breaks the sets after it.** The Done prompt pairs non-warm-ups
by position while the prefill counts *every* logged row, so an ad-hoc drop both shows the next prescribed
set the wrong plan row and is judged as a working set — its reps able to earn that set a heavier weight,
which accepting writes into the plan. Prescribed non-members pair with performed non-members and prescribed
rungs with performed rungs, each in order; a rung logged with no plan row hangs off the set before it, so it
takes that group's rating and rest while staying unjudged. **One limit stays**: pairing within a class is
still positional, so a *skipped* prescribed set shifts the pairs the way an extra one used to — closing that
needs an identity a logged row does not carry, and it is its own id if it is wanted.

Three small rules, without which the shape has no meaning: **a lone rung is refused**, **a warm-up cannot
anchor**, and **deleting an anchor takes its rungs with it**. The column is `dropValueGrams` on a planned
set — migration 34→35, nullable, shipping with the reader that reads it — where null means *not a rung* or
*another rung of the same run*. A plan written before this holds an absolute weight on a `Drop` row and
keeps that reading, and a file that leads with a rung reads it as a plain set: nothing is invented, and
nothing fails to load.

## Later (still self-contained)

Everything that stood here has shipped — the defects found in use, the workout screen's discard, the
workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch, a rest of
zero, the planned-set prefill, the program document, and the eight defects a review of that batch found
and closed (B51-B58) — each with its entry in [CHANGELOG.md](CHANGELOG.md). A candidate graduates to
*Next* — gaining an id and a spelled-out decision — when it is picked up, so this queue is where
unplanned work waits, and *Parked* below is where deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (the app states what happened; it never writes what it decided), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

### Nothing waiting

The queue is empty rather than closed: it is where a candidate waits as a wish until it is picked
up, and picking one up is what gives it an id and a spelled-out decision. The last request that
stood here — an exercise's own weight change — became N77 that way and has shipped, with its entry
in [CHANGELOG.md](CHANGELOG.md).

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
