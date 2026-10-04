package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer

/**
 * A prescribed rest as a person reads it: a duration, or the word for "none" (ROADMAP N45).
 *
 * A stored zero is a deliberate "no rest", and `0:00` reads as a rest that has run out instead.
 * The two states are different intentions, so they get different words rather than one number that
 * has to be interpreted.
 */
@Composable
fun restLabel(seconds: Int): String =
    if (seconds == RestTimer.MIN_PRESCRIBED_SECONDS) {
        stringResource(R.string.rest_none)
    } else {
        RestTimer.format(seconds)
    }
