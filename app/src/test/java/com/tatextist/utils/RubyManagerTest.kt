package com.tatextist.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RubyManagerTest {

    @Test
    fun test_extractRubyList_extracts_all_rubies_in_order() {
        val text = "吾輩は|猫《ねこ》である。|名前《なまえ》はまだない。"
        val rubies = RubyManager.extractRubyList(text)

        assertEquals(2, rubies.size)
        assertEquals("猫", rubies[0].parentText)
        assertEquals("ねこ", rubies[0].rubyText)
        assertEquals("名前", rubies[1].parentText)
        assertEquals("なまえ", rubies[1].rubyText)
    }

    @Test
    fun test_extractRubyList_returns_empty_when_no_ruby() {
        val text = "普通の文章です。"
        val rubies = RubyManager.extractRubyList(text)
        assertEquals(0, rubies.size)
    }

    @Test
    fun test_filterRubyList_filters_by_parent_or_reading() {
        val list = listOf(
            RubyItem(id = "1", parentText = "猫", rubyText = "ねこ"),
            RubyItem(id = "2", parentText = "犬", rubyText = "いぬ"),
            RubyItem(id = "3", parentText = "仔猫", rubyText = "こねこ")
        )

        val resultParent = RubyManager.filterRubyList(list, "猫")
        assertEquals(2, resultParent.size)

        val resultRuby = RubyManager.filterRubyList(list, "いぬ")
        assertEquals(1, resultRuby.size)
        assertEquals("犬", resultRuby[0].parentText)
    }

    @Test
    fun test_removeRuby_replaces_target_ruby_with_parent_text() {
        val text = "吾輩は|猫《ねこ》である。|名前《なまえ》はまだない。"
        val target = RubyItem(id = "1", parentText = "猫", rubyText = "ねこ")
        val updated = RubyManager.removeRuby(text, target)

        assertEquals("吾輩は猫である。|名前《なまえ》はまだない。", updated)
    }
}
