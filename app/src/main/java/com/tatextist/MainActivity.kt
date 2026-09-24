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
import com.tatextist.ui.EditorScreen
import com.tatextist.ui.EditorViewModel
import com.tatextist.ui.theme.TATExtistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // DriveレポジトリをActivityレベルで保持
            val driveRepository = remember { GoogleDriveRepositoryImpl(applicationContext) }
            
            // エディタ画面の記憶係（ViewModel）を準備（Driveレポジトリを注入）
            val viewModel: EditorViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return EditorViewModel(driveRepository) as T
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

                    // 私たちが作ったエディタ画面を表示！
                    EditorScreen(
                        uiState = uiState,
                        onTitleChange = { newTitle ->
                            viewModel.updateTitle(newTitle)
                        },
                        onValueChange = { newValue ->
                            viewModel.updateContent(newValue)
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
                        onToggleDarkMode = {
                            viewModel.toggleDarkMode()
                        }
                    )
                }
            }
        }
    }
}
