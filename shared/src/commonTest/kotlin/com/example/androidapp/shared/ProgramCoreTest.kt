package com.example.androidapp.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The program-edit core's own tests (ROADMAP N170).
 *
 * They live in `commonTest`, so both targets run them: the JVM suite proves what the app links
 * against, and the node suite proves the same answers reached the page. A rule that only held on one
 * side would fail the other here rather than in the field.
 */
class ProgramCoreTest {

    @Test
    fun the_codec_round_trips_a_document() {
        val document = baseDocument()

        val decoded = ProgramDocumentCodec.decode(ProgramDocumentCodec.encode(document))

        assertEquals(document, decoded)
    }

    @Test
    fun the_codec_refuses_a_newer_document_before_reading_it() {
        val refused = assertFailsWith<ProgramFormatException> {
            ProgramDocumentCodec.decode("""{"formatVersion": 5}""")
        }

        assertEquals(ProblemCode.NEWER_VERSION, refused.problem.code)
    }

    @Test
    fun the_codec_refuses_text_that_names_no_version() {
        val refused = assertFailsWith<ProgramFormatException> {
            ProgramDocumentCodec.decode("""{"program": {}}""")
        }

        assertEquals(ProblemCode.MISSING_VERSION, refused.problem.code)
    }

    @Test
    fun the_codec_calls_a_bad_body_malformed() {
        val refused = assertFailsWith<ProgramFormatException> {
            ProgramDocumentCodec.decode("""{"formatVersion": 4, "exportedAt": "soon"}""")
        }

        assertEquals(ProblemCode.MALFORMED, refused.problem.code)
    }

    @Test
    fun validation_accepts_a_gap_left_by_a_deleted_set() {
        // A deleted set keeps its stored index and re-logging appends at the end (B157), so a live
        // plan legitimately reads 1, 3. Refusing the gap would make every such plan uneditable.
        val document = baseDocument(
            templateSets = listOf(
                setRow("set_a", "te_a", 1),
                setRow("set_b", "te_a", 3),
            ),
        )

        assertTrue(validate(document).isEmpty())
    }

    @Test
    fun validation_refuses_a_slot_whose_workout_is_missing() {
        val document = baseDocument(
            slots = listOf(slotRow("slot_a", "tpl_missing")),
        )

        assertEquals(
            listOf(ProblemCode.DANGLING_SLOT_TEMPLATE),
            validate(document).map { it.code },
        )
    }

    @Test
    fun validation_refuses_a_rung_under_a_warm_up() {
        val document = baseDocument(
            templateSets = listOf(
                setRow("set_warm", "te_a", 1, role = ProgramSetRole.WARMUP),
                setRow("set_drop", "te_a", 2, role = ProgramSetRole.DROP, dropValueGrams = 10_000),
            ),
        )

        assertTrue(validate(document).any { it.code == ProblemCode.INVALID_RUNG })
    }

    @Test
    fun validation_accepts_a_drop_run_that_names_its_value_once() {
        val document = baseDocument(
            templateSets = listOf(
                setRow("set_a", "te_a", 1),
                setRow("set_drop", "te_a", 2, role = ProgramSetRole.DROP, dropValueGrams = 10_000),
                setRow("set_drop2", "te_a", 3, role = ProgramSetRole.DROP),
            ),
        )

        assertTrue(validate(document).isEmpty())
    }

    @Test
    fun an_edit_plans_the_renames_adds_and_removals_it_names() {
        val snapshot = baseDocument()
        val edit = baseDocument(
            program = programRow(name = "Autumn block"),
            slots = listOf(
                slotRow("slot_a", "tpl_a"),
                slotRow("slot_b", "tpl_b", position = 1, weekday = Weekday.FRIDAY),
            ),
            templates = listOf(templateRow("tpl_a", "Upper A"), templateRow("tpl_b", "Lower B")),
            templateExercises = listOf(
                plannedExerciseRow("te_a", "tpl_a", "ex_a"),
                plannedExerciseRow("te_b", "tpl_b", "ex_a"),
            ),
            templateSets = listOf(
                setRow("set_a", "te_a", 1),
                setRow("set_b", "te_b", 1),
            ),
            edit = EditMarker(baseExportedAt = T),
        )

        val ready = plan(snapshot, edit) as PlanResult.Ready

        assertEquals(
            listOf(
                ChangeKind.PROGRAM_RENAMED,
                ChangeKind.SLOT_ADDED,
                ChangeKind.TEMPLATE_ADDED,
                ChangeKind.TEMPLATE_EXERCISE_ADDED,
                ChangeKind.SET_ADDED,
            ),
            ready.changes.map { it.kind },
        )
    }

    @Test
    fun a_removed_day_keeps_its_workout_and_says_so() {
        val snapshot = baseDocument()
        val edit = baseDocument(slots = emptyList(), edit = EditMarker(baseExportedAt = T))

        val ready = plan(snapshot, edit) as PlanResult.Ready

        assertEquals(listOf(ChangeKind.SLOT_REMOVED), ready.changes.map { it.kind })
        assertEquals(listOf(WarningCode.WORKOUT_KEPT), ready.warnings.map { it.code })
    }

    @Test
    fun an_edit_of_a_program_that_moved_since_the_export_is_refused() {
        val snapshot = baseDocument(
            templates = listOf(templateRow("tpl_a", "Upper A", updatedAt = T + 10)),
        )
        val edit = baseDocument(edit = EditMarker(baseExportedAt = T))

        val refused = plan(snapshot, edit) as PlanResult.Refused

        assertEquals(ProblemCode.STALE_BASE, refused.problems.single().code)
    }

