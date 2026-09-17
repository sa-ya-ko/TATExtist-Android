package com.tatextist.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveRepositoryImpl(private val context: Context) : GoogleDriveRepository {

    private val scopes = listOf(DriveScopes.DRIVE_FILE)
    
    // Google SignIn Options requesting Drive scopes
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .build()

    private val googleSignInClient = GoogleSignIn.getClient(context, gso)

    override fun getSignInIntent(): android.content.Intent {
        return googleSignInClient.signInIntent
    }

    override suspend fun signIn(): Result<Unit> {
        // Implementation requires Activity result, this will be handled via UI.
        // For the repository layer, we assume signIn is successful if we have an account.
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("サインインしていません。画面からのサインインが必要です。"))
        }
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Tatextist").build()
    }

    override suspend fun saveToDrive(content: String, filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext Result.failure(Exception("Google Driveに接続できません"))

            // 1. 同名のファイルが既に存在するか検索
            val query = "name = '$filename' and trashed = false"
            val fileList = driveService.files().list().setQ(query).setSpaces("drive").execute()
            val existingFile = fileList.files.firstOrNull()

            // 2. ファイルの内容を準備
            val fileContent = com.google.api.client.http.ByteArrayContent.fromString("text/plain", content)

            if (existingFile != null) {
                // 上書き更新
                driveService.files().update(existingFile.id, null, fileContent).execute()
            } else {
                // 新規作成
                val metadata = com.google.api.services.drive.model.File().apply {
                    name = filename
                    mimeType = "text/plain"
                }
                driveService.files().create(metadata, fileContent).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun loadFromDrive(filename: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext Result.failure(Exception("Google Driveに接続できません"))

            val query = "name = '$filename' and trashed = false"
            val fileList = driveService.files().list().setQ(query).setSpaces("drive").execute()
            val existingFile = fileList.files.firstOrNull() 
                ?: return@withContext Result.failure(Exception("ファイル「$filename」が見つかりませんでした"))

            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
            
            Result.success(outputStream.toString("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
