package com.example.androidapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Every migration, applied to a store built from the schema it claims to upgrade (ROADMAP B286).
 *
 * The per-migration tests hand-build the tables they touch and call one migration object directly,
 * which is why they can say nothing about the result: Room's own validation never runs, and a
 * migration that produces the wrong type, constraint or index would pass there and refuse to open
 * on the first device after an upgrade — with no destructive fallback, so the app cannot start.
 *
 * This test builds the **whole** old schema from its exported JSON (the file that shipped with that
 * version), puts a real row in every table, lets Room run the chain, and lets Room validate the
 * result. The negative case is the proof that the validation is live: a no-op 10 → 11 is refused.
 *
 * `room-testing`'s `MigrationTestHelper` would do the same thing; it is not a dependency this
 * repository carries, and adding one is a decision of its own (AGENTS.md), so the schema is read
 * from the committed JSON instead.
 */
@RunWith(AndroidJUnit4::class)
class MigrationSchemaTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val opened = mutableListOf<String>()

    @After
    fun tearDown() {
        opened.forEach { name -> context.deleteDatabase(name) }
    }

    @Test
    fun everyMigratedStore_opensUnderRoomAndKeepsItsRow() {
        val broken = (1 until versions().max())
            .filterNot { it in NEVER_SHIPPED }
            .mapNotNull { from -> upgradeAndCheck(from) }

        assertThat(broken).isEmpty()
    }

    @Test
    fun aMigrationThatChangesNothing_isRefused() {
        // 10 -> 11 adds two columns. A migration that does not add them must fail validation; if this
        // test passes, the check above is proving something.
        val name = "migration-schema-noop.db"
        createStore(name, 10)
        opened += name

        val room = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(
                object : Migration(10, 11) {
                    override fun migrate(db: SupportSQLiteDatabase) = Unit
                },
            )
            .build()
        try {
            assertThat(runCatching { room.openHelper.writableDatabase }.exceptionOrNull()).isNotNull()
        } finally {
            room.close()
        }
    }

    /** The failure this version produced, or null when the migrated store opened and kept its row. */
    private fun upgradeAndCheck(from: Int): String? {
        val name = "migration-schema-$from.db"
        createStore(name, from)
        opened += name

        // Opening is the assertion: Room runs the chain and validates the schema the migrations
        // produced, and throws `Migration didn't properly handle...` when it does not match.
        val failure = openThroughRoom(name)
        if (failure != null) return "from $from: $failure"

        return if (rowCount(name) == 1) {
            null
        } else {
            "from $from: the row written before the chain did not survive it"
        }
    }

    private fun openThroughRoom(name: String): String? {
        val room = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS.toTypedArray())
            .build()
        val failure = runCatching { room.openHelper.writableDatabase }.exceptionOrNull()
        room.close()
        return failure?.message?.lineSequence()?.firstOrNull()
    }

    private fun rowCount(name: String): Int? =
        SQLiteDatabase.openDatabase(
            context.getDatabasePath(name).path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { database ->
            database.rawQuery("SELECT COUNT(*) FROM exercises WHERE id = ?", arrayOf(ROW_ID)).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else null
            }
        }

    /**
     * A store at [version], built from that version's exported schema with one row per table.
     *
     * Every NOT NULL column without a default is filled, because the point of the rows is a real
     * upgrade rather than an empty one: a `NOT NULL` added by a rebuild is exactly where a migration
     * that forgot its backfill breaks, and an empty table cannot show it.
     */
    private fun createStore(name: String, version: Int) {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            database.beginTransaction()
            entitiesOf(version).forEach { entity -> createTable(database, entity) }
            database.version = version
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
            database.close()
        }
    }

    private fun entitiesOf(version: Int): List<JSONObject> {
        val entities = JSONObject(File(repoRoot, "$SCHEMAS/$version.json").readText())
            .getJSONObject("database")
            .getJSONArray("entities")
        return (0 until entities.length()).map { entities.getJSONObject(it) }
    }

    private fun createTable(database: SQLiteDatabase, entity: JSONObject) {
        val table = entity.getString("tableName")
        database.execSQL(entity.getString("createSql").replace(TABLE_PLACEHOLDER, table))
        createIndices(database, entity, table)
        insertOneRow(database, entity)
    }

    private fun createIndices(database: SQLiteDatabase, entity: JSONObject, table: String) {
        val indices = entity.optJSONArray("indices") ?: return
        for (position in 0 until indices.length()) {
            database.execSQL(
                indices.getJSONObject(position)
                    .getString("createSql")
                    .replace(TABLE_PLACEHOLDER, table),
            )
        }
    }

    /** One row, every NOT NULL column filled from the schema's own declaration, so the id is [ROW_ID]. */
    private fun insertOneRow(database: SQLiteDatabase, entity: JSONObject) {
        val table = entity.getString("tableName")
        val columns = mutableListOf<String>()
        val values = mutableListOf<String>()
        val fields = entity.getJSONArray("fields")
        for (index in 0 until fields.length()) {
            val field = fields.getJSONObject(index)
            if (!field.optBoolean("notNull", false) || field.has("defaultValue")) continue
            columns += "`${field.getString("columnName")}`"
            values += literalFor(field)
        }
        if (columns.isEmpty()) {
            database.execSQL("INSERT INTO `$table` DEFAULT VALUES")
        } else {
            database.execSQL(
                "INSERT INTO `$table` (${columns.joinToString(", ")}) " +
                    "VALUES (${values.joinToString(", ")})",
            )
        }
    }

    private fun literalFor(field: JSONObject): String = when (field.optString("affinity", "TEXT")) {
        "INTEGER" -> "1"
        "REAL" -> "1.0"
        "BLOB" -> "X'00'"
        else -> "'$ROW_ID'"
    }

    private fun versions(): List<Int> =
        File(repoRoot, SCHEMAS)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .map { it.nameWithoutExtension.toInt() }
            .sorted()

    private companion object {
        /**
         * The one exported schema that no release ever created, and why it is not a path to test.
         *
         * Schema 10 was current only while N219 and N220 were in the same unreleased cycle. Its
         * `createAllTables` did not declare the default that `MIGRATION_9_10` adds, so a store created
         * by the v10 code and a store upgraded to it disagreed — B240 records that, and v11 declared
         * both defaults, which is the schema 2.16 shipped. Nothing at 10 left this repository, and
         * editing the shipped 10 → 11 to repair a state nobody has is refused.
         */
        val NEVER_SHIPPED = setOf(10)

        const val SCHEMAS =
            "app/schemas/com.example.androidapp.data.local.WorkoutDatabase"
        const val TABLE_PLACEHOLDER = "\${TABLE_NAME}"
        const val ROW_ID = "migration-row"
    }
}
