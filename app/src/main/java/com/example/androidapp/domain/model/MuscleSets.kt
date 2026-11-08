package com.example.androidapp.domain.model

import com.example.androidapp.domain.effectivePrimaryMuscle
import com.example.androidapp.domain.effectiveSecondaryMuscles
import java.time.LocalDate
import java.time.ZoneId

/** What one working set is worth to the muscle it names as its primary, in half-sets (ROADMAP N122). */
const val PRIMARY_HALF_SETS = 2

/** What one working set is worth to each muscle it names as a secondary, in half-sets (ROADMAP N122). */
const val SECONDARY_HALF_SETS = 1

/** The divisor that reads a half-set count back as sets: two halves are one set (ROADMAP N122). */
const val HALF_SETS_PER_SET = 2

/**
 * One muscle's share of a plan or of the logged work, in **half-sets** (ROADMAP N119, N122, N289).
 *
 * A half-set rather than a fraction, and an `Int` rather than a `Double`, for the reason RPE is half-points:
 * a half is exact where a float is not, and this number is summed, compared and divided rather than measured.
 *
 * [muscle] is null for the bucket work whose muscle is stated **nowhere** lands in — no effective primary and
 * no effective secondaries — and **dropping it would make the rest of the plan look more balanced than it
 * is**. Naming it is the honest answer.
 */
data class MuscleSets(val muscle: MuscleGroup?, val halfSets: Int)

/**
 * How a program's planned work spreads across muscles (ROADMAP N119, N122).
 *
 * The shape of a week read *before* it is trained, which is why it reads the plan rather than the log: what a
 * lifter wants to know while authoring is whether the week trains what they think it does, and by the time the
 * history can answer it the week is over. The log's own answer is a second count, [trainedMuscleBalance]
 * (N289); a **verdict** over either is the parked P2.8, a different question.
 *
 * Three rules are inherited rather than restated here:
 *
 * - **A warm-up is not work** (N112), so a ramp does not inflate the muscle it warms up. Counting every planned
 *   set would make a squat day look like it trained quads twice as hard as it does.
 * - **A movement's muscles are own-if-stated, else its category's** (N110), which is why the library is a
 *   parameter: the resolver that decides it is the same one the library, the picker and the session rows read.
 * - **A set counts once for its primary and half for each secondary** (N122). A movement's declared secondaries
 *   are muscles the set really works — a barbell row's biceps and forearms — so crediting only the prime mover
 *   would make a week of rows read as training lats alone. Assisted work is still not split off, and the half
 *   is a share of one set rather than a second set.
 *
 * A muscle is counted once per set: one that is both the primary and a secondary, or named twice in the
 * secondary list, takes the primary's share and no more. **Work that states a muscle nowhere keeps a bucket of
 * its own** rather than being dropped from the total, and a movement that states secondaries but no primary
 * credits them — its own share has no muscle to be counted against, so none is invented for it.
 *
 * **Every credit is positive**, which is why the rows are built without a filter: a row with no working set is
 * skipped before it credits anything, and each credit is one or two halves per working set. The unresolved
 * bucket is one of those rows rather than something excluded from them — what puts it last is the ordering's
 * `muscle == null` tie-break (B124).
 *
 * Ordered by count, largest first, ties in the taxonomy's own order, and the unresolved bucket last: the row a
 * lifter is looking for is the one at the top, and the one that says "this plan states no muscle" is a caveat
 * rather than a category.
 */
fun muscleBalance(
    planned: List<TemplateExercise>,
    library: List<Exercise>,
): List<MuscleSets> {
    val working = mutableMapOf<String, Int>()
    planned.forEach { row ->
        val sets = row.sets.count { it.role.isWork }
        if (sets > 0) working[row.exerciseId] = (working[row.exerciseId] ?: 0) + sets
    }
    return musclesFor(working, library)
}

/**
 * How many working sets the log holds for one movement inside a window (ROADMAP N289).
 *
 * The count comes from the database already aggregated, so the balance view never loads a set per muscle;
 * what is left to the domain is the counting rule and the muscles, which only the library can answer.
 */
data class ExerciseWorkingSets(val exerciseId: String, val workingSets: Int)

/**
 * The windows the trained side of a balance view offers (ROADMAP N289).
 *
 * Days, not durations, for the reason [StatisticsRange]'s windows are: "the last 7 days" means seven dates
 * including today, whatever the length of a day. The week is today and the six before it; the 28-day window
 * is today and the twenty-seven before it — the four-week span a lifter checks a month's work in.
 */
enum class TrainedWindow(val days: Long) {
    LAST_7_DAYS(DAYS_IN_A_WEEK),
    LAST_28_DAYS(DAYS_IN_FOUR_WEEKS),
    ;

