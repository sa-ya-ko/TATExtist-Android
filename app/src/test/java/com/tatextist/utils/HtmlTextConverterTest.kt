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

    @Test
    fun test_convertFromHtml_empty_editor_returns_empty_string() {
        // WebViewで全文字を消去した際に残るタグのパターンを網羅
        assertEquals("", HtmlTextConverter.convertFromHtml("<br>"))
        assertEquals("", HtmlTextConverter.convertFromHtml("<br/>"))
        assertEquals("", HtmlTextConverter.convertFromHtml("<div><br></div>"))
        assertEquals("", HtmlTextConverter.convertFromHtml("<p><br></p>"))
        assertEquals("", HtmlTextConverter.convertFromHtml("\u200B")) // ゼロ幅スペースのみ
        assertEquals("", HtmlTextConverter.convertFromHtml("   "))
    }

    @Test
    fun test_convertFromHtml_purges_orphan_ruby() {
        // 親文字（漢字）が削除され、フリガナのみ残った残骸ルビタグを安全に除去できることを検証
        assertEquals("", HtmlTextConverter.convertFromHtml("<ruby><rt>かんじ</rt></ruby>"))
        assertEquals("", HtmlTextConverter.convertFromHtml("<ruby> <rt>かんじ</rt></ruby>"))
        assertEquals("", HtmlTextConverter.convertFromHtml("<ruby>\u200B<rt>かんじ</rt></ruby>"))
        // 正常なルビがある場合は維持されることを再確認
        assertEquals("|漢字《かんじ》", HtmlTextConverter.convertFromHtml("<ruby>漢字<rt>かんじ</rt></ruby>"))
    }

    @Test
    fun test_extractPlainText_strips_tags_and_extracts_clean_chars() {
        // ユーザーが遭遇したケース: ゴシック体タグが含まれた文字列
        val rawInput = """今度<font style="font-family: sans-serif, &quot;Noto Sans JP&quot;;">はどうかな</font>？"""
        val plain = HtmlTextConverter.extractPlainText(rawInput)
        // タグがすべて除去され、純粋な可視文字「今度はどうかな？」(8文字)になることを検証
        assertEquals("今度はどうかな？", plain)
        assertEquals(8, plain.length)

        // ルビ付きテキストの場合: 親文字のみ抽出されて正確な文字数になることを検証
        val rubyInput = "吾輩は|猫《ねこ》である"
        val plainRuby = HtmlTextConverter.extractPlainText(rubyInput)
        assertEquals("吾輩は猫である", plainRuby)
        assertEquals(7, plainRuby.length) // 「吾輩は猫である」= 7文字 (ルビの「ねこ」や「|《》」はカウントしない)
    }
}

