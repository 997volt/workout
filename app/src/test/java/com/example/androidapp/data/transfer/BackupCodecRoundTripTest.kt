package com.example.androidapp.data.transfer

import com.example.androidapp.data.local.ExerciseEntity
import com.example.androidapp.data.local.MeasurementEntity
import com.example.androidapp.data.local.MeasurementSoreMuscleEntity
import com.example.androidapp.data.local.MetricGoalEntity
import com.example.androidapp.data.local.ProgramDeloadDayEntity
import com.example.androidapp.data.local.ProgramDeloadEntity
import com.example.androidapp.data.local.ProgramEntity
import com.example.androidapp.data.local.ProgramSkipEntity
import com.example.androidapp.data.local.ProgramSlotEntity
import com.example.androidapp.data.local.ProgramSubstitutionEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.SessionExerciseJointEntity
import com.example.androidapp.data.local.SessionSoreMuscleEntity
import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.TemplateSetEntity
import com.example.androidapp.data.local.WorkoutSessionEntity
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.Side
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek

/**
 * Every column survives a trip through the backup codec.
 *
 * This exists because the codec is **hand-written**, listing each field by name, and it has
 * silently dropped an unnamed column three times — a set's location (N9), the assistance on a
 * set (N15), a template's weekday (N16). Each was found by hand, on a device, after shipping.
 * A dropped field is invisible in the other direction: an export still looks complete, and the
 * data is simply absent from the file.
 *
 * The guard is a round trip with **every field set to something distinctive**. A field the
 * codec forgets comes back as its default instead, and the assertion names it. The next column
 * this app adds — N24's superset group is the one waiting — fails here instead of turning up
 * missing in someone's backup.
 *
 * It is a JVM test on purpose: the mappers are pure, so this runs in the ordinary suite rather
 * than needing a device, and it cannot be skipped for being inconvenient.
 */
class BackupCodecRoundTripTest {

