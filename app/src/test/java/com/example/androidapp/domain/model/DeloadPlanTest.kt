package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The deload week's arithmetic (ROADMAP N147).
 *
 * The rule is the whole feature's opinion: the set count follows the plan up to three, the reps are its
 * highest target floored at five, and the load is the plan's heaviest (or history's) kept at the chosen
 * share. It is pure so it can be argued with here rather than by tapping through a screen.
 */
class DeloadPlanTest {

    private fun set(
        index: Int,
        role: SetType = SetType.NORMAL,
        weightGrams: Long? = 100_000L,
        assistanceGrams: Long? = null,
        repsMin: Int? = 5,
        repsMax: Int? = 5,
    ) = TemplateSet(
        id = "ts$index",
        templateExerciseId = "te1",
        setIndex = index,
        role = role,
        targetWeightGrams = weightGrams,
        targetAssistanceGrams = assistanceGrams,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        targetRepsCurrent = null,
        note = null,
    )

    private fun performed(
        index: Int,
        reps: Int,
        weightGrams: Long = 100_000L,
        assistanceGrams: Long = 0L,
        role: SetType = SetType.NORMAL,
    ) = SetEntry(
        id = "p$index",
        sessionExerciseId = "se1",
        setIndex = index,
        reps = reps,
        weightGrams = weightGrams,
        assistanceGrams = assistanceGrams,
        setType = role,
    )

    @Test
    fun aCompoundsThreeHeavySets_keepThreeLighterOnes() {
        // The case a flat "two sets" got wrong: the plan's count is what says how much work the lift does.
        val targets = deloadSetTargets(
            plannedSets = List(3) { set(it) },
            previous = null,
            loadKeptPercent = 60,
            stepGrams = 2_500L,
        )

        assertThat(targets).hasSize(3)
        assertThat(targets.map { it.weightGrams }).containsExactly(60_000L, 60_000L, 60_000L)
        assertThat(targets.map { it.reps }).containsExactly(5, 5, 5)
    }

    @Test
    fun aFiveByFive_keepsThree() {
        // Past three the set count is the volume the week exists to remove.
        val targets = deloadSetTargets(List(5) { set(it) }, null, stepGrams = 2_500L)

        assertThat(targets).hasSize(3)
    }

    @Test
    fun aTwoSetAccessory_staysTwo() {
        val targets = deloadSetTargets(List(2) { set(it) }, null, stepGrams = 2_500L)

        assertThat(targets).hasSize(2)
    }

    @Test
    fun anExerciseWithNoPlannedSets_getsOne() {
        val targets = deloadSetTargets(emptyList(), null, stepGrams = 2_500L)

        assertThat(targets).hasSize(1)
        assertThat(targets.single().reps).isNull()
        assertThat(targets.single().weightGrams).isNull()
    }

    @Test
    fun warmUps_andRungs_areNotSetsOfTheirOwn() {
        // A warm-up is preparation and a rung belongs to the set above it (N112, N79). Counting either
        // would make a 1x5 with a ramp look like a five-set exercise.
        val planned = listOf(
            set(0, role = SetType.WARMUP, weightGrams = 40_000L),
            set(1, role = SetType.WARMUP, weightGrams = 60_000L),
            set(2, weightGrams = 100_000L),
            set(3, role = SetType.DROP, weightGrams = 80_000L),
        )

        val targets = deloadSetTargets(planned, null, stepGrams = 2_500L)

        assertThat(targets).hasSize(1)
        assertThat(targets.single().weightGrams).isEqualTo(60_000L)
    }

    @Test
    fun theHighestRepTarget_isTheOneNumberEverySetGets() {
        val planned = listOf(
            set(0, repsMin = 3, repsMax = 3),
            set(1, repsMin = 8, repsMax = 12),
            set(2, repsMin = 5, repsMax = 5),
        )

        val targets = deloadSetTargets(planned, null, stepGrams = 2_500L)

        assertThat(targets.map { it.reps }).containsExactly(12, 12, 12)
    }

    @Test
    fun aPlannedSingle_isRaisedToFive() {
        // One rep at a deload load trains nothing, and the screen said five would be the floor.
        val targets = deloadSetTargets(listOf(set(0, repsMin = 1, repsMax = 1)), null, stepGrams = 2_500L)

        assertThat(targets).hasSize(1)
        assertThat(targets.single().reps).isEqualTo(5)
    }

    @Test
    fun withNoPlannedLoad_theLastSessionIsWhatIsReduced() {
        val previous = PreviousPerformance(listOf(performed(0, reps = 8, weightGrams = 120_000L)))

        val targets = deloadSetTargets(
            plannedSets = listOf(set(0, weightGrams = null, repsMin = null, repsMax = null)),
            previous = previous,
            loadKeptPercent = 50,
            stepGrams = 2_500L,
        )

        assertThat(targets).hasSize(1)
        assertThat(targets.single().weightGrams).isEqualTo(60_000L)
        assertThat(targets.single().reps).isEqualTo(8)
    }

