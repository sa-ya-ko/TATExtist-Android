package com.tatextist.data

import java.util.UUID

/**
 * メモ1件を保持するデータ構造
 *
 * @property id メモの一意な識別番号（UUID）
 * @property title メモのタイトル
 * @property content メモの本文（プレーンテキスト / 青空文庫形式）
 * @property htmlContent 縦書きエディタの装飾状態（HTML）
 * @property isPinned ピン留めされているかどうか（trueの場合は一覧の最上部に固定）
 * @property isArchived アーカイブされているかどうか（trueの場合はメイン一覧から除外）
 * @property updatedAt 最終更新日時（エポックミリ秒）
 */
data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val htmlContent: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
