package com.example.androidapp.data.transfer

import com.example.androidapp.data.MAX_SET_SECONDS
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.MAX_BODY_PERCENT_TENTHS
import com.example.androidapp.domain.model.MAX_REPS
import com.example.androidapp.domain.model.MAX_TAPE_MILLIMETRES
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.TenPointScale
import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.domain.model.storedZoneOrNull
import com.example.androidapp.domain.repository.BackupProblem
import java.time.LocalDate

/**
 * What a backup file may and may not contain before any of it is merged (B342).
 *
 * The import writes decoded DTOs straight to entities, bypassing every write boundary: a repository
 * refuses a negative load or a hold for no time, but an import never asks it. The program-document path
 * already validates a file before writing; this is the same idea for the backup, covering the values
 * the app can actually store and the conversions that would otherwise throw (an out-of-range zone
 * offset or an epoch day outside `LocalDate`).
 *
 * **Not checked here:** whether a row's id already exists here, and whether a reference resolves on this
 * device. Those are facts about the receiving store, not about the file, and the merge's own ordering
 * answers them. What is checked is everything a hand-edited or foreign file can put in a row.
 */
internal object BackupValidator {

    fun problems(file: BackupFile): List<BackupProblem> = buildList {
        validateExercises(file.exercises, this)
        validateSessions(file.sessions, this)
        validateSessionExercises(file.sessionExercises, this)
        validateSets(file.sets, this)
        validateRatings(file, this)
        validateMeasurements(file, this)
        validateGoals(file, this)
        validateTemplates(file, this)
        validateTemplateExercises(file.templateExercises, this)
        validateTemplateSets(file.templateSets, this)
        validatePrograms(file, this)
    }

    private fun validateExercises(rows: List<ExerciseDto>, out: MutableList<BackupProblem>) {
        rows.forEachIndexed { index, row ->
            val where = "Movement ${index + 1}"
            row.id.required(out, where)
            row.name.required(out, where)
            row.restSeconds?.let {
                if (it !in 0..RestTimer.MAX_PRESCRIBED_SECONDS) {
                    out += BackupProblem(where, "A rest must be between 0 and an hour.")
                }
            }
            row.weightUnit?.let {
                if (WeightUnit.fromName(it) == null) {
                    out += BackupProblem(where, "The weight unit is not one this app knows.")
                }
            }
            row.stepGrams?.let {
                if (it !in 1..Weight.MAX_GRAMS) {
                    out += BackupProblem(where, "A weight step must be positive and within the app's maximum.")
                }
            }
        }
    }

    private fun validateSessions(rows: List<SessionDto>, out: MutableList<BackupProblem>) {
        rows.forEachIndexed { index, row ->
            val where = "Workout ${index + 1}"
            row.id.required(out, where)
            row.finishedAt?.let {
                if (it < row.startedAt) {
                    out += BackupProblem(where, "A workout cannot finish before it starts.")
                }
            }
            row.zoneOffsetMinutes?.let {
                if (storedZoneOrNull(it) == null) {
                    out += BackupProblem(where, "The stored time-zone offset is outside ±18 hours.")
                }
            }
            row.activity?.let {
                if (Activity.named(it) == null) {
                    out += BackupProblem(where, "The activity is not one this app knows.")
                }
            }
        }
    }

    private fun validateSessionExercises(rows: List<SessionExerciseDto>, out: MutableList<BackupProblem>) {
        rows.forEachIndexed { index, row ->
            val where = "Exercise in a workout ${index + 1}"
            row.id.required(out, where)
            row.sessionId.required(out, where)
            row.exerciseId.required(out, where)
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
            row.restSeconds?.let {
                if (it !in 0..RestTimer.MAX_PRESCRIBED_SECONDS) {
                    out += BackupProblem(where, "A rest must be between 0 and an hour.")
                }
            }
            if (!TenPointScale.isValid(row.muscleFeel)) {
                out += BackupProblem(where, "Muscle feel must be between 1 and 10.")
            }
            row.supersetGroup?.let {
                if (it < 1) out += BackupProblem(where, "A superset group must be positive.")
            }
        }
    }

