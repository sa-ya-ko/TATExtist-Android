package com.tatextist.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.ui.text.input.TextFieldValue
import com.tatextist.utils.HtmlTextConverter

import androidx.lifecycle.viewModelScope
import com.tatextist.data.GoogleDriveRepository
import kotlinx.coroutines.launch

class EditorViewModel(
    private val driveRepository: GoogleDriveRepository? = null
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

        _uiState.value = _uiState.value.copy(
            content = newContent,
            characterCount = charCount,
            characterCountWithoutSpaces = charCountWithoutSpaces,
            lineCount = lineCount
        )
    }

    fun toggleDarkMode(enable: Boolean? = null) {
        val newMode = enable ?: !_uiState.value.isDarkMode
        _uiState.value = _uiState.value.copy(isDarkMode = newMode)
    }

    fun updateFontFamily(fontFamily: EditorFontFamily) {
        _uiState.value = _uiState.value.copy(
            fontFamily = fontFamily,
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
}
