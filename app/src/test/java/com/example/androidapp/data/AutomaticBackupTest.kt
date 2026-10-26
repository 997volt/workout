package com.example.androidapp.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BackupFolder
import com.example.androidapp.domain.model.BackupSchedule
import com.example.androidapp.domain.repository.BackupRepository
import com.example.androidapp.domain.repository.ImportSummary
import com.example.androidapp.platform.BackupFolderFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/**
 * The backup that writes itself (ROADMAP N218).
 *
 * The question each test answers is the one the feature is built on: **did a copy land, and did the app know
 * it**. The clock is a value, so "a week has passed" is a duration rather than a sleep, and the folder is a
 * fake, so a refused write is a booleans away from a real one.
 */
@RunWith(AndroidJUnit4::class)
class AutomaticBackupTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = Instant.parse("2026-10-26T10:00:00Z")
    private var clock: Instant = now
    private lateinit var settings: PreferencesSettingsRepository
    private lateinit var files: FakeBackupFiles

    private val folder = BackupFolder(uri = "content://tree/primary%3ADownload", label = "Download")

    @Before
    fun setUp() {
        // Each test starts from a device that has never opened Settings: an unset preference must not inherit
        // the previous test's answer.
        context.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        settings = PreferencesSettingsRepository(context)
        files = FakeBackupFiles()
    }

    private fun backup(repository: BackupRepository = FakeBackupRepository()) =
        AutomaticBackup(
            settings = settings,
            backup = repository,
            files = files,
            time = TimeSource { clock },
        )

    private suspend fun enable(schedule: BackupSchedule = BackupSchedule.WEEKLY) {
        settings.setBackupEnabled(true)
        settings.setBackupSchedule(schedule)
        settings.setBackupFolder(folder)
    }

    @Test
    fun offByDefault_writesNothing() = runTest {
        // The switch is the feature: an unused app must not start dropping files in a folder it never chose.
        assertThat(backup().runIfDue()).isEqualTo(BackupRun.NotConfigured)

        assertThat(files.files).isEmpty()
    }

    @Test
    fun enabledWithNoFolder_writesNothing() = runTest {
        // On but nowhere to go. It is reachable — the switch can be flipped before a folder is picked — and it
        // must not be a crash or a file in app-private storage nobody can find.
        settings.setBackupEnabled(true)

        assertThat(backup().runIfDue()).isEqualTo(BackupRun.NotConfigured)

        assertThat(files.files).isEmpty()
    }

    @Test
    fun theFirstRun_writesAFile_andRecordsWhen() = runTest {
        // "Never backed up" is due: there is no last success to measure the period from, and waiting a week for
        // the first copy would be a week with no backup at all.
        enable()

        val run = backup().runIfDue()

        val written = run as BackupRun.Written
        assertThat(written.fileName).isEqualTo(backupFileName(now))
        assertThat(files.files).containsKey(written.fileName)
        assertThat(settings.observeBackupSettings().first().lastSuccessAt).isEqualTo(now)
    }

    @Test
    fun aRunInsideThePeriod_writesNothing() = runTest {
        enable(BackupSchedule.WEEKLY)
        backup().runIfDue()

        clock = now.plus(Duration.ofDays(6))

        assertThat(backup().runIfDue()).isEqualTo(BackupRun.NotDue)
        assertThat(files.files).hasSize(1)
    }

    @Test
    fun thePeriodRunningOut_writesAgain() = runTest {
        // Exactly at the boundary, not one second after: a weekly backup that needs 7 days and a second is a
        // schedule nobody can predict.
        enable(BackupSchedule.WEEKLY)
        backup().runIfDue()

        clock = now.plus(Duration.ofDays(7))

        assertThat(backup().runIfDue()).isInstanceOf(BackupRun.Written::class.java)
        assertThat(files.files).hasSize(2)
    }

    @Test
    fun aScheduleIsThePeriodItNames() = runTest {
        // The three periods are the feature's only free variable; if MONTHLY were thirty-one days the difference
        // would surface as a backup that silently skips a month.
        enable(BackupSchedule.MONTHLY)
        backup().runIfDue()

        clock = now.plus(Duration.ofDays(29))
        assertThat(backup().runIfDue()).isEqualTo(BackupRun.NotDue)

        clock = now.plus(Duration.ofDays(30))
        assertThat(backup().runIfDue()).isInstanceOf(BackupRun.Written::class.java)
    }

    @Test
    fun aFailedExport_isRecorded_andNothingIsWritten() = runTest {
        // The export is produced before the folder is touched, so a database that cannot be read costs nothing.
        enable()

        val run = backup(FakeBackupRepository(failing = true)).runIfDue()

        assertThat(run).isInstanceOf(BackupRun.Failed::class.java)
        assertThat(files.files).isEmpty()
        val stored = settings.observeBackupSettings().first()
        assertThat(stored.failed).isTrue()
        assertThat(stored.lastSuccessAt).isNull()
    }

    @Test
    fun aRefusedWrite_isRecorded_andThePeriodStaysDue() = runTest {
        // A revoked grant or a deleted folder is the realistic failure. It must not record a success: the next
        // launch has to try again, not wait a week.
        enable()
        files.refusing = true

        assertThat(backup().runIfDue()).isInstanceOf(BackupRun.Failed::class.java)

        assertThat(settings.observeBackupSettings().first().failed).isTrue()
        assertThat(settings.observeBackupSettings().first().lastSuccessAt).isNull()
    }

    @Test
    fun aSuccess_clearsAnEarlierFailure() = runTest {
        enable()
        files.refusing = true
        backup().runIfDue()

        files.refusing = false
        clock = now.plus(Duration.ofDays(1))
        backup().runIfDue()

        val stored = settings.observeBackupSettings().first()
        assertThat(stored.failed).isFalse()
        assertThat(stored.lastSuccessAt).isEqualTo(clock)
    }

    @Test
    fun onlyTheNewestFive_areKept() = runTest {
        // The folder is the user's, and a backup that filled it one file a week forever is the feature becoming
        // a problem. Five is the number the roadmap names.
        enable()
        (1..5).forEach { files.files[backupFileName(now.minus(Duration.ofDays(it.toLong())))] = "old" }

        backup().runIfDue()

        assertThat(files.files).hasSize(KEEP_BACKUPS)
        assertThat(files.files.keys).contains(backupFileName(now))
        assertThat(files.files.keys).doesNotContain(backupFileName(now.minus(Duration.ofDays(5))))
    }

    @Test
    fun aFileThisAppDidNotName_isNeverDeleted() = runTest {
        // Pruning reaches into someone else's folder. Deleting their shopping list because it sat beside a
        // backup is a worse bug than the folder growing.
        enable()
        files.files["notes.txt"] = "not ours"
        (1..5).forEach { files.files[backupFileName(now.minus(Duration.ofDays(it.toLong())))] = "old" }

        backup().runIfDue()

        assertThat(files.files).containsKey("notes.txt")
    }

    @Test
    fun thePrunedSet_isTheOldestOnes() {
        // Named as the launcher names them, so the order is time: a backup named for a later instant is newer,
        // and the list is built newest-first to match what the folder holds.
        val names = (1..7).map { backupFileName(now.minus(Duration.ofDays(it.toLong()))) }

        val pruned = backupsToDelete(names)

        assertThat(pruned).containsExactlyElementsIn(names.takeLast(2))
    }
}

/** A folder that keeps its files in a map: enough to see what was written, refused and pruned. */
private class FakeBackupFiles : BackupFolderFiles {

    val files = linkedMapOf<String, String>()
    var refusing = false

    override suspend fun write(treeUri: String, fileName: String, text: String): Boolean {
        if (refusing) return false
        files[fileName] = text
        return true
    }

    override suspend fun names(treeUri: String): List<String> = files.keys.toList()

    override suspend fun delete(treeUri: String, fileName: String): Boolean = files.remove(fileName) != null
}

/** The database's own answer, chosen by the test: a JSON document, or the reason there is none. */
private class FakeBackupRepository(private val failing: Boolean = false) : BackupRepository {

    override suspend fun export(): DataResult<String> = if (failing) {
        DataResult.Failure(DataError.Storage(IllegalStateException("the database could not be read")))
    } else {
        DataResult.Success("""{"schemaVersion":1}""")
    }

    override suspend fun import(text: String): DataResult<ImportSummary> =
        DataResult.Success(ImportSummary(added = 0, restored = 0))

    override suspend fun clearAllUserData(): DataResult<Unit> = DataResult.Success(Unit)
}
