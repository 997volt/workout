package com.example.androidapp.domain.model

/**
 * The next step each planned set earned, as a decision the lifter makes (ROADMAP N50, N74).
 *
 * N59 withdrew the app's proposal rather than the question: with the next set's values already fields
 * on the screen, a chip beside them had nothing to add. What it left was *where* a proposal belongs
 * and *what* earns one. **Done** is where — the work is over, and what comes next is the decision
 * that remains — and the plan's own targets are what: the offer exists only where the plan named a
 * target RPE for the exercise and the set met it, so the app states a step it can read out of the plan
 * and the log rather than guessing from history.
 *
 * **The plan's terms include its load** (B258). A set performed lighter than the plan named did not do
 * the prescribed set, so it earns no step at all rather than a heavier target on top of a bar nobody
 * loaded; and the weight step lands above what the session actually lifted, because the plan is what
 * moves and it must not move backwards past the work that earned it.
 *
 * **N74 made that step per set, and held it inside the plan's rep range.** Every prescribed working
 * set is judged on its own rather than the exercise as a whole, so a set that was answered earns its
 * step even when a sibling missed. The range a set was authored with — "Reps from"/"Reps to" — is
 * never edited by progression: a set below its ceiling earns **reps**, and a set at the ceiling earns
 * the **weight**, which puts the climb back on the range's floor. The two directions are therefore
 * exclusive wherever the plan wrote a ceiling; a plan that wrote none keeps both, as it always has.
 *
 * **The app still only suggests.** The offer is inert until the lifter accepts one of its directions,
 * and accepting writes the plan and nothing else (DECISIONS.md's "the app suggests; it never writes").
 */

/** The directions a lifter can accept (N50). Enums by name, never ordinal. */
enum class ProgressionDirection {
    LOAD,
    REPS,

    /** A hold's own direction: the plan prescribes seconds and the step moves them (ROADMAP N242). */
    SECONDS,
    ;

    companion object {
        /** The direction a stored name means, or null for one this build does not know (ROADMAP N236). */
        fun fromName(name: String): ProgressionDirection? = entries.firstOrNull { it.name == name }
    }
}

/** One target and the raised one (N50). */
data class ProgressionStep<T>(val from: T, val to: T)

/**
 * One prescribed set, reduced to what progression reads and what the accepted write must keep
 * (N50, N74).
 *
 * Every target is nullable, exactly as it is on the plan: a set that names no load cannot have one
 * raised, and a set that names no reps cannot have its work checked. There is one plan to write back
 * to since N73 — the template's — so nothing here says which one it came from.
 *
 * [targetRpeHalves] is the **exercise's one number**, carried onto every prescribed set by the
 * caller (N59, amended): the rule measures each set against the plan's single target, and a plan that
 * names none leaves every set here null.
 */
data class ProgressionPlanSet(
    val setId: String,
    val setIndex: Int,
    val role: SetType = SetType.NORMAL,
    val targetWeightGrams: Long? = null,
    val targetAssistanceGrams: Long? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    /** Where in the range the lifter has climbed, or null (ROADMAP N74). */
    val targetRepsCurrent: Int? = null,
    /**
     * The hold this set prescribes, in seconds, or null for a counted set (ROADMAP N242).
     *
     * A timed set records seconds and no reps, so a plan that named only reps ignored the hold entirely and
     * *Done* answered it with "a hold is not measured in reps" — the workaround this replaces.
     */
    val targetDurationSeconds: Int? = null,
    val targetRpeHalves: Int? = null,
    val note: String? = null,
) {
    /**
     * The reps this set asks for: where the lifter has climbed to, falling back to the range's floor
     * and then its ceiling — the reading `plannedTargetFor` shares, so the session and the prompt
     * agree about what "5–8" asks for once progression has moved it (N74).
     */
    val targetReps: Int? get() = targetRepsCurrent ?: targetRepsMin ?: targetRepsMax

    /**
     * True when the plan wrote a real range for the weight step to restart (N74).
     *
     * Both ends have to be there and differ: "5 to 5" is one number rather than a range, and a plan
     * that named only one end has no floor to return to — so a weight step on either leaves the reps
     * exactly where they are.
     */
    val restartsAtFloor: Boolean
        get() = targetRepsMin != null && targetRepsMax != null && targetRepsMin < targetRepsMax
}

