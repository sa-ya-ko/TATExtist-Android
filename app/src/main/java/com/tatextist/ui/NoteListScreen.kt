package com.tatextist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tatextist.data.NoteItem
import com.tatextist.utils.HtmlTextConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Google Keepライクなメモ一覧画面
 * Material 3 Expressive ガイドラインに準拠したカードレイアウトとピン留め固定表示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    notes: List<NoteItem>,
    onNoteClick: (String) -> Unit,
    onCreateNewNote: () -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 表示モード: 通常の一覧 or アーカイブ一覧
    var showArchivedOnly by remember { mutableStateOf(false) }

    // フィルタリング（アーカイブ状態 ＆ 検索キーワードによる絞り込み）
    val activeNotes = remember(notes, showArchivedOnly, searchQuery) {
        val base = if (showArchivedOnly) {
            notes.filter { it.isArchived }
        } else {
            notes.filter { !it.isArchived }
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            val query = searchQuery.trim().lowercase()
            base.filter { note ->
                note.title.lowercase().contains(query) ||
                HtmlTextConverter.extractPlainText(note.content).lowercase().contains(query)
            }
        }
    }

    val pinnedNotes = remember(activeNotes, showArchivedOnly, searchQuery) {
        if (showArchivedOnly || searchQuery.isNotBlank()) emptyList() else activeNotes.filter { it.isPinned }
    }

    val otherNotes = remember(activeNotes, showArchivedOnly, searchQuery) {
        if (showArchivedOnly || searchQuery.isNotBlank()) activeNotes else activeNotes.filter { !it.isPinned }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            // Google Keep スタイルのフローティング検索バー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 虫眼鏡アイコン
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "検索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // 検索入力テキストフィールド
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = if (showArchivedOnly) "アーカイブを検索" else "メモを検索",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // 検索クリアボタン（文字入力時のみ表示）
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "検索クリア",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // アーカイブ切り替えアイコンボタン
                        IconButton(
                            onClick = { showArchivedOnly = !showArchivedOnly },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (showArchivedOnly) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (showArchivedOnly) "メモ一覧に戻る" else "アーカイブを見る",
                                tint = if (showArchivedOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // 新規作成ボタン（Keep風 Extended FAB）
            ExtendedFloatingActionButton(
                onClick = onCreateNewNote,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.Add, contentDescription = "新規作成") },
                text = { Text("新しいメモ", style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            )
        }
    ) { innerPadding ->
        if (activeNotes.isEmpty()) {
            // メモが1件もない場合の空画面表示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                val emptyMessage = when {
                    searchQuery.isNotBlank() -> "一致するメモは見つかりませんでした"
                    showArchivedOnly -> "アーカイブされたメモはありません"
                    else -> "メモがありません。\n右下のボタンから執筆を始めましょう。"
                }
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                // ピン留めセクション
                if (pinnedNotes.isNotEmpty()) {
                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = "ピン留め",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp)
                        )
                    }
                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onTogglePin = { onTogglePin(note.id) },
                            onToggleArchive = { onToggleArchive(note.id) },
                            onDelete = { onDeleteNote(note.id) }
                        )
                    }
                }

                // 通常セクション
                if (otherNotes.isNotEmpty()) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "その他",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 12.dp)
                            )
                        }
                    }
                    items(otherNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onTogglePin = { onTogglePin(note.id) },
                            onToggleArchive = { onToggleArchive(note.id) },
                            onDelete = { onDeleteNote(note.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * メモカードコンポーネント（M3 Expressive カードデザイン）
 */
@Composable
private fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(note.updatedAt) {
        dateFormatter.format(Date(note.updatedAt))
    }

    // Android 14+ OS標準コントラスト設定に応じたカード枠線（境界線）の動的調整
    val context = androidx.compose.ui.platform.LocalContext.current
    val contrastLevel = remember { com.tatextist.utils.ContrastHelper.getContrastLevel(context) }
    val cardBorder = when (contrastLevel) {
        com.tatextist.utils.ContrastLevel.HIGH -> androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.outline
        )
        com.tatextist.utils.ContrastLevel.MEDIUM -> androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        com.tatextist.utils.ContrastLevel.STANDARD -> null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // タイトルとピン留めボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (note.title.isNotBlank()) note.title else "無題",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "ピン留め解除" else "ピン留め",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 本文プレビュー（HTMLタグやルビ記法を除去したプレーンテキストで表示）
            val plainPreview = remember(note.content) {
                HtmlTextConverter.extractPlainText(note.content)
            }
            if (plainPreview.isNotBlank()) {
                Text(
                    text = plainPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // フッター（更新日時 ＆ アーカイブ・削除アクション）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row {
                    IconButton(
                        onClick = onToggleArchive,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (note.isArchived) "復元" else "アーカイブ",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
