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
import com.tatextist.ui.EditorScreen
import com.tatextist.ui.EditorViewModel
import com.tatextist.ui.theme.TATExtistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // アプリ全体のテーマ（色やフォントの設定）を適用します
            TATExtistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // エディタ画面の記憶係（ViewModel）を準備
                    val viewModel: EditorViewModel = viewModel()
                    // 記憶係が持っている状態を「常に監視」して取得（更新されたら自動で画面を描き直す）
                    val uiState by viewModel.uiState.collectAsState()

                    // 私たちが作ったエディタ画面を表示！
                    EditorScreen(
                        uiState = uiState,
                        onValueChange = { newValue ->
                            viewModel.updateContent(newValue)
                        }
                    )
                }
            }
        }
    }
}
