package com.example.androidapp.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * One day of a program's deload week (ROADMAP N147).
 *
 * A day is a **workout the creator wrote**, not a slot: it names an ordinary template, carries the day
 * it is offered on, and is keyed to the one week it belongs to through [DeloadWeek]. It is deliberately
 * not a [ProgramSlot] — a slot recurs every week, and a deload is one week — and it is not a
 * [RecordedSubstitution] either: a substitution stands in for one slot's own workout, while these days
 * are the whole of the week and there may be fewer of them than the program has slots.
 */
data class DeloadDay(
    val id: String,
    val programId: String,
    val templateId: String,
    val templateName: String,
    val exerciseCount: Int = 0,
    /** The day's place in the week, in the order the creator listed it. */
    val position: Int = 0,
    /** The weekday it is offered on, or null for an order-only source slot. */
    val weekday: DayOfWeek? = null,
    /**
     * The library exercises this day kept, for the creator's reuse (N150).
     *
     * Filled only by the last-plan read: the week's own reads never need the list, and an empty one is the
     * truthful answer there rather than a second query per day.
     */
    val exerciseIds: List<String> = emptyList(),
)

/**
 * One program's marked deload week and the days it was built from (ROADMAP N147).
 *
 * A week marked with **no days** is P3.10 as it always was: the mark exempts the week and its slots still
 * prescribe their own workouts. A week with days is the creator's plan, and every read of "what is today"
 * takes it instead of the slots.
 */
data class DeloadWeek(
    val programId: String,
    val weekStart: LocalDate,
    val days: List<DeloadDay> = emptyList(),
)

/**
 * What one deload set prescribes (ROADMAP N147).
 *
 * The shape [TemplateSet] already has, reduced to the targets the creator derives: a load (or the
 * assistance it stores instead, N15), and one rep count **or one hold duration** (ROADMAP B396). The set
 * role is always [SetType.NORMAL] — a deload carries no ramp and no rung — so it is not a field.
 */
data class DeloadSetTarget(
    val weightGrams: Long?,
    val assistanceGrams: Long?,
    val reps: Int?,
    /** A timed movement's hold, in seconds; null for a counted set (B396). */
    val durationSeconds: Int? = null,
)

/** The plan's working sets are kept up to this many, because past it the count is the volume a deload removes. */
const val DELOAD_MAX_SETS = 3

/** A deload still has to train the movement, so an exercise the plan gave no sets gets one. */
const val DELOAD_MIN_SETS = 1

/** One rep at a deload load trains nothing, so the draft never asks for fewer than this. */
const val DELOAD_MIN_REPS = 5

/** The share of the plan's load the draft keeps by default: 60%, a 40% back-off. */
const val DELOAD_DEFAULT_LOAD_KEPT_PERCENT = 60

/**
 * The effort a deload workout asks for, in half-points: 6.0 (ROADMAP N300).
 *
 * Without one the session opens its effort field on the app default 9.0, which is the opposite of a
 * deload. This is the week's one deliberate effort; the plan's own RPE is not carried, because a deload
 * at the plan's effort is not a deload.
 */
const val DELOAD_TARGET_RPE_HALVES = 12

/** The lightest share the creator offers, as a percentage kept rather than removed. */
const val DELOAD_LOAD_KEPT_PERCENT_LOW = 50

/** The deepest back-off the creator offers, as a percentage kept. */
const val DELOAD_LOAD_KEPT_PERCENT_HIGH = 70

/** The three shares the creator offers, as percentages kept rather than removed. */
val DELOAD_LOAD_KEPT_CHOICES: List<Int> = listOf(
    DELOAD_LOAD_KEPT_PERCENT_LOW,
    DELOAD_DEFAULT_LOAD_KEPT_PERCENT,
    DELOAD_LOAD_KEPT_PERCENT_HIGH,
)

private const val FULL_PERCENT = 100.0