    @Test
    fun aMeasurement_survivesTheCodec() {
        // ROADMAP N32, and the reason this file exists: the codec lists every field by hand, so a
        // column it forgets comes back as its default — an export that looks complete and is not.
        // Every field is set to something distinctive, so nothing can pass by coincidence.
        val entity = MeasurementEntity(
            id = "m1",
            measuredAt = 1_700_000_000_000L,
            kind = MeasurementKind.DAILY,
            weightGrams = 82_450L,
            bodyFatTenths = 183,
            muscleTenths = 421,
            neckMm = 381L,
            chestMm = 1_042L,
            waistMm = 864L,
            hipsMm = 977L,
            upperArmMm = 363L,
            thighMm = 574L,
            calfMm = 389L,
            createdAt = 1_700_000_000_001L,
            updatedAt = 1_700_000_000_002L,
            deletedAt = 1_700_000_000_003L,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aDaySoreMuscles_surviveTheCodec() {
        // The collection N113 added, on the shape this file exists to hold: a row whose only fault is a
        // field the codec forgets comes back as its default, and nothing else would notice.
        val entity = MeasurementSoreMuscleEntity(
            id = "msm1",
            measurementId = "m1",
            muscle = MuscleGroup.QUADS,
            score = 8,
            position = 1,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aSessionExercise_survivesTheCodec() {
        val entity = SessionExerciseEntity(
            id = "se1",
            sessionId = "s1",
            exerciseId = "back-squat",
            position = 3,
            // The column N24 added: if the codec ever forgets it, this test says so.
            supersetGroup = 7,
            restSeconds = 120,
            techniqueNote = "brace hard",
            // The column N236 added: the declined step is the record the app cannot recompute, so a
            // restore that dropped it would lose the fact rather than re-derive it (B297).
            declinedDirections = "REPS,LOAD",
            finishedAt = 1_700_000_000_000L,
            muscleFeel = 8,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aTemplateExercise_survivesTheCodec() {
        val entity = TemplateExerciseEntity(
            id = "te1",
            templateId = "t1",
            exerciseId = "back-squat",
            position = 2,
            // The column B16 added: a plan's grouping must survive a restore like any other.
            supersetGroup = 4,
            restSeconds = 90,
            techniqueNote = "pause at the bottom",
            // The column N59's amendment added: the plan's one target RPE per exercise. A hand-written
            // codec that forgets it restores the effort as absent, which is the whole trap this file is.
            targetRpeHalves = 17,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aSet_survivesTheCodec() {
        // The assistance column is why this test exists: N15 added it and the codec lost it.
        val entity = SetEntryEntity(
            id = "set1",
            sessionExerciseId = "se1",
            setIndex = 1,
            // A timed set states a hold instead of a rep count (N220), so the fixture is one.
            reps = 0,
            weightGrams = 100_000L,
            rpeHalves = 17,
            note = "slow eccentric",
            setType = SetType.TOP_SET,
            assistanceGrams = 20_000L,
            // The column N219 added: the snapshot a bodyweight set carries. A codec that forgot it would
            // restore the set at its added weight alone, silently changing what was lifted.
            bodyweightGrams = 75_000L,
            // And N220's: the hold. A dropped duration restores a set nobody can read as performed.
            durationSeconds = 45,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aPlannedSet_survivesTheCodec() {
        val entity = TemplateSetEntity(
            id = "ts1",
            templateExerciseId = "te1",
            setIndex = 2,
            // A role that carries an effort and every target: a rung would round-trip with its effort
            // dropped, which is N79's rule and is asserted on its own below.
            role = SetType.FAILURE,
            targetWeightGrams = 92_500L,
            targetAssistanceGrams = 15_000L,
            targetRepsMin = 2,
            targetRepsMax = 4,
            targetRepsCurrent = 3,
            note = "leave one in the tank",
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aPlannedHold_survivesTheCodec() {
        // ROADMAP N242/B297: the format gained the column and no test pinned it, so a mapper that forgot
        // the hold would have passed the gate and restored a carry as a counted set.
        val entity = TemplateSetEntity(
            id = "ts2",
            templateExerciseId = "te1",
            setIndex = 1,
            role = SetType.NORMAL,
            targetWeightGrams = null,
            targetAssistanceGrams = null,
            targetRepsMin = null,
            targetRepsMax = null,
            targetRepsCurrent = null,
            targetDurationSeconds = 45,
            note = null,
            createdAt = 1_600_000_000_002L,
            updatedAt = 1_600_000_000_003L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aPlannedRung_survivesTheCodec_withItsRunValue() {
        // ROADMAP N79: the run's value is held by its first rung, and the codec must carry it or a
        // restore silently leaves the ladder with nothing to take off (B70).
        val entity = TemplateSetEntity(
            id = "ts1",
            templateExerciseId = "te1",
            setIndex = 3,
            role = SetType.DROP,
            targetWeightGrams = 92_500L,
            targetAssistanceGrams = null,
            targetRepsMin = null,
            targetRepsMax = null,
            targetRepsCurrent = null,
            dropValueGrams = 20_000L,
            note = null,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        val roundTripped = entity.toDto().toEntity()
        assertThat(roundTripped.dropValueGrams).isEqualTo(20_000L)
    }

    @Test
    fun aSession_survivesTheCodec() {
        val entity = WorkoutSessionEntity(
            id = "s1",
            startedAt = 1_600_000_000_000L,
            finishedAt = 1_600_000_001_000L,
            restEndsAt = 1_600_000_000_500L,
            readinessNote = "slept badly",
            notes = "good session",
            deletedAt = null,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            // The column P3.3 added: a session's provenance must survive a restore, or a
            // restored done day would read as missed.
            templateId = "t1",
            // And the one N141 added: an activity is a session, so a restore that dropped this
            // would bring every swim back as an empty workout (B145).
            activity = "SWIMMING",
        )

        val restored = entity.toDto().toEntity()

        // The entity's own `restEndsAt` is runtime state the file does not carry (N109), so a restored
        // session never comes back mid-rest; every field the file does carry is asserted for equality.
        assertThat(restored).isEqualTo(entity.copy(restEndsAt = null))
        assertThat(restored.restEndsAt).isNull()
    }

    @Test
    fun anOpenSession_comesBackClosedAtItsOwnStart() {
        // B150: `finishedAt IS NULL` is the app's whole definition of "the workout in progress", so a file
        // exported mid-workout — or one left open by a crash, which the app deliberately preserves — used to
        // decide what the importing device was doing. Ending it at its own start keeps the day and the record
        // without claiming anybody is in it.
        val open = WorkoutSessionEntity(
            id = "s1",
            startedAt = 1_600_000_000_000L,
            finishedAt = null,
            restEndsAt = 1_600_000_000_500L,
            readinessNote = null,
            notes = null,
            deletedAt = null,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_000L,
        )

        val restored = open.toDto().toEntity()

        assertThat(restored.finishedAt).isEqualTo(1_600_000_000_000L)
        assertThat(restored.restEndsAt).isNull()
    }

    @Test
    fun aProgram_survivesTheCodec() {
        val entity = ProgramEntity(
            id = "p1",
            name = "Upper/Lower",
            isActive = true,
            // The column P3.12 added: an authored order must survive a restore, or the union
            // comes back in a different order than the user put it in.
            position = 3,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aProgramSlot_survivesTheCodec() {
        val entity = ProgramSlotEntity(
            id = "slot1",
            programId = "p1",
            templateId = "t1",
            position = 2,
            // A weekday is the column a hand-written codec loses first (N16's own history),
            // so it is set to something distinctive here.
            weekday = DayOfWeek.THURSDAY,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aProgramSkip_survivesTheCodec() {
        val entity = ProgramSkipEntity(
            id = "skip1",
            slotId = "slot1",
            weekStart = 20_345L,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aSessionSoreMuscle_survivesTheCodec() {
        // ROADMAP N62: the muscle and its score are a fact the lifter wrote, and the codec is
        // hand-written — a field it is not told about comes back as its default instead, which is
        // an export that looks complete and is not.
        val entity = SessionSoreMuscleEntity(
            id = "sore1",
            sessionId = "s1",
            muscle = MuscleGroup.HAMSTRINGS,
            score = 7,
            position = 2,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = 1_600_000_000_002L,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aSessionExerciseJoint_survivesTheCodec() {
        // ROADMAP N63: the joint, its side and its score are a fact the lifter wrote, and the codec
        // is hand-written — a field it is not told about comes back as its default instead, which is
        // an export that looks complete and is not. The side is the column this table adds.
        val entity = SessionExerciseJointEntity(
            id = "joint1",
            sessionExerciseId = "se1",
            joint = Joint.KNEE,
            side = Side.LEFT,
            score = 6,
            position = 1,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = 1_600_000_000_002L,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aProgramDeload_survivesTheCodec() {
        // The row P3.10 added: a deload week is authored setup the app cannot recompute, so a codec
        // that forgot it would restore every backed-off week as a miss.
        val entity = ProgramDeloadEntity(
            id = "deload1",
            programId = "p1",
            weekStart = 20_345L,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aProgramDeloadDay_survivesTheCodec() {
        // The row N147 added: the week's own workouts are authored setup the app cannot recompute, so
        // a codec that forgot them would restore the exemption and lose what it excused.
        val entity = ProgramDeloadDayEntity(
            id = "day1",
            deloadId = "deload1",
            templateId = "t2",
            position = 2,
            weekday = DayOfWeek.MONDAY,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aProgramSubstitution_survivesTheCodec() {
        // The row P3.11 added: without it a restored week reads as missed even though it was
        // trained, with something else.
        val entity = ProgramSubstitutionEntity(
            id = "sub1",
            slotId = "slot1",
            weekStart = 20_345L,
            templateId = "t2",
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun anExercise_survivesTheCodec() {
        // A custom exercise's taxonomy and its own rest, which is what N9 and N14 added.
        val entity = ExerciseEntity(
            id = "front-squat",
            name = "Front Squat",
            primaryMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE),
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.SQUAT,
            restSeconds = 150,
            techniqueNote = "elbows up",
            // The column N64 added: the exercise's own display unit. This is the field the codec
            // dropped, which is why it is set here rather than left at its null default.
            weightUnit = "POUNDS",
            // And the one N77 added, on the same reasoning: a restore that drops it silently puts
            // every exercise back on the unit's step, which is what the field exists to override (B70).
            stepGrams = 5_000L,
            // And the one N219 added: a restore that drops it turns a pull-up back into an ordinary
            // movement, whose sets then read as their added weight alone.
            isBodyweight = true,
            // And N220's: dropped, a plank comes back as a movement whose sets ask for reps.
            isTimed = true,
            // And N235's: dropped, a movement the app deliberately never asks comes back as one it asks
            // every session — the override this field exists for.
            progressionMode = "APPLY_AT_DONE",
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aTemplate_survivesTheCodec() {
        // B145: this file exists to fail when a column is not wired, and `versionOf` is the one N134 added — a
        // dropped link would turn a kept version back into a live template.
        val entity = TemplateEntity(
            id = "t1",
            name = "Push day",
            versionOf = "t-live",
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
            deletedAt = null,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }

    @Test
    fun aGoalPeriod_survivesTheCodec() {
        // B145: the whole `metric_goals` table is new (N137), and each of its nullable columns means something
        // different — no kind is a goal written before kinds existed, no value is a direction-only goal, no
        // start is the period the move could not date — so all three are set here.
        val entity = MetricGoalEntity(
            id = "g1",
            metricId = "BODY:WEIGHT",
            kind = "TARGET",
            value = 80_000.0,
            startedAt = 1_600_000_000_000L,
            createdAt = 1_600_000_000_000L,
            updatedAt = 1_600_000_000_001L,
        )

        assertThat(entity.toDto().toEntity()).isEqualTo(entity)
    }
}
