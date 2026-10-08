# Decision evidence

The argument behind the rules in [DECISIONS.md](DECISIONS.md): measurements, observed
history, verbatim output, and the longer case against each rejected alternative. One
heading per feature id, matching the `(evidence)` links in that file.

**This is not the rules.** Where the two differ, [DECISIONS.md](DECISIONS.md) wins. Nothing
here needs reading to follow the rules — it exists so the rules can stay short without the
reasoning behind them being lost, and so a settled question is reopened on evidence rather
than on memory.

## D1

Test forking. Measured on the 4-core CI runner, same branch, same tasks, only
`maxParallelForks` differing: **424 s without forking, 467 s with four forks** — slower,
and summing roughly six times the CPU. Measured locally the same way: 63 s vs 65 s.

Most of the task is compilation and Robolectric's resource merging, which forking cannot
overlap, so there is no wall-clock win to buy. The trigger to revisit is a runner that
measures faster, not a feeling that it should.

## D2

"No dead weight" is strict about APIs that exist to be tested, and lenient about tests'
instruments.

`Weight.step`, `DataResult.map` and `successUnit` had no production caller and no test that
used them as anything but their own subject: they are gone, and the tests that existed only
to exercise them went with them.

A DAO or store method that a test calls to *arrange or read* the subject under test is a
caller — `ExerciseDao.insertAll`, `softDelete` and `CrashLogStore.latest` stay, because
deleting them would mean testing through a different door than the app uses. The line is
what the API is for, not where it is called from.

The hard case was left open on purpose (B47) and is settled the same way: four members of the
statistics round were read only by the tests that assert on them. An accessor that exists for
its own assertion is not a test arranging or reading another subject — it is the API that
exists to be tested — so they went, with the tests that only exercised them. Where such a test
asserted a real behaviour *through* the accessor, it was rewritten to assert the behaviour
directly (the trend line's slope rather than its reading count), because the coverage was never
the accessor's to begin with.

## D4

A test tag arrives with the test that asserts on it.

A backlog of tags is applied in production today and asserted by nothing, which is drift in
the one namespace that exists to stop tests reading English. They are kept rather than
deleted in a sweep, because each marks a real control and the change that next touches it
pays it off by writing the test; the rule that stops the list growing is one-way, so a new
tag without its test is a finding. Tags that map to no control at all are still deleted on
sight. **No count is recorded here**: it is a hand-maintained fact, and those go stale.

## N15

Assistance is a magnitude in its own column, never a signed weight.

`weightGrams` stays non-negative and volume stays `weight * reps`, so an assisted set
contributes nothing rather than subtracting — a sign would have quietly corrupted every
volume trend. The editor shows the load as one signed number (`-20`), and the steppers step
*that* number, so pressing + on an assisted set reduces the help; the weight column itself
still cannot go below zero.

`Load` cannot be a `@JvmInline value class` for the same reason. A value class wraps exactly
one property, so the only way to inline `Load` is to store the load as one signed number —
which is precisely the signed weight above, and would make an assisted set subtract from
volume. The inlining would also buy nothing at the call sites that exist: `parseLoad`
returns `Load?` and the editor holds it in a nullable field, so the value is boxed anyway,
and the project's own rule is that a performance claim comes with a measurement.

## N16

A scheduled plan is a living template, not a dated instance. There is one Friday plan:
editing its sets changes every future Friday until it is edited again.

What was *performed* is the record and is already kept, so dated instances would add a
plan-per-date entity, plan generation and skipped-week handling to support a comparison the
logged sets already allow. Several plans may share a day, and a plan with no day is simply
one you start by hand.

The same instinct governs the companion rule: nothing links a session to the plan it came
from beyond the route that started it, so editing a plan changes what the next workout
prefills. Writing the targets onto the session instead would freeze them and make "living"
false.

## N17

A per-exercise trend plots the number that moves, and says which way is forward.

Load series come from *working* sets only: a warm-up must not become the "heaviest set" on a
chart, which is why N14's roles had to exist first. A set with no added weight is not a load
at all — bodyweight and assisted work carry reps and volume (both zero) exactly as N15
decided they carry. For an assisted exercise the series is the *least* assistance of the
session, labelled "less is more", because on a machine a climbing line means the machine is
doing more of the work.

The one-rep-max estimate is Epley's, taken from the heaviest working set, refused beyond
twelve reps (where it extrapolates rather than calculates) and rounded to the nearest
half-kilo, because an estimate is not precise to the gram.

## N19

The role for the next set is armed at the button, and clears itself.

The alternative — a pending role per exercise in the screen's state — was built first and
then removed: it made transient UI state part of a database-driven flow, needed a `combine`
input and a field on every row, and pushed `ActiveWorkoutViewModel` past the function
ceiling detekt enforces, which the config says means a split, not another +1.

Holding it in the composable that draws the button is smaller, survives the state rebuilds,
and puts the "one set per role choice" rule where the choice is made. The ViewModel takes
the role as an argument to `onLogSet`, so nothing there has to remember to clear it.

## N21

Settings live in `SharedPreferences`, not DataStore.

What is stored is a handful of integers, booleans and two small maps owned by one process,
which is the case `SharedPreferences` is still the right tool for — and DataStore would be a new
dependency for it. The move is warranted when a setting needs a schema or a migration. The goal
map is the closest thing to one and is still a line per metric, so the boundary has not been
reached: a hand-written `id=value` line is smaller than a dependency.

Writes are **committed**, not applied, because the screen reports a real result: a
fire-and-forget write would let it say "saved" about something that never reached disk.

Not all of it is a preference, which is the part this entry got wrong. A metric target (N39) is
something the user *authored*, so it rides in the backup file and "delete everything" clears it,
while the rest, the cue, keep-screen-on and the statistics range stay device preferences and are
deliberately not exported. Calling a target a device preference was losing it on every restore,
in silence — the codec trap (N24) wearing a different hat, and invisible for the same reason.

The default rest is a bounded choice: a typed zero would be no rest at all and a typed negative
is not a setting, so the repository refuses anything outside 5–3600 seconds as `DataError.Invalid`.
(This used to argue from the value becoming an alarm; the alert is gone with N26, and the bounds
are still right — they are about what a rest means.)

## N22

Double progression. Keep the load and add a rep until the plan's rep ceiling is reached,
then add the smallest loadable step (2.5 kg, a pair of 1.25s) and start the range again.

The alternatives were rejected deliberately: a percentage-based rule needs a true one-rep
max this app estimates rather than measures, and a linear weekly add ignores missed
sessions. Two consequences are worth stating: assisted work inverts the direction — the
machine doing less is the progress, so the step comes off the assistance — and with no plan
there is no ceiling, so the app proposes one more rep and stops there, because adding weight
without a target would be the app programming rather than the lifter.

A suggestion carries its reason, and null means "nothing to explain". The number reaches the
screen as a value *with* the rule that produced it, because a number the app chose is an
instruction unless it says why — but only the three progression reasons draw a line. A
prefilled set that is just the plan, or just a repeat of the set logged moments ago, has
nothing to explain, and a line there would train the user to ignore the line that matters.

Nothing here changes a plan or a stored set. A suggestion the app applied silently would be
a programme decision taken without the person training, and this app is a log, not a coach.
Warm-ups are excluded from progression for the reason they are excluded from a load series
and from a plan comparison (N17, N20): a warm-up is not the work a target is measured
against.

## N24 codec

A new column is added to the backup codec in the same change, and the codec is guarded by a
round trip.

The codec is hand-written and lists every field by name, so a column it does not know about
is not an error — the export simply does not contain it, and the loss is invisible until
someone restores a backup that is quietly missing data. It has happened three times: a set's
location (N9), a set's assistance (N15), a template's weekday (N16). That makes it a trap
rather than bad luck.

`BackupCodecRoundTripTest` therefore asserts that every field of every backed-up entity
survives entity → DTO → entity, so the next column fails the suite where it is introduced.
The one field deliberately excluded is a session's `restEndsAt`, because a rest countdown is
device-and-moment state rather than training history — and that line now says so, since the
guard could not tell it apart from a mistake.

## B16

A migration is amended only while its version has never shipped, and the cost is real.

`MIGRATION_15_16` gained a second column for the plan side
(`template_exercises.supersetGroup`) after N24 had already run the first version on
development devices. That is legal only because nothing has ever been released with schema
16, and it is not free: **Room refuses to open a database whose stored identity hash does
not match**, so any device that ran the earlier 15→16 fails with *"Room cannot verify the
data integrity… you have changed schema but forgot to update the version number"* until its
app data is cleared. Confirmed on the emulator rather than assumed.

The rule that follows is that amending is for a version no user has, and a device that
already ran it must be wiped. The alternative, a 16→17 migration, is correct but adds a
version step to prove for data that only exists on developer machines.

## N27

The rest cue stays inside the permission-free envelope.

Removing the background alert left the app declaring nothing, and the obvious way to make a
rest audible and felt would spend that: `Vibrator` needs `android.permission.VIBRATE`. So
the cue is view-level haptics (`performHapticFeedback`, no permission) plus a tone played
in-process, and keep-screen-on is a window flag rather than a wake lock.

This is recorded because the constraint is invisible from the feature's description: "sound
and haptics" reads like a platform call, and the platform call that does it costs the
property N26 was for. If a stronger cue is ever wanted, the trade is a permission and it
should be taken deliberately rather than as a side effect.

## B37 and B40

A removed feature still cleans up after itself.

The rest alert is gone and its notification channel is not: Android keeps one across
updates until uninstall, so a device that ran a pre-N26 build still lists "Rest timer" in
its notification settings. Deleting it costs one idempotent call that needs **no
permission**, so the app does it on every launch and the channel goes.

The alternative — documenting it as accepted — was rejected because it leaves a trace of a
deleted feature on a user's device to save four lines, and because "we removed it" should
mean the device looks like it too. The channel id survives in `AndroidApp` for exactly one
purpose, and its comment says so.

## N30 CI

CI runs nightly and before a release, not on every push.

The emulator is the one piece of infrastructure in this project that has failed without a
test running, so a per-push run would mostly report on the runner, and a red pipeline that
says nothing about the change trains people to ignore it. The per-change guard is the local
gate set in AGENTS.md — the same tasks the build job runs — and the nightly run is what
catches the drift a local run cannot see. A release dispatches the pipeline first
([RELEASING.md](RELEASING.md) step 5), because a release is the wrong time to discover the
gate set has stopped working.

The concurrency group carries the event name, so a release dispatch and the nightly run
cannot cancel each other. That is not hypothetical: a push once cancelled an instrumented
run twenty minutes in, and the cancelled job's summary was indistinguishable from an
infrastructure failure.

## N30 emulator

The emulator runs with VM acceleration, and that is what the flakiness was.

The job had failed on infrastructure four times with four signatures — a corrupt image
download, a boot that outran its timeout, an adb connection that died after a sixteen-minute
boot, and a device that booted and then could not answer `getprop`, so AGP skipped it as
"Unknown API Level" and no test ran.

The emulator's own probe named the cause every time and it was read as background noise:
*"This user doesn't have permissions to use KVM (/dev/kvm). The KVM line in /etc/group is:
[kvm:x:993:]"*. The device is on the runner and the group exists; the runner user is simply
not in it, so the emulator fell back to software emulation — "Disabling Linux hardware
acceleration" — and every one of those signatures is what a starved emulator does.

A udev rule grants the group access and `-accel auto` lets the emulator take it. The levers
tried before this one were all about tolerating a slow emulator rather than checking why it
was slow: a longer boot timeout, a lighter image, a lower API level.

The API level was still 34 rather than the 37 the app ships against, and that revisit has now
been taken, by splitting the trade instead of picking a side. The nightly keeps `aosp_atd` at 34
— the image exists because a starved emulator needed a light one, and that reasoning still holds
for a run nobody is watching — while the pre-release `workflow_dispatch` runs at the highest
published API on the default image, which is the run that can catch an API 35+ behaviour change.
The one thing not verified here was that a system image is published for the shipping API: the
first dispatch was the experiment, and if none exists the highest published API is the number to
use, with the reason recorded here rather than rediscovered.

The first dispatch took that experiment and answered it by failing at download. Google publishes
no `platforms;android-37` — the 37 platforms are `android-37.0`, `-37.1` and `-37.2` — and no
`default` or `aosp_atd` system image at any 37.x, only `google_apis*`, which would put back the
Google services this app is built to run without. The dispatch therefore runs at **36**, the
highest API with a published `default` image: one below the shipping API, and the price of a
published image rather than a second choice about the trade. The gap is recorded here rather
than rediscovered on the next release.

## N32

A measurement is one entry per day, edited rather than added to. The roadmap left this to
implementation and named the two candidates. Several per day was rejected because it makes
the chart noisy and, worse, makes "what did I weigh today" a question with more than one
answer — and the second reading of a day is nearly always a correction of the first rather
than a second measurement.

The day is the local day it was taken, converted from the timestamp at the edge, which is
the same rule N25 established for a session's time: a measurement belongs to the day it
happened where it happened.

An unmeasured tape site stays blank rather than carrying the previous value forward: a
carried number is indistinguishable from a measurement and would draw a flat line through a
site nobody measured that day, which is an invented fact rather than a missing one.

## N33

One tap logs what happened; the app's idea of what should happen is an offer.

The suggestion and the prefill were one value before, which is what made a suggestion into a
decision — a proposal that *is* the prefill is committed by the next tap whether or not
anyone agreed to it. They are now different fields: the prefill is the plan's target, what
you just did, or last time unchanged, while the progression proposal is shown with its
reason and applied only when accepted.

The rule is global, not program-only: how a workout was started says nothing about whether
its lifter progresses by hand.

## N38

Test tags are exposed as resource ids. The app opts in with `testTagsAsResourceId`, so a
device-side tool can address a control by *identity* rather than by coordinates: a tap then
either lands on the control or fails, instead of silently hitting its neighbour and
producing a screen that reads as a bug in the app. Two rounds were spent guessing offsets
before this was found — a Compose app is otherwise an opaque tree of anonymous Views to
everything outside its process. The cost is that the tags become visible to accessibility
tooling, which is what an annotation meant for tooling is for.

Noted as a limit: `android_ui_tree` cannot see popup windows, so items inside a
`DropdownMenu` still have to be tapped by coordinates read from a screenshot.

## B45

A range is a window in the current zone; a session's date is where it happened. These are
two different questions and the app answers them differently on purpose.

A *window* — "the last 7 days", a custom range — is one interval, and an interval has to be
measured somewhere; the device's current zone is the only answer that makes "today" mean
today for the person reading the screen. A *session's date* is a fact about the past, so it
is shown in the zone the session was performed in, which is what N25 stores.

The consequence is real and accepted: after a long flight a workout near midnight can be
listed under one date and fall outside a window that appears to include it. The alternative
— a window per session — is not a window, and one screen cannot be drawn against several
intervals at once. The reason this is written down rather than patched is that the two
readings are both correct about different things, so the next person to notice the mismatch
should find a decision here instead of a bug report.

The companion rule: `ZoneId.systemDefault()` is a function for a reason. A `val` in a
companion object freezes the zone at class load, so a process that outlives a timezone
change keeps computing "today" in a zone the device no longer has. Every screen reads it at
the point of use, and the statistics view model now does too.

## B48

A template's ramp did not survive being run. `startOrResumeSession` seeds a template's
exercises and never its sets, and the pending set was armed at `SetType.NORMAL` and reset to
`NORMAL` after every set — the plan's role was read only to compare plan against actual in the
review. So a ramp was recorded as working sets unless the picker was tapped once per set, and
the review then drew a plan the user appeared to have ignored.

The fix carries the role the way reps and weight are already carried: `PlannedTarget` gained a
`role`, filled from the plan's next unlogged set (the slot's where a slot speaks), and
`SetSuggestion.setType` is what the picker rests at. N19's rule that the role "clears itself"
is unchanged; what changed is the resting value. It is not tidiness: warm-ups are excluded
from records and progression on purpose (N17, N20, N22), so a ramp recorded as working could
set a personal record and inflate volume against a bar nobody cleared.

