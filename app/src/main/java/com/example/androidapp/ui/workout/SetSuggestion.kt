package com.example.androidapp.ui.workout

import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.rungLoadAt

/**
 * The values the next set is shown with, before the lifter changes anything (ROADMAP N59).
 *
 * These are what the workout screen's **Log set** commits, so the rule for choosing them is "what did I
 * do, or what does the plan say" — never "what does the app think I should do next" (ROADMAP N33). The
 * fields that show them *are* the editor (N59), so there is nothing separate to accept: what is on
 * screen is what is written, and disagreeing with it is editing a field.
 */
data class SetSuggestion(
    val reps: Int,
    val weightGrams: Long,
    /** The assistance to prefill, or 0 for none (ROADMAP N15). */
    val assistanceGrams: Long = 0,
    /**
     * The role the pending set is armed with (ROADMAP B48).
     *
     * The plan's next unlogged set decides it, carried the way reps and weight already are: a
     * template that opens with a ramp would otherwise record its warm-ups as working sets, which
     * both inflates volume and can set a personal record against a bar nobody cleared. With no
     * plan — or a plan that names nothing for this set — the old default stands, and the picker
     * beside the fields still overrides it for the one set.
     */
    val setType: SetType = SetType.NORMAL,
    /**
     * What the plan asks this set to feel like, in half-points, or null (ROADMAP N59).
     *
     * **The value the RPE stepper opens on.** N59 first kept it beside an optional field so a
     * prescription could not be recorded as a measurement; it is now written into the field, because
     * the lifter reads the plan's own number and changes it when the set felt different — what is
     * recorded is still what the set was. A plan that names none leaves the field on 9.0.
     */
    val targetRpeHalves: Int? = null,
)

/**
 * What a plan prescribes for one set, or nulls where it prescribes nothing
 * (ROADMAP N14).
 *
 * Every field nullable, because a plan may say "work up to a heavy single" and mean
 * it: there is no weight to prefill, and a zero would be a claim.
 */
data class PlannedTarget(
    val reps: Int?,
    val weightGrams: Long?,
    /** The assistance the plan prescribes, or null (ROADMAP N15). */
    val assistanceGrams: Long? = null,
    /** The role the plan gives this set, or null when there is no plan (ROADMAP B48). */
    val role: SetType? = null,
    /** The RPE the plan asks for, in half-points, or null (ROADMAP N59). */
    val rpeHalves: Int? = null,
)

/**
 * Chooses what the next set is shown with (ROADMAP P1.3, N14, N59).
 *
 * **The prefill is history, not a proposal.** A plan's target for *this* set wins where it says
 * something — a ramp of 100/105/110 kg only works if the second and third sets take their numbers from
 * the plan — then what you just did in this session, then **what you did last time, unchanged**, and only
 * then a default. The plan's RPE is carried the same way (N59's reversal): it fills the RPE stepper,
 * which the lifter changes when the set felt different rather than starting from a blank field.
 *
 * Pure, so the precedence is covered by fast JVM tests rather than by tapping.
 */
