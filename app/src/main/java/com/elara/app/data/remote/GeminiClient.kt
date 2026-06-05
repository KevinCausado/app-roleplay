package com.elara.app.data.remote

import android.graphics.Bitmap
import com.elara.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String get() = BuildConfig.GEMINI_API_KEY
    private val model = "gemini-2.5-flash"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    suspend fun describeImage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val resized = resizeBitmap(bitmap, 768)
        val base64 = bitmapToBase64(resized)

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Describe this image in simple English for an A1-A2 English learner. " +
                                "Name the objects, people, colors, location, and what is happening. " +
                                "Use short sentences. 3-5 sentences maximum. " +
                                "Example: 'I see a kitchen. There is a table. On the table there is a cup of coffee. The light is bright.' " +
                                "Do NOT add commentary, just describe what you see.")
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64)
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty Gemini response")

        if (!response.isSuccessful) {
            throw Exception("Gemini error ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val text = json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .optString("text", "")

        if (text.isBlank()) throw Exception("Gemini returned empty description")
        text.trim()
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = maxOf(width, height).toFloat() / maxSize
        if (ratio <= 1f) return bitmap
        val newW = (width / ratio).toInt()
        val newH = (height / ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        return android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
    }
}
