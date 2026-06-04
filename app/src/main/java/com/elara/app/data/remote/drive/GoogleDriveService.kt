package com.elara.app.data.remote.drive

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveService(private val context: Context) {

    private var driveApi: Drive? = null
    private var folderId: String? = null

    companion object {
        private const val PREFS_NAME = "elara_drive"
        private const val KEY_ACCOUNT = "connected_account"
    }

    init {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNT, null)
        if (saved != null) buildDriveApi(saved)
    }

    private fun buildDriveApi(accountName: String) {
        try {
            val credential = GoogleAccountCredential
                .usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
                .also { it.selectedAccountName = accountName }
            val accountExists = credential.googleAccountManager.accounts.any { it.name == accountName }
            if (!accountExists) { driveApi = null; return }
            driveApi = Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("ElaraApp").build()
        } catch (e: Exception) {
            driveApi = null
        }
    }

    fun setAccount(accountName: String) {
        buildDriveApi(accountName)
        folderId = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCOUNT, accountName).apply()
    }

    fun getSavedAccountName(): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACCOUNT, null)

    fun isAuthenticated(): Boolean = driveApi != null && getSavedAccountName() != null

    private suspend fun getOrCreateFolder(): String = withContext(Dispatchers.IO) {
        folderId?.let { return@withContext it }
        val api = driveApi ?: error("Drive not authenticated")
        try {
            val query = "name = '${DriveConstants.FOLDER_NAME}' and mimeType = '${DriveConstants.MIME_FOLDER}' and trashed = false"
            val result = api.files().list().setQ(query).setSpaces("drive").setFields("files(id,name)").execute()
            if (result.files != null && result.files.isNotEmpty()) {
                result.files.first().id.also { folderId = it }
            } else {
                val folderMeta = File().apply {
                    name = DriveConstants.FOLDER_NAME
                    mimeType = DriveConstants.MIME_FOLDER
                }
                api.files().create(folderMeta).setFields("id").execute().id.also { folderId = it }
            }
        } catch (e: Exception) {
            if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                driveApi = null; folderId = null
            }
            error("Folder error: ${e.message}")
        }
    }

    suspend fun readFile(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val api = driveApi ?: return@withContext null
            val folder = getOrCreateFolder()
            val result = api.files().list()
                .setQ("name = '$fileName' and '$folder' in parents and trashed = false")
                .setSpaces("drive").execute()
            if (result.files.isEmpty()) return@withContext null
            api.files().get(result.files.first().id).executeMediaAsInputStream().bufferedReader().readText()
        } catch (e: Exception) { null }
    }

    suspend fun writeFile(fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val api = driveApi ?: return@withContext false
            val folder = getOrCreateFolder()
            val mediaContent = ByteArrayContent(DriveConstants.MIME_JSON, content.toByteArray())
            val result = api.files().list()
                .setQ("name = '$fileName' and '$folder' in parents and trashed = false")
                .setSpaces("drive").execute()
            if (result.files.isNotEmpty()) {
                api.files().update(result.files.first().id, null, mediaContent).execute()
            } else {
                val fileMeta = File().apply { name = fileName; parents = listOf(folder) }
                api.files().create(fileMeta, mediaContent).setFields("id").execute()
            }
            true
        } catch (e: Exception) { false }
    }
}
