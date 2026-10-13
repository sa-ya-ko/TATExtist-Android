package com.tatextist.ui

import androidx.compose.ui.text.input.TextFieldValue
import com.tatextist.data.DriveFileInfo
import com.tatextist.utils.RubyItem

enum class EditorFontFamily {
    SERIF,
    SANS_SERIF
}

data class FontApplyRequest(
    val fontFamily: EditorFontFamily? = null,
    val fontSize: Int? = null,
    val id: Long = System.currentTimeMillis()
)

enum class PaperSize {
    FIT,
    A4,
    B5
}

data class EditorMargins(
    val top: Int = 20,
    val bottom: Int = 20,
    val right: Int = 20,
    val left: Int = 20
)

data class EditorUiState(
    val documentTitle: String = "無題のドキュメント",
    val content: TextFieldValue = TextFieldValue(),
    val isDarkMode: Boolean = false,
    val fontSize: Int = 20,
    val fontFamily: EditorFontFamily = EditorFontFamily.SERIF,
    val selectedFontFamily: EditorFontFamily = EditorFontFamily.SERIF,
    val fontApplyRequest: FontApplyRequest? = null,
    val showFontSettingsBottomSheet: Boolean = false,
    val showRubyBottomSheet: Boolean = false,
    val rubyList: List<RubyItem> = emptyList(),
    val isRubyListActive: Boolean = false,
    val targetRubyIndex: Int? = null,
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
    val showFilePickerModal: Boolean = false,
    val indentWidth: Float = 0.0f,
    val paperSize: PaperSize = PaperSize.FIT,
    val margins: EditorMargins = EditorMargins(),
    val printRequestId: Long? = null,
    val localFileMessage: String? = null,
    val localFileError: String? = null
)


