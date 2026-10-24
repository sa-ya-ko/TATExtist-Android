package com.tatextist.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * メモの保存・取得・更新・削除を管理する保管庫インターフェース
 */
interface NoteRepository {
    /**
     * 保存されているすべてのメモ一覧を監視するフローを取得する
     * （ピン留め優先、更新日時が新しい順に自動ソート）
     */
    fun getAllNotes(): Flow<List<NoteItem>>

    /**
     * 指定されたIDのメモを1件取得する
     */
    suspend fun getNoteById(id: String): NoteItem?

    /**
     * メモを保存または更新する
     */
    suspend fun saveNote(note: NoteItem): Result<Unit>

    /**
     * 指定したメモのピン留め状態を切り替える
     */
    suspend fun togglePin(id: String): Result<Unit>

    /**
     * 指定したメモのアーカイブ状態を切り替える
     */
    suspend fun toggleArchive(id: String): Result<Unit>

    /**
     * 指定したメモを削除する
     */
    suspend fun deleteNote(id: String): Result<Unit>
}

/**
 * JSONファイルを用いて端末ローカル領域にメモを保存・管理する実装クラス
 */
class LocalJsonNoteRepository(
    private val storageFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    private val mutex = Mutex()
    private val _notesFlow = MutableStateFlow<List<NoteItem>>(emptyList())

    init {
        // 初期読み込み
        loadFromDiskSync()
    }

    override fun getAllNotes(): Flow<List<NoteItem>> = _notesFlow.asStateFlow()

    override suspend fun getNoteById(id: String): NoteItem? = withContext(ioDispatcher) {
        mutex.withLock {
            _notesFlow.value.find { it.id == id }
        }
    }

    override suspend fun saveNote(note: NoteItem): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            runCatching {
                val currentList = _notesFlow.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.id == note.id }

                if (existingIndex >= 0) {
                    currentList[existingIndex] = note
                } else {
                    currentList.add(note)
                }

                updateAndPersist(currentList)
            }
        }
    }

    override suspend fun togglePin(id: String): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            runCatching {
                val currentList = _notesFlow.value.toMutableList()
                val targetIndex = currentList.indexOfFirst { it.id == id }
                if (targetIndex >= 0) {
                    val target = currentList[targetIndex]
                    currentList[targetIndex] = target.copy(isPinned = !target.isPinned)
                    updateAndPersist(currentList)
                }
            }
        }
    }

    override suspend fun toggleArchive(id: String): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            runCatching {
                val currentList = _notesFlow.value.toMutableList()
                val targetIndex = currentList.indexOfFirst { it.id == id }
                if (targetIndex >= 0) {
                    val target = currentList[targetIndex]
                    currentList[targetIndex] = target.copy(isArchived = !target.isArchived)
                    updateAndPersist(currentList)
                }
            }
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            runCatching {
                val currentList = _notesFlow.value.toMutableList()
                currentList.removeAll { it.id == id }
                updateAndPersist(currentList)
            }
        }
    }

    /**
     * リストをソートしてStateFlowを更新し、JSONファイルに書き出す
     */
    private fun updateAndPersist(list: List<NoteItem>) {
        val sortedList = sortNotes(list)
        _notesFlow.value = sortedList
        persistToFile(sortedList)
    }

    /**
     * ソート順:
     * 1. ピン留め（isPinned == true）が優先
     * 2. 更新日時（updatedAt）が新しい順
     */
    private fun sortNotes(list: List<NoteItem>): List<NoteItem> {
        return list.sortedWith(
            compareByDescending<NoteItem> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }

    /**
     * メモリ上のリストをJSONファイルに書き出す
     */
    private fun persistToFile(list: List<NoteItem>) {
        val jsonArray = JSONArray()
        list.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("htmlContent", note.htmlContent)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("updatedAt", note.updatedAt)
            }
            jsonArray.put(obj)
        }
        storageFile.parentFile?.mkdirs()
        storageFile.writeText(jsonArray.toString(), Charsets.UTF_8)
    }

    /**
     * ファイルからJSONを読み込んでメモリのリストを初期化する
     */
    private fun loadFromDiskSync() {
        if (!storageFile.exists()) {
            _notesFlow.value = emptyList()
            return
        }
        runCatching {
            val content = storageFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<NoteItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    NoteItem(
                        id = obj.optString("id"),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        htmlContent = obj.optString("htmlContent", ""),
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        updatedAt = obj.optLong("updatedAt", 0L)
                    )
                )
            }
            _notesFlow.value = sortNotes(list)
        }.onFailure {
            _notesFlow.value = emptyList()
        }
    }
}
