package com.example.androidapp.data

import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The seed library's own shape (ROADMAP P1.1, N110).
 *
 * Pure JVM, because these are properties of the data rather than of the database that carries it: a list a
 * lifter can actually reach is one where every movement has its own id, every link points at a **category**,
 * and every value the pickers offer has an exercise behind it.
 *
 * The library grows, so nothing here pins a count — only the rules a new entry has to keep.
 */
class SeedExercisesTest {

    @Test
    fun everyMovementId_isUnique() {
        assertThat(SeedExercises.all.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun everyCategoryId_isUnique() {
        assertThat(SeedExercises.categories.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun noMovementSharesAnIdWithACategory() {
        // B148, and the two uniqueness tests above are why it happened: each list was clean on its own, and a
        // category and a movement are two **key spaces over one table**. The categories are seeded first and
        // `INSERT OR IGNORE` keeps what exists, so a shared slug is not a clash the database reports — it is
        // the movement being discarded without a word, which is exactly what `back-extension` did until this
        // assertion existed. A family whose bare name is a movement's says `-family`; this is the check that
        // the naming kept up.
        val movements = SeedExercises.all.mapTo(mutableSetOf()) { it.id }
        val shared = SeedExercises.categories.map { it.id }.filter { it in movements }

        assertWithMessage("a slug in both lists is a seeded movement the database will drop")
            .that(shared)
            .isEmpty()
    }

    @Test
    fun everyMovementTheSeedFiles_landsUnderACategoryTheSeedAlsoDeclares() {
        // B148's other half: the collision was visible in `parentOf` as the self-parent no write path can
        // produce (`requireValidLibraryShape` refuses it), so the map is checked as a shape and not only as a
        // set of ids. A movement filed under itself reads as a top-level row and takes its family's muscles
        // with it.
        assertWithMessage("a movement filed under itself is a link nothing can write")
            .that(SeedExercises.parentOf.filter { (movement, parent) -> movement == parent })
            .isEmpty()
    }

    @Test
    fun noTwoMovementsShareAName() {
        // The picker offers a movement by **name**, so two rows with the same one are two rows a lifter
        // cannot tell apart — and the grouping cannot help there, because the picker is flat. Equipment
        // variants are the case this has to allow: *Barbell* and *Dumbbell* Bench Press are different rows,
        // which is exactly why the equipment is part of the name a lifter reads.
        //
        // Compared the way a reader would, so `Farmer's Carry` and `Farmers Carry` count as one name.
        assertThat(SeedExercises.all.map { it.name.readable() }).containsNoDuplicates()
    }

    @Test
    fun noTwoCategoriesShareAName() {
        assertThat(SeedExercises.categories.map { it.name.readable() }).containsNoDuplicates()
    }

    /** A name as a lifter reads it: case, apostrophes, dashes and spacing set aside. */
    private fun String.readable(): String =
        lowercase().replace(Regex("[^a-z0-9]"), "")

    @Test
    fun aMovementIsFiledUnderACategory_neverUnderAnotherMovement() {
        // N110's shape, checked on the data: a movement is nobody's head, so a `parentOf` target that is not
        // a category would be the variation level the model no longer has — and a link whose child is not in
        // the list would be a movement no database ever receives.
        val categories = SeedExercises.categories.mapTo(mutableSetOf()) { it.id }
        val movements = SeedExercises.all.mapTo(mutableSetOf()) { it.id }

        assertWithMessage("a file target that is not a category is a variation level")
            .that(SeedExercises.parentOf.values.filterNot { it in categories })
            .isEmpty()
        assertWithMessage("a filed movement has to exist in the seed")
            .that(SeedExercises.parentOf.keys.filterNot { it in movements })
            .isEmpty()
    }

    @Test
    fun theSeedUsesEveryEquipment_theLibraryOffers() {
        // A value a picker offers and no exercise carries is a menu entry that answers nothing — several sat
        // unused until the strength and bodybuilding set arrived.
        assertThat(SeedExercises.all.mapNotNull { it.equipment }.toSet())
            .containsExactlyElementsIn(Equipment.entries)
    }

    @Test
    fun theSeedUsesEveryPatternAndMuscle_exceptOther() {
        // `OTHER` is the *unstated* answer a custom exercise is created with, so it is deliberately the one
        // value no seeded row carries. Everything else is a question a lifter can ask of the library.
        assertThat(SeedExercises.categories.map { it.pattern }.toSet())
            .containsExactlyElementsIn(MovementPattern.entries - MovementPattern.OTHER)
        assertThat(SeedExercises.categories.map { it.primaryMuscle }.toSet())
            .containsExactlyElementsIn(MuscleGroup.entries - MuscleGroup.OTHER)
    }

    @Test
    fun aFamilyThatTrainsLatsNamesThem_andOneThatTrainsUpperBackNamesThat() {
        // N240: the audit's two findings. The row family named upper back and biceps and not the lats, which
        // is half of what a row is for; the pulldown family was the mirror image, missing the upper back that
        // retracts the shoulder blades.
        val row = SeedExercises.categories.single { it.id == "row" }
        val pulldown = SeedExercises.categories.single { it.id == "lat-pulldown-family" }

        assertThat(row.primaryMuscle).isEqualTo(MuscleGroup.UPPER_BACK)
        assertThat(row.secondaryMuscles).contains(MuscleGroup.LATS)
        assertThat(pulldown.primaryMuscle).isEqualTo(MuscleGroup.LATS)
        assertThat(pulldown.secondaryMuscles).contains(MuscleGroup.UPPER_BACK)
    }

    @Test
    fun noFamilyNamesItsOwnPrimaryMuscleAsASecondaryOne() {
        // The rule the audit reads by: a family states what it trains *besides* its primary, so a muscle in
        // both lists would count one set twice in the balance figure (N119, N122).
        val doubled = SeedExercises.categories
            .filter { it.primaryMuscle in it.secondaryMuscles }
            .map { it.id }

        assertThat(doubled).isEmpty()
    }

    @Test
    fun everySeededRowResolvesToAPrimaryMuscle() {
        // A movement filed under a family reads its muscle from there (N103, N110), so a family that states
        // none would leave every movement under it training nothing - which the balance figure cannot say.
        // The one movement in no family states its own, which is why the answer is "none" rather than "push-up".
        val byId = SeedExercises.categories.associateBy { it.id }
        val unresolved = SeedExercises.all
            .filter { movement ->
                movement.primaryMuscle == null && byId[SeedExercises.parentOf[movement.id]]?.primaryMuscle == null
            }
            .map { it.id }

        assertThat(unresolved).isEmpty()
    }

    @Test
    fun thePlyometricFamily_holdsTheJumpsAndTheOneThrow() {
        // N241: the seed shipped strength and bodybuilding work and no jump, so a program with plyometrics in
        // it authored every movement by hand. The family is where they file, and its pattern is the reason the
        // enum gained one: a jump is not a press, a squat or an isolation.
        val family = SeedExercises.categories.single { it.id == "plyometric" }

        assertThat(family.pattern).isEqualTo(MovementPattern.JUMP)
        assertThat(family.primaryMuscle).isEqualTo(MuscleGroup.QUADS)
        assertThat(SeedExercises.parentOf.filterValues { it == "plyometric" }.keys)
            .containsAtLeast("box-jump", "jump-squat", "broad-jump", "depth-jump", "pogo-hop", "skater-bound")
        assertWithMessage("and the throw states its own muscles, because the family's quads are not its")
            .that(SeedExercises.all.single { it.id == "medicine-ball-throw" }.primaryMuscle)
            .isEqualTo(MuscleGroup.SHOULDERS)
    }

    @Test
    fun everyMovementTheSeedCanFile_isFiled_exceptThePushUp() {
        // N229: eight movements sat at the top level, so a fresh install drew a flat alphabetical pile of
        // loose lifts under its families. One stays loose on purpose - a push-up is none of the press
        // families, which are each a lift's.
        assertWithMessage("the one movement the seed leaves unfiled")
            .that(SeedExercises.all.filterNot { it.id in SeedExercises.parentOf }.map { it.id })
            .containsExactly("push-up")

        assertWithMessage("the ambiguous plain deadlift is gone")
            .that(SeedExercises.all.map { it.id })
            .doesNotContain("deadlift")
        assertThat(SeedExercises.parentOf["conventional-deadlift"]).isEqualTo("deadlift-family")
    }

    @Test
    fun theSeedDeclaresWhichMovementsAreBodyweightAndTimed() {
        // B293: the seeder wrote neither column, so every seeded movement arrived as an ordinary loaded one
        // — a weighted pull-up was recorded as its belt alone, and a plank offered reps. The two flags are
        // the movement's own, so the seed is where they live.
        assertWithMessage("a movement performed with the body is loaded by it")
            .that(SeedExercises.all.filter { it.equipment == Equipment.BODYWEIGHT && !it.isBodyweight }.map { it.id })
            .isEmpty()
        // Loaded by the body even where something else takes weight off it, or is the wheel it rolls on.
        assertThat(SeedExercises.all.single { it.id == "assisted-pull-up" }.isBodyweight).isTrue()
        assertThat(SeedExercises.all.single { it.id == "ab-wheel-rollout" }.isBodyweight).isTrue()

        // A hold is the movement's shape rather than a choice on the day.
        assertThat(SeedExercises.all.filter { it.isTimed }.map { it.id })
            .containsAtLeast("plank", "side-plank", "copenhagen-plank", "farmers-carry", "suitcase-carry")
        assertWithMessage("a counted movement is not a hold")
            .that(SeedExercises.all.single { it.id == "barbell-bench-press" }.isTimed)
            .isFalse()
    }

    @Test
    fun everyMovementStatesItsEquipment() {
        // The one fact a category cannot supply (N110): a family spans equipment, so a movement without one
        // would read as nothing at all.
        assertWithMessage("a movement with no equipment reads as no equipment")
            .that(SeedExercises.all.filter { it.equipment == null }.map { it.id })
            .isEmpty()
    }

    @Test
    fun theBehindTheBackCableCurl_readsTheCurlFamilysMuscles() {
        // N123: the movement states only its equipment, and the family it is filed under is what supplies
        // the rest — the link the seeder writes from `parentOf` and the resolution the library, the session
        // rows and the balance view read live (N110).
        val curl = SeedExercises.all.single { it.id == "behind-the-back-cable-curl" }
        val family = SeedExercises.categories.single { it.id == "curl" }

        assertThat(SeedExercises.parentOf["behind-the-back-cable-curl"]).isEqualTo("curl")
        assertThat(curl.equipment).isEqualTo(Equipment.CABLE)
        assertThat(curl.primaryMuscle).isNull()
        assertThat(curl.secondaryMuscles).isEmpty()
        assertThat(family.primaryMuscle).isEqualTo(MuscleGroup.BICEPS)
        assertThat(family.secondaryMuscles).containsExactly(MuscleGroup.FOREARMS)
    }
}
