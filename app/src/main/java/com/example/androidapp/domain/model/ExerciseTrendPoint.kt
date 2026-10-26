package com.example.androidapp.domain.model

import java.time.Instant
import kotlin.math.roundToLong

/**
 * One finished workout's worth of *one lift* (ROADMAP N17) — which is one exercise, or every row a **head**
 * holds (ROADMAP N97).
 *
 * N13 reads the app's signals across everything; this is the narrower question a lifter
 * actually asks — how is my bench press going — so every value here comes from the sets
 * logged for a single exercise in a single session. A head's family answers the same question about the
 * lift a lifter thinks of as one: volume, reps and the ratings are then sums or averages over the family's
 * rows, each computed once per exercise rather than once per set (B97).
 *
 * **Warm-up sets are excluded from every load series**, which is only expressible
 * because N14's roles exist: a warm-up must not become the "heaviest set" on a chart.
 * Drop and failure sets are working sets and count.
 */
data class ExerciseTrendPoint(
    val startedAt: Instant,
    /** The heaviest working set, or null when the session logged none. */
    val heaviestSetGrams: Long? = null,
    /** Epley's estimate from [heaviestSetGrams], or null when it cannot be estimated. */
    val estimatedOneRepMaxGrams: Long? = null,
    /** `reps × weight` over the working sets. An assisted set contributes nothing (N15). */
    val volumeGrams: Long = 0L,
    val totalReps: Int = 0,
    /**
     * The *least* assistance among the working sets, or null when none was assisted.
     *
     * The least, not the most: on an assisted machine the hardest set is the one with
     * the least help, and that is the number progress moves.
     */
    val leastAssistanceGrams: Long? = null,
    val averageRpe: Double? = null,
    val averageMuscleFeel: Double? = null,
    /**
     * The **worst** joint this session's work reported, whichever it was (ROADMAP N63).
     *
     * Kept beside [jointPainBySite] rather than replaced by it: this is the one number that says "this
     * session hurt", and the sites say *where*. Averaging the two sides of a knee into one figure is
     * exactly what N130 fixed, and dropping the overall figure would lose the question it still answers.
     */
    val averageJointPain: Double? = null,
    /**
     * Each joint site's own worst score in this session, keyed by [jointSiteKey] (ROADMAP N130).
     *
     * **The dimension the average drops.** A rating records which joint, which side, how much — and
     * "left knee 6, right knee 2" charting as one 4 is a number about neither knee. A site absent from
     * this map is one this session did not name, which is a gap rather than a zero: it reads as null,
     * the same way an unmeasured tape site does.
     *
     * The **worst** rather than a mean over this session's exercises, for N63's reason: the same joint
     * aching through three movements is one sore knee, not three readings to average.
     */
    val jointPainBySite: Map<String, Double> = emptyMap(),
)

/** The series a per-exercise screen can draw (ROADMAP N17). */
enum class ExerciseTrendMetric {
    HEAVIEST_SET,
    ESTIMATED_1RM,
    VOLUME,
    TOTAL_REPS,
    ASSISTANCE,
    RPE,
    MUSCLE_FEEL,
    JOINT_PAIN,
    ;

    /** The value this metric takes from a point, or null when it was not recorded. */
    fun valueOf(point: ExerciseTrendPoint): Double? = when (this) {
        HEAVIEST_SET -> point.heaviestSetGrams?.toDouble()
        ESTIMATED_1RM -> point.estimatedOneRepMaxGrams?.toDouble()
        VOLUME -> point.volumeGrams.toDouble()
        TOTAL_REPS -> point.totalReps.toDouble()
        ASSISTANCE -> point.leastAssistanceGrams?.toDouble()
        RPE -> point.averageRpe
        MUSCLE_FEEL -> point.averageMuscleFeel
        JOINT_PAIN -> point.averageJointPain
    }

