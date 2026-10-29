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

Nothing. The whole-codebase review's batch (B268–B291) shipped, and every row of it left this file for
[CHANGELOG.md](CHANGELOG.md) as it landed. The queue below is planned and not started; its first row is the next
thing built.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and
simply not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it
forward**, for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a
graveyard. **It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for
the changelog.

**The order below is the build order**, and it is chosen so each row starts with the least it can depend on: the
library's read-only additions first, then its own data, then its working controls, then the exercise's own screen,
then the schema changes, then the rule that reads history, and last the migrations that need their author's word on
every row. Each row says what it waits for where it waits for anything; a row that says nothing waits for nothing
but the one before it. The numbers quoted here are the measurements the work was planned from, not facts to keep up
to date: a row that ships leaves this file.

### The library as a place you read

Five independent rows, no storage change, each covered by the JVM tests the rules already have.

**N224 — the search finds a lift by how you say it.** Search is substring containment over a movement's name and
the taxonomy it inherits (P1.1a), so *Barbell Bench Press* cannot be found from "bench pr", "bbp" or "ohp". Three
additions, all inside the one pure matcher: **every word of the query must start a word of the name** (so "bench
pr" finds both, in order); **initials match** ("bbp" for Barbell Bench Press); and **a small alias table shipped
with the seed** is searched beside the name ("ohp", "rdl"), because the words a lifter says for a lift are the
seed's to know rather than a row's. Nothing is stored per movement, so a lifter's own movement simply has no alias,
and the taxonomy and family words the search already reaches keep working. Rejected: edit-distance or fuzzy
matching, which needs a threshold this app would then have to defend and finds the wrong lift as often as the right
one.

**N225 — a family the search narrowed says how many it found.** A head's count is the number of children the
*query* matched, so a family holding six movements reads "1 exercise" while a search is on — next to the one row it
found, which is why it reads as a claim rather than an omission. The head carries the family's own size as well and
states both — "1 of 6" — and with no query the two numbers agree so the plain count stays. A folded head keeps its
full count, which is the whole reason a folded family draws one.

**N226 — the next step's dialog says one reason once.** When every prescribed set missed for the same cause the
dialog repeats one sentence per set, three times over. It gains an exercise-wide statement above the sets when one
reason covers all of them, and the per-set rows keep their own detail only where they differ. `NOT_DONE`,
`NOT_IN_PLAN` and `RUNG_OF_A_GROUP` never take part in it: they describe work that is missing or extra rather than
a set that failed the plan, so a session whose plan was never logged would otherwise be summarised as a refusal.
The dialog must not claim the plan is broken either — the plan editor already says a plan that cannot progress and
the reasons why (B181), and it is the screen that can fix it. What the dialog says is what the sets did.

**N227 — the exercise says what the plan will do with the set.** The block's small print carries the taxonomy, the
cue, and the rest prescription when the timer is off — facts worth reading mid-session — and nothing about the step
the plan's own numbers can earn. It gains one line, read from the rule the plan editor prints (`ProgressionRule`'s
cases, shortened): the rep below the range's ceiling, the weight at it, and nothing at all where the plan cannot
progress. It sits **with the small print and never on the prefill's reason line**, because P3.17 settles that line
as where the numbers came from and deliberately not the app's opinion of what to do next (N33); this is the plan's
own rule, drawn as a fact about the exercise the way the rest prescription is. Because both read one rule, the
session and the plan editor cannot promise different steps.

**N228 — a family that changed opens.** The library folds every family on load and remembers the folds (N95, N91),
so filing a movement into a family from its own editor changes a count on a folded row and nothing else. The screen
compares each movement's family with the one it had on the previous composition and opens the family that gained or
lost one, once; the same covers a movement made from the library and one whose category was cleared. No other
family's fold is touched, and the state stays saveable so a rotation keeps it (N84).

### The library's own data

**B292 — a seeded family has taken a movement's id, so *Back Extension* does not exist.** A 2.6 database holds the
seed's *category* under `back-extension`, the id the seed now uses for the **movement**. B148 taught the seeder to
move that row, but the guard asks for one that is still its own parent, and the row on disk has no parent, so the
guard never matches: the device shows **two categories named Back Extension** (the stale one and
`back-extension-family`) and the movement `back-extension` fails to arrive on every start, because `INSERT OR
IGNORE` cannot take a taken id. **The movement therefore cannot be logged at all** — a picker offers movements, and
a category is never named by a set — while a duplicate family is drawn. The repair widens to the shape that is
actually on disk and that no write path can produce: **a row whose id the seed claims for a movement, which is not
that movement** (a category made in the app is minted with a UUID, so a seed slug sitting on the wrong kind of row
is only ever an old seed). Its children are re-filed onto `back-extension-family`, the stale row is **deleted**
rather than soft-deleted — a soft-deleted row keeps its id and would block the movement exactly as it does now —
and the seeder's ordinary insert then lands. The case is built from a 2.6-shaped store in the seeder's own test, so
the guard is proved against the row that caused it rather than the shape B148 imagined.

