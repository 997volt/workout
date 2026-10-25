package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A plan's planned sets (ROADMAP N3, N14).
 *
 * Split out of [TemplateDao] at schema 9 so that interface could add the hidden flag's write without
 * outgrowing the function ceiling: a plan's sets are their own half of the aggregate — every read is
 * ordered by the plan's order, the undo is the one reader that may see a soft-deleted row, and none of
 * it needs the template row itself. The two DAOs share one database and one transaction.
 */
@Dao
interface TemplateSetDao {

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
     * One planned set **whatever its `deletedAt` carries**, with the plan it belongs to (ROADMAP N90).
     *
     * The live queries deliberately hide a soft-deleted row, and a restore is the one reader that exists
     * to find it: the row and its index are still on disk, so putting it back is a question about a row
     * nothing else may see. The plan travels with it because the rows that share the write's own
     * timestamp — the run that went with the set, or its survivors, all of them need renumbering — are
     * read back by template, and a hidden row can no longer be traced to one through the live joins.
     */
    @Query(
        """
        SELECT s.id, s.templateExerciseId, s.deletedAt, e.templateId AS owningTemplateId
        FROM template_sets s
        JOIN template_exercises e ON e.id = s.templateExerciseId
        WHERE s.id = :id
        """,
    )
    suspend fun findTemplateSetWithPlan(id: String): RemovedTemplateSet?

    /**
     * Every planned set of one template, **including the soft-deleted ones** (ROADMAP N90).
     *
     * The one query that may see hidden rows, and it exists for the restore: the rows a removal hid are
     * the rows it puts back, and their stored indexes are the order it re-derives. Everything else goes
     * through [findTemplateSets], which hides them.
     */
    @Query(
        """
        SELECT s.* FROM template_sets s
        JOIN template_exercises e ON e.id = s.templateExerciseId
        WHERE e.templateId = :templateId
        ORDER BY s.setIndex ASC
        """,
    )
    suspend fun findTemplateSetsIncludingDeleted(templateId: String): List<TemplateSetEntity>

    /** The three facts an undo reads off a removed planned set, and the plan it belongs to (ROADMAP N90). */
    data class RemovedTemplateSet(
        val id: String,
        val templateExerciseId: String,
        val deletedAt: Long?,
        val owningTemplateId: String,
    )

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
}