    /**
     * The instants this window covers: the start of its first day to the start of tomorrow, exclusive.
     *
     * Resolved against [today] and [zone] rather than the clock inside, so a test can ask what the window
     * covers and a screen can decide which day it is showing.
     */
    fun window(today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): TimeWindow =
        TimeWindow(
            from = today.minusDays(days - ONE_DAY).atStartOfDay(zone).toInstant(),
            toExclusive = today.plusDays(ONE_DAY).atStartOfDay(zone).toInstant(),
        )
}

/**
 * What the log actually trained, per muscle, as counts (ROADMAP N289).
 *
 * The same rule as [muscleBalance], over what was performed rather than what was planned: a working set is
 * worth one to its primary and a half to each secondary, and a movement's muscles are own-if-stated, else its
 * category's (N110, N122). The four differences are the ones the question implies:
 *
 * - **The counts arrive already aggregated by movement**, from a query that excludes warm-ups and deleted
 *   rows (N112), so nothing here loads a set or re-decides what is work.
 * - **A movement the library no longer holds** — its row removed, or the read that would name it failed —
 *   credits the unresolved bucket rather than disappearing, exactly as plan work that states no muscle does.
 *   Dropping it would make the rest of the log look more balanced than it is.
 * - **A window is part of the question**, not a filter this function invents: the caller reads [TrainedWindow]'s
 *   instants and asks the database for that range.
 * - **It is a factual read, never a verdict.** There is no target range, no minimum, and no warning here;
 *   that question is the parked P2.8, and this type deliberately cannot answer it.
 *
 * Ordered by count, largest first, ties in the taxonomy's own order, and the unresolved bucket last (B124).
 */
fun trainedMuscleBalance(
    trained: List<ExerciseWorkingSets>,
    library: List<Exercise>,
): List<MuscleSets> {
    val working = mutableMapOf<String, Int>()
    trained.forEach { movement ->
        if (movement.workingSets > 0) {
            working[movement.exerciseId] = (working[movement.exerciseId] ?: 0) + movement.workingSets
        }
    }
    return musclesFor(working, library)
}

/**
 * The stored set roles that are work rather than preparation (ROADMAP N112, N289).
 *
 * The trained read's SQL cannot name the enum, so it is handed these names. Taking them from
 * [SetType.isWork] is what keeps one rule: a warm-up is not work, a rung is, and a role added later is
 * counted or skipped by that rule rather than by a list the query keeps in parallel (B155's lesson).
 */
fun loggedWorkRoles(): List<String> = SetType.entries.filter { it.isWork }.map { it.name }

/**
 * The credits both sides of the balance view share: one list from a count per movement.
 *
 * A set counts once for its primary and half for each distinct secondary, and a movement that states neither
 * keeps the unresolved bucket (N122, B124). Every credit is positive, so a zero count is skipped here rather
 * than filtered from the rows afterwards.
 */
private fun musclesFor(working: Map<String, Int>, library: List<Exercise>): List<MuscleSets> {
    val byId = library.associateBy { it.id }
    val credits = mutableMapOf<MuscleGroup?, Int>()

    fun credit(muscle: MuscleGroup?, halfSets: Int) {
        credits[muscle] = (credits[muscle] ?: 0) + halfSets
    }

    working.forEach { (exerciseId, workingSets) ->
        if (workingSets <= 0) return@forEach

        val exercise = byId[exerciseId]
        val primary = exercise?.effectivePrimaryMuscle(library)
        val secondaries = exercise?.effectiveSecondaryMuscles(library).orEmpty().distinct()

        if (primary == null && secondaries.isEmpty()) {
            credit(muscle = null, halfSets = workingSets * PRIMARY_HALF_SETS)
            return@forEach
        }
        primary?.let { credit(muscle = it, halfSets = workingSets * PRIMARY_HALF_SETS) }
        secondaries
            .filter { it != primary }
            .forEach { credit(muscle = it, halfSets = workingSets * SECONDARY_HALF_SETS) }
    }

    return credits
        .map { (muscle, halfSets) -> MuscleSets(muscle, halfSets) }
        .sortedWith(
            compareBy({ it.muscle == null }, { -it.halfSets }, { it.muscle?.ordinal ?: 0 }),
        )
}

/**
 * How many working sets the plan writes, warm-ups excluded (ROADMAP N112, N122).
 *
 * The balance screen's header states this rather than the sum of the credits, because it is the plan's own
 * size — the number a lifter counts in the editor — and the credits no longer sum to it once a set is worth
 * half to each muscle it assists.
 */
fun plannedWorkingSets(planned: List<TemplateExercise>): Int =
    planned.sumOf { row -> row.sets.count { it.role.isWork } }

/** Seven dates, today included: the week a lifter usually checks. */
private const val DAYS_IN_A_WEEK = 7L

/** Four weeks of dates, today included (N289). */
private const val DAYS_IN_FOUR_WEEKS = 28L

/** The window's last day is tomorrow at midnight, exclusive, so one is added and one subtracted. */
private const val ONE_DAY = 1L
