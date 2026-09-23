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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    onTitleChange: (String) -> Unit = {},
    onValueChange: (TextFieldValue) -> Unit = {},
    onSaveToCloud: () -> Unit = {},
    onLoadFromCloud: () -> Unit = {},
    onSelectDriveFile: (com.tatextist.data.DriveFileInfo) -> Unit = {},
    onCloseFilePickerModal: () -> Unit = {},
    onSignOutFromCloud: () -> Unit = {},
    onOpenWordCountModal: () -> Unit = {},
    onCloseWordCountModal: () -> Unit = {},
    onToggleShowWordCountWhileTyping: (Boolean) -> Unit = {},
    onOpenFontSettings: () -> Unit = {},
    onCloseFontSettings: () -> Unit = {},
    onFontFamilyChange: (EditorFontFamily) -> Unit = {},
    onIncreaseFontSize: () -> Unit = {},
    onDecreaseFontSize: () -> Unit = {},
    onConsumeFontApplyRequest: () -> Unit = {},
    onStyleDetected: (EditorFontFamily, Int) -> Unit = { _, _ -> }
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // エラーが出た時はスナックバーで表示する
    LaunchedEffect(uiState.cloudError) {
        if (uiState.cloudError != null) {
            snackbarHostState.showSnackbar(uiState.cloudError)
        }
    }
    // 1. 記憶係（設定パネルが開いているかどうかを覚えておくメモ）
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // ツールメデュードロップダウンの開閉状態
    var showToolsMenu by remember { mutableStateOf(false) }

    // 2. ルビの「ふりがな」を入力するための記憶係
    var rubyReading by remember { mutableStateOf("") }
    
    // 3. WebViewにルビ挿入を命令するためのリクエスト用変数
    var rubyRequest by remember { mutableStateOf<String?>(null) }

    // 4. 太字や斜体、ルビ解除を命令するための変数
    var formatCommand by remember { mutableStateOf<String?>(null) }

    // 画面全体の骨組み（Scaffold）を作ります
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 画面の上のバー（タイトルを表示・編集）
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.text.BasicTextField(
                        value = uiState.documentTitle,
                        onValueChange = onTitleChange,
                        singleLine = true,
                        textStyle = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.documentTitle.isEmpty()) {
                                    Text(
                                        text = "無題のドキュメント",
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    IconButton(
                        onClick = { formatCommand = "undo" },
                        enabled = !uiState.isCloudLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "元に戻す"
                        )
                    }
                    IconButton(
                        onClick = { formatCommand = "redo" },
                        enabled = !uiState.isCloudLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "やり直す"
                        )
                    }

                    if (uiState.isCloudLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    TextButton(
                        onClick = onSaveToCloud,
                        enabled = !uiState.isCloudLoading
                    ) {
                        Text("保存")
                    }
                    TextButton(
                        onClick = onLoadFromCloud,
                        enabled = !uiState.isCloudLoading
                    ) {
                        Text("読込")
                    }

                    // 「ツール」ドロップダウンメニュー
                    Box {
                        TextButton(onClick = { showToolsMenu = true }) {
                            Text("ツール")
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showToolsMenu,
                            onDismissRequest = { showToolsMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("文字カウント") },
                                onClick = {
                                    showToolsMenu = false
                                    onOpenWordCountModal()
                                }
                            )
                            if (uiState.isDriveSignedIn) {
                                androidx.compose.material3.HorizontalDivider()
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "Drive連携解除",
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.error
                                        ) 
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        onSignOutFromCloud()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        // 画面の下のバー（ルビ付与ボタンなどを配置）
        bottomBar = {
            BottomAppBar {
                // 2. ボタンを押したら、メモを「開く（true）」に書き換える
                TextButton(onClick = { showSettingsSheet = true }) {
                    Text("ルビ付与")
                }
                TextButton(onClick = { formatCommand = "removeRuby" }) {
                    Text("ルビ解除")
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // 「入力中に文字数を表示する」が有効な場合のみリアルタイム表示（タップで詳細モーダルを開く）
                if (uiState.showWordCountWhileTyping) {
                    TextButton(
                        onClick = onOpenWordCountModal,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${uiState.characterCount}字",
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                TextButton(onClick = { formatCommand = "bold" }) {
                    Text("B", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                TextButton(onClick = { formatCommand = "italic" }) {
                    Text("I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                IconButton(onClick = onOpenFontSettings) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = "文字設定"
                    )
                }
            }
        }
    ) { innerPadding ->
        // 画面の真ん中（本文を書く場所）に縦書きエディタを配置
        VerticalEditor(
            value = uiState.content,
            onValueChange = onValueChange,
            rubyRequest = rubyRequest,
            onRubyApplied = { rubyRequest = null },
            formatCommand = formatCommand,
            onFormatApplied = { formatCommand = null },
            fontSize = uiState.fontSize,
            fontFamily = uiState.fontFamily,
            fontApplyRequest = uiState.fontApplyRequest,
            onFontApplyConsumed = onConsumeFontApplyRequest,
            onStyleDetected = onStyleDetected,
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
                        // WebViewにルビ挿入のリクエストを送る（テキストの切り貼りはWebViewのJSに任せる）
                        rubyRequest = rubyReading
                        
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

    // 文字・フォント設定ボトムシート
    if (uiState.showFontSettingsBottomSheet) {
        FontSettingsBottomSheet(
            fontSize = uiState.fontSize,
            fontFamily = uiState.fontFamily,
            onFontFamilyChange = onFontFamilyChange,
            onIncreaseFontSize = onIncreaseFontSize,
            onDecreaseFontSize = onDecreaseFontSize,
            onDismissRequest = onCloseFontSettings
        )
    }

    // 4. 文字カウントモーダル（Web版のWordCountModalに準拠）
    if (uiState.showWordCountModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCloseWordCountModal,
            title = {
                Text(
                    text = "文字カウント",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("文字数（スペースなし）")
                        Text(
                            text = "${uiState.characterCountWithoutSpaces}",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    androidx.compose.material3.HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("文字数（スペースあり）")
                        Text(
                            text = "${uiState.characterCount}",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    androidx.compose.material3.HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = uiState.showWordCountWhileTyping,
                            onCheckedChange = onToggleShowWordCountWhileTyping
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "入力中に文字数を表示する",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onCloseWordCountModal) {
                    Text("OK")
                }
            }
        )
    }

    // 5. Google Drive ファイル選択モーダル
    if (uiState.showFilePickerModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCloseFilePickerModal,
            title = {
                Text(
                    text = "Driveから開く",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
            },
            text = {
                if (uiState.cloudFiles.isEmpty()) {
                    Text(
                        text = "保存されているファイルがありません",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(uiState.cloudFiles.size) { index ->
                            val file = uiState.cloudFiles[index]
                            androidx.compose.material3.Surface(
                                onClick = { onSelectDriveFile(file) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = androidx.compose.ui.graphics.Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = file.name,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            if (index < uiState.cloudFiles.size - 1) {
                                androidx.compose.material3.HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onCloseFilePickerModal) {
                    Text("キャンセル")
                }
            }
        )
    }
}
