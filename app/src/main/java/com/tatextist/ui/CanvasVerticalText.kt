package com.tatextist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.text.selection.LocalTextSelectionColors

// タッチ座標から一番近い文字のインデックスを探す関数
fun findClosestIndex(
    touchOffset: Offset,
    layoutDataList: List<CharLayoutData>,
    charSize: Float
): Int {
    if (layoutDataList.isEmpty()) return 0
    var closestIndex = 0
    var minDistanceSq = Float.MAX_VALUE
    
    for (data in layoutDataList) {
        // 文字の中心座標
        val centerX = data.x + charSize / 2f
        val centerY = data.y + charSize / 2f
        val dx = touchOffset.x - centerX
        val dy = touchOffset.y - centerY
        val distanceSq = dx * dx + dy * dy
        if (distanceSq < minDistanceSq) {
            minDistanceSq = distanceSq
            closestIndex = data.index
        }
    }
    return closestIndex
}

@Composable
fun CanvasVerticalText(
    value: TextFieldValue,
    onSelectionChange: (TextRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onBackground
    val textStyle = TextStyle(fontSize = 20.sp, color = textColor)
    val text = value.text

    // ダークモードでも見やすい標準の選択色を取得
    val selectionBackgroundColor = LocalTextSelectionColors.current.backgroundColor

    // タッチ検知のために計算結果を保持する記憶係
    var currentLayoutData by remember { mutableStateOf<List<CharLayoutData>>(emptyList()) }
    var currentCharSize by remember { mutableStateOf(0f) }
    // ドラッグ開始時のインデックスを覚えておく
    var dragStartIndex by remember { mutableStateOf(0) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { this.text = AnnotatedString(text) }
            .pointerInput(text) { // textが変わったら再設定
                detectTapGestures(
                    onTap = { offset ->
                        val index = findClosestIndex(offset, currentLayoutData, currentCharSize)
                        onSelectionChange(TextRange(index, index)) // 単なるタップはカーソル移動のみ
                    },
                    onLongPress = { offset ->
                        val index = findClosestIndex(offset, currentLayoutData, currentCharSize)
                        onSelectionChange(TextRange(index, index + 1)) // 長押しでその1文字を選択する
                    }
                )
            }
            .pointerInput(text) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = findClosestIndex(offset, currentLayoutData, currentCharSize)
                        dragStartIndex = index
                        onSelectionChange(TextRange(index, index))
                    },
                    onDrag = { change, _ ->
                        val currentIndex = findClosestIndex(change.position, currentLayoutData, currentCharSize)
                        // ドラッグ開始位置から現在位置までを選択範囲とする
                        val start = min(dragStartIndex, currentIndex)
                        val end = max(dragStartIndex, currentIndex)
                        // TextFieldValueの仕様上、endは「選択範囲の最後の文字の次」を指すので+1する
                        onSelectionChange(TextRange(start, end + 1))
                    }
                )
            }
    ) {
        val canvasHeight = size.height
        val canvasWidth = size.width
        val charSize = textStyle.fontSize.toPx()
        val lineHeight = charSize * 1.5f
        
        currentCharSize = charSize

        val layoutDataList = VerticalTextLayoutCalculator.calculate(
            text = text,
            maxWidth = canvasWidth,
            maxHeight = canvasHeight,
            charHeight = charSize,
            lineHeight = lineHeight
        )
        
        // タッチ判定用に保存
        currentLayoutData = layoutDataList

        // 1. 選択範囲（ハイライト）の描画
        val selection = value.selection
        if (!selection.collapsed) {
            layoutDataList.forEach { data ->
                // この文字が選択範囲内に入っているか？
                if (data.index in selection.min until selection.max) {
                    drawRect(
                        color = selectionBackgroundColor,
                        topLeft = Offset(data.x, data.y),
                        size = Size(charSize, charSize)
                    )
                }
            }
        }

        // 2. 文字の描画
        layoutDataList.forEach { data ->
            val layoutResult = textMeasurer.measure(
                text = AnnotatedString(data.char.toString()),
                style = textStyle
            )
            drawText(
                textLayoutResult = layoutResult,
                topLeft = Offset(x = data.x, y = data.y)
            )
        }
    }
}
