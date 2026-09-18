package com.example.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DriveBackupManager(private val context: Context) {
    private val client = OkHttpClient()

    suspend fun backupEntry(account: GoogleSignInAccount, entry: JournalEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val email = account.email ?: return@withContext false
            val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
            val token = GoogleAuthUtil.getToken(context, account.account!!, scope)

            // Metadata part
            val metadataJson = """
                {
                    "name": "Journal_${entry.timestamp}.txt",
                    "mimeType": "text/plain"
                }
            """.trimIndent()
            
            // Content part
            val textContent = """
                Date: ${entry.timestamp}
                Mood: ${entry.mood}
                Tags: ${entry.tags}
                
                ${entry.text}
            """.trimIndent()

            val requestBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaTypeOrNull()!!)
                .addPart(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull()))
                .addPart(textContent.toRequestBody("text/plain".toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer ${token}")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("DriveBackup", "Response code: ${response.code}, body: $responseBody")
            
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DriveBackup", "Failed to backup", e)
            false
        }
    }
}