/**
 * The sets a deload week should prescribe for one exercise (ROADMAP N147).
 *
 * **The rule is arithmetic, and stated so the lifter can see it.** The set count follows the plan, because
 * the plan's count is the only evidence of how much work that lift normally does — a "compound" flag would
 * be the app inferring a training intent the lifter already wrote down:
 *
 * - **Sets:** the plan's stand-alone working sets, capped at [DELOAD_MAX_SETS] and floored at
 *   [DELOAD_MIN_SETS]. A warm-up is preparation and a rung belongs to the set above it, so neither is a set
 *   of its own — which is also why no warm-up and no rung is written.
 * - **Reps:** one number, the highest target those sets name, floored at [DELOAD_MIN_REPS]. A 5/3/1 plan
 *   therefore does 5s and a top-single plan does 5s rather than one. Where the plan names none, the last
 *   finished session's highest working reps; where neither does, none — the app does not invent a number
 *   (N14). **A timed hold keeps its seconds** instead (B396): the plan's longest hold, or the last
 *   session's where the plan states none. The reduced set count is the week's reduction; scaling a hold
 *   would be a second opinion about a number the plan already wrote.
 * - **Load:** the plan's heaviest stated working load, or the last finished session's where the plan states
 *   none, kept at [loadKeptPercent] and rounded to [stepGrams] (N77). No load where neither states one,
 *   because zero is a claim. An assisted movement keeps its help instead: assistance is a magnitude, and
 *   reducing it would make the deload harder (N15).
 *
 * **One load per exercise** — what the plan's heaviest set had, over the whole deload exercise. The week is
 * not a ladder, and a rung's number derived from a halved anchor would compound a value nobody wrote.
 */
fun deloadSetTargets(
    plannedSets: List<TemplateSet>,
    previous: PreviousPerformance?,
    loadKeptPercent: Int = DELOAD_DEFAULT_LOAD_KEPT_PERCENT,
    stepGrams: Long = DEFAULT_PROGRESSION_STEP_GRAMS,
): List<DeloadSetTarget> {
    require(stepGrams > 0L) { "a load step has to be positive" }
    require(loadKeptPercent in 1 until FULL_PERCENT.toInt()) {
        "a deload keeps between 1 and 99 percent of the load"
    }

    // Warm-ups are preparation and a rung is the set above it (N112, N79): neither is work the count reads.
    val planned = plannedSets.filter { it.role.recordsEffort }
    val performed = previous?.sets.orEmpty().filter { it.setType.recordsEffort }

    val setCount = planned.size.coerceIn(DELOAD_MIN_SETS, DELOAD_MAX_SETS)
    val hold = deloadHold(planned, performed)
    val reps = deloadReps(planned, performed, hold)
    val (weight, assistance) = deloadLoad(planned, performed, loadKeptPercent, stepGrams)

    val target = DeloadSetTarget(
        weightGrams = weight,
        assistanceGrams = assistance,
        reps = reps,
        durationSeconds = hold,
    )
    return List(setCount) { target }
}

/**
 * The plan's type wins: a hold if it states one, a counted set if it states reps, and history only when
 * the plan is silent about both (N14, B396).
 */
private fun deloadHold(
    planned: List<TemplateSet>,
    performed: List<SetEntry>,
): Int? = when {
    planned.any { it.targetDurationSeconds != null } ->
        planned.mapNotNull { it.targetDurationSeconds }.maxOrNull()

    planned.any { it.targetRepsMax != null || it.targetRepsMin != null || it.targetRepsCurrent != null } ->
        null

    else -> performed.mapNotNull { it.durationSeconds }.maxOrNull()
}

/** One rep number for a counted exercise; a hold has none, and history fills only a silent plan. */
private fun deloadReps(
    planned: List<TemplateSet>,
    performed: List<SetEntry>,
    hold: Int?,
): Int? {
    if (hold != null) return null
    val plannedReps = planned
        .mapNotNull { it.targetRepsMax ?: it.targetRepsMin ?: it.targetRepsCurrent }
        .maxOrNull()
    val performedReps = performed.maxOfOrNull { it.reps }
    return (plannedReps ?: performedReps)?.coerceAtLeast(DELOAD_MIN_REPS)
}

/**
 * The load to write, and the assistance when the movement is assisted (N15).
 *
 * The plan wins where it states a load and history only fills what it leaves unsaid, the precedence the
 * workout's own prefill already uses (N14, N59).
 */
private fun deloadLoad(
    planned: List<TemplateSet>,
    performed: List<SetEntry>,
    loadKeptPercent: Int,
    stepGrams: Long,
): Pair<Long?, Long?> {
    val plannedAssistance = planned.mapNotNull { it.targetAssistanceGrams }.maxOrNull()
    val plannedWeight = planned.mapNotNull { it.targetWeightGrams }.maxOrNull()
    val performedAssistance = performed.maxOfOrNull { it.assistanceGrams }
    val performedWeight = performed
        .filter { it.assistanceGrams == 0L }
        .maxOfOrNull { it.weightGrams }

    val assistance = when {
        plannedAssistance != null -> plannedAssistance
        plannedWeight != null -> null
        else -> performedAssistance
    }
    val assisted = assistance != null && assistance > 0L
    if (assisted) return 0L to assistance

    val source = plannedWeight ?: performedWeight
    val weight = source?.takeIf { it > 0L }
        ?.let { roundToStep(it * loadKeptPercent / FULL_PERCENT, stepGrams) }
    return weight to null
}
