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

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private lateinit var viewModel: EditorViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = EditorViewModel()
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
    fun `when content is updated, state should reflect the new content`() = runTest {
        viewModel.uiState.test {
            // Initial state
            assertEquals("", awaitItem().content)

            // Action
            viewModel.updateContent("こんにちは、世界")

            // State changes (this will fail until we implement updateContent)
            assertEquals("こんにちは、世界", awaitItem().content)
        }
    }
}
