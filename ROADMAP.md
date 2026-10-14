# Workout — Roadmap

> **v2.2** is shipped ([CHANGELOG.md](CHANGELOG.md)), and *Next* holds nothing. Last reviewed against the
> code: 2026-10-15 — the N87–N95 batch shipped as 1.17; **N96–N102 with B95 shipped as 1.18**, with the six
> defects a line-by-line reading of that batch found (B96–B101); **N103–N106, N108, N109 and B102 shipped
> as 2.0**: the taxonomy becoming nullable, the store break that let it happen without a migration — a new
> database file, the 1.x chain and its schemas deleted, both transfer formats refusing 1.x, and the five
> retired enum names (four patterns and `BACK`) gone with the files that could carry them — the rest of the
> legacy the new store cannot hold, the decisions a later id reversed that were still reading as binding, the
> two columns and one transfer field nothing read while schema 1 was still unshipped, and the measurements
> screen's silent write failure; and **P1.1's library set, B103 and N110 shipped as 2.1**: the session and
> template reads no longer throw on an exercise that states no muscle, the library became a category over
> movements with a movement reading its family's pattern and muscles, 61 movements joined the 45, and the
> picker groups exactly as the library is; and **N107 with the N111–N121 batch shipped as 2.2**: the body's
> record split in two with soreness moved to the day and read back as a statistic, a set's number and the
> plan's remainder counting only the sets that stand on their own, the planned-set dialog's ± steppers and its
> one-line effort, rest and accept, today's workout at the foot of home beside the row that records the day,
> the library's long-press family details, a program read for the work it spreads where, and the three menu
> entries that reach what a row names — with the eight defects a reading of that batch found (B109–B116) fixed
> before the tag. The 1.x store is left on the device by design, and the 1.18 APK is still the way to read it.
> Checked against the changelog as well as the code: every shipped id has its entry, and *Next* holds
> nothing. **Reading the unreleased batch line by line found five defects (B121–B125); all five are fixed and
> recorded in [CHANGELOG.md](CHANGELOG.md).** **Reading the N107 and N111–N121 batch line by line — and running
> it on a device — found eight
> defects (B109–B116); all eight are fixed and recorded in [CHANGELOG.md](CHANGELOG.md).**
> Reading the P1.1, B103 and N110 batch line by line — and running it on a device — found five defects
> (B104–B108); all five are fixed and recorded in [CHANGELOG.md](CHANGELOG.md). A line-by-line reading of the
> N87–N95 changes found sixteen defects (B79–B94); all are fixed and recorded in
> [CHANGELOG.md](CHANGELOG.md).
>
> Forward-looking only. What shipped is [CHANGELOG.md](CHANGELOG.md), how a release is cut is
> [RELEASING.md](RELEASING.md), and settled decisions with the rules that apply to every
> change are [DECISIONS.md](DECISIONS.md) — their argument, where there is one, is
> [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md).

A local-only training notebook: write a plan, log what you actually did against it, and let
the app show you the difference and what to do next.

Ids (`F#` foundations, `B#` defects, `N#` the next planned changes, `P#.#` the product
backlog, `R#.#` releases) are stable and go in commit messages. They were assigned when the
work was planned, so they do not always run in order — and an id this file does not list has
shipped, with its entry in the changelog: [CHANGELOG.md](CHANGELOG.md) for the current line,
[CHANGELOG-1.x.md](CHANGELOG-1.x.md) for the 1.x releases.

## Next

Nothing. Everything planned has shipped; the next item is written here when there is one, and never
invented to keep the section full.

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

### N39 — the plan's target, parked by decision

The per-metric target shipped; **the plan's target was scoped out** when the feature was
built, so it is parked rather than planned. It is a different shape of work: the training plan
is not one of the statistics screen's sources, so it means making the plan available to a
screen that knows nothing about it, plus a rule for a lift that is in two plans at once or in
none.

## Explicit non-goals

Permanent, unlike *Parked* above: a parked row becomes possible again the moment its trigger fires, while a
non-goal is a line this app does not cross. They are of three kinds — a product in its own right that would
dilute the logging core, a line the hard constraints already draw, and a shape this repository has decided
against — and each row says which, because a refusal whose reason is lost is the one the next person
overturns.

