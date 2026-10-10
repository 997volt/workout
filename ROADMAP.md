# Workout — Roadmap

> **v1.17** is shipped. Last reviewed against the code: 2026-10-08 — the N87–N95 batch shipped as 1.17 and
> emptied *Next*; the two *Later* requests had graduated into it as N95–N97 and the *Later* section went with
> them, leaving N96 here. **N97 then came out of *Parked***, because the trigger it named has fired, and
> **fourteen parked ids became non-goals** — the platforms, services, sensors and shapes this app will not
> grow into — which is why that section is a table now. **N98–N103 and B95 were added from use**: body weight
> is kept in this app and what that lets the trend say; four smaller requests — the template editor's order, a
> history row's two numbers, an unrated exercise's prompt, and home's body in place of *Recent*; and, from
> reading the library's inheritance, one fact in one home — the taxonomy a child states rather than copies,
> and the equipment's own step that a variation was clearing. **All of it has since shipped except N103**,
> which is a nullable taxonomy — so a row states only what it knows and reads the rest from the head above it.
> A line-by-line reading of those N87–N95 changes
> found sixteen defects; all sixteen are fixed and recorded in [CHANGELOG.md](CHANGELOG.md).
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
shipped, with its entry in [CHANGELOG.md](CHANGELOG.md).

## Next

**Requests from use**, each with the decision it settles. Everything this round asked for has shipped —
the pattern vocabulary and its move onto the category, a head that reads as one number, the energy
adjustment, and the four small requests — each with its entry in [CHANGELOG.md](CHANGELOG.md). **One
change is left**, and it is the only one that rebuilds a table.

- **N103 — the taxonomy becomes nullable, and a row stops copying what it inherits.** A child's
  taxonomy is a *copy* today: a variation is `parent.copy(...)`, and the seed restates a movement's muscle,
  equipment and pattern on every variation it ships. **The model becomes one rule: a row stores what it
  states, and anything unstated is read from the nearest row above that states it.**
  `primaryMuscle`, `equipment` and `movementPattern` become nullable with `null` meaning *inherit*, and `OTHER`
  returns to meaning only *other* — spare values today, because `OTHER` is both a real answer and the
  placeholder a head is seeded with. **The winner is the same for every one of them: own-if-stated, else
  inherit**, which is what the secondary muscles already do and what primary muscle does not:
  `effectivePrimaryMuscle` returns the nearest stating **ancestor** and treats the row's own value as a
  fallback, so a filed exercise's own muscle is written, exported and read by nothing while a head states one
  — and the edit form offers a picker that cannot visibly change anything. One rule ends that.
  **The migration clears a stored value only where the nearest stating ancestor states the identical value —
  never blanket-null**: a movement's equipment is genuinely its own, since `barbell-bench-press` states
  `BARBELL` while the head above it states nothing, so blanking the column would stop the library drawing
  "Barbell" on every bench. It is scoped to **seeded** rows, which carry one batch timestamp and are therefore
  recognisable as seed rather than as a lifter's answer. **A row stops being self-contained, and that is the
  price**: the program document already closes over the parent chain — that half shipped with this round — and
  the backup's `schemaVersion` **must bump**, because a newer file carries `null` where an older build expects
  a value. A file written before this stays safe: it carries full copies, and a stated value still wins under
  the one rule both builds share.
  **One winner rule or two — settled here, because implementing this would otherwise hit it mid-way.** N103
  says the winner is own-if-stated for *every* field; N96, which has shipped, says the pattern belongs to the
  category and reads the head. Those agree only while no movement states what its head also states, and the
  selective clear makes that true of the **data** rather than of the model — so the two rules would diverge the
  first time a lifter set a pattern by hand. **The resolution is own-if-stated, uniformly**, which is N103's
  rule and one rule rather than two: the category is the pattern's home because nothing under it states one,
  not because it outranks them. That keeps everything N96 bought — a family's pattern decided once, and a row
  in no category with none — while dropping the absolute claim that a movement may never state its own. The
  DECISIONS rule N96 landed is amended in the same change, since it currently says the pattern is read off the
  head.
  **What the work touches, in the order that keeps it compiling.** The entity and its two mappers, where the
  three columns become nullable — the enum-by-name storage is unchanged, which is what lets an older file keep
  reading. The domain `Exercise` and the resolvers: `effectivePrimaryMuscle` flips to own-if-stated, and an
  `effectiveEquipment` joins it, so all three read the same way and `effectiveMovementPattern` loses its
  head-wins special case. The seed, whose `barbell(...)` helper stops demanding a taxonomy a movement inherits,
  and the seeder's `INSERT OR IGNORE`. The repository's `create`, `createVariationOf` and `updateExercise` —
  the three places a copy is written. The editor: the draft's fields, and the selectors that must stop offering
  what a head supplies rather than the pattern alone. Both transfer formats, whose DTOs take the nulls and whose
  codec version must bump. The migration itself, its exported schema, and its `MigrationTestHelper` case.
  **Every one of those is a compile error until the whole set is done**, which is the shape of this change
  rather than an accident of it — and the reason it cannot be landed in pieces.

  **The pattern needs no separate work, and N96's remainder is this migration's own rule.** Its entry
  used to say the per-exercise column is dropped, which was written before it was clear that a category is
  a row of the *same* table: there is one `movementPattern` column and it has to survive, because it is
  where a category states its family's pattern. What N96 has left is the movement-level **values** — a
  stale `HORIZONTAL_PUSH` still sitting on a filed movement — and clearing those is exactly the
  selective-clear rule below: a value goes where the nearest stating ancestor states the same fact.
  Nothing reads it either way, because `effectiveMovementPattern` reads the head; the cleanup is what
  makes the stored shape say what the model says.

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
closed (B51-B58), the seven requests that were its last queue (N80-N86), and the N87–N95 batch — each with
its entry in [CHANGELOG.md](CHANGELOG.md).

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
