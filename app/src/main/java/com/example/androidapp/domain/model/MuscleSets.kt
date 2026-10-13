package com.example.androidapp.domain.model

import com.example.androidapp.domain.effectivePrimaryMuscle
import com.example.androidapp.domain.effectiveSecondaryMuscles

/** What one working set is worth to the muscle it names as its primary, in half-sets (ROADMAP N122). */
const val PRIMARY_HALF_SETS = 2

/** What one working set is worth to each muscle it names as a secondary, in half-sets (ROADMAP N122). */
const val SECONDARY_HALF_SETS = 1

/** The divisor that reads a half-set count back as sets: two halves are one set (ROADMAP N122). */
const val HALF_SETS_PER_SET = 2

/**
 * One muscle's share of a plan, in **half-sets** (ROADMAP N119, N122).
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
 * history can answer it the week is over. (A rolling window over logged work is the parked P2.8, a different
 * question with a verdict in it.)
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
 * Ordered by count, largest first, ties in the taxonomy's own order, and the unresolved bucket last: the row a
 * lifter is looking for is the one at the top, and the one that says "this plan states no muscle" is a caveat
 * rather than a category.
 */
fun muscleBalance(
    planned: List<TemplateExercise>,
    library: List<Exercise>,
): List<MuscleSets> {
    val byId = library.associateBy { it.id }
    val credits = mutableMapOf<MuscleGroup?, Int>()

    fun credit(muscle: MuscleGroup?, halfSets: Int) {
        credits[muscle] = (credits[muscle] ?: 0) + halfSets
    }

    planned.forEach { row ->
        val working = row.sets.count { it.role.isWork }
        if (working == 0) return@forEach

        val exercise = byId[row.exerciseId]
        val primary = exercise?.effectivePrimaryMuscle(library)
        val secondaries = exercise?.effectiveSecondaryMuscles(library).orEmpty().distinct()

        if (primary == null && secondaries.isEmpty()) {
            credit(muscle = null, halfSets = working * PRIMARY_HALF_SETS)
            return@forEach
        }
        primary?.let { credit(muscle = it, halfSets = working * PRIMARY_HALF_SETS) }
        secondaries
            .filter { it != primary }
            .forEach { credit(muscle = it, halfSets = working * SECONDARY_HALF_SETS) }
    }

    return credits
        .map { (muscle, halfSets) -> MuscleSets(muscle, halfSets) }
        .filter { it.halfSets > 0 }
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
