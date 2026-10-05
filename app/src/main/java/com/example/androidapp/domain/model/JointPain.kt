package com.example.androidapp.domain.model

/**
 * One joint a rating reported painful, which side of it, and how much (ROADMAP N63).
 *
 * The per-exercise rating used to carry a single `jointPain` number and a free-text "which joints"
 * line. A number with no location cannot be read back a month later, and the location as a sentence
 * cannot be charted — so the location is picked from the body's own joints and each picked one
 * carries its own score, the same shape N62's sore-muscle list uses.
 *
 * [score] is on the shared [TenPointScale], so the app keeps one vocabulary for "how much".
 */
data class JointPain(
    val joint: Joint,
    val side: Side,
    /** 1–10, on [TenPointScale]. */
    val score: Int,
)

/**
 * The joints a rating may name, in the order the picker offers them (ROADMAP N63).
 *
 * [NECK] and [LOWER_BACK] are central: they have no left and right, so they are only ever offered
 * as [Side.CENTRE]. The paired ones offer [Side.LEFT] and [Side.RIGHT] as separate entries, because
 * "knee 6" is half a sentence.
 */
enum class Joint(val label: String) {
    SHOULDER("Shoulder"),
    ELBOW("Elbow"),
    WRIST("Wrist"),
    HIP("Hip"),
    KNEE("Knee"),
    ANKLE("Ankle"),
    NECK("Neck"),
    LOWER_BACK("Lower back"),
}

/** Which side of a joint hurts (ROADMAP N63). [CENTRE] belongs to the joints that have no sides. */
enum class Side(val label: String) {
    LEFT("Left"),
    RIGHT("Right"),
    CENTRE("Centre"),
}

/**
 * The joints that come in a left and a right (ROADMAP N63).
 *
 * A list rather than a property on the enum so the one place that decides which sides a joint offers
 * is [Joint.offeredSides], and nothing else has to remember which is which.
 */
val PAIRED_JOINTS: List<Joint> = listOf(
    Joint.SHOULDER,
    Joint.ELBOW,
    Joint.WRIST,
    Joint.HIP,
    Joint.KNEE,
    Joint.ANKLE,
)

/** The sides a joint can be offered on: both for a paired one, and only [Side.CENTRE] otherwise. */
fun Joint.offeredSides(): List<Side> =
    if (this in PAIRED_JOINTS) listOf(Side.LEFT, Side.RIGHT) else listOf(Side.CENTRE)

/**
 * Every (joint, side) a rating may name, in picker order (ROADMAP N63).
 *
 * Built from [Joint.entries] and [offeredSides], so a central joint can never be offered a left or
 * right and the picker has no list of its own to drift out of step.
 */
val JOINT_SITES: List<Pair<Joint, Side>> =
    Joint.entries.flatMap { joint -> joint.offeredSides().map { side -> joint to side } }

/**
 * The stable id of one (joint, side) site (ROADMAP N63).
 *
 * The pair, not the [JointPain] row: a rating's identity is *which* joint on *which* side, so a test
 * tag and a picker callback both name the same thing while the score is free to change. Left and
 * right are separate ids, which is the whole point.
 */
fun jointSiteKey(joint: Joint, side: Side): String = "${joint.name}_${side.name}"

/**
 * The label a joint site is shown under, e.g. `Left knee` or `Neck`.
 *
 * A central joint carries no side word: "Centre neck" would be a sentence the body does not say.
 */
fun jointSiteLabel(joint: Joint, side: Side): String =
    if (side == Side.CENTRE) joint.label else "${side.label} ${joint.label.lowercase()}"

/** The label a picked joint is shown under, e.g. `Left knee` or `Neck`. */
val JointPain.label: String
    get() = jointSiteLabel(joint, side)
