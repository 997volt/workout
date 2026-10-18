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
 * The shape [TemplateSet] already has, reduced to the three targets the creator derives: a load (or the
 * assistance it stores instead, N15), and one rep count. The set role is always [SetType.NORMAL] — a
 * deload carries no ramp and no rung — so it is not a field.
 */
data class DeloadSetTarget(
    val weightGrams: Long?,
    val assistanceGrams: Long?,
    val reps: Int?,
)

/** The plan's working sets are kept up to this many, because past it the count is the volume a deload removes. */
const val DELOAD_MAX_SETS = 3

/** A deload still has to train the movement, so an exercise the plan gave no sets gets one. */
const val DELOAD_MIN_SETS = 1

/** One rep at a deload load trains nothing, so the draft never asks for fewer than this. */
const val DELOAD_MIN_REPS = 5

/** The share of the plan's load the draft keeps by default: 60%, a 40% back-off. */
const val DELOAD_DEFAULT_LOAD_KEPT_PERCENT = 60

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
 *   (N14).
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
    val plannedReps = planned
        .mapNotNull { it.targetRepsMax ?: it.targetRepsMin ?: it.targetRepsCurrent }
        .maxOrNull()
    val performedReps = performed.maxOfOrNull { it.reps }
    val reps = (plannedReps ?: performedReps)?.coerceAtLeast(DELOAD_MIN_REPS)

    // The plan wins where it states a load and history only fills what it leaves unsaid, the precedence
    // the workout's own prefill already uses (N14, N59).
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
    val weight = if (assisted) {
        // An assisted set stores no added weight (N15); the help is the load.
        0L
    } else {
        val source = plannedWeight ?: performedWeight
        source?.takeIf { it > 0L }
            ?.let { roundToStep(it * loadKeptPercent / FULL_PERCENT, stepGrams) }
    }

    val target = DeloadSetTarget(
        weightGrams = weight,
        assistanceGrams = if (assisted) assistance else null,
        reps = reps,
    )
    return List(setCount) { target }
}
