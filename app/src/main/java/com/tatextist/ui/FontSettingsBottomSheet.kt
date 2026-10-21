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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleSettingsBottomSheet(
    fontSize: Int,
    fontFamily: EditorFontFamily,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isUnderline: Boolean = false,
    isBullet: Boolean = false,
    selectedTextColor: String? = null,
    customColors: List<String> = emptyList(),
    indentWidth: Float = 0.0f,
    paperSize: PaperSize = PaperSize.FIT,
    margins: EditorMargins = EditorMargins(),
    currentParagraphStyle: ParagraphStyle = ParagraphStyle.NORMAL,
    onFontFamilyChange: (EditorFontFamily) -> Unit,
    onParagraphStyleChange: (ParagraphStyle) -> Unit = {},
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onTextColorChange: (String) -> Unit = {},
    onResetTextColor: () -> Unit = {},
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

            // 1.5. 段落スタイルセクション（タイトル、見出し1〜3、標準テキスト）
            Text(
                text = "段落スタイル",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ParagraphStyle.values().forEach { style ->
                    val isSelected = currentParagraphStyle == style
                    FilterChip(
                        selected = isSelected,
                        onClick = { onParagraphStyleChange(style) },
                        label = {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (style != ParagraphStyle.NORMAL) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                }
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

            Spacer(modifier = Modifier.height(20.dp))

            // 2.5 文字色（カラーパレット & カスタム色）
            var showColorPickerDialog by remember { mutableStateOf(false) }

            Text(
                text = "文字色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // プリセット色リスト
            val presetColors = listOf(
                "#000000", // 黒
                "#FFFFFF", // 白
                "#E53935", // 赤
                "#E91E63", // ピンク
                "#1E88E5", // 青
                "#43A047", // 緑
                "#FB8C00"  // オレンジ
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // プリセット色
                presetColors.forEach { hex ->
                    val isSelected = selectedTextColor.equals(hex, ignoreCase = true)
                    val parsedColor = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Black)
                    val isWhite = hex.equals("#FFFFFF", ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else (if (isWhite) MaterialTheme.colorScheme.outline else Color.LightGray.copy(alpha = 0.5f)),
                                shape = CircleShape
                            )
                            .clickable {
                                if (isSelected) {
                                    onResetTextColor()
                                } else {
                                    onTextColorChange(hex)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "選択中",
                                tint = if (hex == "#000000" || hex == "#1E88E5" || hex == "#8E24AA") Color.White else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ユーザー作成カスタム色の履歴（プリセットにないもの）
                customColors.filter { customHex ->
                    presetColors.none { it.equals(customHex, ignoreCase = true) }
                }.forEach { customHex ->
                    val isSelected = selectedTextColor.equals(customHex, ignoreCase = true)
                    val parsedColor = runCatching { Color(android.graphics.Color.parseColor(customHex)) }.getOrDefault(Color.Black)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable {
                                if (isSelected) {
                                    // すでに選択されているカスタム色をタップした場合はカラーピッカーを再起動
                                    showColorPickerDialog = true
                                } else {
                                    onTextColorChange(customHex)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "選択中",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 自由色ピッカー起動ボタン（＋）
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { showColorPickerDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "カスタムカラーを選択",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showColorPickerDialog) {
                CustomColorPickerDialog(
                    initialColorHex = selectedTextColor ?: "#000000",
                    onDismissRequest = { showColorPickerDialog = false },
                    onColorConfirmed = { hex ->
                        onTextColorChange(hex)
                        showColorPickerDialog = false
                    }
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

/**
 * ユーザーが自由に色を選択できるカラーピッカーダイアログ
 * 色相（Hue: 0~360）、彩度（Saturation: 0~1）、明度（Value: 0~1）のスライダーで直感的に色を調合可能
 */
@Composable
fun CustomColorPickerDialog(
    initialColorHex: String,
    onDismissRequest: () -> Unit,
    onColorConfirmed: (String) -> Unit
) {
    val initialHsv = remember(initialColorHex) {
        val hsv = FloatArray(3)
        val parsed = runCatching { android.graphics.Color.parseColor(initialColorHex) }.getOrDefault(android.graphics.Color.BLACK)
        android.graphics.Color.colorToHSV(parsed, hsv)
        hsv
    }

    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(if (initialHsv[1] == 0f && initialHsv[2] == 0f) 1f else initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }

    val currentColorArgb = remember(hue, saturation, value) {
        android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }
    val currentHex = remember(currentColorArgb) {
        String.format("#%06X", 0xFFFFFF and currentColorArgb)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "カスタムカラーを選択", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // プレビュー表示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(currentColorArgb))
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = currentHex,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 色相 (H)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "色相 (H)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 彩度 (S)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "彩度 (S)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 明度 (V)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "明度 (V)", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorConfirmed(currentHex) }) {
                Text("適用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("キャンセル")
            }
        }
    )
}
