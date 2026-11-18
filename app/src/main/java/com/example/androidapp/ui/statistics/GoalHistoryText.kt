package com.example.androidapp.ui.statistics

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.history.HistoryFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * The goal history's day labels when a boundary is not a day (N328, B492).
 *
 * The period reader clips by instants, and a legacy goal row was written at a clock time; the row's
 * range has to show that instant rather than a day that would include readings it does not judge.
 * Extracted from `GoalRow.kt` because that file had reached this project's function ceiling.
 */
@Composable
internal fun goalPeriodEndText(start: Instant, nextStart: Instant, zone: ZoneId): String {
    val next = nextStart.atZone(zone)
    if (next.toLocalTime() != LocalTime.MIDNIGHT) return goalMomentText(nextStart, zone)
    val startDay = start.atZone(zone).toLocalDate()
    val endDay = if (next.toLocalDate() > startDay) next.toLocalDate().minusDays(1) else startDay
    return goalDayText(endDay)
}

/** A boundary as the day it began, plus its clock time when a legacy row wrote one (B492). */
@Composable
internal fun goalMomentText(instant: Instant, zone: ZoneId): String {
    val local = instant.atZone(zone)
    val day = goalDayText(local.toLocalDate())
    return if (local.toLocalTime() == LocalTime.MIDNIGHT) {
        day
    } else {
        stringResource(R.string.statistics_goal_day_at, day, HistoryFormat.time(instant, zone))
    }
}
