package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.androidapp.domain.model.MuscleGroup

/**
 * One muscle a session's readiness note reported sore, and its score (ROADMAP N62).
 *
 * Rows keyed to the session rather than a serialized column, so one muscle's score can be read on
 * its own later — the shape the per-exercise ratings already use (N8). [muscle] is stored by name
 * through [Converters], so reordering the enum cannot reinterpret a row on disk.
 *
 * [position] is the order the muscles were picked in, stored rather than inferred from insertion
 * order, so a list the lifter arranged never depends on row ids sorting usefully.
 */
@Entity(
    tableName = "session_sore_muscles",
    indices = [Index(value = ["sessionId"])],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionSoreMuscleEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    /** Stored by name, never ordinal (ROADMAP N62). */
    val muscle: MuscleGroup,
    /** 1–10, on `TenPointScale` (ROADMAP N62). */
    val score: Int,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
