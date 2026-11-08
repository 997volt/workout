package com.example.androidapp.ui.components

import com.example.androidapp.domain.model.WeightGoalKind

/**
 * The Statistics screen's own tags (ROADMAP N35).
 */
internal object StatisticsTags {
    const val OVERVIEW_WORKOUTS = "statistics_overview_workouts"
    const val OVERVIEW_VOLUME = "statistics_overview_volume"
    const val OVERVIEW_RECORDS = "statistics_overview_records"
    const val METRIC = "statistics_metric"

    /** The metric menu's filter field and its empty answer (N198). */
    const val METRIC_SEARCH = "statistics_metric_search"
    const val METRIC_NO_MATCHES = "statistics_metric_no_matches"
    const val CHART = "statistics_chart"
    const val CHART_FIRST_DATE = "statistics_chart_first_date"
    const val CHART_LAST_DATE = "statistics_chart_last_date"

    /** The tick between the axis's ends (N209). */
    const val CHART_MIDDLE_DATE = "statistics_chart_middle_date"
    const val CHART_MIN = "statistics_chart_min"

    /** The figure's scale: the gutter holding its top, middle and foot (N196). */
    const val CHART_AXIS = "statistics_chart_axis"

    /** The date and value a finger on the figure is over (N200). */
    const val SCRUB_READOUT = "statistics_scrub_readout"

    /** The full-height view of the figure, its own chart and the way back (N201). */
    const val CHART_EXPAND = "statistics_chart_expand"
    const val CHART_EXPANDED = "statistics_chart_expanded"
    const val CHART_EXPANDED_FIGURE = "statistics_chart_expanded_figure"

    /** The expanded view's scrolling body, so a short window is proved to scroll rather than clip (B219). */
    const val CHART_EXPANDED_BODY = "statistics_chart_expanded_body"

    /** The expanded view's name, announced with the dialog (N206). */
    const val CHART_EXPANDED_TITLE = "statistics_chart_expanded_title"
    const val CHART_COLLAPSE = "statistics_chart_collapse"
    const val CHART_MAX = "statistics_chart_max"
    const val CHOOSE_LIFT = "statistics_choose_lift"
    const val LIFT = "statistics_lift"
    const val MEASUREMENTS = "statistics_measurements"

    /** The other screen this tab pushes: adherence over a month (ROADMAP P3.5). */
    const val ADHERENCE = "statistics_adherence"
    const val READINGS_TOGGLE = "statistics_readings_toggle"

    /** The series's average and its fitted slope, which sit above the readings rather than inside (N221). */
    const val SUMMARY_AVERAGE = "statistics_summary_average"
    const val SUMMARY_TREND = "statistics_summary_trend"
    const val MOVING_AVERAGE = "statistics_moving_average"
    const val GOAL = "statistics_goal"
    const val GOAL_SET = "statistics_goal_set"
    const val GOAL_FIELD = "statistics_goal_field"
    const val GOAL_CONFIRM = "statistics_goal_confirm"
    const val GOAL_CLEAR = "statistics_goal_clear"

    /** The energy adjustment a weight trend implies (ROADMAP N98). */
    const val ENERGY = "statistics_energy"
    const val ENERGY_STATEMENT = "statistics_energy_statement"

    /**
     * The derived rate itself (ROADMAP N135).
     *
     * A value rather than the button it used to be: the rate is derived from the goal, so there is
     * nothing here to press — the goal row above is where the number that sets it is chosen.
     */
    const val ENERGY_TARGET = "statistics_energy_target_value"

    /** The band the goal's trend is judged against (ROADMAP N149). */
    const val ENERGY_BAND = "statistics_energy_band"

    /** The date a target weight's rate implies, or its reached line (ROADMAP N151). */
    const val ENERGY_TARGET_DATE = "statistics_energy_target_date"

