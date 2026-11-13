package com.tatextist.config

object AppConfig {
    /**
     * 有料版（Pro版）機能の有効化フラグ
     * - false: 無料版（Google Drive連携、HTML出力、PDF印刷をタップ時にPro案内ダイアログを表示）
     * - true : 有料版 / 開発・検証用（すべてのPro機能が実際に動作）
     */
    const val IS_PRO_VERSION: Boolean = true
}

