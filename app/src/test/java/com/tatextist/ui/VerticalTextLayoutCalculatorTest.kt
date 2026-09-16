package com.tatextist.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerticalTextLayoutCalculatorTest {

    @Test
    fun calculate_wrapsLine_whenTextExceedsMaxHeight() {
        // 1. 準備 (Arrange): 画用紙の高さなどを決めます
        val maxHeight = 100f
        val charHeight = 40f
        val lineHeight = 50f
        val maxWidth = 100f
        val text = "あいうえおか" // 6文字

        // 2. 実行 (Act): 計算係に、それぞれの文字をどこに配置すべきか計算してもらいます
        val layout = VerticalTextLayoutCalculator.calculate(
            text = text,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            charHeight = charHeight,
            lineHeight = lineHeight
        )

        // 3. 確認 (Assert):
        // 高さ100の画用紙に、高さ40の文字を書くので、1行には2文字（40+40=80）しか入らないはずです。
        // つまり、「あい」「うえ」「おか」という3行に分かれる正解になるかをチェックします。
        
        assertEquals(6, layout.size)
        // 1文字目「あ」は 1行目(X = 100 - 40 = 60) の 1文字目(Y = 0)
        assertEquals(60f, layout[0].x)
        assertEquals(0f, layout[0].y)
        assertEquals(0, layout[0].index)
        
        // 3文字目「う」は改行されて 2行目(X = 60 - 50 = 10) の 1文字目(Y = 0) になるはず
        assertEquals(10f, layout[2].x)
        assertEquals(0f, layout[2].y)
        assertEquals(2, layout[2].index)
    }

    @Test
    fun testRubyLayout() {
        // | は1文字、《 》 はルビを囲む記号
        val text = "あ|漢字《かんじ》い"
        val layout = VerticalTextLayoutCalculator.calculate(
            text = text,
            maxWidth = 100f,
            maxHeight = 200f,
            charHeight = 10f,
            lineHeight = 15f
        )

        // 期待される文字数: 'あ'(1) + '漢','字'(2) + 'か','ん','じ'(3) + 'い'(1) = 7文字
        // 記号 | 《 》 は出力されない
        assertEquals(7, layout.size)

        // 'あ' (index 0)
        assertEquals('あ', layout[0].char)
        assertEquals(false, layout[0].isRuby)
        assertEquals(0, layout[0].index)

        // '漢' (index 2: |の次)
        assertEquals('漢', layout[1].char)
        assertEquals(false, layout[1].isRuby)
        assertEquals(2, layout[1].index)

        // 'か' (ルビ, index 5)
        assertEquals('か', layout[3].char)
        assertEquals(true, layout[3].isRuby)
        assertEquals(5, layout[3].index)
        
        // ルビは親文字の少し右側（X座標が大きい）に配置されるはず
        assertTrue(layout[3].x > layout[1].x)

        // 'い' (index 9)
        assertEquals('い', layout[6].char)
        assertEquals(false, layout[6].isRuby)
        assertEquals(9, layout[6].index)
    }
}
