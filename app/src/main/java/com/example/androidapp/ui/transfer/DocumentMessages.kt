package com.example.androidapp.ui.transfer

import com.example.androidapp.domain.DataError

/**
 * The one sentence a rejected document is reported with (ROADMAP B53).
 *
 * An [DataError.Invalid] carries its own message because the layer that rejected the file wrote it
 * for the user; everything else is the storage failure the caller's own string names.
 */
internal fun documentErrorMessage(error: DataError, storageError: String): String = when (error) {
    is DataError.Invalid -> error.message
    DataError.NotFound -> storageError
    is DataError.Storage -> storageError
}
