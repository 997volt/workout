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

**A review of everything since 2.11 found these, and the gates caught none of them.** The shared core
(N170–N173) and the helper commits beside it: each row below is a gap between what the changelog,
[DECISIONS.md](DECISIONS.md) or the page's own specification already say and what the code does, so no test
fails when it regresses. The defects go first and each leaves this file as it lands; the rest are planned work
with nothing built, and the rows are in the order they should be built.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| B185 | medium | **"Download edit" is not re-planned, so a stale preview ships a file the core refuses.** The new-program path re-checks before it writes, deliberately — "the fields may have changed since the last check" — while the edit path trusts the last preview's `ok` and writes the working copy as it stands now. Text fields change the model without re-rendering, so the button never returns to disabled: preview a program, type `4000` in Rest, download, and the page still says *Ready* while the app answers `MALFORMED`. | Re-plan on download in edit mode exactly as the new path re-validates, or clear the preview on every model write and let the button's disabled state be the invalidation — the second makes both modes read the same rule and a stale *Ready* impossible. |
| N179 | medium | **The shared core is outside the static analysis every other line of the app passes.** `:shared` applies the multiplatform and serialization plugins only, detekt is applied in `:app` alone, and the `detekt` task therefore builds `:app:detekt` — so the module that decides what an edit writes into the lifter's data is the one nobody lints, and it misses the build's warnings-as-errors posture too. | Apply detekt to `:shared` with the same config, and add `:shared:detekt` to the gate and to CI in the change that drops the duplicated task list — `DocsConsistencyTest` holds AGENTS.md to CI's, so the two move together. Not the Compose ruleset: `:shared` has no Compose. |
| B186 | medium | **One refusal code carries five faults, and the host's sentence is wrong for three of them.** `MALFORMED` answers a rest of 4000 s, a blank program id, a bad `rowKind` and a `deletedAt` on an edit as "The file is not shaped like a program", and the helper ignores `detail` entirely, so all four rung causes print "needs a set above it that stands on its own" — including the case where the anchor is right there. The core's own contract says a problem carries a code and data, never prose, yet the rung causes are prose no host reads. | Split the codes the hosts must word differently (`INVALID_REST` at least) and let each host own one sentence per code, as the KDoc already promises; make the rung refusal codes (`RUNG_WITHOUT_VALUE`, `RUNG_UNDER_WARMUP`, `RUN_MIXES_KINDS`) rather than an English `detail`, so those sentences can exist. |
| B187 | medium | **A movement's name is resolved against the document and the seed catalog, never against the lifter's own library.** The field therefore creates a movement for any name it has not seen, and the plan turns every carried exercise row the device lacks into an addition: type *Cabel Fly* and correct it, or type the exact name of one of the lifter's own movements, and the edit still creates a row. A typo becomes a permanent movement nobody can delete from the page, and a name can exist twice because nothing is unique on it. | Resolve against the library the page already holds — the loaded export, which the shared-workout line beside it already reads — and take a movement's id from there before minting one. Drop an exercise row from the working copy when the row that named it is renamed or removed, so a correction leaves nothing behind and a misspelling creates nothing the lifter did not keep. |
| B188 | medium | **The editor's answers are silent and every button destroys the keyboard's place.** Its status is a plain paragraph where the rest of the page announces through the live region, so "The edit is not ready", every refusal list and every *Ready* passes a screen-reader user by; and the render empties the host on each add, move and remove, so the focused control is gone and focus falls to the body. ↑/↓ are named by glyph alone, *Remove* repeats without its row, and the "Add an exercise" field has only a placeholder — against the page's own "every input labelled … announced through a status region". | Announce through the existing live region, keep the editor's status and preview in live regions of their own, restore focus to the control an action belongs to after a re-render, and name the controls: `aria-label` with the row's name on ↑/↓ and Remove, and a real label on the exercise field. |
| N180 | medium | **The app's half of the edit path is tested only where the gate does not run.** `ProgramsViewModel` has no test, nothing covers route → dialog → confirm, and the write is exercised only by the instrumented suite, which the default gate skips — so a change that breaks the confirmation or the write still shows a green `./gradlew`. | A `ProgramsViewModelTest` over a hand-written fake, the pattern `ProgramStartGateViewModelTest` already uses: an add goes straight through, an edit stops at a pending preview, confirm writes the kept text and reports an update, dismiss writes nothing, a refused preview reports without a dialog. The deleted-row case belongs in the instrumented suite beside the existing stale test. |
| N181 | medium | **The cross-host guarantee is asserted by presence only, and the page's own edit document is tested nowhere.** A fixture case passes when each expected kind merely *appears* in the plan, so a planner reporting an addition it will skip (B183) or a page that over-reports sails through; the page's suite never calls the functions that build, preview or download an edit, so a wrong base, a missing marker or a dropped field leaves it green; a `(cases \|\| [])` fallback lets five fixture assertions vanish if the global is renamed, and nothing counts them; and the format's one compatibility claim — that an ordinary document written through the shared codec carries no `edit` field — has no test at all. | Exact assertions on both sides, an expected-case count in the node runner the way the instrumented check counts its suite, one page test that opens a document, previews and downloads it and asserts the marker's base and the carried fields, and one core test that encodes a document without an edit and asserts the JSON has no such key. |
| B190 | low | **Changing a set's role keeps the drop value it may no longer carry, and wipes the numbers it needs.** The role handler clears the targets a rung cannot carry but not `dropValueGrams`, so DROP→CLUSTER or DROP→NORMAL leaves a drop value on a set with no drop while the field that would show it is hidden; going the other way, NORMAL→DROP nulls the load and reps, so DROP→NORMAL comes back empty. | Decide the row's fields from its role in one place — a role change clears what the new role cannot carry and leaves what it can — rather than patching two fields in a handler. |
| B191 | low | **A program opened from a backup can never say another program shares its workouts.** The line reads the loaded export, and the backup chooser throws its backup away, so the fact sits in the document the lifter just opened and is never shown. | Keep the source document on the editor's state and read the shared-workout line from it, falling back to the export. |
| B192 | low | **Three sentences still describe the flow before the tabs and the editor.** The program sheet's hint ends "change it later in the app" while the editor sits in the same tab; the confirmation's title shows the name the program *will* have, then a separate line says it will be renamed; and a rows file dropped on Programs is answered "not a program document or a backup" rather than being sent to Import. | Say what the tab now does, title the confirmation with the name the list calls the program, and route a rows file to Import's panel instead of describing what it is not. |
| N182 | low | **The gate now needs a Node toolchain CI never installs.** The build job runs `:shared:jsNodeTest` and `:shared:syncSharedJs` while pinning its JDK and its actions, so it depends on whatever node the runner image ships — the one toolchain in the job that is neither declared nor pinned. | Add a pinned `actions/setup-node` and a version floor to the build job, so an image that changes its Node does not fail a job about the app. |
| N183 | low | **The document format version lives twice again.** The core exports `programFormatVersion()` for exactly the reason the constant moved into it — a version that lived in two places is the first thing to disagree — and nothing calls it: the page declares its own `4` and stamps it on a new program and on one taken from a backup. When the format moves, the page writes its new programs at the old version and nothing catches it before the app refuses them. | Call the core's function for both documents and drop the local constant, or delete the export — an API nothing calls is the next deletion, and a second copy of a version is the drift the module was created to end. |
| N184 | low | **The download is copied a third time.** A new blob-and-anchor download joins two already hand-rolled in the page, and the rule is to extract at the second caller. | One download function for all three, with the file name as its argument. |
| N185 | medium | **N174's format decision does not follow its own argument.** That row moves the backup's `schemaVersion` because an older build reading a sequence as weekly "would repeat the block forever — a wrong meaning rather than a dropped extra", and then keeps the program document at format 4, where the same older build drops the kind and repeats the block forever. A program document is the path a block actually arrives by, so it is the wrong meaning that matters most. | Settle it before N174 is built: either the document's format moves with the backup's and its floor with it, or the row says why an imported block may be repeated by a build that cannot read its kind. Recorded as a decision with the row that implements it, as this section already requires. |

