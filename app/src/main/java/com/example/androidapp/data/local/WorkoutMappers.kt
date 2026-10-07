package com.example.androidapp.data.local

import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.WorkoutSession
import java.time.Instant

/**
 * Entity to domain for sessions.
 *
 * The epoch-millis `Long` in storage becomes a `java.time.Instant` in the
 * domain — the conversion lives here so no ViewModel has to think about it.
 *
 * [soreMuscles] is a parameter rather than a field of the session row (ROADMAP N62): the rows live
 * in their own table, so the reader that has them in hand passes them in. It is deliberately not
 * defaulted — a caller that forgot them would drop a lifter's soreness in silence, which is the
 * trap this codebase's backup notes keep naming.
 */
internal fun WorkoutSessionEntity.toDomain(soreMuscles: List<SessionSoreMuscleEntity>): WorkoutSession =
    WorkoutSession(
        id = id,
        startedAt = Instant.ofEpochMilli(startedAt),
        finishedAt = finishedAt?.let(Instant::ofEpochMilli),
        restEndsAt = restEndsAt?.let(Instant::ofEpochMilli),
        readinessNote = readinessNote,
        soreMuscles = soreMuscles.map { it.toDomain() },
        zoneOffsetMinutes = zoneOffsetMinutes,
        notes = notes,
        // Provenance, so a past workout can say which plan it was (N58, N84). The name is not copied with
        // it: the reader follows the id and reads the template's live name.
        templateId = templateId,
    )

/** One sore-muscle row as the rest of the app reads it (ROADMAP N62). */
internal fun SessionSoreMuscleEntity.toDomain(): SoreMuscle = SoreMuscle(muscle = muscle, score = score)

internal fun SetEntryEntity.toDomain(): SetEntry = SetEntry(
    id = id,
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    reps = reps,
    weightGrams = weightGrams,
    assistanceGrams = assistanceGrams,
    setType = setType,
    rpeHalves = rpeHalves,
    note = note,
    completedAt = completedAt?.let(Instant::ofEpochMilli),
)
