package com.example.androidapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.data.RoomGoalRepository
import com.example.androidapp.data.SETTINGS_FILE_NAME
import com.example.androidapp.data.moveLegacyGoals
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.WeightGoal
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        val row = database.metricGoalDao().allForBackup().single()
        assertEquals("the row is the first move's, not the second's", 1L, row.createdAt)
    }

    @Test
    fun anUndatedPeriod_leads_theDatedOnes_and_theOpenOneIsTheNewest() = runTest {
        // B140: an undated row is the goal that was already on the device, so it is the **oldest** period.
        // Every reader walks the list and closes each period at the next one's start, and the open one — the
        // period the screens draw — is the newest, which is the last of this list.
        val goals = database.metricGoalDao()
        goals.insert(legacyRow(value = 80_000.0))
        goals.insert(datedRow(id = "new", at = 500L, value = 75_000.0))

        val periods = goals.observePeriods(WEIGHT).first()

        assertEquals(listOf("legacy:$WEIGHT", "new"), periods.map { it.id })
        assertEquals(listOf(null, 500L), periods.map { it.startedAt })
    }

    @Test
    fun pressingSet_withTheOpenGoalsOwnValue_opensNothing_evenWithALegacyRow() = runTest {
        // The half of B140 the ordering broke: opening a period compared the new goal against the *legacy*
        // row, so the same number opened a duplicate period and a change back to an old goal read as no
        // change at all (Success, with nothing written).
        val goals = database.metricGoalDao()
        goals.insert(legacyRow(value = 80_000.0))
        var now = Instant.ofEpochMilli(500L)
        val repository = RoomGoalRepository(database, TimeSource { now })
        val firstDay = now.atZone(ZoneId.systemDefault()).toLocalDate()

        repository.savePeriod(WEIGHT, null, firstDay, null, WeightGoal.Target(75_000L))
        repository.savePeriod(WEIGHT, null, firstDay, null, WeightGoal.Target(75_000L))

        assertEquals("the same value twice on one day is one period", 2, goals.observePeriods(WEIGHT).first().size)

        // N215's day granularity: a change is a period on another day, so the clock moves one.
        now = now.plus(Duration.ofDays(1))
        repository.savePeriod(
            WEIGHT,
            null,
            now.atZone(ZoneId.systemDefault()).toLocalDate(),
            null,
            WeightGoal.Target(80_000L),
        )

        assertEquals(
            "a change on a later day is a change to the open goal, not a no-op",
            3,
            goals.observePeriods(WEIGHT).first().size,
        )
    }

    /** The row the one-time move writes: a goal that was already on the device, with no start (N137). */
    private fun legacyRow(value: Double) = MetricGoalEntity(
        id = "legacy:$WEIGHT",
        metricId = WEIGHT,
        kind = "TARGET",
        value = value,
        startedAt = null,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun datedRow(id: String, at: Long, value: Double) = MetricGoalEntity(
        id = id,
        metricId = WEIGHT,
        kind = "TARGET",
        value = value,
        startedAt = at,
        createdAt = at,
        updatedAt = at,
    )

    private fun prefs(): SharedPreferences =
        context.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val WEIGHT = "BODY:WEIGHT"
    }
}
