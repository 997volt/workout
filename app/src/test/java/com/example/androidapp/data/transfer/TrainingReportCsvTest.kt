package com.example.androidapp.data.transfer

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.TrainingDay
import com.example.androidapp.domain.model.TrainingExercise
import com.example.androidapp.domain.model.TrainingMeasurement
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.domain.model.TrainingSession
import com.example.androidapp.domain.model.TrainingSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The one-file block export (ROADMAP N331).
 *
 * One CSV with record types, not a presentation report: the trainer filters `record_type = set` for the
 * training table and joins the plan, the schedule, the body and recovery rows by the `session-1` keys. These
 * assertions are the contract a coach's spreadsheet depends on.
 */
class TrainingReportCsvTest {

    private val systemDate = Instant.parse("2026-10-02T07:00:00Z")
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

    private val report = TrainingReport(
        blockName = "Upper/Lower",
        blockStart = LocalDate.of(2026, 9, 4),
        blockEnd = LocalDate.of(2026, 10, 16),
        blockComplete = true,
        exportedAt = Instant.parse("2026-10-17T10:00:00Z"),
        unit = WeightUnit.KILOGRAMS,
        days = listOf(
            TrainingDay(name = "Day 1 — Push", position = 1, sessionId = "s1"),
            TrainingDay(name = "Day 2 — Pull", position = 2),
        ),
        sessions = listOf(
            TrainingSession(
                id = "s1",
                workout = "Push",
                dayName = "Day 1 — Push",
                startedAt = Instant.parse("2026-10-01T08:00:00Z"),
                zoneOffsetMinutes = 120,
                finishedAt = Instant.parse("2026-10-01T09:00:00Z"),
                // A comma, a quote and a newline in one field: the escaping has to survive a coach's
                // spreadsheet, and this is the test that says so.
                comment = "felt, \"good\"\nnext",
                readinessNote = "slept 6h",
                soreMuscles = listOf(SoreMuscle(MuscleGroup.QUADS, 6)),
                activity = Activity.SWIMMING,
                activityDurationSeconds = 1_800,
                activityDistanceMeters = 1_000,
                exercises = listOf(
                    TrainingExercise(
                        name = "Bench Press",
                        sets = listOf(
                            TrainingSet(
                                setIndex = 0,
                                performed = true,
                                role = SetType.NORMAL,
                                unit = WeightUnit.KILOGRAMS,
                                reps = 5,
                                weightGrams = 100_000L,
                                rpeHalves = 16,
                                comment = "top, set",
                                plannedRole = SetType.NORMAL,
                                plannedReps = 5,
                                plannedWeightGrams = 100_000L,
                                plannedRpeHalves = 16,
                            ),
                            TrainingSet(
                                setIndex = 1,
                                performed = true,
                                role = SetType.FAILED_ATTEMPT,
                                unit = WeightUnit.KILOGRAMS,
                                reps = 0,
                                weightGrams = 110_000L,
                                comment = "failed 110",
                            ),
                            TrainingSet(
                                setIndex = 2,
                                performed = false,
                                role = SetType.NORMAL,
                                unit = WeightUnit.KILOGRAMS,
                                plannedRole = SetType.NORMAL,
                                plannedReps = 8,
                                plannedWeightGrams = 90_000L,
                                plannedRpeHalves = 17,
                            ),
                        ),
                        joints = listOf(JointPain(Joint.KNEE, Side.LEFT, 4)),
                    ),
                ),
            ),
        ),
        measurements = listOf(
            TrainingMeasurement(
                measuredAt = systemDate.let { LocalDate.parse(it) }
                    .atStartOfDay(ZoneId.systemDefault()).toInstant(),
                weightGrams = 80_000L,
                bodyFatTenths = 155,
                muscleTenths = 450,
                tape = mapOf(TapeSite.WAIST to 850L),
                soreMuscles = listOf(SoreMuscle(MuscleGroup.CHEST, 3)),
            ),
        ),
    )

