package com.elara.app.domain.usecase

import com.elara.app.data.remote.ChatMessage
import com.elara.app.data.remote.ChatRequest
import com.elara.app.data.remote.OpenRouterClient
import com.elara.app.data.local.entity.ErrorEntity

class GenerateExerciseUseCase {

    suspend fun generate(errors: List<ErrorEntity>): String {
        if (errors.isEmpty()) return "No errors saved yet. Keep practicing and mistakes will appear here."

        val errorList = errors.takeLast(5).joinToString("\n") {
            "Original: \"${it.original}\" → Corrected: \"${it.corrected}\""
        }

        val prompt = listOf(
            ChatMessage("system", """You are an English tutor for an A2-level learner. 
Based on the grammar mistakes provided, generate 3 fill-in-the-blank exercises.
For each exercise, show: the sentence with a blank [_____], and 3 multiple choice options (A, B, C).
Mark the correct answer with ✓.

Format:
1. "She [_____] to the store yesterday." 
   A) go  B) went ✓  C) gone

Make exercises that specifically target the grammar patterns the user struggles with.
Keep sentences short and relevant to daily life. Reply ONLY with the exercises, no intro."""),
            ChatMessage("user", "Here are my recent mistakes:\n$errorList")
        )

        return try {
            val response = OpenRouterClient.api.chat(
                auth = OpenRouterClient.authHeader,
                request = ChatRequest(messages = prompt, maxTokens = 300)
            )
            response.body()?.choices?.firstOrNull()?.message?.content?.trim() ?: "Could not generate exercises."
        } catch (e: Exception) {
            "Failed to generate exercises: ${e.message}"
        }
    }
}
