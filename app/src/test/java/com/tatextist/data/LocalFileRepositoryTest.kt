package com.tatextist.data

import android.content.ContentResolver
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LocalFileRepositoryTest {

    private val contentResolver: ContentResolver = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalFileRepository

    private val testUri: Uri = mockk()

    @BeforeEach
    fun setUp() {
        repository = LocalFileRepositoryImpl(contentResolver, testDispatcher)
    }

    @Test
    fun `saveText writes plain text to Uri with UTF-8 encoding`() = runTest(testDispatcher) {
        val testContent = "吾輩は猫である。名前はまだ無い。\n|親文字《ルビ》テスト"
        val outputStream = ByteArrayOutputStream()

        every { contentResolver.openOutputStream(testUri, "wt") } returns outputStream

        val result = repository.saveText(testUri, testContent)

        assertTrue(result.isSuccess)
        assertEquals(testContent, outputStream.toString("UTF-8"))
    }

    @Test
    fun `saveText returns failure when openOutputStream throws IOException`() = runTest(testDispatcher) {
        every { contentResolver.openOutputStream(testUri, "wt") } throws IOException("Permission denied")

        val result = repository.saveText(testUri, "content")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `readText reads UTF-8 plain text from Uri`() = runTest(testDispatcher) {
        val expectedContent = "国境の長いトンネルを抜けると雪国であった。\n　・箇条書きテスト"
        val inputStream = ByteArrayInputStream(expectedContent.toByteArray(Charsets.UTF_8))

        every { contentResolver.openInputStream(testUri) } returns inputStream

        val result = repository.readText(testUri)

        assertTrue(result.isSuccess)
        assertEquals(expectedContent, result.getOrNull())
    }

    @Test
    fun `readText returns failure when openInputStream returns null`() = runTest(testDispatcher) {
        every { contentResolver.openInputStream(testUri) } returns null

        val result = repository.readText(testUri)

        assertTrue(result.isFailure)
    }
}
