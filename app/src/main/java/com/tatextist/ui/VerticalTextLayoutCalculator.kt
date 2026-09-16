package com.tatextist.ui

// 1文字の計算結果を保存するための箱（データクラス）
data class CharLayoutData(
    val char: Char,
    val x: Float,
    val y: Float,
    val index: Int
)

object VerticalTextLayoutCalculator {
    fun calculate(
        text: String,
        maxWidth: Float,
        maxHeight: Float,
        charHeight: Float,
        lineHeight: Float
    ): List<CharLayoutData> {
        val resultList = mutableListOf<CharLayoutData>()
        // 右端からスタートする（文字の幅分だけ左にズラして、文字が画面外に出ないようにする）
        var currentX = maxWidth - charHeight
        var currentY = 0f

        text.forEachIndexed { index, char ->
            // もしこの文字を描くと画用紙の下にはみ出るなら、改行（左へズラして一番上に戻る）
            // ※「>」ではなく「>」を使うことで、ピッタリ収まる場合は改行しません。ただし浮動小数点の誤差を考慮します。
            if (currentY > 0f && currentY + charHeight > maxHeight) {
                currentX -= lineHeight
                currentY = 0f
            }

            // 今の座標を記録する
            resultList.add(CharLayoutData(char, currentX, currentY, index))

            // 次の文字のために下にズラす
            currentY += charHeight
        }

        return resultList
    }
}
