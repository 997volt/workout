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
