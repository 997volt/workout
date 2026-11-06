package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The bar every top-level tab root shares (B357).
 *
 * A tab root has no Up: it is the place the tab goes, so an arrow would offer to go somewhere the lifter
 * did not come from. Statistics already had that rule while History, Settings and the Library drew one;
 * keeping it here rather than on four screens is what makes it one rule. Screens reached *from* a root
 * keep their own back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabRootTopBar(
    title: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title, testTag = testTag) },
        actions = actions,
        modifier = modifier,
    )
}
