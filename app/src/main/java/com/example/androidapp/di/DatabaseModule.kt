package com.example.androidapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.androidapp.data.local.MIGRATION_1_2
import com.example.androidapp.data.local.MIGRATION_2_3
import com.example.androidapp.data.local.MIGRATION_3_4
import com.example.androidapp.data.local.MIGRATION_4_5
import com.example.androidapp.data.local.MIGRATION_5_6
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
    fun provideDatabase(
        @ApplicationContext context: Context,
        timeSource: TimeSource,
    ): WorkoutDatabase =
        Room.databaseBuilder(context, WorkoutDatabase::class.java, WorkoutDatabase.NAME)
            // 2.0's store was created at schema 1 and 1.x's file is a different one this build never
            // opens, so there are three migrations: N110's, which re-files the rows a variation created
            // when the library became a category over movements; N113's, which splits the body's
            // record in two and lets a measurement weigh nothing; and N134's, one nullable column that
            // lets a template keep a version; N137's, one table that gives a goal a past; and N141's, one
            // column that lets a session be an activity.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
