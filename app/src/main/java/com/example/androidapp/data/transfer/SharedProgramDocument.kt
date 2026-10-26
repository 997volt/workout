package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.shared.ProgramSetRole
import com.example.androidapp.shared.Weekday
import java.time.DayOfWeek
import com.example.androidapp.shared.ExerciseDto as SharedExerciseDto
import com.example.androidapp.shared.ProgramDocument as SharedProgramDocument
import com.example.androidapp.shared.ProgramDto as SharedProgramDto
import com.example.androidapp.shared.ProgramKind as SharedProgramKind
import com.example.androidapp.shared.ProgramSlotDto as SharedProgramSlotDto
import com.example.androidapp.shared.TemplateDto as SharedTemplateDto
import com.example.androidapp.shared.TemplateExerciseDto as SharedTemplateExerciseDto
import com.example.androidapp.shared.TemplateSetDto as SharedTemplateSetDto

/**
 * The seam between the app's transfer DTOs and the shared document model (ROADMAP N170).
 *
 * The app keeps its DTOs because the backup speaks them too, and because its mappers and tests are
 * written against them. The **format** is the shared model's: [ProgramDocumentCodec] encodes and
 * decodes through these functions, so there is one reader and one writer for the document rather
 * than two that must be kept equal.
 *
 * The translation is mechanical and deliberately total: every field travels, including the ones the
 * page does not edit, so a round trip through the shared model cannot quietly drop one. The enum
 * mapping is by name in both directions — the format has always been names, never ordinals — and an
 * unknown name throws, which the codec turns into the app's "does not look like a program file".
 */
internal fun ProgramDocument.toShared(): SharedProgramDocument = SharedProgramDocument(
    formatVersion = formatVersion,
    exportedAt = exportedAt,
    program = program.toShared(),
    slots = slots.map { it.toShared() },
    templates = templates.map { it.toShared() },
    templateExercises = templateExercises.map { it.toShared() },
    templateSets = templateSets.map { it.toShared() },
    exercises = exercises.map { it.toShared() },
    edit = edit,
)

internal fun SharedProgramDocument.toApp(): ProgramDocument = ProgramDocument(
    formatVersion = formatVersion,
    exportedAt = exportedAt,
    program = program.toApp(),
    slots = slots.map { it.toApp() },
    templates = templates.map { it.toApp() },
    templateExercises = templateExercises.map { it.toApp() },
    templateSets = templateSets.map { it.toApp() },
    exercises = exercises.map { it.toApp() },
    edit = edit,
)

private fun ProgramDto.toShared() = SharedProgramDto(
    id = id,
    name = name,
    isActive = isActive,
    position = position,
    // The format carries the names the app's own enum already has; the mapping is the seam N170
    // describes, and an unknown name cannot reach here because the codec decoded it first.
    kind = SharedProgramKind.valueOf(kind.name),
    startDate = startDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun SharedProgramDto.toApp() = ProgramDto(
    id = id,
    name = name,
    isActive = isActive,
    position = position,
    kind = ProgramKind.valueOf(kind.name),
    startDate = startDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun ProgramSlotDto.toShared() = SharedProgramSlotDto(
    id = id,
    programId = programId,
    templateId = templateId,
    position = position,
    weekday = weekday?.toShared(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun SharedProgramSlotDto.toApp() = ProgramSlotDto(
    id = id,
    programId = programId,
    templateId = templateId,
    position = position,
    weekday = weekday?.toApp(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun TemplateDto.toShared() = SharedTemplateDto(
    id = id,
    name = name,
    versionOf = versionOf,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun SharedTemplateDto.toApp() = TemplateDto(
    id = id,
    name = name,
    versionOf = versionOf,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun TemplateExerciseDto.toShared() = SharedTemplateExerciseDto(
    id = id,
    templateId = templateId,
    exerciseId = exerciseId,
    position = position,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    targetRpeHalves = targetRpeHalves,
    supersetGroup = supersetGroup,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun SharedTemplateExerciseDto.toApp() = TemplateExerciseDto(
    id = id,
    templateId = templateId,
    exerciseId = exerciseId,
    position = position,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    targetRpeHalves = targetRpeHalves,
    supersetGroup = supersetGroup,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun TemplateSetDto.toShared() = SharedTemplateSetDto(
    id = id,
    templateExerciseId = templateExerciseId,
    setIndex = setIndex,
    role = role.toShared(),
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetRepsCurrent = targetRepsCurrent,
    dropValueGrams = dropValueGrams,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun SharedTemplateSetDto.toApp() = TemplateSetDto(
    id = id,
    templateExerciseId = templateExerciseId,
    setIndex = setIndex,
    role = role.toApp(),
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetRepsCurrent = targetRepsCurrent,
    dropValueGrams = dropValueGrams,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun ExerciseDto.toShared() = SharedExerciseDto(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle?.name,
    secondaryMuscles = secondaryMuscles.map { it.name },
    equipment = equipment?.name,
    movementPattern = movementPattern?.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    weightUnit = weightUnit,
    stepGrams = stepGrams,
    parentId = parentId,
    rowKind = rowKind.name,
    // The two flags that change what a set means (ROADMAP N219, N220, B247). Dropped here, a document
    // would recreate a pull-up as an ordinary movement and a plank as a counted lift.
    isBodyweight = isBodyweight,
    isTimed = isTimed,
)

private fun SharedExerciseDto.toApp() = ExerciseDto(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle?.let(MuscleGroup::valueOf),
    secondaryMuscles = secondaryMuscles.map(MuscleGroup::valueOf),
    equipment = equipment?.let(Equipment::valueOf),
    movementPattern = movementPattern?.let(MovementPattern::valueOf),
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    weightUnit = weightUnit,
    stepGrams = stepGrams,
    parentId = parentId,
    rowKind = RowKind.valueOf(rowKind),
    isBodyweight = isBodyweight,
    isTimed = isTimed,
)

private fun DayOfWeek.toShared(): Weekday = Weekday.valueOf(name)

private fun Weekday.toApp(): DayOfWeek = DayOfWeek.valueOf(name)

private fun SetType.toShared(): ProgramSetRole = ProgramSetRole.valueOf(name)

private fun ProgramSetRole.toApp(): SetType = SetType.valueOf(name)
