package com.tatextist.ui

import androidx.compose.ui.text.input.TextFieldValue
import com.tatextist.data.DriveFileInfo

data class EditorUiState(
    val documentTitle: String = "無題のドキュメント",
    val content: TextFieldValue = TextFieldValue(),
    val isDarkMode: Boolean = false,
    val fontSize: Int = 12,
    val isCloudLoading: Boolean = false,
    val cloudError: String? = null,
    val needsSignIn: Boolean = false,
    val characterCount: Int = 0,
    val characterCountWithoutSpaces: Int = 0,
    val lineCount: Int = 1,
    val showWordCountModal: Boolean = false,
    val showWordCountWhileTyping: Boolean = false,
    val isDriveSignedIn: Boolean = false,
    val cloudFiles: List<DriveFileInfo> = emptyList(),
    val showFilePickerModal: Boolean = false
)