**B293 — the seed declares no movement bodyweight or timed.** The seed's movement helper takes a name, an equipment
and a taxonomy, and the seeder's insert writes ten columns; `isBodyweight` and `isTimed` are not among them, so
every seeded row takes the table's own default of false. Two shapes are therefore wrong on the first workout. A
**pull-up, dip, push-up or inverted row** is read as an ordinary loaded movement: the field asks for *Weight*
rather than *Added weight*, the bodyweight prompt never opens, and the set snapshots no bodyweight — so a set at
+20 kg is stored, charted and record-checked as 20 kg rather than 95, because N219's load rule cannot run without
the snapshot. And a **plank, Copenhagen plank or farmer's carry** cannot be logged as a hold at all, because the
editor offers reps. The fix is the seed's own: the helper takes both flags, the insert carries both columns, and
each movement declares what it is — which is also what the plyometric rows need (N241), so it lands before them.
**An existing device needs a guarded repair**, in the shape B148 and B292 established: filled only where the row
still looks like the seed's, idempotent, and never over a row the lifter has edited, because the two flags are
theirs to set.

**N229 — every seeded movement is filed, or the seed says why it cannot be.** Eight of the seed's movements sit at
the top level with no family, which is why a fresh install draws a flat alphabetical pile of loose lifts under its
families: a shape that reads as the library's leftovers rather than as anything the lifter chose. *Face Pull* fits
**Rear Delt** as it stands and *Dip* fits **Triceps Extension**, which already holds Bench Dip and Diamond Push-Up.
Three new families are needed: one for the hamstring curls (*Lying Leg Curl*, *Seated Leg Curl*, *Nordic Curl*),
one for *Leg Extension*, which is quad isolation that *Squat* would name wrongly, and one for the upright rows that
N239 adds — four implements of one lift, with no family to hold them today. Each new family states its own pattern,
primary muscle and secondary list, because a family is where its members read them. The other two are decided
rather than filed: **Push-Up stays at the top level**, because every press family is a *lift's* family and a
push-up is none of them — one seeded movement is loose on purpose, which is a different thing from eight of them
being so — and the plain **Deadlift is merged away** into *Conventional Deadlift* (N238), so the collision of a
family and a child sharing one name goes rather than being renamed around. **An existing device needs a guarded
re-file as well**: `INSERT OR IGNORE` never updates a row, so a movement already on the device keeps its null
family until a repair moves it — only where it still looks like the seed's, idempotent, in the shape B148
established. Rejected: a synthetic *Unfiled* head in the library, which would give the app's own leftovers a name
and a count, and still leave the rows unfiled.

**N240 — the seeded taxonomy is audited, and corrected where it is owned.** Every seeded movement's effective
primary and secondary muscles are reviewed against what the movement trains, and each finding is fixed at the level
that owns it: a **family** states what all of its members share, and a member that differs states its own (N103,
N110 — read live, never copied). The inspection starts from a real gap: the *Row* family names upper back and
biceps and **not the lats**, which is half of what a row is trained for, while the *Lat Pulldown* family names lats
and biceps and not the upper back that does the retracting. Two consequences have to be stated rather than
discovered. **A family's correction moves every member that states nothing**, because the walk is live — which is
why a wrong fact belongs at the level that owns it, and a member's exception belongs on the member. And **the
correction is retroactive**: the balance figure a program shows (N119, N122) is computed from effective muscles in
half-sets, so a corrected family reshapes the numbers of plans already authored. That is what a correction is for,
and it is why this is a review with the author rather than a mechanical pass. It lands after N229, which creates
the families it reviews. Existing devices need the same guarded update, for B148's reason: `INSERT OR IGNORE` never
rewrites a row.

