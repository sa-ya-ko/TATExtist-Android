package com.tatextist.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class CanvasVerticalTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun canvasVerticalText_displaysText() {
        // 画用紙に「縦書きテスト」という文字を描かせるようにお願いします
        composeTestRule.setContent {
            CanvasVerticalText(text = "縦書きテスト")
        }

        // 画面に「縦書きテスト」という文字が存在しているか（正しく読める状態か）をチェックします
        composeTestRule.onNodeWithText("縦書きテスト").assertIsDisplayed()
    }
}
