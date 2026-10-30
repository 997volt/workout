package com.example.androidapp.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.androidapp.ui.HousekeepingViewModel
import com.example.androidapp.ui.adherence.AdherenceRoute
import com.example.androidapp.ui.components.HousekeepingAlert
import com.example.androidapp.ui.components.LocalWeightUnit
import com.example.androidapp.ui.exercises.ExerciseDetailRoute
import com.example.androidapp.ui.exercises.ExerciseLibraryRoute
import com.example.androidapp.ui.history.WorkoutDetailRoute
import com.example.androidapp.ui.history.WorkoutHistoryRoute
import com.example.androidapp.ui.home.WorkoutsHomeRoute
import com.example.androidapp.ui.measurements.MeasurementsRoute
import com.example.androidapp.ui.programs.ProgramBalanceRoute
import com.example.androidapp.ui.programs.ProgramDeloadRoute
import com.example.androidapp.ui.programs.ProgramEditorRoute
import com.example.androidapp.ui.programs.ProgramsRoute
import com.example.androidapp.ui.settings.DiagnosticsRoute
import com.example.androidapp.ui.settings.SettingsRoute
import com.example.androidapp.ui.settings.WeightUnitViewModel
import com.example.androidapp.ui.statistics.StatisticsRoute
import com.example.androidapp.ui.templates.TemplateEditorRoute
import com.example.androidapp.ui.templates.TemplatesRoute
import com.example.androidapp.ui.workout.ActiveWorkoutRoute
import com.example.androidapp.ui.workout.ExercisePickerRoute

