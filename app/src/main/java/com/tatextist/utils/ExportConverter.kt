package com.tatextist.utils

/**
 * エクスポート保存形式
 */
enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    TXT("txt", "text/plain", "テキスト (.txt)"),
    HTML("html", "text/html", "縦書きHTML (.html)"),
    MARKDOWN("md", "text/markdown", "Markdown (.md)")
}

/**
 * エディタのHTMLコンテンツを外部フォーマット（Markdown、スタンドアロンHTML）へエクスポートするためのコンバータ
 */
object ExportConverter {

    /**
     * エディタのHTML構造を標準的なMarkdown（.md）形式に変換する
     */
    fun convertToMarkdown(html: String): String {
        var result = html

        // 1. 見出しタグの変換 (h1 -> #, h2 -> ##, h3 -> ###)
        result = Regex("""<h1[^>]*>\s*(.*?)\s*</h1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .replace(result) { "# ${it.groupValues[1]}\n\n" }
        result = Regex("""<h2[^>]*>\s*(.*?)\s*</h2>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .replace(result) { "## ${it.groupValues[1]}\n\n" }
        result = Regex("""<h3[^>]*>\s*(.*?)\s*</h3>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .replace(result) { "### ${it.groupValues[1]}\n\n" }
        result = Regex("""<h4[^>]*>\s*(.*?)\s*</h4>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .replace(result) { "#### ${it.groupValues[1]}\n\n" }

        // 2. 箇条書き行の変換 (<div class="bullet-line">・?(.*?)</div> -> - $1\n)
        result = Regex("""<div[^>]*class=["'][^"']*bullet-line[^"']*["'][^>]*>\s*・?\s*(.*?)\s*</div>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .replace(result) { "- ${it.groupValues[1]}\n" }

        // 3. インライン装飾の変換
        // 太字 <b>, <strong> -> **テキスト**
        result = Regex("""<(?:b|strong)[^>]*>(.*?)</(?:b|strong)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .replace(result) { "**${it.groupValues[1]}**" }

        // 斜体 <i>, <em> -> *テキスト*
        result = Regex("""<(?:i|em)[^>]*>(.*?)</(?:i|em)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .replace(result) { "*${it.groupValues[1]}*" }

        // 4. 空のブロック <div><br></div> を改行に変換
        result = Regex("""<(?:div|p)[^>]*>\s*<br\s*/?>\s*</(?:div|p)>""", RegexOption.IGNORE_CASE)
            .replace(result, "\n")

        // 5. 通常ブロック要素の開始と終了を改行に変換
        result = Regex("""</div>|</p>|<br\s*/?>""", RegexOption.IGNORE_CASE)
            .replace(result, "\n")
        result = Regex("""<(?:div|p)[^>]*>""", RegexOption.IGNORE_CASE)
            .replace(result, "")

        // 6. マークダウンで許可するタグ（ruby, rt, u, font, span）以外の残存HTMLタグを安全に削除
        result = Regex("""<(?!/?(?:ruby|rt|u|span|font)\b)[^>]+>""", RegexOption.IGNORE_CASE)
            .replace(result, "")

        // 7. 特殊文字のデコードとゼロ幅スペース除去
        result = result
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("\u200B", "")

        return result
    }

    /**
     * ブラウザでそのまま開くだけで美しい縦書き表示ができる、単一のスタンドアロンHTML（.html）を生成する
     */
    fun exportToStandaloneHtml(
        title: String,
        bodyHtml: String,
        fontFamily: String = "serif",
        fontSize: Int = 20,
        backgroundColor: String = "#FAF8F5",
        textColor: String = "#212121"
    ): String {
        val displayTitle = if (title.isNotBlank()) title else "無題のドキュメント"
        val fontCss = when (fontFamily.lowercase()) {
            "sans-serif", "sans_serif" -> "sans-serif, 'Noto Sans JP', 'Hiragino Kaku Gothic ProN', Meiryo"
            else -> "serif, 'Noto Serif JP', 'Hiragino Mincho ProN', 'Yu Mincho'"
        }

        return """
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$displayTitle</title>
    <style>
        * {
            box-sizing: border-box;
        }
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            background-color: $backgroundColor;
            color: $textColor;
            font-family: $fontCss;
            overflow-x: auto;
            overflow-y: hidden;
        }
        .container {
            display: flex;
            justify-content: flex-start;
            align-items: stretch;
            height: 100vh;
            padding: 40px 60px;
        }
        .tategaki-content {
            writing-mode: vertical-rl;
            -webkit-writing-mode: vertical-rl;
            -ms-writing-mode: vertical-rl;
            font-size: ${fontSize}px;
            line-height: 2.0;
            letter-spacing: 0.05em;
            word-break: break-all;
            white-space: pre-wrap;
            height: 100%;
        }
        .tategaki-content h1, 
        .tategaki-content h2, 
        .tategaki-content h3, 
        .tategaki-content h4 {
            margin: 0;
            padding: 0;
            font-weight: bold;
            display: block;
        }
        .tategaki-content h1 { font-size: 1.75em; line-height: 1.6; }
        .tategaki-content h2 { font-size: 1.4em; line-height: 1.6; }
        .tategaki-content h3 { font-size: 1.2em; line-height: 1.7; }
        .tategaki-content h4 { font-size: 1.1em; line-height: 1.8; }
        .tategaki-content .bullet-line {
            padding-top: 2.5em;
            text-indent: -1em;
        }
        .tategaki-content ruby rt {
            font-size: 0.5em;
            letter-spacing: 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <article class="tategaki-content">
$bodyHtml
        </article>
    </div>
</body>
</html>
        """.trimIndent()
    }
}

