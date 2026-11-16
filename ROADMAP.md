# Workout — Roadmap

> **What is planned, not what happened.** *Next* is the ordered batch, *Later* is decided and waiting, *Parked*
> is decided against until a named trigger fires, and the non-goals are lines this app does not cross.
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

## Next — ordered batch

Findings from the whole-app review, in fix order. The rows that change what the app records or
draws lead; the test-count gate follows them, because it is what would let a later regression
disappear unnoticed. Each row is one commit (or a small batch) with its id in the subject, and a
row leaves this section for [CHANGELOG.md](CHANGELOG.md) when it ships.

1. **B454 — An unpaired rung is not an extra working set.** The earned-set card counts a second
   drop or cluster rung as the plan's missing set, and its *Add* appends a `NORMAL` set at the
   rung's load. Count only unpaired non-rung sets; give extra rungs their own rule if they should
   ever earn volume. Cover the two-session extra-rung case with a test.

2. **B455 — The daily measurement shortcut must not reopen its own form.** Saving a new daily
   entry from home reopens the edit dialog because the form's initial edit is keyed to the live
   day entry; make the initial form a one-shot. Pin it with a test that changes the served entry
   from none to the saved row.

3. **B456 — "Delete everything" must not lose its answer to a rotation.** The clear runs in the
   composition's coroutine scope, so a configuration change cancels the sentence and can leave
   the automatic-backup switch off. Move it to the ViewModel's scope, publish a one-shot outcome,
   and disable the action while it runs.

4. **B457 — A counted set against a hold plan is a missed hold.** It currently reports nothing
   measurable, so three such sessions reset the stall run instead of raising the deload. Read a
   missing duration as the hold miss, the same answer the progression offer gives, and pin the
   three-session case.

5. **B458 — A slot before its program existed is not an occurrence.** The streak uses one `since`
   for every active program, so a newly followed program's earlier weekday can break an existing
   run. Bound occurrences by each program's own creation, and test two programs started weeks
   apart.

6. **N315 — The test-count gate must see a deleted test.** The checker compares only names that
   still exist, so removing a test file leaves the declared and executed totals equal and green.
   Pin an absolute count or otherwise fail when the tested inventory shrinks; prove it by deleting
   a test file in a scratch checkout.

7. **B459 — The weekly review must use one week.** Its totals take each session's own zone, while
   "what moved" takes the device's week; a session near a boundary can count in one and not the
   other. Bucket the movement read per session zone, or settle the review on the device week in
   both and record why.

8. **B460 — A failed statistics read must not draw as empty.** A failed per-lift series or library
   read is flattened to an empty list, so a trained lift draws an empty chart and the picker goes
   blank with no error. Carry the failure into the screen's state like the weekly advisories do.

9. **B461 — Template quick-add is not single-flight.** Two taps inside the write append the block
   twice. Guard it the way the workout log, plan editor and volume Add already are, and test two
   clicks.

10. **B462 — Workout detail must acknowledge a repeated failure.** The snackbar is keyed to the
    message and the error is cleared only by a success, so a second storage failure with the same
    sentence is silent. Clear the error after showing it, as the other screens do.

11. **B463 — A reorder is one read and one swap.** The read and the two-position swap are not one
    transaction or one guarded action, so two quick taps can leave two rows on one position. Wrap
    the whole read-and-swap in a transaction or serialize it.

12. **B464 — Adherence day cells must name their action.** A scheduled cell is a button that opens
    the correction dialog, but its description only states the day. Add the action label, and keep
    rest days non-actionable.

13. **B465 — The custom-range dialog must survive rotation.** Its open flag and both picked dates
    are plain `remember`; make them saveable like the other open dialogs.

14. **N316 — The deload confirm's numbers need a test.** The test named for them passes no kept
    set and asserts only that the dialog exists. Assert one set's before → after (weight,
    assistance or hold) and that confirming is the only write.

15. **N317 — The find-the-weight guard needs the real store.** Its idempotence is implemented by
    the ViewModel test's fake, so the production condition can be deleted green. Save twice
    against a real repository with a weight entered in between, and assert it survives.

16. **N318 — Exercise the new writes' failure paths.** Deload-in-place, replace and the backup
    merge are transactional, but nothing forces a failure after their first write; the screens'
    replace, editor and earned-set write failures are also unreachable from their fakes. Add the
    trigger-based rollback test the joint-rating path already has, and a failure return per branch
    that asserts the message is shown and the card or sheet stays usable.

17. **N319 — Let the new fakes assert what they are told.** The replace fake ignores `seeds` and
    `empty`, the shared-plan fake ignores the template and program arguments, and no test plans
    the sharing query itself; N311's timed branch has no test at all. Assert the arguments, plan
    the query against two programs, and cover an earned hold.

18. **N320 — Correct the two stale statements.** README's quoted release size no longer matches
    the built artifact, and `RestTimer` still says its default is a constant until a settings
    screen exists. Drop or derive the size, and update the comment.

19. **N321 — Sweep the remaining low findings.** `IndexUsageTest` plans hand-written SQL rather
    than the DAO's own queries; `MeasurementDayStateTest` reads the real clock; `remainingSeconds`
    can wrap a huge duration into a negative `Int`; and `recordBackupResult` permits success and
    failure at once. Fix or explicitly reject each.

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

Everything that has **shipped** has its entry in [CHANGELOG.md](CHANGELOG.md). That is the whole of the
record: this file does not narrate it.
