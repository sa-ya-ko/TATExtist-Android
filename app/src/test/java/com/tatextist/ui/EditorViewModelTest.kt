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
import com.tatextist.data.GoogleDriveRepository

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private lateinit var viewModel: EditorViewModel
    private lateinit var mockDriveRepository: GoogleDriveRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockDriveRepository = mockk(relaxed = true)
        viewModel = EditorViewModel(driveRepository = mockDriveRepository)
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
}

