package com.example.androidapp.ui.components

/**
 * Test tags for shared controls (ROADMAP P1.17).
 *
 * Tests target these instead of English literals, so a translated string cannot break a test — and so a
 * test failure means behaviour changed, not wording.
 *
 * Split by area (ROADMAP N285): each screen's tags live in a `*Tags.kt` file beside this one — the area's
 * interface is inherited here, and a group that used to be a nested object is now a property naming the
 * area's object, so `TestTags.Home.START_PLAN` still reads the same.
 */
internal object TestTags :
    WorkoutTags,
    ExerciseTags,
    TemplateTags,
    HomeTags,
    HistoryTags,
    SettingsTags,
    ProgramTags {
    val Progression = ProgressionTags
    val Rating = RatingTags
    val Readiness = ReadinessTags
    val Muscle = MuscleTags
    val Home = HomeCardTags
    val Programs = ProgramsTags
    val Deload = DeloadTags
    val Measurements = MeasurementsTags
    val Statistics = StatisticsTags
    val Adherence = AdherenceTags
}
