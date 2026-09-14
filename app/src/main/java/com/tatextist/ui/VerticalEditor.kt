package com.tatextist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Composable
fun VerticalEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        // 見た目担当（Canvasによる文字を立たせた描画）
        CanvasVerticalText(text = text)

        // 入力受付担当（透明なBasicTextField）
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = Color.Transparent // 透明にして裏方のCanvasを見せる
            ),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = 90f // 入力枠だけ回転させてフリックなどを合わせる
                }
        )
    }
}