    /**
     * The weight goal's own row, its kind choices and its field (ROADMAP N135).
     *
     * One tag per kind rather than a position, so a test names the goal it sets — the shape
     * [tape] and the other per-value tags here already use, named by the enum rather than by an
     * ordinal that a reordering would silently change.
     */
    const val WEIGHT_GOAL = "statistics_weight_goal"
    const val WEIGHT_GOAL_SET = "statistics_weight_goal_set"
    const val WEIGHT_GOAL_FIELD = "statistics_weight_goal_field"
    const val WEIGHT_GOAL_CONFIRM = "statistics_weight_goal_confirm"

    fun weightGoalKind(kind: WeightGoalKind) = "statistics_weight_goal_kind_${kind.name.lowercase()}"

    /**
     * The period editor, and the kinds it can state (ROADMAP N215).
     *
     * *No goal* is a kind of period rather than a clear button: a stretch with nothing asked for is what a
     * cleared goal always wrote, and stating it lets one be entered in the past like any other.
     */
    const val GOAL_KIND_NONE = "statistics_goal_kind_none"
    const val GOAL_PERIOD_DIALOG = "statistics_goal_period_dialog"
    const val GOAL_PERIOD_START = "statistics_goal_period_start"
    const val GOAL_PERIOD_END = "statistics_goal_period_end"
    const val GOAL_PERIOD_REMOVE = "statistics_goal_period_remove"
    const val GOAL_PERIOD_ADD = "statistics_goal_period_add"

    /**
     * The goal's history: the way in, the list, and one row per period (ROADMAP N137).
     *
     * Rows are addressed by their **place in the list**, newest first, because a period has no id of its
     * own that a test could name — the thing a row says is its verdict, and the verdict is what a test
     * reads back from the row's own label.
     */
    const val GOAL_HISTORY = "statistics_goal_history"
    const val GOAL_HISTORY_DIALOG = "statistics_goal_history_dialog"
    const val GOAL_HISTORY_CLOSE = "statistics_goal_history_close"

    fun goalOutcome(index: Int) = "statistics_goal_outcome_$index"

    /**
     * The line under the chart naming the plans whose targets are drawn on it (ROADMAP N39).
     *
     * The canvas cannot be asserted and a screen reader cannot read it, so this is the chart's own account
     * of which lines are which — the same reason the axis labels exist.
     */
    const val PLAN_TARGETS = "statistics_plan_targets"

    /**
     * One group's heading in the picker (ROADMAP N130), by the group's own name.
     *
     * The name rather than the enum, so this file keeps its one dependency direction: tags are read by
     * every feature, and a shared file reaching into one of them is the dependency this avoids.
     *
     * The picker had no headings until two series could share a label, so this is what makes the two
     * *Left knee* entries — a workout series and a per-lift one — tellable apart.
     */
    /** One period the moving average can be taken over. */
    fun movingAveragePeriod(period: Int) = "statistics_moving_average_$period"

    /** One reading, addressed by its place in the list — newest first, so 0 is the latest. */
    fun reading(index: Int) = "statistics_reading_$index"

    /** One row of the lift picker, named by the exercise it selects. */
    fun lift(id: String) = "statistics_lift_$id"
    const val CUSTOM_FROM = "statistics_custom_from"
    const val CUSTOM_TO = "statistics_custom_to"
    const val CUSTOM_APPLY = "statistics_custom_apply"

    /** One range chip, and one picker row, named by what they select. */
    fun range(kind: String) = "statistics_range_$kind"

    /** The control the rare ranges sit behind (N197). */
    const val RANGE_MORE = "statistics_range_more"

    /** The group chips the series menu opens on (N199). */
    fun metricGroupChoice(group: String) = "statistics_metric_group_$group"

    /** The one row the joint sites sit behind, and the row that goes back (N199). */
    const val METRIC_JOINT = "statistics_metric_joint"
    const val METRIC_JOINT_BACK = "statistics_metric_joint_back"

    /** A joint's heading over its sides (N205). */
    fun metricJointHeading(joint: String) = "statistics_metric_joint_$joint"

    fun metric(id: String) = "statistics_metric_$id"

    /** A series in the recently used row, named by its key (N212). */
    fun recentSeries(id: String) = "statistics_recent_$id"
}
