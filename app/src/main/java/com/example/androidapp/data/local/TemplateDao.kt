package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Templates and the exercises they hold (ROADMAP N3).
 *
 * Its own DAO rather than more methods on `WorkoutDao`: that interface was already
 * at its ceiling, and a template is a different aggregate from a session — the two
 * only meet when a workout is started from one.
 */
@Dao
interface TemplateDao {

    /**
     * Plans an exercise into a superset, or leaves one (ROADMAP B16).
     *
     * A nullable group, so grouping and ungrouping are the same write. One statement for the whole
     * group, so a plan cannot end up half-paired (ROADMAP B27). Rows updated: how many of [ids] still
     * exist and are live.
     */
    @Query(
        """
        UPDATE template_exercises
        SET supersetGroup = :group, updatedAt = :at
        WHERE id IN (:ids) AND deletedAt IS NULL
        """,
    )
    suspend fun setSupersetGroup(ids: List<String>, group: Int?, at: Long): Int

    /**
     * Every live template with its exercise count, name-ordered.
     *
     * `versionOf IS NULL` beside `deletedAt IS NULL` (ROADMAP N134): a kept version is a template row of
     * its own, and this one predicate is what keeps it out of the list rather than a second table.
     */
    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount,
               t.versionOf AS versionOf,
               t.createdAt AS createdAt,
               t.hiddenAt AS hiddenAt
        FROM templates t
        WHERE t.deletedAt IS NULL AND t.versionOf IS NULL
        ORDER BY t.name ASC
        """,
    )
    fun observeTemplates(): Flow<List<TemplateSummaryRow>>

    /**
     * The workouts this device deleted, name-ordered, with their planned exercise counts (ROADMAP N216).
     *
     * The other side of [observeTemplates]' `deletedAt` predicate. A **kept version** is not here
     * (ROADMAP N134): a group only the lifters' own put-away workouts can appear in must not offer a
     * version as one of them, and a version never sat in the list to be deleted.
     *
     * The count reads `template_exercises` live rows, which a delete never touched: it flags the
     * template alone, so a restored workout comes back with its plan rather than as an empty shell.
     */
    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount,
               t.versionOf AS versionOf,
               t.createdAt AS createdAt,
               t.hiddenAt AS hiddenAt
        FROM templates t
        WHERE t.deletedAt IS NOT NULL AND t.versionOf IS NULL
        ORDER BY t.name ASC
        """,
    )
    fun observeDeletedTemplates(): Flow<List<TemplateSummaryRow>>

    /**
     * Every live kept version of one template, newest first (ROADMAP N134).
     *
     * The other side of [observeTemplates]' predicate: these are the rows it hides, read back where
     * they belong — inside the template they were kept from.
     */
    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount,
               t.versionOf AS versionOf,
               t.createdAt AS createdAt,
               t.hiddenAt AS hiddenAt
        FROM templates t
        WHERE t.deletedAt IS NULL AND t.versionOf = :templateId
        ORDER BY t.createdAt DESC
        """,
    )
    fun observeVersions(templateId: String): Flow<List<TemplateSummaryRow>>

    /** A live template, and **not a version of one**: a version is never edited (ROADMAP N134). */
    @Query("SELECT * FROM templates WHERE id = :id AND deletedAt IS NULL AND versionOf IS NULL")
    suspend fun findById(id: String): TemplateEntity?

    /** One template with its count, re-emitting when it is renamed (N3). */
    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount,
               t.versionOf AS versionOf,
               t.createdAt AS createdAt,
               t.hiddenAt AS hiddenAt
        FROM templates t
        WHERE t.id = :id AND t.deletedAt IS NULL AND t.versionOf IS NULL
        """,
    )
    fun observeTemplate(id: String): Flow<TemplateSummaryRow?>

    /**
     * One template's name whether or not it is deleted, so a past workout keeps naming the plan it was
     * (N58, B74).
     *
     * [observeTemplate] above is the *editor's* read and correctly hides a deleted row — you cannot edit a
     * plan that is gone. History's read must not: a soft delete keeps the row for the export (P1.12), so the
     * name is still there and the workout still says which workout it was.
     */
    @Query("SELECT name FROM templates WHERE id = :id")
    fun observeTemplateName(id: String): Flow<String?>

    @Query("SELECT * FROM template_exercises WHERE id = :id AND deletedAt IS NULL")
    suspend fun findTemplateExercise(id: String): TemplateExerciseEntity?

    @Insert
    suspend fun insertTemplate(template: TemplateEntity)

    /**
     * Rows updated: 0 means the template is gone, was already deleted, or is a **version** (ROADMAP N134).
     *
     * The version predicate is the "never edited" rule at the storage boundary: a version stores its own
     * name, and renaming one would change what a kept plan says it was.
     */
    @Query(
        """
        UPDATE templates
        SET name = :name, updatedAt = :at
        WHERE id = :id AND deletedAt IS NULL AND versionOf IS NULL
        """,
    )
    suspend fun rename(id: String, name: String, at: Long): Int

    @Query("UPDATE templates SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDeleteTemplate(id: String, at: Long): Int

    /**
     * Brings a deleted workout back, plan untouched (ROADMAP N216).
     *
     * Rows updated: 0 means the id is not a deleted workout — live, a kept version, or absent — which
     * the caller reports as not found. **`updatedAt` moves**, because the delete moved it: the clock
     * N171 reads must see a restore as a change, or a program document exported before it would merge
     * into a definition that had in fact shifted.
     *
     * It writes no other column: `hiddenAt` is the lifter's own view of the row and survives the round
     * trip, so a workout deleted while hidden comes back hidden.
     */
    @Query(
        """
        UPDATE templates
        SET deletedAt = NULL, updatedAt = :at
        WHERE id = :id AND deletedAt IS NOT NULL AND versionOf IS NULL
        """,
    )
    suspend fun restoreTemplate(id: String, at: Long): Int

    /**
     * Hides a template from the choices, or brings it back (ROADMAP N214). Rows updated: 0 when it is gone.
     *
     * **`updatedAt` is deliberately not written.** Hiding changes no plan, and that column is the clock
     * N171 reads to refuse a stale edit, so moving it would make every program carrying this workout
     * read as changed on the phone. The version predicate is [rename]'s rule: a kept version is never
     * hidden, because it never appears in the list to need it.
     */
    @Query(
        """
        UPDATE templates
        SET hiddenAt = :at
        WHERE id = :id AND deletedAt IS NULL AND versionOf IS NULL
        """,
    )
    suspend fun setHiddenAt(id: String, at: Long?): Int

    /** The template's exercises with their library details, in stored order. */
    @Query(
        """
        SELECT te.id AS id,
               te.templateId AS templateId,
               te.exerciseId AS exerciseId,
               te.position AS position,
               e.name AS exerciseName,
               -- The effective muscle, resolved one level up, and nullable like the columns behind it
               -- (ROADMAP B103, N103) — see `WorkoutDao.observeSessionExerciseDetails`.
               COALESCE(e.primaryMuscle, parent.primaryMuscle) AS primaryMuscle,
               e.equipment AS equipment,
               te.restSeconds AS restSeconds,
               te.techniqueNote AS techniqueNote,
               te.targetRpeHalves AS targetRpeHalves,
               e.weightUnit AS weightUnit,
               e.stepGrams AS stepGrams,
               te.supersetGroup AS supersetGroup
        FROM template_exercises te
        JOIN exercises e ON e.id = te.exerciseId
        LEFT JOIN exercises parent ON parent.id = e.parentId
        WHERE te.templateId = :templateId
          AND te.deletedAt IS NULL
          AND e.deletedAt IS NULL
        ORDER BY te.position ASC
        """,
    )
    fun observeTemplateExercises(templateId: String): Flow<List<TemplateExerciseDetail>>

    /** The same list, one shot — what starting a workout from a template needs. */
    @Query(
        """
        SELECT te.exerciseId AS exerciseId
        FROM template_exercises te
        JOIN exercises e ON e.id = te.exerciseId
        WHERE te.templateId = :templateId
          AND te.deletedAt IS NULL
          AND e.deletedAt IS NULL
        ORDER BY te.position ASC
        """,
    )
    suspend fun findExerciseIdsInOrder(templateId: String): List<String>

    /**
     * The same list with what the plan prescribes for each exercise (ROADMAP N14),
     * which is what a session started from this template seeds onto its rows.
     *
     * **The library's own soft delete is joined and filtered here too** (B156), as it is in every sibling
     * above. Without it a plan naming a removed movement seeded a session row for it, and because the
     * session's own reads *do* join `exercises` and filter `deletedAt IS NULL`, that row arrived invisible,
     * unremovable, and counted by the history's exercise total — a movement the lifter had removed coming back
     * as a phantom.
     */
    @Query(
        """
        SELECT te.exerciseId AS exerciseId, te.restSeconds AS restSeconds,
               te.supersetGroup AS supersetGroup,
               te.techniqueNote AS techniqueNote
        FROM template_exercises te
        JOIN exercises e ON e.id = te.exerciseId
        WHERE te.templateId = :templateId
          AND te.deletedAt IS NULL
          AND e.deletedAt IS NULL
        ORDER BY te.position ASC
        """,
    )
    suspend fun findPlannedExercises(templateId: String): List<PlannedExercise>

    /**
     * A plan's exercise rows, raw and in order (ROADMAP N47).
     *
     * The entity rather than [PlannedExercise]: a program document carries every column, and the
     * projection drops the ids and timestamps the document needs.
     */
    @Query(
        """
        SELECT * FROM template_exercises
        WHERE templateId = :templateId AND deletedAt IS NULL
        ORDER BY position ASC
        """,
    )
    suspend fun findTemplateExercises(templateId: String): List<TemplateExerciseEntity>

    /** One exercise of a plan, as seeding needs it. */
    @Suppress("LongParameterList")
    data class PlannedExercise(
        val exerciseId: String,
        val restSeconds: Int?,
        val techniqueNote: String?,
        /** The superset this exercise is planned in, or null (ROADMAP N24, B16). */
        val supersetGroup: Int?,
    )

    /** Writes a template exercise's prescribed rest, cue and target effort (ROADMAP N14, N59). */
    @Query(
        """
        UPDATE template_exercises
        SET restSeconds = :restSeconds, techniqueNote = :techniqueNote,
            targetRpeHalves = :targetRpeHalves, updatedAt = :at
        WHERE id = :id AND deletedAt IS NULL
        """,
    )
    suspend fun setExercisePlan(
        id: String,
        restSeconds: Int?,
        techniqueNote: String?,
        targetRpeHalves: Int?,
        at: Long,
    ): Int

    /** Next free position; -1 on an empty template, so callers add 1. */
    @Query("SELECT COALESCE(MAX(position), -1) FROM template_exercises WHERE templateId = :templateId")
    suspend fun maxPosition(templateId: String): Int

    @Insert
    suspend fun insertTemplateExercise(row: TemplateExerciseEntity)

    @Query(
        """
        UPDATE template_exercises
        SET deletedAt = :at, updatedAt = :at
        WHERE id = :id AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteTemplateExercise(id: String, at: Long): Int

    @Query(
        """
        UPDATE template_exercises
        SET position = :position, updatedAt = :at
        WHERE id = :id AND deletedAt IS NULL
        """,
    )
    suspend fun setPosition(id: String, position: Int, at: Long): Int

    /**
     * Swaps two rows' positions in one transaction, so a reorder cannot leave the
     * list half-moved if the second write fails.
     */
    @Transaction
    suspend fun swapPositions(
        firstId: String,
        firstPosition: Int,
        secondId: String,
        secondPosition: Int,
        at: Long,
    ) {
        setPosition(id = firstId, position = firstPosition, at = at)
        setPosition(id = secondId, position = secondPosition, at = at)
    }
}
