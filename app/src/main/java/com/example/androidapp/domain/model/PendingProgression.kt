package com.example.androidapp.domain.model

import com.example.androidapp.domain.WeightUnit

/**
 * The question *Done* froze, and the lifter's answers to it (ROADMAP N74).
 *
 * A snapshot rather than state read back from the plan, and it has to be: the offer is computed from
 * the plan's target and the session's work, so the moment a step is written the same set would offer
 * another one. Freezing the prompt when *Done* is tapped is what makes a per-set choice mean
 * anything, and it also lets the lifter change their mind before anything is written — a selection is
 * not a write, and only the confirming *Done* is.
 *
 * Nothing here touches storage: [chosen] is what the ViewModel writes, one planned set at a time.
 */
data class PendingProgression(
    val sessionExerciseId: String,
    val exerciseName: String,
    /** The unit this exercise is read in, carried so the prompt reads it the way the screen does. */
    val unit: WeightUnit,
    val sets: List<PendingSet>,
) {
    /** One prescribed set as the frozen prompt holds it, and what the lifter chose for it (N74). */
    data class PendingSet(
        val prompt: ProgressionSetPrompt,
        /** The direction the lifter picked, or null while they have picked none (N74). */
        val direction: ProgressionDirection? = null,
        /** True once the step has been written, so a second *Done* cannot write it twice (N74). */
        val applied: Boolean = false,
    ) {
        /** The planned set this row is about, or null for logged work the plan does not name. */
        val planId: String? get() = prompt.planned?.setId

        /** True while this row can still take [direction]: it offers it and has not taken it. */
        fun offers(direction: ProgressionDirection): Boolean {
            val offer = prompt.offer
            return !applied && when (direction) {
                ProgressionDirection.LOAD -> offer?.load != null
                ProgressionDirection.REPS -> offer?.reps != null
            }
        }
    }

    /** The steps the lifter has picked and not yet had written, in the plan's own order (N74). */
    val chosen: List<ProgressionChoice>
        get() = sets.mapNotNull { set ->
            val offer = set.prompt.offer ?: return@mapNotNull null
            val direction = set.direction ?: return@mapNotNull null
            ProgressionChoice(offer, direction)
        }

    /** True once every picked step has been written, so the caller can close the question. */
    val isSettled: Boolean get() = chosen.isEmpty()

    /**
     * The one reason every prescribed set missed for, or null (ROADMAP N226).
     *
     * Read from the frozen rows rather than stored: the rule belongs to the prompt the question was built
     * from, and a copy here would be a second place for one answer to live.
     */
    val sharedMiss: ProgressionSetPrompt? get() = ProgressionPrompt(sets.map { it.prompt }).sharedMiss

    /** How many rows could take [direction] — what the bulk control is offered against (N74). */
    fun offering(direction: ProgressionDirection): Int = sets.count { it.offers(direction) }

    /**
     * Picks [direction] for one set, or takes the pick back when it is the one already there.
     *
     * One direction per set: a lifter cannot both add a rep and add a weight to the same set in one
     * answer, and a row that offers neither ignores the tap rather than recording an impossible choice.
     */
    fun toggle(setId: String, direction: ProgressionDirection): PendingProgression =
        copy(
            sets = sets.map { set ->
                if (set.planId != setId || !set.offers(direction)) {
                    set
                } else {
                    set.copy(direction = direction.takeIf { set.direction != direction })
                }
            },
        )

    /**
     * Picks [direction] on every set that offers it — the plan whose sets share a target (N74).
     *
     * Only the rows that can take it are touched, so a set below its ceiling is not given a weight
     * step it never offered, and a row the lifter had picked the other direction for is switched.
     */
    fun chooseAll(direction: ProgressionDirection): PendingProgression =
        copy(
            sets = sets.map { set ->
                if (set.offers(direction)) set.copy(direction = direction) else set
            },
        )

    /**
     * Picks each row's own step — the direction that row earned (P3.17).
     *
     * The bulk answer for a plan whose sets do not share one: a set below its ceiling earned the rep and a
     * set at the ceiling earned the weight, so "take them all" is not one direction for the plan but each
     * row's own. A row already decided keeps its pick, and a row with nothing to take is untouched.
     *
     * Where a range names no ceiling a row offers both, and the **rep** is taken: it is the smaller step
     * at the same load, and the weight step is the one that restarts a range.
     */
    fun chooseEveryOffer(): PendingProgression = copy(
        sets = sets.map { set ->
            val direction = set.direction ?: set.ownDirection()
            if (direction == null) set else set.copy(direction = direction)
        },
    )

    /** The direction this row's offer is for, or null when it has none left to take (P3.17). */
    private fun PendingSet.ownDirection(): ProgressionDirection? {
        val offer = prompt.offer
        return if (applied || offer == null) {
            null
        } else {
            when {
                offer.reps != null -> ProgressionDirection.REPS
                offer.load != null -> ProgressionDirection.LOAD
                else -> null
            }
        }
    }

    /** How many rows still have a step to take — what the *every* control is offered against (P3.17). */
    fun offeringEvery(): Int = sets.count { !it.applied && it.prompt.offer != null }

    /**
     * Records that one picked step has been written (N74).
     *
     * The row keeps its place with the accepted targets, so the lifter reads what the plan now says
     * instead of watching the row disappear, and its offer is spent: a failure part-way through a
     * confirm must never let the retry write the same set twice.
     */
    fun applied(setId: String, accepted: ProgressionPlanSet): PendingProgression =
        copy(
            sets = sets.map { set ->
                if (set.planId != setId) {
                    set
                } else {
                    set.copy(
                        prompt = set.prompt.copy(planned = accepted, offer = null, miss = null),
                        direction = null,
                        applied = true,
                    )
                }
            },
        )
}

/** One step the lifter picked, and the direction they picked it in (N74). */
data class ProgressionChoice(
    val offer: ProgressionOffer,
    val direction: ProgressionDirection,
)

/** The frozen question for one exercise: every working set, its offer, and nothing chosen yet (N74). */
fun pendingProgressionFor(
    sessionExerciseId: String,
    exerciseName: String,
    unit: WeightUnit,
    prompt: ProgressionPrompt,
): PendingProgression = PendingProgression(
    sessionExerciseId = sessionExerciseId,
    exerciseName = exerciseName,
    unit = unit,
    sets = prompt.sets.map { PendingProgression.PendingSet(prompt = it) },
)