/** One performed set, reduced to what progression reads (N50). Warm-ups are dropped by the rule. */
data class ProgressionPerformance(
    val reps: Int,
    val weightGrams: Long = 0L,
    val assistanceGrams: Long = 0L,
    val rpeHalves: Int? = null,
    val role: SetType = SetType.NORMAL,
    /** How long the set was held, or null for a counted set (ROADMAP N220, B243). */
    val durationSeconds: Int? = null,
)

/**
 * Why a working set earned no step (ROADMAP N74).
 *
 * Stated rather than left blank because *Done* has to say something about work that was not answered:
 * a lifter reading "2 reps short of the plan" learns what the app measured, where an empty row would
 * read as a bug. The last one is the only case where the set *was* answered and still has nowhere to
 * go.
 */
enum class ProgressionMiss {
    /** The plan prescribed it and the session never logged it. */
    NOT_DONE,

    /** The plan prescribes a hold and the set was held shorter than it (ROADMAP N242). */
    HOLD_SHORT,

    /** Work the plan does not name — an extra set — so there is no target a step could move. */
    NOT_IN_PLAN,

    /** The set names no reps, so there is nothing to check the work against or to raise. */
    NO_REP_TARGET,

    /**
     * The set was held, and a hold is not measured in reps (ROADMAP N220, B243).
     *
     * The plan's rep target does not apply to it — a timed movement inside a plan ignores that target — so
     * this says why no step was earned rather than reporting the hold as "zero reps done".
     */
    HOLD,

    /** The exercise names no target RPE, so the app cannot tell whether there was room in hand. */
    NO_TARGET_RPE,

    /** The set was logged without an effort, so nothing is known about the room in hand. */
    UNRATED,

    /** Performed short of the reps this set asked for. */
    REPS_SHORT,

    /** Performed above the effort the plan asked for. */
    OVER_TARGET_RPE,

    /**
     * Performed below the load the plan named, so the plan's set was not done (B258).
     *
     * The plan's targets are reps, a load and an effort; a set that met the first two at a lighter bar
     * moved everything except the prescription. Stated with the difference the app measured, because
     * "no step" on its own reads as a bug — and it is the reason a lifter who dropped the weight for
     * the day is told so rather than being pushed heavier on top of a plan they did not load.
     */
    LOAD_SHORT,

    /**
     * Answered at the top of the range with no load to raise (N74).
     *
     * A bodyweight or assisted set has no added weight a step can move, so reaching its ceiling is
     * the end of the app's suggestions — it says so rather than offering a rep past the range.
     */
    TOPPED_OUT,

    /**
     * A rung of a group, which is never judged on its own (ROADMAP N79).
     *
     * A drop or cluster set carries no target of its own: the load is derived from the set above it,
     * the reps are the same set's own history, and the step the group earned belongs to that first
     * set. Stated rather than left blank, because a lifter reading a row with nothing beside it would
     * take it for a bug.
     */
    RUNG_OF_A_GROUP,
}

/** One working set as the prompt states it: the plan, what was done, and what it earned (N74). */
data class ProgressionSetPrompt(
    val planned: ProgressionPlanSet?,
    val performed: ProgressionPerformance?,
    /** The step this set earned, or null — in which case [miss] says why (N74). */
    val offer: ProgressionOffer? = null,
    /** Why no step was offered, or null when one was (N74). */
    val miss: ProgressionMiss? = null,
)

