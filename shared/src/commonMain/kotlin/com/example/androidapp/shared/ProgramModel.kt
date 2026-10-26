package com.example.androidapp.shared

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * One program and the templates it orders, in the shape the app writes it (ROADMAP N170).
 *
 * This is the app's `ProgramDocument` and its DTOs, moved here so the page and the app read and write
 * one model rather than two that must be kept equal. The JSON is unchanged: the field names, the enum
 * names and every default are the ones [com.example.androidapp.data.transfer.ProgramDocumentCodec]
 * wrote at format version 4, so a file made by the app opens in the page and the other way round.
 *
 * **Enums are names here, not Kotlin types.** The app's taxonomy (`MuscleGroup`, `Equipment`,
 * `MovementPattern`, `RowKind`) and its `SetType` keep their own types on the JVM; this model carries
 * the same names as strings and [ProgramSetRole], and the app maps at its transfer edge. That is what
 * lets the same model compile for JS, where none of those JVM-side types exist.
 */
@Serializable
data class ProgramDocument(
    val formatVersion: Int,
    val exportedAt: Long,
    val program: ProgramDto,
    val slots: List<ProgramSlotDto> = emptyList(),
    val templates: List<TemplateDto> = emptyList(),
    val templateExercises: List<TemplateExerciseDto> = emptyList(),
    val templateSets: List<TemplateSetDto> = emptyList(),
    val exercises: List<ExerciseDto> = emptyList(),
    /**
     * Present only on an edit (ROADMAP N171).
     *
     * A shared document is a proposal to add; an edit is a proposal to change the program it names.
     * The marker is what tells the app which it is holding, and the only format-4 addition N171 makes:
     * a build without it ignores the field and no-ops as it already does.
     *
     * Omitted when null, so an ordinary export's JSON is unchanged by this field's existence.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val edit: EditMarker? = null,
)

/** The base an edit was made from, so the app can refuse an edit of a program that moved since (N171). */
@Serializable
data class EditMarker(val baseExportedAt: Long)

@Serializable
data class ProgramDto(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    val position: Int = 0,
    /**
     * A week that repeats, or an order that ends (ROADMAP N174).
     *
     * Defaulted so a document written before the choice existed is what it always was: a weekly
     * program. The **format** moved with it (N185), because a build that did not know the name would
     * read a sequence as weekly and repeat the block forever.
     */
    val kind: ProgramKind = ProgramKind.WEEKLY,
    /** The day a block begins, as `LocalDate.toEpochDay()`, or null for the day it was created. */
    val startDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** Whether a program repeats or ends, by name (ROADMAP N174). */
@Serializable
enum class ProgramKind { WEEKLY, SEQUENCE }

@Serializable
data class ProgramSlotDto(
    val id: String,
    val programId: String,
    val templateId: String,
    val position: Int,
    val weekday: Weekday? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class TemplateDto(
    val id: String,
    val name: String,
    val versionOf: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class TemplateExerciseDto(
    val id: String,
    val templateId: String,
    val exerciseId: String,
    val position: Int,
    val restSeconds: Int? = null,
    val techniqueNote: String? = null,
    val targetRpeHalves: Int? = null,
    val supersetGroup: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class TemplateSetDto(
    val id: String,
    val templateExerciseId: String,
    val setIndex: Int,
    val role: ProgramSetRole,
    val targetWeightGrams: Long? = null,
    val targetAssistanceGrams: Long? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetRepsCurrent: Int? = null,
    val dropValueGrams: Long? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val primaryMuscle: String?,
    val secondaryMuscles: List<String>,
    val equipment: String?,
    val movementPattern: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val restSeconds: Int? = null,
    val techniqueNote: String? = null,
    val weightUnit: String? = null,
    val stepGrams: Long? = null,
    val parentId: String? = null,
    val rowKind: String = ROW_KIND_MOVEMENT,
    /**
     * Whether the movement is loaded by the body, or held rather than counted (ROADMAP N219, N220).
     *
     * Defaulted, like every added field, so a document written before them decodes as the ordinary,
     * counted movements it described. Carried because the document brings an exercise *whole* (N47): a
     * pull-up arriving as an ordinary lift would have its sets read at their added weight alone (B247).
     */
    val isBodyweight: Boolean = false,
    val isTimed: Boolean = false,
)

/** A day of the week, by name, as the app's `java.time.DayOfWeek` serializes (ROADMAP N170). */
@Serializable
enum class Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

/** The app's `SetType`, by name (ROADMAP N170). */
@Serializable
enum class ProgramSetRole {
    NORMAL,
    WARMUP,
    TOP_SET,
    DROP,
    CLUSTER,
    ;

    /** A rung of the group above it, never judged or loaded on its own (N79). */
    val isRung: Boolean get() = this == DROP || this == CLUSTER
}

const val ROW_KIND_MOVEMENT: String = "MOVEMENT"
const val ROW_KIND_CATEGORY: String = "CATEGORY"
