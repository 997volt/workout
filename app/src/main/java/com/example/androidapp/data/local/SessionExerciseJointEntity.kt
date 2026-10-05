package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.Side

/**
 * One joint a session exercise reported painful, its side, and its score (ROADMAP N63).
 *
 * Rows keyed to the session exercise rather than a serialized column, so one joint's score can be
 * read on its own later — the same shape N62's sore-muscle list uses, and the reason the joint-pain
 * trend can read the worst joint instead of an average of two sides. [joint] and [side] are stored
 * by name through [Converters], so reordering either enum cannot reinterpret a row on disk.
 *
 * [position] is the order the joints were picked in, stored rather than inferred from insertion
 * order, so a list the lifter arranged never depends on row ids sorting usefully.
 */
@Entity(
    tableName = "session_exercise_joints",
    indices = [Index(value = ["sessionExerciseId"])],
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionExerciseJointEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    /** Stored by name, never ordinal (ROADMAP N63). */
    val joint: Joint,
    /** Stored by name, never ordinal (ROADMAP N63). */
    val side: Side,
    /** 1–10, on `TenPointScale` (ROADMAP N63). */
    val score: Int,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