/**
 * What the done prompt states, and the steps it offers (ROADMAP N50, N74).
 *
 * One entry per prescribed working set, in the plan's own order, and then any logged work the plan
 * does not name — so the prompt is the whole session rather than the exercise's last set. An exercise
 * with no plan at all has nothing to state, and the screen does not ask (N50).
 */
data class ProgressionPrompt(
    val sets: List<ProgressionSetPrompt> = emptyList(),
) {
    /** True where the plan can answer *Done* at all: one prescribed set is enough (N50, N74). */
    val hasPlan: Boolean get() = sets.any { it.planned != null }

    /** How many sets earned a step: what the screen says when nothing did (N74). */
    val earned: Int get() = sets.count { it.offer != null }

    /**
     * The one reason every prescribed working set missed for, with the set whose numbers state it, or null
     * (ROADMAP N226).
     *
     * The question states one row per set, which is right until every row says the same thing: a plan with no
     * target RPE printed that sentence once per set, and a plan whose sets all came up two reps short printed
     * the same measured line three times. When one reason covers all of them the question states it once above
     * the sets, and the rows keep their plan and done lines.
     *
     * **Only a reason about the sets themselves takes part.** `NOT_DONE`, `NOT_IN_PLAN` and `RUNG_OF_A_GROUP`
     * describe work that is missing or extra rather than work the plan judged, so a plan nobody logged is not
     * summarised as one refusal; and a single set that earned a step ends it, because then the reason no longer
     * covers all of them.
     *
     * **The measurement has to agree too.** `REPS_SHORT`, `LOAD_SHORT` and `TOPPED_OUT` state a number in the
     * row, and three different shortfalls are three facts rather than one — they collapse only when the number
     * does, which is what makes the collapsed line true of every set it replaces.
     */
    val sharedMiss: ProgressionSetPrompt?
        get() {
            // A warm-up is not judged and a rung is judged through the set above it, so neither is what "every
            // set" means here (N17, N79).
            val work = sets.filter { set ->
                val role = set.planned?.role ?: return@filter false
                role.isWork && !role.isRung
            }
            // One set repeating itself is not a repetition: the row already states its reason.
            if (work.size < MIN_SETS_FOR_ONE_REASON) return null
            val first = work.first()
            val miss = first.miss ?: return null
            if (miss in MISSES_THAT_ARE_NOT_SHARED) return null
            val number = first.statedNumber
            return first.takeIf { set -> work.all { it.miss == miss && it.statedNumber == number } }
        }
}

/** Two sets are the fewest that can say the same thing twice (N226). */
private const val MIN_SETS_FOR_ONE_REASON = 2

/** The misses that describe missing or extra work rather than a set the plan judged (N226). */
private val MISSES_THAT_ARE_NOT_SHARED = setOf(
    ProgressionMiss.NOT_DONE,
    ProgressionMiss.NOT_IN_PLAN,
    ProgressionMiss.RUNG_OF_A_GROUP,
)

/** The number this set's miss states in its sentence, or null where it states none (N226). */
private val ProgressionSetPrompt.statedNumber: Long?
    get() = when (miss) {
        ProgressionMiss.REPS_SHORT -> (planned?.targetReps ?: 0).toLong() - (performed?.reps ?: 0)
        ProgressionMiss.LOAD_SHORT -> (planned?.targetWeightGrams ?: 0L) - (performed?.weightGrams ?: 0L)
        ProgressionMiss.TOPPED_OUT -> (planned?.targetRepsMax ?: 0).toLong()
        // The hold's sentence states the seconds it fell short by (N242), so two different shortfalls are two
        // facts like the reps and the load above (B312).
        ProgressionMiss.HOLD_SHORT ->
            (planned?.targetDurationSeconds ?: 0).toLong() - (performed?.durationSeconds ?: 0).toLong()

        else -> null
    }

