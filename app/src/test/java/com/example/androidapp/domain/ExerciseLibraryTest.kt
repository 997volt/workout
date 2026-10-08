package com.example.androidapp.domain

import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The grouped library and the search that reaches through it (ROADMAP N95).
 *
 * Pure JVM, like [ExerciseSearchTest], because the grouping is a function of the library rather than of the
 * screen that draws it — and because the interesting cases (an orphaned child, a fold, a query that matches
 * only the family) are awkward to arrange through a live database and trivial to arrange here.
 */
class ExerciseLibraryTest {

    private val bench = category("cat-bench", "Bench Press")
    private val squat = category("cat-squat", "Squat")

    private val barbellBench = movement("barbell-bench-press", "Barbell Bench Press", parent = "cat-bench")
    private val dumbbellBench = movement("dumbbell-bench-press", "Dumbbell Bench Press", parent = "cat-bench")
    private val speedDay = movement("bench-speed", "Speed Day", parent = "barbell-bench-press")
    private val backSquat = movement("back-squat", "Back Squat", parent = "cat-squat")
    private val deadlift = movement("deadlift", "Deadlift")

    private val library = listOf(bench, squat, barbellBench, dumbbellBench, speedDay, backSquat, deadlift)

    @Test
    fun aCategory_isARowWithItsChildrenUnderIt_atOneDepth() {
        val rows = libraryRows(library, query = "")

        // Every family first, in name order, each with its children under it; then the movements filed
        // under nothing. Grouping the whole library rather than interleaving families among loose rows is
        // what makes the list scan as a shape — a family split by an unrelated row is two halves of one.
        assertThat(rows.map { it.name }).containsExactly(
            "Bench Press",
            "Barbell Bench Press",
            "Speed Day",
            "Dumbbell Bench Press",
            "Squat",
            "Back Squat",
            "Deadlift",
        ).inOrder()

        assertThat(rows.single { it.id == "cat-bench" }.let { it.isCategory to it.childCount })
            .isEqualTo(true to 2)
        // And a movement says how many variations are under it, which is what its own line reads.
        assertThat(rows.single { it.id == "barbell-bench-press" }.childCount).isEqualTo(1)
        // N95's two rules seen from the list: the family holds the exercises at one depth, and an exercise
        // holds its variations at the next. *Speed Day* never says "bench", and it is found by the names it
        // is filed under rather than by its own.
        assertThat(rows.single { it.id == "barbell-bench-press" }.depth).isEqualTo(1)
        assertThat(rows.single { it.id == "bench-speed" }.depth).isEqualTo(2)
        assertThat(rows.single { it.id == "back-squat" }.depth).isEqualTo(1)
        // A movement in no category is a top-level row, not a child of nothing.
        assertThat(rows.single { it.id == "deadlift" }.depth).isEqualTo(0)
    }

    @Test
    fun aFoldedCategory_hidesItsChildrenAndKeepsItsOwnRow() {
        val rows = libraryRows(library, query = "", foldedCategories = setOf("cat-bench"))

        assertThat(rows.map { it.name }).containsExactly(
            "Bench Press",
            "Squat",
            "Back Squat",
            "Deadlift",
        ).inOrder()
        assertThat(rows.single { it.id == "cat-bench" }.isExpanded).isFalse()
        // The count still says what is inside, which is what a folded head's line promises: two movements,
        // and the count is of *its own* children rather than of everything below it.
        assertThat(rows.single { it.id == "cat-bench" }.childCount).isEqualTo(2)
    }

    @Test
    fun aQueryThatNamesOnlyTheFamily_stillFindsItsVariations() {
        // N95's inheritance, on the surface a lifter notices it: "Speed Day" never says "bench", and it is
        // found by the head it is filed under. This is why a query is asked about the family first.
        val rows = libraryRows(library, query = "bench")

        assertThat(rows.map { it.name }).containsExactly(
            "Bench Press",
            "Barbell Bench Press",
            "Speed Day",
            "Dumbbell Bench Press",
        ).inOrder()
    }

    @Test
    fun aCategoryWithNothingMatching_isNotARow() {
        // A family the query does not touch is not part of the answer, even though it is a row when the
        // query is empty.
        val rows = libraryRows(library, query = "deadlift")

        assertThat(rows.map { it.name }).containsExactly("Deadlift")
    }

    @Test
    fun aChildWhoseHeadIsGone_isStillReachable() {
        // A head is soft-deleted, not removed, and N58's rule is that it still names its children — so an
        // orphan is possible and must not vanish with its head. It is drawn at the top level, which is what
        // it was before it was filed.
        val orphan = movement("lonely", "Lonely Lift", parent = "cat-gone")

        val rows = libraryRows(library + orphan, query = "")

        assertThat(rows.map { it.name }).contains("Lonely Lift")
        assertThat(rows.single { it.id == "lonely" }.depth).isEqualTo(0)
    }