    @Test
    fun everyKindOfRecord_landsInTheOneFile() {
        val records = parse(TrainingReportCsv.write(report))

        val block = records.single { it["record_type"] == "block" }
        assertThat(block["block"]).isEqualTo("Upper/Lower")
        assertThat(block["block_start"]).isEqualTo("2026-09-04")
        assertThat(block["block_end"]).isEqualTo("2026-10-16")
        assertThat(block["block_complete"]).isEqualTo("true")
        assertThat(block["sessions"]).isEqualTo("1")
        assertThat(block["sets"]).isEqualTo("2")
        assertThat(block["planned_not_done"]).isEqualTo("1")
        assertThat(block["measurements_count"]).isEqualTo("1")

        val days = records.filter { it["record_type"] == "day" }
        assertThat(days.map { it["day"] }).containsExactly("Day 1 — Push", "Day 2 — Pull").inOrder()
        assertThat(days[0]["day_status"]).isEqualTo("done")
        assertThat(days[0]["session_key"]).isEqualTo("session-1")
        assertThat(days[1]["day_status"]).isEqualTo("not_done")
        assertThat(days[1]["session_key"]).isEmpty()

        val session = records.single { it["record_type"] == "session" }
        assertThat(session["session_key"]).isEqualTo("session-1")
        assertThat(session["session_date"]).isEqualTo("2026-10-01")
        assertThat(session["session_started"]).isEqualTo("2026-10-01T10:00:00+02:00")
        assertThat(session["session_finished"]).isEqualTo("2026-10-01T11:00:00+02:00")
        assertThat(session["session_minutes"]).isEqualTo("60")
        assertThat(session["workout"]).isEqualTo("Push")
        assertThat(session["block_day"]).isEqualTo("Day 1 — Push")
        assertThat(session["session_comment"]).isEqualTo("felt, \"good\"\nnext")
        assertThat(session["readiness_note"]).isEqualTo("slept 6h")

        val failed = records.single { it["record_type"] == "set" && it["role"] == "Failed attempt" }
        assertThat(failed["session_key"]).isEqualTo("session-1")
        assertThat(failed["exercise"]).isEqualTo("Bench Press")
        assertThat(failed["set_number"]).isEqualTo("2")
        assertThat(failed["reps"]).isEqualTo("0")
        assertThat(failed["weight"]).isEqualTo("110")
        assertThat(failed["unit"]).isEqualTo("kg")
        assertThat(failed["performed"]).isEqualTo("true")
        assertThat(failed["set_comment"]).isEqualTo("failed 110")

        val plannedOnly = records.single { it["record_type"] == "set" && it["performed"] == "false" }
        assertThat(plannedOnly["role"]).isEqualTo("Working")
        assertThat(plannedOnly["planned_role"]).isEqualTo("Working")
        assertThat(plannedOnly["planned_reps"]).isEqualTo("8")
        assertThat(plannedOnly["planned_weight"]).isEqualTo("90")
        assertThat(plannedOnly["planned_unit"]).isEqualTo("kg")
        assertThat(plannedOnly["planned_rpe"]).isEqualTo("8.5")
        assertThat(plannedOnly["weight"]).isEmpty()

        val measurements = records.filter { it["record_type"] == "measurement" }
            .associateBy { it["metric"] to it["measurement_unit"] }
        assertThat(measurements["bodyweight" to "kg"]?.get("value")).isEqualTo("80")
        assertThat(measurements["body_fat" to "%"]?.get("value")).isEqualTo("15.5")
        assertThat(measurements["muscle" to "%"]?.get("value")).isEqualTo("45.0")
        assertThat(measurements["tape_waist" to "mm"]?.get("value")).isEqualTo("850")

        val activity = records.single { it["record_type"] == "activity" }
        assertThat(activity["activity"]).isEqualTo("SWIMMING")
        assertThat(activity["activity_minutes"]).isEqualTo("30")
        assertThat(activity["activity_distance_m"]).isEqualTo("1000")

        val sored = records.filter { it["record_type"] == "sore_muscle" }
        assertThat(sored.map { it["sore_muscle"] }).containsExactly("Quads", "Chest")
        assertThat(sored.map { it["sore_score"] }).containsExactly("6", "3")

        val joint = records.single { it["record_type"] == "joint_pain" }
        assertThat(joint["exercise"]).isEqualTo("Bench Press")
        assertThat(joint["joint"]).isEqualTo("Knee")
        assertThat(joint["side"]).isEqualTo("LEFT")
        assertThat(joint["joint_score"]).isEqualTo("4")
    }

