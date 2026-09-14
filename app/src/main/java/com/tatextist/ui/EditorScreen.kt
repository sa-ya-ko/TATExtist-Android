package com.tatextist.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    onTextChange: (String) -> Unit = {}
) {
    // 画面全体の骨組み（Scaffold）を作ります
    Scaffold(
        // 画面の上のバー（タイトルを表示）
        topBar = {
            TopAppBar(
                title = { Text(uiState.documentTitle) }
            )
        },
        // 画面の下のバー（ルビ付与ボタンなどを配置）
        bottomBar = {
            BottomAppBar {
                TextButton(onClick = { /* ボタンが押された時の動作は後で作ります */ }) {
                    Text("ルビ付与")
                }
            }
        }
    ) { innerPadding ->
        // 画面の真ん中（本文を書く場所）に縦書きエディタを配置
        VerticalEditor(
            text = uiState.content,
            onTextChange = onTextChange,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
