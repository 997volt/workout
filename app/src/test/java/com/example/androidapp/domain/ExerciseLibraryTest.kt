package com.example.androidapp.domain

import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The grouped library and the search that reaches through it (ROADMAP N95, N110).
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
    private val speedDay = movement("bench-speed", "Speed Day", parent = "cat-bench")
    private val backSquat = movement("back-squat", "Back Squat", parent = "cat-squat")
    private val deadlift = movement("deadlift", "Deadlift")

    private val library = listOf(bench, squat, barbellBench, dumbbellBench, speedDay, backSquat, deadlift)

    @Test
    fun aCategory_isARowWithTheMovementsFiledUnderIt_atOneDepth() {
        val rows = libraryRows(library, query = "")

        // Every category first, in name order, with its movements under it; then the movements filed under
        // nothing. Grouping the whole library rather than interleaving categories among loose rows is what
        // makes the list scan as a shape — a family split by an unrelated row is two halves of one.
        assertThat(rows.map { it.name }).containsExactly(
            "Bench Press",
            "Barbell Bench Press",
            "Dumbbell Bench Press",
            "Speed Day",
            "Squat",
            "Back Squat",
            "Deadlift",
        ).inOrder()

        assertThat(rows.single { it.id == "cat-bench" }.let { it.isCategory to it.childCount })
            .isEqualTo(true to 3)
        // **One level and no deeper** (N110): a movement is a leaf, so it holds no count of its own.
        assertThat(rows.single { it.id == "barbell-bench-press" }.let { it.depth to it.childCount })
            .isEqualTo(1 to 0)
        assertThat(rows.single { it.id == "bench-speed" }.depth).isEqualTo(1)
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
        // The count still says what is inside, which is what a folded category's line promises.
        assertThat(rows.single { it.id == "cat-bench" }.childCount).isEqualTo(3)
    }

    @Test
    fun aQueryThatNamesOnlyTheFamily_stillFindsItsMovements() {
        // N95's inheritance, on the surface a lifter notices it: *Speed Day* never says "bench", and it is
        // found by the category it is filed under. This is why a query is asked about the family first.
        val rows = libraryRows(library, query = "bench")

        assertThat(rows.map { it.name }).containsExactly(
            "Bench Press",
            "Barbell Bench Press",
            "Dumbbell Bench Press",
            "Speed Day",
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
    fun aChildWhoseCategoryIsGone_isStillReachable() {
        // A category is soft-deleted, not removed, and N58's rule is that it still names its children — so an
        // orphan is possible and must not vanish with its head. It is drawn at the top level, which is what
        // it was before it was filed.
        val orphan = movement("lonely", "Lonely Lift", parent = "cat-gone")

        val rows = libraryRows(library + orphan, query = "")

        assertThat(rows.map { it.name }).contains("Lonely Lift")
        assertThat(rows.single { it.id == "lonely" }.depth).isEqualTo(0)
    }

    @Test
    fun aCategoryWithNoChildren_isStillARow() {
        // An empty category is a category a lifter just made, waiting for its first movement. Hiding it would
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
    fun aMovement_readsItsCategorysMuscles_live() {
        // ROADMAP N110's inheritance under N103's one rule: a movement that **states nothing** reads its
        // category's, live — one fact with one home, so a family cannot disagree with itself.
        val silent = library.map {
            if (it.id == "barbell-bench-press") it.copy(primaryMuscle = null) else it
        }

        assertThat(silent.first { it.id == "barbell-bench-press" }.effectivePrimaryMuscle(silent))
            .isEqualTo(MuscleGroup.CHEST)
        // And the category's own value is untouched by the child reading it.
        assertThat(silent.first { it.id == "cat-bench" }.primaryMuscle).isEqualTo(MuscleGroup.CHEST)

        // Changing the category changes every movement under it that states none, which is what "live" buys.
        val renamed = silent.map {
            if (it.id == "cat-bench") it.copy(primaryMuscle = MuscleGroup.SHOULDERS) else it
        }

        assertThat(renamed.first { it.id == "barbell-bench-press" }.effectivePrimaryMuscle(renamed))
            .isEqualTo(MuscleGroup.SHOULDERS)
    }

    @Test
    fun aMovementInNoCategory_keepsItsOwnMuscle() {
        // "An exercise in no category carries no pattern" is N96's rule; this is its N95 half — with no
        // category there is nothing to inherit, and the row's own value is the answer rather than a null.
        assertThat(deadlift.effectivePrimaryMuscle(library)).isEqualTo(MuscleGroup.HAMSTRINGS)
    }

    @Test
    fun secondaryMuscles_defaultFromTheCategory_untilTheMovementNamesItsOwn() {
        // "Secondary muscles default from the category and are the movement's to change" — the default applies
        // exactly while the movement is silent, which is the one place the child's own value wins.
        val categoryWithSecondaries = library.map {
            if (it.id == "cat-bench") it.copy(secondaryMuscles = listOf(MuscleGroup.TRICEPS)) else it
        }

        assertThat(
            categoryWithSecondaries.first { it.id == "dumbbell-bench-press" }
                .effectiveSecondaryMuscles(categoryWithSecondaries),
        ).containsExactly(MuscleGroup.TRICEPS)

        val namedItsOwn = categoryWithSecondaries.map {
            if (it.id == "dumbbell-bench-press") it.copy(secondaryMuscles = listOf(MuscleGroup.SHOULDERS)) else it
        }

        assertThat(namedItsOwn.first { it.id == "dumbbell-bench-press" }.effectiveSecondaryMuscles(namedItsOwn))
            .containsExactly(MuscleGroup.SHOULDERS)
    }

    @Test
    fun aMovementNeverBorrowsTheCategorysEquipment() {
        // ROADMAP N110: a family spans equipment — the lat pulldown family alone holds a cable, a bodyweight
        // and a machine movement — so a category is not the fact's home. A category that states one is not
        // read, and a silent movement reads as no equipment at all rather than as the category's.
        val library = listOf(
            category("cat-bench", "Bench Press").copy(equipment = Equipment.BARBELL),
            movement("silent", "Silent", parent = "cat-bench").copy(primaryMuscle = null, equipment = null),
        )

        val row = libraryRows(library, query = "").single { it.id == "silent" }

        // The muscle is the category's — "Chest" alone is the proof, because the category's barbell is not
        // on the line.
        assertThat(row.subtitle).isEqualTo("Chest")
    }

    @Test
    fun aRemovedCategory_stillNamesItsChild() {
        // N58's rule, inherited by N95: a category that has been removed is still the answer to "what is this
        // filed under". The caller passes the library that includes removed rows, which is why this resolves
        // at all — the observer the list reads deliberately hides them.
        val removedCategory = bench.copy(id = "cat-gone", name = "Gone Family")
        val child = movement("filed", "Filed Lift", parent = "cat-gone")

        assertThat(child.headName(listOf(child, removedCategory))).isEqualTo("Gone Family")
        assertThat(child.headName(listOf(child))).isNull()
    }

    @Test
    fun aCategoryThatSaysNothing_leavesTheMovementsOwnMuscle() {
        // The device found this one: `OTHER` is how this app spells "not specified yet", and it is every new
        // category's value — so reading it literally blanked the muscle of every movement filed under a
        // category the lifter had just made, while the row still held a real answer nobody could see.
        val silentCategory = library.map {
            if (it.id == "cat-bench") it.copy(primaryMuscle = MuscleGroup.OTHER) else it
        }

        // The movement's own value — the fixture builds every movement with Hammers — wins, because a stated
        // value is the row's own answer whatever sits above it (N103).
        assertThat(silentCategory.first { it.id == "barbell-bench-press" }.effectivePrimaryMuscle(silentCategory))
            .isEqualTo(MuscleGroup.HAMSTRINGS)
    }

    @Test
    fun aQueryNamingOnlyAMovement_stillDrawsItUnderItsCategory() {
        // B81: a movement matches on its own name while its category does not, so an exact name search must
        // still draw the category above it rather than answering "no exercises match".
        val rows = libraryRows(library, query = "Speed Day")

        assertThat(rows.map { it.name }).containsExactly(
            "Bench Press",
            "Speed Day",
        ).inOrder()
        // The family's own size, and how much of it the query is showing (N225): the head states a fact
        // about the library, not about the search.
        assertThat(rows.single { it.id == "cat-bench" }.childCount).isEqualTo(3)
        assertThat(rows.single { it.id == "cat-bench" }.matchedChildCount).isEqualTo(1)
        assertThat(rows.single { it.id == "cat-bench" }.isNarrowed).isTrue()
    }

    @Test
    fun aFamilyWithNoSearch_statesOneNumber() {
        // The other half of N225: the two counts agree when nothing is narrowing the list, which is what lets
        // the head print a single number without knowing whether a query is on.
        val rows = libraryRows(library, query = "")

        assertThat(rows.single { it.id == "cat-bench" }.matchedChildCount).isEqualTo(3)
        assertThat(rows.single { it.id == "cat-bench" }.isNarrowed).isFalse()
    }

    @Test
    fun aFamilyItsOwnNameMatched_showsItsWholeFamily() {
        // N95's inheritance, read on the count (N225): a query that names the family reaches every movement
        // filed under it, so the head shows all of them and has no second number to state.
        val fresh = category("cat-fresh", "Fresh Family")
        val rows = libraryRows(
            library + fresh + movement("fresh-lift", "Lift", parent = "cat-fresh"),
            query = "fresh",
        )

        val head = rows.single { it.id == "cat-fresh" }
        assertThat(head.childCount).isEqualTo(1)
        assertThat(head.matchedChildCount).isEqualTo(1)
        assertThat(head.isNarrowed).isFalse()
        assertThat(rows.map { it.name }).containsExactly("Fresh Family", "Lift").inOrder()
    }

    @Test
    fun aQuery_opensTheFamilyItReachesInto_ratherThanAnsweringWithAFoldedHead() {
        // N95: the fold is how the library is *browsed*, and a search is not browsing. A query that only a
        // movement matches must show that movement — answering with a head that has to be opened first is the
        // wrong answer to "where is Back Squat", and it is what a folded default would otherwise do. Both
        // grouped screens want this, so it is `libraryRows`' rule rather than either screen's.
        val rows = libraryRows(library, query = "Speed Day", foldedCategories = setOf("cat-bench"))

        assertThat(rows.map { it.name }).containsExactly("Bench Press", "Speed Day").inOrder()
        assertThat(rows.single { it.id == "cat-bench" }.isExpanded).isTrue()
    }

    @Test
    fun aBlankQuery_leavesTheFoldsWhereTheLifterPutThem() {
        // The other half: a query of spaces is not a search (B87 trims it), so the folds stand — otherwise
        // merely clearing the field would unfold the whole library.
        val rows = libraryRows(library, query = "   ", foldedCategories = setOf("cat-bench"))

        assertThat(rows.single { it.id == "cat-bench" }.isExpanded).isFalse()
        assertThat(rows.map { it.name }).doesNotContain("Speed Day")
    }

    @Test
    fun aWhitespaceOnlyQuery_matchesTheWholeLibrary() {
        // B87: the trim lived in the filter the grouping no longer uses, so a query of only spaces matched
        // nothing where v1.16 answered with everything.
        assertThat(libraryRows(library, query = "   ").map { it.name })
            .isEqualTo(libraryRows(library, query = "").map { it.name })
    }

    @Test
    fun theShapeRule_flattensAMovementUnderAMovement_andClearsACycle() {
        // B92, N110: an imported file is the one place a link this app would never write can arrive. A
        // movement filed under a movement is what every variation in an older file is, so it is **folded onto
        // the category it belonged to** rather than cleared — clearing would trade a wrong shape for a lost
        // family.
        val variation = movement("old-variation", "Old Variation", parent = "barbell-bench-press")
            .copy(primaryMuscle = null, secondaryMuscles = emptyList(), equipment = null, movementPattern = null)
        val innerCategory = category("cat-inner", "Inner").copy(parentId = "cat-bench")
        val a = movement("a", "A", parent = "b")
        val b = movement("b", "B", parent = "a")

        val shaped = (library + variation + innerCategory + a + b)
            .withValidLibraryShape()
            .associateBy { it.id }

        // The variation lands under the family, with what it used to read from the bar written onto it.
        assertThat(shaped.getValue("old-variation").parentId).isEqualTo("cat-bench")
        assertThat(shaped.getValue("old-variation").primaryMuscle).isEqualTo(MuscleGroup.HAMSTRINGS)
        assertThat(shaped.getValue("old-variation").equipment).isEqualTo(Equipment.BARBELL)

        // A category sits at the top, and a cycle has no top to fold onto, so those links go.
        assertThat(shaped.getValue("cat-inner").parentId).isNull()
        assertThat(shaped.getValue("a").parentId).isNull()
        assertThat(shaped.getValue("b").parentId).isNull()

        // And a legal library is untouched.
        assertThat(shaped.getValue("barbell-bench-press").parentId).isEqualTo("cat-bench")
    }

    @Test
    fun aMoveIsLeftAlone_whenTheParentIsNotInTheList() {
        // A program document may legitimately name a movement whose category it does not carry, so a link the
        // list cannot judge is kept rather than cleared.
        val orphan = movement("orphan", "Orphan", parent = "cat-elsewhere")

        val shaped = listOf(orphan).withValidLibraryShape()

        assertThat(shaped.single().parentId).isEqualTo("cat-elsewhere")
    }

    @Test
    fun aCategorysSeries_readsItself_andTheMovementsFiledUnderIt() {
        // ROADMAP N97, N110: a family is one number, which is the whole point — three rows of one barbell
        // bench are one lift to a lifter, and only reading them together says so.
        val ids = bench.seriesSubjectIds(library)

        assertThat(ids).containsExactly(
            "cat-bench",
            "barbell-bench-press",
            "dumbbell-bench-press",
            "bench-speed",
        )
    }

    @Test
    fun aMovementsSeries_readsOnlyItself() {
        // One level and no deeper (N110): a movement heads nothing, so its series is its own work, which is
        // what keeps a paused bench's records and progression its own (N97).
        assertThat(barbellBench.seriesSubjectIds(library)).containsExactly("barbell-bench-press")
    }

    @Test
    fun aCycleInTheLibrary_doesNotLoopForever() {
        // A file is the one place a link this app would never write can arrive (B92), and a resolver that
        // trusted the shape would hang rather than answer.
        val library = listOf(
            movement("a", "A", parent = "b"),
            movement("b", "B", parent = "a"),
        )

        assertThat(library.first { it.id == "a" }.seriesSubjectIds(library)).containsExactly("a", "b")
    }

    @Test
    fun aPattern_isTheRowsOwnWhenItStatesOne_andTheCategorysWhenItDoesNot() {
        // ROADMAP N96 amended by N103 and N110: the rule is own-if-stated, uniformly, so a filed movement
        // that says nothing reads its family's and one that states a pattern keeps it — which is what the old
        // head-wins special case refused, and what a lifter setting one by hand would have hit.
        val library = listOf(
            category("cat-bench", "Bench Press").copy(movementPattern = MovementPattern.PRESS),
            movement("barbell-bench-press", "Barbell Bench Press", parent = "cat-bench")
                .copy(movementPattern = null),
            movement("squat", "Back Squat").copy(movementPattern = MovementPattern.SQUAT),
            movement("machine-pec-deck", "Pec Deck", parent = "cat-bench")
                .copy(movementPattern = MovementPattern.ISOLATION),
            movement("loose-carry", "Farmer's Walk").copy(movementPattern = null),
        )

        assertThat(library.first { it.id == "barbell-bench-press" }.effectiveMovementPattern(library))
            .isEqualTo(MovementPattern.PRESS)
        assertWithMessage("its own value wins when it states one, even under a stating category")
            .that(library.first { it.id == "machine-pec-deck" }.effectiveMovementPattern(library))
            .isEqualTo(MovementPattern.ISOLATION)
        assertWithMessage("an unfiled movement that states one keeps it")
            .that(library.first { it.id == "squat" }.effectiveMovementPattern(library))
            .isEqualTo(MovementPattern.SQUAT)
        assertWithMessage("and one that states nothing, with nothing above it, has none")
            .that(library.first { it.id == "loose-carry" }.effectiveMovementPattern(library))
            .isNull()
    }

    @Test
    fun aMuscle_isTheRowsOwnWhenItStatesOne_andTheCategorysOtherwise() {
        // The same one rule for the muscle (ROADMAP N103, N110), including the case the old resolver got
        // wrong: a row's own muscle was written, exported and read by nothing while a category stated one.
        val library = listOf(
            category("cat-bench", "Bench Press"),
            movement("barbell-bench-press", "Barbell Bench Press", parent = "cat-bench")
                .copy(primaryMuscle = null),
            movement("own-answer", "Own Answer", parent = "cat-bench")
                .copy(primaryMuscle = MuscleGroup.SHOULDERS),
            movement("loose", "Loose").copy(primaryMuscle = null),
        )

        assertThat(library.first { it.id == "barbell-bench-press" }.effectivePrimaryMuscle(library))
            .isEqualTo(MuscleGroup.CHEST)
        assertWithMessage("a row that states its own keeps it, category or no category")
            .that(library.first { it.id == "own-answer" }.effectivePrimaryMuscle(library))
            .isEqualTo(MuscleGroup.SHOULDERS)
        assertWithMessage("a row nothing states a muscle for has none rather than a guess")
            .that(library.first { it.id == "loose" }.effectivePrimaryMuscle(library))
            .isNull()
    }

    @Test
    fun onlyACategory_headsOthers() {
        // ROADMAP N110: the movements filed under a category are what a family is, and a movement holds
        // nothing — so the statistics lift list offers a family label for a category and never for a movement.
        assertThat(bench.headsOthers).isTrue()
        assertThat(barbellBench.headsOthers).isFalse()
        // Even a category nothing is filed under yet: it is what a category is *for*.
        assertThat(category("cat-empty", "Empty").headsOthers).isTrue()
    }
}
