package com.example.androidapp.ui.workout

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.PlanComparison
import com.example.androidapp.domain.model.Side
import com.example.androidapp.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The review a finished workout gets (ROADMAP N20).
 *
 * The sentence this screen exists for is the delta on the top set — prescribed 6×2 at 92.5,
 * performed 6×2 at 92.5, top single 2.5 over plan — so that is what is asserted, along with
 * the two things the review must not hide: an exercise that was skipped, and a rating the
 * user had just given.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutReviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var dismissals = 0

    private fun show(review: WorkoutReview) {
        composeTestRule.setContent {
            WorkoutReviewDialog(summary = review, onDismiss = { dismissals++ })
        }
    }

    private fun review(
        comparisons: List<PlanComparison> = emptyList(),
        ratings: List<ExerciseRating> = emptyList(),
        totalSets: Int = 8,
        totalReps: Int = 40,
        totalHolds: Int = 0,
    ) = WorkoutReview(
        note = null,
        readinessNote = null,
        totalSets = totalSets,
        totalHolds = totalHolds,
        totalReps = totalReps,
        totalVolumeGrams = 1_000_000L,
        ratings = ratings,
        comparisons = comparisons,
    )

    @Test
    fun aHold_readsAsItsDuration_notAsZeroReps() {
        // B243: the line a lifter saw after a carry said "3×0 at 100 kg" — the "0 × 45" lie the feature
        // removed, reappearing at the finish.
        show(
            review(
                comparisons = listOf(
                    holdComparison(),
                ),
            ),
        )

        composeTestRule.onNodeWithText("3×0:45", substring = true).assertExists()
        composeTestRule.onNodeWithText("3×0 at", substring = true).assertDoesNotExist()
    }

    @Test
    fun aSessionOfHolds_countsHolds_inTheTotals() {
        show(review(totalSets = 3, totalReps = 0, totalHolds = 3))

        composeTestRule.onNodeWithText("Sets 3 · holds 3 · 1000 kg").assertExists()
    }

    private fun holdComparison() = PlanComparison(
        name = "Carry",
        prescribedSets = 3,
        performedSets = 3,
        prescribedReps = 5,
        performedReps = 0,
        prescribedTopWeightGrams = 100_000L,
        performedTopWeightGrams = 100_000L,
        topSetDeltaGrams = 0L,
        performedHoldSeconds = 45,
    )

    @Test
    fun theTotals_areReadable_withUnits() {
        show(review())

        composeTestRule.onNodeWithText("Sets 8 · reps 40 · 1000 kg").assertExists()
    }

    @Test
    fun anOverPlanTopSet_saysSoInKilos() {
        show(
            review(
                comparisons = listOf(
                    PlanComparison(
                        name = "Deadlift",
                        prescribedSets = 1,
                        performedSets = 1,
                        prescribedReps = 1,
                        performedReps = 1,
                        prescribedTopWeightGrams = 90_000L,
                        performedTopWeightGrams = 92_500L,
                        topSetDeltaGrams = 2_500L,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Warm-up sets are not counted.").assertExists()
        composeTestRule.onNodeWithText("prescribed 1×1 at 90 kg").assertExists()
        composeTestRule.onNodeWithText("performed 1×1 at 92.5 kg").assertExists()
        composeTestRule.onNodeWithText("2.5 kg over plan").assertExists()
    }

    @Test
    fun theEffortEachSideWasDoneAt_isReadable_nextToTheWeight() {
        // ROADMAP N131: the plan names one effort per exercise and the log holds one per set, so the
        // prescribed line carries the target, the performed line the working sets' mean, and the
        // verdict the difference — at the half-point resolution an RPE is logged in.
        show(
            review(
                comparisons = listOf(
                    PlanComparison(
                        name = "Squat",
                        prescribedSets = 3,
                        performedSets = 3,
                        prescribedReps = 15,
                        performedReps = 15,
                        prescribedTopWeightGrams = 100_000L,
                        performedTopWeightGrams = 95_000L,
                        topSetDeltaGrams = -5_000L,
                        prescribedRpeHalves = 16,
                        performedAverageRpeHalves = 19,
                        rpeDeltaHalves = 3,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("prescribed 3×15 at 100 kg · RPE 8").assertExists()
        composeTestRule.onNodeWithText("performed 3×15 at 95 kg · RPE 9.5").assertExists()
        composeTestRule.onNodeWithText("5 kg under plan").assertExists()
        composeTestRule.onNodeWithText("RPE 1.5 over plan").assertExists()
    }

    @Test
    fun anEffortThePlanNamedAndNobodyLogged_saysSo_ratherThanReadingAsMatched() {
        // Silence there would read as "as prescribed", which is the one thing it is not.
        show(
            review(
                comparisons = listOf(
                    PlanComparison(
                        name = "Squat",
                        prescribedSets = 3,
                        performedSets = 3,
                        prescribedReps = 15,
                        performedReps = 15,
                        prescribedTopWeightGrams = 100_000L,
                        performedTopWeightGrams = 100_000L,
                        topSetDeltaGrams = 0L,
                        prescribedRpeHalves = 16,
                        performedAverageRpeHalves = null,
                        rpeDeltaHalves = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("prescribed 3×15 at 100 kg · RPE 8").assertExists()
        composeTestRule.onNodeWithText("performed 3×15 at 100 kg").assertExists()
        composeTestRule.onNodeWithText("no RPE recorded").assertExists()
    }

    @Test
    fun aPlanThatNamedNoEffort_saysNothingAboutIt() {
        // The prescribed line loses its effort, and no verdict claims a comparison the plan never asked
        // for. What the sets were actually rated is still worth reading, so it stays on the line.
        show(
            review(
                comparisons = listOf(
                    PlanComparison(
                        name = "Squat",
                        prescribedSets = 1,
                        performedSets = 1,
                        prescribedReps = 5,
                        performedReps = 5,
                        prescribedTopWeightGrams = 100_000L,
                        performedTopWeightGrams = 100_000L,
                        topSetDeltaGrams = 0L,
                        prescribedRpeHalves = null,
                        performedAverageRpeHalves = 17,
                        rpeDeltaHalves = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("prescribed 1×5 at 100 kg").assertExists()
        composeTestRule.onNodeWithText("performed 1×5 at 100 kg · RPE 8.5").assertExists()
        composeTestRule.onNodeWithText("as planned").assertExists()
        composeTestRule.onNodeWithText("RPE as prescribed").assertDoesNotExist()
        composeTestRule.onNodeWithText("no RPE recorded").assertDoesNotExist()
    }

    @Test
    fun aSkippedExercise_isNotQuietlyOmitted() {
        // A review that only lists what was done is a compliment, not a record.
        show(
            review(
                comparisons = listOf(
                    PlanComparison(
                        name = "Squat",
                        prescribedSets = 5,
                        performedSets = 0,
                        prescribedReps = 5,
                        performedReps = 0,
                        prescribedTopWeightGrams = 100_000L,
                        performedTopWeightGrams = null,
                        topSetDeltaGrams = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("performed not performed").assertExists()
        composeTestRule.onNodeWithText("skipped — the plan asked for it").assertExists()
    }

    @Test
    fun aRatingTheUserJustGave_isReadBack() {
        // The ratings used to go nowhere the user could see (N20's complaint).
        show(
            review(
                ratings = listOf(
                    ExerciseRating(
                        name = "Back Squat",
                        muscleFeel = 8,
                        joints = listOf(JointPain(Joint.KNEE, Side.LEFT, 2)),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("How it felt").assertExists()
        composeTestRule.onNodeWithText("Back Squat — felt 8/10, Left knee 2/10").assertExists()
    }

    @Test
    fun theWorstJoint_isTheOneTheReviewNames() {
        // ROADMAP N63: "left knee 6, right knee 2" is one bad knee, and the line names it.
        show(
            review(
                ratings = listOf(
                    ExerciseRating(
                        name = "Back Squat",
                        muscleFeel = 8,
                        joints = listOf(
                            JointPain(Joint.KNEE, Side.LEFT, 6),
                            JointPain(Joint.KNEE, Side.RIGHT, 2),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Back Squat — felt 8/10, Left knee 6/10").assertExists()
    }

    @Test
    fun withoutAPlan_thereIsNoComparisonSection() {
        show(review())

        composeTestRule.onNodeWithText("Plan vs actual").assertDoesNotExist()
    }

    @Test
    fun done_dismisses() {
        show(review())

        composeTestRule.onNodeWithTag(TestTags.SUMMARY_DONE).performClick()

        assertEquals(1, dismissals)
    }
}
