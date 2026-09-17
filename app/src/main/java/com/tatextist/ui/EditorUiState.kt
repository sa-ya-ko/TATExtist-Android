package com.tatextist.ui

import androidx.compose.ui.text.input.TextFieldValue

data class EditorUiState(
    val documentTitle: String = "無題のドキュメント",
    val content: TextFieldValue = TextFieldValue(),
    val isDarkMode: Boolean = false,
    val fontSize: Int = 12,
    val isCloudLoading: Boolean = false,
    val cloudError: String? = null
)
