package com.example.androidapp.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * What today's soreness and an exercise's own rating do to its next step (ROADMAP N377).
 *
 * The gate is **per exercise and per session**, from the lifter's own records: the day's sore-muscle list
 * and the joint-pain rating on the session exercise. It does not measure recovery — nothing this app records
 * does (N22) — it honours a fact the lifter stated, for the one lift it names, and only for the decision it
 * can answer.
 *
 * **Joint pain holds the step and the offer.** A score of 4 or more stops the auto progression step (reps,
 * load or a hold's seconds) and the earned-set offer; a score of 7 or more also asks the finish for the
 * deload/replace decision. Pain is the one signal that can stop a prescription, because a heavier bar on a
 * painful joint is the direction the plan should not take.
 *
 * **Soreness holds only the offer.** A sore primary muscle at 5 or more, or a sore secondary muscle at 6 or
 * more, withholds the extra-set suggestion and never the step: the set already earned its step by meeting
 * the plan, and soreness is what argues against more volume. A sore muscle the lift does not train is not
 * its business, and a record older than [SORE_STALE_DAYS] is not today's.
 *
 * **Muscle feel is not an input at all.** The rating is *how thoroughly the muscle was worked* (1 = barely,
 * 10 = fully), not how recovered it is, so it cannot say whether another set is safe or a step is earned.
 * It stays recorded and charted.
 *
 * Precedence is pain, then a sore primary, then a sore secondary: the line the finish states, and the one
 * the plan card reads, are the same first reason.
 */
sealed interface RecoveryHold {

    /** True when this hold stops the auto target step, not only the earned-set offer. */
    val holdsStep: Boolean

    /** Joint pain on the exercise, which stops both the step and the offer. */
    data class Joint(val pain: JointPain) : RecoveryHold {
        override val holdsStep: Boolean get() = true
    }

    /**
     * A sore muscle the exercise trains, which stops only the offer.
     *
     * [primary] is the muscle the movement is filed under rather than one it assists, which is why the two
     * have different thresholds.
     */
    data class Sore(val muscle: MuscleGroup, val score: Int, val primary: Boolean) : RecoveryHold {
        override val holdsStep: Boolean get() = false
    }
}

/** The gate's answer for one exercise and one session, and the reason it would state. */
data class RecoveryVerdict(
    /** The first hold in precedence order, or null when nothing holds. */
    val hold: RecoveryHold? = null,
    /** The worst joint at or above [JOINT_PAIN_SEVERE_SCORE], which owes the finish a decision. */
    val severeJoint: JointPain? = null,
) {
    /** True when the auto target step must not be written. */
    val holdsStep: Boolean get() = hold?.holdsStep == true

    /** True when the earned-set offer must not be made. */
    val holdsAdd: Boolean get() = hold != null

    /** True when nothing holds — the state that writes and offers exactly as before. */
    val isClear: Boolean get() = hold == null
}

/** The first joint score that holds a step and an offer (N377). */
const val JOINT_PAIN_HOLD_SCORE = 4

/** The joint score that also owes the finish a deload/replace decision (N377). */
const val JOINT_PAIN_SEVERE_SCORE = 7

/** The first score above 4 on a **primary** muscle that holds the offer (N377). */
const val PRIMARY_SORE_HOLD_SCORE = 5

/** The first score on a **secondary** muscle that holds the offer (N377). */
const val SECONDARY_SORE_HOLD_SCORE = 6

/** How old a daily soreness record may be and still speak for a session (N377). */
const val SORE_STALE_DAYS = 3L

/**
 * The gate's verdict for one exercise and one session (ROADMAP N377).
 *
 * [soreMuscles] is the day's record as it stands at the session's start — the caller has already applied
 * [sorenessSpeaksFor] and mapped it through [primaryMuscle] and [secondaryMuscles].
 *
 * The joints are the exercise's own rating, so no joint is mapped to a movement: the lifter put the score on
 * this lift, and the gate reads it there.
 */
fun recoveryVerdict(
    primaryMuscle: MuscleGroup?,
    secondaryMuscles: List<MuscleGroup>,
    soreMuscles: List<SoreMuscle>,
    joints: List<JointPain>,
): RecoveryVerdict {
    val worstJoint = joints.filter { it.score >= JOINT_PAIN_HOLD_SCORE }.maxByOrNull { it.score }
    val sorePrimary = primaryMuscle?.let { primary ->
        soreMuscles
            .filter { it.muscle == primary && it.score >= PRIMARY_SORE_HOLD_SCORE }
            .maxByOrNull { it.score }
    }
    val soreSecondary = soreMuscles
        .filter { it.muscle in secondaryMuscles && it.muscle != primaryMuscle }
        .filter { it.score >= SECONDARY_SORE_HOLD_SCORE }
        .maxByOrNull { it.score }
    return when {
        worstJoint != null -> RecoveryVerdict(
            hold = RecoveryHold.Joint(worstJoint),
            severeJoint = worstJoint.takeIf { it.score >= JOINT_PAIN_SEVERE_SCORE },
        )

        sorePrimary != null && primaryMuscle != null ->
            RecoveryVerdict(hold = RecoveryHold.Sore(primaryMuscle, sorePrimary.score, primary = true))

        soreSecondary != null ->
            RecoveryVerdict(hold = RecoveryHold.Sore(soreSecondary.muscle, soreSecondary.score, false))

        else -> RecoveryVerdict()
    }
}

/**
 * True when a daily record can still speak for a session starting at [sessionStart] (N377).
 *
 * Soreness is a fact about a day, not a stored recovery score: a record made after the session began is not
 * the readiness it was trained under, and one more than [SORE_STALE_DAYS] earlier is last week's. The callers
 * only see the day, so the rule is a day distance in the session's own zone — the same local-day rule the
 * measurements use (N25, N32).
 */
fun sorenessSpeaksFor(
    recordAt: Instant,
    sessionStart: Instant,
    zone: ZoneId,
): Boolean {
    if (recordAt.isAfter(sessionStart)) return false
    val days = ChronoUnit.DAYS.between(recordAt.localDay(zone), sessionStart.localDay(zone))
    return days in 0..SORE_STALE_DAYS
}