**N241 — plyometric movements are part of the seed.** The seed ships strength and bodybuilding work and no jump,
hop or throw, so a program that includes plyometrics authors every one of them by hand — the same gap the seed's
own note records for the Olympic lifts, and for a related reason: **a family states one movement pattern and one
primary muscle**, and a box jump has an honest claim on the squat pattern, on the calves, the quads and the glutes.
Three things are decided here, in this order. **What ships**: a box jump, a jump squat, a broad jump, a depth jump,
a pogo hop, a bound or skater and a medicine-ball throw, with the equipment each needs. **How they are filed**: one
*Plyometric* family whose stated pattern and primary muscle are the decision, or one family per pattern where the
members disagree — and they can be filed at all only once B293 makes a bodyweight row true, because a plyometric
movement that is not marked bodyweight is handled as a loaded one. **What progression does with them**: a jump
carries no added load, so the offer gives reps below its ceiling and says *topped out* at it (N74) — honest, and it
means a plyometric plan climbs by reps and then stops, unless the jump is a loaded one, where the ordinary rule
applies. The movement pattern enum has no value for it — press, pull, squat, hinge, lunge, carry, isolation, core,
other — so the family either states *other* or the enum gains one. Adding it is additive and safe for the document
format, which carries a pattern as a string rather than an enum, but the pickers and the generated catalog see it,
so it is named here rather than slipped in.

### Bringing a program in

**N243 — a refused program load offers to arrive under another name.** A program document is refused when its id is
already here and the plan under it is not the same one: a pasted table's id is a slug of its name, so a second
*Push Pull Legs* table carries the first one's id, and the app answers *A different plan is already here under that
name, so nothing was added* (B267). The sentence is right, and the lifter then has nowhere to go inside the app —
the ways out are renaming the program already on the device or editing the file by hand. The load becomes **read,
ask, write**: the document preview gains the case it is missing — the id is held **and the work differs** — and the
screen answers it with a dialog that names the clash and offers **Load it under another name**, pre-filled with a
name the list does not hold ("Push Pull Legs (2)"), with *Cancel* as the other answer and a typed name refused
while another program holds it, because telling two programs apart is the point. Confirming writes a **copy**: a
minted id and the typed name for the program, and **new ids for its templates, planned exercises and sets**,
because a program that shares its days with the original is not a separate one — editing one day would rewrite
both, and N171's staleness check would then refuse documents for the other. **Exercise ids are kept**, because
those name library movements rather than copying them. The copy arrives **inactive and last**, like every add (N47,
P3.3), and it is faithful in everything else, a block's start included, which the lifter can edit afterwards
(N186). The original is never touched, and the rename lives in the app rather than in the file, so the document on
disk keeps its id and its name. Rejected: **sharing the original's templates**, which N73 allows on purpose for two
programs that *mean* one workout and which is exactly wrong for a copy whose point is to diverge; **renaming the
program already on the device**, which would take the identity of the plan that is there and trained; and sending
the lifter back to the helper to mint a new id, because the file is what they have. The same copy write is what a
*Duplicate* entry in the programs list would need, which is not part of this row — it is where the helper would be
extracted, at its second caller.

### The library as a place you work

**N230 — the library can be sorted, and says when a lift was last trained.** Order is alphabetical by family and
then by movement, and nothing offers another order. A sort control adds *Last trained*, backed by one new read: the
newest finished session that names each movement — the trends read already answers this for the lifts it was asked
about, but only for those ids, so the library needs its own grouped query. A family takes its most recently trained
member, and a lift never trained sorts last rather than first, so the answer to "what have I not done" is a list
you can read from the bottom. The default stays alphabetical, so a lifter who never opens the control sees the
library they have. This read is what N231's chip and N233's section need too, which is why it lands first.

**N231 — filters narrow the library, and compose with the search.** A row of chips above the list: *All*,
*Not trained in 30 days*, *Bodyweight*, *Timed*, *Unfiled*. Each carries its count from the whole library, an empty
selection falls back to *All* rather than drawing a blank list under a selected chip, and **the search applies
inside the chosen chip** — the two narrow the same list rather than replacing one another, so "bench" inside
*Bodyweight* is the question a lifter actually has. The chip states the window it means ("Not trained in 30 days")
rather than being called *Stale*, and *Unfiled* is the one group an earlier row (N229) shrinks rather than empties.
Rejected: the templates list's rule, where a mode draws its group *instead of* the live list (N217) — a filter and
a search answer the same question, and a list that replaced itself would make the other one disappear without
saying so.

**N232 — a movement can be made from the library.** Making a movement lives in the workout picker (N2, B94) because
the gap is felt mid-session, and the library only makes families — so planning a lift you have never trained, which
is the one place you would want it, forces a workout first. The library's action gains *New movement*, reusing the
picker's dialog, and when the query found nothing the action offers to create exactly what was typed: "no exercises
match *front squat*" becomes one tap. The dialog also takes an optional family, because an unfiled movement is the
shape N229 exists to clean up and creating more of them in silence is the wrong direction. Rejected: leaving
creation to the picker alone, which is a rule about where the gap is *felt* and not a claim that it is never felt
while planning.

