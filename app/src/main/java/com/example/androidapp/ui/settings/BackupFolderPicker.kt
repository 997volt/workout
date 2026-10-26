package com.example.androidapp.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.androidapp.domain.model.BackupFolder

/**
 * The folder picker behind automatic backup (ROADMAP N218).
 *
 * `OpenDocumentTree` rather than `CreateDocument`: the backup has to write on a later launch, when nobody is
 * present to answer a sheet, so what is taken is a **persistable** grant on a whole tree. That grant is the
 * reason the app can still declare no permissions — storage access here is the user handing over one folder,
 * not the app asking for the device's storage.
 *
 * The grant's failure is not reported from here: a provider that refuses leaves the app able to write for this
 * session only, and the first backup that cannot write says so in the settings row, where the user can act on
 * it. Inventing a second message for the same outcome would be two sentences for one failure.
 */
@Composable
fun rememberBackupFolderPicker(onChoose: (BackupFolder) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult // the sheet was dismissed
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        onChoose(BackupFolder(uri = uri.toString(), label = folderLabel(uri)))
    }

    return remember(launcher) { { launcher.launch(null) } }
}

/**
 * What to call a tree the user picked.
 *
 * A tree document's id is `primary:Download/Workouts`, and the last path segment is the folder's own name —
 * which is the part a person recognises. The full id is kept in the URI itself, so nothing here is load
 * bearing: a label that comes out empty is still a working folder, and the row falls back to its own wording.
 */
internal fun folderLabel(uri: Uri): String {
    val id = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        ?: return uri.lastPathSegment.orEmpty().substringAfterLast('/')
    val path = id.substringAfter(':', id)
    return path.substringAfterLast('/').ifBlank { id }
}
