package com.elara.app.ui.conversation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.MessageEntity
import com.elara.app.data.local.entity.ProfileEntity
import com.elara.app.data.local.entity.SessionEntity
import com.elara.app.data.remote.ChatRequest
import com.elara.app.data.remote.OpenRouterClient
import com.elara.app.domain.model.ConversationTurn
import com.elara.app.domain.model.World
import com.elara.app.domain.usecase.BuildPromptUseCase
import com.elara.app.domain.usecase.CalibrateLanguageLevelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ConversationUiState(
    val messages: List<ConversationTurn> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val estimatedLevel: Float = 0.2f,
    val currentWorld: World = World.MODERN,
    val escalationLevel: Int = 0,
    val currentSessionId: Long? = null
)

class ConversationViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.sessionDao()

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState

    private val buildPrompt = BuildPromptUseCase()
    private val calibrate = CalibrateLanguageLevelUseCase()
    private val history = mutableListOf<ConversationTurn>()

    init {
        viewModelScope.launch { loadProfile() }
    }

    private suspend fun loadProfile() {
        val profile = dao.getProfile() ?: ProfileEntity().also { dao.upsertProfile(it) }
        val world = try { World.valueOf(profile.currentWorld) } catch (e: Exception) { World.MODERN }

        // Load last session messages if exists
        profile.currentSessionId?.let { sessionId ->
            val msgs = dao.getMessages(sessionId)
            msgs.forEach { history.add(ConversationTurn(it.role, it.content, it.timestamp)) }
        }

        _uiState.value = _uiState.value.copy(
            currentWorld = world,
            escalationLevel = profile.escalationLevel,
            estimatedLevel = profile.estimatedLevel,
            currentSessionId = profile.currentSessionId,
            messages = history.toList()
        )
    }

    fun setWorld(world: World) {
        viewModelScope.launch {
            val isDark = world == World.SUBMISSIVE || world == World.DOMINATRIX || world == World.YANDERE
            val startEscalation = if (isDark) 9 else 0
            val sessionId = dao.insertSession(SessionEntity(world = world.name))
            val profile = dao.getProfile() ?: ProfileEntity()
            dao.upsertProfile(profile.copy(
                currentWorld = world.name,
                currentSessionId = sessionId,
                escalationLevel = startEscalation
            ))
            history.clear()
            _uiState.value = _uiState.value.copy(
                currentWorld = world,
                currentSessionId = sessionId,
                messages = emptyList(),
                escalationLevel = startEscalation
            )
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        val userTurn = ConversationTurn("user", userInput)
        history.add(userTurn)

        val newLevel = calibrate.calibrate(userInput, _uiState.value.estimatedLevel)

        _uiState.value = _uiState.value.copy(
            messages = history.toList(),
            isLoading = true,
            estimatedLevel = newLevel,
            error = null
        )

        viewModelScope.launch {
            // Ensure session exists
            val sessionId = ensureSession()

            // Persist user message
            dao.insertMessage(MessageEntity(sessionId = sessionId, role = "user", content = userInput))

            try {
                val profile = dao.getProfile() ?: ProfileEntity()
                val messages = buildPrompt.build(
                    history = history.dropLast(1),
                    userInput = userInput,
                    cameraContext = null,
                    world = _uiState.value.currentWorld,
                    estimatedLevel = newLevel,
                    escalationLevel = _uiState.value.escalationLevel,
                    lorebookEntries = profile.lorebookEntries
                )

                val response = OpenRouterClient.api.chat(
                    auth = OpenRouterClient.authHeader,
                    request = ChatRequest(messages = messages)
                )

                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "..."

                val assistantTurn = ConversationTurn("assistant", reply)
                history.add(assistantTurn)

                // Persist assistant message
                dao.insertMessage(MessageEntity(sessionId = sessionId, role = "assistant", content = reply))

                // Update escalation based on content
                val newEscalation = updateEscalation(reply, _uiState.value.escalationLevel)

                // Save profile state
                dao.upsertProfile(
                    (dao.getProfile() ?: ProfileEntity()).copy(
                        estimatedLevel = newLevel,
                        escalationLevel = newEscalation,
                        currentSessionId = sessionId
                    )
                )

                _uiState.value = _uiState.value.copy(
                    messages = history.toList(),
                    isLoading = false,
                    escalationLevel = newEscalation
                )

            } catch (e: Exception) {
                history.removeLastOrNull()
                _uiState.value = _uiState.value.copy(
                    messages = history.toList(),
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private suspend fun ensureSession(): Long {
        val existing = _uiState.value.currentSessionId
        if (existing != null) return existing
        val id = dao.insertSession(SessionEntity(world = _uiState.value.currentWorld.name))
        _uiState.value = _uiState.value.copy(currentSessionId = id)
        dao.upsertProfile((dao.getProfile() ?: ProfileEntity()).copy(currentSessionId = id))
        return id
    }

    private fun updateEscalation(reply: String, current: Int): Int {
        val world = _uiState.value.currentWorld
        val isDark = world == World.SUBMISSIVE || world == World.DOMINATRIX || world == World.YANDERE
        if (isDark) return 9

        val lower = reply.lowercase()
        val hasExplicit = listOf("sex", "naked", "kiss", "touch", "body", "bed", "fuck", "desire")
            .any { lower.contains(it) }
        return if (hasExplicit && current < 9) (current + 1) else current
    }

    fun addLorebookEntry(entry: String) {
        viewModelScope.launch {
            val profile = dao.getProfile() ?: ProfileEntity()
            val updated = profile.lorebookEntries.trimEnd() + "\n$entry"
            dao.upsertProfile(profile.copy(lorebookEntries = updated))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val world = _uiState.value.currentWorld
            val isDark = world == World.SUBMISSIVE || world == World.DOMINATRIX || world == World.YANDERE
            val startEsc = if (isDark) 9 else 0
            val sessionId = dao.insertSession(SessionEntity(world = world.name))
            dao.upsertProfile((dao.getProfile() ?: ProfileEntity()).copy(
                currentSessionId = sessionId,
                escalationLevel = startEsc
            ))
            history.clear()
            _uiState.value = _uiState.value.copy(
                messages = emptyList(),
                currentSessionId = sessionId,
                escalationLevel = startEsc
            )
        }
    }
}
