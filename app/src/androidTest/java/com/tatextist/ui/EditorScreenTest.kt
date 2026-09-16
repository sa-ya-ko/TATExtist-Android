package com.tatextist.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editorScreen_displaysInitialTitleAndContent() {
        // 準備 (Arrange): 画面を表示します
        composeTestRule.setContent {
            EditorScreen(uiState = EditorUiState())
        }

        // 確認 (Assert): 「無題のドキュメント」という文字が画面内にあるかチェックします
        composeTestRule.onNodeWithText("無題のドキュメント").assertIsDisplayed()
    }

    @Test
    fun editorScreen_hasRubyButtonInBottomAppBar() {
        // 準備 (Arrange): 画面を表示します
        composeTestRule.setContent { EditorScreen(uiState = EditorUiState()) }

        // 下部のツールバー（BottomAppBar）に「ルビ付与」というボタンが表示されているかチェック
        composeTestRule.onNodeWithText("ルビ付与").assertIsDisplayed()
    }

    @Test
    fun editorScreen_showsSettingsPanel_whenRubyButtonClicked() {
        // 1. 準備 (Arrange): 画面を表示します
        composeTestRule.setContent { EditorScreen(uiState = EditorUiState()) }

        // 初期状態では「設定パネル」は見えていないことを確認します
        composeTestRule.onNodeWithText("設定パネル").assertDoesNotExist()

        // 2. 実行 (Act): 「ルビ付与」というボタンを探してクリックします
        composeTestRule.onNodeWithText("ルビ付与").performClick()

        // 3. 確認 (Assert): 「設定パネル」が画面に現れたかをチェックします
        composeTestRule.onNodeWithText("設定パネル").assertIsDisplayed()
    }

    @Test
    fun editorScreen_appliesRuby_whenRubyIsEntered() {
        // 1. 準備
        composeTestRule.setContent { EditorScreen(uiState = EditorUiState()) }

        // 2. 実行
        // ①「ルビ付与」ボタンを押してパネルを開く
        composeTestRule.onNodeWithText("ルビ付与").performClick()

        // ②「例: ふりがな」と書かれた入力ボックスを探し、「かんじ」と入力する
        composeTestRule.onNodeWithText("例: ふりがな").performTextInput("かんじ")

        // ③「適用」ボタンを押す
        composeTestRule.onNodeWithText("適用").performClick()

        // 3. 確認
        // 適用ボタンが押された後、パネルがきちんと閉じて画面から消えているかを確認
        composeTestRule.onNodeWithText("適用").assertDoesNotExist()
    }
}
