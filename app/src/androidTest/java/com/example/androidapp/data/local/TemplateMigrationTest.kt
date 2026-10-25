package com.example.androidapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A template can keep a version, carried by [MIGRATION_3_4] (ROADMAP N134).
 *
 * The structure is one nullable column, so this is the direct shape [LibraryMigrationTest] uses rather
 * than a rebuild: `templates` is rewound to its schema-3 shape, rows written into it, and the migration
 * run against that. What it proves is the two halves the feature rests on — a row written before the
 * column existed is **not** a version, and a version written after it is hidden from the templates list
 * while the version read still finds it.
 */
@RunWith(AndroidJUnit4::class)
class TemplateMigrationTest {

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
    fun aTemplateWrittenBeforeVersions_isNotAVersionOfAnything() = runTest {
        val db = v3Store(
            "INSERT INTO templates (id, name, createdAt, updatedAt) VALUES ('t1', 'Push day', 1, 2)",
        )

        MIGRATION_3_4.migrate(db)
        // The readers select the current shape, so the rest of this table's chain runs too: what the
        // case asserts is what 3_4 adds, not that the later column is absent (N214).
        MIGRATION_8_9.migrate(db)

        val row = database.templateDao().findById("t1")
        assertEquals("Push day", row?.name)
        assertNull("the column the migration adds is null for every row already on disk", row?.versionOf)
        assertEquals(
            "and the templates list still holds it",
            listOf("t1"),
            database.templateDao().observeTemplates().first().map { it.id },
        )
    }

    @Test
    fun aVersion_writtenAfterTheMigration_isHiddenFromTheList_andFoundByTheVersionRead() = runTest {
        val db = v3Store(
            "INSERT INTO templates (id, name, createdAt, updatedAt) VALUES ('t1', 'Push day', 1, 2)",
        )
        MIGRATION_3_4.migrate(db)
        // The readers select the current shape, so the rest of this table's chain runs too: what the
        // case asserts is what 3_4 adds, not that the later column is absent (N214).
        MIGRATION_8_9.migrate(db)

        db.execSQL(
            "INSERT INTO templates (id, name, versionOf, createdAt, updatedAt) " +
                "VALUES ('v1', 'Push day', 't1', 3, 3)",
        )

        assertEquals(
            "a version never appears as a template",
            listOf("t1"),
            database.templateDao().observeTemplates().first().map { it.id },
        )
        assertEquals(
            "and it is read back where it belongs",
            listOf("v1"),
            database.templateDao().observeVersions("t1").first().map { it.id },
        )
    }

    @Test
    fun aTemplateWrittenBeforeHiding_isVisible_andOneHiddenAfter_itIsReadBack() = runTest {
        val db = v8Store(
            "INSERT INTO templates (id, name, createdAt, updatedAt) VALUES ('t1', 'Push day', 1, 2)",
        )

        MIGRATION_8_9.migrate(db)

        val row = database.templateDao().findById("t1")
        assertEquals("Push day", row?.name)
        assertNull("a row written before the column existed is offered", row?.hiddenAt)
        assertEquals(
            "and the list holds it",
            listOf("t1"),
            database.templateDao().observeTemplates().first().map { it.id },
        )

        // The write the feature adds, on the migrated column: hidden is read back, and the row still
        // resolves by id, which is what keeps a program slot naming it running (ROADMAP N214).
        database.templateDao().setHiddenAt("t1", 9L)

        assertEquals(
            "the list carries the flag",
            9L,
            database.templateDao().observeTemplates().first().single().hiddenAt,
        )
        assertNotNull(
            "a hidden template is still a template",
            database.templateDao().findById("t1"),
        )
    }

    /**
     * A schema-3 store with `templates` rewound to its old shape.
     *
     * Room creates the file at version 4, so the test drops the table and recreates it as `3.json`
     * spells it. `template_exercises` carries a foreign key to `templates`, so the parent's rows go with
     * the drop and that table is left standing — the summary queries read it, and this migration does
     * not touch it.
     */
    /** A schema-8 store: `templates` as it was before `hiddenAt`, with the version column it already had. */
    private fun v8Store(vararg inserts: String) = database.openHelper.writableDatabase.apply {
        execSQL("DROP TABLE templates")
        execSQL(V8_TEMPLATES)
        inserts.forEach { execSQL(it) }
    }

    private fun v3Store(vararg inserts: String) = database.openHelper.writableDatabase.apply {
        execSQL("DROP TABLE templates")
        execSQL(V3_TEMPLATES)
        inserts.forEach { execSQL(it) }
    }

    private companion object {
        /** `8.json`'s `createSql` for `templates`, with the table's real name in place. */
        const val V8_TEMPLATES = """
            CREATE TABLE IF NOT EXISTS `templates` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `versionOf` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
        """

        /** `3.json`'s `createSql` for `templates`, with the table's real name in place. */
        const val V3_TEMPLATES = """
            CREATE TABLE IF NOT EXISTS `templates` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
        """
    }
}
