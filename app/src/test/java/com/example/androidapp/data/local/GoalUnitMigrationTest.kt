package com.example.androidapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one data migration in the chain (ROADMAP B270).
 *
 * A rating's target was doubled into half-points when it was typed and read back as points, so the value
 * on disk was twice the scale it was drawn on. `MIGRATION_11_12` halves exactly the rows that came through
 * that parse: the six named rating series and the three families whose ids carry a muscle or a site. A
 * weight target is in grams and a body-fat target in tenths of a percent, and both must be left alone —
 * which is the half of the rule a test has to state, because a migration that halved everything would look
 * correct for the rows it was written for.
 */
@RunWith(AndroidJUnit4::class)
class GoalUnitMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "goal-unit-migration.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun ratingTargets_areHalved_andEveryOtherMetricIsLeftAlone() {
        val room = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val database = room.openHelper.writableDatabase
            val goals = listOf(
                "rpe" to "WORKOUT:RPE",
                "sore" to "SORE:QUADS",
                "joint" to "JOINT:KNEE:LEFT",
                "lift_joint" to "EXERCISE_JOINT:KNEE:LEFT",
                "weight" to "BODY:WEIGHT",
                "body_fat" to "BODY:BODY_FAT",
                "tape" to "TAPE:WAIST",
            )
            goals.forEach { (id, metricId) ->
                database.execSQL(
                    "INSERT INTO metric_goals (id, metricId, kind, value, startedAt, createdAt, updatedAt) " +
                        "VALUES (?, ?, NULL, 14.0, NULL, 1, 1)",
                    arrayOf<Any>(id, metricId),
                )
            }

            MIGRATION_11_12.migrate(database)

            assertThat(valueOf(database, "rpe")).isEqualTo(7.0)
            assertThat(valueOf(database, "sore")).isEqualTo(7.0)
            assertThat(valueOf(database, "joint")).isEqualTo(7.0)
            assertThat(valueOf(database, "lift_joint")).isEqualTo(7.0)
            assertThat(valueOf(database, "weight")).isEqualTo(14.0)
            assertThat(valueOf(database, "body_fat")).isEqualTo(14.0)
            assertThat(valueOf(database, "tape")).isEqualTo(14.0)
        } finally {
            room.close()
        }
    }

    private fun valueOf(database: androidx.sqlite.db.SupportSQLiteDatabase, id: String): Double? =
        database.query("SELECT value FROM metric_goals WHERE id = ?", arrayOf<Any>(id)).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getDouble(0) else null
        }
}