fun suggestionForNextSet(
    loggedSets: List<SetRow>,
    previous: PreviousPerformance?,
    planned: PlannedTarget? = null,
    /**
     * What **this same set** did in the previous training, or null (ROADMAP N79).
     *
     * Read only for a rung. A rung carries no rep target of its own — the group's is the anchor's — so
     * "what you just did" would answer with the anchor's reps, and for a drop that is the wrong number
     * entirely: last time's 60 kg rung at 10 reps is a better guess for this one than the 5 the bar
     * above it just did. Null where the set has never been performed, which leaves the old fallback.
     */
    sameSetLastTime: SetEntry? = null,
): SetSuggestion {
    // "What you just did" beats "what you did last time": within a session the set before is the best
    // evidence there is.
    val logged = loggedSets.lastOrNull()
    val lastTime = previous?.sets?.lastOrNull()
    val prefill = when {
        logged != null -> SetSuggestion(logged.reps, logged.weightGrams, logged.assistanceGrams)
        lastTime != null -> SetSuggestion(
            reps = lastTime.reps,
            weightGrams = lastTime.weightGrams,
            assistanceGrams = lastTime.assistanceGrams,
        )

        else -> SetSuggestion(reps = DEFAULT_REPS, weightGrams = Weight.DEFAULT_GRAMS)
    }

    // A plan's load is *one* number: `-20` is 20 kg of help and no added weight, and `100` is 100 kg and
    // no help. Taking the half it names and the other half from the fallback would build a set that is
    // both — which would count the default 20 kg as volume on an assisted set, the exact corruption a
    // signed weight was rejected for (ROADMAP N15).
    val plannedLoad = planned?.let { plannedLoadFor(it) }

    // A rung has no target to answer to, so its reps come from the same set's last performance where
    // there is one, and from what was just done where there is not (ROADMAP N79).
    val rungReps = if (planned?.role?.isRung == true) sameSetLastTime?.reps else null

    return SetSuggestion(
        // The upper bound is the one that matters in a written plan (`max 2`).
        reps = planned?.reps ?: rungReps ?: prefill.reps,
        // A rung whose load cannot be derived — the run names no value, or the anchor has no added
        // weight to take one off — falls back to what was just done, deliberately (ROADMAP B71). The
        // plans refuse to *author* that shape, so this is a row written before the rules or an assisted
        // anchor, and in a live session the lifter may have taken the plates off by hand: the number is
        // a starting point they edit, not a claim the app derived. The plan's own row says why it has
        // none; here the fields must hold something, because what is on screen is what *Log set* writes.
        weightGrams = plannedLoad?.weightGrams ?: prefill.weightGrams,
        assistanceGrams = plannedLoad?.assistanceGrams ?: prefill.assistanceGrams,
        // The armed role follows the plan, so a template's ramp is recorded as warm-ups without a
        // tap per set (B48); with no plan the pending set stays a working set.
        setType = planned?.role ?: prefill.setType,
        targetRpeHalves = planned?.rpeHalves,
    )
}

/**
 * Which roles the next set may be logged as (ROADMAP N79, B63, B64).
 *
 * The same rule the write boundary holds, asked **before the tap** so the picker offers only what can
 * be saved: a run is contiguous and its rows share a role, so a rung's anchor is the set above the
 * run's first row, and that set has to stand on its own. A warm-up does not stand on its own, which is
 * why the first set of an exercise cannot be a rung and neither can one that follows a ramp — and the
 * pending set is always appended, so the walk starts at the end of this list.
 *
 * The boundary asks the same question of the same rows in `RoomWorkoutRepository.canBePerformedAs`;
 * this side answers the picker, and both read `recordsEffort` as the one rule that says what an anchor
 * is. A role that is not a rung is always available: it is what the set is when it is not a member.
 */
internal fun List<SetRow>.canLogAs(role: SetType): Boolean = refusalForLoggingAs(role) == null

/**
 * Why this role cannot be logged here, or null when it can (N145).
 *
 * The rule is one sentence, but it is not one *fault*: the set above may be missing entirely, may be a warm-up,
 * or may already be a rung of the other kind — and the boundary used to answer all three with "A drop set
 * follows the set above it", which tells the lifter what a drop set is rather than what is wrong with the place
 * they tapped. The three are told apart here so the boundary can say which one it is.
 *
 * File-level and pure, like [canLogAs], because it is a question about rows: the session's list answers it, and
 * `RoomWorkoutRepository` asks the same one of the same rows.
 */
internal fun List<SetRow>.refusalForLoggingAs(role: SetType): RungRefusal? {
    var start = size
    while (start > 0 && this[start - 1].setType == role) start--
    val above = getOrNull(start - 1)
    return when {
        !role.isRung -> null
        above == null -> RungRefusal.NothingAbove
        above.setType.recordsEffort -> null
        above.setType.isRung -> RungRefusal.ARungAbove
        else -> RungRefusal.OnlyAWarmUp
    }
}

/**
 * Why a rung cannot go where it was asked for (N145).
 *
 * Three causes, three sentences: a lifter who is told *which* one it is can act on it — move the set, change the
 * role above, or log the working set first — where being told the rule again leaves them to work it out.
 */
internal enum class RungRefusal {
    /** The set is the exercise's first, so there is nothing to hang a run off. */
    NothingAbove,

    /** The set above is a warm-up, which no run reads an anchor from (B63). */
    OnlyAWarmUp,

    /** The set above is a rung of the other kind, and a run's anchor has to stand on its own. */
    ARungAbove,
}

