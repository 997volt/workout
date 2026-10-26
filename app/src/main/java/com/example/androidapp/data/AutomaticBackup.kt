package com.example.androidapp.data

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.model.BackupFolder
import com.example.androidapp.domain.model.BackupSettings
import com.example.androidapp.domain.repository.BackupRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.platform.BackupFolderFiles
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** What one launch's due check did (ROADMAP N218). */
sealed interface BackupRun {
    /** Off, or on with no folder chosen: nothing was due because nothing is configured. */
    data object NotConfigured : BackupRun

    /** Configured, but the period has not run out. */
    data object NotDue : BackupRun

    /** A file was written. */
    data class Written(val fileName: String) : BackupRun

    /** The export or the write failed; the failure is recorded so Settings can say so. */
    data class Failed(val error: DataError) : BackupRun
}

/**
 * The backup that writes itself when the app is next opened (ROADMAP N218).
 *
 * **Never scheduled.** The check runs when the app comes to the front and pays a missed window then, which is
 * what keeps the app at zero permissions: a real background job would need WorkManager or an exact alarm plus
 * a battery exemption (P4.7). The consequence is stated rather than hidden — someone who never opens the app
 * is never backed up, and the settings row's date is how they find that out.
 *
 * The work is ordered so a partial run costs nothing: the export is produced **before** the folder is touched,
 * the new file is written before any old one is pruned, and the success is recorded last. A crash anywhere in
 * the middle leaves the previous backups intact and the period still due, so the next launch tries again.
 */
@Singleton
class AutomaticBackup @Inject constructor(
    private val settings: SettingsRepository,
    private val backup: BackupRepository,
    private val files: BackupFolderFiles,
    private val time: TimeSource,
) {

    suspend fun runIfDue(): BackupRun {
        val current = settings.observeBackupSettings().first()
        val folder = current.folder
        val now = time.now()
        // A `when` rather than a chain of early returns: the three answers are one decision — do not run, not
        // yet, or write — and the code should read like the decision.
        return when {
            !current.enabled || folder == null -> BackupRun.NotConfigured
            !isBackupDue(current, now) -> BackupRun.NotDue
            else -> write(folder, now)
        }
    }

    /** The write itself, ordered so a partial run costs nothing. */
    private suspend fun write(folder: BackupFolder, now: Instant): BackupRun {
        val fileName = backupFileName(now)
        return when (val exported = backup.export()) {
            is DataResult.Failure -> failed(exported.error)

            is DataResult.Success -> if (files.write(folder.uri, fileName, exported.data)) {
                // Pruned only after the new file is on disk: a folder that cannot take the new copy must not
                // also lose an old one, which is the one way this feature could make a user worse off.
                backupsToDelete(files.names(folder.uri)).forEach { files.delete(folder.uri, it) }
                settings.recordBackupResult(now, failed = false)
                BackupRun.Written(fileName)
            } else {
                failed(DataError.Storage(IllegalStateException("the backup file was not written")))
            }
        }
    }

    /**
     * Records the failure and answers with it.
     *
     * The recording's own result is deliberately ignored: there is nobody to tell if the *record* of a failure
     * fails, the file situation it describes is unchanged, and the next launch retries the backup itself.
     */
    private suspend fun failed(error: DataError): BackupRun {
        settings.recordBackupResult(at = null, failed = true)
        return BackupRun.Failed(error)
    }
}

/** How many files one folder keeps. Five weeklies is roughly a month of history without growing forever. */
internal const val KEEP_BACKUPS = 5

private const val BACKUP_PREFIX = "workout-backup-"
private const val BACKUP_SUFFIX = ".json"

/**
 * The name a backup takes: the instant in UTC, so names sort the way time does.
 *
 * UTC rather than the device's zone on purpose — the sort is what prunes the right files, and a device that
 * changes zone must not reorder its own history. The name is a timestamp, not a promise about a local date.
 */
private val FILE_STAMP: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").withZone(ZoneOffset.UTC)

/** Whether the period has run out, or nothing has ever been written (ROADMAP N218). */
internal fun isBackupDue(settings: BackupSettings, now: Instant): Boolean {
    val last = settings.lastSuccessAt ?: return true
    return !now.isBefore(last.plus(settings.schedule.period))
}

/** The file this launch would write. */
internal fun backupFileName(now: Instant): String = "$BACKUP_PREFIX${FILE_STAMP.format(now)}$BACKUP_SUFFIX"

/**
 * The files to prune, oldest first, keeping the newest [KEEP_BACKUPS].
 *
 * Only files this app named are touched: the folder is the user's, and a backup that deleted whatever else
 * was in it would be a worse bug than the one pruning solves.
 */
internal fun backupsToDelete(names: List<String>): List<String> =
    names.filter { it.startsWith(BACKUP_PREFIX) && it.endsWith(BACKUP_SUFFIX) }
        .sortedDescending()
        .drop(KEEP_BACKUPS)