    @Test
    fun aCategoryWithNoChildren_isStillARow() {
        // An empty category is a head a lifter just made, waiting for its first movement. Hiding it would
        // mean a category could only be seen once it was already in use.
        val fresh = category("cat-fresh", "Fresh Family")

        val rows = libraryRows(library + fresh, query = "")

        assertThat(rows.single { it.id == "cat-fresh" }.childCount).isEqualTo(0)
        assertThat(rows.single { it.id == "cat-fresh" }.isExpanded).isTrue()
    }

    @Test
    fun aMovementCarriesItsOwnSubtitle() {
        val rows = libraryRows(library, query = "deadlift")

        assertThat(rows.single().subtitle).isEqualTo("Hamstrings · Barbell")
    }

    private fun category(id: String, name: String) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.OTHER,
        movementPattern = MovementPattern.OTHER,
        rowKind = RowKind.CATEGORY,
    )

    private fun movement(id: String, name: String, parent: String? = null) = Exercise(
        id = id,
        name = name,
        primaryMuscle = MuscleGroup.HAMSTRINGS,
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.HINGE,
        parentId = parent,
        rowKind = RowKind.MOVEMENT,
    )

    @Test
    fun aMovement_readsItsFamilysMuscles_live() {
        // ROADMAP N95's inheritance, and the reason nothing is written to the child when it is filed: one
        // fact with one home, so a family cannot disagree with itself.
        assertThat(barbellBench.effectivePrimaryMuscle(library)).isEqualTo(MuscleGroup.CHEST)
        // And the head's own value is untouched by the child reading it.
        assertThat(bench.primaryMuscle).isEqualTo(MuscleGroup.CHEST)

        // Changing the head changes every movement under it, which is what "live" buys.
        val renamed = library.map { if (it.id == "cat-bench") it.copy(primaryMuscle = MuscleGroup.SHOULDERS) else it }

        assertThat(renamed.first { it.id == "barbell-bench-press" }.effectivePrimaryMuscle(renamed))
            .isEqualTo(MuscleGroup.SHOULDERS)
    }

    @Test
    fun aMovementInNoFamily_keepsItsOwnMuscle() {
        // "An exercise in no category carries no pattern" is N96's rule; this is its N95 half — with no head
        // there is nothing to inherit, and the row's own value is the answer rather than a null.
        assertThat(deadlift.effectivePrimaryMuscle(library)).isEqualTo(MuscleGroup.HAMSTRINGS)
    }

    @Test
    fun secondaryMuscles_defaultFromTheHead_untilTheExerciseNamesItsOwn() {
        // "Secondary muscles default from the category and are the exercise's to change" — the default
        // applies exactly while the exercise is silent, which is the one place the child's own value wins.
        val headWithSecondaries = library.map {
            if (it.id == "cat-bench") it.copy(secondaryMuscles = listOf(MuscleGroup.TRICEPS)) else it
        }

        assertThat(
            headWithSecondaries.first { it.id == "dumbbell-bench-press" }
                .effectiveSecondaryMuscles(headWithSecondaries),
        ).containsExactly(MuscleGroup.TRICEPS)

        val namedItsOwn = headWithSecondaries.map {
            if (it.id == "dumbbell-bench-press") it.copy(secondaryMuscles = listOf(MuscleGroup.SHOULDERS)) else it
        }

        assertThat(namedItsOwn.first { it.id == "dumbbell-bench-press" }.effectiveSecondaryMuscles(namedItsOwn))
            .containsExactly(MuscleGroup.SHOULDERS)
    }

    @Test
    fun aRemovedHead_stillNamesItsChild() {
        // N58's rule, inherited by N95: a head that has been removed is still the answer to "what is this
        // filed under". The caller passes the library that includes removed rows, which is why this resolves
        // at all — the observer the list reads deliberately hides them.
        val removedHead = bench.copy(id = "cat-gone", name = "Gone Family")
        val child = movement("filed", "Filed Lift", parent = "cat-gone")

        assertThat(child.headName(listOf(child, removedHead))).isEqualTo("Gone Family")
        assertThat(child.headName(listOf(child))).isNull()
    }

    @Test
    fun aHeadThatSaysNothing_leavesTheMovementsOwnMuscle() {
        // The device found this one: `OTHER` is how this app spells "not specified yet", and it is every new
        // category's value — so reading it literally blanked the muscle of every movement filed under a head
        // the lifter had just made, while the row still held a real answer nobody could see.
        val silentHead = library.map {
            if (it.id == "cat-bench") it.copy(primaryMuscle = MuscleGroup.OTHER) else it
        }

        // The movement's own value — the fixture builds every movement with Hammers, and the point is that the
        // silent head does not override it.
        assertThat(silentHead.first { it.id == "barbell-bench-press" }.effectivePrimaryMuscle(silentHead))
            .isEqualTo(MuscleGroup.HAMSTRINGS)
    }

    @Test
    fun aQueryNamingOnlyAVariation_stillDrawsItUnderItsExercise() {
        // B81: a variation matches on its own name, but its exercise does not, so the family pass used to
        // emit nothing and the loose pass refused the row — an exact name search answered "no exercises
        // match". The exercise above it is drawn to carry it.
        val rows = libraryRows(library, query = "Speed Day")

        assertThat(rows.map { it.name }).containsExactly(
            "Bench Press",
            "Barbell Bench Press",
            "Speed Day",
        ).inOrder()
        assertThat(rows.single { it.id == "barbell-bench-press" }.childCount).isEqualTo(1)
    }

    @Test
    fun aVariationOfAMovementInNoFamily_isDrawnUnderIt() {
        // B82: a custom exercise in no category can have variations, and they were dropped from the library
        // (the flat picker still offered them). The unfiled movement now carries its own variations, one
        // level down, the same shape a category child has.
        val dip = movement("dip", "Dip")
        val weightedDip = movement("weighted-dip", "Weighted Dip", parent = "dip")

        val rows = libraryRows(library + dip + weightedDip, query = "")

        assertThat(rows.single { it.id == "dip" }.childCount).isEqualTo(1)
        assertThat(rows.single { it.id == "weighted-dip" }.depth).isEqualTo(1)
    }

    @Test
    fun aVariation_readsTheMuscleItsExerciseInherits() {
        // B86: the resolver read the parent's stored field, so a silent exercise's muscle came from the
        // category but its variation's did not. The walk reaches the nearest row that states one.
        val silentBench = library.map {
            if (it.id == "barbell-bench-press") it.copy(primaryMuscle = MuscleGroup.OTHER) else it
        }

        assertThat(
            silentBench.first { it.id == "barbell-bench-press" }.effectivePrimaryMuscle(silentBench),
        ).isEqualTo(MuscleGroup.CHEST)
        assertThat(silentBench.first { it.id == "bench-speed" }.effectivePrimaryMuscle(silentBench))
            .isEqualTo(MuscleGroup.CHEST)
    }

    @Test
    fun aWhitespaceOnlyQuery_matchesTheWholeLibrary() {
        // B87: the trim lived in the filter the grouping no longer uses, so a query of only spaces matched
        // nothing where v1.16 answered with everything.
        assertThat(libraryRows(library, query = "   ").map { it.name })
            .isEqualTo(libraryRows(library, query = "").map { it.name })
    }

    @Test
    fun theFlatList_isFilteredByTheQuery_andOffersNoHeads() {
        // B79: the picker's flat branch ignored its query. It is movements only, still, and a variation is
        // found through the exercise it hangs under.
        val rows = libraryRows(library, query = "bench", grouped = false)

        assertThat(rows.map { it.name }).containsExactly(
            "Barbell Bench Press",
            "Dumbbell Bench Press",
            "Speed Day",
        ).inOrder()
        assertThat(rows.none { it.isCategory }).isTrue()
    }

    @Test
    fun theShapeRule_clearsACycle_aThirdLevel_andACategoryUnderACategory() {
        // B92: an imported file is the one place a parent link this app would never write can arrive. A legal
        // link is untouched, and an illegal one is cleared rather than stored and then hidden by the grouping.
        val a = movement("a", "A", parent = "b")
        val b = movement("b", "B", parent = "a")
        val innerCategory = category("cat-inner", "Inner").copy(parentId = "cat-bench")
        val tooDeep = movement("deep", "Deep", parent = "bench-speed")

        val shaped = (library + a + b + innerCategory + tooDeep).withValidLibraryShape().associateBy { it.id }

        assertThat(shaped.getValue("cat-inner").parentId).isNull()
        assertThat(shaped.getValue("a").parentId).isNull()
        assertThat(shaped.getValue("b").parentId).isNull()
        assertThat(shaped.getValue("deep").parentId).isNull()
        // The legal links survive, including the two-level movement-under-movement.
        assertThat(shaped.getValue("barbell-bench-press").parentId).isEqualTo("cat-bench")
        assertThat(shaped.getValue("bench-speed").parentId).isEqualTo("barbell-bench-press")
    }
}
