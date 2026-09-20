package com.tatextist.data

import android.content.Intent

data class DriveFileInfo(
    val id: String,
    val name: String
)

interface GoogleDriveRepository {
    /**
     * Provides the Intent needed to launch the Google Sign-In flow.
     */
    fun getSignInIntent(): Intent

    /**
     * Signs the user in and establishes Drive API access.
     */
    suspend fun signIn(): Result<Unit>

    /**
     * Saves the provided content to Google Drive under the given filename.
     */
    suspend fun saveToDrive(content: String, filename: String): Result<Unit>

    /**
     * Loads the content of the given filename from Google Drive.
     */
    suspend fun loadFromDrive(filename: String): Result<String>

    /**
     * Loads the content of the given file ID from Google Drive.
     */
    suspend fun loadFromDriveById(fileId: String): Result<String>

    /**
     * Lists files saved in Google Drive by this app.
     */
    suspend fun listDriveFiles(): Result<List<DriveFileInfo>>

    /**
     * Signs out the user from Google Drive.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Checks if the user is currently signed in.
     */
    fun isSignedIn(): Boolean
}
