package com.example.androidapp.domain.model

/**
 * The next step an exercise earned, as a decision the lifter makes (ROADMAP N50).
 *
 * N59 withdrew the app's proposal rather than the question: with the next set's values already fields
 * on the screen, a chip beside them had nothing to add. What it left was *where* a proposal belongs
 * and *what* earns one. **Done** is where — the work is over, and what comes next is the decision
 * that remains — and the plan's own targets are what: the offer exists only where the plan named a
 * target RPE for the exercise and every working set met it, so the app states a step it can read out
 * of the plan and the log rather than guessing from history.
 *
 * **The app still only suggests.** The offer is inert until the lifter accepts one of its directions,
 * and accepting writes the plan and nothing else (DECISIONS.md's "the app suggests; it never writes").
 */

/** The plan an offer's set came from, so the accepted step is written back to the right one (N50). */
enum class ProgressionSource { TEMPLATE, SLOT }

/** The directions a lifter can accept (N50). Enums by name, never ordinal. */
enum class ProgressionDirection { LOAD, REPS }

/** One target and the raised one (N50). */
data class ProgressionStep<T>(val from: T, val to: T)

/**
 * One prescribed set, reduced to what progression reads and what the accepted write must keep (N50).
 *
 * Every target is nullable, exactly as it is on the plan: a set that names no load cannot have one
 * raised, and a set that names no reps cannot have its work checked. [source] rides along because the
 * two plans are written through different repositories (N14, P3.8), and [targetPercentOf1Rm] is a
 * slot's alone — carried so accepting a rep does not silently drop a percentage the set stated.
 *
 * [targetRpeHalves] is the **exercise's one number**, carried onto every prescribed set by the
 * caller (N59, amended): the rule measures each set against the plan's single target, and a plan that
 * names none leaves every set here null. A set's own stored value is the caller's fallback for a plan
 * imported from a backup written before the effort moved to the exercise.
 */
data class ProgressionPlanSet(
    val setId: String,
    val setIndex: Int,
    val source: ProgressionSource,
    val role: SetType = SetType.NORMAL,
    val targetWeightGrams: Long? = null,
    val targetAssistanceGrams: Long? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetRpeHalves: Int? = null,
    val targetPercentOf1Rm: Int? = null,
    val note: String? = null,
    /**
     * The set's **own** stored value of the legacy per-set RPE column, or null (N59).
     *
     * [targetRpeHalves] is the exercise's one number, which the caller carries onto every set so the
     * rule can measure each against it. This is what the set row itself holds, and it is what an
     * accepted step must put back: writing [targetRpeHalves] into the set would copy the exercise's
     * number into a column the reader falls back to, so clearing the exercise's field later would
     * silently resurrect a value the lifter never gave that set.
     */
    val legacyRpeHalves: Int? = null,
) {
    /**
     * The reps this set asks for: **the ceiling a written plan means**, falling back to the floor
     * when that is all it wrote — the reading `plannedTargetFor` and `prescribedTargetFor` already
     * use, so the prompt and the rule agree about what "5–8" asks for.
     */
    val targetReps: Int? get() = targetRepsMax ?: targetRepsMin
}

/** One performed set, reduced to what progression reads (N50). Warm-ups are dropped by the rule. */
data class ProgressionPerformance(
    val reps: Int,
    val weightGrams: Long = 0L,
    val assistanceGrams: Long = 0L,
    val rpeHalves: Int? = null,
    val role: SetType = SetType.NORMAL,
)

/**
 * What the done prompt states, and the step it offers (ROADMAP N50).
 *
 * A prompt rather than a bare offer because *Done* has to say something even when nothing was earned:
 * [planned] is the plan's own target for the last working set — the set a step would change — and
 * [performed] is what the session logged there. Either may be absent, and the wording says so rather
 * than inventing a target the plan never wrote.
 */
data class ProgressionPrompt(
    val planned: ProgressionPlanSet? = null,
    val performed: ProgressionPerformance? = null,
    val offer: ProgressionOffer? = null,
)

/**
 * The step one exercise earned, and the plan set it would change (ROADMAP N50).
 *
 * [reps] is always there when there is an offer at all: the work can only be checked against a rep
 * target, so a set that names none earns nothing. [load] is absent where the plan names no **added**
 * weight to raise — an assisted set's number is the machine's help rather than a load (N15), and a
 * percentage prescription's kilograms are derived rather than written (P3.8), so neither has a
 * target a step can move.
 */
data class ProgressionOffer(
    val set: ProgressionPlanSet,
    val reps: ProgressionStep<Int>,
    val load: ProgressionStep<Long>? = null,
) {
    /**
     * The set as accepting [direction] would leave the plan, or null when that direction is not
     * offered.
     *
     * A rep raises the ceiling where the plan wrote one and the floor where that is all it wrote —
     * N22's "add reps to the plan's rep ceiling", applied to whichever number the plan means.
     */
    fun accepted(direction: ProgressionDirection): ProgressionPlanSet? = when (direction) {
        ProgressionDirection.LOAD -> load?.let { set.copy(targetWeightGrams = it.to) }
        ProgressionDirection.REPS -> when (set.targetRepsMax) {
            null -> set.copy(targetRepsMin = reps.to)
            else -> set.copy(targetRepsMax = reps.to)
        }
    }
}