    @Test
    fun withNeitherPlanNorHistory_thereIsNoLoadToState() {
        // Zero would be a claim (N14); the draft says nothing and the session falls back to its own prefill.
        val targets = deloadSetTargets(listOf(set(0, weightGrams = null)), null, stepGrams = 2_500L)

        assertThat(targets.single().weightGrams).isNull()
        assertThat(targets.single().assistanceGrams).isNull()
    }

    @Test
    fun anAssistedMovement_keepsItsHelp_andDoesNotHalveIt() {
        // Assistance is a magnitude: halving it would make the deload harder (N15).
        val planned = listOf(set(0, weightGrams = 0L, assistanceGrams = 20_000L))

        val targets = deloadSetTargets(planned, null, loadKeptPercent = 50, stepGrams = 2_500L)

        assertThat(targets.single().weightGrams).isEqualTo(0L)
        assertThat(targets.single().assistanceGrams).isEqualTo(20_000L)
    }

    @Test
    fun theLoad_isRoundedToTheExercisesOwnStep() {
        // 102.5 kg at 60% is 61.5, which is not a weight the bar can be loaded with (N77).
        val targets = deloadSetTargets(
            plannedSets = listOf(set(0, weightGrams = 102_500L)),
            previous = null,
            loadKeptPercent = 60,
            stepGrams = 2_500L,
        )

        assertThat(targets.single().weightGrams).isEqualTo(62_500L)
    }

    @Test
    fun theShare_isTheLiftersChoice_andMovesEveryLoad() {
        val planned = listOf(set(0))

        assertThat(deloadSetTargets(planned, null, loadKeptPercent = 50, stepGrams = 2_500L).single().weightGrams)
            .isEqualTo(50_000L)
        assertThat(deloadSetTargets(planned, null, loadKeptPercent = 70, stepGrams = 2_500L).single().weightGrams)
            .isEqualTo(70_000L)
    }

    @Test
    fun aPlanThatStatesALoad_isNotOverruledByHistory() {
        val targets = deloadSetTargets(
            plannedSets = listOf(set(0, weightGrams = 100_000L)),
            previous = PreviousPerformance(listOf(performed(0, reps = 5, weightGrams = 200_000L))),
            stepGrams = 2_500L,
        )

        assertThat(targets.single().weightGrams).isEqualTo(60_000L)
    }

    @Test
    fun aSessionFromADeloadDay_settlesIt() {
        // P3.3's rule: a start silences the day, finished or not, and the session's own week counts.
        val day = DeloadDay(
            id = "d1",
            programId = "p1",
            templateId = "t1",
            templateName = "Heavy lower — deload",
            weekday = DayOfWeek.MONDAY,
            position = 0,
        )
        val today = LocalDate.of(2026, 10, 7) // Wednesday
        val session = ProgramSession(
            sessionId = "s1",
            templateId = "t1",
            startedAt = Instant.parse("2026-10-05T18:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        val pending = deloadPendingOccurrences(listOf(day), listOf(session), today)

        assertThat(pending).isEmpty()
    }

    @Test
    fun anUnresolvedPastDay_isPending_withNoSlot() {
        val days = listOf(
            DeloadDay(
                id = "d1",
                programId = "p1",
                templateId = "t1",
                templateName = "Heavy lower — deload",
                weekday = DayOfWeek.MONDAY,
                position = 0,
            ),
            DeloadDay(
                id = "d2",
                programId = "p1",
                templateId = "t2",
                templateName = "Push — deload",
                weekday = DayOfWeek.FRIDAY,
                position = 1,
            ),
            DeloadDay(
                id = "d3",
                programId = "p1",
                templateId = "t3",
                templateName = "No day",
                weekday = null,
                position = 2,
            ),
        )

        val pending = deloadPendingOccurrences(days, emptyList(), LocalDate.of(2026, 10, 7))

        // Monday has passed; Friday has not; the order-only day can never be missed.
        assertThat(pending).hasSize(1)
        assertThat(pending.single().templateId).isEqualTo("t1")
        assertThat(pending.single().slotId).isNull()
        assertThat(pending.single().date).isEqualTo(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun aSessionFromAnotherWeek_doesNotSettleThisOne() {
        val day = DeloadDay(
            id = "d1",
            programId = "p1",
            templateId = "t1",
            templateName = "Heavy lower — deload",
            weekday = DayOfWeek.MONDAY,
            position = 0,
        )
        val otherWeek = ProgramSession(
            sessionId = "s1",
            templateId = "t1",
            startedAt = Instant.parse("2026-09-28T18:00:00Z"),
            zone = ZoneOffset.UTC,
        )

        val pending = deloadPendingOccurrences(listOf(day), listOf(otherWeek), LocalDate.of(2026, 10, 7))

        assertThat(pending).hasSize(1)
    }
}
