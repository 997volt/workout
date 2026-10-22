package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import com.example.androidapp.platform.CrashLog
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

/**
 * The on-disk backup format (ROADMAP P1.12).
 *
 * Because platform backup is off, this file is the **only** escape hatch: a lost
 * signing key, a signature change or a simple uninstall otherwise means permanent
 * loss. That shapes three decisions:
 *
 *  - **JSON, not CSV.** A CSV cannot represent this schema without flattening it,
 *    and a flattened export cannot be restored faithfully. The point of the file
 *    is a round trip, so it has to carry the real shape.
 *  - **Raw storage values, not domain shapes.** Enums are written by name and
 *    timestamps as epoch millis — exactly what the database holds. Re-deriving
 *    them through the domain layer on import would risk silently changing data.
 *  - **[schemaVersion] is checked.** A file written by a *newer* app is refused
 *    rather than partially read, because guessing at unknown fields is how a
 *    restore quietly loses a column.
 *
 * Soft-deleted rows are included on purpose: they are part of the data, and
 * dropping them would make the restored database differ from the original.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: Long,
    val exercises: List<ExerciseDto>,
    val sessions: List<SessionDto>,
    val sessionExercises: List<SessionExerciseDto>,
    val sets: List<SetDto>,
    /**
     * The muscles each session reported sore, each with its own score (ROADMAP N62).
     *
     * Defaulted like every added collection: a file written before the list existed still decodes,
     * and its sessions simply report no soreness. It must be here in the same change as the table,
     * or an export would silently carry none of it.
     */
    val sessionSoreMuscles: List<SessionSoreMuscleDto> = emptyList(),
    /**
     * The joints each session exercise reported painful, with their sides and scores (ROADMAP N63).
     *
     * Defaulted like every added collection, and it must be here in the same change as the table, or
     * an export would silently carry none of it.
     */
    val sessionExerciseJoints: List<SessionExerciseJointDto> = emptyList(),
    /**
     * Templates and their exercises (ROADMAP N3).
     *
     * Defaulted so a file written before templates existed still decodes — the same
     * rule every added field follows, and the reason the schema version is not
     * bumped for an addition.
     */
    val templates: List<TemplateDto> = emptyList(),
    val templateExercises: List<TemplateExerciseDto> = emptyList(),
    /** A plan's sets (ROADMAP N14). Defaulted, like every added collection. */
    val templateSets: List<TemplateSetDto> = emptyList(),
    /**
     * Programs: an ordered list of templates, each with a weekday (ROADMAP P3.3).
     *
     * Defaulted like every added collection. A program is the user's authored training
     * setup, so it rides in the file with the templates it orders.
     */
    val programs: List<ProgramDto> = emptyList(),
    val programSlots: List<ProgramSlotDto> = emptyList(),
    /**
     * The weeks an occurrence was consciously passed over (ROADMAP P3.3).
     *
     * They are history, not configuration: without them a restored schedule cannot tell a
     * skip from a miss, which is exactly what P3.5 will read them for.
     */
    val programSkips: List<ProgramSkipDto> = emptyList(),
    /**
     * The weeks a program was deliberately backed off (ROADMAP P3.10).
     *
     * Authored setup rather than something the app can recompute: without these rows a restored
     * block cannot tell a deload week from a missed one, and every backed-off week would read as a
     * failure that never happened.
     */
    val programDeloads: List<ProgramDeloadDto> = emptyList(),
    /**
     * The days a marked deload week prescribed (ROADMAP N147).
     *
     * Defaulted like every added collection, and it must be here in the same change as the table: a
     * restore that kept the week's exemption but lost the workouts it excused would be a week the app
     * silently rewrites. The templates those days name travel in [templates] like any other workout.
     */
    val programDeloadDays: List<ProgramDeloadDayDto> = emptyList(),
    /**
     * The workouts that stood in for a slot's own, one week at a time (ROADMAP P3.11).
     *
     * Authored choices the app cannot recompute: without them a restored week would be read as
     * missed even though it was trained, with something else.
     */
    val programSubstitutions: List<ProgramSubstitutionDto> = emptyList(),
    /**
     * Body measurements (ROADMAP N32).
     *
     * Defaulted, like every added collection — and it must be here in the same change as the table,
     * or an export would silently carry none of them.
     */
    val measurements: List<MeasurementDto> = emptyList(),
    /**
     * The muscles each daily measurement reported sore (ROADMAP N62, moved here by N113).
     *
     * Defaulted, like every added collection — and it must be here in the same change as the table, or
     * an export would silently carry none of them. The session's own sore rows still travel in
     * [sessionSoreMuscles]: they are what an older workout opened with, and history keeps reading them.
     */
    val measurementSoreMuscles: List<MeasurementSoreMuscleDto> = emptyList(),
    /**
     * The periods each metric's goal went through (ROADMAP N137).
     *
     * The rows are what makes a goal's **past** a fact: a file that carried only the current value would
     * restore a lifter's goal and lose the answer to "what was I asking for in March". They travel beside
     * [goals] rather than replacing them — those two maps are a build that predates periods, and a file still
     * has to read in one.
     *
     * Defaulted like every added field, so a file written before periods existed imports as the single goal it
     * holds: [goals] and [goalKinds] are read as one undated period per metric when this is empty.
     */
    val goalPeriods: List<GoalPeriodDto> = emptyList(),
    /**
     * The goal values as a build that **predates periods** reads them, keyed by `MetricKey.id` in each metric's
     * own units (ROADMAP N39, N137).
     *
     * They are the compatibility half of the file rather than the store: a goal is a row in `metric_goals` now,
     * and these two maps are written **from the period that is open** so the halves cannot disagree. A metric
     * with a value and no kind in [goalKinds] is a goal from a file written before kinds existed, and reads as a
     * **target**. The rest of settings (rest, cue, keep-screen-on, the statistics range) stays a device
     * preference and is deliberately not in this file (N21).
     *
     * Defaulted so a file written before this field existed still decodes.
     */
    val goals: Map<String, Double> = emptyMap(),
    /**
     * The **kind** of each goal in [goals], by `WeightGoalKind`'s name, keyed the same way (ROADMAP N135).
     *
     * Beside the values rather than folded into them: the goals map is one number per metric and is already
     * in files this app has written, so a goal's kind travels as its own map. A metric with a value and no
     * kind here is a goal from a file written before kinds existed, and reads as a **target**.
     *
     * Defaulted so a file written before this field existed still decodes.
     */
    val goalKinds: Map<String, String> = emptyMap(),
    /**
     * Diagnostics, not user data (ROADMAP F11). They ride along with an export
     * because a release build is not debuggable and this is the only way a crash log
     * reaches the user; import deliberately ignores them.
     *
     * Defaulted so a file written before this field existed still decodes.
     */
    val crashLogs: List<CrashLog> = emptyList(),
)

