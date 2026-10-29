package com.example.androidapp.data.local

import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet

/**
 * A template exercise joined with the library exercise it refers to.
 *
 * The same shape as [SessionExerciseDetail]: one query fills the template editor,
 * instead of a query plus an N+1 walk over the library.
 */
data class TemplateExerciseDetail(
    val id: String,
    val templateId: String,
    val exerciseId: String,
    val position: Int,
    val exerciseName: String,
    /**
     * The muscle this exercise trains, or null when nothing states one (ROADMAP B103, N103).
     *
     * Nullable because the column is, and the query resolves the parent — see [SessionExerciseDetail],
     * which carries the same projection for the same reason.
     */
    val primaryMuscle: MuscleGroup?,
    /** The equipment, or null when unstated (ROADMAP B103). Never inherited: a family spans equipment. */
    val equipment: Equipment?,
    val restSeconds: Int?,
    val techniqueNote: String?,
    /** The effort the plan builds to, in half-points, or null (N59, amended). */
    val targetRpeHalves: Int?,
    /** The library exercise's own display unit by name, or null to follow the app (N64). */
    val weightUnit: String?,
    /** The library exercise's own weight step, or null for the unit's (ROADMAP N77). */
    val stepGrams: Long?,
    /** The movement's own answer to *Done* by name, or null to follow the app (ROADMAP N235). */
    val progressionMode: String?,
    val supersetGroup: Int?,
)

internal fun TemplateExerciseDetail.toDomain(
    sets: List<TemplateSet> = emptyList(),
): TemplateExercise = TemplateExercise(
    id = id,
    templateId = templateId,
    exerciseId = exerciseId,
    position = position,
    exerciseName = exerciseName,
    primaryMuscle = primaryMuscle,
    equipment = equipment,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    targetRpeHalves = targetRpeHalves,
    weightUnit = WeightUnit.fromName(weightUnit),
    stepGrams = stepGrams,
    progressionMode = ProgressionMode.fromName(progressionMode),
    supersetGroup = supersetGroup,
    sets = sets,
)

internal fun TemplateSetEntity.toDomain(): TemplateSet = TemplateSet(
    id = id,
    templateExerciseId = templateExerciseId,
    setIndex = setIndex,
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetRepsCurrent = targetRepsCurrent,
    dropValueGrams = dropValueGrams,
    note = note,
)

/**
 * A template and how many exercises it holds.
 *
 * The count comes from SQL rather than from loading the exercises, so the list can
 * render every template without touching the join table.
 */
data class TemplateSummaryRow(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    /** The template this row is a kept version of, or null (ROADMAP N134). */
    val versionOf: String?,
    /**
     * When the row was written (ROADMAP N134).
     *
     * A version's kept-at is read off it; the templates query selects it too because one row type
     * serves both reads, and the mapper only lets a version keep the timestamp.
     */
    val createdAt: Long,
    /**
     * When the lifter hid this template from the choices, or null (ROADMAP N214).
     *
     * Selected by every read that serves the list and the editor; a version's read carries it too
     * because one row type serves both, and a version is never in the list to be hidden.
     */
    val hiddenAt: Long? = null,
)
