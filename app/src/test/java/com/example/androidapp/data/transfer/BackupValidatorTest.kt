package com.example.androidapp.data.transfer

import com.example.androidapp.domain.repository.BackupProblem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The values a backup may not carry into the store (B342).
 *
 * The import writes decoded rows straight to entities, so these are the checks that stand in for the
 * write boundaries it bypasses. A valid app export is the baseline; each case changes one field.
 */
class BackupValidatorTest {

    @Test
    fun anOrdinaryBackup_hasNoProblems() {
        assertThat(BackupValidator.problems(valid())).isEmpty()
    }

    @Test
    fun aCountedSet_needsPositiveReps_andAHoldStatesSecondsInstead() {
        val zeroReps = valid().copy(sets = listOf(set.copy(reps = 0)))
        assertThat(BackupValidator.problems(zeroReps).messages())
            .contains("A counted set needs between 1 and 10000 reps.")

        val both = valid().copy(sets = listOf(set.copy(reps = 5, durationSeconds = 30)))
        assertThat(BackupValidator.problems(both).messages())
            .contains("A held set records seconds instead of reps.")

        val longHold = valid().copy(sets = listOf(set.copy(reps = 0, durationSeconds = 86_401)))
        assertThat(BackupValidator.problems(longHold).messages())
            .contains("A hold must be between 1 second and a day.")
    }

    @Test
    fun loadsAndRpe_areWithinTheirRanges() {
        val negative = valid().copy(sets = listOf(set.copy(weightGrams = -1)))
        assertThat(BackupValidator.problems(negative).messages())
            .contains("A load must be between 0 and the app's maximum.")

        val huge = valid().copy(sets = listOf(set.copy(weightGrams = 1_000_001)))
        assertThat(BackupValidator.problems(huge)).isNotEmpty()

        val rpe = valid().copy(sets = listOf(set.copy(rpeHalves = 21)))
        assertThat(BackupValidator.problems(rpe).messages())
            .contains("RPE must be between 1 and 10 in half steps.")
    }

    @Test
    fun bodyPercentages_andTapeLengths_haveCeilings() {
        val fat = valid().copy(measurements = listOf(measurement.copy(bodyFatTenths = 1_001)))
        assertThat(BackupValidator.problems(fat).messages())
            .contains("Body fat must be between 0% and 100%.")

        val tape = valid().copy(measurements = listOf(measurement.copy(waistMm = 5_001)))
        assertThat(BackupValidator.problems(tape).messages())
            .contains("A tape measurement is outside the app's range.")
    }

    @Test
    fun aZoneOffset_andAWeek_mustBeRepresentable() {
        val zone = valid().copy(sessions = listOf(session.copy(zoneOffsetMinutes = 18 * 60 + 1)))
        assertThat(BackupValidator.problems(zone).messages())
            .contains("The stored time-zone offset is outside ±18 hours.")

        val week = valid().copy(
            programSkips = listOf(
                ProgramSkipDto(
                    id = "skip-1",
                    slotId = "slot-1",
                    weekStart = Long.MAX_VALUE,
                    createdAt = 0L,
                    updatedAt = 0L,
                ),
            ),
        )
        assertThat(BackupValidator.problems(week).messages())
            .contains("The week is outside the calendar.")
    }

    @Test
    fun aPlannedSet_cannotStateRepsAndAHoldTogether() {
        val both = valid().copy(
            templateSets = listOf(
                templateSet.copy(targetRepsMin = 5, targetRepsMax = 8, targetDurationSeconds = 30),
            ),
        )
        assertThat(BackupValidator.problems(both).messages())
            .contains("A planned set cannot state reps and a hold together.")
    }

    @Test
    fun aLegacyFile_mayCarryValuesOutsideTheNewCeilings() {
        // B374: these are exactly the values a pre-B359 build could write. The file is accepted as-is
        // and the preview says so; only a current file is held to the current ceilings.
        val legacy = valid().copy(
            limitsVersion = 0,
            sets = listOf(set.copy(reps = 20_000, weightGrams = 2_000_000L)),
            measurements = listOf(measurement.copy(bodyFatTenths = 1_830, waistMm = 9_999)),
        )

        assertThat(BackupValidator.problems(legacy)).isEmpty()
    }

