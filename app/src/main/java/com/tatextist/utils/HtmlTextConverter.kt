package com.tatextist.utils

object HtmlTextConverter {
    fun convertToHtml(text: String): String {
        var result = text

        // 0. 見出し注記の復元 (［＃大見出し］...［＃大見出し終わり］ -> <h1>...</h1> 等)
        val h1Regex = Regex("""［＃大見出し］(.*?)［＃大見出し終わり］\r?\n?""")
        result = h1Regex.replace(result) { "<h1>${it.groupValues[1]}</h1>" }

        val h2Regex = Regex("""［＃中見出し］(.*?)［＃中見出し終わり］\r?\n?""")
        result = h2Regex.replace(result) { "<h2>${it.groupValues[1]}</h2>" }

        val h3Regex = Regex("""［＃小見出し］(.*?)［＃小見出し終わり］\r?\n?""")
        result = h3Regex.replace(result) { "<h3>${it.groupValues[1]}</h3>" }

        // 1. ルビの変換 (|漢字《かんじ》 -> <ruby>漢字<rt>かんじ</rt></ruby>)
        val rubyRegex = Regex("""\|(.+?)《(.+?)》""")
        result = rubyRegex.replace(result) { matchResult ->
            val base = matchResult.groupValues[1]
            val ruby = matchResult.groupValues[2]
            "<ruby>$base<rt>$ruby</rt></ruby>"
        }
        
        // 2. 改行の変換 (\n -> <br>)
        result = result.replace("\n", "<br>")
        
        return result
    }

    fun convertFromHtml(html: String): String {
        // 0. エディタ全消去時の残骸タグ (<br>, <div><br></div> 等) の空判定
        // 可視文字やルビが存在しない場合は即座に空文字を返し、誤って文字数1 (\n) と判定されるのを防ぐ
        val stripped = html.replace(Regex("""<[^>]+>"""), "").replace("\u200B", "").trim()
        if (stripped.isEmpty() && !html.contains("<ruby", ignoreCase = true)) {
            return ""
        }

        var result = html

        // 0.1. 見出しタグ (h1, h2, h3, h4) を青空文庫の見出し注記に変換
        val h1Pattern = Regex("""<h1[^>]*>\s*(.*?)\s*</h1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        result = h1Pattern.replace(result) { "［＃大見出し］${it.groupValues[1]}［＃大見出し終わり］\n" }

        val h2Pattern = Regex("""<h2[^>]*>\s*(.*?)\s*</h2>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        result = h2Pattern.replace(result) { "［＃中見出し］${it.groupValues[1]}［＃中見出し終わり］\n" }

        val h3Pattern = Regex("""<h3[^>]*>\s*(.*?)\s*</h3>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        result = h3Pattern.replace(result) { "［＃小見出し］${it.groupValues[1]}［＃小見出し終わり］\n" }

        val h4Pattern = Regex("""<h4[^>]*>\s*(.*?)\s*</h4>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        result = h4Pattern.replace(result) { "［＃小見出し］${it.groupValues[1]}［＃小見出し終わり］\n" }

        // 1. <ruby> を |漢字《かんじ》 に戻す
        // 親文字（漢字）が空の残骸ルビタグは完全に破棄する
        val rubyRegex = Regex(
            """<ruby[^>]*>\s*(.*?)\s*<rt[^>]*>\s*(.*?)\s*</rt>\s*</ruby>""", 
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        result = rubyRegex.replace(result) { matchResult ->
            val rawBase = matchResult.groupValues[1]
            val ruby = matchResult.groupValues[2].replace(Regex("""<[^>]+>"""), "").replace("\u200B", "").trim()

            // 親文字内の許可タグ（u, b, i, span, font）をチェック
            val tagMatch = Regex("""^<([a-zA-Z0-9]+)([^>]*)>(.*?)</\1>$""", RegexOption.DOT_MATCHES_ALL).find(rawBase.trim())
            if (tagMatch != null) {
                val tag = tagMatch.groupValues[1]
                val attrs = tagMatch.groupValues[2]
                val innerBase = tagMatch.groupValues[3].replace("\u200B", "").trim()
                if (innerBase.isEmpty()) {
                    ""
                } else {
                    "<$tag$attrs>|$innerBase《$ruby》</$tag>"
                }
            } else {
                val cleanBase = rawBase.replace(Regex("""<[^>]+>"""), "").replace("\u200B", "").trim()
                if (cleanBase.isEmpty()) {
                    // 親文字が削除された孤立ルビは残骸なので消去
                    ""
                } else {
                    "|$cleanBase《$ruby》"
                }
            }
        }

        // 2. 空のブロック <div><br></div>, <p><br></p> などを1つの改行にまとめる
        result = result.replace(Regex("""<(?:div|p)[^>]*>\s*<br\s*/?>\s*</(?:div|p)>""", RegexOption.IGNORE_CASE), "\n")
        result = result.replace(Regex("""<br\s*/?>\s*</(?:div|p)>""", RegexOption.IGNORE_CASE), "\n")

        // 3. 1行目がタグなしで2行目から<div>が始まる場合（1行目<div>2行目</div>）の改行補正
        // 文字列の途中（タグの終了や改行の直後ではない場所）から突然<div>や<p>が始まる直前に \n を挿入
        result = result.replace(Regex("""(?<=[^>\n\r])\s*<(?:div|p)\b[^>]*>""", RegexOption.IGNORE_CASE), "\n")

        // 4. ブロック要素の終了タグと改行タグを \n に変換
        result = result.replace(Regex("""</div>|</p>|<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")

        // 5. その他のHTMLタグ（太字 <b>, 斜体 <i>, 傍線 <u>, 装飾 <span>, <font> 以外）をすべて削除
        result = result.replace(Regex("""<(?!/?(?:b|i|u|ruby|rt|span|font)\b)[^>]+>""", RegexOption.IGNORE_CASE), "")

        // 7. 特殊文字のデコードとゼロ幅スペースの除去
        result = result.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("\u200B", "")

        // 8. 空白や改行のみ残った場合は空文字として返す
        val finalStripped = result.replace("\n", "").trim()
        if (finalStripped.isEmpty()) {
            return ""
        }

        return result
    }

    /**
     * 文字数カウント・行数計算用の純粋な可視プレーンテキストを抽出する。
     * - ルビ記法 (|漢字《かんじ》) は親文字「漢字」のみを抽出
     * - 残存しているすべてのHTMLタグ (<font>, <span>, <b>, <i> 等) を完全除去
     * - ゼロ幅スペース (\u200B) を完全除去
     */
    fun extractPlainText(text: String): String {
        var result = text

        // 0. 青空文庫の見出し注記を除去して中身のテキストのみを残す
        val headingRegex = Regex("""［＃(?:大見出し|中見出し|小見出し)］(.*?)［＃(?:大見出し|中見出し|小見出し)終わり］""", RegexOption.DOT_MATCHES_ALL)
        result = headingRegex.replace(result) { it.groupValues[1] }

        // 1. ルビ記法 |漢字《かんじ》 から親文字のみ抽出
        val rubyRegex = Regex("""\|(.+?)《.+?》""")
        result = rubyRegex.replace(result) { matchResult ->
            matchResult.groupValues[1]
        }

        // 2. 残存しているHTMLタグ (<font style="...">, <span>, <b> 等) を完全除去
        result = result.replace(Regex("""<[^>]+>"""), "")

        // 3. ゼロ幅スペースの除去
        result = result.replace("\u200B", "")

        return result
    }
}
