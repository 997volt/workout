package com.example.androidapp.ui.components

import com.example.androidapp.domain.DataError

/**
 * One write's answer to the editor that asked for it (N284).
 *
 * The editor waits for the write, so a refusal keeps its values instead of closing on the tap. The answer
 * names the row it is about; a screen-global success flag cannot distinguish two editors for the same set,
 * and an answer left sticky cannot make the same success happen twice. Workout, template and history editors
 * use this type; the host clears it when the editor closes.
 */
sealed interface WriteOutcome<out T> {
    data class Written<T>(val value: T) : WriteOutcome<T>
    data class Refused<T>(val value: T, val error: DataError) : WriteOutcome<T>
}
