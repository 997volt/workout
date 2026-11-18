package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.androidapp.domain.model.SetType

/**
 * The plan one session exercise was prescribed from, as it stood when the workout started (ROADMAP B471).
 *
 * The stall rule and the volume rule compare a past session to *a plan*, and the living template is
 * no longer that plan once progression has moved it. The snapshot is written when the exercise is
 * seeded from a template, so every later judgement of that session reads the prescription it was
 * trained under, not today's.
 *
 * One row per planned set, including warm-ups and rungs: the rules filter by role themselves (N17,
 * N79), and a snapshot that dropped them would not be the plan. The row cascades with its session
 * exercise, because a removed exercise has no prescription to judge.
 */
@Entity(
    tableName = "session_plan_sets",
    primaryKeys = ["sessionExerciseId", "setIndex"],
    indices = [Index("sessionExerciseId")],
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionPlanSetEntity(
    val sessionExerciseId: String,
    /** The plan's own order; the rule pairs by class and order, so this is the only tie-breaker. */
    val setIndex: Int,
    val role: SetType,
    val targetWeightGrams: Long?,
    val targetAssistanceGrams: Long?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetRepsCurrent: Int?,
    val targetDurationSeconds: Int?,
    /** The exercise's one target effort, carried onto every set as the plan read it (N59). */
    val targetRpeHalves: Int?,
)
