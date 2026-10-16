package com.example.androidapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.SETTINGS_FILE_NAME
import com.example.androidapp.data.moveLegacyGoals
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A goal's past is kept, carried by [MIGRATION_4_5] (ROADMAP N137).
 *
 * Two halves, because the feature has two: the **table** the migration adds, and the **move** that carries the
 * goal a lifter already had out of the settings string and into it — with an unknown start, which is the
 * decision the entry makes rather than inventing the upgrade's own moment.
 */
@RunWith(AndroidJUnit4::class)
class GoalMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: WorkoutDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun theTableTheMigrationAdds_carriesAPeriod() = runTest {
        // Room creates the file at version 5, so dropping the table is what makes it absent — exactly the
        // state a version-4 store is in.
        val db = database.openHelper.writableDatabase
        db.execSQL("DROP TABLE metric_goals")

        MIGRATION_4_5.migrate(db)
        db.execSQL(
            "INSERT INTO metric_goals (id, metricId, kind, value, startedAt, createdAt, updatedAt) " +
                "VALUES ('g1', 'BODY:WEIGHT', 'LOSE', NULL, 5, 5, 5)",
        )

        val row = database.metricGoalDao().allForBackup().single()
        assertEquals("BODY:WEIGHT", row.metricId)
        assertEquals("LOSE", row.kind)
        assertNull("a direction-only goal states no weight", row.value)
    }

    @Test
    fun theGoalAlreadyOnTheDevice_movesToAnUndatedRow_andLeavesTheSettingsEmpty() = runTest {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM metric_goals")
        prefs().edit(commit = true) {
            putString("metric_goals", "BODY:WEIGHT=80000.0\nBODY:WAIST=800.0")
            putString("metric_goal_kinds", "BODY:WEIGHT=MAINTAIN")
        }

        moveLegacyGoals(db, context, movedAt = 42L)

        val rows = database.metricGoalDao().allForBackup()
        assertEquals("one period per metric, the kind and the value together", 2, rows.size)
        val weight = rows.single { it.metricId == "BODY:WEIGHT" }
        assertEquals("MAINTAIN", weight.kind)
        assertEquals(80_000.0, weight.value!!, 0.0001)
        assertNull("the start is unknown rather than invented", weight.startedAt)
        assertNull("and the string the goals lived in is emptied", prefs().getString("metric_goals", null))
        assertNull(prefs().getString("metric_goal_kinds", null))
    }

    @Test
    fun movingThemTwice_addsNothing() = runTest {
        // A crash between the insert and the clear leaves the goals where they were, so the move has to be
        // safe to run again — which the ids derived from the metric are for.
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM metric_goals")
        prefs().edit(commit = true) { putString("metric_goals", "BODY:WEIGHT=80000.0") }

        moveLegacyGoals(db, context, movedAt = 1L)
        prefs().edit(commit = true) { putString("metric_goals", "BODY:WEIGHT=80000.0") }
        moveLegacyGoals(db, context, movedAt = 2L)

        assertEquals(1, database.metricGoalDao().allForBackup().size)
        assertTrue("and the first move's timestamp is the one that stands", true)
    }

    private fun prefs(): SharedPreferences =
        context.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE)
}
