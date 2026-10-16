package com.example.androidapp.ui.history

import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.WorkoutSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * N146's search and filter, as rules (ROADMAP N146).
 *
 * Pure JVM, like the month grouping it wraps: the alternative is driving a screen to type a letter, and the
 * questions here — what a query can reach, what an empty month does, what the chip hides — are all answerable
 * from the rows.
 *
 * The zone and the locale are named rather than defaulted, the same rule [HistoryFormat] holds: a test that
 * reads the machine's zone would pass in one place and fail in another.
 */
class HistorySearchTest {

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")
    private val locale: Locale = Locale.ENGLISH

    @Test
    fun anEmptyQuery_showsEveryAdmittedRow() {
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay),
            query = "",
            filter = HistoryFilter.ALL,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.flatMap { it.workouts }.map { it.id }).containsExactly("squat", "swim")
    }

    @Test
    fun aQuery_reachesTheMonthsHeading() {
        // The heading is what a lifter reads to find a workout, so it is what a query has to be able to name:
        // "September" and the row's plan are the two halves of "where is that session".
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay),
            query = "september",
            filter = HistoryFilter.ALL,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.map { it.month }).containsExactly(YearMonth.of(2026, 9))
    }

    @Test
    fun aQuery_reachesThePlansName() {
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay),
            query = "push",
            filter = HistoryFilter.ALL,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.flatMap { it.workouts }.map { it.id }).containsExactly("squat")
    }

    @Test
    fun aQuery_reachesTheActivity_asWords() {
        // The stored name is `ROCK_CLIMBING`-shaped and nobody types an underscore, while the row draws "Rock
        // climbing": the query and the row are compared with the spaces taken out, so both spellings find it.
        // The row's own label is a string resource, which is why the stored name is what is matched.
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay, climbDay),
            query = "rock climbing",
            filter = HistoryFilter.ALL,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.flatMap { it.workouts }.map { it.id }).containsExactly("climb")
    }

    @Test
    fun aMonthTheQueryEmptied_isNotAHeadingOverNothing() {
        // The grouping runs first, so a month whose rows all failed the query would otherwise be drawn as a
        // heading with nothing under it — the shape a reader takes for a broken list.
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay, augustRow),
            query = "push",
            filter = HistoryFilter.ALL,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.map { it.month }).containsExactly(YearMonth.of(2026, 9))
        assertThat(groups.single().workouts.map { it.id }).containsExactly("squat")
    }

    @Test
    fun theActivityChip_showsOnlyTheDaysThatWereNotWorkouts() {
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay, climbDay),
            query = "",
            filter = HistoryFilter.ACTIVITIES,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.flatMap { it.workouts }.map { it.id }).containsExactly("swim", "climb")
    }

    @Test
    fun theWorkoutChip_hidesThem() {
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay),
            query = "",
            filter = HistoryFilter.WORKOUTS,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.flatMap { it.workouts }.map { it.id }).containsExactly("squat")
    }

    @Test
    fun aQueryAndAChip_applyTogether() {
        // The two are one question, not two: a search for a plan while the activities chip is on must not
        // reach past the chip. Asked of the rows rather than of the UI, because the combination is a rule.
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay, climbDay),
            query = "swim",
            filter = HistoryFilter.WORKOUTS,
            zone = zone,
            locale = locale,
        )

        assertThat(groups).isEmpty()
    }

    @Test
    fun aWhitespaceQuery_isNoQuery() {
        // `trim`, so a stray space does not empty the list — the same trap the library's search had (B87).
        val groups = historyGroups(
            workouts = listOf(squatDay, swimDay),
            query = "   ",
            filter = HistoryFilter.ALL,
            zone = zone,
            locale = locale,
        )

        assertThat(groups.flatMap { it.workouts }).hasSize(2)
    }

    private val squatDay = summary(
        id = "squat",
        at = "2026-09-28T07:00:00Z",
        templateName = "Push day",
        setCount = 12,
    )

    private val swimDay = summary(
        id = "swim",
        at = "2026-09-20T07:00:00Z",
        activity = Activity.SWIMMING,
    )

    private val climbDay = summary(
        id = "climb",
        at = "2026-09-10T07:00:00Z",
        activity = Activity.ROCK_CLIMBING,
    )

    /** A month whose row the query does **not** match, which is what "the query emptied a month" means. */
    private val augustRow = summary(
        id = "august",
        at = "2026-08-30T07:00:00Z",
        templateName = "Leg day",
        setCount = 9,
    )

    private fun summary(
        id: String,
        at: String,
        templateName: String? = null,
        setCount: Int = 0,
        activity: Activity? = null,
    ) = WorkoutSummary(
        id = id,
        startedAt = Instant.parse(at),
        finishedAt = Instant.parse(at).plusSeconds(3_600),
        exerciseCount = if (activity == null) 4 else 0,
        setCount = setCount,
        volumeGrams = setCount * 1_000L,
        templateName = templateName,
        activity = activity,
    )
}
