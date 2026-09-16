package com.tatextist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp

@Composable
fun VerticalEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    // タッチされた時にキーボードを呼び出すためのスイッチ
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        // 1. 入力受付担当（後ろに隠す）
        // キャンバスの後ろにあるため、直接のタッチは検知できなくなります
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester) // フォーカス要求を受け取れるようにする
                .graphicsLayer {
                    rotationZ = 90f
                }
        )
        
        // 2. 見た目担当（上に重ねてタッチを横取りする）
        // 指が触れた場所を自力で計算し、選択範囲の変更とハイライトを行います
        CanvasVerticalText(
            value = value,
            onSelectionChange = { newSelection ->
                // 新しい選択範囲を上位（記憶係）に伝える
                onValueChange(value.copy(selection = newSelection))
                // タッチされたら、後ろに隠れている入力枠にフォーカスを当ててキーボードを出す
                focusRequester.requestFocus()
            }
        )
    }
}
