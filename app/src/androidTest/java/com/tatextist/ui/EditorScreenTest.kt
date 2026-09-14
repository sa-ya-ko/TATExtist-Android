package com.tatextist.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editorScreen_displaysTopAndBottomBars() {
        // テスト用のダミー状態を作成
        val testState = EditorUiState(documentTitle = "テスト文書")

        // 画面を起動
        composeTestRule.setContent {
            EditorScreen(uiState = testState)
        }

        // 上部のタイトルバー（TopAppBar）に「テスト文書」という文字が表示されているかチェック
        composeTestRule.onNodeWithText("テスト文書").assertIsDisplayed()
        
        // 下部のツールバー（BottomAppBar）に「ルビ付与」というボタンが表示されているかチェック
        composeTestRule.onNodeWithText("ルビ付与").assertIsDisplayed()
    }
}
