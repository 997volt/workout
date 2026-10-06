package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.SetType

/**
 * The role picker — one vocabulary for a set that is planned and a set that was
 * performed (ROADMAP N14).
 *
 * Shared because the plan dialog and the set editor had a copy each: two near-identical
 * dropdowns over the same enum, which is how "the same roles everywhere" turns into two
 * lists that drift. A dropdown rather than chips because the roles with words on them
 * do not fit a phone's dialog width.
 */
@Composable
fun SetRoleSelector(
    role: SetType,
    onSelect: (SetType) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = TestTags.SET_ROLE,
    optionTag: (String) -> String = TestTags::setRole,
    /**
     * Which roles may be chosen at this place in the plan or the session (ROADMAP N79, B64).
     *
     * A rung hangs off the set above it, so a control that offers **Drop** or **Cluster** where there is
     * nothing to hang off invites a tap the write boundary then refuses. The caller knows the position
     * and answers this; the default offers every role, which is what a picker with no position to speak
     * of — a preview, a component test — wants.
     *
     * The armed [role] stays listed whether or not it is offered, so the control always shows what the
     * set currently is: a plan's next prescribed set can be a rung this position cannot take, and a menu
     * whose selected value is missing from it reads as a bug.
     */
    offers: (SetType) -> Boolean = { true },
) {
    var open by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        AppTextButton(
            onClick = { open = true },
            modifier = Modifier.testTag(testTag),
        ) {
            Text(stringResource(R.string.template_set_role, role.label))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SetType.entries.filter { offers(it) || it == role }.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    modifier = Modifier.testTag(optionTag(option.name)),
                )
            }
        }
    }
}
