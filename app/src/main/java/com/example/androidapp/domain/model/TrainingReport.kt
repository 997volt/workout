package com.example.androidapp.domain.model

import com.example.androidapp.domain.WeightUnit
import java.time.Instant
import java.time.LocalDate

/**
 * The last training block, as the one CSV a lifter sends their coach (ROADMAP N331).
 *
 * **Not the backup.** The backup is the whole device — every measurement, every soft-deleted row,
 * every setting — and a coach cannot read it. This is the block's training and body only, with the
 * plan beside what was performed, so "what did they do and what was prescribed" is one file.
 *
 * The report carries the rows; [com.example.androidapp.data.transfer.TrainingReportCsv] writes them.
 * Keeping the content here, as data, is what lets the selection and the file be tested without a
 * device and without a share sheet.
 */
data class TrainingReport(
    val blockName: String,
    val blockStart: LocalDate,
    val blockEnd: LocalDate,
    val blockComplete: Boolean,
    val exportedAt: Instant,
    /** The unit the measurements are stated in; each set carries its exercise's own unit. */
    val unit: WeightUnit,
    val days: List<TrainingDay> = emptyList(),
    val sessions: List<TrainingSession> = emptyList(),
    val measurements: List<TrainingMeasurement> = emptyList(),
)

/**
 * One day of the block's plan.
 *
 * A sequence block has an order, not a calendar, so a day has no date; [sessionId] is the session that
 * stood for it, or null when it was not trained. A weekly program's dates are not part of this row;
 * the report's session rows carry the dates.
 */
data class TrainingDay(
    val name: String,
    val position: Int,
    val sessionId: String? = null,
)

/** One finished workout or activity in the block, with everything it recorded. */
data class TrainingSession(
    val id: String,
    val workout: String,
    /** The block day this session stood for, or null for an unplanned workout. */
    val dayName: String? = null,
    val startedAt: Instant,
    /** The zone the session was performed in, or null when it predates that column (N25). */
    val zoneOffsetMinutes: Int? = null,
    val finishedAt: Instant,
    val comment: String? = null,
    val readinessNote: String? = null,
    val soreMuscles: List<SoreMuscle> = emptyList(),
    val activity: Activity? = null,
    val activityDurationSeconds: Int? = null,
    val activityDistanceMeters: Long? = null,
    val exercises: List<TrainingExercise> = emptyList(),
)

/** One exercise in a session, its performed and prescribed sets, and its joint ratings. */
data class TrainingExercise(
    val name: String,
    val sets: List<TrainingSet> = emptyList(),
    val joints: List<JointPain> = emptyList(),
)

/**
 * One set, performed, prescribed, or both.
 *
 * A plan row and its performance share a row when the plan's set index has a performed set; a plan row
 * with no performance is [performed] false and has no actual values; a performed set the plan did not
 * name has no planned values. A **failed attempt** is a performed set with [role] `FAILED_ATTEMPT` and
 * no completed rep — it is shown, but it is not volume.
 */
data class TrainingSet(
    val setIndex: Int,
    val performed: Boolean,
    /** The performed role, or the prescribed one when [performed] is false. */
    val role: SetType,
    val unit: WeightUnit,
    val reps: Int = 0,
    val durationSeconds: Int? = null,
    val weightGrams: Long = 0L,
    val assistanceGrams: Long = 0L,
    val bodyweightGrams: Long? = null,
    val rpeHalves: Int? = null,
    val comment: String? = null,
    val plannedRole: SetType? = null,
    val plannedReps: Int? = null,
    val plannedWeightGrams: Long? = null,
    val plannedRpeHalves: Int? = null,
)

/** One body measurement in the block: a daily entry, or one tape day. */
data class TrainingMeasurement(
    val measuredAt: Instant,
    val kind: MeasurementKind,
    val weightGrams: Long? = null,
    val bodyFatTenths: Int? = null,
    val muscleTenths: Int? = null,
    val tape: Map<TapeSite, Long> = emptyMap(),
    val soreMuscles: List<SoreMuscle> = emptyList(),
)
