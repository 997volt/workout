package com.example.androidapp.data.local

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.RecordEvent
import java.time.Instant

/**
 * One row of the records timeline (ROADMAP N290).
 *
 * A projection, not an entity: which sets are records is decided by SQL — the rule can see the rows before
 * it — and this only carries the four things the list prints.
 */
data class RecordEventRow(
    val exerciseName: String,
    val performedAt: Long,
    /** The zone the session was performed in, or null when it predates the column (ROADMAP N25). */
    val zoneOffsetMinutes: Int?,
    val reps: Int,
    val weightGrams: Long,
    val durationSeconds: Int?,
    /** The exercise's own unit, by name, or null to follow the app setting (ROADMAP N64). */
    val weightUnit: String?,
)

/**
 * The row as the timeline shows it, or null for a shape the record rule cannot produce.
 *
 * A timed set is a [RecordEvent.Hold] even when it also carries a load — a carry holds both and the record
 * is its time (N220, N233). Everything else is the weighted record the SQL already judged; a row with no
 * reps or no weight would be one the write boundary refuses, so it draws nothing rather than claiming a
 * record the app never announced.
 */
internal fun RecordEventRow.toDomain(): RecordEvent? {
    val at = Instant.ofEpochMilli(performedAt)
    return if (durationSeconds != null && durationSeconds > 0) {
        RecordEvent.Hold(
            exerciseName = exerciseName,
            performedAt = at,
            zoneOffsetMinutes = zoneOffsetMinutes,
            seconds = durationSeconds,
        )
    } else if (reps > 0 && weightGrams > 0L) {
        RecordEvent.Load(
            exerciseName = exerciseName,
            performedAt = at,
            zoneOffsetMinutes = zoneOffsetMinutes,
            weightUnit = WeightUnit.fromName(weightUnit),
            weightGrams = weightGrams,
            reps = reps,
        )
    } else {
        null
    }
}
