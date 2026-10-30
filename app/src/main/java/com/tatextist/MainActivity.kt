package com.tatextist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.tatextist.data.GoogleDriveRepositoryImpl
import com.tatextist.data.LocalFileRepositoryImpl
import com.tatextist.ui.EditorScreen
import com.tatextist.ui.EditorViewModel
import com.tatextist.ui.theme.TATExtistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Driveレポジトリおよびローカルファイルレポジトリ、メモ保管庫を準備
            val driveRepository = remember { GoogleDriveRepositoryImpl(applicationContext) }
            val localFileRepository = remember { LocalFileRepositoryImpl(contentResolver) }
            val noteRepository = remember {
                val storageFile = java.io.File(filesDir, "notes.json")
                com.tatextist.data.LocalJsonNoteRepository(storageFile)
            }
            
            // エディタ画面の記憶係（ViewModel）を準備（レポジトリを注入）
            val viewModel: EditorViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return EditorViewModel(
                            driveRepository = driveRepository,
                            localFileRepository = localFileRepository,
                            noteRepository = noteRepository
                        ) as T
                    }
                }
            )
            // 記憶係が持っている状態を「常に監視」して取得（更新されたら自動で画面を描き直す）
            val uiState by viewModel.uiState.collectAsState()

            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            LaunchedEffect(Unit) {
                viewModel.toggleDarkMode(systemInDark)
            }

            // アプリ全体のテーマ（色やフォントの設定）を適用します
            TATExtistTheme(darkTheme = uiState.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {

                    // サインイン画面を呼び出すランチャー
                    val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            // 認証成功時にもう一度保存や読み込みをユーザーに促す等の処理が可能
                        }
                    }

                    // ViewModelが「サインインが必要」と言ってきたらランチャーを起動する
                    LaunchedEffect(uiState.needsSignIn) {
                        if (uiState.needsSignIn) {
                            signInLauncher.launch(driveRepository.getSignInIntent())
                            viewModel.onSignInLaunched()
                        }
                    }

                    // 画面モードに応じて「メモ一覧画面」または「エディタ画面」を表示
                    when (uiState.screenMode) {
                        com.tatextist.ui.ScreenMode.LIST -> {
                            com.tatextist.ui.NoteListScreen(
                                notes = uiState.notes,
                                onNoteClick = { noteId ->
                                    viewModel.openNote(noteId)
                                },
                                onCreateNewNote = {
                                    viewModel.createNewNote()
                                },
                                onTogglePin = { noteId ->
                                    viewModel.togglePin(noteId)
                                },
                                onToggleArchive = { noteId ->
                                    viewModel.toggleArchive(noteId)
                                },
                                onDeleteNote = { noteId ->
                                    viewModel.deleteNote(noteId)
                                }
                            )
                        }
                        com.tatextist.ui.ScreenMode.EDITOR -> {
                            EditorScreen(
                                uiState = uiState,
                                onTitleChange = { newTitle ->
                                    viewModel.updateTitle(newTitle)
                                },
                                onValueChange = { newValue, rawHtml ->
                                    viewModel.updateContent(newValue, rawHtml)
                                },
                                onSaveToCloud = {
                                    viewModel.saveToCloud()
                                },
                                onLoadFromCloud = {
                                    viewModel.fetchDriveFiles()
                                },
                                onSelectDriveFile = { file ->
                                    viewModel.loadDriveFile(file)
                                },
                                onCloseFilePickerModal = {
                                    viewModel.closeFilePickerModal()
                                },
                                onSignOutFromCloud = {
                                    viewModel.signOutFromCloud()
                                },
                                onOpenWordCountModal = {
                                    viewModel.openWordCountModal()
                                },
                                onCloseWordCountModal = {
                                    viewModel.closeWordCountModal()
                                },
                                onToggleShowWordCountWhileTyping = { enabled ->
                                    viewModel.toggleShowWordCountWhileTyping(enabled)
                                },
                                onOpenFontSettings = {
                                    viewModel.openFontSettingsBottomSheet()
                                },
                                onCloseFontSettings = {
                                    viewModel.closeFontSettingsBottomSheet()
                                },
                                onFontFamilyChange = { fontFamily ->
                                    viewModel.updateFontFamily(fontFamily)
                                },
                                onIncreaseFontSize = {
                                    viewModel.increaseFontSize()
                                },
                                onDecreaseFontSize = {
                                    viewModel.decreaseFontSize()
                                },
                                onConsumeFontApplyRequest = {
                                    viewModel.consumeFontApplyRequest()
                                },
                                onStyleDetected = { family, size ->
                                    viewModel.updateDetectedStyle(family, size)
                                },
                                onColorDetected = { hexColor ->
                                    viewModel.updateDetectedColor(hexColor)
                                },
                                onTextColorChange = { hexColor ->
                                    viewModel.updateTextColor(hexColor)
                                },
                                onResetTextColor = {
                                    viewModel.resetTextColor()
                                },
                                onToggleDarkMode = {
                                    viewModel.toggleDarkMode()
                                },
                                onRemoveRubyItem = { item ->
                                    viewModel.removeRubyItem(item)
                                },
                                onSelectRubyItem = { item ->
                                    val index = uiState.rubyList.indexOf(item)
                                    if (index >= 0) {
                                        viewModel.jumpToRuby(index)
                                    }
                                },
                                onRubyTabChanged = { isListTab ->
                                    viewModel.setRubyListActive(isListTab)
                                },
                                onRubyJumpConsumed = {
                                    viewModel.consumeRubyJump()
                                },
                                onIndentWidthChange = { width ->
                                    viewModel.updateIndentWidth(width)
                                },
                                onPaperSizeChange = { size ->
                                    viewModel.setPaperSize(size)
                                },
                                onMarginChange = { top, bottom, right, left ->
                                    viewModel.updateMargins(top = top, bottom = bottom, right = right, left = left)
                                },
                                onSaveToLocalFile = { uri, format ->
                                    viewModel.saveToLocalFile(uri, format)
                                },
                                onLoadFromLocalFile = { uri ->
                                    viewModel.loadFromLocalFile(uri)
                                },
                                onClearLocalFileMessage = {
                                    viewModel.clearLocalFileMessage()
                                },
                                onPrintRequest = {
                                    viewModel.requestPrint()
                                },
                                onPrintConsumed = {
                                    viewModel.consumePrintRequest()
                                },
                                onParagraphStyleChange = { style ->
                                    viewModel.applyParagraphStyle(style)
                                },
                                onParagraphStyleDetected = { tag ->
                                    viewModel.onParagraphStyleDetected(tag)
                                },
                                onParagraphStyleConsumed = {
                                    viewModel.onParagraphStyleConsumed()
                                },
                                onNavigateBack = {
                                    viewModel.navigateToList()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
