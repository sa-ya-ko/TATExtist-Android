package com.tatextist.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Rule
import org.junit.Test

class VerticalEditorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verticalEditor_displaysWebView() {
        composeTestRule.setContent {
            VerticalEditor(
                value = TextFieldValue("吾輩は猫である"),
                onValueChange = {},
                modifier = Modifier.testTag("VerticalEditorWebView")
            )
        }

        // WebView（AndroidView）がクラッシュせずに画面に表示されることを確認する
        composeTestRule.onNodeWithTag("VerticalEditorWebView").assertIsDisplayed()
    }
}
