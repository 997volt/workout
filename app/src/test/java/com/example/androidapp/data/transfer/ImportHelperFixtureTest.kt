package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import java.io.File

/**
 * The import helper's output, read by the app's own decoder (ROADMAP N160).
 *
 * The helper and the app are two programs that speak one format, and until this test nothing crossed
 * the seam: the tool asserted against its own expectations, so a `schemaVersion` bump or a field whose
 * meaning moved would break the import with the app's generic "that does not look like a backup file"
 * and no build would notice. The fixture is **synthetic and produced by the tool itself**
 * (`node docs/fixtures/generate.js`), so it is exactly what `docs/` emits rather than a hand-copied
 * shape that can drift from both sides.
 *
 * What is asserted is the **contract the merge depends on**, not the fixture's story: the four
 * collections and nothing else, finished sessions, whole grams and half-points, references that
 * resolve, and ids the merge can key on. A regeneration that changes any of those is a deliberate
 * format change and this test is where it has to be argued.
 */
class ImportHelperFixtureTest {

    private val fixture = File(repoRoot, "docs/fixtures/import-helper-backup.json")

    private val text: String by lazy { fixture.readText() }

    @Test
    fun the_file_carries_the_four_collections_and_nothing_else() {
        // A generated `goals`, `programs` or `measurements` block would be data the tool has no business
        // writing; the app defaults every absent collection to empty (N159).
        val keys = Json.parseToJsonElement(text).jsonObject.keys

        assertThat(keys).containsExactly(
            "schemaVersion",
            "exportedAt",
            "exercises",
            "sessions",
            "sessionExercises",
            "sets",
        )
    }

    @Test
    fun the_backup_decodes_and_is_the_fixtures_shape() {
        val file = BackupCodec.decode(text)

        assertThat(file.schemaVersion).isEqualTo(BackupCodec.CURRENT_SCHEMA_VERSION)
        assertThat(file.sessions).hasSize(2)
        assertThat(file.sessionExercises).hasSize(3)
        assertThat(file.sets).hasSize(6)
        assertThat(file.exercises).hasSize(3)
    }

    @Test
    fun every_session_is_finished_and_carries_its_own_zone() {
        // `finishedAt IS NULL` is the app's definition of the open workout, so a generated session
        // without one would be picked up as a session in progress (WorkoutDao).
        BackupCodec.decode(text).sessions.forEach { session ->
            assertThat(session.finishedAt).isNotNull()
            assertThat(session.finishedAt!!).isGreaterThan(session.startedAt)
            assertThat(session.zoneOffsetMinutes).isNotNull()
            assertThat(session.createdAt).isEqualTo(session.startedAt)
            assertThat(session.updatedAt).isEqualTo(session.startedAt)
            assertThat(session.deletedAt).isNull()
        }
    }

    @Test
    fun weights_are_whole_grams_and_rpe_is_half_points() {
        val sets = BackupCodec.decode(text).sets

        sets.forEach { set ->
            assertThat(set.weightGrams).isAtLeast(0L)
            assertThat(set.setIndex).isAtLeast(1)
            assertThat(set.deletedAt).isNull()
            set.rpeHalves?.let { halves ->
                assertThat(halves).isAtLeast(0)
                assertThat(halves).isAtMost(20)
            }
        }
        // 150 lb is stored in grams, not in the unit it was typed in.
        assertThat(sets.map { it.weightGrams }).contains(68039L)
        // RPE 8 and 8.5 travel as 16 and 17.
        assertThat(sets.mapNotNull { it.rpeHalves }).containsAtLeast(16, 17)
        // A warm-up is a role, so the app's record scan excludes it.
        assertThat(sets.map { it.setType }).contains(SetType.WARMUP)
    }

    @Test
    fun positions_and_set_indices_are_contiguous_from_one() {
        val file = BackupCodec.decode(text)

        file.sessionExercises.groupBy { it.sessionId }.forEach { (_, rows) ->
            assertThat(rows.map { it.position }.sorted()).isEqualTo((1..rows.size).toList())
        }
        file.sets.groupBy { it.sessionExerciseId }.forEach { (_, rows) ->
            assertThat(rows.map { it.setIndex }.sorted()).isEqualTo((1..rows.size).toList())
        }
    }

    @Test
    fun every_reference_resolves_inside_the_file() {
        // A dangling reference is the failure the app reports as a storage error and rolls back, so the
        // fixture must never contain one by accident (RoomBackupRepository, one transaction).
        val file = BackupCodec.decode(text)
        val exerciseIds = file.exercises.mapTo(mutableSetOf()) { it.id }
        val sessionIds = file.sessions.mapTo(mutableSetOf()) { it.id }
        val sessionExerciseIds = file.sessionExercises.mapTo(mutableSetOf()) { it.id }

        file.sessionExercises.forEach { row ->
            assertThat(row.sessionId).isIn(sessionIds)
            assertThat(row.exerciseId).isIn(exerciseIds)
        }
        file.sets.forEach { set ->
            assertThat(set.sessionExerciseId).isIn(sessionExerciseIds)
        }
    }

    @Test
    fun every_id_is_the_tools_and_no_id_repeats() {
        // Deterministic `imp_` ids are what make a re-import a no-op (N159); a duplicate is a row that
        // would silently vanish into another.
        val file = BackupCodec.decode(text)
        val ids = file.exercises.map { it.id } +
            file.sessions.map { it.id } +
            file.sessionExercises.map { it.id } +
            file.sets.map { it.id }

        assertThat(ids).isNotEmpty()
        assertThat(ids).containsNoDuplicates()
        assertThat(ids.filterNot { it.startsWith("imp_") }).isEmpty()
    }

    @Test
    fun an_exercise_the_library_lacks_states_only_what_it_knows() {
        // The fixture is generated with no library loaded, so every exercise is one the tool created. A
        // row states what it knows and the decoder's own default makes it a movement (N103, N95).
        val exercises = BackupCodec.decode(text).exercises

        exercises.forEach { exercise ->
            assertThat(exercise.name).isNotEmpty()
            assertThat(exercise.primaryMuscle).isNull()
            assertThat(exercise.secondaryMuscles).isEmpty()
            assertThat(exercise.equipment).isNull()
            assertThat(exercise.movementPattern).isNull()
            assertThat(exercise.rowKind).isEqualTo(RowKind.MOVEMENT)
            assertThat(exercise.deletedAt).isNull()
        }
        // The pound row's exercise is marked in the unit it is displayed in (N64).
        assertThat(exercises.single { it.name == "Standing Calf Raise" }.weightUnit).isEqualTo("POUNDS")
        assertThat(exercises.filterNot { it.name == "Standing Calf Raise" }.map { it.weightUnit })
            .containsExactly("KILOGRAMS", "KILOGRAMS")
    }
}
