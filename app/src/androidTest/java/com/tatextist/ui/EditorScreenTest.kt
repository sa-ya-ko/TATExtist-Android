package com.tatextist.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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

    @Test
    fun editorScreen_displaysCharacterAndLineCount_onlyWhenEnabled() {
        // 通常時（showWordCountWhileTyping = false）は常時表示されない
        val defaultState = EditorUiState(characterCount = 42, lineCount = 3, showWordCountWhileTyping = false)
        composeTestRule.setContent { EditorScreen(uiState = defaultState) }
        composeTestRule.onNodeWithText("42字").assertDoesNotExist()

        // 有効時（showWordCountWhileTyping = true）は表示される（行数は表示されない）
        val enabledState = EditorUiState(characterCount = 42, lineCount = 3, showWordCountWhileTyping = true)
        composeTestRule.setContent { EditorScreen(uiState = enabledState) }
        composeTestRule.onNodeWithText("42字").assertIsDisplayed()
        composeTestRule.onNodeWithText("3行").assertDoesNotExist()
    }

    @Test
    fun editorScreen_tappingCharacterCount_opensWordCountModal() {
        var modalOpened = false
        val enabledState = EditorUiState(characterCount = 42, showWordCountWhileTyping = true)
        composeTestRule.setContent {
            EditorScreen(
                uiState = enabledState,
                onOpenWordCountModal = { modalOpened = true }
            )
        }

        // 下部バーの「42字」をタップするとコールバックが呼ばれること
        composeTestRule.onNodeWithText("42字").performClick()
        assert(modalOpened)
    }

    @Test
    fun editorScreen_displaysWordCountModal_whenShowWordCountModalIsTrue() {
        val modalState = EditorUiState(
            characterCount = 100,
            characterCountWithoutSpaces = 85,
            showWordCountModal = true
        )
        composeTestRule.setContent { EditorScreen(uiState = modalState) }

        // モーダル内の項目が表示されることを確認
        composeTestRule.onNodeWithText("文字カウント").assertIsDisplayed()
        composeTestRule.onNodeWithText("文字数（スペースなし）").assertIsDisplayed()
        composeTestRule.onNodeWithText("85").assertIsDisplayed()
        composeTestRule.onNodeWithText("文字数（スペースあり）").assertIsDisplayed()
        composeTestRule.onNodeWithText("100").assertIsDisplayed()
        composeTestRule.onNodeWithText("入力中に文字数を表示する").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun editorScreen_displaysFilePickerModal_andHandlesSelection() {
        var selectedFile: com.tatextist.data.DriveFileInfo? = null
        val testFiles = listOf(
            com.tatextist.data.DriveFileInfo(id = "1", name = "こころ.txt"),
            com.tatextist.data.DriveFileInfo(id = "2", name = "銀河鉄道の夜.txt")
        )
        val modalState = EditorUiState(
            cloudFiles = testFiles,
            showFilePickerModal = true
        )
        composeTestRule.setContent {
            EditorScreen(
                uiState = modalState,
                onSelectDriveFile = { selectedFile = it }
            )
        }

        // ダイアログのタイトルとファイル一覧が表示されること
        composeTestRule.onNodeWithText("Driveから開く").assertIsDisplayed()
        composeTestRule.onNodeWithText("こころ.txt").assertIsDisplayed()
        composeTestRule.onNodeWithText("銀河鉄道の夜.txt").assertIsDisplayed()

        // ファイル名をタップすると選択コールバックが呼ばれること
        composeTestRule.onNodeWithText("こころ.txt").performClick()
        assert(selectedFile?.name == "こころ.txt")
    }

    @Test
    fun editorScreen_allowsEditingTitle() {
        var updatedTitle = ""
        val initialState = EditorUiState(documentTitle = "無題のドキュメント")
        composeTestRule.setContent {
            EditorScreen(
                uiState = initialState,
                onTitleChange = { updatedTitle = it }
            )
        }

        // タイトル入力欄を見つけて新しいタイトルを入力する
        composeTestRule.onNodeWithText("無題のドキュメント").performTextInput(" (新)")
        assert(updatedTitle.isNotEmpty())
    }

    @Test
    fun editorScreen_hasUndoAndRedoButtons() {
        composeTestRule.setContent {
            EditorScreen(uiState = EditorUiState())
        }

        // Undo（元に戻す）ボタンとRedo（やり直す）ボタンが表示されていること
        composeTestRule.onNodeWithContentDescription("元に戻す").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("やり直す").assertIsDisplayed()
    }
}