@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    /**
     * What this row states, or null for *unstated* (ROADMAP N103).
     *
     * Nullable because 2.0's model is "a row stores what it states, and anything unstated is read from the
     * nearest row above that states it". These four stay **required** rather than defaulted: a document that
     * omits them is a pre-2.0 one, and the gate refuses that by its version rather than letting a default
     * quietly answer for a row that meant to state something.
     */
    val primaryMuscle: MuscleGroup?,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment?,
    val movementPattern: MovementPattern?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /**
     * The exercise's own rest and cue (ROADMAP N5). Defaulted, not required: a file
     * written before these existed must still decode, and the codec's schema version
     * is deliberately not bumped for an added field (see `BackupCodecTest`).
     */
    val restSeconds: Int? = null,
    val techniqueNote: String? = null,
    /**
     * The exercise's own display unit by name, or null to follow the app setting (ROADMAP N64).
     *
     * Defaulted, not required: a file written before this existed still decodes, and the codec's
     * schema version is deliberately not bumped for an added field (see `BackupCodecTest`).
     */
    val weightUnit: String? = null,
    /**
     * The exercise's own weight step in whole grams, or null for the unit's (ROADMAP N77).
     *
     * Defaulted and unbumped, like the unit above: a file written before this existed still decodes,
     * and an absent value means exactly what the app meant before the field did.
     */
    val stepGrams: Long? = null,
    /**
     * The row this exercise hangs under, or null (ROADMAP N95).
     *
     * Defaulted and unbumped, like the two fields above: a file written before categories existed decodes
     * with every row pristine, and null means exactly what the app meant before the field did.
     */
    val parentId: String? = null,
    /**
     * Whether this row is a movement or a category head, stored by name (ROADMAP N95).
     *
     * **Defaulted rather than required, and the default is the load-bearing part.** Every row a file could
     * have held before categories is a movement, so an absent value has one true meaning — and the opposite
     * default would turn an older file's whole library into heads, which are never offered.
     */
    val rowKind: RowKind = RowKind.MOVEMENT,
)

