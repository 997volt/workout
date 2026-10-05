package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.TenPointScale

/**
 * A picked list of named sites, each carrying its own 1–10 score (ROADMAP N62, N63).
 *
 * Extracted at its second caller: the readiness note picks sore muscles (N62) and the per-exercise
 * rating picks painful joints with their side (N63). Both are the same interaction — add one from a
 * dropdown, step its score, take it off — and only the vocabulary differs, so the vocabulary is what
 * the callers pass in. Building it twice would mean two places to fix a stepped score.
 *
 * The editor reports rather than stores: it draws [picked] and calls back on every change, so the
 * dialog that owns the list decides what a save means.
 *
 * A dropdown rather than chips because the option sets have words on them and do not fit a phone's
 * dialog width. The list scrolls within its own box, so a lifter who names everything still reaches
 * the buttons that commit the dialog.
 */
@Composable
internal fun ScoredPicksEditor(
    picked: List<ScoredPick>,
    options: List<ScoredPick>,
    tags: ScoredPickTags,
    strings: ScoredPickStrings,
    onAdd: (String) -> Unit,
    onScore: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    // A site already on the list is not offered again: two rows for one site would make "quads 8"
    // and "quads 3" both true, and a rating has one score per site.
    val available = options.filter { option -> picked.none { it.key == option.key } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = SCORED_PICKS_MAX_HEIGHT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = strings.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        picked.forEach { pick ->
            ScoredPickRow(pick = pick, tags = tags, strings = strings, onScore = onScore, onRemove = onRemove)
        }
        Box {
            AppTextButton(
                onClick = { picking = true },
                modifier = Modifier.testTag(tags.add),
                enabled = available.isNotEmpty(),
            ) {
                Text(strings.add)
            }
            DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
                available.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            picking = false
                            onAdd(option.key)
                        },
                        modifier = Modifier.testTag(tags.option(option.key)),
                    )
                }
            }
        }
    }
}

/** One picked site: its name, its own 1–10 score, and the control that takes it off. */
@Composable
private fun ScoredPickRow(
    pick: ScoredPick,
    tags: ScoredPickTags,
    strings: ScoredPickStrings,
    onScore: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(tags.row(pick.key)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pick.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        // A stepper rather than a field: the score can only be one of ten values, and a number box
        // would need the same "is this on the scale" guard the repository already has to keep.
        IconButton(
            onClick = { onScore(pick.key, pick.score - 1) },
            enabled = pick.score > TenPointScale.MIN,
            modifier = Modifier.testTag(tags.decrease(pick.key)),
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = strings.decrease(pick.label),
            )
        }
        Text(
            text = stringResource(R.string.scored_pick_score, pick.score),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag(tags.score(pick.key)),
        )
        IconButton(
            onClick = { onScore(pick.key, pick.score + 1) },
            enabled = pick.score < TenPointScale.MAX,
            modifier = Modifier.testTag(tags.increase(pick.key)),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = strings.increase(pick.label),
            )
        }
        IconButton(
            onClick = { onRemove(pick.key) },
            modifier = Modifier.testTag(tags.remove(pick.key)),
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = strings.remove(pick.label),
            )
        }
    }
}

/** One entry a scored-picks list offers or holds: an identity, a name, and a score (N62, N63). */
internal data class ScoredPick(
    /** Stable identity: what a test tag and a callback name. */
    val key: String,
    /** The user-facing name of this pick. */
    val label: String,
    /** 1–10. An option carries the score a fresh pick starts on. */
    val score: Int,
)

/**
 * The tag prefixes the two scored-picks lists derive their controls from (N62, N63).
 *
 * They live beside [ScoredPickTags] rather than in `TestTags` because they are the editor's own naming
 * scheme and `TestTags`' constants are derived from them: the coverage gate reads every `const val` in
 * `TestTags` as a tag a test must assert on, and a prefix is not a tag.
 */
internal const val READINESS_SORE_TAG_PREFIX = "readiness_sore"
internal const val RATING_JOINT_TAG_PREFIX = "rating_joint"

/**
 * The test tags of one scored-picks list, derived from its prefix (N62, N63).
 *
 * A prefix rather than seven tag values: the scheme is `"<prefix>_<control>[_<key>]"` for both lists,
 * and the constants that spell it out live in `TestTags`, derived from the same prefixes.
 */
internal class ScoredPickTags(private val prefix: String) {
    val add: String get() = "${prefix}_add"

    fun option(key: String): String = "${prefix}_option_$key"

    fun row(key: String): String = "${prefix}_row_$key"

    fun score(key: String): String = "${prefix}_score_$key"

    fun decrease(key: String): String = "${prefix}_decrease_$key"

    fun increase(key: String): String = "${prefix}_increase_$key"

    fun remove(key: String): String = "${prefix}_remove_$key"
}

/**
 * The wording of one scored-picks list.
 *
 * The three per-pick strings are composable, because each names the pick it acts on and the name is
 * only known once the row is drawn.
 */
internal class ScoredPickStrings(
    val title: String,
    val add: String,
    val decrease: @Composable (String) -> String,
    val increase: @Composable (String) -> String,
    val remove: @Composable (String) -> String,
)

/** How tall a scored list may grow before it scrolls, keeping the dialog's buttons on screen. */
private val SCORED_PICKS_MAX_HEIGHT = 200.dp

/**
 * The score a freshly added pick starts on: the bottom of the scale (ROADMAP N62, N63).
 *
 * A pick is added the moment the lifter names the site, before they have said how bad it is, and 1
 * claims the least rather than the middle — a 5 would record a severity nobody chose and read back as
 * though they had. The lifter steps it up to what it actually is.
 */
internal const val DEFAULT_SCORED_PICK = 1
