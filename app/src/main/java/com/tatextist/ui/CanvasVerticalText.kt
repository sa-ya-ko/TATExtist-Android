package com.tatextist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@Composable
fun CanvasVerticalText(
    text: String,
    modifier: Modifier = Modifier
) {
    // 文字のサイズなどを計算する係（TextMeasurer）
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = 20.sp)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // 自動テストや読み上げ機能に「ここにこの文字があるよ」と教える設定
            .semantics { this.text = AnnotatedString(text) }
    ) {
        val canvasHeight = size.height
        // 1文字の縦横の基本サイズ（フォントサイズと同じ）
        val charSize = textStyle.fontSize.toPx()
        // 行の間隔（文字の幅の1.5倍に設定）
        val lineHeight = charSize * 1.5f

        // 先ほど作った計算係に、すべての文字の座標（XとY）を計算してもらいます
        val layoutDataList = VerticalTextLayoutCalculator.calculate(
            text = text,
            maxHeight = canvasHeight,
            charHeight = charSize,
            lineHeight = lineHeight
        )

        // 計算結果に従って、画用紙に文字を順番に描いていきます
        layoutDataList.forEach { data ->
            // 1文字のサイズを測る
            val layoutResult = textMeasurer.measure(
                text = AnnotatedString(data.char.toString()),
                style = textStyle
            )

            // 計算済みの座標 (data.x, data.y) に文字を描画する
            drawText(
                textLayoutResult = layoutResult,
                topLeft = Offset(x = data.x, y = data.y)
            )
        }
    }
}
