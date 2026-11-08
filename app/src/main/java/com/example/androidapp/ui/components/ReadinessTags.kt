package com.example.androidapp.ui.components

/**
 * The readiness note's own controls (ROADMAP N4, N62).
 */
internal object ReadinessTags {
    /** The control that opened the muscle picker, kept so a test can assert it is gone (N113). */
    const val SORE_ADD = "${READINESS_SORE_TAG_PREFIX}_add"

    /**
     * One muscle's **read-only** line in history (ROADMAP N62).
     *
     * A live session no longer writes the list (N113), and the rows an older one recorded still read
     * here rather than nowhere.
     */
    fun soreLine(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_line_$muscle"
}