### The exercise's own screen

**N233 — the screen says when the lift was last trained and what its best is.** `personalRecords(exerciseId, null)`
returns the heaviest working set at each rep count, and its own comment says the `null` is "what a records view
wants" — that view was never built, and the read serves only the record check on the workout screen. The exercise's
screen gains the section: the last session's date with the working sets it holds, and the best set at each rep
count (or the longest hold, for a timed movement), with the trends button it already has left where it is.
`previousPerformance` already answers the first half for the prefill, so this is a screen and not a query. **No
schema change.**

**N234 — the workout's ⋮ opens the exercise, which reverses N121.** The shared menu already carries an optional
*Open exercise* entry and the template editor supplies it; the live workout withholds it on purpose, with a test
that asserts its absence. The reversal's argument is that the alternative was left unstated: to look at a lift
mid-session you switch to Library, find the movement, open it and switch back — a larger interruption than two
deliberate taps in an overflow that sits at the block's corner, away from the set fields; N53's order is unchanged,
so the entry stays first among the optional ones. Opening a destination **pushes**, so the session's ViewModel, its
running rest timer and the draft in the next set's fields are untouched and one back tap returns to them. Waits for
N233: a look is worth the trip once the screen says something about the lift. The absence test becomes a presence
test.

### A plan's progression, stored

**N235 — a movement can answer *Done* its own way.** The progression mode is one device preference for the whole
app (*Ask*, *Apply at Done*, *Off*), so a 5/3/1 squat and a movement being gently worked back are told the same
thing. The movement gains its own answer, **null meaning *follow the app setting***, drawn in its editor as a
four-way choice; the settings control keeps its three and its hint says it is the default. It is stored on the
movement rather than in the settings, because **it decides what the app writes into a plan**, and this project's
rule is that a setting is a device preference while a metric target is training data: a restore must not silently
return a movement to *Ask*. Schema 13, a nullable column read by name, with the exported schema as the contract.
The backup carries it as a defaulted field, so the transfer format's version does not move. A **program document
does not carry it** — a document describes a plan, while this is the receiving library's view of a movement, the
same split `hiddenAt` follows. The Done flow resolves it in the one place it reads the mode, and the plan editor's
sentence gains a clause when the movement's answer overrides the setting, because otherwise it promises a question
that will not be asked. Rejected: a column on a plan's exercise, which P3.17 already refused as a second place to
say one thing.

**N236 — a step that was offered and not taken is recorded.** *Not now* drops the question and writes nothing, so a
week later History cannot say a step was there to take — the offer is frozen from the plan as it was, and the plan
has moved since, so it cannot be recomputed. The session exercise records what was offered and declined: the
directions, written when the lifter declines, drawn on the workout's review and on History's detail as *step
offered, not taken*. Schema 14. **A declined step is not a miss**: the deload rule (N237) counts the sets that
failed the plan, not the answer the lifter gave, and a lift that declined a step every week is not a lift that is
stalling.

**N242 — a plan can prescribe a hold, and a hold can progress.** A timed set already works: it records seconds,
writes no reps, keeps its load, and the editor offers both (N220). Two things still do not, and N220 wrote down
why. **A plan prescribes reps**, so a hold inside a plan ignores its target and a session saved as a plan writes no
rep target at all (B244); and **the next-step question answers a hold with "a hold is not measured in reps"**, so a
timed movement never progresses — which is why a 45-second carry counts toward neither volume nor a record today. A
lifter who trains carries and planks for time therefore has a logged history and no prescription, which is the one
thing this app is for. The change: a plan's set gains an optional **duration** beside its reps, one or the other
and never both, which is the shape the write boundary already refuses on a performed set; the session's prefill
reads it, so a planned 45-second carry opens on 45; the offer gains a **third direction** — seconds, moved by the
movement's own step — under the same gate as the other two, so a hold answered at or under the plan's effort earns
its next step and nothing is written until it is accepted; and the plan editor's sentence states it. A hold earns
seconds or the set earns reps, never both, which is the exclusivity the rep and weight directions already have
(N74). Rejected: a rep target with a note saying "for time", which is what a lifter does today and is the reason
the time is not in the plan.

### A plan's progression, learned

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
because a row merged away is a row not worth naming.

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
