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

/**
 * The colour a glyph draws in *on* each accent (ROADMAP B55).
 *
 * Not a blanket white, which is what this used to be: white against `Amber` is 2.5:1, against
 * `Teal` 2.9:1 and against `Coral` 3.0:1 — under the contrast a graphic needs, and a tile whose
 * glyph cannot be read is a tile that has stopped doing its one job. The page's own ink reads at
 * 5:1 or better on all four of those, and `Indigo` is the one accent dark enough that white is the
 * better half of the pair. The split is asserted rather than eyeballed, by `TileAccentTest`.
 */
val TileAccent.onColor: Color
    get() = when (this) {
        TileAccent.Indigo -> Color.White
        TileAccent.Teal, TileAccent.Coral, TileAccent.Amber, TileAccent.Sky -> Ink
    }
