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
import com.elara.app.data.remote.ChatMessage
import com.elara.app.domain.model.Character
import com.elara.app.domain.model.ConversationTurn
import com.elara.app.domain.model.Personality
import com.elara.app.domain.model.World
import com.elara.app.domain.usecase.BuildPromptUseCase
import com.elara.app.domain.usecase.CalibrateLanguageLevelUseCase
import com.elara.app.domain.usecase.UpdateCharacterMemoryUseCase
import kotlinx.coroutines.async
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
    val currentCharacter: Character = Character.ELARA,
    val escalationLevel: Int = 0,
    val currentSessionId: Long? = null
)

class ConversationViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.sessionDao()
    private val memoryDao = db.characterMemoryDao()

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState

    private val buildPrompt = BuildPromptUseCase()
    private val calibrate = CalibrateLanguageLevelUseCase()
    private val updateMemory = UpdateCharacterMemoryUseCase(memoryDao)
    private val history = mutableListOf<ConversationTurn>()
    private var messagesSinceMemoryUpdate = 0

    init {
        viewModelScope.launch { loadProfile() }
    }

    private suspend fun loadProfile() {
        var profile = dao.getProfile() ?: ProfileEntity().also { dao.upsertProfile(it) }
        var world = try { World.valueOf(profile.currentWorld) } catch (e: Exception) { World.MODERN }
        var personality = try { Personality.valueOf(profile.currentPersonality) } catch (e: Exception) { Personality.FRIENDLY }
        val character = try { Character.valueOf(profile.currentCharacter) } catch (e: Exception) { Character.ELARA }

        if (profile.currentWorld == "SUBMISSIVE" || profile.currentWorld == "DOMINATRIX" || profile.currentWorld == "YANDERE") {
            personality = try { Personality.valueOf(profile.currentWorld) } catch (e: Exception) { Personality.FRIENDLY }
            world = World.MODERN
            profile = profile.copy(currentWorld = "MODERN", currentPersonality = personality.name)
            dao.upsertProfile(profile)
        }

        val session = dao.getSessionByCombo(world.name, personality.name, character.name)
            ?: dao.insertSession(SessionEntity(world = world.name, personality = personality.name, character = character.name)).let {
                dao.getSessionByCombo(world.name, personality.name, character.name)
            }
        session?.let { s ->
            val msgs = dao.getMessages(s.id)
            msgs.forEach { history.add(ConversationTurn(it.role, it.content, it.timestamp)) }
            dao.upsertProfile(profile.copy(
                currentWorld = world.name,
                currentPersonality = personality.name,
                currentCharacter = character.name,
                currentSessionId = s.id,
                escalationLevel = s.escalationLevel
            ))
        }

        _uiState.value = _uiState.value.copy(
            currentWorld = world,
            currentPersonality = personality,
            currentCharacter = character,
            escalationLevel = session?.escalationLevel ?: profile.escalationLevel,
            estimatedLevel = profile.estimatedLevel,
            currentSessionId = session?.id,
            messages = history.toList()
        )
    }

    fun setWorld(world: World) {
        viewModelScope.launch {
            switchSession(world, _uiState.value.currentPersonality, _uiState.value.currentCharacter)
        }
    }

    fun setPersonality(personality: Personality) {
        viewModelScope.launch {
            switchSession(_uiState.value.currentWorld, personality, _uiState.value.currentCharacter)
        }
    }

    fun setCharacter(character: Character) {
        viewModelScope.launch {
            switchSession(_uiState.value.currentWorld, _uiState.value.currentPersonality, character)
        }
    }

    private suspend fun switchSession(world: World, personality: Personality, character: Character) {
        val startEsc = if (personality != Personality.FRIENDLY) 9 else 0
        val existing = dao.getSessionByCombo(world.name, personality.name, character.name)

        if (existing != null) {
            val msgs = dao.getMessages(existing.id)
            history.clear()
            msgs.forEach { history.add(ConversationTurn(it.role, it.content, it.timestamp)) }
            val profile = dao.getProfile() ?: ProfileEntity()
            dao.upsertProfile(profile.copy(
                currentWorld = world.name,
                currentPersonality = personality.name,
                currentCharacter = character.name,
                currentSessionId = existing.id,
                escalationLevel = existing.escalationLevel
            ))
            _uiState.value = _uiState.value.copy(
                currentWorld = world,
                currentPersonality = personality,
                currentCharacter = character,
                currentSessionId = existing.id,
                messages = history.toList(),
                escalationLevel = existing.escalationLevel
            )
        } else {
            val sessionId = dao.insertSession(SessionEntity(
                world = world.name, personality = personality.name, character = character.name
            ))
            val profile = dao.getProfile() ?: ProfileEntity()
            dao.upsertProfile(profile.copy(
                currentWorld = world.name,
                currentPersonality = personality.name,
                currentCharacter = character.name,
                currentSessionId = sessionId,
                escalationLevel = startEsc
            ))
            history.clear()
            _uiState.value = _uiState.value.copy(
                currentWorld = world,
                currentPersonality = personality,
                currentCharacter = character,
                currentSessionId = sessionId,
                messages = emptyList(),
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
                val charId = _uiState.value.currentCharacter.name
                val charMemory = memoryDao.get(charId)?.memory ?: ""
                val messages = buildPrompt.build(
                    history = history.dropLast(1),
                    userInput = userInput,
                    cameraContext = null,
                    world = _uiState.value.currentWorld,
                    personality = _uiState.value.currentPersonality,
                    character = _uiState.value.currentCharacter,
                    estimatedLevel = newLevel,
                    escalationLevel = _uiState.value.escalationLevel,
                    lorebookEntries = profile.lorebookEntries,
                    characterMemory = charMemory
                )

                // Launch grammar check in parallel with main chat call
                val correctionDeferred = if (userInput.trim().split(" ").size >= 3) {
                    async { checkGrammar(userInput) }
                } else null

                val response = OpenRouterClient.api.chat(
                    auth = OpenRouterClient.authHeader,
                    request = ChatRequest(messages = messages)
                )

                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "..."

                val correction = correctionDeferred?.await()

                // Update the user turn with the correction result
                val userIdx = history.indexOfLast { it.role == "user" && it.content == userInput }
                if (userIdx >= 0 && correction != null) {
                    history[userIdx] = history[userIdx].copy(correction = correction)
                }

                val assistantTurn = ConversationTurn("assistant", reply)
                history.add(assistantTurn)

                dao.insertMessage(MessageEntity(sessionId = sessionId, role = "assistant", content = reply))

                val newEscalation = updateEscalation(reply, _uiState.value.escalationLevel)

                messagesSinceMemoryUpdate++
                if (messagesSinceMemoryUpdate >= 8) {
                    messagesSinceMemoryUpdate = 0
                    val chatMessages = history.map { ChatMessage(it.role, it.content) }
                    updateMemory.update(_uiState.value.currentCharacter.name, chatMessages)
                }

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

    private suspend fun checkGrammar(userInput: String): String? {
        return try {
            val prompt = listOf(
                ChatMessage("system", "You are a grammar checker. If the sentence has a grammar error, reply with ONLY the corrected sentence. If it is already correct, reply with exactly: OK"),
                ChatMessage("user", userInput)
            )
            val response = OpenRouterClient.api.chat(
                auth = OpenRouterClient.authHeader,
                request = ChatRequest(messages = prompt, maxTokens = 80)
            )
            val result = response.body()?.choices?.firstOrNull()?.message?.content?.trim() ?: return null
            if (result == "OK" || result.equals(userInput.trim(), ignoreCase = true)) null else result
        } catch (e: Exception) {
            null
        }
    }

    fun getSuggestedTopics(): List<String> {
        val char = _uiState.value.currentCharacter
        val world = _uiState.value.currentWorld
        return when (world) {
            com.elara.app.domain.model.World.MODERN -> listOf(
                "What do you do for fun on weekends?",
                "Have you ever traveled abroad?",
                "What kind of music do you like?",
                "Do you cook at home or eat out more?"
            )
            com.elara.app.domain.model.World.MEDIEVAL -> listOf(
                "What dangers lurk in these lands?",
                "Have you ever fought in battle?",
                "What do you think of the king?",
                "Tell me about the village market."
            )
            com.elara.app.domain.model.World.SCIFI -> listOf(
                "What planet did you come from?",
                "Have you seen the new ships at the station?",
                "Do you trust the AI systems here?",
                "What do you think of the colony expansion?"
            )
            com.elara.app.domain.model.World.WESTERN -> listOf(
                "Where are you headed, stranger?",
                "Have you heard about the robbery?",
                "Do you know the sheriff around here?",
                "What do you think of this town?"
            )
            com.elara.app.domain.model.World.BEACH -> listOf(
                "Do you come here often?",
                "What's your favorite thing about the ocean?",
                "Are you staying at the resort?",
                "Want to grab a drink nearby?"
            )
            com.elara.app.domain.model.World.CYBERPUNK -> listOf(
                "Which corp do you work for?",
                "Have you heard about the data heist last night?",
                "Do you have any augmentations?",
                "Where do you get your supplies?"
            )
        }
    }

    private suspend fun ensureSession(): Long {
        val existing = _uiState.value.currentSessionId
        if (existing != null) return existing
        val state = _uiState.value
        val session = dao.getSessionByCombo(state.currentWorld.name, state.currentPersonality.name, state.currentCharacter.name)
            ?: dao.insertSession(SessionEntity(
                world = state.currentWorld.name,
                personality = state.currentPersonality.name,
                character = state.currentCharacter.name
            )).let { dao.getSessionByCombo(state.currentWorld.name, state.currentPersonality.name, state.currentCharacter.name)!! }
        _uiState.value = _uiState.value.copy(currentSessionId = session.id)
        dao.upsertProfile((dao.getProfile() ?: ProfileEntity()).copy(currentSessionId = session.id))
        return session.id
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
            val state = _uiState.value
            val isDark = state.currentPersonality != Personality.FRIENDLY
            val startEsc = if (isDark) 9 else 0
            val sessionId = dao.insertSession(SessionEntity(
                world = state.currentWorld.name,
                personality = state.currentPersonality.name,
                character = state.currentCharacter.name
            ))
            dao.upsertProfile((dao.getProfile() ?: ProfileEntity()).copy(
                currentWorld = state.currentWorld.name,
                currentPersonality = state.currentPersonality.name,
                currentCharacter = state.currentCharacter.name,
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