    /**
     * Whether a bigger number is better (N17's assisted direction).
     *
     * On an assisted machine more help is not progress, so the screen says which way is
     * forward rather than drawing a climb that reads as improvement. The number itself
     * is plotted as recorded; only the label changes.
     */
    val higherIsBetter: Boolean get() = this != ASSISTANCE

    /** Whether this metric is a load, and so drawn from zero rather than on 1–10. */
    val isLoad: Boolean
        get() = this == HEAVIEST_SET || this == ESTIMATED_1RM || this == VOLUME ||
            this == TOTAL_REPS || this == ASSISTANCE
}

/** One metric's values across the window, oldest first, null where it was not recorded. */
fun List<ExerciseTrendPoint>.valuesOf(metric: ExerciseTrendMetric): List<Double?> =
    map { metric.valueOf(it) }

/** The most recent recorded value, not `last()`: the newest session may lack this metric. */
fun List<ExerciseTrendPoint>.latestValue(metric: ExerciseTrendMetric): Double? =
    mapNotNull { metric.valueOf(it) }.lastOrNull()

/** The mean of the recorded values, or null when none were. */
fun List<ExerciseTrendPoint>.averageValue(metric: ExerciseTrendMetric): Double? {
    val recorded = mapNotNull { metric.valueOf(it) }
    return if (recorded.isEmpty()) null else recorded.average()
}

/**
 * Epley's one-rep-max estimate, or null when it would be a guess dressed as a number.
 *
 * `weight × (1 + reps / 30)`. A single rep *is* the max, so it returns the weight. Beyond
 * [MAX_ESTIMATED_REPS] the formula stops meaning anything — it extrapolates a max from
 * a set that was nowhere near one — so this returns null rather than a confident figure
 * the app cannot stand behind.
 */
fun estimatedOneRepMax(weightGrams: Long, reps: Int): Long? = when {
    weightGrams <= 0L -> null
    reps == 1 -> weightGrams
    // Rounded to the nearest half-kilo: the estimate is not precise to the gram, and
    // printing 25.333 kg would dress a formula's output as a measurement.
    reps in 2..MAX_ESTIMATED_REPS ->
        (weightGrams * (1.0 + reps / EPLEY_REPS_DIVISOR) / ESTIMATE_STEP_GRAMS)
            .roundToLong() * ESTIMATE_STEP_GRAMS

    else -> null
}

/** The estimate is rounded to this, because it is an estimate. */
private const val ESTIMATE_STEP_GRAMS = 500L

/** Epley's own divisor: `1 + reps / 30`. */
private const val EPLEY_REPS_DIVISOR = 30.0

/** Above this, Epley's estimate is extrapolation rather than arithmetic. */
const val MAX_ESTIMATED_REPS = 12

/**
 * The flat rows a session contributes, as the DAO returns them (N17).
 *
 * One row per logged set, with the session's ratings repeated, and a single row with a
 * null weight when the session recorded the exercise but logged no set at all. Keeping
 * the shape flat is what lets [toExerciseTrendPoints] hold all the arithmetic — the
 * grouping, the warm-up exclusion and the averages — in one pure, tested place.
 */
data class ExerciseTrendRow(
    val sessionId: String,
    /**
     * The exercise's row in this session — one identity per logged exercise (ROADMAP B97).
     *
     * The DAO returns one row per logged set and repeats the exercise's own rating on each, so this is what
     * makes "the exercise's rating" expressible when a **head** feeds several exercises of one family in.
     */
    val sessionExerciseId: String,
    val startedAt: Instant,
    val muscleFeel: Int?,
    val jointPain: Int?,
    val weightGrams: Long?,
    val reps: Int?,
    val rpeHalves: Int?,
    val setType: SetType?,
    val assistanceGrams: Long?,
    /** The lifter's weight if this set was bodyweight-loaded, or null (ROADMAP N219). */
    val bodyweightGrams: Long? = null,
)

