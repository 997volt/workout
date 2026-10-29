package com.example.androidapp.data.transfer

import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the backup file format (ROADMAP P1.12).
 *
 * The format's whole job is a faithful round trip, so that is what is asserted —
 * including the fields that are easy to forget, like soft-delete timestamps and
 * the secondary-muscle list.
 */
class BackupCodecTest {

    private val sample = BackupFile(
        schemaVersion = BackupCodec.CURRENT_SCHEMA_VERSION,
        exportedAt = 1_790_000_000_000L,
        exercises = listOf(
            ExerciseDto(
                id = "back-squat",
                name = "Back Squat",
                primaryMuscle = MuscleGroup.QUADS,
                secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE),
                equipment = Equipment.BARBELL,
                movementPattern = MovementPattern.SQUAT,
                createdAt = 1L,
                updatedAt = 2L,
                deletedAt = null,
                restSeconds = 180,
                techniqueNote = "Brace, sit back",
            ),
            // A soft-deleted row: the backup must carry it, not quietly drop it.
            ExerciseDto(
                id = "gone",
                name = "Removed Lift",
                primaryMuscle = MuscleGroup.LOWER_BACK,
                secondaryMuscles = emptyList(),
                equipment = Equipment.CABLE,
                movementPattern = MovementPattern.PULL,
                createdAt = 3L,
                updatedAt = 4L,
                deletedAt = 5L,
            ),
        ),
        sessions = listOf(
            SessionDto(
                id = "s1",
                startedAt = 10L,
                finishedAt = 20L,
                notes = "felt heavy",
                readinessNote = "Slept badly, legs heavy",
                createdAt = 10L,
                updatedAt = 20L,
                deletedAt = null,
            ),
        ),
        sessionExercises = listOf(
            SessionExerciseDto(
                id = "se1",
                sessionId = "s1",
                exerciseId = "back-squat",
                position = 0,
                finishedAt = 12L,
                muscleFeel = 8,
                createdAt = 10L,
                updatedAt = 12L,
                deletedAt = null,
            ),
        ),
        // The joints each exercise reported painful (ROADMAP N63): a fact the lifter wrote, so the
        // hand-written codec must name it or an export loses it in silence.
        sessionExerciseJoints = listOf(
            SessionExerciseJointDto(
                id = "sej1",
                sessionExerciseId = "se1",
                joint = Joint.KNEE,
                side = Side.LEFT,
                score = 6,
                position = 0,
                createdAt = 12L,
                updatedAt = 12L,
                deletedAt = null,
            ),
        ),
        sets = listOf(
            SetDto(
                id = "set1",
                sessionExerciseId = "se1",
                setIndex = 0,
                reps = 5,
                weightGrams = 100_000,
                // A role that records an effort, which is what the RPE below belongs to: since N67 a
                // warm-up carries none, so a fixture that gave one an RPE would model a file this app
                // can no longer produce.
                setType = SetType.NORMAL,
                rpeHalves = 19,
                note = "Felt heavy",
                createdAt = 11L,
                updatedAt = 11L,
                deletedAt = null,
            ),
        ),
        // A template is the user's plan, so the escape hatch has to carry it (N3),
        // soft-deleted rows included.
        templates = listOf(
            TemplateDto(
                id = "t1",
                name = "Push day",
                createdAt = 30L,
                updatedAt = 31L,
                deletedAt = null,
            ),
            TemplateDto(
                id = "t2",
                name = "Deleted plan",
                createdAt = 32L,
                updatedAt = 33L,
                deletedAt = 34L,
            ),
        ),
        templateExercises = listOf(
            TemplateExerciseDto(
                id = "te1",
                templateId = "t1",
                exerciseId = "back-squat",
                position = 0,
                restSeconds = 180,
                techniqueNote = "Slow descent, no sinking",
                createdAt = 30L,
                updatedAt = 30L,
                deletedAt = null,
            ),
        ),
        // A plan's sets are the plan (ROADMAP N14). This sample is what proves the
        // codec carries them: a field the DTO does not name is dropped in silence.
        templateSets = listOf(
            TemplateSetDto(
                id = "ts1",
                templateExerciseId = "te1",
                setIndex = 0,
                role = SetType.TOP_SET,
                targetWeightGrams = 140_000L,
                targetRepsMin = 1,
                targetRepsMax = 2,
                note = "grind",
                createdAt = 30L,
                updatedAt = 30L,
                deletedAt = null,
            ),
        ),
    )

    @Test
    fun encodeThenDecode_returnsExactlyTheSameData() {
        val restored = BackupCodec.decode(BackupCodec.encode(sample))

        assertEquals(sample, restored)
    }

    @Test
    fun enumsAreWrittenByName_notByOrdinal() {
        // An ordinal would be re-interpreted silently if the enum were ever
        // reordered, and the file may outlive several app versions.
        val text = BackupCodec.encode(sample)

        assertTrue("expected the enum name in the file", text.contains("\"QUADS\""))
        // A role that is not the enum's first constant, so an ordinal writer could not match it.
        assertTrue(text.contains("\"TOP_SET\""))
        // A picked joint travels as its names too (ROADMAP N63), left and right apart.
        assertTrue(text.contains("\"KNEE\""))
        assertTrue(text.contains("\"LEFT\""))
    }

    @Test
    fun aFileWrittenBeforeTheJointListExisted_stillDecodes_seeingNone() {
        // ROADMAP N63 added a whole collection, defaulted to empty like every other, so a file that
        // carries none still decodes rather than failing.
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val olderFile = JsonObject(tree - "sessionExerciseJoints")

        val restored = BackupCodec.decode(olderFile.toString())

        assertEquals(emptyList<SessionExerciseJointDto>(), restored.sessionExerciseJoints)
    }

    @Test
    fun anExerciseWrittenBeforeCategoriesExisted_stillDecodes_asAMovement() {
        // ROADMAP N95. The default is the load-bearing part rather than the decoding: every row a file could
        // have held before categories is a movement, so an absent kind has one true meaning — and the
        // opposite default would turn an older file's whole library into heads, which are never offered.
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val olderExercises = tree.getValue("exercises").jsonArray.map { element ->
            JsonObject(element.jsonObject - "rowKind" - "parentId")
        }
        val olderFile = JsonObject(tree + ("exercises" to JsonArray(olderExercises)))

        val restored = BackupCodec.decode(olderFile.toString())

        assertEquals(
            "a file with no kind in it is a library of movements",
            listOf(RowKind.MOVEMENT),
            restored.exercises.map { it.rowKind }.distinct(),
        )
        assertEquals(
            "and none of them is filed under anything",
            listOf(null),
            restored.exercises.map { it.parentId }.distinct(),
        )
    }

    @Test
    fun aSoftDeletedRowSurvivesTheRoundTrip() {
        // Dropping it would restore into a database that differs from the original.
        val restored = BackupCodec.decode(BackupCodec.encode(sample))

        assertEquals(1, restored.exercises.count { it.deletedAt != null })
    }

    @Test
    fun garbageIsRejectedWithAMessageWorthShowing() {
        val thrown = assertThrows(InvalidInputException::class.java) {
            BackupCodec.decode("this is not json")
        }

        assertTrue(
            "the message is user-facing, not a stack trace",
            thrown.message.orEmpty().contains("backup file"),
        )
    }

    @Test
    fun aFileFromANewerAppIsRefused_ratherThanPartiallyRead() {
        val fromTheFuture = BackupCodec.encode(
            sample.copy(schemaVersion = BackupCodec.CURRENT_SCHEMA_VERSION + 1),
        )

        val thrown = assertThrows(InvalidInputException::class.java) {
            BackupCodec.decode(fromTheFuture)
        }

        assertTrue(
            "the message should say why, not just fail",
            thrown.message.orEmpty().contains("newer version"),
        )
    }

    @Test
    fun aFileFromBeforeHolds_isRead_asTheCountedSetsItHolds() {
        // B253 moved the ceiling to 7 because a hold cannot be represented by a v6 build. The floor stays
        // at 5, which is the other half of that decision: every older file still reads, and a set without a
        // duration is simply counted.
        val older = sample.copy(
            schemaVersion = 6,
            sets = sample.sets.map { it.copy(durationSeconds = null, reps = 8) },
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(older))

        assertEquals(6, decoded.schemaVersion)
        assertNull(decoded.sets.first().durationSeconds)
        assertEquals(8, decoded.sets.first().reps)
    }

    @Test
    fun aNewerFileCarryingAValueThisBuildCannotRead_isRefusedAsNewer() {
        // ROADMAP B62: the version is read before the body, so a file whose only fault is being newer
        // is answered as newer even when it carries an enum name this build has no constant for —
        // which is the very case the version exists for, and the one the old order got wrong.
        val fromTheFuture = BackupCodec.encode(sample)
            .replace(Regex("\"schemaVersion\"\\s*:\\s*\\d+"), "\"schemaVersion\": 99")
            .replace(
                Regex("\"primaryMuscle\"\\s*:\\s*\"[A-Za-z_]+\""),
                "\"primaryMuscle\": \"A_MUSCLE_FROM_THE_FUTURE\"",
            )
        assertTrue("the fixture must carry an unreadable value", fromTheFuture.contains("A_MUSCLE_FROM_THE_FUTURE"))

        val thrown = assertThrows(InvalidInputException::class.java) {
            BackupCodec.decode(fromTheFuture)
        }

        assertTrue(
            "a newer file is newer, not corrupt",
            thrown.message.orEmpty().contains("newer version"),
        )
    }

    @Test
    fun aKnownVersionsFileCarryingAnUnreadableValue_isRefusedAsCorrupt() {
        // The other half: with the version known, an enum name this build cannot represent is the file
        // being wrong rather than new, and it is answered as the corrupt case.
        val corrupt = BackupCodec.encode(sample).replace(
            Regex("\"primaryMuscle\"\\s*:\\s*\"[A-Za-z_]+\""),
            "\"primaryMuscle\": \"A_MUSCLE_FROM_THE_FUTURE\"",
        )

        val thrown = assertThrows(InvalidInputException::class.java) {
            BackupCodec.decode(corrupt)
        }

        assertTrue(
            "a known version that does not decode is corrupt",
            thrown.message.orEmpty().contains("does not look like a backup file"),
        )
    }

    @Test
    fun unknownFieldsAreTolerated() {
        // Within the same schema version a newer build may add fields; refusing
        // them would make the format brittle for no gain.
        val text = BackupCodec.encode(sample).replaceFirst("{", "{\n  \"somethingNew\": 42,")

        assertEquals(sample, BackupCodec.decode(text))
    }

    @Test
    fun aFileWrittenBeforeTemplatesExisted_stillDecodes_seeingNone() {
        // N3 added whole collections rather than fields. They are defaulted to
        // empty, which is what lets a file from before templates restore cleanly
        // instead of failing to decode.
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val olderFile = JsonObject(tree - "templates" - "templateExercises" - "templateSets")

        val restored = BackupCodec.decode(olderFile.toString())

        assertEquals(emptyList<TemplateDto>(), restored.templates)
        assertEquals(emptyList<TemplateExerciseDto>(), restored.templateExercises)
        assertEquals(emptyList<TemplateSetDto>(), restored.templateSets)
        assertEquals("everything else still decodes", "Push day", sample.templates.first().name)
    }

    @Test
    fun aFileWrittenBeforeTheN4ToN7FieldsExisted_stillDecodes() {
        // N4 through N7 each added fields and deliberately did *not* bump the schema
        // version (the version moves when a field changes meaning or is removed).
        // That is only safe because every added field is defaulted, so a file
        // written before they existed must still decode — reading them unset.
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val olderExercises = tree.getValue("exercises").jsonArray.map { element ->
            JsonObject(
                element.jsonObject.filterKeys { it != "restSeconds" && it != "techniqueNote" },
            )
        }
        val olderSessions = tree.getValue("sessions").jsonArray.map { element ->
            JsonObject(element.jsonObject.filterKeys { it != "readinessNote" })
        }
        val olderSessionExercises = tree.getValue("sessionExercises").jsonArray.map { element ->
            JsonObject(
                element.jsonObject.filterKeys { it != "finishedAt" && it != "muscleFeel" },
            )
        }
        val olderSets = tree.getValue("sets").jsonArray.map { element ->
            JsonObject(element.jsonObject.filterKeys { it != "rpeHalves" && it != "note" })
        }
        val olderFile = JsonObject(
            tree + mapOf(
                "exercises" to JsonArray(olderExercises),
                "sessions" to JsonArray(olderSessions),
                "sessionExercises" to JsonArray(olderSessionExercises),
                "sets" to JsonArray(olderSets),
            ),
        )

        val restored = BackupCodec.decode(olderFile.toString())

        assertEquals(null, restored.exercises.first().restSeconds)
        assertEquals(null, restored.exercises.first().techniqueNote)
        assertEquals(null, restored.sessions.first().readinessNote)
        assertEquals(null, restored.sessionExercises.first().finishedAt)
        assertEquals(null, restored.sessionExercises.first().muscleFeel)
        assertEquals(null, restored.sets.first().rpeHalves)
        assertEquals(null, restored.sets.first().note)
    }

    @Test
    fun aFileWrittenBeforePlansExisted_stillDecodes_withThePlanFieldsUnset() {
        // N14 added a collection *and* two fields on template exercises. A file from
        // before it must decode, with the plan absent and the rest and cue unset —
        // which is "use the library's", not zero (ROADMAP N14, N5).
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val exercises = tree["templateExercises"]!!.jsonArray.map { element ->
            JsonObject(
                element.jsonObject -
                    "restSeconds" -
                    "techniqueNote",
            )
        }
        val olderFile = JsonObject(
            tree - "templateSets" + ("templateExercises" to JsonArray(exercises)),
        )

        val restored = BackupCodec.decode(olderFile.toString())

        assertEquals(emptyList<TemplateSetDto>(), restored.templateSets)
        val exercise = restored.templateExercises.single()
        assertNull(exercise.restSeconds)
        assertNull(exercise.techniqueNote)
        assertEquals("the rest of the exercise still decodes", "back-squat", exercise.exerciseId)
    }

    @Test
    fun aFileWrittenBeforeTheExerciseTargetRpe_stillDecodes_withNone() {
        // ROADMAP N59, amended: the effort moved from the set to the exercise, and the new field is
        // defaulted so a file from before it decodes with no target rather than failing.
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val exercises = tree["templateExercises"]!!.jsonArray.map { element ->
            JsonObject(element.jsonObject - "targetRpeHalves")
        }
        val olderFile = JsonObject(tree + ("templateExercises" to JsonArray(exercises)))

        val restored = BackupCodec.decode(olderFile.toString())

        assertNull(restored.templateExercises.single().targetRpeHalves)
    }

    @Test
    fun aPlanWithNoTargets_survivesTheRoundTrip_asNulls() {
        // Nullable targets are the point: an absent weight is not a zero (N14).
        val bare = sample.copy(
            templateSets = listOf(
                TemplateSetDto(
                    id = "ts1",
                    templateExerciseId = "te1",
                    setIndex = 0,
                    role = SetType.WARMUP,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )

        val restored = BackupCodec.decode(BackupCodec.encode(bare))

        val set = restored.templateSets.single()
        assertEquals(SetType.WARMUP, set.role)
        assertNull(set.targetWeightGrams)
        assertNull(set.targetRepsMin)
        assertNull(set.targetRepsMax)
    }

    @Test
    fun aFileWrittenBeforeAssistanceExisted_stillDecodes_seeingNone() {
        // N15 added a column to sets and a target to plans. A file from before it must
        // decode with 0 and null — "no assistance" — rather than fail or invent one.
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val sets = tree["sets"]!!.jsonArray.map { element ->
            JsonObject(element.jsonObject - "assistanceGrams")
        }
        val planSets = tree["templateSets"]!!.jsonArray.map { element ->
            JsonObject(element.jsonObject - "targetAssistanceGrams")
        }
        val olderFile = JsonObject(
            tree + ("sets" to JsonArray(sets)) + ("templateSets" to JsonArray(planSets)),
        )

        val restored = BackupCodec.decode(olderFile.toString())

        assertEquals(0L, restored.sets.first().assistanceGrams)
        assertNull(restored.templateSets.first().targetAssistanceGrams)
    }

    @Test
    fun aFileThatStillPinsATemplateToADay_decodes_andThePinIsDropped() {
        // N56 removed the column and the DTO field with it. A file written before that carries the
        // pin, and dropping a field on read is only safe because the codec ignores keys this build
        // does not know (BackupCodec's own note, and the schemaVersion gate above it).
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val templates = tree["templates"]!!.jsonArray.map { element ->
            JsonObject(element.jsonObject + ("weekday" to JsonPrimitive("FRIDAY")))
        }
        val olderFile = JsonObject(tree + ("templates" to JsonArray(templates)))

        val restored = BackupCodec.decode(olderFile.toString())

        assertEquals("the workout's own name still decodes", "Push day", restored.templates.first().name)
    }

    @Test
    fun metricTargets_surviveTheRoundTrip() {
        // Goals live in settings rather than the database, so the entity-by-entity round trip
        // cannot cover them — and they were absent from every export until this field existed
        // (N39). The keys are `MetricKey.id` values, which is what the screen looks them up by.
        val withGoals = sample.copy(goals = mapOf("body.weight" to 80.0, "workout.rpe" to 7.5))

        val restored = BackupCodec.decode(BackupCodec.encode(withGoals))

        assertEquals(withGoals.goals, restored.goals)
    }

    @Test
    fun aFileWrittenBeforeGoalsExisted_stillDecodes_seeingNone() {
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val olderFile = JsonObject(tree - "goals")

        assertEquals(emptyMap<String, Double>(), BackupCodec.decode(olderFile.toString()).goals)
    }

    @Test
    fun goalKinds_surviveTheRoundTrip_besideTheirValues() {
        // ROADMAP N135: a goal's *kind* is what its rate is derived from, so a file that carried the value and
        // not the kind would restore a maintenance goal as a target — a different goal, not an incomplete one.
        val withKinds = sample.copy(
            goals = mapOf("BODY:WEIGHT" to 80_000.0),
            goalKinds = mapOf("BODY:WEIGHT" to "MAINTAIN"),
        )

        val restored = BackupCodec.decode(BackupCodec.encode(withKinds))

        assertEquals(withKinds.goalKinds, restored.goalKinds)
        assertEquals(withKinds.goals, restored.goals)
    }

    @Test
    fun goalPeriods_surviveTheRoundTrip_withTheirDates() {
        // ROADMAP N137: the periods are a goal's past, so a file that carried only the current value would
        // restore the goal and lose the answer to "what was I asking for in March".
        val withPeriods = sample.copy(
            goalPeriods = listOf(
                GoalPeriodDto(
                    id = "g1",
                    metricId = "BODY:WEIGHT",
                    kind = "TARGET",
                    value = 80_000.0,
                    startedAt = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
                GoalPeriodDto(
                    id = "g2",
                    metricId = "BODY:WEIGHT",
                    kind = "MAINTAIN",
                    value = 79_000.0,
                    startedAt = 1_700_000_000_000L,
                    createdAt = 2L,
                    updatedAt = 2L,
                ),
            ),
        )

        val restored = BackupCodec.decode(BackupCodec.encode(withPeriods))

        assertEquals(withPeriods.goalPeriods, restored.goalPeriods)
    }

    @Test
    fun aFileWrittenBeforePeriodsExisted_stillDecodes_seeingNone() {
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val olderFile = JsonObject(tree - "goalPeriods")

        assertEquals(emptyList<GoalPeriodDto>(), BackupCodec.decode(olderFile.toString()).goalPeriods)
    }

    @Test
    fun aFileWrittenBeforeGoalKindsExisted_stillDecodes_seeingNone() {
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val olderFile = JsonObject(tree - "goalKinds")

        assertEquals(emptyMap<String, String>(), BackupCodec.decode(olderFile.toString()).goalKinds)
    }

    @Test
    fun aKeptVersion_survivesTheRoundTrip_withTheTemplateItCameFrom() {
        // ROADMAP N134: the export carries versions and the `versionOf` link with them, so an import
        // re-attaches them instead of flattening them into ordinary templates. The file is the only path
        // off the device, and a silent loss found after a restore is the worst form of this feature.
        val version = sample.templates.first().copy(versionOf = "template-1")

        val restored = BackupCodec.decode(BackupCodec.encode(sample.copy(templates = listOf(version))))

        assertEquals(listOf(version), restored.templates)
    }

    @Test
    fun aFileWrittenBeforeVersionsExisted_stillDecodes_asPlainTemplates() {
        // The field is defaulted, so a file that never named it reads as it was written: no row is a
        // version of anything (N134). Written before versions existed is the same schemaVersion, because
        // an added nullable field is not a format break.
        val json = Json { prettyPrint = false }
        val tree = json.parseToJsonElement(BackupCodec.encode(sample)).jsonObject
        val templates = tree["templates"]!!.jsonArray.map { element ->
            JsonObject(element.jsonObject - "versionOf")
        }
        val olderFile = JsonObject(tree + ("templates" to JsonArray(templates)))

        val restored = BackupCodec.decode(olderFile.toString())

        assertNull(restored.templates.first().versionOf)
    }

    @Test
    fun aFileWrittenBeforeAMovementCouldAnswerDone_decodesAsFollowingTheApp() {
        // ROADMAP N235: the field is defaulted and the format's version does not move, so a backup written
        // before this existed still decodes — with every movement following the setting, which is what it did.
        val dto = Json.decodeFromString<ExerciseDto>(
            """
            {
              "id": "ex1",
              "name": "Back Squat",
              "primaryMuscle": null,
              "secondaryMuscles": [],
              "equipment": "BARBELL",
              "movementPattern": "SQUAT",
              "createdAt": 1,
              "updatedAt": 1
            }
            """.trimIndent(),
        )

        assertNull(dto.progressionMode)
    }
}
