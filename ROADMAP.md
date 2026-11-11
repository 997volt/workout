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

## Next — the 2.28 review

The twelve commits after 2.28 were read against the decisions they claim to follow (N294, N299, N300,
N301 and B358, B395–B398), and the rows below are what that read found. They are ordered by what they
cost, the last section is the coverage that let them through plus one scope question, and the review
itself changed no code.

### The rest of the review

- **B410 — The PBKDF2 work factor is below the figure it cites.** `ITERATIONS = 210_000` is documented
  as OWASP's floor for PBKDF2-HMAC-SHA256
  ([EncryptedBackup.kt](app/src/main/java/com/example/androidapp/data/transfer/EncryptedBackup.kt)), but
  that table puts HMAC-SHA256 at 600,000 and 210,000 is its SHA-512 figure. The envelope carries the
  count and the reader accepts a range, so raising it is backward-compatible. Fixed means 600,000,
  measured on a phone, or a corrected comment — not both a wrong note and the cheaper number.

- **B411 — Detecting an encrypted file parses the whole backup on the main thread.** `isEncrypted`
  builds a full `JsonElement` tree of the file just to read one field
  ([EncryptedBackup.kt](app/src/main/java/com/example/androidapp/data/transfer/EncryptedBackup.kt)) and
  is called from the import coroutine on Main
  ([DataTransferViewModel.kt](app/src/main/java/com/example/androidapp/ui/transfer/DataTransferViewModel.kt)),
  so every **plain** import pays a second full parse and a stall that grows with the backup. Fixed means
  a cheap prefix or lookup, or the check off the main thread.

- **B412 — The encrypted export can lose its password while the file picker is open.**
  `pendingExportPassword` is composition-held
  ([DataTransferActions.kt](app/src/main/java/com/example/androidapp/ui/transfer/DataTransferActions.kt)),
  and the activity handles no configuration change itself, so a rotation while the document picker is
  in front recreates the screen, the callback sees a null password and returns silently — a file was
  chosen and nothing was written or said. Fixed means holding the password in the ViewModel that owns
  the write, or reporting the cancellation.

- **B413 — The password prompt does not survive a rotation.** The dialog's two fields and the export
  prompt's open flag are plain `remember`
  ([PasswordPromptDialog.kt](app/src/main/java/com/example/androidapp/ui/transfer/PasswordPromptDialog.kt),
  [SettingsTransferDialogs.kt](app/src/main/java/com/example/androidapp/ui/settings/SettingsTransferDialogs.kt)):
  an import reopens with empty fields, an export prompt closes and drops what was typed. The precedent
  is the bodyweight prompt held in its ViewModel (B283); `rememberSaveable` is the wrong fix because it
  would put a password in the saved state.

- **B414 — Reset is not guarded while it writes.** The baseline's button takes no `enabled` and neither
  reset action checks `writing`, though the switch does
  ([ProgramProgressionScreen.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramProgressionScreen.kt),
  [ProgramProgressionViewModel.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramProgressionViewModel.kt)),
  so a second confirm runs a concurrent reset and the first completion clears the flag while the second
  is in flight. Fixed means disabling the button and returning early from both actions.

- **B415 — The recovery screen's "this week" is frozen at construction.** `thisWeek`/`nextWeek` are
  constructor values ([ProgramWeeksViewModel.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramWeeksViewModel.kt)),
  so a screen kept across Sunday→Monday labels the previous week "This week" and exempts it. The balance
  screen solved the same shape with a resume read (N286). Fixed means recomputing the pair on resume.

- **B416 — A dead test tag.** `TestTags.Programs.DELOAD` has had no reference since the editor's menu
  moved to `WEEKS_MENU`
  ([ProgramsTags.kt](app/src/main/java/com/example/androidapp/ui/components/ProgramsTags.kt)); the rule
  is to delete an API the moment nothing calls it. Its comment still names a bar entry that no longer
  exists.

- **B417 — The preflight tells the lifter to fix plans that are not broken.** The "Nothing in this
  program can progress yet. Fix the plans below." line is gated only on `willProgress` being empty
  ([ProgramProgressionScreen.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramProgressionScreen.kt)),
  so a program whose movements all answer for themselves — `blocked` empty, only "Answers for
  themselves" listed — is told to fix a list that has nothing wrong in it.
  `AutoProgressionPreflight.hasNothing` models the distinction but has no production caller. Fixed means
  branching on `blocked` versus `overridden`, a sentence for the all-overridden state, and either using
  `hasNothing` or deleting it.

- **B418 — Three sentences still describe the pre-N300 shape.** `ProgramSchedule.monthAdherence` still
  says the missed-day question is unaffected
  ([ProgramSchedule.kt](app/src/main/java/com/example/androidapp/domain/model/ProgramSchedule.kt))
  although a marked program is now dropped from it; the creator's intro still describes counted sets
  only and omits the 6.0 effort
  ([strings.xml](app/src/main/res/values/strings.xml)); and the Adherence unmark dialog still says
  "Unmark this deload week?" for the renamed exemption. Fixed means correcting all three.

- **B419 — A logged step's *before* is not checked against the plan.** `requireWriteMatches` compares
  only `after` to the edit
  ([RoomTemplateRepository.kt](app/src/main/java/com/example/androidapp/data/RoomTemplateRepository.kt)),
  so a caller racing the plan could record a `before` that never was and the undo would restore it.
  Fixed means comparing both targets against the row read inside the transaction.

- **B420 — Two small consistency nits the review found.** `ProgramWeeksRoute` omits the `modifier`
  parameter every sibling route carries
  ([ProgramWeeksScreen.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramWeeksScreen.kt)),
  and the cadence card's spacing differs from the other settings cards — no top padding on the hint and
  no row gap when the four chips wrap
  ([DeloadSuggestionSection.kt](app/src/main/java/com/example/androidapp/ui/settings/DeloadSuggestionSection.kt)).

### Coverage and one open question

- **N302 — The tests the review found missing.** `ProgramProgressionViewModel` has no unit test, although
  every sibling program ViewModel does, so the consent-before-enable path and the reset are only exercised
  through hand-built screen state (the recovery screen's own wiring now has a
  [test](app/src/test/java/com/example/androidapp/ui/programs/ProgramWeeksViewModelTest.kt)). The
  program-document half of "importing a program never starts writing" — the
  switch dropped by the `:shared` mapping
  ([SharedProgramDocument.kt](app/src/main/java/com/example/androidapp/data/transfer/SharedProgramDocument.kt)) —
  is untested. And `PlanEntriesTest.substitutingBackRestoresThePlan` builds a row whose origin is already
  cleared ([PlanEntriesTest.kt](app/src/test/java/com/example/androidapp/ui/workout/PlanEntriesTest.kt)),
  so the clearing it names, which lives in `RoomWorkoutRepository`, has no test at all. Fixed means those
  three tests, not a coverage number.

- **N303 — Decide whether a block may be auto-adjusted.** The auto switch is offered for a sequence
  ([ProgramEditorScreen.kt](app/src/main/java/com/example/androidapp/ui/programs/ProgramEditorScreen.kt))
  although the row it ships from scoped auto progression to a followed **weekly** program, and a block is
  exactly what N300 and B398 kept out of the calendar action. Nothing in the implementation is weekly-only
  — resolution is by template, so a block's sessions do get adjusted — which makes this a decision rather
  than a bug. Fixed means either a sentence in [DECISIONS.md](DECISIONS.md) saying why a block may be
  auto, or the same `isSequence` guard the recovery week already takes.

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
