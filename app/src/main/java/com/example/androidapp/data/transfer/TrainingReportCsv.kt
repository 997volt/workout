package com.example.androidapp.data.transfer

import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TrainingDay
import com.example.androidapp.domain.model.TrainingMeasurement
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.domain.model.TrainingSession
import com.example.androidapp.domain.model.TrainingSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Writes the block report as one CSV (ROADMAP N331).
 *
 * One file, with a `record_type` column: a strict one-row-per-set table cannot also carry the block's
 * schedule, an activity, a body measurement or a recovery rating. The coach filters `record_type = set`
 * for the training table and joins the rest by `S1`, `S2`… — stable keys the writer mints, never the
 * app's internal UUIDs.
 *
 * The format is deliberately spreadsheet-shaped: UTF-8, RFC 4180 quoting, ISO dates and dot decimals,
 * one unit column beside each number. It is not a backup: the backup carries the whole device; this
 * carries the block's training and body, with the plan beside what was performed.
 */
object TrainingReportCsv {

    /** A set's display number is the plan's zero-based index plus one. */
    private const val SET_NUMBER_OFFSET = 1

    private const val SECONDS_PER_MINUTE = 60

    private const val TENTHS_PER_WHOLE = 10.0

    private val QUOTED_CHARS = setOf(',', '"', '\n', '\r')

    private val HEADER = listOf(
        "record_type",
        "block",
        "block_start",
        "block_end",
        "block_complete",
        "exported_at",
        "sessions",
        "sets",
        "planned_not_done",
        "measurements_count",
        "day",
        "day_position",
        "day_status",
        "session_key",
        "session_date",
        "session_started",
        "session_finished",
        "session_minutes",
        "workout",
        "block_day",
        "session_comment",
        "readiness_note",
        "exercise",
        "set_number",
        "role",
        "planned_role",
        "planned_reps",
        "planned_weight",
        "planned_unit",
        "reps",
        "hold_seconds",
        "weight",
        "unit",
        "assistance",
        "bodyweight",
        "rpe",
        "set_comment",
        "performed",
        "activity",
        "activity_minutes",
        "activity_distance_m",
        "metric",
        "value",
        "measurement_unit",
        "date",
        "sore_muscle",
        "sore_score",
        "joint",
        "side",
        "joint_score",
    )

    /** The file's text, ready to write as UTF-8. */
    fun write(report: TrainingReport): String {
        val sessionKeys = report.sessions.mapIndexed { index, session ->
            session.id to "S${index + 1}"
        }.toMap()
        val rows = buildList {
            add(blockRow(report))
            report.days.forEach { day -> add(dayRow(day, sessionKeys)) }
            report.sessions.forEach { session ->
                val key = sessionKeys.getValue(session.id)
                add(sessionRow(session, key))
                session.exercises.forEach { exercise ->
                    exercise.sets.forEach { set -> add(setRow(session, key, exercise.name, set)) }
                    exercise.joints.forEach { joint -> add(jointRow(session, key, exercise.name, joint)) }
                }
                session.soreMuscles.forEach { sore -> add(soreRow(session, key, sore)) }
                session.activity?.let { activity -> add(activityRow(session, key, activity)) }
            }
            report.measurements.forEach { measurement -> addAll(addMeasurementRows(measurement, report.unit)) }
        }

        return buildString {
            append(HEADER.joinToString(","))
            append("\r\n")
            rows.forEach { row ->
                append(HEADER.joinToString(",") { column -> escape(row[column]) })
                append("\r\n")
            }
        }
    }

    private fun blockRow(report: TrainingReport): Map<String, String?> {
        val performedSets = report.sessions.sumOf { session ->
            session.exercises.sumOf {
                it.sets.count { set -> set.performed }
            }
        }
        val plannedNotDone = report.sessions.sumOf { session ->
            session.exercises.sumOf { exercise -> exercise.sets.count { set -> !set.performed } }
        }
        return mapOf(
            "record_type" to "block",
            "block" to report.blockName,
            "block_start" to report.blockStart.toString(),
            "block_end" to report.blockEnd.toString(),
            "block_complete" to report.blockComplete.toString(),
            "exported_at" to report.exportedAt.toString(),
            "sessions" to report.sessions.size.toString(),
            "sets" to performedSets.toString(),
            "planned_not_done" to plannedNotDone.toString(),
            "measurements_count" to report.measurements.size.toString(),
        )
    }

    private fun dayRow(day: TrainingDay, sessionKeys: Map<String, String>): Map<String, String?> = mapOf(
        "record_type" to "day",
        "day" to day.name,
        "day_position" to day.position.toString(),
        "day_status" to if (day.sessionId == null) "not_done" else "done",
        "session_key" to day.sessionId?.let(sessionKeys::get),
    )

    private fun sessionRow(session: TrainingSession, key: String): Map<String, String?> {
        val offset = offsetOf(session)
        return mapOf(
            "record_type" to "session",
            "session_key" to key,
            "session_date" to localDate(session, offset).toString(),
            "session_started" to offsetDateTime(session.startedAt, offset),
            "session_finished" to offsetDateTime(session.finishedAt, offset),
            "session_minutes" to minutes(session.startedAt, session.finishedAt),
            "workout" to session.workout,
            "block_day" to session.dayName,
            "session_comment" to session.comment,
            "readiness_note" to session.readinessNote,
        )
    }

