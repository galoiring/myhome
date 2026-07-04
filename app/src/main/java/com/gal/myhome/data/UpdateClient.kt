package com.gal.myhome.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val notes: String,
)

class UpdateClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** manifestUrl points at update.json, served next to app-release.apk. */
    suspend fun checkForUpdate(manifestUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(manifestUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val o = JSONObject(resp.body?.string() ?: return@withContext null)
                UpdateInfo(
                    versionCode = o.getInt("versionCode"),
                    versionName = o.optString("versionName", "?"),
                    url = o.getString("url"),
                    notes = o.optString("notes", ""),
                )
            }
        } catch (_: Exception) { null }
    }

    /** Downloads the APK into [destDir], returning the file on success. */
    suspend fun downloadApk(url: String, destDir: File): File? = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                val file = File(destDir, "update.apk")
                file.outputStream().use { out -> body.byteStream().copyTo(out) }
                file
            }
        } catch (_: IOException) { null }
    }
}
