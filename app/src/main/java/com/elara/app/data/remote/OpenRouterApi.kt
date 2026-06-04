package com.elara.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "deepseek/deepseek-v4-flash",
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 150,
    val temperature: Float = 0.85f,
    val stream: Boolean = false
)

data class ChatChoice(
    val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Header("HTTP-Referer") referer: String = "https://elara-app.local",
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
