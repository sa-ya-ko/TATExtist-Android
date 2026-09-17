package com.tatextist.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.ui.text.input.TextFieldValue

import androidx.lifecycle.viewModelScope
import com.tatextist.data.GoogleDriveRepository
import kotlinx.coroutines.launch

class EditorViewModel(
    private val driveRepository: GoogleDriveRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(documentTitle = newTitle)
    }

    fun updateContent(newContent: TextFieldValue) {
        _uiState.value = _uiState.value.copy(content = newContent)
    }

    fun saveToCloud() {
        if (driveRepository == null) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloudLoading = true, cloudError = null)

            val signInResult = driveRepository.signIn()
            if (signInResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    needsSignIn = true
                )
                return@launch
            }

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
                    needsSignIn = true
                )
                return@launch
            }

            val result = driveRepository.loadFromDrive(filename)
            
            if (result.isSuccess) {
                val content = result.getOrNull() ?: ""
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    content = TextFieldValue(text = content),
                    documentTitle = filename
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isCloudLoading = false,
                    cloudError = result.exceptionOrNull()?.message ?: "読み込みに失敗しました"
                )
            }
        }
    }

    fun onSignInLaunched() {
        _uiState.value = _uiState.value.copy(needsSignIn = false)
    }
}
