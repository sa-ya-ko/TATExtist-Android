package com.tatextist.utils

object HtmlTextConverter {
    fun convertToHtml(text: String): String {
        // 1. ルビの変換 (|漢字《かんじ》 -> <ruby>漢字<rt>かんじ</rt></ruby>)
        val rubyRegex = Regex("""\|(.+?)《(.+?)》""")
        var result = rubyRegex.replace(text) { matchResult ->
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

        // 1. <ruby> を |漢字《かんじ》 に戻す
        // 親文字（漢字）が空の残骸ルビタグは完全に破棄する
        val rubyRegex = Regex(
            """<ruby[^>]*>\s*(.*?)\s*<rt[^>]*>\s*(.*?)\s*</rt>\s*</ruby>""", 
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        var result = rubyRegex.replace(html) { matchResult ->
            val rawBase = matchResult.groupValues[1]
            val cleanBase = rawBase.replace(Regex("""<[^>]+>"""), "").replace("\u200B", "").trim()
            val ruby = matchResult.groupValues[2].replace(Regex("""<[^>]+>"""), "").replace("\u200B", "").trim()
            if (cleanBase.isEmpty()) {
                // 親文字が削除された孤立ルビは残骸なので消去
                ""
            } else {
                "|$cleanBase《$ruby》"
            }
        }

        // 2. 空のブロック <div><br></div> などを1つの改行にまとめる
        result = result.replace(Regex("""<br\s*/?>\s*</div>""", RegexOption.IGNORE_CASE), "\n")

        // 3. ブロック要素の終了タグと改行タグを \n に変換
        result = result.replace(Regex("""</div>|</p>|<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")

        // 4. その他のHTMLタグ（太字 <b>, 斜体 <i>, 装飾 <span>, <font> 以外）をすべて削除
        // 万が一ルビのパースに失敗しても、<ruby>と<rt>を消してテキストを破壊しないよう保護リストに追加
        result = result.replace(Regex("""<(?!/?(?:b|i|ruby|rt|span|font)\b)[^>]+>""", RegexOption.IGNORE_CASE), "")

        // 5. 特殊文字のデコードとゼロ幅スペース（スタイルプレースホルダー用）の除去
        result = result.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
        // 6. 空白や改行のみ残った場合は空文字として返す
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
        // 1. ルビ記法 |漢字《かんじ》 から親文字のみ抽出
        val rubyRegex = Regex("""\|(.+?)《.+?》""")
        var result = rubyRegex.replace(text) { matchResult ->
            matchResult.groupValues[1]
        }

        // 2. 残存しているHTMLタグ (<font style="...">, <span>, <b> 等) を完全除去
        result = result.replace(Regex("""<[^>]+>"""), "")

        // 3. ゼロ幅スペースの除去
        result = result.replace("\u200B", "")

        return result
    }
}