    @Test
    fun aPlanThatPrescribesAssistanceAndAHold_writesBothBesideItsRpe() {
        // N337: the prescription is reps, load *and* effort/hold/help. Before this the report carried
        // the RPE and dropped it, and a plan that named only assistance or a hold lost its numbers.
        val plan = TrainingSet(
            setIndex = 0,
            performed = false,
            role = SetType.NORMAL,
            unit = WeightUnit.KILOGRAMS,
            plannedRole = SetType.NORMAL,
            plannedAssistanceGrams = 20_000L,
            plannedDurationSeconds = 45,
            plannedRpeHalves = 17,
        )
        val csv = report.copy(
            days = emptyList(),
            sessions = listOf(
                report.sessions.single().copy(
                    exercises = listOf(TrainingExercise(name = "Assisted Bench", sets = listOf(plan))),
                ),
            ),
            measurements = emptyList(),
        )

        val row = parse(TrainingReportCsv.write(csv)).single { it["record_type"] == "set" }

        assertThat(row["planned_assistance"]).isEqualTo("20")
        assertThat(row["planned_unit"]).isEqualTo("kg")
        assertThat(row["planned_hold_seconds"]).isEqualTo("45")
        assertThat(row["planned_rpe"]).isEqualTo("8.5")
        assertThat(row["planned_weight"]).isEmpty()
    }

    @Test
    fun aQuotedField_withACommaQuoteAndNewline_isOneValue() {
        // The parser used above is itself the assertion that RFC 4180 quoting holds; this one names the
        // three characters that break a naive `split(",")`.
        val session = parse(TrainingReportCsv.write(report)).single { it["record_type"] == "session" }

        assertThat(session["session_comment"]).isEqualTo("felt, \"good\"\nnext")
    }

    @Suppress("CyclomaticComplexMethod") // A deliberately small RFC 4180 reader; the writer is the subject.
    private fun parse(csv: String): List<Map<String, String>> {
        val records = mutableListOf<List<String>>()
        val record = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            when {
                quoted && c == '"' && i + 1 < csv.length && csv[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                quoted && c == '"' -> quoted = false
                quoted -> field.append(c)
                c == '"' -> quoted = true
                c == ',' -> {
                    record.add(field.toString())
                    field.clear()
                }
                c == '\r' -> Unit
                c == '\n' -> {
                    record.add(field.toString())
                    field.clear()
                    records.add(record.toList())
                    record.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || record.isNotEmpty()) {
            record.add(field.toString())
            records.add(record.toList())
        }
        val header = records.first()
        return records.drop(1)
            .filter { row -> row.any { it.isNotEmpty() } }
            .map { row -> header.indices.associate { header[it] to (row.getOrNull(it) ?: "") } }
    }

    @Test
    fun aPoundsBodyweightHold_carriesItsOwnUnitBodyweightAndSeconds() {
        // N351: the set columns the format advertises — the exercise's unit, the composed load's
        // bodyweight and a hold's seconds — in one row.
        val hold = TrainingSet(
            setIndex = 0,
            performed = true,
            role = SetType.NORMAL,
            unit = WeightUnit.POUNDS,
            reps = 0,
            durationSeconds = 90,
            weightGrams = 20_000L,
            bodyweightGrams = 75_000L,
        )
        val csv = report.copy(
            days = emptyList(),
            sessions = listOf(
                report.sessions.single().copy(
                    exercises = listOf(TrainingExercise(name = "Weighted Hold", sets = listOf(hold))),
                ),
            ),
            measurements = emptyList(),
        )

        val row = parse(TrainingReportCsv.write(csv)).single { it["record_type"] == "set" }

        assertThat(row["unit"]).isEqualTo("lb")
        assertThat(row["weight"]).isEqualTo("44.1")
        assertThat(row["bodyweight"]).isEqualTo("165.3")
        assertThat(row["hold_seconds"]).isEqualTo("90")
        assertThat(row["reps"]).isEqualTo("0")
    }

    @Test
    fun aCellThatLooksLikeAFormula_isNeutralized_butANegativeNumberIsNot() {
        // N345: a lifter's comment is data, not a formula in the coach's spreadsheet. A negative value
        // the format may grow later still has to survive as a number.
        val dangerous = report.copy(
            blockName = "-20",
            sessions = listOf(report.sessions.single().copy(comment = "=HYPERLINK(\"http://x\")")),
        )
        val records = parse(TrainingReportCsv.write(dangerous))

        assertThat(records.single { it["record_type"] == "session" }["session_comment"])
            .isEqualTo("'=HYPERLINK(\"http://x\")")
        assertThat(records.single { it["record_type"] == "block" }["block"]).isEqualTo("-20")
    }

    @Test
    fun anEmptyReport_isAHeaderAndABlockRow() {
        val csv = TrainingReportCsv.write(
            report.copy(days = emptyList(), sessions = emptyList(), measurements = emptyList()),
        )

        val lines = csv.lineSequence().filter { it.isNotEmpty() }.toList()
        assertThat(lines).hasSize(2)
        assertThat(lines[0]).startsWith("record_type,")
        assertThat(lines[1]).startsWith("block,Upper/Lower")
    }
}
