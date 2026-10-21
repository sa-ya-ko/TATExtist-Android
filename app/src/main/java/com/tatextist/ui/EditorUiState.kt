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
    val textColor: String? = null,
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
    val selectedTextColor: String? = null,
    val customColors: List<String> = emptyList(),
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
    val localFileError: String? = null,
    val currentParagraphStyle: ParagraphStyle = ParagraphStyle.NORMAL,
    val paragraphStyleRequest: ParagraphStyle? = null
)

enum class ParagraphStyle(val label: String, val tag: String) {
    NORMAL("標準テキスト", "p"),
    TITLE("タイトル", "h1"),
    HEADING_1("見出し1", "h2"),
    HEADING_2("見出し2", "h3"),
    HEADING_3("見出し3", "h4");

    companion object {
        fun fromTag(tag: String?): ParagraphStyle {
            return when (tag?.lowercase()) {
                "h1" -> TITLE
                "h2" -> HEADING_1
                "h3" -> HEADING_2
                "h4" -> HEADING_3
                else -> NORMAL
            }
        }
    }
}


