package com.example.androidapp.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.androidapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The one CSV the share sheet stages: its file name and text. */
data class TrainingReportFile(val name: String, val csv: String)

/**
 * Stages [file] in the cache and opens the system share sheet (ROADMAP N331).
 *
 * The file leaves only through the app the user picks. It is staged in the cache under the
 * provider declared for `reports/`, granted read-only for that one URI, and never uploaded by the
 * app; the provider is not exported and the cache is cleared by the system.
 */
suspend fun shareTrainingReport(context: Context, file: TrainingReportFile): Boolean {
    val folder = File(context.cacheDir, "reports").apply { mkdirs() }
    val target = File(folder, file.name)
    val written = withContext(Dispatchers.IO) {
        runCatching { target.writeText(file.csv) }.isSuccess
    }
    if (!written) return false

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.reportfiles", target)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.history_block_title))
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.history_block_share)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    withContext(Dispatchers.Main) { context.startActivity(chooser) }
    return true
}