/**
 * The sentence a refusal is reported with (N145).
 *
 * Built here rather than read from a resource because the write boundary is a repository and has no
 * `Context` — the same reason every other message it throws is a string. The label follows the role the lifter
 * asked for, which is the one thing the sentence has to name.
 */
internal fun RungRefusal.sentence(role: SetType): String {
    val name = role.label.lowercase()
    return when (this) {
        RungRefusal.NothingAbove ->
            "A $name set needs a working set above it, and this is the exercise's first set."

        RungRefusal.OnlyAWarmUp ->
            "A $name set needs a working set above it, and the set above this one is a warm-up."

        RungRefusal.ARungAbove ->
            "A $name set needs a working set above it, and the set above this one is another kind of rung."
    }
}

/**
 * What one entry of the plan prescribes for the next set of its exercise (N14, N54).
 *
 * Takes the entry the caller has **already resolved** to this exercise rather than a position: the
 * session's order is its own once N54 lets it be edited, so a position stopped being a name for a
 * plan entry, and matching on it pointed a moved exercise at its neighbour's targets. Pairing a
 * session's rows with the plan's is [planEntriesFor]'s one job, and it matches by movement instead.
 * A plan that has nothing to say about this set — or no entry at all — is null, which leaves the
 * older rule below to decide.
 */
fun plannedTargetFor(
    planned: TemplateExercise?,
    nextIndex: Int,
    /**
     * What this set's *anchor* actually loaded, or null (ROADMAP N79).
     *
     * A drop is taken off the bar in front of you, so a rung's load is derived from what the anchor did
     * in this session rather than from the plan's number for it — which is also why this is a parameter
     * here: the plan alone cannot answer it.
     */
    anchorLoad: Load? = null,
): PlannedTarget? {
    val sets = planned?.sets.orEmpty()
    val index = sets.indexOfFirst { it.setIndex == nextIndex }
    val set = sets.getOrNull(index) ?: return null

    // A rung carries no targets of its own (ROADMAP N79): its load is derived, and its reps are the
    // caller's business — the same set's last performance — so both are answered as absent here.
    return if (set.role.isRung) {
        // **Both halves of the derived load** (B153): a cluster rung repeats the anchor's load assistance
        // included, so handing on only its weight read a machine-assisted anchor as a stated 0 kg —
        // `plannedLoadFor` reads a non-null 0 as *the plan said 0*, so the 20 kg the machine was giving never
        // reached the field and the set was logged as bodyweight. A drop rung is unchanged: its ladder exists
        // only on an anchor with added weight, so it still derives a weight and no assistance.
        val derived = sets.rungLoadAt(index, anchorLoad)
        PlannedTarget(
            reps = null,
            weightGrams = derived?.weightGrams,
            assistanceGrams = derived?.assistanceGrams,
            role = set.role,
            rpeHalves = null,
        )
    } else {
        PlannedTarget(
            // The number the lifter has climbed to, or the range's floor where progression has not moved
            // it yet (ROADMAP N74) — the upper bound is only what a plan that wrote no floor means.
            reps = set.targetRepsCurrent ?: set.targetRepsMin ?: set.targetRepsMax,
            weightGrams = set.targetWeightGrams,
            assistanceGrams = set.targetAssistanceGrams,
            // A planned set's role travels with its targets, so the ramp is armed, not retyped (B48).
            role = set.role,
            // The exercise's one target RPE is what the stepper opens on (N59).
            rpeHalves = planned?.targetRpeHalves,
        )
    }
}

/** Typical working-set reps when there is nothing to go on. */
const val DEFAULT_REPS = 8

/**
 * The load a plan names, as the split the app stores.
 *
 * A plan's load is *one* number: `-20` is 20 kg of help and no added weight, and `100` is
 * 100 kg and no help. Taking the half it names and the other half from the fallback would
 * build a set that is both — which would count the default 20 kg as volume on an assisted
 * set, the exact corruption a signed weight was rejected for (ROADMAP N15).
 */
private fun plannedLoadFor(planned: PlannedTarget): Load? = when {
    planned.assistanceGrams != null && planned.assistanceGrams > 0L ->
        Load(weightGrams = 0L, assistanceGrams = planned.assistanceGrams)

    planned.weightGrams != null -> Load(planned.weightGrams, assistanceGrams = 0L)

    else -> null
}
