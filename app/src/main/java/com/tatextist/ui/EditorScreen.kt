package com.tatextist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    onValueChange: (TextFieldValue) -> Unit = {}
) {
    // 1. 記憶係（設定パネルが開いているかどうかを覚えておくメモ）
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // 2. ルビの「ふりがな」を入力するための記憶係
    var rubyReading by remember { mutableStateOf("") }

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
                // 2. ボタンを押したら、メモを「開く（true）」に書き換える
                TextButton(onClick = { showSettingsSheet = true }) {
                    Text("ルビ付与")
                }
            }
        }
    ) { innerPadding ->
        // 画面の真ん中（本文を書く場所）に縦書きエディタを配置
        VerticalEditor(
            value = uiState.content,
            onValueChange = onValueChange,
            modifier = Modifier.padding(innerPadding)
        )
    }

    // 3. ボトムシート（設定パネル）
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showSettingsSheet = false 
                rubyReading = "" // 閉じる時は入力欄を空にする
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("ルビを入力")
                
                OutlinedTextField(
                    value = rubyReading,
                    onValueChange = { rubyReading = it },
                    placeholder = { Text("例: ふりがな") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { 
                        showSettingsSheet = false
                        rubyReading = ""
                    }) {
                        Text("キャンセル")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = {
                        // ルビ挿入ロジック
                        val currentText = uiState.content.text
                        val selection = uiState.content.selection
                        
                        // 選択範囲がある場合のみルビを振る
                        if (!selection.collapsed) {
                            val start = selection.min
                            val end = selection.max
                            val baseText = currentText.substring(start, end)
                            
                            // |漢字《かんじ》 の形式を作成
                            val replacement = "|$baseText《$rubyReading》"
                            
                            // 文字列を結合
                            val newText = currentText.substring(0, start) + 
                                          replacement + 
                                          currentText.substring(end)
                            
                            // 記憶係に新しい文章と、カーソル位置（挿入したルビの直後）を渡す
                            val newCursorPos = start + replacement.length
                            onValueChange(TextFieldValue(text = newText, selection = TextRange(newCursorPos)))
                        }
                        
                        showSettingsSheet = false
                        rubyReading = ""
                    }) {
                        Text("適用")
                    }
                }
                
                // 下部の余白（スマホのナビゲーションバーと被らないように）
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
