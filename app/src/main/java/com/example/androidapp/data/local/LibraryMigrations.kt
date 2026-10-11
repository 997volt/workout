package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.SeedExercises
import com.example.androidapp.domain.model.RowKind

/**
 * The library's shape went from three levels to two (ROADMAP N110): a category holds movements, and a
 * movement is no longer a version of another movement.
 *
 * This carries a database written under the old shape across. **The schema does not change** — `parentId`
 * and `rowKind` mean what they always did — so every statement here is data, and the version moves only so
 * they run once rather than on every open.
 *
 * Three things have to happen, in this order:
 *
 *  1. **The seeded categories state their taxonomy now.** The old seeder wrote a category's pattern and left
 *     its muscle and secondary list unstated, so without this a movement that now reads them would read
 *     nothing. Read from [SeedExercises.categories] rather than restated in SQL: the seed is the source of
 *     truth for what a family holds, and a second copy here would be a second thing to keep in step. The
 *     guard leaves a value the lifter set alone — only an entirely unstated category is filled.
 *  2. **What a movement read from the movement above it is written down.** A variation stated no muscle, no
 *     equipment and no pattern; it read them from the lift it was a version of. As an ordinary movement it
 *     states its own, so the values are materialised first — equipment included, even though equipment is
 *     never inherited *live* (a family spans equipment), because this is the one moment a formerly inherited
 *     value has no other home.
 *  3. **The link is re-filed to the category.** Folding first is what makes the order load-bearing: after
 *     this statement the movement above is no longer reachable from the row.
 *
 * A movement whose parent is not a movement is untouched, so an ordinary library passes through unchanged.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val converters = Converters()

        SeedExercises.categories.forEach { category ->
            db.execSQL(
                """
                UPDATE exercises
                SET primaryMuscle = ?, secondaryMuscles = ?
                WHERE id = ?
                  AND rowKind = ?
                  AND primaryMuscle IS NULL
                  AND secondaryMuscles = ''
                """.trimIndent(),
                arrayOf<Any?>(
                    converters.fromMuscleGroup(category.primaryMuscle),
                    converters.fromMuscleGroups(category.secondaryMuscles),
                    category.id,
                    RowKind.CATEGORY.name,
                ),
            )
        }

        // What each movement read from the movement above it, folded onto itself. `parentId` is untouched
        // here, so the subquery still finds the lift this row was a version of.
        db.execSQL(
            """
            UPDATE exercises
            SET primaryMuscle = COALESCE(
                    primaryMuscle,
                    (SELECT p.primaryMuscle FROM exercises p WHERE p.id = exercises.parentId)
                ),
                equipment = COALESCE(
                    equipment,
                    (SELECT p.equipment FROM exercises p WHERE p.id = exercises.parentId)
                ),
                movementPattern = COALESCE(
                    movementPattern,
                    (SELECT p.movementPattern FROM exercises p WHERE p.id = exercises.parentId)
                ),
                secondaryMuscles = CASE
                    WHEN secondaryMuscles = ''
                    THEN COALESCE(
                        (SELECT p.secondaryMuscles FROM exercises p WHERE p.id = exercises.parentId),
                        ''
                    )
                    ELSE secondaryMuscles
                END
            WHERE parentId IN (SELECT id FROM exercises WHERE rowKind = ?)
            """.trimIndent(),
            arrayOf<Any?>(RowKind.MOVEMENT.name),
        )

        // And now the link itself: the first category above the row, or nothing when there is none.
        db.execSQL(
            """
            UPDATE exercises
            SET parentId = (SELECT p.parentId FROM exercises p WHERE p.id = exercises.parentId)
            WHERE parentId IN (SELECT id FROM exercises WHERE rowKind = ?)
            """.trimIndent(),
            arrayOf<Any?>(RowKind.MOVEMENT.name),
        )
    }
}