/**
 * The step one set earned, and the plan set it would change (ROADMAP N50, N74).
 *
 * Either direction may be absent — a bodyweight set has no load to raise, a plan may name no reps —
 * and for a set inside a written range **exactly one is offered**: below the ceiling the reps move
 * and the weight waits, at the ceiling the weight moves and the reps restart. A plan that wrote no
 * ceiling is the one case that keeps both.
 */
data class ProgressionOffer(
    val set: ProgressionPlanSet,
    /** The rep step, or null where the plan's range has no room left in it (N74). */
    val reps: ProgressionStep<Int>? = null,
    /** The load step, or null where the plan names no added weight to raise (N15, N74). */
    val load: ProgressionStep<Long>? = null,
    /**
     * The hold step, or null when the plan prescribes no duration (ROADMAP N242).
     *
     * **A hold earns seconds or the set earns reps, never both**: the three directions above and this one are
     * exclusive by construction, because [offerFor] returns a hold step only for a set the plan measured in
     * seconds and a rep or load step only for one it measured in reps.
     */
    val seconds: ProgressionStep<Int>? = null,
) {
    /**
     * The directions this offer could be taken in, in the order the dialog draws them (ROADMAP N236).
     *
     * What a *declined* step records: the offer is frozen from the plan as it was, so the direction it
     * named cannot be recomputed later — the plan has moved on since.
     */
    val directions: Set<ProgressionDirection>
        get() = buildSet {
            if (reps != null) add(ProgressionDirection.REPS)
            if (load != null) add(ProgressionDirection.LOAD)
            if (seconds != null) add(ProgressionDirection.SECONDS)
        }

    /**
     * The set as accepting [direction] would leave the plan, or null when that direction is not
     * offered.
     *
     * A rep moves **where the lifter is in the range** and never the range's two ends (N74). A weight
     * step on a real range puts that climb back on the floor — the range is started again a step
     * heavier — while a plan that named no range keeps the reps it had.
     */
    fun accepted(direction: ProgressionDirection): ProgressionPlanSet? = when (direction) {
        ProgressionDirection.LOAD -> load?.let { step ->
            set.copy(
                targetWeightGrams = step.to,
                targetRepsCurrent = set.targetRepsMin.takeIf { set.restartsAtFloor } ?: set.targetRepsCurrent,
            )
        }

        ProgressionDirection.REPS -> reps?.let { set.copy(targetRepsCurrent = it.to) }

        ProgressionDirection.SECONDS -> seconds?.let { set.copy(targetDurationSeconds = it.to) }
    }
}

/**
 * The prescribed sets and the performed ones, paired **by class** (ROADMAP N79, B305).
 *
 * Prescribed work sets pair with performed work sets and prescribed rungs with performed rungs, each in order —
 * so a logged rung can only ever be extra, never a stand-in for a work set the session did not log. The offer
 * and the stall rule read this one pairing, so the two cannot disagree about which set a session answered.
 */
internal data class PerformancePairs(
    /** One entry per prescribed set, null where the session logged nothing for it. */
    val bySetId: Map<String, ProgressionPerformance?>,
    /** Indexes into the warm-up-filtered performed list, for the prompt's extra-work half. */
    val usedIndexes: Set<Int>,
)

internal fun pairedPerformances(
    planned: List<ProgressionPlanSet>,
    performed: List<ProgressionPerformance>,
): PerformancePairs {
    val plannedWork = planned.plannedWork()
    val performedWork = performed.performedWork()
    val workIndexes = performedWork.indices.filterNot { performedWork[it].role.isRung }
    val rungIndexes = performedWork.indices.filter { performedWork[it].role.isRung }
    val pairs = mutableMapOf<String, ProgressionPerformance?>()
    val used = mutableSetOf<Int>()
    listOf(true, false).forEach { rungs ->
        plannedWork.filter { it.role.isRung == rungs }.forEachIndexed { index, set ->
            val at = (if (rungs) rungIndexes else workIndexes).getOrNull(index)
            pairs[set.setId] = at?.let { performedWork[it] }
            at?.let { used += it }
        }
    }
    return PerformancePairs(bySetId = pairs, usedIndexes = used)
}