    @Test
    fun a_document_without_an_edit_marker_is_refused_as_an_edit() {
        val refused = plan(baseDocument(), baseDocument()) as PlanResult.Refused

        assertEquals(ProblemCode.NO_EDIT_MARKER, refused.problems.single().code)
    }

    @Test
    fun a_library_row_is_never_rewritten_by_an_edit() {
        val snapshot = baseDocument(exercises = listOf(exerciseRow("ex_a", "Bench Press")))
        val edit = baseDocument(
            exercises = listOf(exerciseRow("ex_a", "Bench Press (renamed)")),
            edit = EditMarker(baseExportedAt = T),
        )

        val ready = plan(snapshot, edit) as PlanResult.Ready

        assertFalse(ready.changes.any { it.kind == ChangeKind.EXERCISE_ADDED })
        assertFalse(ready.changes.any { it.id == "ex_a" })
    }

    @Test
    fun a_new_movement_is_planned_with_its_definition_and_warned_about() {
        val snapshot = baseDocument()
        val edit = baseDocument(
            templateExercises = listOf(plannedExerciseRow("te_a", "tpl_a", "ex_new")),
            templateSets = listOf(setRow("set_a", "te_a", 1)),
            exercises = listOf(exerciseRow("ex_a", "Bench Press"), exerciseRow("ex_new", "Machine Curl")),
            edit = EditMarker(baseExportedAt = T),
        )

        val ready = plan(snapshot, edit) as PlanResult.Ready

        assertTrue(ready.changes.any { it.kind == ChangeKind.EXERCISE_ADDED })
        assertTrue(ready.warnings.any { it.code == WarningCode.NEW_MOVEMENT })
    }

    @Test
    fun the_page_door_returns_the_same_decision_as_the_kotlin_call() {
        val snapshot = baseDocument()
        val edit = baseDocument(
            program = programRow(name = "Autumn block"),
            edit = EditMarker(baseExportedAt = T),
        )

        val output = planJson(
            ProgramDocumentCodec.encode(snapshot),
            ProgramDocumentCodec.encode(edit),
        )

        assertTrue(output.contains("\"ok\":true"))
        assertTrue(output.contains("PROGRAM_RENAMED"))
    }

    @Test
    fun the_page_door_reports_a_refusal_as_a_problem() {
        val output = planJson(
            ProgramDocumentCodec.encode(baseDocument()),
            ProgramDocumentCodec.encode(baseDocument()),
        )

        assertTrue(output.contains("\"ok\":false"))
        assertTrue(output.contains("NO_EDIT_MARKER"))
    }
}

internal const val T: Long = 1_000L

internal fun programRow(name: String = "Block") = ProgramDto(
    id = "pgm_1",
    name = name,
    createdAt = T,
    updatedAt = T,
)

internal fun slotRow(
    id: String,
    templateId: String,
    position: Int = 0,
    weekday: Weekday? = Weekday.MONDAY,
    updatedAt: Long = T,
) = ProgramSlotDto(
    id = id,
    programId = "pgm_1",
    templateId = templateId,
    position = position,
    weekday = weekday,
    createdAt = T,
    updatedAt = updatedAt,
)

internal fun templateRow(id: String, name: String, updatedAt: Long = T) = TemplateDto(
    id = id,
    name = name,
    createdAt = T,
    updatedAt = updatedAt,
)

internal fun exerciseRow(id: String, name: String) = ExerciseDto(
    id = id,
    name = name,
    primaryMuscle = "CHEST",
    secondaryMuscles = listOf("TRICEPS"),
    equipment = "BARBELL",
    movementPattern = "PRESS",
    createdAt = T,
    updatedAt = T,
)

internal fun plannedExerciseRow(
    id: String,
    templateId: String,
    exerciseId: String,
    position: Int = 0,
) = TemplateExerciseDto(
    id = id,
    templateId = templateId,
    exerciseId = exerciseId,
    position = position,
    createdAt = T,
    updatedAt = T,
)

internal fun setRow(
    id: String,
    templateExerciseId: String,
    setIndex: Int,
    role: ProgramSetRole = ProgramSetRole.NORMAL,
    dropValueGrams: Long? = null,
) = TemplateSetDto(
    id = id,
    templateExerciseId = templateExerciseId,
    setIndex = setIndex,
    role = role,
    targetWeightGrams = if (role.isRung) null else 80_000L,
    targetRepsMin = if (role.isRung) null else 5,
    targetRepsMax = if (role.isRung) null else 5,
    dropValueGrams = dropValueGrams,
    createdAt = T,
    updatedAt = T,
)

internal fun baseDocument(
    program: ProgramDto = programRow(),
    slots: List<ProgramSlotDto> = listOf(slotRow("slot_a", "tpl_a")),
    templates: List<TemplateDto> = listOf(templateRow("tpl_a", "Upper A")),
    templateExercises: List<TemplateExerciseDto> = listOf(plannedExerciseRow("te_a", "tpl_a", "ex_a")),
    templateSets: List<TemplateSetDto> = listOf(setRow("set_a", "te_a", 1)),
    exercises: List<ExerciseDto> = listOf(exerciseRow("ex_a", "Bench Press")),
    edit: EditMarker? = null,
) = ProgramDocument(
    formatVersion = PROGRAM_FORMAT_VERSION,
    exportedAt = T,
    program = program,
    slots = slots,
    templates = templates,
    templateExercises = templateExercises,
    templateSets = templateSets,
    exercises = exercises,
    edit = edit,
)
