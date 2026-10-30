package com.example.androidapp.data.transfer

import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.MeasurementEntity
import com.example.androidapp.data.local.MeasurementSoreMuscleEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.TemplateSetEntity
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.validLibraryShape

/**
 * Entity <-> backup DTO (ROADMAP P1.12).
 *
 * Field-for-field and in both directions, so an export followed by an import is a
 * no-op on the data. Anything clever here would show up as a restore that is
 * subtly not the original.
 */

internal fun MeasurementEntity.toDto() = MeasurementDto(
    id = id,
    measuredAt = measuredAt,
    kind = kind,
    weightGrams = weightGrams,
    bodyFatTenths = bodyFatTenths,
    muscleTenths = muscleTenths,
    neckMm = neckMm,
    chestMm = chestMm,
    waistMm = waistMm,
    hipsMm = hipsMm,
    upperArmMm = upperArmMm,
    thighMm = thighMm,
    calfMm = calfMm,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun MeasurementDto.toEntity() = MeasurementEntity(
    id = id,
    measuredAt = measuredAt,
    kind = kind,
    weightGrams = weightGrams,
    bodyFatTenths = bodyFatTenths,
    muscleTenths = muscleTenths,
    neckMm = neckMm,
    chestMm = chestMm,
    waistMm = waistMm,
    hipsMm = hipsMm,
    upperArmMm = upperArmMm,
    thighMm = thighMm,
    calfMm = calfMm,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun MeasurementSoreMuscleEntity.toDto() = MeasurementSoreMuscleDto(
    id = id,
    measurementId = measurementId,
    muscle = muscle,
    score = score,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun MeasurementSoreMuscleDto.toEntity() = MeasurementSoreMuscleEntity(
    id = id,
    measurementId = measurementId,
    muscle = muscle,
    score = score,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun ExerciseEntity.toDto() = ExerciseDto(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    movementPattern = movementPattern,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    weightUnit = weightUnit,
    stepGrams = stepGrams,
    isBodyweight = isBodyweight,
    isTimed = isTimed,
    parentId = parentId,
    rowKind = rowKind,
    progressionMode = progressionMode,
)

internal fun ExerciseDto.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    movementPattern = movementPattern,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    // The exercise's own display unit (ROADMAP N64). A restore that dropped it silently reverted
    // every exercise to the app setting, which is the whole thing that field exists to override.
    weightUnit = weightUnit,
    // And its own weight step (ROADMAP N77), for the same reason: dropping it would silently put
    // every exercise back on the unit's step, which is what the field exists to override.
    stepGrams = stepGrams,
    // Whether the movement is loaded by the body (ROADMAP N219). Dropping it would restore a pull-up as an
    // ordinary movement, and its sets would then read as their added weight alone.
    isBodyweight = isBodyweight,
    isTimed = isTimed,
    // The library's shape (ROADMAP N95). Dropping `parentId` would flatten a restored library into a list,
    // and dropping `rowKind` would turn every head into a lift a picker offers — the two halves of the
    // feature that a round trip has to carry or it is not a round trip.
    parentId = parentId,
    rowKind = rowKind,
    // The movement's own answer to Done (ROADMAP N235). Dropping it would restore a movement the app
    // deliberately never asks as one it asks every session, which is the override this field exists for.
    progressionMode = progressionMode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/**
 * [validLibraryShape] for a backup DTO (ROADMAP N95, N110, B92): the import writes raw rows, so the shape
 * rule has to run before the entity does. A legal library is unchanged; a movement filed under a movement —
 * what every variation in an older file is — is **flattened under the category it belonged to**, with what it
 * used to read from that movement written onto it. Clearing the link instead would lose the family.
 */
internal fun List<ExerciseDto>.withValidLibraryShape(): List<ExerciseDto> =
    validLibraryShape(
        id = { it.id },
        parentId = { it.parentId },
        isCategory = { it.rowKind == RowKind.CATEGORY },
        withParent = { row, parent -> row.copy(parentId = parent) },
        inherit = { child, parent ->
            child.copy(
                primaryMuscle = child.primaryMuscle ?: parent.primaryMuscle,
                secondaryMuscles = child.secondaryMuscles.ifEmpty { parent.secondaryMuscles },
                equipment = child.equipment ?: parent.equipment,
                movementPattern = child.movementPattern ?: parent.movementPattern,
            )
        },
    )

internal fun WorkoutSessionEntity.toDto() = SessionDto(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt,
    notes = notes,
    readinessNote = readinessNote,
    activity = activity,
    // Carried: where a session happened is a fact about the training,
    // not about the device it was written on (ROADMAP N25).
    zoneOffsetMinutes = zoneOffsetMinutes,
    // Provenance, and history once the session is done (ROADMAP P3.3) — it must survive a restore
    // or a restored schedule would read a done day as missed.
    templateId = templateId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/**
 * A restored session must not still be "in progress": a session left open in the
 * file would hijack the app on next launch and look like a workout the user never
 * started.
 *
 * **A missing end is closed at the start** (B150), not carried through as null. `finishedAt IS NULL` is the
 * app's whole definition of "the workout in progress" — home offers to resume it and the active screen opens
 * on it — so a file exported mid-workout, or one left open by a crash (which the app deliberately preserves),
 * used to decide what this device was doing the moment it was imported. Importing during a workout then wrote
 * a **second** open row, and because history reads only finished sessions and the active read returns the
 * newest, the older one became unreachable. Ending it at its own start keeps the record and its day without
 * claiming the lifter is in it: the session is history, which is the only thing a file can honestly describe.
 */
internal fun SessionDto.toEntity() = WorkoutSessionEntity(
    id = id,
    startedAt = startedAt,
    finishedAt = finishedAt ?: startedAt,
    notes = notes,
    // And the rest countdown is not resurrected either: the store's own column keeps the value, the file
    // does not carry it (N109).
    restEndsAt = null,
    readinessNote = readinessNote,
    zoneOffsetMinutes = zoneOffsetMinutes,
    // An activity survives the restore for the same reason it is stored: the day it answers for.
    activity = activity,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    // Provenance survives the restore: without it a restored done day would read as missed.
    templateId = templateId,
)

internal fun SessionExerciseEntity.toDto() = SessionExerciseDto(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    position = position,
    finishedAt = finishedAt,
    muscleFeel = muscleFeel,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    supersetGroup = supersetGroup,
    // The declined step travels with the file (N236): it is the one record the app cannot recompute.
    declinedDirections = declinedDirections,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun SessionExerciseDto.toEntity() = SessionExerciseEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    position = position,
    finishedAt = finishedAt,
    muscleFeel = muscleFeel,
    restSeconds = restSeconds,
    techniqueNote = techniqueNote,
    supersetGroup = supersetGroup,
    declinedDirections = declinedDirections,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun SetEntryEntity.toDto() = SetDto(
    id = id,
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    reps = reps,
    weightGrams = weightGrams,
    assistanceGrams = assistanceGrams,
    // The snapshot travels with the set (ROADMAP N219): the file is the copy, and a copy that dropped it
    // would restore every bodyweight set as its added weight alone.
    bodyweightGrams = bodyweightGrams,
    durationSeconds = durationSeconds,
    setType = setType,
    rpeHalves = rpeHalves,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun SetDto.toEntity() = SetEntryEntity(
    id = id,
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    reps = reps,
    weightGrams = weightGrams,
    assistanceGrams = assistanceGrams,
    // The snapshot travels with the set (ROADMAP N219): the file is the copy, and a copy that dropped it
    // would restore every bodyweight set as its added weight alone.
    bodyweightGrams = bodyweightGrams,
    durationSeconds = durationSeconds,
    setType = setType,
    // A warm-up carries no effort at all (ROADMAP N67), so the role decides here as it does at
    // every other write boundary.
    rpeHalves = rpeHalves.takeIf { setType.recordsEffort },
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun TemplateEntity.toDto() = TemplateDto(
    id = id,
    name = name,
    versionOf = versionOf,
    hiddenAt = hiddenAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun TemplateDto.toEntity() = TemplateEntity(
    id = id,
    name = name,
    versionOf = versionOf,
    hiddenAt = hiddenAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun TemplateExerciseEntity.toDto() = TemplateExerciseDto(
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

internal fun TemplateExerciseDto.toEntity() = TemplateExerciseEntity(
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

internal fun TemplateSetEntity.toDto() = TemplateSetDto(
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
    targetDurationSeconds = targetDurationSeconds,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun TemplateSetDto.toEntity() = TemplateSetEntity(
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
    targetDurationSeconds = targetDurationSeconds,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