/**
 * The prompt one exercise's *Done* opens with (ROADMAP N50, N74).
 *
 * The plan's sets are paired with the session's by class, warm-ups removed from both sides (N17, N20, N22,
 * N79 — see [pairedPerformances]), and each pair is judged on its own. Work the plan does not name is stated
 * after them with no step to offer, so the prompt accounts for the whole session. An exercise with no plan
 * states no sets rather than failing.
 */
fun progressionPromptFor(
    planned: List<ProgressionPlanSet>,
    performed: List<ProgressionPerformance>,
    /**
     * The smallest loadable step in the unit this exercise is read in (ROADMAP N64): 2.5 kg for a
     * metric lifter, 5 lb for a pound-loading machine, so the offered step is one it can load.
     */
    stepGrams: Long = DEFAULT_PROGRESSION_STEP_GRAMS,
): ProgressionPrompt {
    val plannedWork = planned.plannedWork()
    val performedWork = performed.performedWork()
    val paired = pairedPerformances(planned, performed)

    val prescribed = plannedWork.map { set -> set.toPrompt(paired.bySetId[set.setId], stepGrams) }
    val extra = performedWork
        .filterIndexed { index, _ -> index !in paired.usedIndexes }
        .map { ProgressionSetPrompt(planned = null, performed = it, miss = ProgressionMiss.NOT_IN_PLAN) }
    return ProgressionPrompt(sets = prescribed + extra)
}

/**
 * One prescribed set and what the session did to it (N74).
 *
 * The offer and the miss are two halves of one judgement, so they are made together: a set either
 * earned a step or has a reason it did not, never both and never neither.
 */
private fun ProgressionPlanSet.toPrompt(
    done: ProgressionPerformance?,
    stepGrams: Long,
): ProgressionSetPrompt {
    // A rung is never judged: the group's step is earned or missed by the set it hangs off (N79).
    if (role.isRung) {
        return ProgressionSetPrompt(
            planned = this,
            performed = done,
            miss = ProgressionMiss.RUNG_OF_A_GROUP,
        )
    }

    val unanswered = answeredMiss(done)
    val offer = if (unanswered == null && done != null) offerFor(done, stepGrams) else null
    return ProgressionSetPrompt(
        planned = this,
        performed = done,
        offer = offer,
        // An answered set with no step is the one case the plan cannot take further (N74).
        miss = unanswered ?: ProgressionMiss.TOPPED_OUT.takeIf { offer == null },
    )
}

/**
 * Why the plan's own terms were not met, or null when they were (N74, B258).
 *
 * Every check the old exercise-wide rule made, made against one set: the plan has to name reps and
 * one target effort for the exercise, the session has to have recorded an effort, and the work has to
 * have met the reps without going over that effort. **The load is one of the plan's terms too**: a set
 * performed lighter than the plan named did not do the prescribed set, so it earns nothing at all —
 * not the load, and not a rep climbed on a bar the plan never asked for.
 */
private fun ProgressionPlanSet.answeredMiss(done: ProgressionPerformance?): ProgressionMiss? {
    val reps = targetReps
    val rpeTarget = targetRpeHalves
    val plannedLoad = addedWeightGrams
    val lifted = done?.addedWeightGrams
    val plannedHold = targetDurationSeconds
    return when {
        done == null -> ProgressionMiss.NOT_DONE
        // The plan prescribes seconds: a hold is answered by the same gate the counted set is — the work has
        // to reach the prescription and not exceed the effort (ROADMAP N242).
        plannedHold != null -> holdMiss(done, plannedHold)

        done.durationSeconds != null -> ProgressionMiss.HOLD
        reps == null -> ProgressionMiss.NO_REP_TARGET
        rpeTarget == null -> ProgressionMiss.NO_TARGET_RPE
        done.rpeHalves == null -> ProgressionMiss.UNRATED
        done.reps < reps -> ProgressionMiss.REPS_SHORT
        done.rpeHalves > rpeTarget -> ProgressionMiss.OVER_TARGET_RPE
        // Last, so the sentences the rule already stated keep their precedence: a set that was also
        // short or over its effort is explained by that first, and this is the fact that remains.
        plannedLoad != null && lifted != null && lifted < plannedLoad -> ProgressionMiss.LOAD_SHORT
        else -> null
    }
}

