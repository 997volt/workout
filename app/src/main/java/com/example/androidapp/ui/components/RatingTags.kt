package com.example.androidapp.ui.components

/**
 * The joints an exercise reported painful (ROADMAP N63): every control is addressed by the joint and
 * side it acts on.
 */
internal object RatingTags {
    /** The control that opens the joint picker. */
    const val JOINT_ADD = "${RATING_JOINT_TAG_PREFIX}_add"

    /** One option of the picker, addressed by the `Joint` and `Side` names. */
    fun jointOption(site: String) = "${RATING_JOINT_TAG_PREFIX}_option_$site"

    /** One picked joint's row, and that row's own score controls. */
    fun jointRow(site: String) = "${RATING_JOINT_TAG_PREFIX}_row_$site"
    fun jointScore(site: String) = "${RATING_JOINT_TAG_PREFIX}_score_$site"
    fun jointDecrease(site: String) = "${RATING_JOINT_TAG_PREFIX}_decrease_$site"
    fun jointIncrease(site: String) = "${RATING_JOINT_TAG_PREFIX}_increase_$site"

    /** Takes the joint off the list. */
    fun jointRemove(site: String) = "${RATING_JOINT_TAG_PREFIX}_remove_$site"
}
