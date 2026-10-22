package com.example.androidapp.domain.model

/**
 * One followed block's place, as the adherence screen says it (ROADMAP N189).
 *
 * [position] is one-based and equals [length] when the block is complete; [complete] is what the
 * sentence uses, so a finished block does not read as "day 9 of 9" waiting to be done.
 */
data class BlockProgress(
    val name: String,
    val position: Int,
    val length: Int,
    val complete: Boolean,
)
