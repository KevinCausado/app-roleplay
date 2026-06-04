package com.elara.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object TranslatorClient {

    suspend fun translate(word: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(word.trim(), "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=en|es"
            val response = URL(url).readText()
            val json = JSONObject(response)
            json.getJSONObject("responseData").getString("translatedText")
        } catch (e: Exception) {
            "..."
        }
    }
}