The rejected alternative — a second "apply the plan's role" pass over the logged sets after
the fact — would rewrite the record from the plan, which is the opposite of N14's rule that a
logged set may differ from the plan and is the record of what happened.

## B49

"Add warm-ups" was fully implemented in the ViewModel, fully unit-tested, and never called.
`TemplateEditorRoute` passed every callback except `onAddWarmUpSets`, so the screen used the
parameter's default empty lambda: a tap wrote nothing, reported nothing and changed nothing.
The logic under test was right, which is exactly why nothing failed.

The repair is one line at the route, plus the removal of that default. A default empty lambda
on a callback a screen cannot work without converts a forgotten wire-up from a compile error
into a silent no-op, so the default goes and the next omission fails the build. Optional
callbacks may still default; required ones may not.

**B52 is the same rule tested on the next change that could break it.** The program document's
export and load were added as `(() -> Unit)? = null` on `ProgramEditorScreen` and `ProgramsScreen`
— the very shape this section was written about, introduced by the change that wrote it. A route
that forgot one would have drawn no control and failed nothing, and the screens' tests could not
have caught it, because they pass the callback in rather than going through the route. Both are
required now, and the reason is recorded at the parameter so the next one is not a judgement call.

## B50

Found while diagnosing B49: the guard asked whether a non-warm-up set had a weight *typed*
(`targetWeightGrams != null`), while the action needed a weight a ramp could be taken *from*.

Three cases slipped between them. An assisted set is stored as `0` kg of added weight and `20`
kg of help (N15), so `-20` offered the button. A `0` kg or `2.5` kg working weight offered it
too, because every fraction rounds up to at least one 2.5 kg step, the filter `1 until
workingWeightGrams` drops them all, and the ramp comes back empty. In both, the action returned
success without writing — silent by construction, where the dialog's own comment already held
that a control which would do nothing is worse than no control. Pressing twice was the opposite
failure: the guard stayed true and `prependSets` shifted the plan down, stacking a second ramp
in front of the first.

One predicate, `warmUpRampFor(sets)`, now decides both the guard and the action, which makes
the silent path unreachable. The test that appeared to cover the light case asserted
`all { it.weightGrams < 5_000L }` — vacuously true on exactly the empty list the bug produced —
and is now an assertion on the list itself.

## B55

`IconTile` drew every glyph in `Color.White`, which is what the reference screenshot shows and what
the component's own doc said. Ratios are where a screenshot stops being evidence: white is 2.5:1
against `Amber`, 2.9:1 against `Teal` and 3.0:1 against `Coral`, under the 3:1 a graphic needs.
`Indigo` is the one accent dark enough that white is the better half of the pair; on the other four
the page's own ink reads at 5:1 or better.

The tile is decorative — a headline beside it names the row — so this was never a WCAG failure, and
a fix was not urgent. It is still the tile's whole job: the colour is what lets a list be recognised
before it is read, and a glyph nobody can make out is that job half done. So the accent names an
`onColor` instead of assuming white, and `TileAccentTest` computes the ratios and holds every pair at
3:1 or better. Asserting it rather than eyeballing it is the point: the palette will be edited again,
and this is the one property of it that a screenshot cannot show.

## N41

A workout with logged sets had no exit but Finish, which files it in history; only a workout with
*nothing* in it could be discarded. So the third case — sets the user wants gone — had no answer.

The prompt keys off the same emptiness that already decided whether a discard was offered:
`isEmpty` keeps its prompt-free button because there is nothing to lose, and anything else asks
first and names the count. The second half is the part worth writing down: a discard is a *soft*
delete, but a workout started from a program slot that is never finished is a **miss**, because
P3.5 holds that only a finished session settles an occurrence. A prompt that said "delete this
workout?" would therefore be lying by omission, so it says that dropping out is counted against
the program. The signal is the route's slot id — a session started from an unscheduled template is
not an occurrence and gets no such warning.

