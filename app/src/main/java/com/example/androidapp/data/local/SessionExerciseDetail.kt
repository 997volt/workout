package com.example.androidapp.data.local

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.SessionExercise
import java.time.Instant

/**
 * A session exercise joined with the exercise it refers to.
 *
 * Room projects the join straight into this type, so filling a workout screen is
 * one query instead of a session query plus an N+1 walk over the library. That
 * matters on the screen the user is staring at mid-set.
 */
data class SessionExerciseDetail(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val position: Int,
    val exerciseName: String,
    /**
     * The muscle this exercise trains, or null when nothing states one (ROADMAP B103, N103).
     *
     * Nullable because the column is: N103 made *unstated* a real value, a custom exercise is created
     * with a name and nothing else, and a movement may leave the fact to the category it is filed
     * under. The query resolves that parent, so this is what the exercise **effectively** trains —
     * and a row that resolves to nothing reads as absent rather than crashing the read.
     */
    val primaryMuscle: MuscleGroup?,
    /** The equipment, or null when unstated (ROADMAP B103). Never inherited: a family spans equipment. */
    val equipment: Equipment?,
    /** The library exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int?,
    /** Shown under the exercise name while lifting (ROADMAP N5). */
    val techniqueNote: String?,
    /**
     * The library exercise's own display unit by name, or null to follow the app setting (N64).
     *
     * Read from the library rather than snapshotted onto the session, unlike the rest and cue: a
     * display preference is not part of what the plan prescribed, and changing it should reach a
     * workout that is already open.
     */
    val weightUnit: String?,
    /** The library exercise's own weight step, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long?,
    /** Whether the movement is loaded by the body (ROADMAP N219). */
    val isBodyweight: Boolean,
    /** Whether a set of this movement is held rather than counted (ROADMAP N220). */
    val isTimed: Boolean,
    /** The movement's own answer to *Done* by name, or null to follow the app setting (ROADMAP N235). */
    val progressionMode: String?,
    /** When this exercise was marked done, or null (ROADMAP N7). */
    val finishedAt: Long?,
    /** How well the target muscle was worked, 1–10, or null (ROADMAP N8). */
    val muscleFeel: Int?,
    val supersetGroup: Int?,
)

/**
 * The projected row as the domain reads it, with the joints the query cannot join (ROADMAP N63).
 *
 * [joints] is a parameter rather than a field of the projection for the same reason N62's
 * sore-muscle list is: the rows live in their own table, so the reader that has them in hand passes
 * them in. It is deliberately not defaulted — a caller that forgot them would drop the rating.
 */
internal fun SessionExerciseDetail.toDomain(
    joints: List<SessionExerciseJointEntity>,
): SessionExercise = SessionExercise(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    position = position,
    exerciseName = exerciseName,
    primaryMuscle = primaryMuscle,
    equipment = equipment,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    weightUnit = WeightUnit.fromName(weightUnit),
    stepGrams = stepGrams,
    isBodyweight = isBodyweight,
    isTimed = isTimed,
    progressionMode = ProgressionMode.fromName(progressionMode),
    finishedAt = finishedAt?.let(Instant::ofEpochMilli),
    muscleFeel = muscleFeel,
    joints = joints.map { it.toDomain() },
    supersetGroup = supersetGroup,
)

/** One joint row as the rest of the app reads it (ROADMAP N63). */
internal fun SessionExerciseJointEntity.toDomain(): JointPain =
    JointPain(joint = joint, side = side, score = score)
