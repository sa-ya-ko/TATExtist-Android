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

    @Test
    fun test_convertFromHtml_with_consecutive_rubies() {
        val input = "<ruby>検索<rt>けんさく</rt></ruby><ruby>機能<rt>きのう</rt></ruby>"
        val expected = "|検索《けんさく》|機能《きのう》"

        val actual = HtmlTextConverter.convertFromHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_convertToHtml_with_consecutive_rubies() {
        val input = "|検索《けんさく》|機能《きのう》"
        val expected = "<ruby>検索<rt>けんさく</rt></ruby><ruby>機能<rt>きのう</rt></ruby>"

        val actual = HtmlTextConverter.convertToHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_ruby_followed_by_plain_text_conversion() {
        val htmlInput = "<ruby>あ<rt>あいうえお</rt></ruby>あ"
        val expectedAozora = "|あ《あいうえお》あ"

        val actualAozora = HtmlTextConverter.convertFromHtml(htmlInput)
        assertEquals(expectedAozora, actualAozora)

        val actualHtml = HtmlTextConverter.convertToHtml(expectedAozora)
        assertEquals(htmlInput, actualHtml)
    }

    @Test
    fun test_ruby_with_subsequent_text_not_swallowed_in_ruby_base() {
        // 1. 正常なケース: ルビの外側に文字が入力された場合
        val correctHtml = "<ruby>あ<rt>あいうえお</rt></ruby>あ"
        val correctAozora = HtmlTextConverter.convertFromHtml(correctHtml)
        assertEquals("|あ《あいうえお》あ", correctAozora)

        // 2. 異常ケース（不具合発生時の状態）: 親文字の中に取り込まれてしまった場合
        val swallowedHtml = "<ruby>ああ<rt>あいうえお</rt></ruby>"
        val swallowedAozora = HtmlTextConverter.convertFromHtml(swallowedHtml)
        assertEquals("|ああ《あいうえお》", swallowedAozora)

        // 3. 両者が明確に異なる結果になることを確認（取り込みバグの検出保証）
        org.junit.jupiter.api.Assertions.assertNotEquals(correctAozora, swallowedAozora)
    }

    @Test
    fun test_convertFromHtml_ignores_empty_text_nodes_and_zero_width_spaces() {
        // 空文字や長さ0の空テキストノードがrubyの直後に存在していても
        // 青空文庫変換時に余計な記号やスペースが出力されないことを検証
        val htmlWithEmptySibling = "<ruby>あ<rt>あいうえお</rt></ruby>"
        val actual = HtmlTextConverter.convertFromHtml(htmlWithEmptySibling)
        assertEquals("|あ《あいうえお》", actual)
    }

    @Test
    fun test_convertFromHtml_with_ruby_containing_data_attributes() {
        // data-base等の属性が付与されたrubyタグでも正しく青空文庫形式に変換できることを検証
        val input = """<ruby data-base="あ">あ<rt>あいうえお</rt></ruby>"""
        val expected = "|あ《あいうえお》"
        val actual = HtmlTextConverter.convertFromHtml(input)
        assertEquals(expected, actual)
    }

    @Test
    fun test_convertFromHtml_purges_zero_width_space_after_ruby() {
        // ルビの直後に一時的なゼロ幅スペース \u200B が置かれているケース
        val htmlWithZws = "<ruby>あ<rt>あいうえお</rt></ruby>\u200B"
        val actualAozora = HtmlTextConverter.convertFromHtml(htmlWithZws)
        assertEquals("|あ《あいうえお》", actualAozora)

        // 文字数カウントの検証: 「あ」の1文字のみとして正確に判定される
        val plain = HtmlTextConverter.extractPlainText(actualAozora)
        assertEquals("あ", plain)
        assertEquals(1, plain.length)

        // ゼロ幅スペースを挟んで次の文字「い」が入力されたケース
        val htmlWithNextChar = "<ruby>あ<rt>あいうえお</rt></ruby>\u200Bい"
        val actualAozoraNext = HtmlTextConverter.convertFromHtml(htmlWithNextChar)
        assertEquals("|あ《あいうえお》い", actualAozoraNext)
        val plainNext = HtmlTextConverter.extractPlainText(actualAozoraNext)
        assertEquals("あい", plainNext)
        assertEquals(2, plainNext.length)
    }
}