The action lives behind the top bar's overflow rather than beside Finish, because the visible slot
is the one a lifter reaches for mid-set and a destructive action does not belong under a thumb.
An undo was rejected: the row is soft-deleted, but an undo would have to reopen a workout the user
asked to be gone and would say nothing about the occurrence it settled.

## N44

The rest timer had one behaviour: log a set, an end instant is written, the screen counts it down
with ±15s controls, and the end chimes (N27). The ask is the opposite preference — someone who
rests by feel and would rather not be counted at — but the prescription is still worth reading, so
the answer is a switch rather than a removal.

**Off shows the exercise's own rest as a fixed label**, falling back to the app default where the
exercise has none: the same number the timer would have counted, drawn as a fact about the exercise
instead of a countdown.

**Off has to mean the timer is genuinely not running.** The rejected alternative was to keep writing
the end instant and simply not draw the bar; that is a countdown hidden behind a static number, and
the interval would still be burning — a rest that "ended" silently, or chimed if the cue were on.
So the end instant is never written while the switch is off, and a rest already running is cleared
when the switch goes off.

That is also why this is not the same feature as a prescribed rest of zero (N45). This one is a
preference about the timer; that one is a fact about the exercise. An app-wide zero would take the
rest out of every exercise at once, which is this switch's job, so zero stays out of the default
rest's choices.

## N45

The rest field had two states and needed three. Empty meant *inherit*; a positive number was that
rest; and **zero was refused** — in three repositories, with two different sentences for one rule,
and as a red field in the library editor, the template plan editor and the slot prescription dialog.
So "this exercise needs no rest" could not be written down at all, and a zero read as an error when
it is a perfectly ordinary intention.

The change is validation and wording rather than plumbing, which is why the three sentences collapse
into one (`RestTimer.NEGATIVE_REST_REFUSAL`) and the floor moves from one to
`RestTimer.MIN_PRESCRIBED_SECONDS`. Nothing downstream needed touching: `startRest` computes an end
instant at *now*, so a zero rest never runs, and nothing collapses zero into "absent" — both the
`?:` fallbacks and the DAO's `COALESCE` test null rather than falsiness.

Two things stay deliberately unchanged. The **default rest** keeps its 5–3600 bound (N21), because a
typed zero there would be an app-wide "no rest" — the switch's job (N44) — and the settings screen
already says so in a comment. And the **display** gained a word: a stored zero renders as "None"
rather than `0:00`, because `0:00` reads as a rest that has run out rather than one that was never
wanted. That is a `restLabel` helper beside `RestTimer.format`, used by the library detail and the
workout screen's static prescription.

## N46

The *Planned sets* dialog offered a blank **Add set** beside **Duplicate**. Duplicate appended a
copy of every set the exercise already had, which is a loop wearing a button: it doubles (1, 2, 4,
8), so three or five sets always end in manual adds, and what it stood in for was a *prefill* rather
than an operation of its own. It also appended the whole plan — ramp included — into an order this
app went out of its way to make reliable: `setIndex` is stored rather than inferred, and B34 exists
because a generated ramp once landed after the work it was written to prepare for.

So the button and its whole path go — the repository method, the DAO read behind it, the tag, the
string, and the test that only exercised it — and Add set starts from the last set instead: blank
only while the exercise has none. It is strictly better than what it replaced. Any count including
the odd ones, appended in place after the work, with the ramp left where Add warm-ups put it. The
role prefills too, which is safe precisely because Add warm-ups *prepends*: the last set is the last
working set.

The two surfaces that author sets disagreed, and removing the button from only one would have moved
the inconsistency rather than settled it. A slot's prescription dialog never had the button but did
have the blank form, so it takes the same prefill — one rule, two call sites. It needs a lookup
rather than a one-liner because its sets hang off the editor rather than sitting in local scope; the
last one is read back through the exercise's prescription.

The cost is accepted rather than marked: prefilled values look exactly like saved ones, so "Add set"
then Save without touching a field is a plausible accidental double-add. The title already says it
is adding and the list behind it shows the count; a set-*count* control would be the more honest
affordance if it ever needs revisiting.

## N42

Three changes to the workouts tab, one argument: the tab that starts a workout should answer "what
am I training today" and get out of the way.

**"See all workouts" went.** It was a text button on the *Recent* heading whose only job was to
open History — the tab beside it. A heading that carries a way out of its own section duplicates
the tab bar, and it read as one more row rather than as navigation the moment it sat with the
section's name. History keeps every entry point it already has.

**Repeat-last gave its slot to Programs.** Programs is the screen the whole scheduling half is
edited from and it was reachable only from the overflow. A destination belongs in the action row;
the menu is where it got lost. Repeat-last is not deleted with its button — the feature keeps its
one-tap copy and gains a better address (N48, a finished workout in History), because dropping an
entry point to a shipped feature is not the same as deciding against the feature.

**The overflow went.** With Programs out of it, it held only export, import and delete-everything
— actions on the whole database rather than on a workout (N43). Keeping a three-dot menu for zero
workout actions is the "control that would do nothing" the ramp rule already rejects, and the
placement had already moved twice: the library held these actions, B1 moved them to the home
overflow as "back where you start". Settings is a third and better answer rather than a return to
the first, because B1's complaint was that the library sat two menus from where the user starts,
not that a data action belongs beside *Start workout*.

## N43

Export, import and delete-everything act on everything the user has recorded, so the screen about
the app is where they belong. Each keeps what it does today — the SAF pickers, the additive import,
the typed confirmation with the export inside it (N18) — and delete-everything stays last and
coloured, because it is still the one entry that can cost the user something.

The move needed one shared piece rather than a copy: the held-failure-to-snackbar conversion the
home route used was extracted to `FailureMessage` when Settings became its second caller, so the
two screens cannot drift in how a failed write is reported.

## N48

The repeat was "the last workout's, or nothing": a boolean on the route, and a repository query
whose SQL picked the newest finished session. That is all a button on the home screen could mean,
and N42 removed the button — but the feature was not deleted with it.

The action moves to the row that names the workout. So the address becomes an argument: the route
carries the session id, and the repository copies *that* session's live exercises. The SQL that
picked "the last one" goes with it; the query that serves the repeat is the one N31 already uses to
build a template from a session, which carries the same two rules (a library row deleted since is
skipped, and the same exercise twice stays twice) and the columns B41 needs for rest, note and
grouping.

Two consequences worth stating. **A row deleted since the list was drawn is reported** rather than
opening a blank session: the user asked for a specific workout, and an empty one would be a silent
lie. And **a repeat is still a start**, so it goes through P3.3's missed-day question like every
other start; the History screen hosts the gate for it, and a skip that could not be recorded is
shown on that screen's own host because the workout still starts.

The row's second action is an icon in the trailing slot rather than a second full-width target,
because a row is one thing you tap. It is offered only where `isRepeatable` says a repeat would
copy something — the helper B43's tail added for exactly this question, which is why it stayed
through N42 while its only caller was gone.

## N47

Programs and their templates already rode in the backup, and import already merged rather than
overwrote — so what was missing was not a reader but a *document*. There was no way to carry one
program to another device, or to accept one somebody else wrote, without moving the whole database.

**A format of its own, with a version of its own.** A [BackupFile] cannot be trimmed into a program
document: four of its collections are required rather than defaulted, so a program-only file cannot
be a small backup. [ProgramDocument] reuses the backup's DTOs because they are already the
raw-storage shape the mappers speak, but its `formatVersion` is separate from `schemaVersion` — the
two change for different reasons, and a new backup column does not make an old program file wrong.
The same explicit gate applies: a document from a newer build is refused rather than partially read.

**What travels is the definition, not the history.** The program, its slots, the templates those
slots name, their planned exercises and sets, and what each slot prescribes. Skips, deload weeks and
substitutions are keyed by week and are the lifter's record; a received program starts with no past
rather than with somebody else's.

