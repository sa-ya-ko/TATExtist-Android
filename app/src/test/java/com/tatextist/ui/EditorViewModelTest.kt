package com.tatextist.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import com.tatextist.data.GoogleDriveRepository
import com.tatextist.data.LocalFileRepository
import android.net.Uri

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private lateinit var viewModel: EditorViewModel
    private lateinit var mockDriveRepository: GoogleDriveRepository
    private lateinit var mockLocalFileRepository: LocalFileRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockDriveRepository = mockk(relaxed = true)
        mockLocalFileRepository = mockk(relaxed = true)
        viewModel = EditorViewModel(
            driveRepository = mockDriveRepository,
            localFileRepository = mockLocalFileRepository
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when title is updated, state should reflect the new title`() = runTest {
        viewModel.uiState.test {
            // Initial state
            assertEquals("無題のドキュメント", awaitItem().documentTitle)

            // Action
            viewModel.updateTitle("新しいタイトル")

            // State changes (this will fail until we implement updateTitle)
            assertEquals("新しいタイトル", awaitItem().documentTitle)
        }
    }

    @Test
    fun `when content is updated with selection, state should reflect the new content and selection`() = runTest {
        viewModel.uiState.test {
            // Initial state (should default to empty TextFieldValue)
            assertEquals(androidx.compose.ui.text.input.TextFieldValue(), awaitItem().content)

            // Action: Text with selection
            val newValue = androidx.compose.ui.text.input.TextFieldValue(
                text = "こんにちは、世界",
                selection = androidx.compose.ui.text.TextRange(0, 5)
            )
            viewModel.updateContent(newValue)

            // State changes (will fail to compile/run because updateContent doesn't accept TextFieldValue yet)
            val newState = awaitItem().content
            assertEquals("こんにちは、世界", newState.text)
            assertEquals(androidx.compose.ui.text.TextRange(0, 5), newState.selection)
        }
    }
    @Test
    fun `when saveToCloud succeeds, isCloudLoading should be false and no error`() = runTest {
        coEvery { mockDriveRepository.signIn() } returns Result.success(Unit)
        coEvery { mockDriveRepository.saveToDrive(any(), any()) } returns Result.success(Unit)

        // Action
        viewModel.saveToCloud()

        viewModel.uiState.test {
            // State after save succeeds (due to UnconfinedTestDispatcher, execution is immediate)
            val successState = awaitItem()
            assertEquals(false, successState.isCloudLoading)
            assertEquals(null, successState.cloudError)
        }
    }

    @Test
    fun `when loadFromCloud fails, error message should be set`() = runTest {
        coEvery { mockDriveRepository.signIn() } returns Result.success(Unit)
        coEvery { mockDriveRepository.loadFromDrive(any()) } returns Result.failure(Exception("Not Found"))

        // Action
        viewModel.loadFromCloud("file.txt")

        viewModel.uiState.test {
            // State after load fails
            val errorState = awaitItem()
            assertEquals(false, errorState.isCloudLoading)
            assertEquals("Not Found", errorState.cloudError)
        }
    }

    @Test
    fun `when content is updated, character and line counts should be updated`() = runTest {
        viewModel.uiState.test {
            // Initial state: empty text
            val initial = awaitItem()
            assertEquals(0, initial.characterCount)
            assertEquals(0, initial.characterCountWithoutSpaces)
            assertEquals(1, initial.lineCount)

            // Action: input multi-line text with spaces
            val textWithSpaces = "こんにちは\n 世界"
            viewModel.updateContent(androidx.compose.ui.text.input.TextFieldValue(text = textWithSpaces))

            // State changes:
            // "こんにちは\n 世界" length = 9 chars
            // without spaces/newlines: "こんにちは世界" = 7 chars
            // lines: 2 lines
            val updated = awaitItem()
            assertEquals(9, updated.characterCount)
            assertEquals(7, updated.characterCountWithoutSpaces)
            assertEquals(2, updated.lineCount)
        }
    }

    @Test
    fun `word count modal and live display state should be toggled correctly`() = runTest {
        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(false, initial.showWordCountModal)
            assertEquals(false, initial.showWordCountWhileTyping)

            // Open modal
            viewModel.openWordCountModal()
            assertEquals(true, awaitItem().showWordCountModal)

            // Toggle live display
            viewModel.toggleShowWordCountWhileTyping(true)
            assertEquals(true, awaitItem().showWordCountWhileTyping)

            // Close modal
            viewModel.closeWordCountModal()
            assertEquals(false, awaitItem().showWordCountModal)
        }
    }

    @Test
    fun `when signOutFromCloud succeeds, isDriveSignedIn should be false`() = runTest {
        coEvery { mockDriveRepository.signOut() } returns Result.success(Unit)

        viewModel.signOutFromCloud()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isDriveSignedIn)
        }
    }

    @Test
    fun `when fetchDriveFiles succeeds, cloudFiles and showFilePickerModal should be updated`() = runTest {
        val mockFiles = listOf(
            com.tatextist.data.DriveFileInfo(id = "1", name = "作品1.txt"),
            com.tatextist.data.DriveFileInfo(id = "2", name = "作品2.txt")
        )
        coEvery { mockDriveRepository.signIn() } returns Result.success(Unit)
        coEvery { mockDriveRepository.listDriveFiles() } returns Result.success(mockFiles)

        viewModel.fetchDriveFiles()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(true, state.showFilePickerModal)
            assertEquals(2, state.cloudFiles.size)
            assertEquals("作品1.txt", state.cloudFiles[0].name)
        }
    }

    @Test
    fun `when loadDriveFile succeeds, content and title should update and modal should close`() = runTest {
        val targetFile = com.tatextist.data.DriveFileInfo(id = "file123", name = "吾輩は猫である.txt")
        coEvery { mockDriveRepository.signIn() } returns Result.success(Unit)
        coEvery { mockDriveRepository.loadFromDriveById("file123") } returns Result.success("吾輩は猫である。名前はまだ無い。")

        viewModel.loadDriveFile(targetFile)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.showFilePickerModal)
            assertEquals("吾輩は猫である.txt", state.documentTitle)
            assertEquals("吾輩は猫である。名前はまだ無い。", state.content.text)
        }
    }

    @Test
    fun `when fontFamily is updated, state should reflect the new fontFamily`() = runTest {
        viewModel.uiState.test {
            assertEquals(EditorFontFamily.SERIF, awaitItem().fontFamily)

            viewModel.updateFontFamily(EditorFontFamily.SANS_SERIF)
            assertEquals(EditorFontFamily.SANS_SERIF, awaitItem().fontFamily)
        }
    }

    @Test
    fun `when fontSize is increased or decreased, state should update within bounds`() = runTest {
        viewModel.uiState.test {
            assertEquals(20, awaitItem().fontSize)

            viewModel.increaseFontSize()
            assertEquals(21, awaitItem().fontSize)

            viewModel.decreaseFontSize()
            assertEquals(20, awaitItem().fontSize)

            // Test custom direct update and boundaries
            viewModel.updateFontSize(36)
            assertEquals(36, awaitItem().fontSize)

            viewModel.increaseFontSize() // Should not exceed 36
            expectNoEvents()

            viewModel.updateFontSize(12)
            assertEquals(12, awaitItem().fontSize)

            viewModel.decreaseFontSize() // Should not go below 12
            expectNoEvents()
        }
    }

    @Test
    fun `when font settings bottom sheet is opened or closed, state should reflect accordingly`() = runTest {
        viewModel.uiState.test {
            assertEquals(false, awaitItem().showFontSettingsBottomSheet)

            viewModel.openFontSettingsBottomSheet()
            assertEquals(true, awaitItem().showFontSettingsBottomSheet)

            viewModel.closeFontSettingsBottomSheet()
            assertEquals(false, awaitItem().showFontSettingsBottomSheet)
        }
    }

    @Test
    fun `updateDetectedStyle_synchronizesUiStateWithSelectedText`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateFontFamily(EditorFontFamily.SANS_SERIF)
            assertEquals(EditorFontFamily.SANS_SERIF, awaitItem().fontFamily)
            viewModel.updateFontSize(30)
            assertEquals(30, awaitItem().fontSize)

            viewModel.updateDetectedStyle(EditorFontFamily.SERIF, 20)

            val detectedState = awaitItem()
            assertEquals(EditorFontFamily.SERIF, detectedState.fontFamily)
            assertEquals(20, detectedState.fontSize)
        }
    }

    @Test
    fun `updateFontFamily_emitsApplyRequestToWebView`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateFontFamily(EditorFontFamily.SANS_SERIF)

            val state = awaitItem()
            assertNotNull(state.fontApplyRequest)
            assertEquals(EditorFontFamily.SANS_SERIF, state.fontApplyRequest?.fontFamily)
        }
    }

    @Test
    fun `toggleDarkMode_shouldInvertIsDarkModeState`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(false, initial.isDarkMode)

            viewModel.toggleDarkMode()
            assertEquals(true, awaitItem().isDarkMode)

            viewModel.toggleDarkMode()
            assertEquals(false, awaitItem().isDarkMode)
        }
    }

    @Test
    fun `updateContent_extractsRubyListAutomatically`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateContent(
                androidx.compose.ui.text.input.TextFieldValue("吾輩は|猫《ねこ》である")
            )

            val state = awaitItem()
            assertEquals(1, state.rubyList.size)
            assertEquals("猫", state.rubyList[0].parentText)
            assertEquals("ねこ", state.rubyList[0].rubyText)
        }
    }

    @Test
    fun `removeRubyItem_removesRubyFromContent`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateContent(
                androidx.compose.ui.text.input.TextFieldValue("吾輩は|猫《ねこ》である")
            )
            val stateWithRuby = awaitItem()
            val targetRuby = stateWithRuby.rubyList[0]

            viewModel.removeRubyItem(targetRuby)

            val updatedState = awaitItem()
            assertEquals("吾輩は猫である", updatedState.content.text)
            assertEquals(0, updatedState.rubyList.size)
        }
    }

    @Test
    fun `removeRubyItem_withConsecutiveRubies_doesNotCorruptPrecedingRuby`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateContent(
                androidx.compose.ui.text.input.TextFieldValue("|検索《けんさく》|機能《きのう》")
            )
            val state = awaitItem()
            assertEquals(2, state.rubyList.size)
            assertEquals("検索", state.rubyList[0].parentText)
            assertEquals("機能", state.rubyList[1].parentText)

            viewModel.removeRubyItem(state.rubyList[1])

            val updated = awaitItem()
            assertEquals("|検索《けんさく》機能", updated.content.text)
            assertEquals(1, updated.rubyList.size)
            assertEquals("検索", updated.rubyList[0].parentText)
            assertEquals("けんさく", updated.rubyList[0].rubyText)
        }
    }

    @Test
    fun `setRubyListActive_updatesIsRubyListActiveState`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(false, initial.isRubyListActive)

            viewModel.setRubyListActive(true)
            assertEquals(true, awaitItem().isRubyListActive)

            viewModel.setRubyListActive(false)
            assertEquals(false, awaitItem().isRubyListActive)
        }
    }

    @Test
    fun `jumpToRuby_setsTargetRubyIndexAndConsumesIt`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.jumpToRuby(2)
            val stateWithJump = awaitItem()
            assertEquals(2, stateWithJump.targetRubyIndex)

            viewModel.consumeRubyJump()
            assertEquals(null, awaitItem().targetRubyIndex)
        }
    }

    @Test
    fun `updateIndentWidth_updatesWidthAndGuardsAgainstInvalidValues`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            // デフォルトは「インデントなし（0.0f）」
            assertEquals(0.0f, initial.indentWidth)

            viewModel.updateIndentWidth(0.5f)
            assertEquals(0.5f, awaitItem().indentWidth)

            viewModel.updateIndentWidth(1.5f)
            assertEquals(1.5f, awaitItem().indentWidth)

            viewModel.updateIndentWidth(0.0f)
            assertEquals(0.0f, awaitItem().indentWidth)

            // 範囲外（負の数や4.5f以上）の不正値は弾かれる
            viewModel.updateIndentWidth(-0.5f)
            viewModel.updateIndentWidth(4.5f)
            expectNoEvents()
        }
    }

    @Test
    fun `when saveToLocalFile succeeds, localFileMessage should be set`() = runTest {
        val testUri: Uri = mockk()
        coEvery { mockLocalFileRepository.saveText(testUri, any()) } returns Result.success(Unit)

        viewModel.saveToLocalFile(testUri)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("端末に保存しました", state.localFileMessage)
            assertEquals(null, state.localFileError)
        }
    }

    @Test
    fun `when saveToLocalFile fails, localFileError should be set`() = runTest {
        val testUri: Uri = mockk()
        coEvery { mockLocalFileRepository.saveText(testUri, any()) } returns Result.failure(Exception("Disk full"))

        viewModel.saveToLocalFile(testUri)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("ファイルの保存に失敗しました: Disk full", state.localFileError)
            assertEquals(null, state.localFileMessage)
        }
    }

    @Test
    fun `when saveToLocalFile succeeds, metadata header should be included in saved text`() = runTest {
        val testUri: Uri = mockk()
        val capturedText = slot<String>()
        coEvery { mockLocalFileRepository.saveText(testUri, capture(capturedText)) } returns Result.success(Unit)

        viewModel.setPaperSize(PaperSize.A4)
        viewModel.updateMargins(top = 32, bottom = 28, right = 16, left = 24)
        viewModel.updateContent(androidx.compose.ui.text.input.TextFieldValue("テスト本文"))

        viewModel.saveToLocalFile(testUri)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("端末に保存しました", state.localFileMessage)
            assertEquals(null, state.localFileError)
        }

        val expectedHeader = "［＃レイアウト設定：paper=A4,top=32,bottom=28,right=16,left=24］\n"
        org.junit.Assert.assertTrue(capturedText.captured.startsWith(expectedHeader))
        org.junit.Assert.assertTrue(capturedText.captured.endsWith("テスト本文"))
    }

    @Test
    fun `when loadFromLocalFile with metadata header succeeds, content and layout settings should be restored`() = runTest {
        val testUri: Uri = mockk()
        val fileContentWithMeta = "［＃レイアウト設定：paper=B5,top=40,bottom=35,right=15,left=20］\n|親文字《ルビ》テスト"
        coEvery { mockLocalFileRepository.readText(testUri) } returns Result.success(fileContentWithMeta)

        viewModel.loadFromLocalFile(testUri)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("端末からファイルを読み込みました", state.localFileMessage)
            assertEquals(null, state.localFileError)
            assertEquals("<ruby>親文字<rt>ルビ</rt></ruby>テスト", state.content.text)
            assertEquals(PaperSize.B5, state.paperSize)
            assertEquals(EditorMargins(top = 40, bottom = 35, right = 15, left = 20), state.margins)
        }
    }

    @Test
    fun `when loadFromLocalFile fails, localFileError should be set`() = runTest {
        val testUri: Uri = mockk()
        coEvery { mockLocalFileRepository.readText(testUri) } returns Result.failure(Exception("File not found"))

        viewModel.loadFromLocalFile(testUri)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("ファイルの読み込みに失敗しました: File not found", state.localFileError)
            assertEquals(null, state.localFileMessage)
        }
    }

    @Test
    fun `when setPaperSize is called, paperSize in uiState should be updated`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(PaperSize.FIT, initial.paperSize)

            viewModel.setPaperSize(PaperSize.A4)
            val updatedToA4 = awaitItem()
            assertEquals(PaperSize.A4, updatedToA4.paperSize)

            viewModel.setPaperSize(PaperSize.B5)
            val updatedToB5 = awaitItem()
            assertEquals(PaperSize.B5, updatedToB5.paperSize)

            viewModel.setPaperSize(PaperSize.FIT)
            val updatedToFit = awaitItem()
            assertEquals(PaperSize.FIT, updatedToFit.paperSize)
        }
    }

    @Test
    fun `when updateMargins is called, margins in uiState should be updated`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(EditorMargins(top = 20, bottom = 20, right = 20, left = 20), initial.margins)

            viewModel.updateMargins(top = 30, right = 15)
            val updated1 = awaitItem()
            assertEquals(EditorMargins(top = 30, bottom = 20, right = 15, left = 20), updated1.margins)

            viewModel.updateMargins(bottom = 40, left = 25)
            val updated2 = awaitItem()
            assertEquals(EditorMargins(top = 30, bottom = 40, right = 15, left = 25), updated2.margins)
        }
    }

    @Test
    fun `when requestPrint called with content, printRequestId should be set`() = runTest {
        viewModel.updateContent(androidx.compose.ui.text.input.TextFieldValue("印刷テスト"))
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(null, initial.printRequestId)

            viewModel.requestPrint()
            val stateWithPrint = awaitItem()
            org.junit.jupiter.api.Assertions.assertNotNull(stateWithPrint.printRequestId)

            viewModel.consumePrintRequest()
            val stateConsumed = awaitItem()
            assertEquals(null, stateConsumed.printRequestId)
        }
    }

    @Test
    fun `when requestPrint called with empty content, error message should be set`() = runTest {
        viewModel.updateContent(androidx.compose.ui.text.input.TextFieldValue(""))
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(null, initial.printRequestId)

            viewModel.requestPrint()
            val stateError = awaitItem()
            assertEquals("印刷するテキストがありません", stateError.localFileError)
            assertEquals(null, stateError.printRequestId)
        }
    }

    @Test
    fun `when textColor is updated with hex, state and fontApplyRequest should reflect the new color`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(null, initial.selectedTextColor)

            viewModel.updateTextColor("#E53935")
            val updated = awaitItem()
            assertEquals("#E53935", updated.selectedTextColor)
            assertEquals("#E53935", updated.fontApplyRequest?.textColor)
        }
    }

    @Test
    fun `when textColor is reset, fontApplyRequest should request color reset`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateTextColor("#E53935")
            assertEquals("#E53935", awaitItem().selectedTextColor)

            viewModel.resetTextColor()
            val resetState = awaitItem()
            assertEquals(null, resetState.selectedTextColor)
            assertEquals("", resetState.fontApplyRequest?.textColor) // 空文字でリセットを表現
        }
    }

    @Test
    fun `updateDetectedColor_synchronizesDetectedTextColor`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateDetectedColor("#1E88E5")
            val detected = awaitItem()
            assertEquals("#1E88E5", detected.selectedTextColor)
        }
    }

    @Test
    fun `when paragraph style is applied, state and paragraphStyleRequest should reflect the new style`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(ParagraphStyle.NORMAL, initial.currentParagraphStyle)
            assertEquals(null, initial.paragraphStyleRequest)

            viewModel.applyParagraphStyle(ParagraphStyle.TITLE)
            val updated = awaitItem()
            assertEquals(ParagraphStyle.TITLE, updated.currentParagraphStyle)
            assertEquals(ParagraphStyle.TITLE, updated.paragraphStyleRequest)
        }
    }

    @Test
    fun `when same paragraph style is applied again, it should reset to NORMAL`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.applyParagraphStyle(ParagraphStyle.HEADING_1)
            assertEquals(ParagraphStyle.HEADING_1, awaitItem().currentParagraphStyle)

            viewModel.applyParagraphStyle(ParagraphStyle.HEADING_1)
            val resetState = awaitItem()
            assertEquals(ParagraphStyle.NORMAL, resetState.currentParagraphStyle)
            assertEquals(ParagraphStyle.NORMAL, resetState.paragraphStyleRequest)
        }
    }

    @Test
    fun `when onParagraphStyleConsumed is called, paragraphStyleRequest should become null`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.applyParagraphStyle(ParagraphStyle.HEADING_2)
            awaitItem()

            viewModel.onParagraphStyleConsumed()
            val consumedState = awaitItem()
            assertEquals(null, consumedState.paragraphStyleRequest)
            assertEquals(ParagraphStyle.HEADING_2, consumedState.currentParagraphStyle)
        }
    }

    @Test
    fun `when onParagraphStyleDetected is called with tag, currentParagraphStyle should synchronize`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onParagraphStyleDetected("h3")
            val detected = awaitItem()
            assertEquals(ParagraphStyle.HEADING_2, detected.currentParagraphStyle)

            viewModel.onParagraphStyleDetected("p")
            val reset = awaitItem()
            assertEquals(ParagraphStyle.NORMAL, reset.currentParagraphStyle)
        }
    }
}



