package com.example.androidapp.data

import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.data.local.toEntity
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.toDataError
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.ExerciseRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Room-backed [ExerciseRepository] (ROADMAP F5).
 *
 * This replaced `InMemoryExerciseRepository` without a single change to a
 * composable or a ViewModel, which is what the interface was for.
 *
 * Mapping to the domain type at this boundary means the soft-delete filter and
 * the sync columns stay entirely inside the data layer.
 *
 * Writes go through [dataResultOf] for the same reason the workout repository's
 * do (F7): a failed library write is a value the screen can report, not an
 * exception that disappears inside a coroutine.
 */
@Singleton
class RoomExerciseRepository @Inject constructor(
    database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : ExerciseRepository {

    private val dao = database.exerciseDao()

    override fun observeExercises(): Flow<DataResult<List<Exercise>>> =
        dao.observeAll()
            .map<List<ExerciseEntity>, DataResult<List<Exercise>>> { rows ->
                DataResult.Success(rows.map { it.toDomain() })
            }
            // A Room failure arrives here rather than at a call site, which is
            // exactly why it used to be invisible (ROADMAP B4).
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                emit(DataResult.Failure(throwable.toDataError()))
            }

    override suspend fun getExercise(id: String): DataResult<Exercise?> = dataResultOf {
        dao.findById(id)?.toDomain()
    }

    override suspend fun createCustomExercise(name: String): DataResult<Exercise> = dataResultOf {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidInputException("Give the exercise a name.")

        // The taxonomy is stored as unspecified rather than left null, because
        // the columns are non-nullable. Enums are stored by name, so these values
        // need no migration — which is what lets this land before the N3/N4-N8
        // schema decision (ROADMAP N2).
        val exercise = Exercise(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            primaryMuscle = MuscleGroup.OTHER,
            secondaryMuscles = emptyList(),
            equipment = Equipment.OTHER,
            movementPattern = MovementPattern.OTHER,
            isCustom = true,
        )
        dao.insert(exercise.toEntity(now = timeSource.nowEpochMillis()))
        exercise
    }

    override suspend fun updateExercise(exercise: Exercise): DataResult<Unit> = dataResultOf {
        val trimmed = exercise.name.trim()
        if (trimmed.isEmpty()) throw InvalidInputException("Give the exercise a name.")
        // Zero is a value — "this exercise has no rest" — and leaving it unset is how "use the app
        // default" is expressed, so only a negative is input we cannot use (ROADMAP N5, N45).
        if (exercise.restSeconds != null && exercise.restSeconds < RestTimer.MIN_PRESCRIBED_SECONDS) {
            throw InvalidInputException(RestTimer.NEGATIVE_REST_REFUSAL)
        }
        // A step of zero is not a small step, it is no step at all: the ± buttons would do nothing and
        // the warm-up ramp divides by it (ROADMAP N77). Null stays "the unit's own".
        if (exercise.stepGrams != null && exercise.stepGrams <= 0L) {
            throw InvalidInputException("A weight step must be more than zero.")
        }

        // Read the stored row first. The domain type deliberately carries no
        // createdAt, and the DAO writes every column, so rebuilding from the row
        // is what preserves the original creation time. findById also filters
        // soft-deleted rows, which is what stops an edit resurrecting one.
        val stored = dao.findById(exercise.id)
            ?: throw NotFoundException("exercise ${exercise.id}")

        val updated = stored.copy(
            name = trimmed,
            primaryMuscle = exercise.primaryMuscle,
            secondaryMuscles = exercise.secondaryMuscles,
            equipment = exercise.equipment,
            movementPattern = exercise.movementPattern,
            restSeconds = exercise.restSeconds,
            // A cleared cue is stored as null, not as an empty string: two
            // representations of "nothing" would show up differently on screen.
            techniqueNote = exercise.techniqueNote?.trim()?.ifEmpty { null },
            // The exercise's own display unit, by name, or null for "follow the app" (ROADMAP N64).
            // Rebuilt rows write every column, so leaving this out silently discarded the choice the
            // form had just made — the screen showed the new value from its own state while the row
            // kept null, and the unit reverted on the next read.
            weightUnit = exercise.weightUnit?.name,
            // The exercise's own weight step in grams, or null for the unit's own (ROADMAP N77).
            stepGrams = exercise.stepGrams,
            updatedAt = timeSource.nowEpochMillis(),
        )
        if (dao.update(updated) == 0) throw NotFoundException("exercise ${exercise.id}")
    }
}
