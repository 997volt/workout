package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.WeightUnit

/**
 * The unit every weight is shown and typed in, unless the exercise names its own (ROADMAP N64).
 *
 * An ambient rather than one more parameter on every composable, for the reason the theme is one: it
 * is display configuration that *every* screen needs, and the alternative does not survive the
 * function-length ceilings this project enforces — the batch that added this had already been forced
 * to split three functions for exceeding them.
 *
 * The default is kilograms, so a preview, a test, or any screen rendered outside the app shell
 * behaves exactly as the app did before this existed. **Nothing outside composition reads it**:
 * `Weight.format`, `parse` and `stepGrams` all take the unit as a parameter, so no pure function
 * depends on an ambient.
 */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KILOGRAMS }

/**
 * The unit one exercise's numbers are read in: its own if it sets one, the app's otherwise (N64).
 *
 * Every display and parse site that belongs to an exercise goes through this rather than reading
 * [LocalWeightUnit] directly, which is what makes "everything is in lb for this exercise" one rule
 * instead of a rule per screen.
 */
@Composable
fun exerciseWeightUnit(override: WeightUnit?): WeightUnit = override ?: LocalWeightUnit.current

/**
 * What the unit is called where a weight is written out: `kg` or `lb`.
 *
 * The label belongs to a string resource rather than the enum, because a translation may put it
 * somewhere else in the sentence — which is why the number and its unit travel as two arguments.
 */
@Composable
fun WeightUnit.label(): String = stringResource(
    when (this) {
        WeightUnit.KILOGRAMS -> R.string.unit_kilograms
        WeightUnit.POUNDS -> R.string.unit_pounds
    },
)
