package com.example.androidapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Which accent a coloured surface wears.
 *
 * Named by colour rather than by meaning on purpose. An accent says "this row is a different
 * kind of thing from that row", and binding a hue to a concept (`Exercise`, `Program`) would
 * mean inventing a sixth colour the day a new kind of row appears, and reusing a colour — and
 * so losing the distinction — every time one is retired.
 */
enum class TileAccent {
    Indigo,
    Teal,
    Coral,
    Amber,
    Sky,
}

/**
 * The colour each accent draws in.
 *
 * Every branch names its colour through [TileAccent] on the left and the palette on the right.
 * That is not decoration: inside this file the two sets of names are the same five words, so an
 * unqualified right-hand side resolves to the *entry* and the property answers with itself.
 */
val TileAccent.color: Color
    get() = when (this) {
        TileAccent.Indigo -> Indigo
        TileAccent.Teal -> Teal
        TileAccent.Coral -> Coral
        TileAccent.Amber -> Amber
        TileAccent.Sky -> Sky
    }
