package com.example.androidapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.local.ALL_MIGRATIONS
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.seedMissingExercises
import com.example.androidapp.data.moveLegacyGoals
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.platform.CrashLogStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    // The builder takes varargs and B286 requires the migrations to be one list a test can read, so
    // the single copy this costs is paid once, when the database is created. Suppressed here rather
    // than by excluding the rule for the file: the suppression is the whole exception, and it is this
    // call's.
    @Suppress("SpreadOperator")
    fun provideDatabase(
        @ApplicationContext context: Context,
        timeSource: TimeSource,
    ): WorkoutDatabase =
        Room.databaseBuilder(context, WorkoutDatabase::class.java, WorkoutDatabase.NAME)
            // The chain itself lives in ALL_MIGRATIONS, one list the tests can inspect; what each step
            // did to the schema is on WorkoutDatabase. A migration this list forgets fails a test before
            // it can fail an upgrade.
            .addMigrations(*ALL_MIGRATIONS.toTypedArray())
            .addCallback(
                object : RoomDatabase.Callback() {
                    /**
                     * Tops up the seed library on **every** open, not just creation
                     * (ROADMAP F15).
                     *
                     * `onCreate` fires once per database file, so seeding there meant a
                     * later release could never deliver new exercises to an existing
                     * install. `onOpen` runs after `onCreate` on a first open and on every
                     * open after that, so one path covers first launch and upgrade alike.
                     *
                     * Running synchronously on this connection is also the fix for the
                     * first-launch flash: dispatching the insert to a coroutine raced the
                     * first read, so `observeExercises()` emitted an empty library and the
                     * UI cannot tell that apart from "nothing matched your search".
                     */
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        seedMissingExercises(db, timeSource.nowEpochMillis())
                        // The same kind of one-time move, of the same "what is already on disk" (N137):
                        // the goals a lifter had live in settings, and the rows that keep their past are
                        // here. It writes an unknown start rather than inventing the upgrade's own moment.
                        moveLegacyGoals(db, context, timeSource.nowEpochMillis())
                    }
                },
            )
            .build()

    /** Crash logs live beside the database, in app-private storage (ROADMAP F11). */
    @Provides
    @Singleton
    fun provideCrashLogStore(@ApplicationContext context: Context): CrashLogStore =
        CrashLogStore(File(context.filesDir, "crash-logs"))
}
