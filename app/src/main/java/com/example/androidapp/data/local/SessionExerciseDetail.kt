package com.example.androidapp.data.local

import com.example.androidapp.domain.model.JointPain
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
    val primaryMuscle: com.example.androidapp.domain.model.MuscleGroup,
    val equipment: com.example.androidapp.domain.model.Equipment,
    /** The library exercise's own rest, or null for the app default (ROADMAP N5). */
    val restSeconds: Int?,
    /** Shown under the exercise name while lifting (ROADMAP N5). */
    val techniqueNote: String?,
    /** When this exercise was marked done, or null (ROADMAP N7). */
    val finishedAt: Long?,
    /** How well the target muscle was worked, 1–10, or null (ROADMAP N8). */
    val muscleFeel: Int?,
    /** Legacy joint pain, 1–10, or null (ROADMAP N8) — read for a session rated before N63. */
    val jointPain: Int?,
    /** Legacy "which joints" free text, or null (ROADMAP N9) — read, never rewritten. */
    val jointPainNote: String?,
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
    finishedAt = finishedAt?.let(Instant::ofEpochMilli),
    muscleFeel = muscleFeel,
    jointPain = jointPain,
    jointPainNote = jointPainNote,
    joints = joints.map { it.toDomain() },
    supersetGroup = supersetGroup,
)

/** One joint row as the rest of the app reads it (ROADMAP N63). */
internal fun SessionExerciseJointEntity.toDomain(): JointPain =
    JointPain(joint = joint, side = side, score = score)