/**
 * Groups the rows by session, in the order they arrived (oldest first), and computes
 * each session's series.
 *
 * [jointPainBySite] is passed in rather than derived from [rows] because the rows carry only the worst
 * joint's *score*, not which joint it was: the per-site read is its own query, and this function stays
 * the place the arithmetic lives.
 */
fun List<ExerciseTrendRow>.toExerciseTrendPoints(
    jointPainBySite: Map<String, Map<String, Double>> = emptyMap(),
): List<ExerciseTrendPoint> =
    groupBy { it.sessionId }
        .map { (_, rows) -> rows.first().let { it.startedAt to rows } }
        .sortedBy { it.first }
        .map { (startedAt, rows) -> rows.toPoint(startedAt, jointPainBySite) }

/**
 * The load this row was performed at, in grams (ROADMAP N219).
 *
 * [weightGrams] is null only when the exercise has no set in this session — a `LEFT JOIN` row, which the
 * caller filters out — so treating it as zero here is safe and keeps the arithmetic in one place.
 */
private val ExerciseTrendRow.performedLoadGrams: Long
    get() = loadGrams(weightGrams ?: 0L, bodyweightGrams, assistanceGrams ?: 0L)

private fun List<ExerciseTrendRow>.toPoint(
    startedAt: Instant,
    jointPainBySite: Map<String, Map<String, Double>>,
): ExerciseTrendPoint {
    // A warm-up is not a working set: it must not become the heaviest set on the chart.
    val working = filter { it.weightGrams != null && it.setType != SetType.WARMUP }
    // A bodyweight set carries the lifter as its load and is the heaviest set of the day if it is; an
    // assisted set carries what is left after the machine — `loadGrams` decides, once, for all three figures
    // below (ROADMAP N219).
    val heaviest = working.filter { it.performedLoadGrams > 0L }.maxByOrNull { it.performedLoadGrams }
    // One value per *exercise*, not per set (ROADMAP B97). A head feeds several exercises of one family into
    // this session, each stating its own rating while the DAO repeats it on that exercise's every set row —
    // so `first()` reported whichever row sorted first, and averaging the rows would weight a movement by how
    // many sets it happened to log. The ratings are the family's, averaged over the rows that state one.
    val perExercise = distinctBy { it.sessionExerciseId }
    // A head is several exercises in one session, so a site's score in this session is the worst of them —
    // the same rule N63 applies within one exercise, one level up (ROADMAP N130).
    val sites = perExercise
        .flatMap { jointPainBySite[it.sessionExerciseId].orEmpty().entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, scores) -> scores.max() }

    return ExerciseTrendPoint(
        startedAt = startedAt,
        heaviestSetGrams = heaviest?.performedLoadGrams,
        estimatedOneRepMaxGrams = heaviest?.let {
            estimatedOneRepMax(it.performedLoadGrams, it.reps ?: 0)
        },
        volumeGrams = working.sumOf { it.performedLoadGrams * (it.reps ?: 0) },
        totalReps = working.sumOf { it.reps ?: 0 },
        leastAssistanceGrams = working
            .mapNotNull { it.assistanceGrams?.takeIf { help -> help > 0L } }
            .minOrNull(),
        averageRpe = averageRpeOf(this),
        averageMuscleFeel = perExercise.mapNotNull { it.muscleFeel }.averageOrNull(),
        averageJointPain = perExercise.mapNotNull { it.jointPain }.averageOrNull(),
        jointPainBySite = sites,
    )
}

/** The mean of these, or null when there is nothing to average — a rating nobody recorded. */
private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()

/** The session's RPE average, in points, over the sets that recorded one (N6's halves). */
private fun averageRpeOf(rows: List<ExerciseTrendRow>): Double? {
    val halves = rows.mapNotNull { it.rpeHalves }
    return if (halves.isEmpty()) null else halves.average() / Rpe.HALVES_PER_POINT
}
