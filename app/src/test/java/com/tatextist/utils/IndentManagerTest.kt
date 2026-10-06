package com.tatextist.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IndentManagerTest {

    @Test
    fun test_applyIndent_with_default_1_width() {
        val input = "吾輩は猫である"
        val expected = "　吾輩は猫である"
        val actual = IndentManager.applyIndent(input, width = 1)
        assertEquals(expected, actual)
    }

    @Test
    fun test_applyIndent_with_custom_2_width() {
        val input = "吾輩は猫である"
        val expected = "　　吾輩は猫である"
        val actual = IndentManager.applyIndent(input, width = 2)
        assertEquals(expected, actual)
    }

    @Test
    fun test_applyIndent_multiple_lines() {
        val input = "春はあけぼの\n夏は夜"
        val expected = "　春はあけぼの\n　夏は夜"
        val actual = IndentManager.applyIndent(input, width = 1)
        assertEquals(expected, actual)
    }

    @Test
    fun test_removeIndent_with_1_width() {
        val input = "　吾輩は猫である"
        val expected = "吾輩は猫である"
        val actual = IndentManager.removeIndent(input, width = 1)
        assertEquals(expected, actual)
    }

    @Test
    fun test_removeIndent_with_2_width_from_3_spaces() {
        val input = "　　　吾輩は猫である"
        val expected = "　吾輩は猫である"
        val actual = IndentManager.removeIndent(input, width = 2)
        assertEquals(expected, actual)
    }

    @Test
    fun test_removeIndent_when_no_leading_space() {
        val input = "吾輩は猫である"
        val expected = "吾輩は猫である"
        val actual = IndentManager.removeIndent(input, width = 2)
        assertEquals(expected, actual)
    }

    @Test
    fun test_generateIndentSpaces_with_half_width_and_mixed() {
        // 0.0 -> ""
        assertEquals("", IndentManager.generateIndentSpaces(0.0f))
        // 0.5 -> 半角スペース1個
        assertEquals(" ", IndentManager.generateIndentSpaces(0.5f))
        // 1.0 -> 全角スペース1個
        assertEquals("　", IndentManager.generateIndentSpaces(1.0f))
        // 1.5 -> 全角スペース1個 + 半角スペース1個
        assertEquals("　 ", IndentManager.generateIndentSpaces(1.5f))
        // 2.5 -> 全角スペース2個 + 半角スペース1個
        assertEquals("　　 ", IndentManager.generateIndentSpaces(2.5f))
    }

    @Test
    fun test_applyIndent_with_float_width() {
        val input = "吾輩は猫である"
        // 0.5字インデント
        assertEquals(" 吾輩は猫である", IndentManager.applyIndent(input, width = 0.5f))
        // 1.5字インデント
        assertEquals("　 吾輩は猫である", IndentManager.applyIndent(input, width = 1.5f))
    }

    @Test
    fun test_toggleBullet_always_applies_1_5_indent() {
        val input = "項目その一"
        // 箇条書き付与: 常に1.5文字インデント（全角1文字 + 半角1文字）+ 「・」
        val bulleted = IndentManager.toggleBullet(input)
        assertEquals("　 ・項目その一", bulleted)

        // 箇条書き解除（再度toggleで元に戻る）
        val unbulleted = IndentManager.toggleBullet(bulleted)
        assertEquals("項目その一", unbulleted)
    }

    @Test
    fun test_toggleBullet_with_base_indent_adds_1_5_characters() {
        val input = "項目その一"
        // 本文インデントが1.5文字の場合 -> 1.5 + 1.5 = 3.0文字インデント（全角3文字） + 「・」
        val bulleted = IndentManager.toggleBullet(input, baseIndentWidth = 1.5f)
        assertEquals("　　　・項目その一", bulleted)

        // 解除時は元のテキストに戻る
        val unbulleted = IndentManager.toggleBullet(bulleted, baseIndentWidth = 1.5f)
        assertEquals("項目その一", unbulleted)
    }

    @Test
    fun test_toggleBullet_multiple_lines() {
        val input = "りんご\nみかん"
        val bulleted = IndentManager.toggleBullet(input)
        assertEquals("　 ・りんご\n　 ・みかん", bulleted)

        val unbulleted = IndentManager.toggleBullet(bulleted)
        assertEquals("りんご\nみかん", unbulleted)
    }
}



