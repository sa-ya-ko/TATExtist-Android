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
        // WebViewが属性や改行を勝手に追加した場合でも確実にパースできるよう強化
        val rubyRegex = Regex(
            """<ruby[^>]*>\s*(.*?)\s*<rt[^>]*>\s*(.*?)\s*</rt>\s*</ruby>""", 
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        var result = rubyRegex.replace(html) { matchResult ->
            val base = matchResult.groupValues[1]
            // フリガナ部分に入り込んだ不要なタグ（<b>や<i>など）を全て剥がす
            val ruby = matchResult.groupValues[2].replace(Regex("""<[^>]+>"""), "")
            "|$base《$ruby》"
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
            .replace("\u200B", "")

        return result
    }
}
