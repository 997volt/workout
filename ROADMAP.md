# Workout — Roadmap

> **v1.18** is shipped. Last reviewed against the code: 2026-10-10 — the N87–N95 batch shipped as 1.17 and
> emptied *Next*; the two *Later* requests had graduated into it as N95–N97 and the *Later* section went with
> them, leaving N96 here. **N97 then came out of *Parked***, because the trigger it named has fired, and
> **fourteen parked ids became non-goals** — the platforms, services, sensors and shapes this app will not
> grow into — which is why that section is a table now. **N98–N103 and B95 were added from use**: body weight
> is kept in this app and what that lets the trend say; four smaller requests — the template editor's order, a
> history row's two numbers, an unrated exercise's prompt, and home's body in place of *Recent*; and, from
> reading the library's inheritance, one fact in one home — the taxonomy a child states rather than copies,
> and the equipment's own step that a variation was clearing. **All of it has since shipped as 1.18 except
> N103**, which is a nullable taxonomy — so a row states only what it knows and reads the rest from the head
> above it.
> Checked against the changelog as well as the code: every shipped id has its entry there, and **N103 and N104
> are the only two this file still lists as planned**. Reading this batch line by line found six defects
> (B96–B101); all six are fixed and recorded in [CHANGELOG.md](CHANGELOG.md), so *Next* holds the taxonomy
> again — and, planned beside it, **the 2.0 break that makes it a new store instead of a migration (N104)**,
> affordable only while the log on this device is the only one there is.
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
adjustment, and the four small requests — each with its entry in [CHANGELOG.md](CHANGELOG.md). **Two
changes are left, and they are one piece of work**: the taxonomy, and the 2.0 break it rides on. The break is
the licence the taxonomy needed — with no other user yet, 1.x is left behind rather than carried, which
deletes the migration half of the taxonomy change instead of writing it.

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
  **There is no migration, because N104 gives this a new store.** The selective-clear rule this entry used to
  carry — clear a stored value only where the nearest stating ancestor states the identical value, and only on
  seeded rows — existed to clean 1.x rows that state what they inherit, and a fresh store has none: the seed
  writes a null wherever a row means *inherit*, so the library is right the first time it is opened. **A row
  stops being self-contained, and that is the price**: the program document already closes over the parent
  chain — that half shipped with this round — and a file written before 2.0 is refused rather than read, since
  both formats move to v4 and the gate learns a minimum (N104).
  **One winner rule or two — settled here, because implementing this would otherwise hit it mid-way.** N103
  says the winner is own-if-stated for *every* field; N96, which has shipped, says the pattern belongs to the
  category and reads the head. Those agree only while no movement states what its head also states, and the
  seed makes that true of the **data** rather than of the model — so the two rules would diverge the
  first time a lifter set a pattern by hand. **The resolution is own-if-stated, uniformly**, which is N103's
  rule and one rule rather than two: the category is the pattern's home because nothing under it states one,
  not because it outranks them. That keeps everything N96 bought — a family's pattern decided once, and a row
  in no category with none — while dropping the absolute claim that a movement may never state its own. The
  DECISIONS rule N96 landed is amended in the same change, since it currently says the pattern is read off the
  head.
  **What the work touches, in the order that keeps it compiling.** The entity and its two mappers, where the
  three columns become nullable — the enum-by-name storage is unchanged, so the column keeps reading names
  rather than ordinals. The domain `Exercise` and the resolvers: `effectivePrimaryMuscle` flips to own-if-stated, and an
  `effectiveEquipment` joins it, so all three read the same way and `effectiveMovementPattern` loses its
  head-wins special case. The seed, whose `barbell(...)` helper stops demanding a taxonomy a movement inherits,
  and the seeder's `INSERT OR IGNORE`. The repository's `create`, `createVariationOf` and `updateExercise` —
  the three places a copy is written. The editor: the draft's fields, and the selectors that must stop offering
  what a head supplies rather than the pattern alone. Both transfer formats, whose DTOs take the nulls and whose
  version moves to 4 with the gate's new lower bound (N104). **No migration and no exported schema**: the store
  is new, so there is nothing to carry.
  **Every one of those is a compile error until the whole set is done**, which is the shape of this change
  rather than an accident of it — and the reason it cannot be landed in pieces.

  **N96's remainder is the seed, not a migration.** Its entry used to say the per-exercise column is
  dropped, which was written before it was clear that a category is a row of the *same* table: there is one
  `movementPattern` column and it has to survive, because it is where a category states its family's pattern.
  What N96 has left is the movement-level **values** — a stale `HORIZONTAL_PUSH` sitting on a filed movement —
  and on N104's new store the seed never writes one: a movement that inherits states nothing, and the four
  retired names go with the shim, so there is no value left to clear.

