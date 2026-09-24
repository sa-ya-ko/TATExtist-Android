package com.tatextist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleSettingsBottomSheet(
    fontSize: Int,
    fontFamily: EditorFontFamily,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    onFontFamilyChange: (EditorFontFamily) -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onFormatCommand: (String) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "スタイル",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. 装飾セクション（太字・斜体）
            Text(
                text = "装飾",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = isBold,
                    onClick = { onFormatCommand("bold") },
                    label = { 
                        Text("太字 (Bold)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) 
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = isItalic,
                    onClick = { onFormatCommand("italic") },
                    label = { 
                        Text("斜体 (Italic)", style = MaterialTheme.typography.labelLarge, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) 
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. フォント選択セクション
            Text(
                text = "フォント",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = fontFamily == EditorFontFamily.SERIF,
                    onClick = { onFontFamilyChange(EditorFontFamily.SERIF) },
                    label = { Text("明朝体 (Serif)", style = MaterialTheme.typography.labelLarge) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = fontFamily == EditorFontFamily.SANS_SERIF,
                    onClick = { onFontFamilyChange(EditorFontFamily.SANS_SERIF) },
                    label = { Text("ゴシック体 (Sans)", style = MaterialTheme.typography.labelLarge) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 文字サイズ調整
            Text(
                text = "文字サイズ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.FilledTonalIconButton(
                    onClick = onDecreaseFontSize,
                    enabled = fontSize > 12,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "文字サイズを小さくする"
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = "${fontSize} px",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.width(20.dp))

                androidx.compose.material3.FilledTonalIconButton(
                    onClick = onIncreaseFontSize,
                    enabled = fontSize < 64,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "文字サイズを大きくする"
                    )
                }
            }
        }
    }
}

// 既存の呼び出しとの互換用
@Composable
fun FontSettingsBottomSheet(
    fontSize: Int,
    fontFamily: EditorFontFamily,
    onFontFamilyChange: (EditorFontFamily) -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onDismissRequest: () -> Unit
) {
    StyleSettingsBottomSheet(
        fontSize = fontSize,
        fontFamily = fontFamily,
        onFontFamilyChange = onFontFamilyChange,
        onIncreaseFontSize = onIncreaseFontSize,
        onDecreaseFontSize = onDecreaseFontSize,
        onFormatCommand = {},
        onDismissRequest = onDismissRequest
    )
}
