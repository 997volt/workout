package com.example.androidapp.data

import com.example.androidapp.domain.model.ExerciseTrendPoint
import com.example.androidapp.domain.model.ExerciseTrendRow
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.toExerciseTrendPoints
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * The progress chart's series, pinned to the app's own arithmetic (N165).
 *
 * The web page draws the same two numbers the app's exercise trend draws — the heaviest working set and
 * Epley's estimate from it — so the two implementations must agree or the big screen quietly contradicts
 * the phone. This test **generates** the fixture: it feeds the app's real
 * [toExerciseTrendPoints] the rows below, writes the whole `window.PROGRESS_FIXTURE` assignment (input and
 * expected series) to `build/`, and compares it with the committed file. The tool's test page then runs its
 * own series over the same input and asserts the same points.
 *
 * The data is chosen to pin the rules that are easy to get wrong: a warm-up is not the heaviest set, the
 * estimate is rounded to half a kilo, a single rep returns the weight itself, and a set above
 * [com.example.androidapp.domain.model.MAX_ESTIMATED_REPS] has a heaviest set but no estimate.
 *
 * Regenerate by copying the generated file:
 *
 *     cp app/build/progress-series-expected.js docs/fixtures/progress-series.js
 */
class ProgressSeriesFixtureTest {

    @Serializable
    private data class FixtureSession(val id: String, val startedAt: Long)

    @Serializable
    private data class FixtureSessionExercise(
        val id: String,
        val sessionId: String,
        val exerciseId: String,
    )

    @Serializable
    private data class FixtureSet(
        val sessionExerciseId: String,
        val weightGrams: Long,
        val reps: Int,
        val setType: String,
    )

    @Serializable
    private data class ExpectedPoint(
        val startedAt: Long,
        val heaviestSetGrams: Long?,
        val estimatedOneRepMaxGrams: Long?,
    )

    @Serializable
    private data class Fixture(
        val exerciseId: String,
        val sessions: List<FixtureSession>,
        val sessionExercises: List<FixtureSessionExercise>,
        val sets: List<FixtureSet>,
        val expected: List<ExpectedPoint>,
    )

    private val exerciseId = "barbell-bench-press"

    private val sessions = listOf(
        FixtureSession("s1", Instant.parse("2024-01-05T18:00:00Z").toEpochMilli()),
        FixtureSession("s2", Instant.parse("2024-02-02T18:00:00Z").toEpochMilli()),
        FixtureSession("s3", Instant.parse("2024-03-01T18:00:00Z").toEpochMilli()),
        FixtureSession("s4", Instant.parse("2024-04-01T18:00:00Z").toEpochMilli()),
    )

    private val sessionExercises = sessions.mapIndexed { index, session ->
        FixtureSessionExercise("se${index + 1}", session.id, exerciseId)
    }

    /**
     * Four sessions: a warm-up above nothing, an estimate that rounds, a set past the estimate's ceiling,
     * and a single that is its own max.
     */
    private val sets = listOf(
        FixtureSet("se1", 60_000L, 10, SetType.WARMUP.name),
        FixtureSet("se1", 100_000L, 5, SetType.NORMAL.name),
        FixtureSet("se1", 95_000L, 8, SetType.NORMAL.name),
        FixtureSet("se2", 105_000L, 3, SetType.NORMAL.name),
        FixtureSet("se2", 100_000L, 10, SetType.NORMAL.name),
        FixtureSet("se3", 60_000L, 10, SetType.WARMUP.name),
        FixtureSet("se3", 90_000L, 15, SetType.NORMAL.name),
        FixtureSet("se4", 120_000L, 1, SetType.NORMAL.name),
    )

    private val json = Json { prettyPrint = true }

    /** Runs the app's own rule over the fixture, exactly as the statistics screen would. */
    private fun expectedPoints(): List<ExpectedPoint> {
        val startedAt = sessions.associate { it.id to it.startedAt }
        val bySessionExercise = sessionExercises.associateBy { it.id }
        val rows = sets.map { set ->
            val link = bySessionExercise.getValue(set.sessionExerciseId)
            ExerciseTrendRow(
                sessionId = link.sessionId,
                sessionExerciseId = link.id,
                startedAt = Instant.ofEpochMilli(startedAt.getValue(link.sessionId)),
                muscleFeel = null,
                jointPain = null,
                weightGrams = set.weightGrams,
                reps = set.reps,
                rpeHalves = null,
                setType = SetType.valueOf(set.setType),
                assistanceGrams = null,
            )
        }
        return rows.toExerciseTrendPoints().map { point: ExerciseTrendPoint ->
            ExpectedPoint(
                startedAt = point.startedAt.toEpochMilli(),
                heaviestSetGrams = point.heaviestSetGrams,
                estimatedOneRepMaxGrams = point.estimatedOneRepMaxGrams,
            )
        }
    }

    private fun expectedFile(): String {
        val fixture = Fixture(
            exerciseId = exerciseId,
            sessions = sessions,
            sessionExercises = sessionExercises,
            sets = sets,
            expected = expectedPoints(),
        )
        return """
            |/* The progress chart's fixture: input and the series the app computes from it (N165).
            | *
            | * GENERATED by ProgressSeriesFixtureTest from the app's own toExerciseTrendPoints(). Do not edit.
            | * The tool's test page runs its series over `sessions`/`sets` and asserts `expected`.
            | */
            |window.PROGRESS_FIXTURE = ${json.encodeToString(fixture)};
            |
        """.trimMargin()
    }

    @Test
    fun theToolsProgressSeriesIsTheAppsTrend() {
        val expected = expectedFile()
        val generated = File(repoRoot, "build/progress-series-expected.js")
        generated.parentFile.mkdirs()
        generated.writeText(expected)

        val committed = File(repoRoot, "docs/fixtures/progress-series.js")
        if (!committed.isFile) {
            throw AssertionError(
                "docs/fixtures/progress-series.js is missing; " +
                    "copy build/progress-series-expected.js over it.",
            )
        }
        assertThat(committed.readText()).isEqualTo(expected)
    }
}
