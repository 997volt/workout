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

**The review of what shipped after 2.10.** Reading the work that landed since the 2.10 release found
behaviours the shipped claims do not keep, plus two clean-up passes. Nothing here is fixed yet, and each row
names where the claim and the code disagree.

| # | Severity | What | Fix |
| --- | --- | --- | --- |
| B177 | high | **A deleted example program cannot come back.** An example is loaded through the program-document merge, which ignores an id that already exists, and deleting a program only soft-deletes it. The empty Programs screen therefore offers the card again, and tapping it answers "That program is already here." while the list stays empty — contradicting the changelog draft, which promises the card restores it. The same dead end applies to *Load* of any document whose program was deleted on the device. Exercises already have the opposite, deliberate rule (B51: a deleted row is not resurrected), so this is an assumption rather than a decision. | Decide which rule programs follow: restore a document's soft-deleted rows on import, or keep insert-only and stop promising the card restores — naming the templates that survive and the way back. Then make the changelog sentence true either way. |
| B178 | high | **A Waistline import can leave two live DAILY entries on one day.** The helper's id is a hash of the day and kind, and the document merge inserts measurements by id alone, while the app's own rule is one entry of each kind per day, enforced at write time by `findOnDay`. A day the lifter already recorded by hand carries a UUID id, so the import adds a second live row for it; every read lists both and every chart plots both — the state B151 was written to prevent. The claim that the helper's id is "the app's own identity for a measurement" is false. | Make an existing day win: dedupe by (local day, kind), in the helper against the loaded export and at the app's import boundary. Correct DECISIONS.md and IMPORT-HELPER.md. |
| B180 | medium | **A program document is identified by its display name, so a same-named sheet edits an existing program.** The document's program id is a hash of the name. A sheet whose day was added or renamed keeps that id, so the program row is ignored while the new day's template and slot are inserted into whatever program already holds it — an edit, which "creates and never edits" refuses. The preview's duplicate warning says the import "will add a second one", which is the reverse: a same-named program created *in the app* does become a second one, while a sheet's does not. | Give a document an identity that is not its display name (or ask on a clash), and make the warning state what will happen: a new program, merged days, or a refusal. |
| B181 | medium | **The plan's progression line states a rule *Done* does not follow.** `ProgressionStepNotice` prints only the weight step, but below a rep ceiling the offer is +1 rep; its no-weight branch claims no progression while rep steps remain; and it treats an assisted set (`targetWeightGrams = 0`) as having a load where the domain's `addedWeightGrams` says it does not. The changelog and DECISIONS.md repeat the same premise. | Word the rule the prompt actually applies — reps below the ceiling, the movement's own step at it, and which half is missing — compute "has a load" the way `addedWeightGrams` does, and correct the two docs. |
| N168 | low | **The review's shape cleanups.** `SettingSwitch`'s KDoc now sits above the new mode composable and the new one has two (`SettingsScreen.kt`); `PlanDialogs.kt`'s file comment became a second doc on `SaveAsPlanDialog`; `TestTags.SETTINGS_PROGRESSION` has no caller; `MAX_QUICK_SETS` is 20 in two files; `ProgressionSection` and `SetEditorHost` are pass-throughs with one caller each; `addToTemplate` drops its `addSet` and history failures without a word; and the why-line can credit a rung's reps to the last set overall, which is not what prefilled them. | One pass over those files: the doc comments, the dead constant, one home for the limit, the wrappers gone or carrying a line of logic, the failures reported the way `onAddSets` reports them, and the reason line naming `sameSetLastTime` when that is what it read. |
| N169 | low | **The review's copy and fixture nits.** `SeedCatalogTest` says 120 rows where the file holds 126; `ProgramsViewModel`'s `Dispatchers` import is out of order; the three settings chips sit in a plain Row and cannot wrap at a large font; `progressSummary` says "no set at 12 reps or fewer" for a lift with no added weight at all; `fixtures/program-document.json` is hand-made where the seed and progress fixtures are generated and compared; and one test's name promises a "keeps a pick" case it does not exercise. | Correct the counts and the sentence, use the wrapping row the weekday chips already use, generate the program fixture from the sheet, and either split the test or rename it. |

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
