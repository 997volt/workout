package com.example.androidapp.platform

import java.io.File

/**
 * Removes the reports a previous share staged (N344).
 *
 * The app cannot know when the receiving app has finished reading a shared file, so it does not delete
 * the one it just handed over. What it can do is keep the folder to one file: every new share clears
 * the previous staged report before writing its own. The cache is app-private either way, but health
 * data should not accumulate there for the life of the install.
 */
internal fun clearStagedReports(folder: File) {
    folder.listFiles()?.forEach { it.delete() }
}
