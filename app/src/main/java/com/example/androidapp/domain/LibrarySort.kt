package com.example.androidapp.domain

/**
 * How the library is ordered (ROADMAP N230).
 *
 * **Name is the default and stays it**: a lifter who never opens the control sees the library they have, and
 * the list only moves when the sort is asked for. Last trained sorts by the read the chip and the detail
 * screen use, so the three cannot disagree about when a lift was last trained.
 */
enum class LibrarySort {
    /** Families by name, movements by name under each, unfiled movements last (the library as it was). */
    NAME,

    /** Most recently trained first, never trained last — the list you read from the bottom (N230). */
    LAST_TRAINED,
}
