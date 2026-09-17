package com.tatextist.data

import android.content.Intent

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
}
