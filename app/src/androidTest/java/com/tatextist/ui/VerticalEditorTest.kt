package com.tatextist.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class VerticalEditorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verticalEditor_displaysText() {
        composeTestRule.setContent {
            VerticalEditor(
                text = "吾輩は猫である",
                onTextChange = {}
            )
        }

        // 入力したテキストが正しく画面に表示されているかチェック
        composeTestRule.onNodeWithText("吾輩は猫である").assertIsDisplayed()
    }
}