    private fun validateSets(rows: List<SetDto>, out: MutableList<BackupProblem>) {
        rows.forEachIndexed { index, row ->
            val where = "Set ${index + 1}"
            row.id.required(out, where)
            row.sessionExerciseId.required(out, where)
            if (row.setIndex < 0) out += BackupProblem(where, "A set index cannot be negative.")
            row.weightGrams.load(out, where)
            row.assistanceGrams.load(out, where)
            row.bodyweightGrams?.let {
                if (it !in 1..Weight.MAX_GRAMS) {
                    out += BackupProblem(where, "A bodyweight must be positive and within the app's maximum.")
                }
            }
            row.rpeHalves?.let {
                if (!Rpe.isValid(it)) out += BackupProblem(where, "RPE must be between 1 and 10 in half steps.")
            }
            if (row.durationSeconds == null) {
                if (row.reps !in 1..MAX_REPS) {
                    out += BackupProblem(where, "A counted set needs between 1 and $MAX_REPS reps.")
                }
            } else {
                if (row.reps != 0) {
                    out += BackupProblem(where, "A held set records seconds instead of reps.")
                }
                if (row.durationSeconds !in 1..MAX_SET_SECONDS) {
                    out += BackupProblem(where, "A hold must be between 1 second and a day.")
                }
            }
        }
    }

    private fun validateRatings(file: BackupFile, out: MutableList<BackupProblem>) {
        file.sessionSoreMuscles.forEachIndexed { index, row ->
            val where = "Soreness ${index + 1}"
            row.id.required(out, where)
            row.sessionId.required(out, where)
            if (!TenPointScale.isValid(row.score)) {
                out += BackupProblem(where, "A soreness score must be between 1 and 10.")
            }
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
        }
        file.sessionExerciseJoints.forEachIndexed { index, row ->
            val where = "Joint pain ${index + 1}"
            row.id.required(out, where)
            row.sessionExerciseId.required(out, where)
            if (!TenPointScale.isValid(row.score)) {
                out += BackupProblem(where, "A joint score must be between 1 and 10.")
            }
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
        }
        file.measurementSoreMuscles.forEachIndexed { index, row ->
            val where = "Measurement soreness ${index + 1}"
            row.id.required(out, where)
            row.measurementId.required(out, where)
            if (!TenPointScale.isValid(row.score)) {
                out += BackupProblem(where, "A soreness score must be between 1 and 10.")
            }
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
        }
    }

    private fun validateMeasurements(file: BackupFile, out: MutableList<BackupProblem>) {
        file.measurements.forEachIndexed { index, row ->
            val where = "Measurement ${index + 1}"
            row.id.required(out, where)
            row.weightGrams?.let {
                if (it !in 1..Weight.MAX_GRAMS) {
                    out += BackupProblem(where, "A body weight must be positive and within the app's maximum.")
                }
            }
            row.bodyFatTenths?.let {
                if (it !in 0..MAX_BODY_PERCENT_TENTHS) {
                    out += BackupProblem(where, "Body fat must be between 0% and 100%.")
                }
            }
            row.muscleTenths?.let {
                if (it !in 0..MAX_BODY_PERCENT_TENTHS) {
                    out += BackupProblem(where, "Muscle must be between 0% and 100%.")
                }
            }
            listOf(
                row.neckMm,
                row.chestMm,
                row.waistMm,
                row.hipsMm,
                row.upperArmMm,
                row.thighMm,
                row.calfMm,
            ).forEach { millimetres ->
                millimetres?.let {
                    if (it !in 0..MAX_TAPE_MILLIMETRES) {
                        out += BackupProblem(where, "A tape measurement is outside the app's range.")
                    }
                }
            }
        }
    }

    private fun validateGoals(file: BackupFile, out: MutableList<BackupProblem>) {
        file.goalPeriods.forEachIndexed { index, row ->
            val where = "Goal ${index + 1}"
            row.id.required(out, where)
            row.metricId.required(out, where)
            row.kind?.let {
                if (WeightGoalKind.entries.none { kind -> kind.name == it }) {
                    out += BackupProblem(where, "The goal kind is not one this app knows.")
                }
            }
            row.value?.let {
                if (!it.isFinite() || it < 0.0) {
                    out += BackupProblem(where, "A goal must be a finite, non-negative number.")
                }
            }
        }
        file.goalKinds.forEach { (metric, kind) ->
            if (WeightGoalKind.entries.none { it.name == kind }) {
                out += BackupProblem("Goal for $metric", "The goal kind is not one this app knows.")
            }
        }
    }

    private fun validateTemplates(file: BackupFile, out: MutableList<BackupProblem>) {
        file.templates.forEachIndexed { index, row ->
            val where = "Workout template ${index + 1}"
            row.id.required(out, where)
            row.name.required(out, where)
        }
    }

