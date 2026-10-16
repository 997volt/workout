package com.example.androidapp.data.local

import com.example.androidapp.domain.model.PlannedSetTarget

/**
 * One planned set's target as the query reads it (ROADMAP N39).
 *
 * A row type of this package rather than the domain model, the shape [TemplateSummaryRow] set: what a query
 * returns is a shape of the query, and the domain's own reading of it is [PlannedSetTarget].
 */
data class PlanTargetRow(
    val templateId: String,
    val templateName: String,
    val weightGrams: Long?,
    val assistanceGrams: Long?,
    val reps: Int?,
    val rpeHalves: Int?,
)

/** The domain's reading of the same row. */
internal fun PlanTargetRow.toPlannedSetTarget() = PlannedSetTarget(
    templateId = templateId,
    templateName = templateName,
    weightGrams = weightGrams,
    assistanceGrams = assistanceGrams,
    reps = reps,
    rpeHalves = rpeHalves,
)
