package com.example.androidapp.data.transfer

import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.shared.EditMarker
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.time.DayOfWeek

/**
 * A program document as a copy (ROADMAP N243).
 *
 * The rule the load needed: a document whose id is already here cannot be merged, so it can only be copied —
 * and a copy is only a *second* program when its rows are second rows. What matters here is that every id is
 * minted, every reference follows the id it names, and nothing the document does not describe is touched.
 */
class ProgramDocumentCopyTest {

    private val document = ProgramDocument(
        formatVersion = 1,
        exportedAt = 1_000L,
        program = ProgramDto(
            id = "p1",
            name = "Push Pull Legs",
            isActive = true,
            position = 4,
            createdAt = 1L,
            updatedAt = 2L,
        ),
        slots = listOf(
            slot("sl1", "t1", DayOfWeek.MONDAY, position = 0),
            slot("sl2", "t2", DayOfWeek.TUESDAY, position = 1),
        ),
        templates = listOf(
            template("t1", "Push", versionOf = "v9"),
            template("t2", "Pull"),
            // A workout the document carries but no slot names: still the copy's own row.
            template("t3", "Legs"),
        ),
        templateExercises = listOf(
            planned("pe1", templateId = "t1", exerciseId = "barbell-bench-press", position = 0),
            planned("pe2", templateId = "t2", exerciseId = "barbell-row", position = 0),
        ),
        templateSets = listOf(
            set("s1", plannedId = "pe1", setIndex = 0),
            set("s2", plannedId = "pe2", setIndex = 0),
        ),
        exercises = listOf(exercise("barbell-bench-press", "Barbell Bench Press")),
        edit = EditMarker(baseExportedAt = 7L),
    )

    @Test
    fun theProgram_getsAnIdOfItsOwn_andTheNameTheLifterChose() {
        val copy = copyOf(document)

        assertThat(copy.program.id).isNotEqualTo("p1")
        assertThat(copy.program.name).isEqualTo("Push Pull Legs (2)")
        assertThat(copy.program.isActive).isFalse()
        assertThat(copy.program.position).isEqualTo(0)
    }

    @Test
    fun theEditMarker_isGone_becauseACopyIsAnAdd() {
        // The marker is what turns a load into a proposal to change a program this device holds (N171); a copy
        // is the other kind of document entirely.
        assertThat(copyOf(document).edit).isNull()
    }

    @Test
    fun everyWorkoutIsItsOwnRow_andEveryReferenceFollowsIt() {
        val copy = copyOf(document)
        val originalIds = document.templates.map { it.id }.toSet()
        val copyIds = copy.templates.map { it.id }.toSet()

        assertThat(copy.templates).hasSize(3)
        assertWithMessage("not one template keeps its id").that(copyIds.intersect(originalIds)).isEmpty()
        // Every slot names the copy's program and one of the copy's own workouts.
        copy.slots.forEach { slot ->
            assertThat(slot.programId).isEqualTo(copy.program.id)
            assertThat(slot.templateId).isIn(copyIds)
        }
    }

    @Test
    fun thePlannedWorkFollowsTheWorkoutsItBelongsTo() {
        val copy = copyOf(document)
        val copyTemplateIds = copy.templates.map { it.id }.toSet()
        val copyPlannedIds = copy.templateExercises.map { it.id }.toSet()

        assertWithMessage("every planned exercise keeps its movement and gains its workout")
            .that(copy.templateExercises.map { it.templateId }.all { it in copyTemplateIds })
            .isTrue()
        assertThat(copy.templateExercises.map { it.exerciseId })
            .containsExactly("barbell-bench-press", "barbell-row")
        assertWithMessage("and every set follows the planned exercise it belongs to")
            .that(copy.templateSets.map { it.templateExerciseId }.all { it in copyPlannedIds })
            .isTrue()
        assertThat(copy.templateSets.map { it.id }).containsNoneIn(document.templateSets.map { it.id })
    }

