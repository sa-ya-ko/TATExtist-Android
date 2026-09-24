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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
    onStyleDetected: (EditorFontFamily, Int) -> Unit = { _, _ -> },
    onToggleDarkMode: () -> Unit = {}
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
    
    // ツールボトムシートの開閉状態
    var showToolsBottomSheet by remember { mutableStateOf(false) }

    // 2. ルビの「ふりがな」を入力するための記憶係
    var rubyReading by remember { mutableStateOf("") }
    
    // 3. WebViewにルビ挿入を命令するためのリクエスト用変数
    var rubyRequest by remember { mutableStateOf<String?>(null) }

    // 4. 太字や斜体、ルビ解除を命令するための変数
    var formatCommand by remember { mutableStateOf<String?>(null) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }

    // 画面全体の骨組み（Scaffold）を作ります
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 画面の上のバー（タイトルを表示・編集）
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
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
                    // 1. 保存（Google Drive保存）ボタン
                    if (uiState.isCloudLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onSaveToCloud,
                        enabled = !uiState.isCloudLoading,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("保存", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 2. Undo（取り消し）ボタン
                    IconButton(
                        onClick = { formatCommand = "undo" },
                        enabled = !uiState.isCloudLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "元に戻す"
                        )
                    }

                    // 3. Redo（やり直し）ボタン
                    IconButton(
                        onClick = { formatCommand = "redo" },
                        enabled = !uiState.isCloudLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "やり直す"
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // 4. ダークモード/ライトモード切り替え（最右端）
                    FilledTonalIconToggleButton(
                        checked = uiState.isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.padding(horizontal = 2.dp),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        AnimatedContent(
                            targetState = uiState.isDarkMode,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.85f))
                                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.85f))
                            },
                            label = "DarkModeToggleAnimation"
                        ) { isDark ->
                            if (isDark) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = "ライトモードに切り替え"
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = "ダークモードに切り替え"
                                )
                            }
                        }
                    }
                }
            )
        },
        // 画面の下のバー（M3 Expressive フローティング・ツールバー）
        bottomBar = {
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. スタイルボトムシート展開ボタン
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onOpenFontSettings()
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("スタイル", style = MaterialTheme.typography.labelLarge)
                    }

                    // 2. ルビボトムシート展開ボタン
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            showSettingsSheet = true
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("ルビ", style = MaterialTheme.typography.labelLarge)
                    }

                    // 3. ツールボトムシート展開ボタン
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            showToolsBottomSheet = true
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("ツール", style = MaterialTheme.typography.labelLarge)
                    }

                    // 4. リアルタイム文字数バッジ（ツールの右側に配置）
                    if (uiState.showWordCountWhileTyping) {
                        androidx.compose.material3.Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onOpenWordCountModal()
                            },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "${uiState.characterCount}字",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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

    // 3. ボトムシート（ルビ設定パネル：付与と解除を統合）
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showSettingsSheet = false 
                rubyReading = "" // 閉じる時は入力欄を空にする
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ルビ（ふりがな）設定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = rubyReading,
                    onValueChange = { rubyReading = it },
                    placeholder = { Text("例: ふりがな") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            formatCommand = "removeRuby"
                            showSettingsSheet = false
                            rubyReading = ""
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Text("ルビ解除")
                    }

                    Row {
                        TextButton(onClick = { 
                            showSettingsSheet = false
                            rubyReading = ""
                        }) {
                            Text("キャンセル")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                rubyRequest = rubyReading
                                showSettingsSheet = false
                                rubyReading = ""
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Text("適用")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 文字・フォント・スタイル設定ボトムシート
    if (uiState.showFontSettingsBottomSheet) {
        StyleSettingsBottomSheet(
            fontSize = uiState.fontSize,
            fontFamily = uiState.fontFamily,
            isBold = isBold,
            isItalic = isItalic,
            onFontFamilyChange = onFontFamilyChange,
            onIncreaseFontSize = onIncreaseFontSize,
            onDecreaseFontSize = onDecreaseFontSize,
            onFormatCommand = { cmd ->
                if (cmd == "bold") isBold = !isBold
                if (cmd == "italic") isItalic = !isItalic
                formatCommand = cmd
            },
            onDismissRequest = onCloseFontSettings
        )
    }

    // ツールボトムシート（M3 Expressive 準拠）
    if (showToolsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showToolsBottomSheet = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ツール",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Google Driveから開く（読込）
                androidx.compose.material3.Surface(
                    onClick = {
                        showToolsBottomSheet = false
                        onLoadFromCloud()
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Driveから開く（読込）",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 2. 文字数カウント
                androidx.compose.material3.Surface(
                    onClick = {
                        showToolsBottomSheet = false
                        onOpenWordCountModal()
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "文字数カウント",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 3. Drive連携解除（ログイン時のみ）
                if (uiState.isDriveSignedIn) {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    androidx.compose.material3.Surface(
                        onClick = {
                            showToolsBottomSheet = false
                            onSignOutFromCloud()
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drive連携解除",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // 4. 文字カウントモーダル（Web版のWordCountModalに準拠）
    if (uiState.showWordCountModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCloseWordCountModal,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "文字カウント",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                        Text("文字数（スペースを除く）")
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
                        Text("文字数")
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
                Button(
                    onClick = onCloseWordCountModal,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text("OK")
                }
            }
        )
    }

    // 5. Google Drive ファイル選択モーダル
    if (uiState.showFilePickerModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCloseFilePickerModal,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Driveから開く",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                color = androidx.compose.ui.graphics.Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
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
                TextButton(
                    onClick = onCloseFilePickerModal,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text("キャンセル")
                }
            }
        )
    }
}
