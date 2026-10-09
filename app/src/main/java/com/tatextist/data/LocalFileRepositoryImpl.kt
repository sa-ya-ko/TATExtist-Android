package com.tatextist.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LocalFileRepositoryImpl(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalFileRepository {

    override suspend fun saveText(uri: Uri, content: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val outputStream = contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("Failed to open output stream for URI: $uri")
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        }
    }

    override suspend fun readText(uri: Uri): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IOException("Failed to open input stream for URI: $uri")
            inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        }
    }
}
