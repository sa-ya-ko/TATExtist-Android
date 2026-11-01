package com.tatextist.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.ui.text.input.TextFieldValue
import com.tatextist.utils.HtmlTextConverter
import com.tatextist.utils.RubyItem
import com.tatextist.utils.RubyManager

import androidx.lifecycle.viewModelScope
import com.tatextist.data.GoogleDriveRepository
import com.tatextist.data.LocalFileRepository
import com.tatextist.data.NoteItem
import com.tatextist.data.NoteRepository
import android.net.Uri
import kotlinx.coroutines.launch
import java.util.UUID

class EditorViewModel(
    private val driveRepository: GoogleDriveRepository? = null,
    private val localFileRepository: LocalFileRepository? = null,
    private val noteRepository: NoteRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        val initialSignedIn = driveRepository?.isSignedIn() ?: false
        _uiState.value = _uiState.value.copy(isDriveSignedIn = initialSignedIn)

        // メモ一覧を監視してUiStateに反映
        noteRepository?.let { repo ->
            viewModelScope.launch {
                repo.getAllNotes().collect { list ->
                    _uiState.value = _uiState.value.copy(notes = list)
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(documentTitle = newTitle)
    }

    fun updateContent(newContent: TextFieldValue, rawHtml: String? = null) {
        val plainText = HtmlTextConverter.extractPlainText(newContent.text)
        val charCount = plainText.length
        val charCountWithoutSpaces = plainText.replace("\\s+".toRegex(), "").length
        val lineCount = if (plainText.isEmpty()) 1 else plainText.lines().size
        val rubies = RubyManager.extractRubyList(newContent.text)

        _uiState.value = _uiState.value.copy(
            content = newContent,
            currentHtmlContent = rawHtml ?: _uiState.value.currentHtmlContent,
            characterCount = charCount,
            characterCountWithoutSpaces = charCountWithoutSpaces,
            lineCount = lineCount,
            rubyList = rubies
        )
    }

    fun removeRubyItem(item: RubyItem) {
        val currentText = _uiState.value.content.text
        val updatedText = RubyManager.removeRuby(currentText, item)
        updateContent(_uiState.value.content.copy(text = updatedText))
    }

    fun openRubyBottomSheet() {
        _uiState.value = _uiState.value.copy(showRubyBottomSheet = true)
    }

    fun closeRubyBottomSheet() {
        _uiState.value = _uiState.value.copy(
            showRubyBottomSheet = false,
            isRubyListActive = false
        )
    }

    fun setRubyListActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isRubyListActive = active)
    }

    fun jumpToRuby(index: Int) {
        _uiState.value = _uiState.value.copy(targetRubyIndex = index)
    }

    fun consumeRubyJump() {
        _uiState.value = _uiState.value.copy(targetRubyIndex = null)
    }

    fun toggleDarkMode(enable: Boolean? = null) {
        val newMode = enable ?: !_uiState.value.isDarkMode
        _uiState.value = _uiState.value.copy(isDarkMode = newMode)
    }

    fun updateFontFamily(fontFamily: EditorFontFamily) {
        _uiState.value = _uiState.value.copy(
            fontFamily = fontFamily,
            selectedFontFamily = fontFamily,
            fontApplyRequest = FontApplyRequest(fontFamily = fontFamily)
        )
    }

    fun updateFontSize(newSize: Int) {
        val clampedSize = newSize.coerceIn(12, 36)
        _uiState.value = _uiState.value.copy(
            fontSize = clampedSize,
            fontApplyRequest = FontApplyRequest(fontSize = clampedSize)
        )
    }

    fun increaseFontSize() {
        val currentSize = _uiState.value.fontSize
        if (currentSize < 36) {
            updateFontSize(currentSize + 1)
        }
    }

    fun decreaseFontSize() {
        val currentSize = _uiState.value.fontSize
        if (currentSize > 12) {
            updateFontSize(currentSize - 1)
        }
    }

    fun updateDetectedStyle(fontFamily: EditorFontFamily, fontSize: Int) {
        _uiState.value = _uiState.value.copy(
            fontFamily = fontFamily,
            selectedFontFamily = fontFamily,
            fontSize = fontSize.coerceIn(12, 36)
        )
    }

    fun updateTextColor(hexColor: String) {
        val updatedCustom = if (hexColor !in _uiState.value.customColors) {
            (_uiState.value.customColors + hexColor).takeLast(8)
        } else {
            _uiState.value.customColors
        }
        _uiState.value = _uiState.value.copy(
            selectedTextColor = hexColor,
            customColors = updatedCustom,
            fontApplyRequest = FontApplyRequest(textColor = hexColor)
        )
    }

    fun resetTextColor() {
        _uiState.value = _uiState.value.copy(
            selectedTextColor = null,
            fontApplyRequest = FontApplyRequest(textColor = "")
        )
    }

    fun updateDetectedColor(hexColor: String?) {
        _uiState.value = _uiState.value.copy(
            selectedTextColor = hexColor
        )
    }

    fun consumeFontApplyRequest() {
        _uiState.value = _uiState.value.copy(fontApplyRequest = null)
    }

    fun openFontSettingsBottomSheet() {
        _uiState.value = _uiState.value.copy(showFontSettingsBottomSheet = true)
    }

    fun closeFontSettingsBottomSheet() {
        _uiState.value = _uiState.value.copy(showFontSettingsBottomSheet = false)
    }

    fun saveToCloud() {
        if (driveRepository == null) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloudLoading = true, cloudError = null)

            val signInResult = driveRepository.signIn()
            if (signInResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    needsSignIn = true,
                    isDriveSignedIn = false
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isDriveSignedIn = true)
            val result = driveRepository.saveToDrive(uiState.value.content.text, uiState.value.documentTitle)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isCloudLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    cloudError = result.exceptionOrNull()?.message ?: "保存に失敗しました"
                )
            }
        }
    }

    fun loadFromCloud(filename: String) {
        if (driveRepository == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloudLoading = true, cloudError = null)

            val signInResult = driveRepository.signIn()
            if (signInResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    needsSignIn = true,
                    isDriveSignedIn = false
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isDriveSignedIn = true)
            val result = driveRepository.loadFromDrive(filename)
            
            if (result.isSuccess) {
                val content = result.getOrNull() ?: ""
                val plainText = HtmlTextConverter.extractPlainText(content)
                val charCount = plainText.length
                val charCountWithoutSpaces = plainText.replace("\\s+".toRegex(), "").length
                val lineCount = if (plainText.isEmpty()) 1 else plainText.lines().size

                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    content = TextFieldValue(text = content),
                    documentTitle = filename,
                    characterCount = charCount,
                    characterCountWithoutSpaces = charCountWithoutSpaces,
                    lineCount = lineCount
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    cloudError = result.exceptionOrNull()?.message ?: "読み込みに失敗しました"
                )
            }
        }
    }

    fun signOutFromCloud() {
        if (driveRepository == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloudLoading = true, cloudError = null)
            val result = driveRepository.signOut()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    isDriveSignedIn = false,
                    cloudError = "Drive連携を解除しました"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    cloudError = result.exceptionOrNull()?.message ?: "連携解除に失敗しました"
                )
            }
        }
    }

    fun onSignInLaunched() {
        _uiState.value = _uiState.value.copy(needsSignIn = false)
    }

    fun openWordCountModal() {
        _uiState.value = _uiState.value.copy(showWordCountModal = true)
    }

    fun closeWordCountModal() {
        _uiState.value = _uiState.value.copy(showWordCountModal = false)
    }

    fun toggleShowWordCountWhileTyping(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showWordCountWhileTyping = enabled)
    }

    fun fetchDriveFiles() {
        if (driveRepository == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloudLoading = true, cloudError = null)

            val signInResult = driveRepository.signIn()
            if (signInResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    needsSignIn = true,
                    isDriveSignedIn = false
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isDriveSignedIn = true)
            val result = driveRepository.listDriveFiles()

            if (result.isSuccess) {
                val files = result.getOrNull() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    cloudFiles = files,
                    showFilePickerModal = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    cloudError = result.exceptionOrNull()?.message ?: "ファイル一覧の取得に失敗しました"
                )
            }
        }
    }

    fun loadDriveFile(file: com.tatextist.data.DriveFileInfo) {
        if (driveRepository == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloudLoading = true, cloudError = null)

            val signInResult = driveRepository.signIn()
            if (signInResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    needsSignIn = true,
                    isDriveSignedIn = false
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isDriveSignedIn = true)
            val result = driveRepository.loadFromDriveById(file.id)

            if (result.isSuccess) {
                val content = result.getOrNull() ?: ""
                val plainText = HtmlTextConverter.extractPlainText(content)
                val charCount = plainText.length
                val charCountWithoutSpaces = plainText.replace("\\s+".toRegex(), "").length
                val lineCount = if (plainText.isEmpty()) 1 else plainText.lines().size

                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    content = TextFieldValue(text = content),
                    documentTitle = file.name,
                    characterCount = charCount,
                    characterCountWithoutSpaces = charCountWithoutSpaces,
                    lineCount = lineCount,
                    showFilePickerModal = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    cloudError = result.exceptionOrNull()?.message ?: "読み込みに失敗しました"
                )
            }
        }
    }

    fun closeFilePickerModal() {
        _uiState.value = _uiState.value.copy(showFilePickerModal = false)
    }

    fun updateIndentWidth(width: Float) {
        if (width in 0.0f..4.0f) {
            _uiState.value = _uiState.value.copy(indentWidth = width)
        }
    }

    /**
     * 現在のエディタ内容を指定されたフォーマット（TXT / HTML / MARKDOWN）に変換し、ローカルファイルURIに保存する
     */
    fun saveToLocalFile(uri: Uri, format: com.tatextist.utils.ExportFormat = com.tatextist.utils.ExportFormat.TXT) {
        val repo = localFileRepository ?: return
        viewModelScope.launch {
            val state = _uiState.value
            val rawSource = if (state.currentHtmlContent.isNotBlank()) state.currentHtmlContent else state.content.text

            val textToSave = when (format) {
                com.tatextist.utils.ExportFormat.TXT -> {
                    val aozoraText = HtmlTextConverter.convertFromHtml(rawSource)
                    val metaHeader = "［＃レイアウト設定：title=${state.documentTitle},paper=${state.paperSize.name},top=${state.margins.top},bottom=${state.margins.bottom},right=${state.margins.right},left=${state.margins.left}］\n"
                    metaHeader + aozoraText
                }
                com.tatextist.utils.ExportFormat.HTML -> {
                    val fontFam = if (state.fontFamily == EditorFontFamily.SANS_SERIF) "sans-serif" else "serif"
                    com.tatextist.utils.ExportConverter.exportToStandaloneHtml(
                        title = state.documentTitle,
                        bodyHtml = rawSource,
                        fontFamily = fontFam,
                        fontSize = state.fontSize
                    )
                }
                com.tatextist.utils.ExportFormat.MARKDOWN -> {
                    com.tatextist.utils.ExportConverter.convertToMarkdown(rawSource)
                }
            }

            val result = repo.saveText(uri, textToSave)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    localFileMessage = "${format.label} を保存しました",
                    localFileError = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    localFileError = "ファイルの保存に失敗しました: ${result.exceptionOrNull()?.message}",
                    localFileMessage = null
                )
            }
        }
    }

    /**
     * ローカルファイルURIから青空文庫形式テキストを読み込み、HTMLに変換してエディタに反映する
     */
    fun loadFromLocalFile(uri: Uri) {
        val repo = localFileRepository ?: return
        viewModelScope.launch {
            val result = repo.readText(uri)
            if (result.isSuccess) {
                val rawText = result.getOrNull() ?: ""
                var aozoraText = rawText
                var loadedTitle: String? = null
                var loadedPaperSize: PaperSize? = null
                var loadedMargins: EditorMargins? = null

                // 先頭の［＃レイアウト設定：...］メタデータを検出・抽出
                val metaRegex = Regex("""^［＃レイアウト設定：([^］]+)］\r?\n?""")
                val match = metaRegex.find(rawText)
                if (match != null) {
                    val metaParams = match.groupValues[1].split(",")
                    var top = _uiState.value.margins.top
                    var bottom = _uiState.value.margins.bottom
                    var right = _uiState.value.margins.right
                    var left = _uiState.value.margins.left
                    for (param in metaParams) {
                        val kv = param.split("=")
                        if (kv.size == 2) {
                            val key = kv[0].trim()
                            val value = kv[1].trim()
                            when (key) {
                                "title" -> {
                                    if (value.isNotBlank()) loadedTitle = value
                                }
                                "paper" -> {
                                    loadedPaperSize = when (value) {
                                        "A4" -> PaperSize.A4
                                        "B5" -> PaperSize.B5
                                        else -> PaperSize.FIT
                                    }
                                }
                                "top" -> value.toIntOrNull()?.let { top = it }
                                "bottom" -> value.toIntOrNull()?.let { bottom = it }
                                "right" -> value.toIntOrNull()?.let { right = it }
                                "left" -> value.toIntOrNull()?.let { left = it }
                            }
                        }
                    }
                    loadedMargins = EditorMargins(top = top, bottom = bottom, right = right, left = left)
                    aozoraText = rawText.substring(match.range.last + 1)
                }

                val htmlContent = HtmlTextConverter.convertToHtml(aozoraText)
                updateContent(TextFieldValue(text = htmlContent), rawHtml = htmlContent)
                _uiState.value = _uiState.value.copy(
                    documentTitle = loadedTitle ?: _uiState.value.documentTitle,
                    paperSize = loadedPaperSize ?: _uiState.value.paperSize,
                    margins = loadedMargins ?: _uiState.value.margins,
                    localFileMessage = "端末からファイルを読み込みました",
                    localFileError = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    localFileError = "ファイルの読み込みに失敗しました: ${result.exceptionOrNull()?.message}",
                    localFileMessage = null
                )
            }
        }
    }

    /**
     * ローカルファイル操作通知メッセージを消去する
     */
    fun clearLocalFileMessage() {
        _uiState.value = _uiState.value.copy(
            localFileMessage = null,
            localFileError = null
        )
    }

    /**
     * 用紙サイズ（FIT, A4, B5）を設定する
     */
    fun setPaperSize(paperSize: PaperSize) {
        _uiState.value = _uiState.value.copy(paperSize = paperSize)
    }

    /**
     * 余白（上、下、右、左）を設定する
     */
    fun updateMargins(top: Int? = null, bottom: Int? = null, right: Int? = null, left: Int? = null) {
        val cur = _uiState.value.margins
        _uiState.value = _uiState.value.copy(
            margins = cur.copy(
                top = top ?: cur.top,
                bottom = bottom ?: cur.bottom,
                right = right ?: cur.right,
                left = left ?: cur.left
            )
        )
    }

    /**
     * PDFエクスポート・印刷リクエストを発行する
     */
    fun requestPrint() {
        if (_uiState.value.content.text.isBlank()) {
            _uiState.value = _uiState.value.copy(
                localFileError = "印刷するテキストがありません",
                localFileMessage = null
            )
            return
        }
        _uiState.value = _uiState.value.copy(printRequestId = System.currentTimeMillis())
    }

    /**
     * PDFエクスポート・印刷リクエストを消費する
     */
    fun consumePrintRequest() {
        _uiState.value = _uiState.value.copy(printRequestId = null)
    }

    /**
     * 段落スタイル（タイトル、見出し1〜3、標準テキスト）を適用する
     * すでに同じスタイルが選択されている場合は NORMAL にリセットする
     */
    fun applyParagraphStyle(style: ParagraphStyle) {
        val targetStyle = if (_uiState.value.currentParagraphStyle == style) {
            ParagraphStyle.NORMAL
        } else {
            style
        }
        _uiState.value = _uiState.value.copy(
            currentParagraphStyle = targetStyle,
            paragraphStyleRequest = targetStyle
        )
    }

    /**
     * WebViewへの段落スタイル適用リクエストを消費する
     */
    fun onParagraphStyleConsumed() {
        _uiState.value = _uiState.value.copy(paragraphStyleRequest = null)
    }

    /**
     * WebViewから検出された段落スタイル（h1〜h4, p 等）をUiStateに同期する
     */
    fun onParagraphStyleDetected(tag: String?) {
        val detected = ParagraphStyle.fromTag(tag)
        _uiState.value = _uiState.value.copy(currentParagraphStyle = detected)
    }

    /**
     * 新規メモを作成してエディタ画面に遷移する
     */
    fun createNewNote() {
        val newId = UUID.randomUUID().toString()
        _uiState.value = _uiState.value.copy(
            screenMode = ScreenMode.EDITOR,
            currentNoteId = newId,
            documentTitle = "",
            content = TextFieldValue(""),
            currentHtmlContent = "",
            characterCount = 0,
            characterCountWithoutSpaces = 0,
            lineCount = 1,
            rubyList = emptyList(),
            currentParagraphStyle = ParagraphStyle.NORMAL,
            paragraphStyleRequest = null,
            fontFamily = EditorFontFamily.SERIF,
            selectedFontFamily = EditorFontFamily.SERIF,
            fontSize = 20,
            selectedTextColor = null,
            fontApplyRequest = null,
            indentWidth = 0.0f
        )
    }

    /**
     * 既存のメモを開いてエディタ画面に遷移する
     */
    fun openNote(noteId: String) {
        viewModelScope.launch {
            val note = noteRepository?.getNoteById(noteId)
            val title = note?.title ?: ""
            val contentText = note?.content ?: ""
            val htmlText = note?.htmlContent ?: ""
            val rubies = RubyManager.extractRubyList(contentText)
            val plainText = HtmlTextConverter.extractPlainText(contentText)
            val noteFontFamily = if (note?.fontFamily == "SANS_SERIF") EditorFontFamily.SANS_SERIF else EditorFontFamily.SERIF
            val noteIndentWidth = note?.indentWidth ?: 0.0f

            _uiState.value = _uiState.value.copy(
                screenMode = ScreenMode.EDITOR,
                currentNoteId = noteId,
                documentTitle = title,
                content = TextFieldValue(contentText),
                currentHtmlContent = htmlText,
                characterCount = plainText.length,
                characterCountWithoutSpaces = plainText.replace("\\s+".toRegex(), "").length,
                lineCount = if (plainText.isEmpty()) 1 else plainText.lines().size,
                rubyList = rubies,
                currentParagraphStyle = ParagraphStyle.NORMAL,
                paragraphStyleRequest = null,
                fontFamily = noteFontFamily,
                selectedFontFamily = noteFontFamily,
                fontSize = 20,
                selectedTextColor = null,
                fontApplyRequest = null,
                indentWidth = noteIndentWidth
            )
        }
    }

    /**
     * エディタからメモ一覧画面に戻る（編集中メモの自動保存を実施）
     */
    fun navigateToList() {
        val currentId = _uiState.value.currentNoteId
        val currentTitle = _uiState.value.documentTitle
        val currentContent = _uiState.value.content.text
        val currentHtml = _uiState.value.currentHtmlContent
        val currentFontFamilyStr = if (_uiState.value.fontFamily == EditorFontFamily.SANS_SERIF) "SANS_SERIF" else "SERIF"

        if (currentId != null && noteRepository != null) {
            viewModelScope.launch {
                val existingNote = noteRepository.getNoteById(currentId)
                val noteToSave = NoteItem(
                    id = currentId,
                    title = currentTitle,
                    content = currentContent,
                    htmlContent = if (currentHtml.isNotBlank()) currentHtml else (existingNote?.htmlContent ?: ""),
                    isPinned = existingNote?.isPinned ?: false,
                    isArchived = existingNote?.isArchived ?: false,
                    fontFamily = currentFontFamilyStr,
                    indentWidth = _uiState.value.indentWidth,
                    updatedAt = System.currentTimeMillis()
                )
                noteRepository.saveNote(noteToSave)
            }
        }

        _uiState.value = _uiState.value.copy(
            screenMode = ScreenMode.LIST,
            currentNoteId = null
        )
    }

    /**
     * メモのピン留め状態を反転する
     */
    fun togglePin(noteId: String) {
        viewModelScope.launch {
            noteRepository?.togglePin(noteId)
        }
    }

    /**
     * メモのアーカイブ状態を反転する
     */
    fun toggleArchive(noteId: String) {
        viewModelScope.launch {
            noteRepository?.toggleArchive(noteId)
        }
    }

    /**
     * メモを削除する
     */
    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository?.deleteNote(noteId)
        }
    }

    /**
     * メモ一覧の検索キーワードを更新する
     */
    fun updateNoteSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(noteSearchQuery = query)
    }
}


