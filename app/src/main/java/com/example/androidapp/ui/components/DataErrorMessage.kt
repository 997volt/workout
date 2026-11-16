package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.DataError

/**
 * Turns a [DataError] into something a user can read.
 *
 * Shared, because every screen that can fail a write needs the same three cases
 * and they must not drift apart. `Invalid` is shown verbatim: it was written for
 * the user by the layer that rejected the input ("that backup was written by a
 * newer version"), which is more useful than any generic wording here.
 */
@Composable
fun dataErrorMessage(error: DataError): String = dataErrorMessage(
    error = error,
    notFound = stringResource(R.string.workout_error_not_found),
    storage = stringResource(R.string.workout_error_storage),
)

/** The same answer when the caller already has the two strings, e.g. to keep a message saveable. */
fun dataErrorMessage(error: DataError, notFound: String, storage: String): String = when (error) {
    DataError.NotFound -> notFound
    is DataError.Invalid -> error.message
    is DataError.Storage -> storage
}
