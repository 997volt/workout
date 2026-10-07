package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    /** Every live template with its exercise count, name-ordered. */
    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount
        FROM templates t
        WHERE t.deletedAt IS NULL
        ORDER BY t.name ASC
        """,
    )
    fun observeTemplates(): Flow<List<TemplateSummaryRow>>

    @Query("SELECT * FROM templates WHERE id = :id AND deletedAt IS NULL")
    suspend fun findById(id: String): TemplateEntity?

    /** One template with its count, re-emitting when it is renamed (N3). */
    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               (
                   SELECT COUNT(*) FROM template_exercises te
                   WHERE te.templateId = t.id AND te.deletedAt IS NULL
               ) AS exerciseCount
        FROM templates t
        WHERE t.id = :id AND t.deletedAt IS NULL
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

    /** Rows updated: 0 means the template is gone or was already deleted. */
    @Query("UPDATE templates SET name = :name, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun rename(id: String, name: String, at: Long): Int

    @Query("UPDATE templates SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDeleteTemplate(id: String, at: Long): Int

    /** The template's exercises with their library details, in stored order. */
    @Query(
        """
        SELECT te.id AS id,
               te.templateId AS templateId,
               te.exerciseId AS exerciseId,
               te.position AS position,
               e.name AS exerciseName,
               e.primaryMuscle AS primaryMuscle,
               e.equipment AS equipment,
               te.restSeconds AS restSeconds,
               te.techniqueNote AS techniqueNote,
               te.targetRpeHalves AS targetRpeHalves,
               e.weightUnit AS weightUnit,
               e.stepGrams AS stepGrams,
               te.supersetGroup AS supersetGroup
        FROM template_exercises te
        JOIN exercises e ON e.id = te.exerciseId
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
     */
    @Query(
        """
        SELECT te.exerciseId AS exerciseId, te.restSeconds AS restSeconds,
               te.supersetGroup AS supersetGroup,
               te.techniqueNote AS techniqueNote
        FROM template_exercises te
        WHERE te.templateId = :templateId AND te.deletedAt IS NULL
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

    /** Every live planned set of a plan's live exercises, in order (ROADMAP N47). */
    @Query(
        """
        SELECT s.* FROM template_sets s
        JOIN template_exercises e ON e.id = s.templateExerciseId
        WHERE e.templateId = :templateId
          AND e.deletedAt IS NULL
          AND s.deletedAt IS NULL
        ORDER BY s.setIndex ASC
        """,
    )
    suspend fun findTemplateSets(templateId: String): List<TemplateSetEntity>

    /** One exercise of a plan, as seeding needs it. */
    @Suppress("LongParameterList")
    data class PlannedExercise(
        val exerciseId: String,
        val restSeconds: Int?,
        val techniqueNote: String?,
        /** The superset this exercise is planned in, or null (ROADMAP N24, B16). */
        val supersetGroup: Int?,
    )

    /**
     * Every planned set of a template, ordered by the exercise's position then the
     * set's index (ROADMAP N14).
     *
     * One query for the whole template rather than one per exercise: the editor shows
     * them all at once, and N+1 flows would be a subscription per exercise.
     */
    @Query(
        """
        SELECT ts.* FROM template_sets ts
        JOIN template_exercises te ON te.id = ts.templateExerciseId
        WHERE te.templateId = :templateId
          AND ts.deletedAt IS NULL
          AND te.deletedAt IS NULL
        ORDER BY te.position ASC, ts.setIndex ASC
        """,
    )
    fun observeTemplateSets(templateId: String): Flow<List<TemplateSetEntity>>

    @Query("SELECT * FROM template_sets WHERE id = :id AND deletedAt IS NULL")
    suspend fun findTemplateSet(id: String): TemplateSetEntity?

        /**
     * Moves every set of one planned exercise down by [by], making room at the head (ROADMAP B34).
     *
     * One statement rather than a loop: a half-shifted plan is a plan whose order is wrong, and the
     * order is the whole point of writing a warm-up ramp in front of the work.
     */
    @Query(
        """
        UPDATE template_sets
        SET setIndex = setIndex + :by, updatedAt = :at
        WHERE templateExerciseId = :templateExerciseId AND deletedAt IS NULL
        """,
    )
    suspend fun shiftSetIndexes(templateExerciseId: String, by: Int, at: Long): Int

@Query(
        """
        SELECT COALESCE(MAX(setIndex), -1) FROM template_sets
        WHERE templateExerciseId = :templateExerciseId AND deletedAt IS NULL
        """,
    )
    suspend fun maxSetIndex(templateExerciseId: String): Int

    @Insert
    suspend fun insertTemplateSet(row: TemplateSetEntity)

    @Update
    suspend fun updateTemplateSet(row: TemplateSetEntity): Int

    @Query(
        """
        UPDATE template_sets
        SET deletedAt = :at, updatedAt = :at
        WHERE id = :id AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteTemplateSet(id: String, at: Long): Int

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

    /**
     * Clears the legacy per-set target effort of one planned exercise (ROADMAP N59).
     *
     * The effort is the exercise's one number now, and the per-set column is only the fallback a plan
     * written before the change arrives with. Clearing the exercise's field is how a lifter says the
     * plan names no effort, and without this the reader would fall back to the per-set value the clear
     * was meant to remove. It is called only on a transition to null, so a plan whose effort only ever
     * lived on its sets — an imported pre-change backup — keeps it.
     */
    @Query(
        """
        UPDATE template_sets
        SET targetRpeHalves = NULL, updatedAt = :at
        WHERE templateExerciseId = :templateExerciseId
          AND targetRpeHalves IS NOT NULL
          AND deletedAt IS NULL
        """,
    )
    suspend fun clearSetTargetRpe(templateExerciseId: String, at: Long): Int

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
