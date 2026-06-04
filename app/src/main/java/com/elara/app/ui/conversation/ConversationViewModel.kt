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
import com.elara.app.domain.model.Personality
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
    val currentPersonality: Personality = Personality.FRIENDLY,
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
        var profile = dao.getProfile() ?: ProfileEntity().also { dao.upsertProfile(it) }
        var world = try { World.valueOf(profile.currentWorld) } catch (e: Exception) { World.MODERN }
        var personality = try { Personality.valueOf(profile.currentPersonality) } catch (e: Exception) { Personality.FRIENDLY }

        // Migration: old dark worlds are now personalities
        if (profile.currentWorld == "SUBMISSIVE" || profile.currentWorld == "DOMINATRIX" || profile.currentWorld == "YANDERE") {
            personality = try { Personality.valueOf(profile.currentWorld) } catch (e: Exception) { Personality.FRIENDLY }
            world = World.MODERN
            profile = profile.copy(currentWorld = "MODERN", currentPersonality = personality.name)
            dao.upsertProfile(profile)
        }

        profile.currentSessionId?.let { sessionId ->
            val msgs = dao.getMessages(sessionId)
            msgs.forEach { history.add(ConversationTurn(it.role, it.content, it.timestamp)) }
        }

        _uiState.value = _uiState.value.copy(
            currentWorld = world,
            currentPersonality = personality,
            escalationLevel = profile.escalationLevel,
            estimatedLevel = profile.estimatedLevel,
            currentSessionId = profile.currentSessionId,
            messages = history.toList()
        )
    }

    fun setWorld(world: World) {
        viewModelScope.launch {
            val personality = _uiState.value.currentPersonality
            val startEsc = if (personality != Personality.FRIENDLY) 9 else 0
            val sessionId = dao.insertSession(SessionEntity(world = world.name))
            val profile = dao.getProfile() ?: ProfileEntity()
            dao.upsertProfile(profile.copy(
                currentWorld = world.name,
                currentSessionId = sessionId,
                escalationLevel = startEsc
            ))
            history.clear()
            _uiState.value = _uiState.value.copy(
                currentWorld = world,
                currentSessionId = sessionId,
                messages = emptyList(),
                escalationLevel = startEsc
            )
        }
    }

    fun setPersonality(personality: Personality) {
        viewModelScope.launch {
            val startEsc = if (personality != Personality.FRIENDLY) 9 else 0
            val profile = dao.getProfile() ?: ProfileEntity()
            dao.upsertProfile(profile.copy(currentPersonality = personality.name, escalationLevel = startEsc))
            _uiState.value = _uiState.value.copy(
                currentPersonality = personality,
                escalationLevel = startEsc
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
            val sessionId = ensureSession()
            dao.insertMessage(MessageEntity(sessionId = sessionId, role = "user", content = userInput))

            try {
                val profile = dao.getProfile() ?: ProfileEntity()
                val messages = buildPrompt.build(
                    history = history.dropLast(1),
                    userInput = userInput,
                    cameraContext = null,
                    world = _uiState.value.currentWorld,
                    personality = _uiState.value.currentPersonality,
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

                dao.insertMessage(MessageEntity(sessionId = sessionId, role = "assistant", content = reply))

                val newEscalation = updateEscalation(reply, _uiState.value.escalationLevel)

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
        if (_uiState.value.currentPersonality != Personality.FRIENDLY) return 9
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
            val isDark = _uiState.value.currentPersonality != Personality.FRIENDLY
            val startEsc = if (isDark) 9 else 0
            val sessionId = dao.insertSession(SessionEntity(world = _uiState.value.currentWorld.name))
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