**Every exercise the document names travels whole.** This is the decision the format forced. A
template references exercises by id. The seeded library's ids are permanent slugs that mean the same
thing on every device, so those would resolve anywhere — but an exercise the *user* created carries
a generated id that means nothing on the receiver. Rather than branch on seeded-versus-custom (which
would need a list of slugs and would still lose a user's exercise the moment a seed changed), the
document carries the definition of **every** exercise its templates name. The receiver inserts what
it does not already have, keyed by the id the document used, so a seeded row simply finds itself
present and a user's exercise is recreated.

**Loading merges and overwrites nothing.** Every insert ignores a row whose id is already present,
which makes loading the same file twice a no-op rather than a second copy and means a document can
never cost the user a program they wrote. Two consequences are deliberate. The imported program
arrives **inactive and at the end of the list**, because following a program is a choice rather than
something a file makes (P3.3's "a new program is not made active") and the authored order belongs to
the user (P3.12). And a movement whose exercise is neither carried nor present on the device is
**dropped**, because the foreign key would otherwise roll the whole document back — one missing
exercise is not a reason to refuse a program. The load reports the counts, including what it
dropped, so the outcome is said rather than guessed.

**B51 — "present" had to mean one thing.** The presence read was `ExerciseDao.findById`, which
filters `deletedAt IS NULL`, and the insert beside it was `INSERT OR IGNORE`, which skips an id that
exists *including* a soft-deleted one. A row the device held but had deleted therefore failed both
tests: the insert skipped it and the read could not see it, so every movement naming it was dropped
and the sentence told the user the exercise was "not on this device" — false, and false in the one
way that matters, because the user could look at their library, remember deleting it, and have no
idea whether the file or the app was wrong.

The backup answers the identical question the other way. `restoreSoftDeleted` reads
`softDeletedExerciseIds` and `@Update`s those rows back, because a backup is a *restore*: it exists
to reproduce the source database, and a row the file has live is one the target should have live
too. A program document is not a restore. It is an additive share, and the rule it was built on is
that it "overwrites nothing" and can never cost the user something they wrote — so resurrecting a
lift they deliberately deleted is exactly the write that rule refuses. The movement is dropped, and
the message says the library rather than the device, which is true whether the row is absent or
merely hidden. The alternative — restore it, as the backup does — was rejected on that rule rather
than on effort; the cost of dropping is a visibly thinner program, which the count reports.

**B56 — the invariant the interactive writes kept and the import did not.** P3.8 holds that a slot
prescribes only what its template trains, and `setSlotExercisePlan` enforces it with
`requireExerciseInTemplate`. The import writes through the *backup* DAOs, which are deliberately
raw, so nothing re-checked it. The case that reaches this is narrow but real: two devices that once
shared a template id (a restore, or an earlier load of the same document) and then diverged, where
the document's prescription names a movement the local template has since dropped. The result is a
prescription row no screen can show, waiting to become visible and wrong if the template ever gains
that exercise again. The load now asks the same question the interactive path asks, against what
each template holds *after* the carried exercises have landed — so a template that travelled in the
document is judged on what it just received, and one that was already here is judged on what it
holds now. The pure predicate exists so the rule is held by a JVM test rather than by a device.

## P3.3

Programs. The shape was decided before the code, and the interesting part is what each
decision rules out.

**A skip is an event, not a flag.** The obvious model is a `skipped` boolean on the slot. It
cannot work: the same weekday recurs every week, so the flag would have to be reset, and it
would be wrong the moment two weeks in a row were missed. A row per (slot, week) needs no
resetting and answers "what did I miss in the week of the 5th" — which is exactly the query
P3.5's adherence is made of. The week's Monday is stored as an epoch day rather than a
timestamp, because "which week" is a calendar question and a millisecond would drag a
timezone into it.

**Provenance, not prescription, on the session.** An occurrence is settled by the session
started from its template, so the session has to say which template that was. N16 explicitly
rejected copying a plan's *targets* onto a session — that freezes what the plan prescribes and
makes "living template" false. Copying the template's *id* is the opposite: it records where
the session came from and changes nothing about the plan. The column is therefore written
**only on insert**: `findOrCreateActiveSession` returns early for a resumed session, so a
resumed workout keeps the provenance it was created with rather than acquiring a new one.

**Matching is by template and date, and the rule order is stated because ties are real.** More
than one slot can reference the same template (a lift trained twice a week is the normal
case), so one session has to choose which occurrence it settles. Exact weekday first, then the
latest earlier slot, then the earliest later one, then the earliest unresolved; a session
resolves one occurrence and an occurrence is settled by the first session that matches it.
Matching by *exercises* was rejected — it breaks the moment a template is edited, and it
cannot tell two slots apart. The week is Monday-start and taken in the session's own zone
(N25), which is the only reading that survives a workout performed after a flight.

**The question is asked at the point of starting.** Asking at launch was rejected outright: an
app that interrogates you when you open it is one you stop opening. Only days strictly before
today count as missed, so a Friday slot on a Wednesday is not "settled" by a Continue that
would be inventing a decision. Continue writes every pending skip in one call for the same
reason it exists at all: asking again for the next miss turns two misses into two
interrogations.

**One active program is a flag on the row.** The alternative — an id in `SharedPreferences` —
was rejected because a program is training data: it rides in the backup like a plan or a
measurement, and a preference pointing at it would split one answer across two stores and be
lost on a restore. `setActiveProgram` clears the others and sets this one in a single
transaction, so "at most one" cannot be observed half-applied. `isActive` also makes the
fallback explicit: no live row active means the home screen reads the N16 pins it always did.

**The migration leaves `templateId` null, deliberately.** A workout recorded before programs
existed cannot be given the template it was started from — that was never captured — and
stamping one on would make it settle an occurrence it never touched. Null means "unknown",
and the matcher finds no session for those weeks, which is the honest outcome.

## P3.5

Adherence and a calendar. The shape is P3.3's read forward: the prompt's occurrence matching
already answers "was Tuesday done", and the aggregate asks the same question over a month.

**The unit is the occurrence because that is the unit P3.3 settles.** A day-level ratio was the
obvious alternative and it is wrong in a way that only shows up in data: a program may place two
slots on one weekday — a doubled session is the normal shape of that — and P3.3 resolves and skips
them one at a time. Counting days would let a session settle the day and hide the second slot's
miss inside it. The calendar draws days because "which days did I train" is a different question
from "how much of the schedule happened", and the two are allowed to differ: the ratio counts two
while the grid marks one.

**Done means finished.** The prompt and the aggregate disagree on purpose. P3.3's question is
"should I nag you about Tuesday?", asked at the point of starting, and a session that was started
is one you are in the middle of — nagging would be absurd. Adherence's question is whether the
training happened, and a session opened and abandoned is not training. Reusing the started rule
was rejected for that reason, and the narrower read costs one predicate: the window's *finished*
sessions rather than every started one, which is also what stops an abandoned start marking a
trained day.

**One window, the month the grid shows.** The alternative — a rolling ratio beside a calendar of
the month — was rejected because the number and the grid would then be counting different spans,
which is the kind of mismatch a reader notices and cannot explain. The calendar goes back through
history and forward no further than the current month, and only a day strictly before today can
be missed, which is P3.3's rule and the reason a Friday slot on a Wednesday is not a failure.

**No program, no ratio.** The fallback home keeps is the N16 pins, and a pin records that a
weekday is trained, never that a rest on it was chosen — there is no skip row to consult. A ratio
over pins would therefore count every deliberate rest as a miss, which is worse than no number.
The calendar still marks the days trained, because that question needs no schedule at all; the
ratio is absent, and the screen says which of the two absences it is.

**No schema change.** `program_slots`, `program_skips` and a session's `templateId` and
`finishedAt` are everything P3.5 reads, and all of them shipped with P3.3 at v20. The aggregate
is a pure function over them, so the arithmetic worth arguing about is a JVM test rather than a
device one.

## N57

The headline becomes `Sun, Oct 4, 2026` — the locale's short weekday, then the `MEDIUM` date the row
already showed. The month heading above already says the month, so repeating it in full
(`Sunday, October 4, 2026`) would spend the headline on the one part the reader already has; the part
a lifter navigates by — "the Sunday session", "two days after Monday" — is the weekday. The name is
the locale's own (`TextStyle.SHORT`) rather than an English pattern, for the same reason
`HistoryFormat.month` uses `TextStyle.FULL`: the app is local-only but not English-only, and a
hard-coded `EEE` would be right in exactly one language. The test pins the locale it asserts
(`Locale.UK`) so it reads as "the locale's abbreviation" rather than as an English string — and it
caught that `en_GB` writes September as `Sept`, which is the point of using the locale's names at all.
One formatter serves home's *Recent* row too, because the two are the same question about the same
object.

## N58

A session started from a template has carried `templateId` since P3.3, and the history projection never
selected it — so *Push A* on a Tuesday and an empty workout on a Tuesday read identically. The name is
the half of the row that says what the session *was*.

**Live, not snapshot.** The alternative was a `templateName` column on the session, written when it
started. It costs a migration for a copy of a string that already exists one join away, and what it
buys is only the *rename* half of the problem — a renamed template would keep the old name in history.
The other half, deletion, the live read already answers: `deleteTemplate` is a soft delete (P1.12), so
the row is still there and the workout still says which workout it was. The accepted cost is that
renaming a template relabels the past, which N16 already accepts as the meaning of a living template:
the workout's own identity is when it happened, and that is the headline.

**The line wraps rather than truncating.** The roadmap asks for that, and the reason is worth stating:
`AppRow`'s supporting `Text` carries no `maxLines` today, so the four items — name, duration, sets,
volume — wrap onto a second line on a phone. An ellipsis would hide the one item that cannot be
inferred from the rest of the row. A fifth item would be the point at which the line stops being
scannable, and that is a change to make deliberately rather than by accumulation.

**The repeat's edge.** N48's repeat starts a session from a finished workout's exercises and left
`templateId` null, so a repeated *Push A* would have been the one history row that lost its name. The
repeat now copies its *source's* template id, which is provenance rather than prescription: the targets
still come from the exercises the session copies, and nothing about N16's "a session reads the plan at
the start" changes. Carrying the source's session id instead — a second provenance column — was
rejected as a much larger change for a label.

Two changes to one row, and the row's job is the argument for both. A history row has to answer "when
was this" and "what was it", and it answered the first with a date inside a month heading and the
second with nothing at all — a session started from a template carried `templateId` from P3.3 and the
projection never selected it, so *Push A* on a Tuesday and an empty workout on a Tuesday read
identically.

**The weekday rather than a longer date.** The headline becomes `Sun, Oct 4, 2026`. The month heading
above already says the month, so repeating it in full (`Sunday, October 4, 2026`) would spend the
headline on the one part the reader already has, and the part a lifter actually navigates by — "the
Sunday session", "two days after Monday" — is the weekday. The name is the locale's own
(`TextStyle.SHORT`) rather than an English pattern, for the same reason `HistoryFormat.month` uses
`TextStyle.FULL`: the app is local-only but not English-only, and a hard-coded `EEE` would be right in
exactly one language. The test pins the locale it asserts (`Locale.UK`) so it reads as "the locale's
abbreviation" rather than as an English string — and it caught that `en_GB` writes September as
`Sept`, which is the point of using the locale's names at all.

**Live, not snapshot.** The alternative was a `templateName` column on the session, written when it
started. It costs a migration for a copy of a string that already exists one join away, and what it
buys is only the *rename* half of the problem — a renamed template would keep the old name in history.
The other half, deletion, the live read already answers: `deleteTemplate` is a soft delete (P1.12), so
the row is still there and the workout still says which workout it was. The accepted cost is that
renaming a template relabels the past, which N16 already accepts as the meaning of a living template:
the workout's own identity is when it happened, and that is the headline.

**Read once, by two surfaces.** Home's *Recent* row and the history row are the same question about the
same object, so they share one formatter and one supporting line. The roadmap asks for a name that
"wraps rather than truncates", and the reason is worth stating: `AppRow`'s supporting `Text` carries no
`maxLines` today, so the four items — name, duration, sets, volume — wrap onto a second line on a phone.
An ellipsis would hide the one item that cannot be inferred from the rest of the row.

**The repeat's edge.** N48's repeat starts a session from a finished workout's exercises, and it left
`templateId` null — so a repeated *Push A* would have been the one history row that lost its name. The
repeat now copies its *source's* template id, which is provenance rather than prescription: the targets
still come from the exercises the session copies, and nothing about N16's "a session reads the plan at
the start" changes. Carrying the source's session id instead — a second provenance column — was
rejected as a much larger change for a label.

## N56

The pin was a decent answer to a question the app has since answered better. N16 added it when a
template was the only thing that could be scheduled: *Today* meant "the plans that name this weekday",
and with no program in the picture that was the whole schedule. P3.3 then introduced the program and
P3.12 allowed more than one, so a day gained an owner that has an order, a next-up and an adherence —
and the template's own pin became the fallback branch of `todaysPlanFor`, reached only when nothing was
active. A fallback that answers a scheduling question with a non-scheduling object is the definition of
the weaker copy, and the roadmap's own rule ("a day is a scheduling fact, and scheduling is what a
program is for") is the argument for deleting it rather than keeping both.

**What "no *Today* list" means.** With no active program, home shows the recent workouts and no
scheduled section. That is a visible behaviour change and it is the intended one: before, a user with a
pinned template saw it under *Today*, and after the change they see nothing until they put it in a
program. The alternative — keeping the pins as a fallback "so nothing is lost" — was rejected because
it is exactly the second answer this change exists to remove, and because it would leave two code paths
deciding what Tuesday is. The accepted cost is stated in the changelog rather than hidden behind a
migration that tries to invent the schedule back.

**The column goes, and the migration rebuilds the table.** SQLite has supported `DROP COLUMN` since
3.35 and Room's bundled version is newer, but the rebuild is what the other table-shaped migrations
here do and it states the surviving columns explicitly: `id`, `name`, `createdAt`, `updatedAt`,
`deletedAt`. The rows keep their `id`, so every foreign key into `templates` — `template_exercises`,
`program_slots`, `program_substitutions` — still resolves, and the migration test asserts the planned
exercise still points at its template after the upgrade. Dropping a column ahead of the code that
stops reading it is what the project's own migration rule forbids; here the code and the migration
ship together, and nothing reads `weekday` afterwards.

**The backup DTO's field goes with it.** The two candidates were keeping `weekday` in `TemplateDto` so
older *and newer* files could carry a pin the app ignores, or deleting it with the column. Keeping it
was rejected: an exported file is a statement about this app's data, and a field no version reads is a
promise the format cannot keep. `BackupCodec` already sets `ignoreUnknownKeys` (for a file from a newer
build), so a file written before the change decodes with the pin skipped — which is the same thing the
column removal does to the database — and the codec test now asserts exactly that.

## N55

The row's two jobs were fused into one control: the card's only action was *Start*, so the only way to
find out what a workout contained was to begin it. That is F16's and N16's old problem in a smaller
place — a screen answering a question nobody asked — and the fix is the one N42 already made at the
other end of this screen: put the thing where the thumb is and let each target mean one thing.

**The field opens a read-only dialog, not the editor.** A template had exactly one destination, its
editor, and the roadmap named the choice. The editor was rejected because reading a plan is not
editing it: every target in the plan would sit one mis-tap from being rewritten while the user was
only looking, and the app's own safeguard against that — the deliberate save on the name field, the
per-set dialogs — is a cost paid for a browse that did not need it. A preview screen of its own was
rejected second: the answer is a short ordered list of exercise names, so a destination with a back
button, a route and a ViewModel would be scaffolding around a dialog. What a preview *should* show
beyond the names — targets, rest, cues — is not decided here; the dialog is the smallest thing that
answers "what is in this one", and a screen can replace it if looking turns out to want more.

**The read is per tap, not a flow per row.** The dialog holds one plan for as long as it is open, and
the read goes through the same repository the editor uses, so a rename shows here too — N16's living
template seen from the other end. A flow per next-up row would be a live query behind a field nobody
has tapped: with more than one active program (P3.12) that is one query per program per home screen,
for a look that most sessions never take. The cost is that a template edited on another screen while
the dialog is open is not reflected until it is reopened, which is the same staleness the rest of the
screen's own snapshot has.

**Compact, because the bar repeats.** The row is a label, a name, a supporting line and a start, not a
card with a tile: more than one program may be active, so the bar may hold several next-up rows above
the start pill, and cards would push the primary action off the screen. The *Today* rows stay cards
because there is one day and a card is the shape a scheduled workout reads as.

## N54

The session's list is already stored in an order — `session_exercises.position` — and that is the
whole reason this is a small change rather than a new concept. What it was for is provenance: the
positions are written when a plan seeds the workout, and a repeat copies them (N48, B41). Nothing
reads them as something the user owns. N54 is the statement that they are the session's, not the
plan's: the write swaps two rows' positions and the template's `template_exercises.position` is a
different column that is never touched.

**Persist as it changes, not at the end.** The alternative considered was holding a draft order in the
ViewModel and writing it when the workout finishes. That is what makes the reorder "free" — one write,
and nothing to undo if the user changes their mind — but it fails on two counts that matter more. The
workout screen is the one screen that exists to survive a process death (P1.8: the session is in the
database before anything is logged), so an order that lives only in memory is the one piece of the
screen that could vanish. And a repeat copies the *stored* order, so a workout that was rearranged and
then repeated would repeat in an order the lifter never chose. The cost of writing as it changes is one
transaction per tap, which is the same cost `removeExercise` already pays.

**A swap, not a renumbering.** Both rows' positions are written in one transaction, the shape B27
established for a superset group: a failure cannot leave two exercises claiming one position, which is
an order that no longer means anything. At the top or the bottom there is no neighbour, and that is
reported as success — a menu entry that is legal to tap and does nothing is not an error the user can
act on, and the alternative (disabling the entry) is a state the row would have to know the length of
the list to compute.

**A superset member moves as itself.** Swapping with the neighbour keeps a round adjacent, because
adjacency *is* what the grouping means on this screen — N24's round logic compares set counts within a
group, and reordering never splits one. Moving a whole group as a unit was rejected as a rule nobody
asked for: it would make the two entries mean something different depending on a grouping the user set
up earlier, which is exactly the kind of hidden state N19 and N24 both avoid.

## N51

The one-tap path was D3/B7's rule stated as a feature: *Log set* wrote the set its button described,
because withholding knowledge so that display and storage agree was judged worse than the alternative.
The alternative it was compared against was an app that quietly wrote something other than what it
said — which is not the choice N51 makes. The choice is between one step that decides and one step
that states, and the evidence for the second is what happened in use: a set that differed from the
prefill was *logged and then edited*, so the user paid the same dialog anyway and paid it after a wrong
row existed. The role picker was the clearest case — three warm-ups meant three log-then-edit round
trips, which is why N19 exists, and with the dialog doing the writing it is one tap per warm-up still.

What the button loses is its values, and that is the honest half of the change. A label reading
"Log set · 100 kg × 5" described the row's offer; the row's offer is now the dialog's *initial* state,
which is a different claim — it is a starting point the user can change, not a statement of what the
tap writes. Keeping the values on the label while the dialog decided the write would have been the D3
problem inverted: a control that says one thing and does another. So the label becomes "Log set" and
the notice beside it still answers "what am I about to write" for the plan's own last set (N52).

N19's rule survives the move unchanged, and that is worth stating because the picker changed hands. A
role is still a decision about *this* set, made at the moment it is written, and clearing itself is now
structural rather than remembered: the dialog is keyed on the row's logged-set count, so a write
re-reads the plan's next unlogged set (B48) for the next dialog. Nothing lingers because there is no
longer a value left lying around to linger.

Retiring B7 for this one control is deliberate rather than an oversight. B7's own rule was "a logged
set may differ from the plan, never from the button"; the second half exists so that display and
storage cannot disagree. With the dialog as the display, they still cannot.

**Superseded by [N59](#n59).** The dialog-as-display kept display and storage in agreement, but it put
a confirmation between every set and made the button mute about what it would write. N59 draws the same
values on the screen the lifter is already reading, which is the display and storage agreeing *before*
the write rather than at it.

## N59

The problem N51 solved was real — a set that differed from the prefill was written first and corrected
afterwards, so the user paid the dialog anyway and paid it on top of a wrong row. The shape it chose
paid for that with a step between every set, and with a button that could no longer say what it wrote:
the label went from "Log set · 100 kg × 5" to "Log set", and the values moved into a dialog nobody sees
until they open it. N51's own evidence named that cost — "the label becomes 'Log set'" — and accepted
it because the alternative was a control that decided for the lifter. There was a third shape.

Putting the values on the workout screen as fields reconciles both: they are visible and editable
before anything is committed, so the write states what the lifter read, and the button beside them
carries no values because the fields *are* the values. B7's rule is restored rather than retired, and
the editor keeps its other job — correcting a set that already exists, which is the case N51's dialog
was originally built for.

What this costs is screen space: every open exercise now carries a role picker, two steppers and an
RPE field, where before it carried one button. That was accepted deliberately, because the values are
what the lifter is deciding about at that moment, and the alternative — a compact summary that opens
into fields — hides them behind the same tap the dialog did.

The offer (N33) went with the dialog, and that is the part worth arguing rather than assuming. It was
built on the premise that a proposal which *is* the prefill is a suggestion only until the next tap
commits it. With visible, editable fields that premise no longer holds in the same way: the prefill is
read before it is committed, so a proposal folded into it is not silently committed — but it is also no
longer a *proposal*, because nothing distinguishes the app's arithmetic from last time's numbers. Since
a proposal with no way to tell it apart from history is not a proposal, and ROADMAP N50 owns reworking
what the app should suggest, the honest move was to withdraw it rather than keep a rule nothing calls.
Its tests went with it; the loadable step it shared with the warm-up ramp (N28) stayed.

## N74

The question N50 shipped was exercise-wide twice over, and both halves failed in use. Earning required
**every** prescribed working set to be met, so one missed back-off set earned the lifter nothing at all —
not even the top set that had answered the plan. And the step always landed on the **last** working set,
so the dialog stated one unlabeled plan line and one done line about a set the lifter could not identify,
offered two mutually exclusive directions with no reason to prefer either, and closed the exercise in the
same tap that answered it. "I don't get how to use it" is what that shape produces, and it is not a
wording problem: there was no set in the question to answer.

Judging each set on its own is what puts one there, and it costs nothing in sureness — the old rule's
"every set met" was the app's way of being certain, and the same certainty is available per set from that
set's own reps and effort. The pairing stays the plan's Nth working set against the session's Nth, with
warm-ups dropped from both sides, because `setIndex` breaks the moment a plan's warm-up goes unlogged
(N17, N20, N22). A prescribed set that was never performed earns nothing and says "not done"; work the
plan does not name is stated too, with no target to change, so the question accounts for the session
rather than for part of it.

**The direction needed a rule, not a menu.** Offering a load and a rep side by side asked the lifter to
choose between them with nothing to choose on, which is the second half of why the old question read as
unanswerable. N22's rule — add reps to the plan's rep ceiling, then the load step and start the range
again — is that rule, and N50's refusal of it ("that computed from the rep ceiling alone and offered
itself beside the next set") was a refusal of its *gate*, not of its shape. Gated on the session's own
RPE it returns: below the range's ceiling the reps move and the weight waits, at the ceiling the weight
moves and the range restarts. A plan that wrote no ceiling keeps both directions, which is what it did
before. A bodyweight or assisted set at its ceiling is offered nothing, because the app has neither a rep
inside the range nor a weight to move, and saying so is better than inventing a step or moving a load that
`Weight.display` would then hide behind the assistance (N15).

**The range could not stay in the ceiling's column.** N50's rep step raised `targetRepsMax`, which grew
the prescription instead of the lifter's place in it: "5 to 8" became 5–9, then 5–10. Holding the range
still and moving the target needs a third number per planned set, which is `targetRepsCurrent`
(migration 31→32). Overloading the ceiling was rejected outright — a weight step has to put the reps back
on the floor while the ceiling survives for the next climb, and one column cannot be both. Deriving the
target from the last session's reps was rejected too: it needs no column, but it moves the plan whenever
history moves, so deleting a logged set would change what the next run asks — the stored "next target"
N16 and N50 refused. The backfill takes the range's **floor**, `from` where the plan wrote one and its
`to` otherwise, so a ranged plan's first session under the rule asks for the bottom of its range and
climbs. Starting at the ceiling was the alternative, and it would leave the rep direction unreachable
until a weight step had happened — unreachable in exactly the case the change exists to serve.

**Frozen when *Done* is tapped**, because the offer is a function of the plan and the session: the first
step written changes the plan target the offer was read from, so the same set would immediately offer
another step it has already taken. That is also why a pick is not a write, and why the picks are
view-model state rather than a `remember` — a rotation mid-answer has to reopen the question on the
answers already made. One *Done* writes them all; a failed write marks what landed and leaves the
exercise open, so a retry writes only what is left rather than a step twice.

## N75

The sore list and the exercise taxonomy were already one vocabulary — `SORE_MUSCLE_GROUPS` is derived
from `MuscleGroup.entries` — so adding adductors as a second, sore-only list would have created the
thing the derivation exists to prevent: two names for one idea, drifting the first time a movement
claimed a muscle the sore list did not know. One entry puts it in the picker, the exercise dropdown and
the search at once, and it is the same word in all three.

**Back could not simply be replaced.** Every muscle is stored by name and read back through
`MuscleGroup.valueOf`, so removing the constant does not deprecate the value — it makes every row that
still says `BACK` throw on read, and every export written before the split fail to restore with a
decoder error. Keeping it as a legacy value costs one enum entry and one filter on two lists, and it is
the shape N59 and N63 already chose for a value the app stopped offering but must still read. Excluding
it from `SELECTABLE_MUSCLE_GROUPS` is what keeps anything new from being tagged with it.

The seeded library had to move by migration rather than by editing the seed, because the seed only ever
inserts: it is an `INSERT OR IGNORE` top-up on every database open that deliberately never updates an
existing row (a REPLACE would fight the session exercises that reference the row by foreign key and
would undo a soft delete). Re-classifying the seed list therefore reaches fresh installs and nothing
else. The migration is written to touch only what the app itself wrote and only where the lifter has
not answered for it — each statement is guarded by the seeded value and keyed to the seeded ids, and
the secondary lists are rewritten token by token rather than replaced whole. A custom exercise tagged
*Back* is left alone on purpose: nothing in the row says which of the three it is, and picking one would
be exactly the guess this project refuses elsewhere.

The deadlifts are the case that shows the judgement is the lifter's rather than arithmetic: they leave
the back group for `HAMSTRINGS`, which the Romanian deadlift already carried, and the `HAMSTRINGS` in
their secondary list becomes `LOWER_BACK` — the erectors holding a heavy hinge, which is what the
retired tag was standing for.

**`CURRENT_SCHEMA_VERSION` moved to 2**, and this is the first change that needed it. Its own rule says
a bump is for a field that changes meaning *or* for a newer file carrying data an older build cannot
represent. Adding `weightUnit` did not qualify (an unknown key is ignored, the field is absent), but
adding enum *values* does: an older build's decoder throws on `LATS`, so without the bump the lifter
would be told the file "does not look like a backup" when the truth is that the app is older.

## N76

The two controls were already adjacent and already about the same moment, which is why they read wrong:
a field, then eight points under it a button, is the shape of a form. Putting the action at the end of
the rating row makes one line of the pair and costs nothing else — N69's argument for the *foot* is
untouched, since the row is still the last thing in the exercise block.

Two consequences were decided rather than discovered. The rating row's click target covered the full
width, so it had to give up the half the action needs: a full-width target under a button is a control
that swallows its neighbour. And the rating composable has a second caller in History, so the change is
a layout at this call site and nothing on the component's own surface.

Moving the action back up beside the header was the obvious alternative and is refused for N53's reason:
the header is read constantly mid-session, and the actions left it because a link in every header cost
more attention than it earned.

## N77

The step is a fact about the equipment, and equipment belongs to the movement: a leg press that adds
5 kg and a lat pulldown that adds 5 lb are the same two exercises on different machines. A plan names
*what to lift*, not how the machine is loaded, so a per-plan step would ask the lifter to restate the
same machine in every plan that uses it.

Three places read `Weight.stepGrams(unit)` — the ± buttons, the warm-up ramp and the progression offer
— and all three had the same defect, so all three read the exercise's value instead. The ramp is the one
worth arguing: leaving it on the unit's step would build a ramp of weights the machine cannot select,
which is a plan nobody can follow and the same wrong answer one screen over. Nothing else changed,
because the plan editors type a load rather than stepping one.

It is deliberately **not** an ambient, unlike the unit (N64). The unit is display configuration every
screen needs; the step is a property of the movement a screen is showing, so it rides on the row the
session and the plan already join from the library, and the pure functions keep taking it as an
argument. Making it a setting was rejected: there is no app-wide step to have, since the machine varies
by movement. Deriving it from the equipment enum was rejected as a guess — "machine" does not say
whether the stack jumps by 2.5 or by 10.

A step of zero is refused rather than stored. It is not a small step but no step: the ± buttons would
move nothing, and the ramp divides by the step when it rounds, so zero is a crash rather than a
preference.

## N78

The tab bar is off the logger by N34's decision, and that decision is about *accidental* exit: a bar
under a thumb that is mid-set is an invitation to lose the session. The overflow is the opposite kind of
control — a deliberate step to reach it — so putting the two plan screens there keeps N34's argument
whole while removing the part that was never argued: that a lifter could not *look* at a plan without
ending a workout.

Home's links were hidden for the same reason the bar was, one screen over, and they are the same denial:
the session is open either way, so which screen you are standing on changes nothing about it. They stay
now, and the pill below them still says *Resume*, so the primary act has not moved.

**N87 reverses half of it, and only half.** The overflow entry was never the thing that made the look
possible: home's links do, and they stay visible while a session is open. So mid-workout the overflow
goes back to being a logger's control — the *Discard* it held before N78 — and the two plans are reached
where the decision to train is taken, the start bar. The reversal is recorded here rather than in a fresh
section because N78's own argument is what it keeps: the tab bar stays off the logger, starting is still
withheld in the templates list, and looking at a plan still does not require finishing a workout — one
step back to home rather than a menu inside the session.

Two things follow from the menu holding one entry. **The ⋮ is drawn only when there is something to
discard**, since an empty session's discard is prompt-free and lives in the body (N41) and a menu that
opens on nothing is a control that lies. And the two tags the entry used go with it, because the project
deletes an API the moment nothing calls it.

**B43's rule survives narrowed rather than being dropped.** It withheld a second way to *start* a
workout while one was running, because a start that lands in an existing session is a way to lose one.
Starting is idempotent — `startOrResumeSession` returns the open session — so a live *Start* on another
template would not create a second workout, it would silently move the lifter into the one already
running from a template they were not looking at. That is why the templates list disables the button and
the row says why, rather than the screen being unreachable: the withholding belongs on the *start*, not
on the look. Making Templates a tab, or showing the tab bar on the logger, was rejected for N34's
reason.

## N87

N78 put the two plan screens in the workout's overflow and argued it from N34: the tab bar is off the logger
because a bar under a mid-set thumb loses sessions, while an overflow entry is a deliberate step. That
argument is about how a *look* is reached, and it still holds — N87 changes which look needs reaching. Home
already kept its plan links visible while a session was open, for N78's own reason, so the overflow entry was
a second door to a room that was never locked. The reversal removes the door, not the room: mid-workout the ⋮
is the logger's own business, and *Templates* and *Programs* are one step back, where the decision to train
is taken.

**What follows is that the ⋮ must stop being unconditional.** It was drawn for every open session because the
entry that had to be reachable from an empty one lived in it; with that entry gone, the only thing left is
*Discard*, and an empty session's discard is prompt-free and drawn in the body (N41). A menu that opens on
nothing is a control that lies, so its condition became the same one the entry already had. The two tags go
with the entries, per the deletion rule.

**B43's withholding is the part that did not move.** It was always about the *start*, not the look: starting
is idempotent, so a live *Start* on another template would silently move the lifter into the session already
running from a template they were not looking at. That is why the templates list disables the button and the
row says why, and it is untouched — including its reachability from home, which is now the only way in.

## N88

The row's supporting line joined the program's name and the count with a separator, and the two read as one
sentence. Splitting them is not a wrap rule: the request is about the *shape* of the row, so the count gets
its own line whatever the program is called — a one-word program name must not pull it back up. The
implementation is therefore two `Text`s rather than a flexible layout, which is also what makes the shape
assertable by position rather than by eye.

**The *today* card keeps its one line.** It is a different layout (N16, N61) and the request names the next-up
row, so the shared plural string stays shared and only one of its two call sites moved. The cost the entry
named is real and was measured: N86's gap between the row's text and its start pill is a function of how tall
that text is, so a fourth line shrinks it — the device reads 8 dp after the change, which is the number N86
asked for.

## N95

The grouping is a link because the two halves pull opposite ways, and only one of them can be a *merge*. What a
lifter needs from a family is that its rows stop fragmenting the views — muscle-group volume, "how much pressing
am I doing", the per-lift breakdown — and what they must not lose is that a paused bench is a different lift with
its own records, its own step and its own prefill. A rename merges the rows and loses the second; a merge loses
both. So the head is its own kind of row, and the whole design follows from that: it must never be offered, or a
lifter logs against a category and the records it exists to keep apart are joined; and it must be a *statistic*,
which is why it is not derived from `movementPattern` and `primaryMuscle`. Those two answer "horizontal chest
pressing", which a fly and a dip are part of; a category answers "bench press", which is the cut a lifter
actually makes. Only one of those can be computed, and it is not the useful one.

**Inheritance is live, and that is a decision rather than an implementation detail.** Copying a head's muscle
onto its children looks simpler and fails the first time a lifter renames a family: the head then describes only
the children that never disagreed with it, and the ones filed earlier keep a value nobody can see the source of.
Resolving it means one fact has one home, which is the same argument N58 makes for a template's name and the same
one a variation's muscles already rested on. The cost is that a head with an unspecified muscle — which every
*new* category is — passes that on, and a seeded movement filed under one reads *Other* until the head is filled
in. That is the honest answer under the rule rather than a bug in it: the head says nothing, so the child has
nothing to inherit, and inventing a value would be the app claiming a fact the lifter has not stated.

**Secondary muscles are the one place the child wins**, and the entry says so: they "default from the category
and are the exercise's to change". So the default applies exactly while the exercise is silent, which is why the
inheritance is a fallback on an empty list rather than an override flag — an empty list already means "I have
named none", and a second field would be a second way to say the same thing.

**A removed head still names its children**, which needed a read the list deliberately does not use: every row,
soft-deleted ones included, consulted for names and inheritance only. The list and the pickers keep reading the
observer that hides removed rows, because a removed row must not be offered. The split is the same one the soft
delete already relies on, applied to a *name* rather than to a row.

**Both transfer formats carry the shape, and neither version moves.** An added field with a correct default is
not a format change by this project's rule, and the default is load-bearing here: every row a file could have
held before categories is a movement, so an absent kind has exactly one true meaning. The opposite default would
turn an older file's whole library into heads, which are never offered. An older build reading a file with the
fields present still opens it — unknown keys are ignored and the version is read before the body (B62) — which is
why a bump would buy nothing.

## N94

The bar's order and the app's entry point were one fact while Workouts sat first, and the enum's own
doc said so: "Workouts first because it is where the app opens". The request separates them. Statistics
takes the first place, History the second, Workouts the third where Statistics was, and the tab the app
*opens* on does not move — that is `startDestination = WorkoutsHome` and `lastTab`'s default of
`AppTab.WORKOUTS`, neither of which the enum's order touches. So the app still opens on Workouts, now
the third tab selected, and the two statements stop pretending to be one.

The doc loses the rationale it can no longer hold rather than being left contradicting the order, which
is the class of defect the N80–N86 review found twice. Nothing else is keyed to the order: `forRoute`
and `switchTab` match by route, so the swap is the enum's `entries` alone. What does move with it is the
bar's read order, which follows the enum — a screen reader announces Statistics first — and that is the
part a test now asserts, because "five named tabs existed" would have passed either way.

## N90

The workout screen's undo answers "I mis-tapped" by re-logging the set, and it appends on purpose: a session
is read by what was done, so the values coming back is the whole of the promise. A plan cannot make that
promise, because a plan *is* an order. A drop run is the sharp case — `runAt` and `rungWeightAt` read a
ladder off the rows above it, so a restored rung appended at the end is either stranded or silently attached
to a different anchor, and either way the plan now says something the lifter did not write. So the restore
splices rather than appends, and the soft delete is what pays for it: the row kept its `setIndex`, so putting
it back is a matter of making room rather than of guessing where it went.

**The renumbering runs again on the way back.** `removeSet` compacts the survivors to `0..n-1` (B72), which
means the removed run's stored indexes are the only record of where it sat. The restore therefore merges the
two groups by those indexes and renumbers the result, rather than inserting at a remembered position: the
position is not remembered anywhere, and inventing one from the removal's own timestamp would be a second
source of truth for the same fact. This is also why the undo holds nothing but the set: the repository can
answer "what did this removal take" from the row itself.

**`deletedAt` is the correlation id, and that was chosen over a column.** A run's rungs share no other mark —
they are consecutive rows with no parent pointer — so "which rows went together" has to come from somewhere.
A `removedWith` column would be a schema change, a migration and a second fact to keep true, for a value that
already exists: every row a single removal hid carries that removal's own timestamp. The dependency is real
and it is recorded here — a fixed clock would make every removal on a device one run — and the instrumented
suite moves its clock between removals to hold it. The alternative, restoring every hidden row of the
exercise, was rejected outright: it puts back a set the lifter deleted deliberately a minute earlier, which
turns one undo into a different, wrong write.

**The second capability cost the DAO one query and the repository one override**, and both thresholds in
`detekt.yml` moved by one rather than a type being split. A restore is the only reader that must see a
soft-deleted row, so it cannot be folded into the live queries — that is what "soft delete" means — and a
`TemplateUndoDao` would own half of one table to serve one call. The note beside the thresholds names both
and why, which is the same convention the earlier moves followed.

## N91

The block grew by accretion and each addition was individually right: N14's targets, N79's plan lines,
N80's own line for the cue, N81's sets on the block and *Add set* at its foot. The result was that the
screen's one scanning surface — the list of names — was the smallest thing on it. Folding is the change that
gives the names back without taking any of those away, which is why it folds *the whole block* rather than
just the fields: a half-folded block would still be a scroll of controls with one name at the top.

**The name is the control, and the ⋮ is not.** A chevron or an *Open* button beside the name would be a
smaller tap target for the thing the lifter aims at anyway, and the row already carries a menu whose entries
act on the exercise as a whole — move, superset, remove, *Add warm-ups* — so folding through the menu would
have made the name's own obvious gesture a two-tap detour. The consequence is the accessibility work: the
label names the action ("Open Back Squat", "Fold Back Squat") and a `stateDescription` reports which it is,
because an identical name that does different things depending on invisible state is exactly what a screen
reader cannot discover by itself.

**`rememberSaveable`, and both halves of the state saved.** N84 settled the rule this follows: the activity
declares no `configChanges`, so a mode the user chose must survive a rotation. The second half is subtler and
was a bug on the way: the state has to distinguish "the lifter folded this" from "this block has never been
drawn", because those two are what tell a pre-existing block from an added one — and a fold map with only one
flag re-derives the wrong answer for whichever case it is not tuned for. An exercise added after the screen
opened is absent from the map, which is the signal that it opens.

**A `mutableSetOf()` inside a state object does not work, and the reason is worth recording.** Compose
compares state by `equals`, and a set mutated in place is still `equals` to itself, so the write is not a
change: the tap ran, the set changed, and nothing recomposed — the block stayed exactly as it was. The state
is therefore an immutable map replaced on every toggle, which costs a copy of a handful of pairs per tap and
buys a fold that actually folds.

**The shared state moved to `ui/components/RowFold.kt`, and two helpers left the screen.** The editor file
and its longest function are both at the ceiling detekt enforces, and the honest answers were a type that is
about *a list and a per-row flag* rather than about templates, plus a warm-up entry and a row menu that read
on their own. The `TestTags` object ceiling moved by one for the same reason as its three previous moves: a
row a test has to tap is one more per-id helper in one flat namespace.

## N92

The problem is reach, not visibility. Every program was already on the screen; the one a lifter opens with
one program authored sat first, under the app bar, which on a modern phone is the corner a thumb cannot
reach without regripping, while the *New program* FAB occupied the only reachable spot — an action that
makes a *second* program, offered to someone who came to open their first. So the active one moves to the
foot rather than the list being sorted: sorting would make the reference above it agree with a reach
decision, and the authored order is the user's (P3.12).

**The card is a bottom bar rather than a last list row**, and the difference is what it does under scroll: a
row scrolls away, so with a long list the reachable thing is reachable only until the list is scrolled, which
is exactly when it is least findable. The scaffold's `bottomBar` is fixed, and it also settles the collision
with the FAB for free — Material places a floating action above a bottom bar.

**The button was not moved into the bar.** That is the tempting simplification: one row, card and button
side by side. It fails on width, because the card's share becomes a function of the button's label and the
user's font scale, and a card whose width is a computed guess about another control overflows or truncates
rather than simply crowding. The FAB is also the shape this app gives "the one action this screen makes", and
demoting it to a bar entry would spend that meaning to save a row of height.

**One active program is shown when several are.** P3.12 allows more than one, so "the active program" is not
a unique thing; the card shows one of them and the list still carries every one, with its own *In use* label,
so nothing became unreachable and no case the single-program user has is generalised into a rule for the
others. With none active there is no card, because a card for nothing would be a control that cannot do
anything (N53, N67).

## N93

The two screens are siblings under one idea — a plan, and the schedule that orders it — and the only path
between them was back home and in. Each bar already carries the actions that belong to its own screen
(Programs' *Load*, N47), and this is a place to go rather than an action on what is listed, so it sits with
them rather than floating over the list.

**It deliberately says nothing about a running workout.** B43's withholding belongs on a *start*, not on a
look (N78's distinction), and this entry starts nothing: the templates list's rows still disable *Start* while
a session is open, so a way across cannot become a third way to begin one. Hiding the entry instead would
have been the roundabout version of the same rule, applied where it does not belong.

## N79

A drop set and a cluster set are the same shape once the app stops treating them as sets of their own:
a run of sets that hangs off a working set. Naming them apart was the user's decision and it is
load-bearing rather than cosmetic — **the two progress by different rules**, and a role is what a rule
hangs on. A drop's load is *derived* (you get 80 by stripping the top set's bar), so a load step on it
is meaningless and the parent is what progresses; a cluster's rungs carry the parent's load, so they
move with it and the group's judgement is what decides.

**The value is stored, not the weights.** Keeping a weight per rung was the obvious alternative and it
fails twice: it drifts from the anchor the moment the anchor is edited or progressed, and "progress all
of them by the same weight" then needs a multi-row write with nothing to keep the rows honest. Storing
one value per run makes the ladder a function — 100 with 20 off is 80, then 60 — so accepting a step
writes one row and no rung can disagree with its anchor. It also settles the prefill: a rung's weight is
*computed*, which is why it is not "what you did last set" (for a cluster the two coincide; for a drop
the second rung is 60, not 80), and why it is computed from what the anchor **actually did** — a drop is
taken off the bar in front of you, so 20 kg off a set loaded 5 kg heavy is 85, not 80. A cumulative
ladder rather than a fixed rung weight was also the user's call, and it is the shape a lifter strips in.

**Null is the answer wherever the arithmetic cannot be done**, and that is a correctness rule rather
than defensiveness: this app stores a signed load, so a negative weight is not a small weight but
assistance (N15). A rung computing to −20 would have become "20 kg of help" — a silently corrupted set —
so an exhausted ladder, an anchor with no added weight, a run naming no value and a plan that begins
with a rung all read as *carrying no derived weight*, and the caller falls back to what the row itself
holds. Legacy reading is the same idea: a `Drop` row written before this keeps its absolute weight, and
a file that leads with a rung reads it as a plain set, so nothing is invented and nothing fails to load.

**One rating per group** falls out of `recordsEffort` rather than a new rule, which is why it is one
property with two reasons: a warm-up is preparation, a rung is work that is not rated separately, and
both answers are "this set does not stand as a performance of its own". The record scan takes the same
predicate — a rung cannot set a record — while **volume is untouched**, because it already sums every
set and every rep. A rating already stored on a drop row stays on disk and stops being displayed, the
shape the other legacy columns have.

**Judged on the first set** is the user's rule for both kinds, and its consequence is recorded rather
than discovered: **a rung cut short does not hold the group back**. That is coherent precisely because a
rung has no target of its own to miss — the group's target is the anchor's, and the rungs are what was
done. I argued for judging a cluster on every rung, on the grounds that a rung carries the same load and
its reps are a real target; the user overruled it for symmetry with drops, and the trade is written down
here so the next person does not read it as an oversight.

**The pairing fix is part of this rather than a follow-up.** The Done prompt counted non-warm-up sets by
position while the prefill counted every logged row, so a drop logged after a working set shifted every
later pair: the app judged the *second* prescribed set against the drop's ten reps against a five-rep
ceiling, offered it a heavier weight for it, and accepting wrote that into the plan. That is a wrong
write, not a wrong label, which is why it ships here. Pairing is now by class — prescribed work sets
with performed work sets, prescribed rungs with performed rungs, each in order — and the limit that
remains is named rather than implied: within a class it is still positional, so a **skipped** prescribed
set shifts the pairs the way an extra one used to. Closing that needs an identity a logged row does not
carry, and it is its own id if it is wanted.

**Rest needed no field.** The exercise's rest already runs once, and a superset round already suppresses
it until the round closes, so the run reuses that shape: the anchor does not rest, the run's last rung
does. A run added live rather than planned still rests after the anchor, which is the honest reading of
a plan that does not name it, and *Skip rest* is on screen.

Rejected: a percentage of the anchor (a concept this app does not have), a per-rung rating (the group is
one effort), per-rung rep targets (nothing would measure them), reusing `Drop` for both kinds (they
progress differently), and treating a cluster as the intra-set meaning the literature usually gives the
word — the user chose it for a group of separately logged same-weight sets, and inside this app that is
what it means.

## Truth, Turbine

New and touched tests assert with Truth, and assert Flow sequences with Turbine.

JUnit's `assertEquals(expected, actual)` puts two bare values side by side with nothing
saying which was which, and the arguments are easy to swap; `assertThat(actual).isEqualTo(expected)`
cannot be written the wrong way round. Turbine is for what a polled `.value` cannot express
at all: a *sequence* of emissions, or an invariant that has to hold across one.

Both are JVM-only (`testImplementation`), and the core Truth artifact rather than
`truth-android`, which would pull androidx.test stubs alongside Robolectric's. Adopted after
the review, so most files still use JUnit: they migrate **as they are touched**, never in a
sweep, and a file is not left half-converted. This is a preference about failure messages,
not a correctness gate — do not spend a change on migration alone.

## Releases

Releases are manual, and CI cannot attach an artifact: it builds a release APK to prove R8
succeeds but signs nothing, so automating steps 4–7 of the procedure would mean putting
`workout.jks` **and its passwords** into GitHub Secrets.

**Decided against, for now.** The automation has a genuine engineering benefit — CI building
from the tag would make the tag/APK mismatch impossible *by construction*, rather than merely
documented — weighed against moving a permanent signing key into a third party's store, where
any workflow in this repository could reach it, for an app released a few times a year. Ten
documented minutes is the cheaper side of that trade.

If the cadence ever changes, do it as a **GitHub Environment secret with required reviewers**, so
a human approves before any job can read the key. A raw repository secret readable by any
workflow is the version that should stay unbuilt.

## B59-B72

A review of the N74–N79 batch, read line by line before its release. Fourteen defects, four of
which were judgement calls rather than slips; the rest are recorded in the CHANGELOG and their tests.

**Grandfathering an existing row, or backfilling it (B59).** The plan boundary re-checks every rung rule
over every stored row, and migration 34→35 leaves a pre-N79 drop run's value null — so the rules refused
a plan the app could still read, on any edit at all, with the error naming a run while the lifter edited
another row. Two answers looked available. **Backfill a value in the migration:** rejected, because there
is no number it could write that the lifter meant. Any constant invents a load, moves a ladder nobody
asked to move, and — worst — becomes indistinguishable from a value the lifter did author; a nullable
column with a defined reading ("this run names no value, so its rungs carry what they carry") is the
honest shape, and the read path already implements it. **Grandfather by set id and by problem sentence:**
kept. An untouched row is left as wrong as it was, a new row is judged on its own, and a write that makes
an existing row worse or moves the problem elsewhere is still refused — so the rule still binds every
write the app can *cause*, while a plan written before the rule stays editable and correctable. The
comparison is by the problem's own sentence rather than by a boolean, because "still exactly as broken"
and "broken in a new way" are different events.

**Delete a run with its anchor, or promote the first rung (B60).** DECISIONS had recorded "deleting an
anchor takes its rungs with it" since the shape was planned, and no code held it: `removeSet`
soft-deleted one row and left a run standing over a gap. Promoting the stranded first rung to a set of its
own is the alternative that preserves the lifter's work, and it is the one to reject: a rung deliberately
names no reps and no weight, so promotion invents both, and invented numbers are indistinguishable from
authored ones. Deleting is what the decision says and what the shape can defend.

**Which message a newer file gets (B62).** The version gate existed and the version was bumped, and the
message still could not arrive: both codecs decoded the document first, and a newer file dies *inside*
the decoder on an enum name this build lacks — the exact data the gate is for. Reading the version out of
the JSON tree costs one parse of the same text and turns one answer ("corrupt") into the two the format
actually has. The program document was the second telling: it carries the exercise DTOs and planned sets,
so the split muscle names and `SetType.CLUSTER` travel in it, and its version had been left at 1 when
they were added. A bump cannot repair an already-released build's ordering — only this one's — but it
makes the contract true from here, and every document v1.14 *can* read is now refused with the right
sentence. The old tests missed both because they built the "future" file from a sample whose enum values
were all known, which exercises the branch that already worked.

**One index space, or two (B72).** A plan is matched to a running session by position: the prefill pairs
the next logged set with `setIndex == loggedSets.size`, and N79's rest rule asks whether the plan row at
that position continues a run. Removing a *middle* planned set left a gap, so "the number of sets logged"
and "the plan's own index" stopped agreeing, and the same set could be prefilled from one row and rested
against another. Renumbering the survivors makes position mean the plan's order and nothing else, which is
the invariant `prependSets` already holds from the other direction. The assumption is older than N79 — the
prefill leaned on it — but N79 was the first rule where getting it wrong changed what the screen *does*,
which is what made it worth fixing rather than tolerating.
