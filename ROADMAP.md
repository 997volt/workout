# Workout — Roadmap

> **v2.1** is shipped ([CHANGELOG.md](CHANGELOG.md)), and *Next* holds two items. Last reviewed against the
> code: 2026-10-11 — the N87–N95 batch shipped as 1.17; **N96–N102 with B95 shipped as 1.18**, with the six
> defects a line-by-line reading of that batch found (B96–B101); **N103–N106, N108, N109 and B102 shipped
> as 2.0**: the taxonomy becoming nullable, the store break that let it happen without a migration — a new
> database file, the 1.x chain and its schemas deleted, both transfer formats refusing 1.x, and the five
> retired enum names (four patterns and `BACK`) gone with the files that could carry them — the rest of the
> legacy the new store cannot hold, the decisions a later id reversed that were still reading as binding, the
> two columns and one transfer field nothing read while schema 1 was still unshipped, and the measurements
> screen's silent write failure; and **P1.1's library set, B103 and N110 shipped as 2.1**: the session and
> template reads no longer throw on an exercise that states no muscle, the library became a category over
> movements with a movement reading its family's pattern and muscles, 61 movements joined the 45, and the
> picker groups exactly as the library is. The 1.x store is left on the device by design, and the 1.18 APK is
> still the way to read it.
> Checked against the changelog as well as the code: every shipped id has its entry, and *Next* holds two
> items — soreness becoming a statistic, which is the reading N113's own record was missing, and a program's own
> muscle balance read from its overflow. **Reading the P1.1, B103 and N110 batch line by
> line — and running it on a device — found five defects (B104–B108); all five are fixed and recorded in
> [CHANGELOG.md](CHANGELOG.md).** A line-by-line reading of the N87–N95 changes found sixteen defects
> (B79–B94); all are fixed and recorded in [CHANGELOG.md](CHANGELOG.md).
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

Two items are planned: the reading for the soreness N113 records, and a program's muscle balance.

- **Soreness becomes a statistic, one series per muscle** (N118). N113 had the app start recording the muscles a
  day reports sore, each with its own score, and **nothing reads them back**: the measurements list and a
  session's own history rows are the only places they appear, so the one number the app asks for every day is
  the one it never shows. This adds the reading, not the record — a fifth `MetricKey` beside `Tape`, with the
  registry, chart, axis, readings and picker the other series already use.
  **It is shaped like a tape site because it is the same kind of fact**: `MetricKey.Soreness(muscle)`, listed
  under `MetricGroup.BODY`, labelled with the taxonomy's own `MuscleGroup.label`, and offered for
  `SORE_MUSCLE_GROUPS` — the selectable muscles minus *Other* (N62, N75) — so the picker offers exactly what the
  daily form lets a lifter choose, and cannot offer a muscle nobody can be sore in. The unit is the
  `MetricUnit.RATING` the workout's own effort, muscle feel and joint pain already plot, on the same fixed
  1–10 `RATING_SCALE`, and **`higherIsBetter = false`** is the one difference: like joint pain, more soreness is
  worse, and the chart already knows how to say which way is forward (N17, N39). `MetricKey.id` gains
  `SORE:<muscle>`, so a stored preference — and the rate target N98 gives every metric for free — names a stable
  series like every other metric's. The registry's own test asserts each key and each entry exist in both
  directions, so a half-added series cannot ship.
  **A day that states nothing for a muscle is a gap, not a zero**, which is the rule the tape series already
  follows: null is *unstated* (N110), and a line drawn at zero would claim a lifter was not sore on a day they
  simply did not answer. No schema moves and no transfer format moves, because the data is already in the daily
  record — it is a reading change, which is what makes it the cheapest way to close what N113 left open.
  Rejected: one aggregate "soreness" series counting how many muscles were sore in a day, which cannot say
  *which* muscle and answers only the question the list already answers.

- **A program can be read for the work it spreads where** (N119). The program editor's overflow gains *Muscle
  balance*: a destination of its own that counts the program's **planned working sets per muscle group**, so
  "does this week train what I think it does" is answerable before the week is trained rather than after. Two
  rules it inherits rather than inventing: a **warm-up is not work** (N112), so a ramp does not inflate the
  muscle it warms up, and a movement's muscle is **own-if-stated, else its category's** (N110) — which makes the
  read the program's templates *plus the library's categories*, because a movement that states none inherits the
  one it is filed under, and one that states none anywhere is a bucket of its own rather than being dropped:
  dropping it would make the rest look more balanced than the plan is. A set counts once, for its primary
  muscle, and assisted work is listed beside the count rather than folded into it — splitting a set between
  primary and secondary muscles would invent a fraction the plan never stated.
  **The read is new**: the editor loads one slot's template at a time for its preview (N72), so this is the first
  thing to read every template the program's slots name. Nothing is stored and no schema moves — it is the plan
  read as a week, drawn as a row per group with its count and its share of the program's total. **The bar drops
  to one control**: *Export* and *Delete* move into that same overflow, which is where *Muscle balance* arrives.
  Three actions with one of them destructive is the order N43 settled for the database's own actions — the one
  that cannot be undone goes last and reads as destructive — and it stops Delete being a one-tap icon beside a
  button a lifter presses often, at the cost of a tap each, which is what the overflow is for.
  Rejected: counting exercises rather than sets, which reads eight sets of squats and one set of curls as
  balanced; and reading **logged** history instead of the plan, because that is P2.8 — a rolling window with a
  verdict in it — which stays parked with its trigger intact, since this reads what is prescribed.

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
records the day (N114) — each with its entry in [CHANGELOG.md](CHANGELOG.md).

***Next* holds two items**: N118, found by asking what the app now records and never reads back, and N119,
which asks the same question of the *plan* — what it prescribes is read today only slot by slot, so a week's
shape is nowhere. The section is filled from what the log and the plan hold, never to keep itself full, and the
second item came with a bar that had grown a control too many. **The reading that produced 2.1 found five defects in it, all fixed before the tag (B104–B108)** — the one
that mattered left the instrumented job red, because a program document's former variation imports under its
family now and the test still asserted the link the shape no longer has. 2.0 shipped with the loudest note this project has written — the 1.x store stays on the device and
2.0 does not read it — and the rule that allowed that is in [DECISIONS.md](DECISIONS.md), with the window it
stays payable in.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
