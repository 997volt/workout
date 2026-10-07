# Workout — Roadmap

> **v1.15** is shipped. Last reviewed against the code: 2026-10-07.
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

**The defects a review of the unreleased N80–N86 batch found**, each with the rule it holds. They carry
`B#` ids because they are defects rather than scope, and they stand here because that batch has not
shipped yet: what it does is recorded in [CHANGELOG.md](CHANGELOG.md) under *Unreleased*, and these are
what stand between it and a release. The two that change behaviour come first, because each is the code
doing something other than the decision it cites and each needs that decision settled before the fix;
the rest are independent of them and of each other, so they can be taken in any order — the prose and
test ones touch no production code at all.

- **B73 — a next-up row's substitute advances the run, and its *Restore* entry does nothing.**
  [HomeActions.kt](app/src/main/java/com/example/androidapp/ui/home/HomeActions.kt) starts the picked
  template carrying the slot and records nothing (N85), and the entry's whole argument is that the run then
  stays put — "there was no occurrence for the substitution to be *in*". It does not stay put: the session
  stores only its `templateId`, and
  [ProgramRun.kt](app/src/main/java/com/example/androidapp/domain/model/ProgramRun.kt) derives the run from
  finished sessions by template
  ([ProgramSchedule.kt](app/src/main/java/com/example/androidapp/domain/model/ProgramSchedule.kt)), so a pick
  that names another slot of the same program resolves to that slot and moves the cursor past it. The
  derivation says so: order-only A→B→C with the run at A and a finished B lands on C, and weekday
  Mon/Wed/Fri with the run at Mon and a finished Wed lands on Fri — only a template **outside** the program
  leaves the run at A, which is the one case that is not the archetypal use. Either record enough to key the
  occurrence, exclude the session from the run's read, or rewrite the decision and the changelog entry to own
  the behaviour. The same feature's other half: the picker's *Restore the scheduled workout* routes to the
  same `null` pick, which the `templateId != null` guard in the same file drops without starting anything —
  and on a next-up row there was no substitution to restore, so the entry does nothing at all. Which fix
  that half takes depends on the answer above: hide the entry where nothing can be restored, or make it
  start the run's own slot. The one N85 test is screen plumbing and cannot see the run, so neither half is
  guarded. The decision itself lives only in that KDoc and the changelog — [DECISIONS.md](DECISIONS.md) has
  no N85 entry — so whichever way it is settled, the rule belongs there rather than in a comment.
- **B74 — a deleted plan keeps naming the workout in the list and stops naming it in the detail.**
  [WorkoutDetailViewModel.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailViewModel.kt)
  reads the title's name through `observeTemplate`, whose query filters `deletedAt IS NULL`
  ([TemplateDao.kt](app/src/main/java/com/example/androidapp/data/local/TemplateDao.kt)), while the history
  list's join deliberately does not
  ([WorkoutDao.kt](app/src/main/java/com/example/androidapp/data/local/WorkoutDao.kt)) because
  [DECISIONS.md](DECISIONS.md) settles that a soft-deleted template "still names the workout it was" (N58).
  So once a plan is deleted the list row keeps the name and the detail falls back to its date — exactly the
  disagreement N58 set out to remove. The new comment asserts the code does the opposite of what it does,
  and so do the `templateId` KDoc and the changelog's "as N58 requires". Read the name without the deleted
  filter — not by changing `observeTemplate`, which the editor relies on — or amend the rule; either way the
  read is untested, since the ViewModel test fakes `observeTemplate` as `flowOf(null)` and the screen test
  injects the name, so only the fallback is covered.
- **B75 — the prose the batch left claiming what it had made false.** Two production KDocs still say
  `primary` fills "the Start pill"
  ([Color.kt](app/src/main/java/com/example/androidapp/ui/theme/Color.kt),
  [AppTextButton.kt](app/src/main/java/com/example/androidapp/ui/components/AppTextButton.kt)), which N83
  ended: the planned starts draw `secondaryContainer`, the empty start draws `primaryContainer`, and
  *Resume* is the only control left carrying the accent. [DECISIONS.md](DECISIONS.md)'s N49 entry, edited by
  the same change, names "the workout screen's *Resume*" where *Resume* is on home — and says "home's start
  bar" ten lines later — and broke its paragraph with a stray short line.
  [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md) still calls N74–N79 "the unreleased … batch" although they
  shipped in 1.15 and the identical phrase was removed from this file.
