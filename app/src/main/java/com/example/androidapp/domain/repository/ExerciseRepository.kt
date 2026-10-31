package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Read and write access to the exercise library.
 *
 * The UI depends only on this interface, so the in-memory implementation
 * shipped today can be swapped for a Room-backed one (ROADMAP F5) without
 * touching a single composable or ViewModel.
 */
interface ExerciseRepository {

    /**
     * Observes the whole library, re-emitting whenever it changes.
     *
     * A [DataResult] rather than a bare list (ROADMAP B4): a database failure used
     * to escape this flow and take the screen down, and a failure the UI can render
     * beats an exception that disappears inside a coroutine. The same argument F7
     * made for the writes, applied to the two reads that were still exempt.
     */
    fun observeExercises(): Flow<DataResult<List<Exercise>>>

    /**
     * The exercise with [id], as a value: `Success(null)` means it does not exist,
     * `Failure` means the read itself failed. The two used to be indistinguishable.
     */
    suspend fun getExercise(id: String): DataResult<Exercise?>

    /**
     * Every row of the library, **removed ones included**, for resolving the name of a head (ROADMAP N95).
     *
     * One question, and it is a naming one: a deleted head still names its children, so the name has to be
     * reachable once the row stops being offered. This is deliberately not the observer the screens read —
     * that one hides removed rows, because a list must not offer them — and deliberately not a flow, because
     * it is consulted rather than watched.
     */
    suspend fun getAllIncludingDeleted(): DataResult<List<Exercise>>

    /**
     * When each movement was last trained, keyed by movement id (ROADMAP N230).
     *
     * The newest **finished** session that names it, and nothing for a movement never trained — which is a
     * fact the library sorts by and a filter counts, and the detail screen's history reads (N233).
     */
    fun observeLastTrained(): Flow<DataResult<Map<String, Instant>>>

    /**
     * Stores a new custom exercise named [name] and returns it (ROADMAP N2).
     *
     * Creation deliberately asks for the name only: the taxonomy fields are
     * non-nullable on disk, so an unedited custom exercise is stored as
     * unspecified (`OTHER`) and can be filled in later from the detail screen.
     * A UUID id is what makes it a user-created entry rather than a seeded one,
     * whose ids are permanent slugs.
     */
    suspend fun createCustomExercise(name: String): DataResult<Exercise>

    /**
     * Stores a new **category** head named [name] and returns it (ROADMAP N95).
     *
     * A category is a full row of the library — named, editable, soft-deletable and carried by both
     * transfer formats — so it is created by the same path as a custom exercise and differs only in
     * [com.example.androidapp.domain.model.RowKind]. That is the point of the row kind rather than a table
     * of its own: the two kinds share every rule except whether they may be offered by a picker.
     *
     * Its taxonomy columns are stored as unspecified for [createCustomExercise]'s reason, and the equipment
     * placeholder is deliberate — a family spans equipment, and every child sets its own.
     */
    suspend fun createCategory(name: String): DataResult<Exercise>

    /**
     * Saves the editable attributes of an existing exercise (ROADMAP N2).
     *
     * **This is also how a row is filed.** [Exercise.parentId] is one of those attributes, so *move to
     * category* is this same write rather than a flow of its own; two methods for one row edit would be two
     * places to keep the validation in, and a screen that changes a category already holds the row.
     *
     * Fails with [com.example.androidapp.domain.DataError.NotFound] when [exercise]
     * no longer exists, so a stale detail screen cannot silently write nothing.
     */
    suspend fun updateExercise(exercise: Exercise): DataResult<Unit>

    /**
     * Merges [sourceId] into [survivorId] (ROADMAP N245).
     *
     * The deliberate twin of the automatic repair: sessions and plans move to the survivor, the source's own
     * settings fill the survivor's gaps, the retired id is recorded and the source row is deleted. The repair
     * deliberately leaves a row the lifter configured alone; this is how they settle it without deleting a
     * row's history.
     *
     * Both ids must name live movements; a category, a missing row or a merge into itself is refused.
     */
    suspend fun mergeExercises(sourceId: String, survivorId: String): DataResult<Unit>
}
