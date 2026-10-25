package com.example.androidapp.ui.templates

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.TestTags

/**
 * What the editor's overflow holds: the plan's versions, and hiding it from the choices (N134, N214).
 *
 * Its own file so the editor screen stays inside the length and emitter limits this project sets, and
 * because the items are one idea — the plan's own actions, as against Delete beside them.
 */
@Composable
internal fun TemplateEditorMenu(
    expanded: Boolean,
    versionsLabel: String,
    hidden: Boolean,
    canHide: Boolean,
    onDismiss: () -> Unit,
    onKeepVersion: () -> Unit,
    onOpenVersions: () -> Unit,
    onToggleHide: () -> Unit,
) {
    // One emission, the menu itself: several sibling items at this function's top level is the shape
    // the Compose rules refuse, and the menu already draws them inside its own column.
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.template_keep_version)) },
            onClick = {
                onDismiss()
                onKeepVersion()
            },
            modifier = Modifier.testTag(TestTags.TEMPLATE_KEEP_VERSION),
        )
        DropdownMenuItem(
            text = { Text(versionsLabel) },
            onClick = {
                onDismiss()
                onOpenVersions()
            },
            modifier = Modifier.testTag(TestTags.TEMPLATE_VERSIONS),
        )
        // Under the versions, as the plan's own actions: hiding is what the lifter does to a plan they
        // keep but no longer choose from (ROADMAP N214). A new workout holds nothing to hide, so the item
        // is absent on one — but never absent when it is already hidden, or a plan emptied later would
        // have no way back.
        if (hidden || canHide) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (hidden) R.string.template_unhide_action else R.string.template_hide,
                        ),
                    )
                },
                onClick = {
                    onDismiss()
                    onToggleHide()
                },
                modifier = Modifier.testTag(TestTags.TEMPLATE_HIDE),
            )
        }
    }
}
