package com.elara.app.data.remote

import com.elara.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object OpenRouterClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: OpenRouterApi = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenRouterApi::class.java)

    val authHeader get() = "Bearer ${BuildConfig.OPENROUTER_API_KEY}"
}