/**
 * The prompt one exercise's *Done* opens with (ROADMAP N50).
 *
 * [planned] and [performed] are the plan and the session reduced to what progression reads, in the
 * exercise's own order. The prompt states the **last working set** on each side: that is the set the
 * session built toward, the one a step would change, and pairing the whole lists is [progressionOfferFor]'s
 * job rather than the wording's. An exercise with no plan states no plan rather than failing.
 */
fun progressionPromptFor(
    planned: List<ProgressionPlanSet>,
    performed: List<ProgressionPerformance>,
): ProgressionPrompt {
    val plannedWork = planned.plannedWork()
    return ProgressionPrompt(
        planned = plannedWork.lastOrNull(),
        performed = performed.performedWork().lastOrNull(),
        offer = progressionOfferFor(planned, performed),
    )
}

/**
 * The next step one exercise earned, or null (ROADMAP N50).
 *
 * **Earned** means the plan named a target RPE for the exercise — one number, carried onto every
 * prescribed working set by the caller — and every one of them was performed with its reps met at or
 * under that RPE, so the plan was answered with room in hand. The plan's Nth working set is paired
 * with the session's Nth working set: **the exercise's logged order**, with warm-ups removed from both
 * sides first (N17, N20, N22). Matching on `setIndex` would break the moment a plan's warm-up went
 * unlogged — every later index would shift and honest work would read as unattempted — while the order
 * of the work itself is the pairing the plan already means. A prescribed set with no performed
 * counterpart is work not done, and one rep short or one RPE above target earns nothing: this is the
 * app saying it is sure, so it says nothing otherwise.
 *
 * The same holds for the two things it cannot check — a plan that names no target RPE, or a session
 * that recorded none — which is why an unrated session suggests nothing rather than guessing (N59
 * shows the plan's RPE in the field rather than recording it).
 *
 * [stepGrams] is the smallest loadable step, so a barbell that jumps 2.5 kg is moved by one it has.
 */
fun progressionOfferFor(
    planned: List<ProgressionPlanSet>,
    performed: List<ProgressionPerformance>,
    stepGrams: Long = DEFAULT_PROGRESSION_STEP_GRAMS,
): ProgressionOffer? {
    val plannedWork = planned.plannedWork()
    val performedWork = performed.performedWork()
    val changed = plannedWork.lastOrNull()
    val repTarget = changed?.targetReps
    val earned = repTarget != null && plannedWork.withIndex().all { (index, plan) ->
        plan.wasMetBy(performedWork.getOrNull(index))
    }
    if (changed == null || repTarget == null || !earned) return null
    return ProgressionOffer(
        set = changed,
        reps = ProgressionStep(repTarget, repTarget + REPS_PER_STEP),
        load = changed.addedWeightGrams?.let { ProgressionStep(it, it + stepGrams) },
    )
}

/**
 * The **added** weight this set names, or null (N15).
 *
 * A load a step can raise is a bar's kilograms, and only that. An assisted set stores its number as
 * a magnitude of help with `targetWeightGrams = 0` — the shape both plan editors write for `-20` —
 * so a non-null column is not on its own a load to raise; raising it would put 2.5 kg on a machine
 * doing 20 kg of the work, and `Weight.display` would then show `-20` and hide the write. A
 * bodyweight set's `0` is the same absence. The warm-up ramp already guards this (`<= 0`), and this
 * is the rule's own half of it.
 */
private val ProgressionPlanSet.addedWeightGrams: Long?
    get() = targetWeightGrams?.takeIf { it > 0L && targetAssistanceGrams == null }

/** The sets that are work: a warm-up is not what a target is measured against (N17, N20, N22). */
private fun List<ProgressionPlanSet>.plannedWork(): List<ProgressionPlanSet> =
    filterNot { it.role == SetType.WARMUP }.sortedBy { it.setIndex }

/** The same exclusion on the session's side, in the order the sets were logged (N50). */
private fun List<ProgressionPerformance>.performedWork(): List<ProgressionPerformance> =
    filterNot { it.role == SetType.WARMUP }

/**
 * True when the plan's set was answered: its reps met, at or under the RPE the plan asked for.
 *
 * Both halves are what the plan wrote, so a set the plan gave no rep target or no RPE to was never
 * answered — and an unrecorded RPE is the same absence (N59).
 */
private fun ProgressionPlanSet.wasMetBy(done: ProgressionPerformance?): Boolean {
    val reps = targetReps
    val rpeTarget = targetRpeHalves
    return done != null && done.rpeHalves != null && reps != null && rpeTarget != null &&
        done.reps >= reps && done.rpeHalves <= rpeTarget
}

/** One rep is the step a rep direction raises by: the smallest unit a plan counts in. */
private const val REPS_PER_STEP = 1
