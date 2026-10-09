package com.tatextist.data

import android.net.Uri

/**
 * 端末ローカルストレージとのファイル入出力を担うリポジトリインターフェース
 */
interface LocalFileRepository {
    /**
     * 指定されたURIにテキストコンテンツをUTF-8で保存する
     */
    suspend fun saveText(uri: Uri, content: String): Result<Unit>

    /**
     * 指定されたURIからテキストコンテンツをUTF-8で読み込む
     */
    suspend fun readText(uri: Uri): Result<String>
}
