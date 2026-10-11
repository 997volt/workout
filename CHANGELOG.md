# Changelog

Notable changes to Workout, newest first. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions are the
`versionName` from [`version.properties`](version.properties), with the
`versionCode` in brackets because that is what Android actually compares.

Each entry says what shipped and why it mattered. The reasoning behind a decision — the
alternatives rejected, the measurements, the argument — lives in
[DECISIONS.md](DECISIONS.md) and [DECISIONS-EVIDENCE.md](DECISIONS-EVIDENCE.md), and is not
repeated here.

**1.0–1.18 live in [CHANGELOG-1.x.md](CHANGELOG-1.x.md)**, kept whole rather than deleted because the
id a 1.x commit names is still looked up by it. This file carries the current release and what is
unreleased.

## [Unreleased]

**2.0 starts a new store, and it does not read the old one.** Upgrading leaves 1.x's file
(`workout.db`) on the device, untouched, and starts an empty library-backed store beside it: **2.0 cannot
read your 1.x history, and it refuses a backup or a program document written by 1.18 rather than
misreading it.** Export from the 1.18 build first if you want the log — that build is still the way to read
it afterwards — and note that "delete everything" in 2.0 removes it.

### Changed

- **A row states what it knows, and reads the rest from the head above it** (N103).
  `primaryMuscle`, `equipment` and `movementPattern` are nullable, and **null is *unstated*** — `OTHER` goes
  back to meaning only *other*, which is a real answer a lifter gives. One rule covers all of them, the
  secondary list included: **own-if-stated, else inherit**, walking all the way up, so a variation reads what
  its movement inherited from the category above both. A row nothing states a value for answers *none* rather
  than a guess, and the editor offers a field only where no head supplies it — a picker can no longer write a
  value the resolver would ignore, which is what the old head-wins rule did to a movement's own muscle. The
  seed follows: a movement states its own muscle, equipment and secondary list and no pattern, a variation
  states nothing at all, and a new exercise or category starts with a name.

- **The detail screen reads the whole chain, not the head alone** (N103, B96). A variation's pattern and a
  muscle stated two rows up were invisible on its page; the screen now draws the resolved answer — and the
  family's secondary list is shown once, where it belongs, instead of twice.

### Removed

- **1.x's store, and everything that carried it** (N104). The database is a new file at schema 1, so the
  migration chain, its exported schemas and its suite are gone — nothing in 2.0 can reach them. The four
  retired movement-pattern names and the shim that filtered them go with it: both transfer formats move to
  v4, and the version gate now refuses a **1.x file before the decoder**, which is what makes deleting a name
  a 1.x row could carry safe. `fallbackToDestructiveMigration` stays absent: the old store is left alone
  rather than dropped, which is the whole difference between this and losing data.

- **The rest of the legacy the new store cannot hold goes with it** (N105). N104's rule — a name nothing
  can reach should not exist — reached the four retired movement-pattern names and nothing else. The
  `BACK` group the N75 split retired is gone from the enum with its `SELECTABLE_MUSCLE_GROUPS` filter; a
  session exercise no longer carries the single `jointPain` number or its free text (N63), so the rating
  editors, the history row and the joint-pain trend read the picked list alone; and `template_sets` no
  longer carries a per-set target effort (N59), so a plan's one RPE is the exercise's. The transfer format
  also reads no pre-half `rpe` / `targetRpe` field (N6), which only a 1.x file ever wrote. Schema 1 is
  amended in place rather than migrated, which is legal because 2.0 has not shipped (B16).

- **Two columns and a transfer field nothing read are gone** (N108, N109). `exercises.isCustom` and
  `set_entries.completedAt` were written and round-tripped but read by no decision: the id scheme already
  says which rows are the lifter's (seeded slugs against UUIDs), and `createdAt` is the same instant as
  `completedAt`, because a set is never completed later than it is created. Schema 1 was amended in place
  while it is still unshipped (B16). The backup also stops carrying a session's `restEndsAt` —
  device-and-moment state the restore discarded — so the codec's one documented round-trip exclusion is
  gone with it.

### Fixed

- **A failed save or delete on Measurements is surfaced** (B102). The ViewModel reported the failure on its
  error channel, but the screen never read it and `onDismissError` was never called, so a write that did not
  land said nothing — the one screen that behaved that way while every other one shows the same message
  through a snackbar. The screen now reads the channel, and the always-null `error` field on its UI state
  went with the fix.

