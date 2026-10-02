package com.tatextist.utils

/**
 * ルビ情報を保持するデータモデル
 */
data class RubyItem(
    val id: String,
    val parentText: String,
    val rubyText: String,
    val startIndex: Int = 0
)

/**
 * 本文中のルビ抽出、検索、解除を行うユーティリティ
 */
object RubyManager {
    // 青空文庫形式のルビ記法: |親文字《ふりがな》 を抽出する正規表現
    private val rubyRegex = Regex("""\|([^《]+)《([^》]+)》""")

    /**
     * 本文テキストからすべてのルビをリストとして抽出する
     */
    fun extractRubyList(text: String): List<RubyItem> {
        return rubyRegex.findAll(text).mapIndexed { index, matchResult ->
            val parent = matchResult.groupValues[1]
            val ruby = matchResult.groupValues[2]
            RubyItem(
                id = "${index}_${parent}_$ruby",
                parentText = parent,
                rubyText = ruby,
                startIndex = matchResult.range.first
            )
        }.toList()
    }

    /**
     * クエリ（親文字またはふりがな）でリストを部分一致フィルタリングする
     */
    fun filterRubyList(list: List<RubyItem>, query: String): List<RubyItem> {
        if (query.isBlank()) return list
        return list.filter {
            it.parentText.contains(query, ignoreCase = true) ||
            it.rubyText.contains(query, ignoreCase = true)
        }
    }

    /**
     * 指定したルビを本文から解除（|親文字《ルビ》 を 親文字 に置換）する
     */
    fun removeRuby(text: String, item: RubyItem): String {
        val targetPattern = "|${item.parentText}《${item.rubyText}》"
        return text.replaceFirst(targetPattern, item.parentText)
    }
}
