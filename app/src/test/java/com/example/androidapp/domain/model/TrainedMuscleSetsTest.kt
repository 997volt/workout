package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * What the log trained, counted the same way as a plan (ROADMAP N289).
 *
 * The counts arrive from the database already grouped by movement, so the cases worth pinning are the ones
 * that could make the screen state something that did not happen: a primary credited once per set, a secondary
 * at half, a rung counted while a warm-up is not, a movement's family muscle inherited, and work the library
 * cannot name kept rather than dropped.
 *
 * The windows are pinned to their **dates** rather than to durations — seven and twenty-eight dates including
 * today, half-open at the far end — because that is the rule a lifter reads "last 7 days" as.
 */
class TrainedMuscleSetsTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 2)

    private fun movement(
        id: String,
        muscle: MuscleGroup?,
        parent: String? = null,
        secondaries: List<MuscleGroup> = emptyList(),
    ) = Exercise(
        id = id,
        name = id,
        primaryMuscle = muscle,
        secondaryMuscles = secondaries,
        parentId = parent,
    )

    private fun category(id: String, muscle: MuscleGroup?) = Exercise(
        id = id,
        name = id,
        primaryMuscle = muscle,
        rowKind = RowKind.CATEGORY,
    )

    private fun sets(exerciseId: String, workingSets: Int) =
        ExerciseWorkingSets(exerciseId = exerciseId, workingSets = workingSets)

    private fun at(date: LocalDate, hour: Int = 12): Instant =
        date.atTime(hour, 0).atZone(zone).toInstant()

    @Test
    fun aPrimaryTakesEveryWorkingSet_andASecondaryTakesHalfOfEach() {
        val library = listOf(
            movement(
                "barbell-row",
                MuscleGroup.LATS,
                secondaries = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            ),
        )

        val balance = trainedMuscleBalance(trained = listOf(sets("barbell-row", 3)), library = library)

        // Three sets: lats 3, biceps 1.5, forearms 1.5 — the plan's own credits, over what was performed.
        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.LATS, 6),
            MuscleSets(MuscleGroup.BICEPS, 3),
            MuscleSets(MuscleGroup.FOREARMS, 3),
        )
    }

    @Test
    fun aMovementInheritsItsCategorysMuscles() {
        // N110, read live: the log names the movement, but only the library knows its family.
        val library = listOf(
            category("cat-row", MuscleGroup.LATS),
            movement("cable-row", muscle = null, parent = "cat-row"),
        )

        val balance = trainedMuscleBalance(trained = listOf(sets("cable-row", 2)), library = library)

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.LATS, 4))
    }

    @Test
    fun aMuscleThatIsBothPrimaryAndSecondary_isCountedOnce_andADuplicateSecondaryToo() {
        val library = listOf(
            movement(
                "leg-press",
                MuscleGroup.QUADS,
                secondaries = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.HAMSTRINGS),
            ),
        )

        val balance = trainedMuscleBalance(trained = listOf(sets("leg-press", 2)), library = library)

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.QUADS, 4),
            MuscleSets(MuscleGroup.HAMSTRINGS, 2),
        )
    }

    @Test
    fun twoRowsForOneMovement_areOneCount() {
        // A query may return a second row for the same id — a movement trained in two sessions — and the log
        // credits the movement once per set, not once per row.
        val library = listOf(movement("squat", MuscleGroup.QUADS))

        val balance = trainedMuscleBalance(
            trained = listOf(sets("squat", 2), sets("squat", 3)),
            library = library,
        )

        assertThat(balance).containsExactly(MuscleSets(MuscleGroup.QUADS, 10))
    }

    @Test
    fun workTheLibraryCannotName_keepsTheUnresolvedBucket_ratherThanVanishing() {
        // A row the library no longer holds is work that happened; dropping it would understate the log and
        // make the rest look more balanced than it is (N122's bucket, read over history).
        val library = listOf(movement("squat", MuscleGroup.QUADS))

        val balance = trainedMuscleBalance(
            trained = listOf(sets("squat", 1), sets("mystery", 2)),
            library = library,
        )

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.QUADS, 2),
            MuscleSets(null, 4),
        )
    }

    @Test
    fun secondariesWithoutAPrimary_areCredited_andInventNoPrimary() {
        val library = listOf(
            movement("carry", muscle = null, secondaries = listOf(MuscleGroup.FOREARMS, MuscleGroup.CORE)),
        )

        val balance = trainedMuscleBalance(trained = listOf(sets("carry", 2)), library = library)

        assertThat(balance).containsExactly(
            MuscleSets(MuscleGroup.FOREARMS, 2),
            MuscleSets(MuscleGroup.CORE, 2),
        )
    }

    @Test
    fun noWorkInTheWindow_hasNoRows() {
        val balance = trainedMuscleBalance(
            trained = listOf(sets("squat", 0)),
            library = listOf(movement("squat", MuscleGroup.QUADS)),
        )

        assertThat(balance).isEmpty()
    }

    @Test
    fun theBiggestTotalsComeFirst_andTiesFollowTheTaxonomy() {
        val library = listOf(
            movement("squat", MuscleGroup.QUADS),
            movement("curl", MuscleGroup.BICEPS),
            movement("row", MuscleGroup.LATS),
        )

        val balance = trainedMuscleBalance(
            trained = listOf(sets("curl", 1), sets("row", 1), sets("squat", 3)),
            library = library,
        )

        assertThat(balance.map { it.muscle }).containsExactly(
            MuscleGroup.QUADS,
            MuscleGroup.LATS,
            MuscleGroup.BICEPS,
        ).inOrder()
    }

    @Test
    fun warmUpsAreNotWork_andEveryOtherRoleIs() {
        // The query is handed these names instead of spelling a literal, so this is where N112's rule is
        // pinned: a ramp cannot enter the count, while a rung — which is work (N79) — stays in.
        val roles = loggedWorkRoles()

        assertThat(roles).doesNotContain(SetType.WARMUP.name)
        assertThat(roles).containsAtLeast(
            SetType.NORMAL.name,
            SetType.TOP_SET.name,
            SetType.DROP.name,
            SetType.CLUSTER.name,
            SetType.FAILURE.name,
        )
        assertThat(roles).hasSize(SetType.entries.count { it.isWork })
    }

    @Test
    fun sevenDays_coverTodayAndTheSixDatesBeforeIt() {
        val window = TrainedWindow.LAST_7_DAYS.window(today, zone)

        assertThat(window.from).isEqualTo(at(LocalDate.of(2026, 9, 26), hour = 0))
        assertThat(window.toExclusive).isEqualTo(at(LocalDate.of(2026, 10, 3), hour = 0))
        assertThat(at(LocalDate.of(2026, 9, 26), hour = 0) in window).isTrue()
        assertThat(at(today, hour = 23) in window).isTrue()
    }

    @Test
    fun twentyEightDays_coverTodayAndTheTwentySevenDatesBeforeIt() {
        val window = TrainedWindow.LAST_28_DAYS.window(today, zone)

        assertThat(window.from).isEqualTo(at(LocalDate.of(2026, 9, 5), hour = 0))
        assertThat(window.toExclusive).isEqualTo(at(LocalDate.of(2026, 10, 3), hour = 0))
    }

    @Test
    fun aWindowIsHalfOpen_theInstantBeforeItAndTomorrowsMidnightAreNotInIt() {
        val window = TrainedWindow.LAST_7_DAYS.window(today, zone)

        val firstMidnight = at(LocalDate.of(2026, 9, 26), hour = 0)
        val justBefore = Instant.ofEpochMilli(firstMidnight.toEpochMilli() - 1)

        assertThat(justBefore in window).isFalse()
        assertThat(at(LocalDate.of(2026, 10, 3), hour = 0) in window).isFalse()
    }

    @Test
    fun aWindowFollowsTheZoneItIsReadIn() {
        // The screen's day is the device's, so the boundaries are that zone's midnights rather than UTC's.
        val tokyo = ZoneId.of("Asia/Tokyo")
        val window = TrainedWindow.LAST_7_DAYS.window(today, tokyo)

        assertThat(window.from).isEqualTo(LocalDate.of(2026, 9, 26).atStartOfDay(tokyo).toInstant())
        assertThat(window.toExclusive).isEqualTo(LocalDate.of(2026, 10, 3).atStartOfDay(tokyo).toInstant())
    }

    @Test
    fun theSameWindowResolvedLaterCoversTheLaterDates() {
        // The window is an intent over dates, not a frozen pair: resolved tomorrow it starts and ends a day
        // later, which is what keeps a screen open across midnight from showing yesterday's week (N286's rule).
        val now = TrainedWindow.LAST_7_DAYS.window(today, zone)
        val tomorrow = TrainedWindow.LAST_7_DAYS.window(today.plusDays(1), zone)

        assertThat(tomorrow.from).isEqualTo(now.from.plusSeconds(86_400))
        assertThat(tomorrow.toExclusive).isEqualTo(now.toExclusive.plusSeconds(86_400))
    }
}
