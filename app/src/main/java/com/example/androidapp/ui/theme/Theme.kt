package com.example.androidapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The one colour scheme, dark by construction.
 *
 * Every role is written out rather than left to `darkColorScheme`'s defaults. The defaults are
 * Material's baseline purples, and a scheme that only overrides three of them keeps those
 * purples in every component nobody thought about — an error container here, a chip outline
 * there — which is how a themed app ends up looking half-themed.
 */
private val WorkoutColors = darkColorScheme(
    // The app's colour: primary actions, the selected tab, section headings that must lead.
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoDeep,
    onPrimaryContainer = Color.White,
    inversePrimary = IndigoDeep,

    // Teal and coral are category accents rather than second and third brands; a component
    // that reaches for `secondary` unthinkingly still lands on the palette.
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Teal,
    onSecondaryContainer = Color.White,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = Coral,
    onTertiaryContainer = Color.White,

    // Not black: the page sits below the cards, which are the lit surface.
    background = Ink,
    onBackground = Chalk,

    surface = Ink,
    onSurface = Chalk,
    surfaceVariant = InkRaised,
    onSurfaceVariant = ChalkMuted,
    surfaceTint = Indigo,

    // The tone ladder Material's newer components read from (cards, sheets, menus). Filled in
    // so a component that picks one of these gets a step of the palette rather than a default.
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkRaised,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHigh,
    surfaceDim = Ink,
    surfaceBright = InkHigh,

    inverseSurface = Chalk,
    inverseOnSurface = Ink,

    outline = Hairline,
    outlineVariant = Hairline,
    scrim = Scrim,

    // Coral is a category (the appointments tile); destruction reads redder on purpose.
    error = Crimson,
    onError = Color.White,
    errorContainer = Crimson,
    onErrorContainer = Color.White,
)

/**
 * Corners.
 *
 * Generous and consistent: the reference's cards are soft rectangles, and a list where the
 * card, the tile inside it and the button under it all share a family of radii is most of what
 * makes the surface look designed rather than assembled.
 */
private val WorkoutShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The app's theme.
 *
 * Takes no `darkTheme` and no `dynamicColor`: there is one scheme and it is the one above, so
 * the two parameters that used to pick between four of them are gone rather than defaulted.
 * The name is kept because every screen and every Robolectric test already wraps itself in it,
 * and a rename would be a mechanical edit through ~40 files that changes nothing a user sees.
 */
@Composable
fun AndroidAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WorkoutColors,
        typography = Typography,
        shapes = WorkoutShapes,
        content = content,
    )
}
