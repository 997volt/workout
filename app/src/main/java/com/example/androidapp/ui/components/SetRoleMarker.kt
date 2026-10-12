package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.SetType

/**
 * What a set was, where that is worth saying: its role, or null for an ordinary working set
 * (ROADMAP N14, N112).
 *
 * A working set is the default and needs no label, so one says nothing. Shared from N112 rather than
 * written twice: the workout's row carried this rule and the history row did not, and once a warm-up
 * stopped taking a number (N112) that left the history row saying nothing at all about a set that was
 * preparation.
 */
@Composable
internal fun setRoleMarker(setType: SetType): String? =
    setType.takeIf { it != SetType.NORMAL }
        ?.let { stringResource(R.string.set_role_marker, it.label) }
