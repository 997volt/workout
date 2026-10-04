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
