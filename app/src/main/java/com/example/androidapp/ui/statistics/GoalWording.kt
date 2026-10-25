package com.example.androidapp.ui.statistics

import androidx.annotation.StringRes
import com.example.androidapp.R
import com.example.androidapp.domain.model.WeightGoalKind
import com.example.androidapp.ui.history.HistoryFormat
import java.time.LocalDate
import java.time.ZoneId

/** A day as the goal rows write it: the app's own date format, in the device's zone (N215). */
internal fun goalDayText(date: LocalDate): String =
    HistoryFormat.date(date.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

/** What each kind of weight goal is called (N135). */
@StringRes
internal fun weightGoalKindLabel(kind: WeightGoalKind): Int = when (kind) {
    WeightGoalKind.TARGET -> R.string.statistics_weight_goal_target
    WeightGoalKind.LOSE -> R.string.statistics_weight_goal_lose
    WeightGoalKind.GAIN -> R.string.statistics_weight_goal_gain
    WeightGoalKind.MAINTAIN -> R.string.statistics_weight_goal_maintain
}
