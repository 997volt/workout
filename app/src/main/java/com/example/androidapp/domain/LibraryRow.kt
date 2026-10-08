package com.example.androidapp.domain

/**
 * One line of the grouped library.
 *
 * [depth] is what the screen indents by, and what tells a head from a row under one. It is computed here
 * rather than by the composable because "how deep is this" is a fact about the library's shape rather than
 * about the list that draws it.
 */
data class LibraryRow(
    val id: String,
    val name: String,
    val subtitle: String?,
    val depth: Int,
    val isCategory: Boolean,
    /** The children a category holds, in order; zero for a movement. */
    val childCount: Int = 0,
    /**
     * True when this row's children are being shown, or when it has none to show.
     *
     * A movement is always "open" — it has nothing to fold — so the screen asks one field rather than
     * asking the kind first and the fold second.
     */
    val isExpanded: Boolean = true,
)
