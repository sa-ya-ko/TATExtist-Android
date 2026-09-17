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
        mockDriveRepository = mockk()
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
}