    @Test
    fun aPhasedProgramDay_isAnOrdinaryRow() {
        // N292: the backup carries the authored week like any other field, so an ordinary phased file
        // has nothing to say about it.
        val phased = valid().copy(
            programSlots = listOf(programSlot(week = 2)),
        )

        assertThat(BackupValidator.problems(phased)).isEmpty()
    }

    @Test
    fun aProgramDay_cannotNameWeekZero() {
        val zero = valid().copy(
            programSlots = listOf(programSlot(week = 0)),
        )

        assertThat(BackupValidator.problems(zero).messages())
            .contains("A program week is numbered from 1.")
    }

    @Test
    fun aProgram_thatMixesPhasedAndUnphasedDays_isRefused() {
        // N292: an unphased day in a phased program is never read, so a backup carrying the mixed
        // shape would restore a day the lifter could not see.
        val mixed = valid().copy(
            programSlots = listOf(
                programSlot(week = 1),
                programSlot(week = 1).copy(id = "slot-2", phaseWeek = null, position = 1),
            ),
        )

        assertThat(BackupValidator.problems(mixed).messages())
            .contains("Its days must all name a week, or none of them.")
    }

    private fun programSlot(week: Int) = ProgramSlotDto(
        id = "slot-1",
        programId = "program-1",
        templateId = "template-1",
        position = 0,
        phaseWeek = week,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun List<BackupProblem>.messages(): List<String> = map { it.message }

    private fun valid(): BackupFile = BackupFile(
        schemaVersion = BackupCodec.CURRENT_SCHEMA_VERSION,
        limitsVersion = BackupCodec.CURRENT_LIMITS_VERSION,
        exportedAt = 0L,
        exercises = listOf(
            ExerciseDto(
                id = "back-squat",
                name = "Back Squat",
                primaryMuscle = com.example.androidapp.domain.model.MuscleGroup.QUADS,
                secondaryMuscles = emptyList(),
                equipment = com.example.androidapp.domain.model.Equipment.BARBELL,
                movementPattern = com.example.androidapp.domain.model.MovementPattern.SQUAT,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        ),
        sessions = listOf(session),
        sessionExercises = listOf(
            SessionExerciseDto(
                id = "se-1",
                sessionId = "session-1",
                exerciseId = "back-squat",
                position = 0,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        ),
        sets = listOf(set),
        measurements = listOf(measurement),
        programs = listOf(
            ProgramDto(
                id = "program-1",
                name = "Program",
                createdAt = 0L,
                updatedAt = 0L,
            ),
        ),
        templates = listOf(
            TemplateDto(
                id = "template-1",
                name = "Squat day",
                createdAt = 0L,
                updatedAt = 0L,
            ),
        ),
        templateExercises = listOf(
            TemplateExerciseDto(
                id = "te-1",
                templateId = "template-1",
                exerciseId = "back-squat",
                position = 0,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        ),
        templateSets = listOf(templateSet),
    )

    private companion object {
        val session = SessionDto(
            id = "session-1",
            startedAt = 1_000L,
            finishedAt = 2_000L,
            createdAt = 0L,
            updatedAt = 0L,
        )

        val set = SetDto(
            id = "set-1",
            sessionExerciseId = "se-1",
            setIndex = 0,
            reps = 5,
            weightGrams = 100_000L,
            setType = com.example.androidapp.domain.model.SetType.NORMAL,
            createdAt = 0L,
            updatedAt = 0L,
        )

        val measurement = MeasurementDto(
            id = "measurement-1",
            measuredAt = 1_000L,
            kind = com.example.androidapp.domain.model.MeasurementKind.DAILY,
            createdAt = 0L,
            updatedAt = 0L,
        )

        val templateSet = TemplateSetDto(
            id = "ts-1",
            templateExerciseId = "te-1",
            setIndex = 0,
            role = com.example.androidapp.domain.model.SetType.NORMAL,
            targetRepsMin = 5,
            targetRepsMax = 8,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
}