| # | Non-goal | Why it is permanent |
| --- | --- | --- |
| — | Nutrition / calorie tracking | A product in its own right. The app states the energy implication of a weight trend it already owns (N98) and stores no food and no intake. |
| P3.7, P5.2 | Social feeds, friends and shared routines | Accounts, servers and moderation would have to be owned, and a feed needs a public. |
| — | Live GPS route tracking | A different product: this one logs sets, not distance. |
| — | A web dashboard | Statistics is where the log becomes useful; sending health data to a second place to read your own trend is worse privacy and worse UX, not a division of labour. |
| P4.1 | Health Connect read/write | A sharing integration. This app stores data for its user, and the export file is the only path off the device. |
| P4.2 | Foreground service | The app declares no permissions and holds no background work; a rest timer that needs a service to survive is the service's product. |
| P4.3 | Home-screen widget | A second surface is a second product to keep true. The app is opened and used. |
| P4.4 | Quick Settings / launcher shortcuts | One entry point is the app; a shortcut is the launcher's job rather than a second way in. |
| P4.5 | Wear OS companion | It needs `play-services-wearable`, and the no-Google-Play-services line is permanent. |
| P4.6 | Bluetooth heart-rate straps | That is heart-rate training rather than logging, with a sensor between the lifter and the log. |
| P4.7 | WorkManager reminders | The app never pursues its user: no notification permission, and nothing that asks to be opened. |
| P4.8 | Large-screen layouts | One layout, deliberately — a second is a second surface to design, test and hold to the per-screen accessibility rule. |
| P4.9 | Offline-first sync | It needs a backend and accounts, and the no-`INTERNET` line forbids the first; the largest irreversible commitment this app could make. |
| P5.3 | Monetization / Play Billing | Play Billing is a Play-services dependency, which the hard constraints forbid. |
| F6 | Module split into `:core:*` / `:feature:*` | One module is the shape, and the trigger the row named — a second surface such as Wear or a widget — is ruled out above, so the named goal it was waiting to become cannot arrive. |
| F11b | Product analytics | It buys nothing on a single-user local tool and would breach the no-`INTERNET` line. |

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

Everything that has stood in *Next* has shipped — the defects found in use, the workout screen's discard, the
workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch, a rest of
zero, the planned-set prefill, the program document, the eight defects a review of that batch found and
closed (B51-B58), the seven requests that were its last queue (N80-N86), the N87–N95 batch, the N96–N102
batch with B95 and the six defects a review of *it* found (B96-B101), 2.0 — the taxonomy and the store
break (N103, N104), the legacy and dead weight it left behind (N105, N106, N108, N109) and the measurements
screen's silent write failure (B102) — and 2.1 — P1.1's strength and bodybuilding set, the library as a
category over movements (N110), the unstated-taxonomy read (B103) and the five defects the reading of that
batch found (B104–B108), the warm-ups out of the set numbering and the plan's remainder (N112), the body's
record split in two with soreness moved to the day (N113), and the long entries deferring their argument to
the evidence (N107), the four that followed it — today's workout to the foot of home (N115), the set dialog's
one-line effort, rest and accept (N116), the library's long-press family details (N117) and home's row that
records the day (N114), a program slot's ⋮ opening its template (N120), a planned row's ⋮ opening the lift
(N121), soreness becoming a statistic (N118) and a program's muscle balance with the bar's overflow (N119) —
each with its entry in [CHANGELOG.md](CHANGELOG.md), the eight defects a reading of that batch found
(B109–B116) and the four a lifter found in use (B117–B120) among them — and N122, the half-credit a movement's
secondary muscles now take, N123, the behind-the-back cable curl the seed gained, and N124, the next-up
surface moving into the body, with the five defects a review of that batch found (B121–B125) fixed before the
tag.

***Next* holds nothing again.** N118
was found by asking what the app now records and never reads back, and N119
asked the same question of the *plan* — what it prescribes was read only slot by slot, so a week's shape was
nowhere. Both shipped, along with the three menu entries that reached a thing a row named. The section is filled
from what the log and the plan hold, never to keep itself full. **The reading that produced 2.1 found five defects in it, all fixed before the tag (B104–B108)** — the one
that mattered left the instrumented job red, because a program document's former variation imports under its
family now and the test still asserted the link the shape no longer has. 2.0 shipped with the loudest note this project has written — the 1.x store stays on the device and
2.0 does not read it — and the rule that allowed that is in [DECISIONS.md](DECISIONS.md), with the window it
stays payable in.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