- **N104 — 2.0 starts a new store, and 1.x is not carried across.** The break N103 rides on, and the reason
  the taxonomy change has no migration. **The mechanism is a new database file** — `workout-2.db` beside the
  existing `workout.db` — so Room creates it fresh, the seeder populates the library, and the 1.x file is
  never opened by this build: it stays on the device, untouched and app-private, and **the 1.18 APK is still
  the way to read it**. Nothing is migrated and nothing is destroyed, which is why this leaves the standing
  rule against `fallbackToDestructiveMigration()` exactly where it is: the old store is left alone rather
  than truncated.
  **What the break deletes, because nothing in 2.0 can reach it**: the 1.x migration chain (`Migrations.kt`
  and `ALL_MIGRATIONS`), every schema it exported, and `WorkoutDatabaseMigrationTest`. The new store starts
  at schema **1**, so a fresh `1.json` is written for it and the chained versions' schema files go with the
  chain — a schema file nobody migrates to is a contract with no counterparty, and the git history holds
  what they documented. The four retired pattern names and `SELECTABLE_MOVEMENT_PATTERNS` go with them
  (N96's shim): no 2.0 row, and no file 2.0 will read, can carry one.
  **Both transfer formats refuse 1.x rather than read it**, and that is a change *to* the gate rather than a
  way around it (B62): `VersionGate` gains a **minimum** beside its maximum, so a 1.x backup or program
  document is refused before the decoder, with a sentence that says "written by 1.x" rather than "corrupt" —
  which is the only reason deleting the retired names is safe, since the decoder is what would have thrown
  on them. The formats move to **4** and keep counting up, because 1.x files exist and a fresh "1" would
  name both shapes. A 1.x device and a 2.0 device cannot hand each other a program or a backup either, which
  is what *no backwards compatibility* means for the two formats.
  **The price is the history on the device, and the window it is payable in.** A 2.0 install cannot read its
  own 1.x log, and the export is one-way — 2.0 refuses the file 1.18 writes — so exporting from 1.18 *before*
  upgrading is part of the release rather than a footnote, and the release notes say so. **This is affordable
  only because there is no second user yet**: it is a door that closes the first time somebody else's log
  exists, and after that the same change is a migration done by the book.
  **"Delete everything" has to mean it**, which is the hard constraint reaching a second store:
  `clearAllUserData` clears the open database, the goals and the crash logs today, and with two stores it
  must delete the 1.x file too — otherwise a lifter's "delete everything" leaves their training history on
  disk in a file no screen mentions.
  **The release is 2.0**, and `versionCode` keeps increasing, so Android compares it the same way. The rule
  this settles — **a major release may start a new store instead of migrating one** — amends the "done by
  the book" schema rule (N24) and lands in [DECISIONS.md](DECISIONS.md) with the change: an exception this
  large, left unrecorded, is the one the next person reads as an oversight.
  **Rejected: keep the chain as documentation.** A migration suite and its schema files that no code path
  can reach, to describe stores this build cannot open.
  **Rejected: migrate by the book.** One selective `UPDATE` and a version bump, but it buys nothing for a
  library whose only reader is its author, and it is the half of N103 this entry exists to delete.

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
closed (B51-B58), the seven requests that were its last queue (N80-N86), the N87–N95 batch, and this round's
N96–N102 with B95, and the six defects a review of *it* found (B96-B101) — each with its entry in
[CHANGELOG.md](CHANGELOG.md).

**N103 and N104 are the exception, and the two entries left above it.** N103's transfer half — a program
document closing over a variation's ancestors — shipped with the rest; the nullable taxonomy the id is named
for has not, and the entry says what is left of it and in what order. N104 is the 2.0 break that deletes that
entry's migration half and strands the 1.x log on the device **by design**, which is why it is the one change
here that has to be said in the release notes rather than only in the changelog.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what they named
that is not a feature is a settled decision: no dated instances (N16), nothing automatic (the app states
what happened; it never writes what it decided), a weekday-less slot that is never missed and is
order-only, and more than one active program, which P3.12 allowed.

Bump the review stamp at the top whenever this file is checked against the code.
