package com.example.androidapp.data.local

import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.WorkoutSummary
import java.time.Instant

/**
 * One row of the history list: a finished workout and its totals.
 *
 * A projection rather than an entity, because nothing here is a column — the
 * counts and the volume are computed by the query (see `WorkoutDao.observeHistory`).
 */
data class WorkoutSummaryRow(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeGrams: Long,
    /** The zone the session was performed in, or null when it predates the column (ROADMAP N25). */
    val zoneOffsetMinutes: Int?,
    /** Exercises still in the library, which is what a repeat would copy (ROADMAP B43's tail). */
    val repeatableExerciseCount: Int,
    /** The template this session was started from, read live, or null (ROADMAP N58). */
    val templateName: String?,
    /**
     * The activity this session was, stored by name, or null for a workout (ROADMAP N141).
     *
     * A `String` rather than the enum for the reason every stored name is one until it is read: a name this
     * build does not know reads as *not an activity* rather than throwing, and the list still draws.
     */
    val activity: String?,
)

internal fun WorkoutSummaryRow.toDomain(): WorkoutSummary = WorkoutSummary(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAt),
    finishedAt = finishedAt?.let(Instant::ofEpochMilli),
    exerciseCount = exerciseCount,
    setCount = setCount,
    volumeGrams = volumeGrams,
    zoneOffsetMinutes = zoneOffsetMinutes,
    repeatableExerciseCount = repeatableExerciseCount,
    templateName = templateName,
    activity = activity?.let { name -> Activity.entries.firstOrNull { it.name == name } },
)
