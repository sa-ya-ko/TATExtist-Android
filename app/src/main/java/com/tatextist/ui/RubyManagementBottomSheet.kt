package com.tatextist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tatextist.utils.RubyItem
import com.tatextist.utils.RubyManager

import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

/**
 * ルビ管理（付与・一覧表示・検索・個別解除）を提供するボトムシート
 * Material 3 Expressive および アダプティブレイアウト準拠
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RubyManagementBottomSheet(
    rubyList: List<RubyItem>, // 本文から抽出されたルビ一覧（EditorUiStateより）
    onApplyRuby: (rubyText: String) -> Unit, // ルビ付与の適用コールバック
    onRemoveRubyCurrentSelection: () -> Unit, // 現在選択中の文字のルビ解除コールバック
    onRemoveRubyItem: (RubyItem) -> Unit, // 一覧から指定された特定ルビの解除コールバック
    onSelectRubyItem: (RubyItem) -> Unit = {}, // 一覧アイテムをタップした時のジャンプコールバック
    onTabChanged: (isListTab: Boolean) -> Unit = {}, // 一覧タブが開いているかどうかの通知
    onDismissRequest: () -> Unit // ボトムシートを閉じるコールバック
) {
    val sheetState = rememberModalBottomSheetState()

    // 現在選択されているタブ（0: ルビ付与, 1: ルビ一覧）を記憶
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    // ルビ付与タブのふりがな入力文字列を記憶
    var rubyReading by remember { mutableStateOf("") }
    // ルビ一覧タブの検索入力文字列を記憶
    var searchQuery by remember { mutableStateOf("") }

    // ボトムシートが閉じられたときに確実にハイライトを解除する
    DisposableEffect(Unit) {
        onDispose {
            onTabChanged(false)
        }
    }

    // タブが切り替わったときにハイライト状態を通知
    LaunchedEffect(selectedTabIndex) {
        onTabChanged(selectedTabIndex == 1)
    }

    // 検索語またはルビ一覧に変更があった場合のみ、RubyManager.filterRubyList で絞り込み計算を実行
    val filteredRubyList = remember(rubyList, searchQuery) {
        RubyManager.filterRubyList(rubyList, searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = {
            onTabChanged(false)
            onDismissRequest()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp) // PCやタブレットで横に伸びすぎないよう最大幅を560dpに制限（アダプティブ対応）
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally) // 画面中央に配置
                .verticalScroll(rememberScrollState()) // 縦方向が狭い画面でも全体をスクロール可能にして見切れを防止
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() // システムナビゲーションバーとの重なりを防止
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // シートタイトル
            Text(
                text = "ルビ設定・管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // M3 PrimaryTabRow: 「ルビ付与」と「ルビ一覧 (件数)」の切り替えタブ
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("ルビ付与", style = MaterialTheme.typography.labelLarge) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("ルビ一覧 (${rubyList.size})", style = MaterialTheme.typography.labelLarge) }
                )
            }

            // ==========================================
            // タブ 0: ルビ付与パネル
            // ==========================================
            if (selectedTabIndex == 0) {
                // ふりがな入力欄
                OutlinedTextField(
                    value = rubyReading,
                    onValueChange = { rubyReading = it },
                    placeholder = { Text("例: ふりがな") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 操作ボタン群（解除と適用）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 選択中のルビを解除するボタン
                    OutlinedButton(
                        onClick = {
                            onRemoveRubyCurrentSelection()
                            onDismissRequest()
                            rubyReading = ""
                        },
                        shape = CircleShape
                    ) {
                        Text("ルビ解除")
                    }

                    // 入力したふりがなを適用するボタン
                    Button(
                        onClick = {
                            if (rubyReading.isNotBlank()) {
                                onApplyRuby(rubyReading)
                                onDismissRequest()
                                rubyReading = ""
                            }
                        },
                        enabled = rubyReading.isNotBlank(),
                        shape = CircleShape
                    ) {
                        Text("適用")
                    }
                }
            }

            // ==========================================
            // タブ 1: ルビ一覧・検索パネル
            // ==========================================
            if (selectedTabIndex == 1) {
                // 検索入力欄（親文字またはふりがなで絞り込み）
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("漢字またはふりがなで検索") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "検索"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (rubyList.isEmpty()) {
                    // 本文にルビが1つもない場合の案内表示
                    Text(
                        text = "現在、本文内にルビは設定されていません。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else if (filteredRubyList.isEmpty()) {
                    // 検索条件に該当するルビがない場合の表示
                    Text(
                        text = "「$searchQuery」に一致するルビは見つかりませんでした。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    // 絞り込まれたルビアイテムを一覧表示
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredRubyList.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectRubyItem(item) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 親文字とふりがなの組み合わせ表示
                                Column {
                                    Text(
                                        text = item.parentText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.rubyText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // 個別解除ボタン（タップすると該当ルビを本文から外す）
                                IconButton(
                                    onClick = { onRemoveRubyItem(item) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "このルビを解除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (index < filteredRubyList.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