    @Test
    fun theLibrarysMovements_areNotCopied() {
        // A movement a document carries is the same movement: the merge adds what the device does not have,
        // and copying it would put a second *Barbell Bench Press* in the library.
        assertThat(copyOf(document).exercises).isEqualTo(document.exercises)
    }

    @Test
    fun aCopiedWorkout_startsItsOwnLineage() {
        // `versionOf` is what a program document's staleness is measured against (B287), and a copy is a new
        // definition rather than a version of the one it came from.
        assertThat(copyOf(document).templates.map { it.versionOf }).containsExactly(null, null, null)
    }

    @Test
    fun theOriginal_isUntouched() {
        copyOf(document)

        assertThat(document.program.id).isEqualTo("p1")
        assertThat(document.templates.map { it.id }).containsExactly("t1", "t2", "t3")
    }

    @Test
    fun aSlotNamingAWorkout_theDocumentDoesNotCarry_keepsThatReference() {
        // A document may name a workout the device holds without describing it, and the merge keeps that slot.
        val shared = document.copy(slots = listOf(slot("sl1", "device-workout", DayOfWeek.FRIDAY, position = 0)))

        assertThat(copyOf(shared).slots.single().templateId).isEqualTo("device-workout")
    }

    @Test
    fun plannedWorkWhoseWorkoutTheDocumentDoesNotCarry_isDropped() {
        // B302: a planned exercise used to fall back to the document's own template id, which for the original
        // is a live device workout — so the "copy" wrote rows into the program it was copied from.
        val dangling = document.copy(
            templateExercises = document.templateExercises + planned(
                "pe3",
                templateId = "device-workout",
                exerciseId = "barbell-row",
                position = 1,
            ),
            templateSets = document.templateSets + set("s3", plannedId = "pe3", setIndex = 0),
        )

        val copy = copyOf(dangling)
        val copyTemplateIds = copy.templates.map { it.id }.toSet()
        val copyPlannedIds = copy.templateExercises.map { it.id }.toSet()

        assertThat(copy.templateExercises).hasSize(2)
        assertThat(copy.templateSets).hasSize(2)
        assertWithMessage("no planned exercise hangs off a workout the copy does not hold")
            .that(copy.templateExercises.map { it.templateId }.all { it in copyTemplateIds })
            .isTrue()
        assertWithMessage("and every set follows a planned exercise the copy holds")
            .that(copy.templateSets.map { it.templateExerciseId }.all { it in copyPlannedIds })
            .isTrue()
    }

    /** Minting ids in order, so a test can read which row got which. */
    private fun copyOf(original: ProgramDocument): ProgramDocument {
        var next = 0
        return programDocumentCopy(original, "Push Pull Legs (2)") { "new-${next++}" }
    }

    private fun slot(id: String, templateId: String, weekday: DayOfWeek?, position: Int) = ProgramSlotDto(
        id = id,
        programId = "p1",
        templateId = templateId,
        position = position,
        weekday = weekday,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun template(id: String, name: String, versionOf: String? = null) = TemplateDto(
        id = id,
        name = name,
        versionOf = versionOf,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun planned(id: String, templateId: String, exerciseId: String, position: Int) =
        TemplateExerciseDto(
            id = id,
            templateId = templateId,
            exerciseId = exerciseId,
            position = position,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun set(id: String, plannedId: String, setIndex: Int) = TemplateSetDto(
        id = id,
        templateExerciseId = plannedId,
        setIndex = setIndex,
        role = SetType.NORMAL,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun exercise(id: String, name: String) = ExerciseDto(
        id = id,
        name = name,
        primaryMuscle = null,
        secondaryMuscles = emptyList(),
        equipment = null,
        movementPattern = null,
        createdAt = 1L,
        updatedAt = 1L,
        parentId = null,
        rowKind = RowKind.MOVEMENT,
    )
}
