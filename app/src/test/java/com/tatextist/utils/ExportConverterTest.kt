package com.tatextist.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportConverterTest {

    @Test
    fun `convertToMarkdown converts heading tags to markdown hashes`() {
        val html = "<h1>タイトル</h1><h2>大見出し</h2><h3>中見出し</h3>"
        val expected = "# タイトル\n\n## 大見出し\n\n### 中見出し\n\n"
        val actual = ExportConverter.convertToMarkdown(html)
        assertEquals(expected, actual)
    }

    @Test
    fun `convertToMarkdown converts bold italic and decorations`() {
        val html = "<b>太字テキスト</b>と<i>斜体テキスト</i>"
        val expected = "**太字テキスト**と*斜体テキスト*"
        val actual = ExportConverter.convertToMarkdown(html)
        assertEquals(expected, actual)
    }

    @Test
    fun `convertToMarkdown converts bullet list items`() {
        val html = "<div class=\"bullet-line\">・箇条書き1</div><div class=\"bullet-line\">・箇条書き2</div>"
        val expected = "- 箇条書き1\n- 箇条書き2\n"
        val actual = ExportConverter.convertToMarkdown(html)
        assertEquals(expected, actual)
    }

    @Test
    fun `convertToMarkdown preserves ruby tags`() {
        val html = "<ruby>親文字<rt>ルビ</rt></ruby>"
        val actual = ExportConverter.convertToMarkdown(html)
        assertTrue(actual.contains("<ruby>親文字<rt>ルビ</rt></ruby>"))
    }

    @Test
    fun `exportToStandaloneHtml generates complete html document with vertical layout`() {
        val title = "テスト小説"
        val bodyHtml = "<h1>テスト小説</h1><p>これは縦書きの本文です。</p>"
        val result = ExportConverter.exportToStandaloneHtml(
            title = title,
            bodyHtml = bodyHtml,
            fontFamily = "serif",
            fontSize = 20
        )

        assertTrue(result.contains("<!DOCTYPE html>"))
        assertTrue(result.contains("<title>$title</title>"))
        assertTrue(result.contains("writing-mode: vertical-rl;"))
        assertTrue(result.contains("font-family: serif,"))
        assertTrue(result.contains("<h1>テスト小説</h1>"))
    }
}

