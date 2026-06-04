package com.elara.app.domain.usecase

import com.elara.app.data.local.dao.CharacterMemoryDao
import com.elara.app.data.local.entity.CharacterMemoryEntity
import com.elara.app.data.remote.ChatMessage
import com.elara.app.data.remote.OpenRouterApi
import com.elara.app.data.remote.ChatRequest
import com.elara.app.data.remote.OpenRouterClient

class UpdateCharacterMemoryUseCase(
    private val dao: CharacterMemoryDao
) {
    suspend fun update(characterId: String, recentMessages: List<ChatMessage>) {
        if (recentMessages.size < 6) return

        val existing = dao.get(characterId)?.memory ?: ""

        val prompt = buildString {
            append("You are a memory extractor. Read this conversation and extract factual things Kevin revealed about himself.\n\n")
            append("EXISTING MEMORY:\n$existing\n\n")
            append("RECENT CONVERSATION:\n")
            recentMessages.takeLast(10).forEach { msg ->
                if (msg.role == "user") append("Kevin: ${msg.content}\n")
            }
            append("\nOutput a SHORT updated memory list (max 10 bullet points). Facts only. Things like: job, hobbies, family, feelings, goals, daily life. In English. Start each line with -")
        }

        try {
            val response = OpenRouterClient.api.chat(
                auth = OpenRouterClient.authHeader,
                request = ChatRequest(
                    messages = listOf(ChatMessage("user", prompt)),
                    max_tokens = 300
                )
            )
            val newMemory = response.body()?.choices?.firstOrNull()?.message?.content ?: return
            dao.upsert(CharacterMemoryEntity(characterId = characterId, memory = newMemory))
        } catch (e: Exception) {
            // silently fail — memory update is non-critical
        }
    }
}
