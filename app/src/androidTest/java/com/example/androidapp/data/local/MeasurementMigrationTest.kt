package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.MuscleGroup
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The body's record split in two, carried across by [MIGRATION_2_3] (ROADMAP N113).
 *
 * **The schema is not identical either side of this one** — a `kind` arrives and a weight that must be
 * present becomes one that may be absent, which SQLite cannot do with `ALTER` — so a database Room creates
 * today cannot stand in for a schema-2 one. This test rewinds the two tables the migration touches to their
 * old shape, writes real rows into them, and runs the migration against that: the direct shape
 * [LibraryMigrationTest] uses, for the migration whose schema *is* the same either side.
 *
 * `room-testing`'s `MigrationTestHelper` would build the old schema from `2.json`. It is not a dependency
 * this repository carries, and adding one is a decision of its own rather than a rider on a defect, so the
 * old `measurements` table is spelled out here exactly as `2.json` gives it.
 */
@RunWith(AndroidJUnit4::class)
class MeasurementMigrationTest {

    private lateinit var database: WorkoutDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aLegacyEntryThatWeighedOnly_becomesThatDaysDailyRecord() = runTest {
        val db = v2Store(
            """
            INSERT INTO measurements (id, measuredAt, weightGrams, bodyFatTenths, createdAt, updatedAt)
            VALUES ('m1', 1000, 82000, 183, 1, 1)
            """.trimIndent(),
        )

        MIGRATION_2_3.migrate(db)

        val rows = database.measurementDao().allForExport()
        val daily = rows.single()
        assertEquals("no tape site was measured, so there is no second record", "m1", daily.id)
        assertEquals(MeasurementKind.DAILY, daily.kind)
        assertEquals(82000L, daily.weightGrams)
        assertEquals(183, daily.bodyFatTenths)
        assertNull("a daily record carries no tape site", daily.waistMm)
    }

    @Test
    fun aLegacyEntryThatMeasuredASite_alsoBecomesATapeRecord() = runTest {
        val db = v2Store(
            """
            INSERT INTO measurements (id, measuredAt, weightGrams, waistMm, createdAt, updatedAt)
            VALUES ('m1', 1000, 82000, 860, 1, 1)
            """.trimIndent(),
        )

        MIGRATION_2_3.migrate(db)

        val rows = database.measurementDao().allForExport().associateBy { it.kind }
        val daily = rows.getValue(MeasurementKind.DAILY)
        val tape = rows.getValue(MeasurementKind.TAPE)
        assertEquals("the daily record is the one the old row was", "m1", daily.id)
        assertEquals(82000L, daily.weightGrams)
        assertNull("and it states no site — the tape half is its own record", daily.waistMm)
        assertEquals("m1-tape", tape.id)
        assertNull("a tape record claims no weight", tape.weightGrams)
        assertEquals(860L, tape.waistMm)
    }

    @Test
    fun aLegacyRowsDeletion_isCarriedToBothRecords() = runTest {
        val db = v2Store(
            """
            INSERT INTO measurements (id, measuredAt, weightGrams, waistMm, createdAt, updatedAt, deletedAt)
            VALUES ('m1', 1000, 82000, 860, 1, 2, 3)
            """.trimIndent(),
        )

        MIGRATION_2_3.migrate(db)

        assertEquals(
            "both halves stay as hidden as the row they came from",
            listOf(3L, 3L),
            database.measurementDao().allForExport().map { it.deletedAt ?: -1L }.sorted(),
        )
    }

    @Test
    fun theSoreTableIsCreated_andKeyedToTheDailyRecord() = runTest {
        val db = v2Store(
            """
            INSERT INTO measurements (id, measuredAt, weightGrams, createdAt, updatedAt)
            VALUES ('m1', 1000, 82000, 1, 1)
            """.trimIndent(),
        )

        MIGRATION_2_3.migrate(db)

        assertEquals(
            "the table the daily record's soreness lands in is created empty",
            emptyList<String>(),
            database.measurementSoreMuscleDao().allForBackup().map { it.id },
        )

        database.measurementSoreMuscleDao().insertAll(
            listOf(
                MeasurementSoreMuscleEntity(
                    id = "s1",
                    measurementId = "m1",
                    muscle = MuscleGroup.QUADS,
                    score = 8,
                    position = 0,
                    createdAt = 1,
                    updatedAt = 1,
                    deletedAt = null,
                ),
            ),
        )

        assertEquals(
            "and its foreign key points at a record the migration wrote",
            listOf("s1"),
            database.measurementSoreMuscleDao().allForBackup().map { it.id },
        )
    }

    /**
     * A schema-2 store with the two tables this migration touches rewound to their old shape.
     *
     * Room creates the file at version 3, so the test drops what it is about to recreate: `measurements`
     * as `2.json` spells it (a required weight, no `kind`), and the sore table the migration's own
     * `CREATE TABLE IF NOT EXISTS` is then responsible for building again.
     */
    private fun v2Store(vararg inserts: String) = database.openHelper.writableDatabase.apply {
        execSQL("DROP TABLE measurement_sore_muscles")
        execSQL("DROP TABLE measurements")
        execSQL(V2_MEASUREMENTS)
        inserts.forEach { execSQL(it) }
    }

    private companion object {
        /** `2.json`'s `createSql` for `measurements`, with the table's real name in place. */
        const val V2_MEASUREMENTS = """
            CREATE TABLE IF NOT EXISTS `measurements` (
                `id` TEXT NOT NULL,
                `measuredAt` INTEGER NOT NULL,
                `weightGrams` INTEGER NOT NULL,
                `bodyFatTenths` INTEGER,
                `muscleTenths` INTEGER,
                `neckMm` INTEGER,
                `chestMm` INTEGER,
                `waistMm` INTEGER,
                `hipsMm` INTEGER,
                `upperArmMm` INTEGER,
                `thighMm` INTEGER,
                `calfMm` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
        """
    }
}