/**
 * One dated set of body measurements (ROADMAP N32, two records since N113).
 *
 * [kind] is **required rather than defaulted**, for the reason [ExerciseDto]'s taxonomy is: a row must
 * state which of the two records it is, and the version gate refuses a file that predates the split
 * rather than letting a default quietly answer for a row that meant to state something. Every number is
 * nullable and null means "not taken" rather than zero — a waist recorded without a scale reading is a
 * real entry, and a zero would be indistinguishable from a measurement of nothing.
 */
@Serializable
data class MeasurementDto(
    val id: String,
    val measuredAt: Long,
    /** Which of the two records this is (ROADMAP N113). */
    val kind: MeasurementKind,
    /** Whole grams, or null when the scale was not stepped on (ROADMAP N113). */
    val weightGrams: Long? = null,
    /** Tenths of a percent, the unit RPE halves exist for. */
    val bodyFatTenths: Int? = null,
    val muscleTenths: Int? = null,
    /** Tape sites in millimetres, every one optional. */
    val neckMm: Long? = null,
    val chestMm: Long? = null,
    val waistMm: Long? = null,
    val hipsMm: Long? = null,
    val upperArmMm: Long? = null,
    val thighMm: Long? = null,
    val calfMm: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * One muscle a day's measurement reported sore, and its score (ROADMAP N62, moved by N113).
 *
 * Its own collection rather than a column, the shape it had on a session: one muscle's score is read
 * on its own later, and a serialized list would have to be parsed to ask.
 */
@Serializable
data class MeasurementSoreMuscleDto(
    val id: String,
    val measurementId: String,
    val muscle: MuscleGroup,
    /** 1–10, on `TenPointScale`. */
    val score: Int,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class SessionDto(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val notes: String? = null,
    /** Defaulted for the same reason as [ExerciseDto.restSeconds] (ROADMAP N4). */
    val readinessNote: String? = null,
    /**
     * The zone the session was performed in, in minutes from UTC (ROADMAP N25).
     *
     * Null for sessions recorded before the column existed; a restored one keeps that null rather
     * than acquiring an offset it never had.
     */
    val zoneOffsetMinutes: Int? = null,
    /**
     * The template this session was started from, or null (ROADMAP P3.3).
     *
     * Defaulted for the same reason as every added field: a file written before programs
     * existed still decodes, and its sessions simply have no provenance.
     */
    val templateId: String? = null,
    /**
     * The activity this session is, by name, or null for a workout (ROADMAP N141).
     *
     * Defaulted for the same reason as every added field: a file written before activities existed still
     * decodes, and its sessions are workouts.
     */
    val activity: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class SessionExerciseDto(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val position: Int,
    /** Defaulted for the same reason as [ExerciseDto.restSeconds] (ROADMAP N7, N8). */
    val finishedAt: Long? = null,
    val muscleFeel: Int? = null,
    /** The rest and cue the plan prescribed for this exercise, if any (N14). */
    val restSeconds: Int? = null,
    val techniqueNote: String? = null,
    /**
     * Which superset or circuit this exercise belonged to (ROADMAP N24), or null.
     *
     * Defaulted like every field this file has gained, so a backup written before N24 still
     * reads: an ungrouped exercise is what every exercise used to be.
     */
    val supersetGroup: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * One muscle a session reported sore, and its score (ROADMAP N62).
 *
 * [muscle] travels as the enum name, like every other enum in this file, and [position] is the
 * order the lifter picked them in — data, not a presentational detail.
 */
@Serializable
data class SessionSoreMuscleDto(
    val id: String,
    val sessionId: String,
    val muscle: MuscleGroup,
    /** 1–10, on `TenPointScale`. */
    val score: Int,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * One joint a session exercise reported painful, its side, and its score (ROADMAP N63).
 *
 * [joint] and [side] travel as enum names, like every other enum in this file, and [position] is
 * the order the lifter picked them in — data, not a presentational detail.
 */
@Serializable
data class SessionExerciseJointDto(
    val id: String,
    val sessionExerciseId: String,
    val joint: Joint,
    val side: Side,
    /** 1–10, on `TenPointScale`. */
    val score: Int,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class SetDto(
    val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weightGrams: Long,
    /** The machine's assistance in grams, 0 for none (ROADMAP N15). */
    val assistanceGrams: Long = 0,
    val setType: SetType,
    /** Defaulted for the same reason as [ExerciseDto.restSeconds] (ROADMAP N6). */
    val rpeHalves: Int? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * One period of one metric's goal (ROADMAP N137).
 *
 * All three of `kind`, `value` and `startedAt` are nullable on purpose, and each null means something
 * different: no kind is a goal written before kinds existed (a target), no value is a direction-only goal, and
 * no start is the period the move could not date. `kind` and `value` both null is a period of **no goal** —
 * clearing one closes a period rather than deleting a row.
 */
@Serializable
data class GoalPeriodDto(
    val id: String,
    val metricId: String,
    val kind: String? = null,
    val value: Double? = null,
    val startedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class TemplateDto(
    val id: String,
    val name: String,
    /**
     * The template this row is a kept version of, or null (ROADMAP N134).
     *
     * Defaulted like every added field, so a file written before versions existed imports as the
     * templates it holds. The export **carries versions** rather than dropping them: the file is the
     * only path off the device, and flattening a kept version into an ordinary template — or losing it
     * — is the one loss this feature cannot survive.
     */
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
    /** The rest and cue the plan prescribes, or null to use the library's (N14). */
    val restSeconds: Int? = null,
    val techniqueNote: String? = null,
    /**
     * The effort the plan builds to, in half-points, or null (N59, amended).
     *
     * One number per exercise rather than one per set. Defaulted, like every added field.
     */
    val targetRpeHalves: Int? = null,
    /** The superset this exercise is planned in, or null (ROADMAP N24, B16). */
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
    val role: SetType,
    val targetWeightGrams: Long? = null,
    val targetAssistanceGrams: Long? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    /** Where in the range the lifter has climbed, or null (ROADMAP N74). */
    val targetRepsCurrent: Int? = null,
    /**
     * The run's drop value in grams, or null (ROADMAP N79).
     *
     * Held by the run's first rung and inherited by the rest, so a null here means either "not a rung"
     * or "another rung of the same run". Defaulted and unbumped like every other added field: a file
     * written before this decodes, and an absent value means exactly what it meant before.
     */
    val dropValueGrams: Long? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** A program (ROADMAP P3.3, P3.12): a name, whether home follows it, and its place in the order. */
@Serializable
data class ProgramDto(
    val id: String,
    val name: String,
    val isActive: Boolean = false,
    /**
     * The authored order, low first (ROADMAP P3.12).
     *
     * Defaulted so a file written before programs could be ordered still decodes. Every row then
     * carries 0 and the list falls back to name order — the old active-first grouping is not
     * reconstructed — and the first move re-numbers the whole list, so a restored file is
     * reorderable rather than stuck.
     */
    val position: Int = 0,
    /**
     * A week that repeats, or an order that ends (ROADMAP N174).
     *
     * Defaulted so every file written before the choice existed decodes as what it is: a weekly
     * program. The **backup's** schema version moved with it, because a build that did not know the
     * name would read a sequence as a weekly program and repeat it forever (N185).
     */
    val kind: ProgramKind = ProgramKind.WEEKLY,
    /** The day a block begins, as `LocalDate.toEpochDay()`, or null (ROADMAP N174). */
    val startDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** One slot of a program: a template, a place in the order, and a weekday (ROADMAP P3.3). */
@Serializable
data class ProgramSlotDto(
    val id: String,
    val programId: String,
    val templateId: String,
    val position: Int,
    val weekday: DayOfWeek? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** A recorded skip: a slot's occurrence in one week (ROADMAP P3.3). */
@Serializable
data class ProgramSkipDto(
    val id: String,
    val slotId: String,
    /** Monday of the week, as `LocalDate.toEpochDay()`. */
    val weekStart: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** A week a program was deliberately backed off (ROADMAP P3.10). */
@Serializable
data class ProgramDeloadDto(
    val id: String,
    val programId: String,
    /** Monday of the deloaded week, as `LocalDate.toEpochDay()`. */
    val weekStart: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** One day of a marked deload week (ROADMAP N147). */
@Serializable
data class ProgramDeloadDayDto(
    val id: String,
    val deloadId: String,
    val templateId: String,
    val position: Int,
    val weekday: DayOfWeek? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** One occurrence trained with a different workout (ROADMAP P3.11). */
@Serializable
data class ProgramSubstitutionDto(
    val id: String,
    val slotId: String,
    /** Monday of the week, as `LocalDate.toEpochDay()`. */
    val weekStart: Long,
    val templateId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
