package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.TemplateExercise

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

    return SetSuggestion(
        // The upper bound is the one that matters in a written plan (`max 2`).
        reps = planned?.reps ?: prefill.reps,
        weightGrams = plannedLoad?.weightGrams ?: prefill.weightGrams,
        assistanceGrams = plannedLoad?.assistanceGrams ?: prefill.assistanceGrams,
        // The armed role follows the plan, so a template's ramp is recorded as warm-ups without a
        // tap per set (B48); with no plan the pending set stays a working set.
        setType = planned?.role ?: prefill.setType,
        targetRpeHalves = planned?.rpeHalves,
    )
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
): PlannedTarget? = planned
    ?.sets
    ?.firstOrNull { it.setIndex == nextIndex }
    ?.let { set ->
        // The number the lifter has climbed to, or the range's floor where progression has not moved
        // it yet (ROADMAP N74) — the upper bound is only what a plan that wrote no floor means.
        PlannedTarget(
            reps = set.targetRepsCurrent ?: set.targetRepsMin ?: set.targetRepsMax,
            weightGrams = set.targetWeightGrams,
            assistanceGrams = set.targetAssistanceGrams,
            // A planned set's role travels with its targets, so the ramp is armed, not retyped (B48).
            role = set.role,
            // The exercise's one target RPE is what the stepper opens on (N59); a set's own value is
            // only the fallback for a plan written before the effort moved to the exercise.
            rpeHalves = planned.targetRpeHalves ?: set.targetRpeHalves,
        )
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