/**
 * The app's single navigation graph (ROADMAP F2, P1.2).
 *
 * Destinations are registered by route *type*, so [ExerciseDetail]'s
 * `exerciseId` is read back with `SavedStateHandle.toRoute()` in its ViewModel
 * instead of being plucked out of a stringly-typed bundle.
 *
 * The graph is grouped into one extension per area rather than listed flat here:
 * a single function holding every destination stops being readable long before it
 * stops compiling, and the group is usually what a new screen belongs to.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    // A default so a test can supply its own (the compose-rules ViewModelInjection rule); the shell is
    // the one composable that owns the housekeeping notice, so the ViewModel belongs to it.
    housekeepingViewModel: HousekeepingViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    // A pushed detail keeps its tab highlighted, which is what makes the bar a map of where you are
    // rather than five buttons that forget the moment you open something. The rule lives in
    // `TabHighlight` rather than here so it can be asserted without the application graph (B264).
    val highlight = rememberSaveable(saver = TabHighlight.Saver) { TabHighlight() }
    LaunchedEffect(destination?.route) { highlight.onRoute(destination?.route) }

    // The unit every weight is read in, provided once here rather than threaded through every
    // screen between this and the field that shows a number (ROADMAP N64).
    val weightUnit by hiltViewModel<WeightUnitViewModel>().weightUnit.collectAsStateWithLifecycle()

    // A housekeeping pass that could not finish is said once, wherever the lifter is (B324): the state is
    // produced by the database's open, before any screen has a ViewModel, and the retry is the same pass.
    val repairFailure by housekeepingViewModel.failure.collectAsStateWithLifecycle()
    val repairing by housekeepingViewModel.retrying.collectAsStateWithLifecycle()
    HousekeepingAlert(
        failure = repairFailure,
        retrying = repairing,
        onRetry = housekeepingViewModel::retry,
        onDismiss = housekeepingViewModel::dismiss,
    )

    val showBar = showsTabBar(destination?.route)
    Scaffold(
        // The shell owns the bar's height and nothing else: each screen keeps its own top bar and its own
        // status-bar inset, which is what stops the two from padding the same content twice.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                AppTabBar(
                    selected = highlight.selected,
                    // The tap writes the highlight before the navigation does (N223): a tab restored at a
                    // detail settles on a route that names no tab, so the route cannot say which tab is
                    // showing, and the switch has to be told what was showing to tell a re-tap from a switch.
                    onSelect = { tab -> navController.switchTab(tab, selected = highlight.onTap(tab)) },
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        CompositionLocalProvider(LocalWeightUnit provides weightUnit) {
            NavHost(
                navController = navController,
                startDestination = WorkoutsHome,
                // Applied once here instead of by every screen (ROADMAP N34).
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            ) {
                homeDestinations(navController)
                workoutDestinations(navController)
                templateDestinations(navController)
                programDestinations(navController)
                historyDestinations(navController)
            }
        }
    }
}

/** Home, the exercise library, one exercise's detail, and the app's settings. */
private fun NavGraphBuilder.homeDestinations(navController: NavHostController) {
    composable<Measurements> { entry ->
        MeasurementsRoute(
            onBack = { navController.popBackStack() },
            openDaily = entry.toRoute<Measurements>().openDaily,
        )
    }

    // Adherence is pushed from Statistics, next to Measurements: both answer a question the tab
    // raises, and neither is a sixth tab (ROADMAP P3.5, N34).
    composable<Adherence> {
        AdherenceRoute(onBack = { navController.popBackStack() })
    }

    composable<Settings> {
        SettingsRoute(
            onBack = { navController.popBackStack() },
            onOpenDiagnostics = { navController.navigate(Diagnostics) },
        )
    }

    composable<Diagnostics> {
        DiagnosticsRoute(onBack = { navController.popBackStack() })
    }

    composable<WorkoutsHome> {
        WorkoutsHomeRoute(
            onStartWorkout = { navController.navigate(ActiveWorkout()) },
            // The start action's other half: home offers the choice, the template
            // list makes it (ROADMAP N3).
            onOpenTemplates = { navController.navigate(WorkoutTemplates) },
            // Today's plan starts directly, with the plan's id and the slot it was scheduled as —
            // the same destination the template list reaches (ROADMAP N16, P3.8).
            onStartTemplate = { templateId, slotId ->
                navController.navigate(ActiveWorkout(templateId = templateId, slotId = slotId))
            },
            // The schedule the today's-plan section is read from (ROADMAP P3.3), now a row in the
            // body rather than the overflow it got lost in (N42, N102).
            onOpenPrograms = { navController.navigate(Programs) },
            // Home is where the weight the chart reads gets written, so the way in is here too (N102) —
            // and while today has none, the row above it opens the daily form directly (N114).
            onOpenMeasurements = { navController.navigate(Measurements()) },
            onMeasureToday = { navController.navigate(Measurements(openDaily = true)) },
        )
    }

    composable<Statistics> {
        StatisticsRoute(
            // Recording and reading are different jobs, so the measurements screen stays a destination of
            // its own — reached from here, which is the tab that answers how everything is going (N35).
            onOpenMeasurements = { navController.navigate(Measurements()) },
            // The aggregate over the session-level plan-versus-actual: how often the scheduled
            // days happened (ROADMAP P3.5).
            onOpenAdherence = { navController.navigate(Adherence) },
        )
    }

    composable<ExerciseLibrary> {
        ExerciseLibraryRoute(
            onExerciseClick = { exerciseId ->
                navController.navigate(ExerciseDetail(exerciseId))
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable<ExerciseDetail> {
        ExerciseDetailRoute(
            onBack = { navController.popBackStack() },
            // The lift's own trends (ROADMAP N17), reached from the library.
            onOpenTrends = { exerciseId -> navController.navigate(Statistics(exerciseId)) },
        )
    }
}

/** The in-progress workout and the picker it opens. */
private fun NavGraphBuilder.workoutDestinations(navController: NavHostController) {
    composable<ActiveWorkout> {
        ActiveWorkoutRoute(
            onAddExercise = { navController.navigate(ExercisePicker()) },
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
            // A **push**, so the session, its rest timer and the draft in the next set's fields are all
            // exactly as they were and one back tap returns to them (ROADMAP N234).
            onOpenExercise = { exerciseId -> navController.navigate(ExerciseDetail(exerciseId)) },
        )
    }

    composable<ExercisePicker> {
        ExercisePickerRoute(
            onAddExercise = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}

/** Templates: the list and one template's editor (ROADMAP N3). */
private fun NavGraphBuilder.templateDestinations(navController: NavHostController) {
    composable<WorkoutTemplates> {
        TemplatesRoute(
            onOpenTemplate = { templateId -> navController.navigate(TemplateEditor(templateId)) },
            // Starting from a template is a navigation, not a write: the workout
            // screen opens the session and seeds it, so seeding cannot happen
            // twice and cannot happen with no screen to show it.
            onStartTemplate = { templateId ->
                navController.navigate(ActiveWorkout(templateId = templateId))
            },
            onBack = { navController.popBackStack() },
            // The sibling list (ROADMAP N93). Nothing about the start is said here: the rows below
            // carry the withholding themselves while a session is open (N78, B43).
            onOpenPrograms = { navController.navigate(Programs) },
        )
    }

    composable<TemplateEditor> {
        TemplateEditorRoute(
            onAddExercise = { templateId ->
                navController.navigate(ExercisePicker(templateId = templateId))
            },
            onBack = { navController.popBackStack() },
            // The lift behind a planned row (N121): the same destination the library reaches, so a plan and
            // the library cannot show one lift two ways.
            onOpenExercise = { exerciseId -> navController.navigate(ExerciseDetail(exerciseId)) },
        )
    }
}

/** Programs: the list and one program's editor (ROADMAP P3.3). */
private fun NavGraphBuilder.programDestinations(navController: NavHostController) {
    composable<Programs> {
        ProgramsRoute(
            onOpenProgram = { programId -> navController.navigate(ProgramEditor(programId)) },
            onBack = { navController.popBackStack() },
            // The way across (ROADMAP N93): the two plan screens were siblings with a trip home
            // between them.
            onOpenTemplates = { navController.navigate(WorkoutTemplates) },
        )
    }

    composable<ProgramBalance> {
        ProgramBalanceRoute(onBack = { navController.popBackStack() })
    }

    composable<ProgramDeload> {
        ProgramDeloadRoute(
            onBack = { navController.popBackStack() },
            onOpenWorkouts = { navController.navigate(WorkoutTemplates) },
        )
    }

    composable<ProgramEditor> { entry ->
        val programId = entry.toRoute<ProgramEditor>().programId
        ProgramEditorRoute(
            onBack = { navController.popBackStack() },
            // The program that is open, read as a week (N119).
            onOpenBalance = { navController.navigate(ProgramBalance(programId)) },
            // The same program's deload week, built from its own workouts (N147).
            onOpenDeload = { navController.navigate(ProgramDeload(programId)) },
            // The way across a program's own schedule: a slot names a workout, and this reaches the workout
            // itself (N120). Templates is where that editor otherwise lives.
            onOpenTemplate = { templateId -> navController.navigate(TemplateEditor(templateId)) },
        )
    }
}

/** Finished workouts and one workout's detail. */
private fun NavGraphBuilder.historyDestinations(navController: NavHostController) {
    composable<WorkoutHistory> {
        WorkoutHistoryRoute(
            onOpenWorkout = { sessionId -> navController.navigate(WorkoutDetail(sessionId)) },
            // The entry point the home-screen repeat button gave up (ROADMAP N48): the workout the
            // row names, copied into a fresh session.
            onRepeatWorkout = { sessionId ->
                navController.navigate(ActiveWorkout(repeatSessionId = sessionId))
            },
            // The missed day's own start, which the question can answer instead of the repeat (B271).
            onStartTemplate = { templateId, slotId ->
                navController.navigate(ActiveWorkout(templateId = templateId, slotId = slotId))
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable<WorkoutDetail> {
        WorkoutDetailRoute(
            onBack = { navController.popBackStack() },
            // The lift you just did, tapped to see how it is going (ROADMAP N17).
            onOpenExerciseTrends = { exerciseId ->
                navController.navigate(Statistics(exerciseId))
            },
            // Saving a workout as a plan offers to go straight to it (ROADMAP N31) — to the editor,
            // not the list, because the point of the offer is the plan that was just made.
            onOpenTemplate = { templateId -> navController.navigate(TemplateEditor(templateId)) },
            // Scheduling the copy lands in the program it was added to, where its days are set (P3.18).
            onOpenProgram = { programId -> navController.navigate(ProgramEditor(programId)) },
        )
    }
}
