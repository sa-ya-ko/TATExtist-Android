package com.tatextist.utils

object IndentManager {
    /**
     * 指定された幅（width文字分、0.5字刻み対応）のスペース文字列を生成する
     * 例: 0.5f -> " ", 1.0f -> "　", 1.5f -> "　 "
     */
    fun generateIndentSpaces(width: Float): String {
        if (width <= 0.0f) return ""
        val fullCount = width.toInt()
        val hasHalf = (width - fullCount) >= 0.4f
        val fullSpaces = "　".repeat(fullCount)
        val halfSpace = if (hasHalf) " " else ""
        return fullSpaces + halfSpace
    }

    /**
     * 各行の先頭に、指定された幅（width文字分）のスペースを挿入する（字下げ）
     */
    fun applyIndent(text: String, width: Float): String {
        if (width <= 0.0f) return text
        val indentSpaces = generateIndentSpaces(width)
        return text.lines().joinToString("\n") { line ->
            indentSpaces + line
        }
    }

    fun applyIndent(text: String, width: Int): String {
        return applyIndent(text, width.toFloat())
    }

    /**
     * 各行の先頭から、指定された幅（最大width文字分）のスペースを削除する（字上げ）
     */
    fun removeIndent(text: String, width: Int = 1): String {
        if (width < 1) return text
        return text.lines().joinToString("\n") { line ->
            var count = 0
            var idx = 0
            while (idx < line.length && count < width) {
                if (line[idx] == '　' || line[idx] == ' ') {
                    count++
                    idx++
                } else {
                    break
                }
            }
            line.substring(idx)
        }
    }

    /**
     * 箇条書きの標準インデント幅（本文に対して常に1.5文字インデント: 全角1文字 + 半角1文字）
     */
    const val BULLET_INDENT_WIDTH = 1.5f

    /**
     * 行が箇条書き行であるかを判定（先頭に空白があっても、その直後に「・」があるかどうか）
     */
    fun isBulletLine(line: String): Boolean {
        val trimmed = line.trimStart('　', ' ')
        return trimmed.startsWith("・")
    }

    /**
     * 各行の箇条書き（・）をトグル（付与／解除）する。
     * - baseIndentWidth: 本文の基本インデント幅（設定された字下げ幅）
     * - bulletIndentOffset: 箇条書きによる追加インデント幅（デフォルトは常に1.5文字）
     * 本文インデントに対して常に1.5文字分が追加で字下げされる。
     */
    fun toggleBullet(
        text: String,
        baseIndentWidth: Float = 0.0f,
        bulletIndentOffset: Float = BULLET_INDENT_WIDTH
    ): String {
        val totalBulletIndent = baseIndentWidth + bulletIndentOffset
        val bulletPrefix = generateIndentSpaces(totalBulletIndent) + "・"
        return text.lines().joinToString("\n") { line ->
            if (isBulletLine(line)) {
                // 箇条書き解除: 先頭のスペースと「・」を取り除く
                val trimmed = line.trimStart('　', ' ')
                if (trimmed.startsWith("・")) {
                    trimmed.substring(1)
                } else {
                    line
                }
            } else {
                // 箇条書き付与: (本文インデント + 1.5文字) + 「・」を行の先頭に付与
                bulletPrefix + line
            }
        }
    }

}