The rows that ask a question rather than only a fix — what an edit does with a row the device has deleted, and
what a page may promise before the app plans the same document — settle it in [DECISIONS.md](DECISIONS.md) with
the row that answers it, not here.

**A program can be a finite block, not only a weekly rotation.** A program is a week that repeats: a slot is a
template plus a weekday, that weekday recurs, and every week reads the same templates. A peaking block — nine
sessions, each with its own loads, done on alternating Mondays and Fridays and then over — has nowhere to live:
its nine sessions are nine templates, but nothing can say that the ninth is the last, or that session 4 is due on
*that* Friday rather than every Friday. That is the work after this release, and its shape is agreed: a program says whether it
repeats, and in one that does not, a slot's weekday *places* a single occurrence instead of scheduling a recurring
one. The loads need no new layer, because in a sequence every session is already its own template. Nothing is
built yet, and the rows are in build order.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| P3.21 | medium | **A program can be a finite block, with a beginning and an end.** The app's schedule has one shape: a weekday recurs, so nine distinct sessions cannot be placed on alternating Mondays and Fridays, and a load prescribed for "week 3" has no week to belong to. A block is the ordinary way a peak is written down, and today it can only be done by starting templates by hand from the library, with no due date and no sign of where the block is. | A **program kind** — weekly (today's, and the default) or sequence — and an authored **start date**, so a block's first session has a date to be due on rather than an accident of when the program was created. In a sequence the weekday is a **placement**: the session is due on the first such weekday after the last thing done, and the sequence **ends** instead of wrapping. A sequence is still one program whose slots are templates, so each session's loads are prescribed once and never move. **Progression is untouched**, and it is harmless here for a structural reason: a step accepted on session 3 rewrites only session 3's template, which a finite block never runs again — so RPE keeps earning steps on the weekly program and the block's numbers do not drift. Not planned in this block: a week-indexed plan entity, loads computed by the app, adherence scoring for a sequence, a per-program progression mode, and a `Kind`/`Starts on` column on the helper's sheet, which the app's editor covers until the sheet grows one. |
| N174 | high | **The model says what kind a program is, and when it starts.** Nothing in `programs` distinguishes a schedule that repeats from one that is done, and there is no authored date to hang a block's first due date on: `createdAt` is when the row was written, which is not when the block begins. | `programs.kind` stores `WEEKLY` or `SEQUENCE` **by name**, and `programs.startDate` an epoch day with null meaning the day it was created — both in one migration 7→8, read by the code added beside them. The program document gains both as optional fields and the format stays 4, the way N171's `edit` did; the backup carries both and its `schemaVersion` moves to 6, because an older build reading a sequence as weekly would repeat the block forever — a wrong meaning rather than a dropped extra. No new tables. |
| N175 | high | **The schedule is a derived next session and due date, not a stored plan.** A due date that is written down is N16's dated instance by another route: it goes stale when a session is done late, when a slot moves, or when the block is edited, and every screen would read it. | A pure sequence schedule — next slot, due date, complete — beside `programRun`, over the slots, sessions, skips, start date and today, so the screens and the tests read one function. **Next** is the first slot with nothing done and no skip; **due** is the first matching weekday strictly after the last event, or at or after the start date when nothing has happened yet; a slot with no weekday means the next day, so "whenever" sessions still work. `ProgramRun` gains a finished state rather than wrapping to the first slot. A weekly program answers exactly as it does now. |
| N176 | high | **Home offers a block's next session, and can start it.** With no weekday matching a recurring rule, a sequence's sessions are otherwise reachable only from the library, one template at a time — the run is read in the editor and nothing acts on it (N125). | When the next session is due or overdue, home's plan surface shows it as a card — name, `Day k of N`, its due date — and the card starts it with the slot id it already carries, so the prefill history is the slot's own. Nothing is offered before the start date. **No missed-day question is asked**: nothing is dropped, because the session stays next until it is done or consciously skipped, and asking on the way in is the interrogation P3.3 refused. |
| N177 | medium | **A rest day names what is next, for both kinds.** The rest line says only that today is rest; on a block's off day, or a weekly program's, the useful sentence is which session is coming and when. | The rest line names the next session: a sequence's next slot and its due date, a weekly program's next slot in the run and the weekday it lands on. **Informational only** — N125 removed the startable next-up row on purpose, and naming Friday's workout is not the same as acting on it. |
| N178 | medium | **The editor authors the kind and the start date.** A block arrives by import, so the fields cannot live only in a create dialog; and a sequence slot's weekday picker labelled "weekday" would describe the wrong rule. | A weekly/sequence toggle and **Starts on** (default: the creation date) in the program editor, the weekday picker labelled **Due** for a sequence slot, and `Day k of N` beside the order. An imported program is edited the same way, which is the path a block arrives by; a create-time dialog is not planned. |

None of this is a rule until it is built: the decisions it needs — that a sequence is an order with an end rather
than a dated instance (N16), that a weekday can place one occurrence rather than schedule a recurring one (P3.3,
P3.9), and why the backup's schema version moves — are recorded in [DECISIONS.md](DECISIONS.md) with the row that
implements them.

## Later — decided, and not being started

Agreed work that is not next: the design is settled so it does not have to be re-argued, and nothing here is being
built. It is **not** *Parked* — a parked row is a decision not to plan something, while these are planned and simply
not now — and it is not *Next*, which is what is actually being worked on. Each row names **what brings it forward**,
for the reason a parked row names its revisit trigger: a queue that cannot say why it is waiting is a graveyard.
**It drains like every other section**: a row picked up moves to *Next*, and one that ships leaves for the changelog.

**Nothing.**

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
