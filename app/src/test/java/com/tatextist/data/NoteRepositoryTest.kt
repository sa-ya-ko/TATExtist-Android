package com.tatextist.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class NoteRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tempDir: File
    private lateinit var repository: NoteRepository

    @BeforeEach
    fun setUp() {
        // テストごとに安全な一時ディレクトリと一意なファイルを作成
        tempDir = Files.createTempDirectory("note_test").toFile()
        val storageFile = File(tempDir, "notes_${System.nanoTime()}.json")
        repository = LocalJsonNoteRepository(storageFile, testDispatcher)
    }

    @Test
    fun `initial state returns empty list`() = runTest(testDispatcher) {
        // 初回起動時（ファイルが存在しない時）は空のリストが返ることを確認
        val notes = repository.getAllNotes().first()
        assertTrue(notes.isEmpty())
    }

    @Test
    fun `saveNote and getNoteById retrieves saved note`() = runTest(testDispatcher) {
        // メモを新規作成して保存
        val note = NoteItem(
            id = "note-1",
            title = "吾輩は猫である",
            content = "名前はまだ無い。",
            htmlContent = "<p>名前はまだ無い。</p>",
            isPinned = false,
            isArchived = false,
            updatedAt = 1000L
        )

        val saveResult = repository.saveNote(note)
        if (saveResult.isFailure) {
            saveResult.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(saveResult.isSuccess, "Save failed: ${saveResult.exceptionOrNull()?.message}")

        // ID指定で取得できることを確認
        val fetched = repository.getNoteById("note-1")
        assertNotNull(fetched)
        assertEquals("吾輩は猫である", fetched?.title)
        assertEquals("名前はまだ無い。", fetched?.content)
        assertEquals("<p>名前はまだ無い。</p>", fetched?.htmlContent)
    }

    @Test
    fun `getAllNotes sorts pinned notes first and by updatedAt descending`() = runTest(testDispatcher) {
        // 複数メモを異なるピン状態・更新日時で保存
        val note1 = NoteItem(id = "1", title = "通常古い", updatedAt = 100L, isPinned = false)
        val note2 = NoteItem(id = "2", title = "通常新しい", updatedAt = 300L, isPinned = false)
        val note3 = NoteItem(id = "3", title = "ピン留め古い", updatedAt = 200L, isPinned = true)
        val note4 = NoteItem(id = "4", title = "ピン留め新しい", updatedAt = 400L, isPinned = true)

        repository.saveNote(note1)
        repository.saveNote(note2)
        repository.saveNote(note3)
        repository.saveNote(note4)

        val list = repository.getAllNotes().first()

        // 期待される並び順: ピン新しい(4) -> ピン古い(3) -> 通常新しい(2) -> 通常古い(1)
        assertEquals(listOf("4", "3", "2", "1"), list.map { it.id })
    }

    @Test
    fun `togglePin inverts pinned state and persists`() = runTest(testDispatcher) {
        val note = NoteItem(id = "note-1", title = "ピン留めテスト", isPinned = false)
        repository.saveNote(note)

        // ピン留めをONにする
        repository.togglePin("note-1")
        val pinned = repository.getNoteById("note-1")
        assertTrue(pinned?.isPinned == true)

        // もう一度呼んでOFFにする
        repository.togglePin("note-1")
        val unpinned = repository.getNoteById("note-1")
        assertFalse(unpinned?.isPinned == true)
    }

    @Test
    fun `toggleArchive inverts archived state and persists`() = runTest(testDispatcher) {
        val note = NoteItem(id = "note-1", title = "アーカイブテスト", isArchived = false)
        repository.saveNote(note)

        // アーカイブをONにする
        repository.toggleArchive("note-1")
        val archived = repository.getNoteById("note-1")
        assertTrue(archived?.isArchived == true)

        // もう一度呼んでOFFにする（アーカイブ解除）
        repository.toggleArchive("note-1")
        val unarchived = repository.getNoteById("note-1")
        assertFalse(unarchived?.isArchived == true)
    }

    @Test
    fun `deleteNote removes note from repository`() = runTest(testDispatcher) {
        val note = NoteItem(id = "note-1", title = "削除テスト")
        repository.saveNote(note)

        val deleteResult = repository.deleteNote("note-1")
        assertTrue(deleteResult.isSuccess)

        val fetched = repository.getNoteById("note-1")
        assertNull(fetched)
    }
}
