package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.domain.repository.BackupProblem

/**
 * The goal rows a backup may not carry into the store (B484).
 *
 * Extracted from [BackupValidator] because the kind/value and period/half rules are a small subject of their
 * own and the enclosing object had reached this project's function ceiling. The same baseline applies: a
 * valid app export is valid, and every check here stands in for a write boundary the import bypasses.
 */
internal object GoalBackupValidator {

    fun validate(file: BackupFile, out: MutableList<BackupProblem>) {
        validatePeriods(file.goalPeriods, file.exportedAt, out)
        validateHalves(file, out)
        // The older halves are compatibility, not a second store: when both are present the later half must
        // describe the period that is open, because the import takes the periods (B484).
        if (file.goalPeriods.isNotEmpty()) validateHalvesAgreeWithPeriods(file, out)
    }

    private fun validatePeriods(rows: List<GoalPeriodDto>, exportedAt: Long, out: MutableList<BackupProblem>) {
        rows.forEachIndexed { index, row ->
            val where = "Goal ${index + 1}"
            row.id.required(out, where)
            row.metricId.required(out, where)
            if (row.startedAt != null && row.startedAt > exportedAt) {
                out += BackupProblem(where, "A goal cannot start after the backup was taken.")
            }
            val known = row.kind == null || WeightGoalKind.entries.any { it.name == row.kind }
            if (!known) {
                out += BackupProblem(where, "The goal kind is not one this app knows.")
            }
            row.value?.let {
                if (!it.isFinite() || it < 0.0) {
                    out += BackupProblem(where, "A goal must be a finite, non-negative number.")
                }
            }
            validateWeightGoalValue(row.metricId, row.kind, row.value, where, out)
            if (known) validateTerms(row.kind, row.value, where, out)
        }
        rows.groupBy { it.metricId }.forEach { (metric, metricRows) ->
            if (metricRows.count { it.startedAt == null } > 1) {
                out += BackupProblem("Goal for $metric", "A goal can have only one undated period.")
            }
        }
    }

    private fun validateHalves(file: BackupFile, out: MutableList<BackupProblem>) {
        file.goals.forEach { (metric, value) ->
            validateWeightGoalValue(metric, file.goalKinds[metric], value, "Goal for $metric", out)
        }
        file.goalKinds.forEach { (metric, kind) ->
            val where = "Goal for $metric"
            if (WeightGoalKind.entries.none { it.name == kind }) {
                out += BackupProblem(where, "The goal kind is not one this app knows.")
            } else {
                validateTerms(kind, file.goals[metric], where, out)
            }
        }
    }

    /** A kind and a value are one statement: a target needs a weight, a direction cannot have one. */
    private fun validateTerms(kind: String?, value: Double?, where: String, out: MutableList<BackupProblem>) {
        when {
            isValueKind(kind) && value == null ->
                out += BackupProblem(where, "A target or maintenance goal needs a weight.")

            isDirectionKind(kind) && value != null ->
                out += BackupProblem(where, "A direction goal cannot state a weight.")
        }
    }

    private fun validateHalvesAgreeWithPeriods(file: BackupFile, out: MutableList<BackupProblem>) {
        val open = openPeriods(file.goalPeriods)
        (file.goals.keys + file.goalKinds.keys).forEach { metric ->
            val where = "Goal for $metric"
            val period = open[metric]
            if (period == null) {
                out += BackupProblem(where, "The goal has no period in this file.")
                return@forEach
            }
            val value = file.goals[metric]
            if (value != null && value != period.value) {
                out += BackupProblem(where, "The goal value disagrees with its open period.")
            }
            val kind = file.goalKinds[metric]
            if (kind != null && WeightGoalKind.entries.none { it.name == kind }) return@forEach
            // A target with no kind is still a target: the half reads the same as the period under the old
            // app's "value and no kind is a target" rule. Every other missing kind is a different statement.
            val compatible = kind == period.kind ||
                (period.kind == WeightGoalKind.TARGET.name && kind == null)
            if (!compatible) out += BackupProblem(where, "The goal kind disagrees with its open period.")
        }
    }

    /**
     * The open period per metric, by the same order `MetricGoalDao.observeCurrent` uses: a dated row beats
     * the undated legacy one, then the latest start, then the latest creation.
     */
    private fun openPeriods(rows: List<GoalPeriodDto>): Map<String, GoalPeriodDto> =
        rows.groupBy { it.metricId }.mapValues { (_, metricRows) ->
            metricRows.sortedWith(
                compareBy<GoalPeriodDto> { it.startedAt == null }
                    .thenByDescending { it.startedAt ?: Long.MIN_VALUE }
                    .thenByDescending { it.createdAt },
            ).first()
        }

    /**
     * A weight is a body, not a number: a target or corridor of zero is not a goal (B481).
     *
     * The older `goals` half has no kind on a file written before kinds existed, so the metric id is the only
     * thing that can say whether its zero is a weight. A non-weight level target of zero is left alone; only
     * the weight kind and the weight metric are refused.
     */
    private fun validateWeightGoalValue(
        metricId: String,
        kind: String?,
        value: Double?,
        where: String,
        out: MutableList<BackupProblem>,
    ) {
        val valueKind = isValueKind(kind) || (kind == null && metricId == WEIGHT_METRIC)
        if (valueKind && value == 0.0) {
            out += BackupProblem(where, "A weight goal must be heavier than zero.")
        }
    }

    private fun isValueKind(kind: String?): Boolean =
        kind == WeightGoalKind.TARGET.name || kind == WeightGoalKind.MAINTAIN.name

    private fun isDirectionKind(kind: String?): Boolean =
        kind == WeightGoalKind.LOSE.name || kind == WeightGoalKind.GAIN.name

    private fun String.required(out: MutableList<BackupProblem>, where: String) {
        if (isBlank()) out += BackupProblem(where, "An id or name is empty.")
    }

    /** The weight metric's backup id; the UI's own constant is internal to its module. */
    private const val WEIGHT_METRIC = "BODY:WEIGHT"
}
