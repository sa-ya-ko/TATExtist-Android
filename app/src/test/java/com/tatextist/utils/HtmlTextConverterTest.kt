package com.tatextist.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HtmlTextConverterTest {

    @Test
    fun test_convertToHtml_with_ruby() {
        val input = "吾輩は|猫《ねこ》である\n名前はまだない"
        val expected = "吾輩は<ruby>猫<rt>ねこ</rt></ruby>である<br>名前はまだない"
        
        val actual = HtmlTextConverter.convertToHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_convertFromHtml_with_ruby() {
        val input = "吾輩は<ruby>猫<rt>ねこ</rt></ruby>である"
        val expected = "吾輩は|猫《ねこ》である"
        
        val actual = HtmlTextConverter.convertFromHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_convertFromHtml_cleans_unwanted_tags() {
        val input = "<div>あいう</div><div>えお<br></div>"
        // 期待値: <div>の区切りは改行、<br>も改行として解釈する
        val expected = "あいう\nえお\n"
        
        val actual = HtmlTextConverter.convertFromHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_convertToHtml_with_decorations() {
        // Compose側のプレーンテキストに太字や斜体のタグが含まれていた場合、
        // WebViewにそのまま渡る（エスケープや破壊をしない）ことを期待
        val input = "吾輩は<b>猫</b>で<i>ある</i>"
        val expected = "吾輩は<b>猫</b>で<i>ある</i>"
        
        val actual = HtmlTextConverter.convertToHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_convertFromHtml_preserves_decorations() {
        val input = "吾輩は<b>猫</b>で<i>ある</i>"
        val expected = "吾輩は<b>猫</b>で<i>ある</i>"
        
        val actual = HtmlTextConverter.convertFromHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_convertFromHtml_preserves_span_styles() {
        val input = "吾輩は<span style=\"font-size: 24px;\">猫</span>である"
        val expected = "吾輩は<span style=\"font-size: 24px;\">猫</span>である"
        
        val actual = HtmlTextConverter.convertFromHtml(input)
        assertEquals(expected, actual)
    }
}

