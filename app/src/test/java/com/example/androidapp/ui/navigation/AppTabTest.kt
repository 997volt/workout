package com.example.androidapp.ui.navigation

import androidx.core.os.bundleOf
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.theme.AndroidAppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The bottom bar and the rules behind it (ROADMAP N34).
 *
 * The matching is on a route *string* rather than a `NavDestination`, which is what lets the rules be
 * tested without standing up a graph — and the graph test at the end checks the part that is genuinely
 * about navigation: that a tab switch leaves a back entry behind rather than a trail of tabs.
 */
@RunWith(AndroidJUnit4::class)
class AppTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val history = WorkoutHistory.serializer().descriptor.serialName
    private val library = ExerciseLibrary.serializer().descriptor.serialName
    private val workouts = WorkoutsHome.serializer().descriptor.serialName
    private val statistics = Statistics.serializer().descriptor.serialName
    private val detail = ExerciseDetail.serializer().descriptor.serialName

    @Test
    fun theBar_offersFiveNamedTabs_inTheEnumsOrder_andSaysWhichIsSelected() {
        composeTestRule.setContent { AndroidAppTheme { AppTabBar(selected = AppTab.HISTORY, onSelect = {}) } }

        // The bar's read order is the enum's order (N94), so the list below is asserted rather than
        // merely iterated: a screen reader announces Statistics first and Workouts third.
        assertThat(AppTab.entries.map { it.testTag }).containsExactly(
            TestTags.TAB_STATISTICS,
            TestTags.TAB_HISTORY,
            TestTags.TAB_WORKOUTS,
            TestTags.TAB_LIBRARY,
            TestTags.TAB_SETTINGS,
        ).inOrder()

        AppTab.entries.forEach { tab ->
            composeTestRule.onNodeWithTag(tab.testTag).assertExists()
        }

        // The state a screen reader announces, which is the difference between a map and five squares.
        composeTestRule.onNodeWithTag(TestTags.TAB_HISTORY).assertIsSelected()
        composeTestRule.onNodeWithTag(TestTags.TAB_WORKOUTS).assertIsNotSelected()
    }

    @Test
    fun theAppOpensOnWorkouts_thoughTheBarPutsItThird() {
        // N94 separates the two things the old order ran together: the swap moves Workouts to the third
        // place, and the tab the app opens on is `startDestination` — which the swap does not touch.
        assertThat(AppTab.entries.first()).isEqualTo(AppTab.STATISTICS)
        assertThat(AppTab.entries[2]).isEqualTo(AppTab.WORKOUTS)
        assertThat(AppTab.forRoute(WorkoutsHome.serializer().descriptor.serialName)).isEqualTo(AppTab.WORKOUTS)
    }

    @Test
    fun choosingATab_reportsThatTab() {
        var chosen: AppTab? = null
        composeTestRule.setContent {
            AndroidAppTheme { AppTabBar(selected = AppTab.WORKOUTS, onSelect = { chosen = it }) }
        }

        composeTestRule.onNodeWithTag(TestTags.TAB_LIBRARY).performClick()

        assertThat(chosen).isEqualTo(AppTab.LIBRARY)
    }

    @Test
    fun aTabRootMapsToItsTab_andAPushedRouteDoesNot() {
        assertThat(AppTab.forRoute(history)).isEqualTo(AppTab.HISTORY)
        assertThat(AppTab.forRoute(workouts)).isEqualTo(AppTab.WORKOUTS)

        // WorkoutDetail is pushed from a tab, so it is not a tab: a bar that lit up for it would be
        // claiming the detail is a destination of its own.
        val detail = WorkoutDetail("s1").let { WorkoutDetail.serializer().descriptor.serialName }
        assertThat(AppTab.forRoute(detail)).isNull()
        assertThat(AppTab.forRoute(null)).isNull()
    }

    @Test
    fun aRouteCarryingArguments_stillMapsToItsTab() {
        // The pattern a route with arguments compiles to has them appended, which is why the match is a
        // prefix: an equality would miss exactly the routes that carry data.
        assertThat(AppTab.STATISTICS.isCurrent("$statistics?from={from}")).isTrue()
    }

    @Test
    fun theBarHidesDuringAWorkout_andOnlyThere() {
        val workout = ActiveWorkout.serializer().descriptor.serialName
        val picker = ExercisePicker.serializer().descriptor.serialName

        assertWithMessage("a live set logger is not a tab").that(showsTabBar(workout)).isFalse()
        assertWithMessage("nor is the picker one step earlier").that(showsTabBar("$picker?templateId={templateId}"))
            .isFalse()

        assertWithMessage("a tab root keeps it").that(showsTabBar(history)).isTrue()
        val detail = "${WorkoutDetail.serializer().descriptor.serialName}?sessionId={sessionId}"
        assertWithMessage("and so does a detail pushed inside a tab").that(showsTabBar(detail)).isTrue()
        assertWithMessage("and before the graph settles").that(showsTabBar(null)).isTrue()
    }

    @Test
    fun switchingTabs_leavesOneBackEntry_andBackReturnsToWorkouts() {
        val navController = graph()

        composeTestRule.runOnIdle { navController.switchTab(AppTab.HISTORY, selected = AppTab.WORKOUTS) }
        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(history)

        composeTestRule.runOnIdle { navController.switchTab(AppTab.LIBRARY, selected = AppTab.HISTORY) }
        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(library)

        // Coming back finds the tab rather than rebuilding a path through the one in between.
        composeTestRule.runOnIdle { navController.switchTab(AppTab.HISTORY, selected = AppTab.LIBRARY) }
        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(history)

        // And back from a tab root lands on Workouts, not on the tab visited before it.
        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(workouts)
    }

    @Test
    fun reTappingTheActiveTab_returnsToItsRoot() {
        // N223: the tab you are in is also the way back to its front page. Library → an exercise → the Library
        // button used to leave the exercise on screen.
        val navController = graph()

        composeTestRule.runOnIdle { navController.switchTab(AppTab.LIBRARY, selected = AppTab.WORKOUTS) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { navController.navigate(ExerciseDetail("e1")) }
        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).startsWith(detail)

        // The bar still says Library while the detail is open, which is what makes this a re-tap.
        composeTestRule.runOnIdle { navController.switchTab(AppTab.LIBRARY, selected = AppTab.LIBRARY) }
        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(library)

        // And back from the floored root still reaches Workouts, as it did before.
        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(workouts)
    }

    @Test
    fun reTappingTheRoot_changesNothing() {
        // A tap on the button with nothing above it must not rebuild the screen under it: the entry keeps its
        // id, so the list's scroll and the ViewModel's state are the ones already on screen.
        val navController = graph()

        composeTestRule.runOnIdle { navController.switchTab(AppTab.STATISTICS, selected = AppTab.WORKOUTS) }
        composeTestRule.waitForIdle()
        val before = navController.currentBackStackEntry?.id

        composeTestRule.runOnIdle { navController.switchTab(AppTab.STATISTICS, selected = AppTab.STATISTICS) }
        composeTestRule.waitForIdle()

        assertThat(navController.currentBackStackEntry?.id).isEqualTo(before)
        assertThat(navController.currentDestination?.route).startsWith(statistics)
    }

    @Test
    fun reTappingStatisticsOpenedFromAWorkout_reachesTheTabsOwnStack() {
        // A Statistics pushed from a workout is the destination the bar highlights as Statistics, but it is a
        // reading of one lift rather than the tab's front page: tapping the tab brings the tab's own stack back
        // instead of leaving that tap doing nothing.
        val navController = graph()

        composeTestRule.runOnIdle { navController.switchTab(AppTab.HISTORY, selected = AppTab.WORKOUTS) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { navController.navigate(WorkoutDetail("s1")) }
        composeTestRule.runOnIdle { navController.navigate(Statistics(exerciseId = "e1")) }
        composeTestRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.arguments?.getString("exerciseId")).isEqualTo("e1")

        // The bar says Statistics for the pushed screen, so the tap is a re-tap on the tab it highlights.
        composeTestRule.runOnIdle { navController.switchTab(AppTab.STATISTICS, selected = AppTab.STATISTICS) }
        composeTestRule.waitForIdle()

        assertThat(navController.currentDestination?.route).startsWith(statistics)
        assertWithMessage("the lift it was opened for is not the tab's front page")
            .that(navController.currentBackStackEntry?.arguments?.getString("exerciseId"))
            .isNull()
    }

    @Test
    fun everyTabsOwnRoute_isItsFrontPage_soAReTapChangesNothing() {
        // B265: the question "is the entry on top this tab's front page" is now asked of the tab's own route
        // instead of "does any argument hold a value", and this pins the answer for every tab the bar offers —
        // a sixth tab root with a parameter is what would make this test the place the rule fails.
        val navController = graph()

        AppTab.entries.forEach { tab ->
            val other = AppTab.entries.first { it != tab }
            composeTestRule.runOnIdle { navController.switchTab(tab, selected = other) }
            composeTestRule.waitForIdle()
            val before = navController.currentBackStackEntry

            composeTestRule.runOnIdle { navController.switchTab(tab, selected = tab) }
            composeTestRule.waitForIdle()
            val after = navController.currentBackStackEntry

            assertWithMessage("${tab.name}'s own route survives a re-tap")
                .that(after?.id)
                .isEqualTo(before?.id)
            assertWithMessage("and it is still the same screen")
                .that(after?.destination?.route)
                .isEqualTo(before?.destination?.route)
        }
    }

    @Test
    fun aRootWithANonNullDefault_isItsOwnFrontPage() {
        // B265, the case the earlier rule got wrong. `openDaily` stands in for a tab root whose parameter has a
        // non-null default — `Measurements` is that shape already: asking for "any non-null argument" would read
        // the front page as a pushed screen and take the switch branch on every re-tap.
        val openDaily = mapOf(
            "openDaily" to NavArgument.Builder().setType(NavType.BoolType).setDefaultValue(false).build(),
        )

        assertThat(isFrontPage(AppTab.STATISTICS, statistics, openDaily, bundleOf("openDaily" to false))).isTrue()
        assertWithMessage("a value of its own is what makes it a pushed screen")
            .that(isFrontPage(AppTab.STATISTICS, statistics, openDaily, bundleOf("openDaily" to true)))
            .isFalse()
    }

    @Test
    fun theLiftReading_isNotTheStatisticsFrontPage() {
        val exerciseId = mapOf(
            "exerciseId" to NavArgument.Builder().setType(NavType.StringType).setIsNullable(true).build(),
        )

        assertWithMessage("the series index, with its argument at the default")
            .that(isFrontPage(AppTab.STATISTICS, statistics, exerciseId, bundleOf("exerciseId" to null)))
            .isTrue()
        assertWithMessage("one lift, which is what falls through to the tab's own stack")
            .that(isFrontPage(AppTab.STATISTICS, statistics, exerciseId, bundleOf("exerciseId" to "e1")))
            .isFalse()
        assertWithMessage("and a screen that is not the tab at all is never its front page")
            .that(isFrontPage(AppTab.STATISTICS, detail, exerciseId, bundleOf("exerciseId" to "e1")))
            .isFalse()
    }

    /** The five tab roots and two pushed screens, which is what the rules below are about. */
    private fun graph(): NavHostController {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = WorkoutsHome) {
                composable<WorkoutsHome> { Text("workouts") }
                composable<WorkoutHistory> { Text("history") }
                composable<ExerciseLibrary> { Text("library") }
                composable<Statistics> { Text("statistics") }
                composable<Settings> { Text("settings") }
                composable<ExerciseDetail> { Text("exercise") }
                composable<WorkoutDetail> { Text("workout") }
            }
        }
        return navController
    }
}
