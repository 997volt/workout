package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

/**
 * The recovery gate N377 settled: what a sore muscle and a painful joint do to a step and to the offer.
 *
 * The two directions are deliberately different — pain stops the prescription, soreness only stops more
 * volume — and muscle feel is not a signal at all, which the function's signature states by not taking it.
 * These cases pin the thresholds, the precedence and the one day rule.
 */
class RecoveryGateTest {

    private val quads = MuscleGroup.QUADS
    private val glutes = MuscleGroup.GLUTES

    private fun verdict(
        sore: List<SoreMuscle> = emptyList(),
        joints: List<JointPain> = emptyList(),
        primary: MuscleGroup? = quads,
        secondary: List<MuscleGroup> = listOf(glutes),
    ): RecoveryVerdict = recoveryVerdict(
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        soreMuscles = sore,
        joints = joints,
    )

    private fun pain(score: Int, joint: Joint = Joint.KNEE, side: Side = Side.RIGHT) =
        JointPain(joint, side, score)

    @Test
    fun nothingRecorded_isClear() {
        val result = verdict()

        assertThat(result.isClear).isTrue()
        assertThat(result.holdsStep).isFalse()
        assertThat(result.holdsAdd).isFalse()
        assertThat(result.severeJoint).isNull()
    }

    @Test
    fun jointPainAtThree_isStillClear() {
        assertThat(verdict(joints = listOf(pain(3))).isClear).isTrue()
    }

    @Test
    fun jointPainAtFour_holdsTheStepAndTheOffer() {
        val result = verdict(joints = listOf(pain(4)))

        assertThat(result.holdsStep).isTrue()
        assertThat(result.holdsAdd).isTrue()
        assertThat(result.severeJoint).isNull()
        val hold = result.hold
        assertThat(hold).isInstanceOf(RecoveryHold.Joint::class.java)
        assertThat((hold as RecoveryHold.Joint).pain.score).isEqualTo(4)
    }

    @Test
    fun theWorstJointIsTheReason() {
        val result = verdict(
            joints = listOf(pain(5, Joint.SHOULDER, Side.LEFT), pain(6, Joint.KNEE, Side.RIGHT)),
        )

        assertThat((result.hold as RecoveryHold.Joint).pain.joint).isEqualTo(Joint.KNEE)
        assertThat(result.hold?.let { (it as RecoveryHold.Joint).pain.score }).isEqualTo(6)
    }

    @Test
    fun jointPainAtSeven_isAlsoSevere() {
        val result = verdict(joints = listOf(pain(6), pain(7)))

        assertThat(result.holdsStep).isTrue()
        assertThat(result.severeJoint?.score).isEqualTo(7)
        assertThat((result.hold as RecoveryHold.Joint).pain.score).isEqualTo(7)
    }

    @Test
    fun painOutranksSoreness_asTheReason() {
        val result = verdict(
            joints = listOf(pain(4)),
            sore = listOf(SoreMuscle(quads, 9)),
        )

        assertThat(result.hold).isInstanceOf(RecoveryHold.Joint::class.java)
    }

    @Test
    fun aSorePrimaryMuscleAtFour_isClear() {
        assertThat(verdict(sore = listOf(SoreMuscle(quads, 4))).isClear).isTrue()
    }

    @Test
    fun aSorePrimaryMuscleAtFive_holdsTheOfferOnly() {
        val result = verdict(sore = listOf(SoreMuscle(quads, 5)))

        assertThat(result.holdsStep).isFalse()
        assertThat(result.holdsAdd).isTrue()
        val hold = result.hold as RecoveryHold.Sore
        assertThat(hold.primary).isTrue()
        assertThat(hold.muscle).isEqualTo(quads)
        assertThat(hold.score).isEqualTo(5)
    }

    @Test
    fun aSoreSecondaryMuscleAtFive_isClear() {
        assertThat(verdict(sore = listOf(SoreMuscle(glutes, 5))).isClear).isTrue()
    }

    @Test
    fun aSoreSecondaryMuscleAtSix_holdsTheOfferOnly() {
        val result = verdict(sore = listOf(SoreMuscle(glutes, 6)))

        assertThat(result.holdsStep).isFalse()
        assertThat(result.holdsAdd).isTrue()
        val hold = result.hold as RecoveryHold.Sore
        assertThat(hold.primary).isFalse()
        assertThat(hold.muscle).isEqualTo(glutes)
    }

    @Test
    fun aSoreMuscleTheExerciseDoesNotTrain_isIgnored() {
        assertThat(verdict(sore = listOf(SoreMuscle(MuscleGroup.CALVES, 10))).isClear).isTrue()
    }

    @Test
    fun aMuscleBothPrimaryAndSecondary_readsAsPrimary() {
        val result = verdict(
            primary = quads,
            secondary = listOf(quads),
            sore = listOf(SoreMuscle(quads, 5)),
        )

        val hold = result.hold as RecoveryHold.Sore
        assertThat(hold.primary).isTrue()
        assertThat(result.holdsAdd).isTrue()
    }

    @Test
    fun anUnstatedPrimary_stillReadsTheSecondaryRule() {
        val result = verdict(primary = null, sore = listOf(SoreMuscle(glutes, 6)))

        assertThat(result.holdsAdd).isTrue()
        assertThat((result.hold as RecoveryHold.Sore).primary).isFalse()
    }

    @Test
    fun theSorestPrimaryIsTheReason() {
        val result = verdict(
            primary = quads,
            secondary = listOf(quads, glutes),
            sore = listOf(SoreMuscle(glutes, 8), SoreMuscle(quads, 6)),
        )

        val hold = result.hold as RecoveryHold.Sore
        assertThat(hold.primary).isTrue()
        assertThat(hold.score).isEqualTo(6)
    }

    @Test
    fun aRecordFromTheSameDaySpeaks() {
        val start = Instant.parse("2026-10-05T18:00:00Z")

        assertThat(sorenessSpeaksFor(Instant.parse("2026-10-05T06:00:00Z"), start, ZoneOffset.UTC))
            .isTrue()
    }

    @Test
    fun aRecordThreeDaysOldStillSpeaks_andFourDoesNot() {
        val start = Instant.parse("2026-10-05T18:00:00Z")
        val three = Instant.parse("2026-10-02T18:00:00Z")
        val four = Instant.parse("2026-10-01T18:00:00Z")

        assertThat(sorenessSpeaksFor(three, start, ZoneOffset.UTC)).isTrue()
        assertThat(sorenessSpeaksFor(four, start, ZoneOffset.UTC)).isFalse()
    }

    @Test
    fun aRecordMadeAfterTheSessionStarted_doesNotSpeakForIt() {
        val start = Instant.parse("2026-10-05T06:00:00Z")
        val later = Instant.parse("2026-10-05T18:00:00Z")

        assertThat(sorenessSpeaksFor(later, start, ZoneOffset.UTC)).isFalse()
    }

    @Test
    fun theDayIsTheSessionsOwn() {
        // 16:00 UTC on the 1st is already the 2nd in Tokyo, so a Tokyo session three local days later
        // still hears the record while a UTC reading calls it four days old.
        val tokyo = ZoneOffset.ofHours(9)
        val start = Instant.parse("2026-10-05T00:30:00Z")
        val record = Instant.parse("2026-10-01T16:00:00Z")

        assertThat(sorenessSpeaksFor(record, start, ZoneOffset.UTC)).isFalse()
        assertThat(sorenessSpeaksFor(record, start, tokyo)).isTrue()
    }
}
