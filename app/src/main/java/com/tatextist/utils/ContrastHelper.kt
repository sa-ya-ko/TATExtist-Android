package com.tatextist.utils

/**
 * コントラストレベル（標準 / 中 / 高）
 */
enum class ContrastLevel {
    STANDARD,
    MEDIUM,
    HIGH
}

/**
 * Android 14+ (API 34+) のOSコントラスト設定を検知・判定するヘルパー
 */
object ContrastHelper {

    /**
     * Android 14+ の UiModeManager.contrast 値（0.0f〜1.0f）から ContrastLevel を判定する
     */
    fun fromContrastValue(contrastValue: Float): ContrastLevel {
        return when {
            contrastValue >= 0.66f -> ContrastLevel.HIGH
            contrastValue >= 0.33f -> ContrastLevel.MEDIUM
            else -> ContrastLevel.STANDARD
        }
    }

    /**
     * 現在の端末環境（Context）からコントラストレベルを取得する
     * - Android 14 (API 34) 以降: UiModeManager.contrast を参照
     * - Android 13 以前: 常に STANDARD を返す
     */
    fun getContrastLevel(context: android.content.Context): ContrastLevel {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val uiModeManager = context.getSystemService(android.app.UiModeManager::class.java)
            val contrast = uiModeManager?.contrast ?: 0.0f
            fromContrastValue(contrast)
        } else {
            ContrastLevel.STANDARD
        }
    }
}