/**
 * Why a hold the plan prescribes was not answered, or null when it was (ROADMAP N242).
 *
 * The same four questions the counted set's branch asks — was it logged, was an effort recorded on both
 * sides, did the work reach the prescription, did it stay inside the effort — asked of a duration instead of
 * a rep count. A counted set against a plan that prescribes seconds is the one case that reads as [HOLD]:
 * the plan measured a hold and the set was not one.
 */
private fun ProgressionPlanSet.holdMiss(
    done: ProgressionPerformance,
    plannedHold: Int,
): ProgressionMiss? = when {
    done.durationSeconds == null -> ProgressionMiss.HOLD
    targetRpeHalves == null -> ProgressionMiss.NO_TARGET_RPE
    done.rpeHalves == null -> ProgressionMiss.UNRATED
    done.durationSeconds < plannedHold -> ProgressionMiss.HOLD_SHORT
    done.rpeHalves > targetRpeHalves -> ProgressionMiss.OVER_TARGET_RPE
    else -> null
}

/**
 * The step an answered set earned, or null when the range has nothing left to give (N74, B258).
 *
 * **Below the range's ceiling the reps move and the weight waits**, and the target becomes one past
 * what was actually done rather than one past the number the plan asked for — asked 5, did 7 of an 8
 * ceiling, next target 8, so the plan tracks the lifter. **At the ceiling the weight moves instead**,
 * and a plan that wrote no ceiling is the case that keeps both directions, as it always did. A set at
 * the ceiling with no load to raise returns null, which [toPrompt] states as [ProgressionMiss.TOPPED_OUT].
 *
 * The weight step lands **above what the session lifted**, not one step above the stored number
 * (B258): the plan is 140 kg and the set was 150, so the plan follows to 152.5 — where the old
 * arithmetic offered 142.5, a target below the work the lifter had already done.
 */
private fun ProgressionPlanSet.offerFor(
    done: ProgressionPerformance,
    stepGrams: Long,
): ProgressionOffer? {
    // **A hold is answered first**: a plan that prescribes seconds names no reps at all, so the rep branch
    // below would say it has nowhere to go (ROADMAP N242).
    val plannedHold = targetDurationSeconds
    val held = done.durationSeconds
    val seconds = if (plannedHold != null && held != null) {
        ProgressionStep(
            plannedHold,
            stepAbove(
                planned = plannedHold.toLong(),
                performed = held.toLong(),
                stepGrams = HOLD_STEP_SECONDS.toLong(),
            ).toInt(),
        )
    } else {
        null
    }
    val reps = targetReps
    val ceiling = targetRepsMax
    val load = addedWeightGrams?.let { planned ->
        ProgressionStep(planned, stepAbove(planned, done.addedWeightGrams ?: planned, stepGrams))
    }
    return when {
        // The plan prescribes seconds and the set was held: the step is time, and only time.
        seconds != null -> ProgressionOffer(set = this, seconds = seconds)

        // A hold against a plan that counts has no rep target to move (B243): it earns no step.
        held != null -> null

        // A plan that prescribes seconds but whose set was counted has nowhere to go either (N242).
        plannedHold != null -> null

        reps == null -> null

        ceiling == null -> ProgressionOffer(
            set = this,
            reps = ProgressionStep(reps, reps + REPS_PER_STEP),
            load = load,
        )

        done.reps < ceiling -> ProgressionOffer(
            set = this,
            reps = ProgressionStep(reps, minOf(done.reps + REPS_PER_STEP, ceiling)),
        )

        else -> load?.let { ProgressionOffer(set = this, load = it) }
    }
}

