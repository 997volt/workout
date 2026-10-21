package com.example.androidapp.data

import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

/**
 * The import helper's copy of the seeded library, kept equal to the app's (B176).
 *
 * The tool maps an exercise name to a permanent slug, and it cannot do that without knowing the library:
 * before this, an unloaded app export meant every name became a *new* exercise, so a correct import split
 * a lifter's history from the seeded movements it belongs to. The seeded library is app content, so it can
 * travel with the tool — but only if the two cannot drift, which is what this test is for.
 *
 * It **writes the expected file to `build/` and then compares**, rather than only comparing: a seed change
 * has to be followed by a regeneration, and a failure that prints no diff would leave the next person
 * editing 126 rows by hand. The fix is a copy:
 *
 *     cp app/build/seed-catalog-expected.js docs/seed-catalog.js
 */
class SeedCatalogTest {

    /** The `ExerciseDto` fields the tool reads, in one stable order. */
    @Serializable
    private data class SeedRow(
        val id: String,
        val name: String,
        val primaryMuscle: String?,
        val secondaryMuscles: List<String>,
        val equipment: String?,
        val movementPattern: String?,
        val parentId: String?,
        val rowKind: String,
        val createdAt: Long,
        val updatedAt: Long,
    )

    private val json = Json { prettyPrint = false }

    private fun rows(): List<SeedRow> {
        // Categories first, because a movement's parent has to be a row the file also holds (N95).
        val categories = SeedExercises.categories.map { category ->
            SeedRow(
                id = category.id,
                name = category.name,
                primaryMuscle = category.primaryMuscle.name,
                secondaryMuscles = category.secondaryMuscles.map { it.name },
                equipment = null,
                movementPattern = category.pattern.name,
                parentId = null,
                rowKind = RowKind.CATEGORY.name,
                createdAt = 0L,
                updatedAt = 0L,
            )
        }
        val movements = SeedExercises.all.map { exercise ->
            SeedRow(
                id = exercise.id,
                name = exercise.name,
                primaryMuscle = exercise.primaryMuscle?.name,
                secondaryMuscles = exercise.secondaryMuscles.map { it.name },
                equipment = exercise.equipment?.name,
                movementPattern = exercise.movementPattern?.name,
                parentId = SeedExercises.parentOf[exercise.id],
                rowKind = exercise.rowKind.name,
                createdAt = 0L,
                updatedAt = 0L,
            )
        }
        return categories + movements
    }

    private fun expectedFile(): String {
        val body = rows().joinToString(",\n") { "  " + json.encodeToString(it) }
        return """
            |/* The exercise library the app ships, for name-to-slug mapping without an export.
            | *
            | * GENERATED from app/src/main/java/com/example/androidapp/data/SeedExercises.kt — do not edit.
            | * SeedCatalogTest compares this file to the seed and writes the expected copy to
            | * build/seed-catalog-expected.js when they disagree; copy that over this file to regenerate.
            | */
            |window.SEEDED_EXERCISES = [
            |$body
            |];
            |
        """.trimMargin()
    }

    @Test
    fun theToolsSeededLibraryIsTheAppsSeed() {
        val expected = expectedFile()
        val generated = File(repoRoot, "build/seed-catalog-expected.js")
        generated.parentFile.mkdirs()
        generated.writeText(expected)

        val committed = File(repoRoot, "docs/seed-catalog.js")
        if (!committed.isFile) {
            throw AssertionError(
                "docs/seed-catalog.js is missing; copy build/seed-catalog-expected.js over it.",
            )
        }
        assertThat(committed.readText()).isEqualTo(expected)
    }
}
