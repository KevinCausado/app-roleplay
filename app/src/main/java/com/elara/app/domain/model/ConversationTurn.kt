package com.elara.app.domain.model

data class ConversationTurn(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cameraContext: String? = null,
    val correction: String? = null,
    val phraseSuggestion: String? = null
)
