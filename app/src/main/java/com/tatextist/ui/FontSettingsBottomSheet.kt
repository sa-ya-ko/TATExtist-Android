package com.tatextist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    isUnderline: Boolean = false,
    isBullet: Boolean = false,
    indentWidth: Float = 0.0f,
    paperSize: PaperSize = PaperSize.FIT,
    margins: EditorMargins = EditorMargins(),
    onFontFamilyChange: (EditorFontFamily) -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onIndentWidthChange: (Float) -> Unit = {},
    onPaperSizeChange: (PaperSize) -> Unit = {},
    onMarginChange: (top: Int?, bottom: Int?, right: Int?, left: Int?) -> Unit = { _, _, _, _ -> },
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

            // 1. 装飾セクション（太字・斜体・傍線・箇条書き）
            Text(
                text = "装飾",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isBold,
                    onClick = { onFormatCommand("bold") },
                    label = { 
                        Text("太字", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) 
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = isItalic,
                    onClick = { onFormatCommand("italic") },
                    label = { 
                        Text("斜体", style = MaterialTheme.typography.labelMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) 
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = isUnderline,
                    onClick = { onFormatCommand("underline") },
                    label = { 
                        Text("傍線", style = MaterialTheme.typography.labelMedium, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline) 
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = isBullet,
                    onClick = { onFormatCommand("bulletList") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { 
                        Text("箇条書き", style = MaterialTheme.typography.labelMedium) 
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1.3f)
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

            // 3. 文字サイズ設定セクション
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

            Spacer(modifier = Modifier.height(24.dp))

            // 4. インデント設定
            Text(
                text = "インデント",
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
                    onClick = { if (indentWidth > 0.0f) onIndentWidthChange((indentWidth - 0.5f).coerceAtLeast(0.0f)) },
                    enabled = indentWidth > 0.0f,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "インデント幅を減らす")
                }

                Spacer(modifier = Modifier.width(20.dp))

                val formattedWidth = when {
                    indentWidth <= 0.0f -> "なし"
                    indentWidth % 1.0f == 0.0f -> "${indentWidth.toInt()} 字"
                    else -> String.format(java.util.Locale.JAPAN, "%.1f 字", indentWidth)
                }

                Text(
                    text = formattedWidth,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.width(20.dp))

                androidx.compose.material3.FilledTonalIconButton(
                    onClick = { if (indentWidth < 4.0f) onIndentWidthChange((indentWidth + 0.5f).coerceAtMost(4.0f)) },
                    enabled = indentWidth < 4.0f,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "インデント幅を増やす")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 用紙サイズ設定
            Text(
                text = "用紙サイズ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = paperSize == PaperSize.FIT,
                    onClick = { onPaperSizeChange(PaperSize.FIT) },
                    label = { Text("フィット", style = MaterialTheme.typography.labelMedium) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = paperSize == PaperSize.A4,
                    onClick = { onPaperSizeChange(PaperSize.A4) },
                    label = { Text("A4 (横長)", style = MaterialTheme.typography.labelMedium) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = paperSize == PaperSize.B5,
                    onClick = { onPaperSizeChange(PaperSize.B5) },
                    label = { Text("B5 (横長)", style = MaterialTheme.typography.labelMedium) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. 余白(px) 設定
            Text(
                text = "余白(px)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 1段目: 上()　下()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MarginInputField(
                    label = "上",
                    value = margins.top,
                    onValueChange = { onMarginChange(it, null, null, null) },
                    modifier = Modifier.weight(1f)
                )
                MarginInputField(
                    label = "下",
                    value = margins.bottom,
                    onValueChange = { onMarginChange(null, it, null, null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2段目: 右()　左()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MarginInputField(
                    label = "右",
                    value = margins.right,
                    onValueChange = { onMarginChange(null, null, it, null) },
                    modifier = Modifier.weight(1f)
                )
                MarginInputField(
                    label = "左",
                    value = margins.left,
                    onValueChange = { onMarginChange(null, null, null, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarginInputField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) {
        mutableStateOf(value.toString())
    }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newStr: String ->
            val digits = newStr.filter { ch: Char -> ch.isDigit() }
            textValue = digits
            val intVal = digits.toIntOrNull()
            if (intVal != null) {
                onValueChange(intVal.coerceIn(0, 120))
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier
    )
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
