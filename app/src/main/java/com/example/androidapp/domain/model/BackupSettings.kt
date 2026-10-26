package com.example.androidapp.domain.model

import java.time.Instant

/**
 * The folder the user picked for automatic backups (ROADMAP N218).
 *
 * The label is stored beside the URI rather than resolved on every render: reading a display name back
 * from the provider is a query that can fail on a folder that was moved, and a setting row should not
 * depend on a provider being awake. It is re-resolved when the folder is picked, which is the only moment
 * the user is present to fix it.
 */
data class BackupFolder(
    /** The tree URI: both where files go and what the persisted grant is held for. */
    val uri: String,
    /** What to call it on screen. */
    val label: String,
)

/**
 * Everything the launcher and the settings screen need to know about automatic backups (ROADMAP N218).
 *
 * One value rather than six separate flows: the switch, the period, the folder and where the last attempt
 * ended are always read together, and a due check that saw a folder but not the switch it belongs to would
 * be a bug the type could have prevented. The store keeps them as separate preferences, because a partial
 * write of one of them is harmless — a folder with no switch simply never runs.
 */
data class BackupSettings(
    /** Off by default: a backup the user did not ask for is a file they did not ask for. */
    val enabled: Boolean = false,
    val schedule: BackupSchedule = BackupSchedule.DEFAULT,
    /** Null until a folder is chosen; an enabled backup with no folder does not run. */
    val folder: BackupFolder? = null,
    /** When a backup last succeeded, or null for never. */
    val lastSuccessAt: Instant? = null,
    /**
     * When a backup was last attempted, success or failure — the retry's own clock (B249).
     *
     * The period measures *copies*, and a failure does not produce one, so due-ness alone would retry a
     * broken folder on every foregrounding, each time running the whole export. This is the brake: an attempt
     * is made at the period's pace, however the last one ended.
     */
    val lastAttemptAt: Instant? = null,
    /** Whether the last attempt failed. Cleared by the next success. */
    val failed: Boolean = false,
)
