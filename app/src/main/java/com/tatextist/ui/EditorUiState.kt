package com.tatextist.ui

data class EditorUiState(
    val documentTitle: String = "無題のドキュメント",
    val content: String = "",
    val isDarkMode: Boolean = false,
    val fontSize: Int = 12
)
