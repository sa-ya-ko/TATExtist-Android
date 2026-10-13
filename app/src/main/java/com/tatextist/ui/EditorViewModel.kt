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
import android.net.Uri
import kotlinx.coroutines.launch

class EditorViewModel(
    private val driveRepository: GoogleDriveRepository? = null,
    private val localFileRepository: LocalFileRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        val initialSignedIn = driveRepository?.isSignedIn() ?: false
        _uiState.value = _uiState.value.copy(isDriveSignedIn = initialSignedIn)
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(documentTitle = newTitle)
    }

    fun updateContent(newContent: TextFieldValue) {
        val plainText = HtmlTextConverter.extractPlainText(newContent.text)
        val charCount = plainText.length
        val charCountWithoutSpaces = plainText.replace("\\s+".toRegex(), "").length
        val lineCount = if (plainText.isEmpty()) 1 else plainText.lines().size
        val rubies = RubyManager.extractRubyList(newContent.text)

        _uiState.value = _uiState.value.copy(
            content = newContent,
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
     * 現在のエディタ内容を青空文庫形式テキストに変換し、ローカルファイルURIに保存する
     */
    fun saveToLocalFile(uri: Uri) {
        val repo = localFileRepository ?: return
        viewModelScope.launch {
            val aozoraText = HtmlTextConverter.convertFromHtml(_uiState.value.content.text)
            val state = _uiState.value
            val metaHeader = "［＃レイアウト設定：paper=${state.paperSize.name},top=${state.margins.top},bottom=${state.margins.bottom},right=${state.margins.right},left=${state.margins.left}］\n"
            val textToSave = metaHeader + aozoraText
            val result = repo.saveText(uri, textToSave)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    localFileMessage = "端末に保存しました",
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
                updateContent(TextFieldValue(text = htmlContent))
                _uiState.value = _uiState.value.copy(
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
}


