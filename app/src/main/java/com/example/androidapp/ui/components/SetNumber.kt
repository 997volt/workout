package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.SetType

/**
 * The number each set takes, or null for one that takes none (ROADMAP N112).
 *
 * One rule for the three lists that number sets — the workout's logged rows, the history detail's and
 * the template editor's planned ones — because they are three readings of one sequence: a **warm-up
 * takes no number**, so an exercise's first working set is 1 whatever its ramp did. A rung keeps its
 * number ([SetType.isWork]), because a run is work the plan asked for.
 *
 * [role] reads the role off each row, so each caller's own row type stays out of this.
 */
internal fun <T> List<T>.setNumbers(role: (T) -> SetType): List<Int?> {
    var number = 0
    return map { row -> if (role(row).isWork) ++number else null }
}

/** The width a blank number holds open, so a numbered row and a warm-up start at the same place. */
private val NUMBER_COLUMN_MIN_WIDTH = 20.dp

/**
 * A set's place among the sets that stand on their own, or the blank column a warm-up leaves
 * (ROADMAP N112).
 *
 * Blank rather than absent on purpose: dropping the number outright would slide a warm-up's load to
 * the left of every working set's, which reads as a different kind of row rather than as the same row
 * with no number. A warm-up is not left silent by this — its role marker says what it was.
 */
@Composable
internal fun SetNumber(number: Int?, modifier: Modifier = Modifier) {
    Text(
        text = number?.let { stringResource(R.string.set_number, it) }.orEmpty(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.widthIn(min = NUMBER_COLUMN_MIN_WIDTH),
    )
}
