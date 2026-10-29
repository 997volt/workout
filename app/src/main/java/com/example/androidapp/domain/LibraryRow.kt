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
    /**
     * How many movements this family holds, whether or not a search found them; zero for a movement
     * (ROADMAP N225).
     *
     * The family's own size rather than the number of rows drawn under it, because that is what the head
     * claims: a search used to make a family of six read "1 exercise", which is a statement about the library
     * that is not true.
     */
    val childCount: Int = 0,
    /**
     * How many of them the search found, or [childCount] when there is no search (ROADMAP N225).
     *
     * Equal to [childCount] for the ordinary case, which is why the head can print one number without asking
     * whether a query is on.
     */
    val matchedChildCount: Int = childCount,
    /**
     * True when this row's children are being shown, or when it has none to show.
     *
     * A movement is always "open" — it has nothing to fold — so the screen asks one field rather than
     * asking the kind first and the fold second.
     */
    val isExpanded: Boolean = true,
) {
    /** True when a search is showing only part of this family, so the head states both numbers (N225). */
    val isNarrowed: Boolean get() = matchedChildCount != childCount
}
