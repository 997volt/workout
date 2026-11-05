package com.example.androidapp.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.ui.theme.AndroidAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Section headers and screen titles are headings to a screen reader (B353).
 *
 * Every long screen is otherwise one flat focus list; these are the two components that make
 * TalkBack's heading navigation possible.
 */
@RunWith(AndroidJUnit4::class)
class HeadingSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aSectionHeader_isAHeading() {
        composeTestRule.setContent { AndroidAppTheme { SectionHeader("Workout") } }

        composeTestRule.onNodeWithText("Workout")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun aTopBarTitle_isAHeading() {
        composeTestRule.setContent { AndroidAppTheme { TopBarTitle("Statistics") } }

        composeTestRule.onNodeWithText("STATISTICS")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