- **B76 — a planned exercise's plan controls share one tag across rows.**
  [TemplateEditorScreen.kt](app/src/main/java/com/example/androidapp/ui/templates/TemplateEditorScreen.kt)
  applies the single constants `TEMPLATE_PLAN_ADD` and `TEMPLATE_PLAN_EMPTY` once per exercise block, while
  the controls the same change added are per-id (`templatePlanSet`, `templatePlanRemove`,
  `templateAddWarmUps` in [TestTags.kt](app/src/main/java/com/example/androidapp/ui/components/TestTags.kt)).
  Two planned exercises are therefore two nodes under one tag, which is why the tests have to narrow to a
  single exercise to address either control. Give both an id, the shape the sibling tags already use.
- **B77 — the guards the review found weakened or missing.**
  [TemplateEditorScreenTest.kt](app/src/test/java/com/example/androidapp/ui/templates/TemplateEditorScreenTest.kt)
  re-homes B6's half-point guard to `assertExists`, where the dialog test it replaced asserted
  `assertIsDisplayed`, so a line composed but clipped below the fold now passes; a third deleted test — the
  whole-RPE trailing zero — was not re-homed at all, though the format is still covered at unit level. The
  warm-up entry's negative test opens the ⋮ and asserts the entry absent with no anchor that the menu
  opened, so a menu that failed to render passes it. The "reads on the block" tests mix `assertExists` with
  `assertIsDisplayed` where the names claim what a lifter reads.
  [PaletteContrastTest.kt](app/src/test/java/com/example/androidapp/ui/theme/PaletteContrastTest.kt)'s new
  N83 scan matches the literal anywhere in the file rather than at the control, and checks `containerColor`
  without the `contentColor` it is paired with.
  [WorkoutDetailScreenTest.kt](app/src/test/java/com/example/androidapp/ui/history/WorkoutDetailScreenTest.kt)
  addresses the set delete by the English "Delete set" in the change that otherwise replaced such a literal
  with a tag, and the empty-workout test's name claims an order it never asserts.
- **B78 — the batch's small cleanups.**
  [WorkoutDetailScreen.kt](app/src/main/java/com/example/androidapp/ui/history/WorkoutDetailScreen.kt) has a
  line of trailing whitespace, against [.editorconfig](.editorconfig)'s `trim_trailing_whitespace`;
  [HomeActions.kt](app/src/main/java/com/example/androidapp/ui/home/HomeActions.kt) runs two declarations
  together with no blank line between them; and
  [TemplatePlanDialogs.kt](app/src/main/java/com/example/androidapp/ui/components/TemplateSetDialog.kt)
  still bears the name of the composable N81 deleted, while holding only the set dialog.

## Later (still self-contained)

**Nothing.** Everything that stood here has shipped — the defects found in use, the workout screen's
discard, the workouts tab cut back, repeat-last in History, Settings' data section and rest-timer switch,
a rest of zero, the planned-set prefill, the program document, the eight defects a review of that batch
found and closed (B51-B58), and the seven requests that were its last queue (N80-N86) — each with its
entry in [CHANGELOG.md](CHANGELOG.md). A candidate graduates there — gaining an id and a spelled-out
decision — when it is picked up, so this queue is where unplanned work waits, and *Parked* below is where
deliberate non-work lives.

The last two rounds of deferred scope — P3.3's and P3.5's — are built as P3.8-P3.16, and what
they named that is not a feature is a settled decision: no dated instances (N16), nothing
automatic (the app states what happened; it never writes what it decided), a weekday-less slot that is never missed
and is order-only, and more than one active program, which P3.12 allowed.

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