/**
 * The plan's next load, always above the load the session actually lifted (B258).
 *
 * The step moves the plan, so one measured from the stored number alone can land *below* the work
 * already done — 140 kg beaten by a 150 kg set became 142.5 kg, a target the lifter had just beaten
 * before they accepted it. The smallest value of the plan's own grid above both is what it takes:
 * snapped onto the plan's step rather than `performed + one`, because a typed load can sit between
 * steps while every other row of the plan is on it.
 */
internal fun stepAbove(planned: Long, performed: Long, stepGrams: Long): Long {
    val step = stepGrams.coerceAtLeast(1L)
    val steps = ((performed - planned) / step) + 1
    return planned + steps.coerceAtLeast(1L) * step
}

/** True when this set names an added load a step can raise — what [progressionRuleFor] reads (N74). */
val ProgressionPlanSet.namesAddedLoad: Boolean get() = addedWeightGrams != null

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

/**
 * The **load** the set was performed at, or null when it is not the shape a plan's load is (B258).
 *
 * The plan's number is added weight with no help (see [addedWeightGrams]); a set that states
 * assistance is a machine carrying part of the bar, so the two numbers do not describe the same thing
 * and comparing them would call an assisted set "below" a plan it may match exactly. Null leaves the
 * load out of the judgement, which is the answer the rule gave before it read loads at all.
 */
private val ProgressionPerformance.addedWeightGrams: Long?
    get() = weightGrams.takeIf { assistanceGrams <= 0L }

/** The sets that are work: a warm-up is not what a target is measured against (N17, N20, N22). */
private fun List<ProgressionPlanSet>.plannedWork(): List<ProgressionPlanSet> =
    filterNot { it.role == SetType.WARMUP }.sortedBy { it.setIndex }

/** The same exclusion on the session's side, in the order the sets were logged (N50). */
private fun List<ProgressionPerformance>.performedWork(): List<ProgressionPerformance> =
    filterNot { it.role == SetType.WARMUP }

/** One rep is the step a rep direction raises by: the smallest unit a plan counts in. */
private const val REPS_PER_STEP = 1

/**
 * The step a hold moves by, in seconds (ROADMAP N242).
 *
 * A hold has no grid to snap to — a machine's step is a load — so the plan moves by one fixed block of time,
 * which is the resolution the set's own ±15 s buttons already use.
 */
const val HOLD_STEP_SECONDS: Int = 15

/**
 * This plan's sets as the progression rule reads them (N227).
 *
 * The mapping the session already made, moved to the model because the plan editor needs the same shape: one
 * rule reading one translation, rather than two predicates that must be kept in step.
 *
 * **The target RPE is one number for the exercise** (N59), so every set carries it rather than its own — which
 * is what lets the rule check every working set against that single target, and why [progressionRuleFor] takes
 * the number beside the sets.
 */
fun TemplateExercise.progressionSets(): List<ProgressionPlanSet> = sets.map { set ->
    ProgressionPlanSet(
        setId = set.id,
        setIndex = set.setIndex,
        role = set.role,
        targetWeightGrams = set.targetWeightGrams,
        targetAssistanceGrams = set.targetAssistanceGrams,
        targetRepsMin = set.targetRepsMin,
        targetRepsMax = set.targetRepsMax,
        // Where in the range the lifter has climbed (N74): the number the rule measures and a step moves.
        targetRepsCurrent = set.targetRepsCurrent,
        // The hold the plan prescribes, which is what a timed set is measured in (N242).
        targetDurationSeconds = set.targetDurationSeconds,
        targetRpeHalves = targetRpeHalves,
        note = set.note,
    )
}