    private fun setRow(
        session: TrainingSession,
        key: String,
        exercise: String,
        set: TrainingSet,
    ): Map<String, String?> {
        val role = if (set.performed) set.role else (set.plannedRole ?: set.role)
        return mapOf(
            "record_type" to "set",
            "session_key" to key,
            "session_date" to localDate(session, offsetOf(session)).toString(),
            "workout" to session.workout,
            "block_day" to session.dayName,
            "exercise" to exercise,
            "set_number" to (set.setIndex + SET_NUMBER_OFFSET).toString(),
            "role" to role.label,
            "performed" to set.performed.toString(),
        ) + plannedFields(set) + actualFields(set)
    }

    private fun plannedFields(set: TrainingSet): Map<String, String?> = buildMap {
        set.plannedRole?.let { put("planned_role", it.label) }
        set.plannedReps?.let { put("planned_reps", it.toString()) }
        set.plannedWeightGrams?.let {
            put("planned_weight", Weight.format(it, set.unit))
            put("planned_unit", unitLabel(set.unit))
        }
    }

    private fun actualFields(set: TrainingSet): Map<String, String?> {
        if (!set.performed) return emptyMap()
        return buildMap {
            put("reps", set.reps.toString())
            set.durationSeconds?.let { put("hold_seconds", it.toString()) }
            put("weight", Weight.format(set.weightGrams, set.unit))
            put("unit", unitLabel(set.unit))
            if (set.assistanceGrams > 0L) put("assistance", Weight.format(set.assistanceGrams, set.unit))
            set.bodyweightGrams?.let { put("bodyweight", Weight.format(it, set.unit)) }
            set.rpeHalves?.let { put("rpe", Rpe.format(it)) }
            set.comment?.let { put("set_comment", it) }
        }
    }

    private fun activityRow(session: TrainingSession, key: String, activity: Activity): Map<String, String?> = mapOf(
        "record_type" to "activity",
        "session_key" to key,
        "session_date" to localDate(session, offsetOf(session)).toString(),
        "workout" to session.workout,
        "activity" to activity.name,
        "activity_minutes" to session.activityDurationSeconds?.let { (it / SECONDS_PER_MINUTE).toString() },
        "activity_distance_m" to session.activityDistanceMeters?.toString(),
        "session_comment" to session.comment,
    )

    private fun soreRow(session: TrainingSession, key: String, sore: SoreMuscle): Map<String, String?> = mapOf(
        "record_type" to "sore_muscle",
        "session_key" to key,
        "session_date" to localDate(session, offsetOf(session)).toString(),
        "date" to localDate(session, offsetOf(session)).toString(),
        "sore_muscle" to sore.muscle.label,
        "sore_score" to sore.score.toString(),
    )

    private fun jointRow(
        session: TrainingSession,
        key: String,
        exercise: String,
        joint: JointPain,
    ): Map<String, String?> = mapOf(
        "record_type" to "joint_pain",
        "session_key" to key,
        "session_date" to localDate(session, offsetOf(session)).toString(),
        "date" to localDate(session, offsetOf(session)).toString(),
        "exercise" to exercise,
        "joint" to joint.joint.label,
        "side" to joint.side.name,
        "joint_score" to joint.score.toString(),
    )

    private fun addMeasurementRows(measurement: TrainingMeasurement, unit: WeightUnit): List<Map<String, String?>> {
        val date = measurement.measuredAt.atZone(ZoneId.systemDefault()).toLocalDate()
        val rows = mutableListOf<Map<String, String?>>()
        measurement.weightGrams?.let {
            rows.add(measurementRow(date, "bodyweight", Weight.format(it, unit), unitLabel(unit)))
        }
        measurement.bodyFatTenths?.let { rows.add(measurementRow(date, "body_fat", tenths(it), "%")) }
        measurement.muscleTenths?.let { rows.add(measurementRow(date, "muscle", tenths(it), "%")) }
        measurement.tape.forEach { (site, millimetres) ->
            rows.add(measurementRow(date, "tape_${site.name.lowercase(Locale.ROOT)}", millimetres.toString(), "mm"))
        }
        measurement.soreMuscles.forEach { sore ->
            rows.add(
                mapOf(
                    "record_type" to "sore_muscle",
                    "date" to date.toString(),
                    "sore_muscle" to sore.muscle.label,
                    "sore_score" to sore.score.toString(),
                ),
            )
        }
        return rows
    }

    private fun measurementRow(
        date: LocalDate,
        metric: String,
        value: String,
        unit: String,
    ): Map<String, String?> = mapOf(
        "record_type" to "measurement",
        "date" to date.toString(),
        "metric" to metric,
        "value" to value,
        "measurement_unit" to unit,
    )

    private fun tenths(value: Int): String = String.format(Locale.ROOT, "%.1f", value / TENTHS_PER_WHOLE)

    private fun offsetOf(session: TrainingSession): ZoneOffset =
        session.zoneOffsetMinutes?.let { ZoneOffset.ofTotalSeconds(it * SECONDS_PER_MINUTE) } ?: ZoneOffset.UTC

    private fun localDate(session: TrainingSession, offset: ZoneOffset): LocalDate =
        session.startedAt.atOffset(offset).toLocalDate()

    private fun offsetDateTime(instant: Instant, offset: ZoneOffset): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atOffset(offset))

    private fun minutes(startedAt: Instant, finishedAt: Instant): String =
        java.time.Duration.between(startedAt, finishedAt).toMinutes().toString()

    private fun unitLabel(unit: WeightUnit): String = when (unit) {
        WeightUnit.KILOGRAMS -> "kg"
        WeightUnit.POUNDS -> "lb"
    }

    /** RFC 4180: quote a value that carries a comma, a quote or a newline; double its quotes. */
    private fun escape(value: String?): String {
        val text = value.orEmpty()
        return if (text.any { it in QUOTED_CHARS }) {
            '"' + text.replace("\"", "\"\"") + '"'
        } else {
            text
        }
    }
}
