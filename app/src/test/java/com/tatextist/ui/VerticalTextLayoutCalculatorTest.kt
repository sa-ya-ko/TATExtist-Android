package com.tatextist.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerticalTextLayoutCalculatorTest {

    @Test
    fun calculateLayout_wrapsToNextLine_whenHeightExceedsMax() {
        // 1. 準備 (Arrange): 画用紙の高さと、1文字のサイズを仮決めします
        val maxHeight = 100f
        val charHeight = 40f
        val lineHeight = 50f // 行の間隔（右から左へズレる幅）
        val text = "あいうえおか" // 6文字

        // 2. 実行 (Act): 計算係に、それぞれの文字をどこに配置すべきか計算してもらいます
        val result = VerticalTextLayoutCalculator.calculate(
            text = text,
            maxHeight = maxHeight,
            charHeight = charHeight,
            lineHeight = lineHeight
        )

        // 3. 確認 (Assert):
        // 高さ100の画用紙に、高さ40の文字を書くので、1行には2文字（40+40=80）しか入らないはずです。
        // つまり、「あい」「うえ」「おか」という3行に分かれる正解になるかをチェックします。
        
        // 1文字目「あ」は 1行目(X = 0) の 1文字目(Y = 0)
        assertEquals(0f, result[0].x)
        assertEquals(0f, result[0].y)
        
        // 3文字目「う」は改行されて 2行目(X = -50) の 1文字目(Y = 0) になるはず
        assertEquals(-50f, result[2].x)
        assertEquals(0f, result[2].y)
    }
}