    private fun validateTemplateExercises(rows: List<TemplateExerciseDto>, out: MutableList<BackupProblem>) {
        rows.forEachIndexed { index, row ->
            val where = "Planned exercise ${index + 1}"
            row.id.required(out, where)
            row.templateId.required(out, where)
            row.exerciseId.required(out, where)
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
            row.restSeconds?.let {
                if (it !in 0..RestTimer.MAX_PRESCRIBED_SECONDS) {
                    out += BackupProblem(where, "A rest must be between 0 and an hour.")
                }
            }
            row.targetRpeHalves?.let {
                if (!Rpe.isValid(it)) out += BackupProblem(where, "RPE must be between 1 and 10 in half steps.")
            }
            row.supersetGroup?.let {
                if (it < 1) out += BackupProblem(where, "A superset group must be positive.")
            }
        }
    }

    private fun validateTemplateSets(rows: List<TemplateSetDto>, out: MutableList<BackupProblem>) {
        rows.forEachIndexed { index, row ->
            val where = "Planned set ${index + 1}"
            row.id.required(out, where)
            row.templateExerciseId.required(out, where)
            if (row.setIndex < 0) out += BackupProblem(where, "A set index cannot be negative.")
            row.targetWeightGrams?.load(out, where)
            row.targetAssistanceGrams?.load(out, where)
            row.dropValueGrams?.load(out, where)
            validatePlannedReps(row, where, out)
            row.targetDurationSeconds?.let {
                if (it !in 1..MAX_SET_SECONDS) {
                    out += BackupProblem(where, "A hold must be between 1 second and a day.")
                }
                if (row.targetRepsMin != null || row.targetRepsMax != null || row.targetRepsCurrent != null) {
                    out += BackupProblem(where, "A planned set cannot state reps and a hold together.")
                }
            }
        }
    }

    private fun validatePlannedReps(row: TemplateSetDto, where: String, out: MutableList<BackupProblem>) {
        listOfNotNull(row.targetRepsMin, row.targetRepsMax, row.targetRepsCurrent).forEach {
            if (it !in 1..MAX_REPS) out += BackupProblem(where, "Planned reps must be between 1 and $MAX_REPS.")
        }
        val min = row.targetRepsMin
        val max = row.targetRepsMax
        if (min != null && max != null && min > max) {
            out += BackupProblem(where, "A rep range cannot end below its start.")
        }
        row.targetRepsCurrent?.let { current ->
            if (min != null && current < min) out += BackupProblem(where, "The current rep is below its range.")
            if (max != null && current > max) out += BackupProblem(where, "The current rep is above its range.")
        }
    }

    private fun validatePrograms(file: BackupFile, out: MutableList<BackupProblem>) {
        file.programs.forEachIndexed { index, row ->
            val where = "Program ${index + 1}"
            row.id.required(out, where)
            row.name.required(out, where)
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
            row.startDate?.let {
                if (!validEpochDay(it)) out += BackupProblem(where, "The start date is outside the calendar.")
            }
        }
        file.programSlots.forEachIndexed { index, row ->
            val where = "Program day ${index + 1}"
            row.id.required(out, where)
            row.programId.required(out, where)
            row.templateId.required(out, where)
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
        }
        file.programSkips.forEachIndexed { index, row ->
            validateWeek(row.weekStart, "Program skip ${index + 1}", out)
        }
        file.programDeloads.forEachIndexed { index, row ->
            validateWeek(row.weekStart, "Deload week ${index + 1}", out)
        }
        file.programDeloadDays.forEachIndexed { index, row ->
            val where = "Deload day ${index + 1}"
            row.id.required(out, where)
            row.deloadId.required(out, where)
            row.templateId.required(out, where)
            if (row.position < 0) out += BackupProblem(where, "A position cannot be negative.")
        }
        file.programSubstitutions.forEachIndexed { index, row ->
            validateWeek(row.weekStart, "Substitution ${index + 1}", out)
            row.slotId.required(out, "Substitution ${index + 1}")
            row.templateId.required(out, "Substitution ${index + 1}")
        }
    }

    private fun validateWeek(weekStart: Long, where: String, out: MutableList<BackupProblem>) {
        if (!validEpochDay(weekStart)) {
            out += BackupProblem(where, "The week is outside the calendar.")
        }
    }

    private fun validEpochDay(value: Long): Boolean =
        runCatching { LocalDate.ofEpochDay(value) }.isSuccess

    private fun String.required(out: MutableList<BackupProblem>, where: String) {
        if (isBlank()) out += BackupProblem(where, "An id or name is empty.")
    }

    private fun Long.load(out: MutableList<BackupProblem>, where: String) {
        if (this < 0L || this > Weight.MAX_GRAMS) {
            out += BackupProblem(where, "A load must be between 0 and the app's maximum.")
        }
    }
}
