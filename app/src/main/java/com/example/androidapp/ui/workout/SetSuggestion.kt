package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.SlotPrescription
import com.example.androidapp.domain.model.SlotSet
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.TemplateExercise
import kotlin.math.roundToLong

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
        // The upper bound is the one a written plan means (`max 2`).
        PlannedTarget(
            reps = set.targetRepsMax ?: set.targetRepsMin,
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
 * The weight a percentage of an estimated one-rep max prescribes, or null (ROADMAP P3.8).
 *
 * The estimate is N17's Epley figure. An exercise with nothing estimable has **no number**, and
 * this says so by returning null rather than borrowing one — the caller then falls back to
 * history, which is the honest answer. The result is rounded to the smallest loadable step
 * (N22's 2.5 kg), because a prescription is a target to load rather than a formula's output.
 */
fun prescribedWeightGrams(percentOf1Rm: Int, estimatedOneRepMaxGrams: Long?): Long? {
    val estimate = estimatedOneRepMaxGrams?.takeIf { it > 0L && percentOf1Rm in 1..MAX_PERCENT }
        ?: return null
    val raw = estimate.toDouble() * percentOf1Rm / PERCENT
    return (raw / Weight.DEFAULT_STEP_GRAMS).roundToLong() * Weight.DEFAULT_STEP_GRAMS
}

/**
 * What a slot prescribes for the next set of one exercise, merged with the template (ROADMAP P3.8).
 *
 * The slot wins **where it speaks** (N14), field by field: a set that writes reps but no load keeps
 * the template's load, and one that writes only a note leaves the template's target standing rather
 * than shadowing it with nulls. That is what "anything you leave alone uses the workout's own
 * targets" promises on the dialog.
 *
 * The **load is one number**, though, so it is taken whole from one source or the other: a slot that
 * names 100 kg must not also inherit the template's assistance, or the set would count the kilograms
 * as volume while the machine did the work (N15). [template] is the template's own target for the
 * same set, or null when it has none.
 *
 * The **target RPE is one number for the whole exercise** (N59, amended), so the slot's own value
 * stands over the template's even for a set the slot prescribed no row for: an exercise-level target
 * is not per-set, and a set's own stored value is only the fallback a pre-change plan arrives with.
 *
 * A percentage resolves through [prescribedWeightGrams], so an exercise with no estimate leaves the
 * load open and, with no template load to fall back to either, the prefill takes history rather than
 * inventing a number.
 */
fun prescribedTargetFor(
    prescription: SlotPrescription?,
    nextIndex: Int,
    estimatedOneRepMaxGrams: Long?,
    template: PlannedTarget? = null,
): PlannedTarget? {
    val prescribed = prescription?.sets?.firstOrNull { it.setIndex == nextIndex }
    // The slot's one RPE covers the whole exercise, so a set it wrote no row for still takes the
    // slot's number over the template's rather than quietly keeping the template's (N59). With no
    // slot RPE either, the slot says nothing about this set and the template stands whole.
    val slotRpe = prescription?.targetRpeHalves
    return when {
        prescribed != null -> mergedTargetFor(prescribed, slotRpe, estimatedOneRepMaxGrams, template)
        slotRpe != null -> (template ?: PlannedTarget(reps = null, weightGrams = null))
            .copy(rpeHalves = slotRpe)

        else -> template
    }
}

/**
 * A prescribed set merged with the template's targets, field by field (N14, P3.8, N59).
 *
 * Its own function so [prescribedTargetFor] reads as the one decision it is — whether the slot speaks
 * at all — rather than as the merge arithmetic too.
 */
private fun mergedTargetFor(
    prescribed: SlotSet,
    slotRpe: Int?,
    estimatedOneRepMaxGrams: Long?,
    template: PlannedTarget?,
): PlannedTarget {
    val percentWeight = prescribed.targetPercentOf1Rm?.let {
        prescribedWeightGrams(it, estimatedOneRepMaxGrams)
    }
    val slotWeight = prescribed.targetWeightGrams ?: percentWeight
    val slotNamesLoad = slotWeight != null || prescribed.targetAssistanceGrams != null
    return PlannedTarget(
        // The upper bound is the one a written prescription means (`max 2`).
        reps = prescribed.targetRepsMax ?: prescribed.targetRepsMin ?: template?.reps,
        // A weight the slot wrote wins over a percentage; the two are alternatives, not a sum.
        weightGrams = if (slotNamesLoad) slotWeight else template?.weightGrams,
        assistanceGrams = if (slotNamesLoad) {
            prescribed.targetAssistanceGrams
        } else {
            template?.assistanceGrams
        },
        // The slot's own role wins wherever the slot speaks at all: its sets carry the plan's
        // vocabulary and default to a working set, so there is no "left alone" to fall back for (B48).
        role = prescribed.role,
        // The slot's one RPE where it names one, the set's own where only that was written, and the
        // template's where the slot is silent (N59, N14).
        rpeHalves = slotRpe ?: prescribed.targetRpeHalves ?: template?.rpeHalves,
    )
}

/** Above this, a "percentage of the max" is no longer a percentage of a max. */
private const val MAX_PERCENT = 100

/** A percentage as the fraction of the estimate it names. */
private const val PERCENT = 100.0

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
