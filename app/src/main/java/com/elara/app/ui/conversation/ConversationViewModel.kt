package com.elara.app.ui.conversation

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.MessageEntity
import com.elara.app.data.local.entity.ProfileEntity
import com.elara.app.data.local.entity.SessionEntity
import com.elara.app.data.remote.ChatMessage
import com.elara.app.data.remote.ChatRequest
import com.elara.app.data.remote.GeminiClient
import com.elara.app.data.remote.OpenRouterClient
import com.elara.app.domain.model.Character
import com.elara.app.domain.model.ConversationTurn
import com.elara.app.domain.model.Personality
import com.elara.app.domain.model.Scenario
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
    val currentSessionId: Long? = null,
    val activeScenario: Scenario? = null,
    val photoDescription: String? = null
)

class ConversationViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.sessionDao()
    private val memoryDao = db.characterMemoryDao()
    private val studyPlanDao = db.studyPlanDao()
    private val errorDao = db.errorDao()

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState

    private val buildPrompt = BuildPromptUseCase()
    private val calibrate = CalibrateLanguageLevelUseCase()
    private val updateMemory = UpdateCharacterMemoryUseCase(memoryDao)
    private val history = mutableListOf<ConversationTurn>()
    private var messagesSinceMemoryUpdate = 0
    private var photoBitmap: Bitmap? = null

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

    fun setScenario(scenario: Scenario?) {
        if (scenario != null) {
            val turn = ConversationTurn("assistant", scenario.openingLine)
            history.add(turn)
            viewModelScope.launch {
                val sessionId = ensureSession()
                dao.insertMessage(MessageEntity(sessionId = sessionId, role = "assistant", content = scenario.openingLine))
            }
        }
        _uiState.value = _uiState.value.copy(
            activeScenario = scenario,
            messages = history.toList()
        )
    }

    fun completeScenario(scenarioId: String) {
        viewModelScope.launch {
            val plan = studyPlanDao.get() ?: com.elara.app.data.local.entity.StudyPlanEntity()
            val existing = plan.completedScenarios.split(",").filter { it.isNotBlank() }.toMutableSet()
            existing.add(scenarioId)
            studyPlanDao.upsert(plan.copy(completedScenarios = existing.joinToString(",")))
        }
    }

    fun setPhotoDescription(description: String, bitmap: Bitmap? = null) {
        photoBitmap = bitmap
        _uiState.value = _uiState.value.copy(photoDescription = description)
    }

    fun clearPhotoDescription() {
        photoBitmap = null
        _uiState.value = _uiState.value.copy(photoDescription = null)
    }

    fun reactToPhoto() {
        val bitmap = photoBitmap ?: return
        photoBitmap = null

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val sessionId = ensureSession()
                val profile = dao.getProfile() ?: ProfileEntity()
                val charId = _uiState.value.currentCharacter.name
                val charMemory = memoryDao.get(charId)?.memory ?: ""
                val scenarioPrompt = _uiState.value.activeScenario?.let { scenario ->
                    """You are in a guided learning scenario: "${scenario.title}"
${scenario.systemPrompt}

Learning objectives: ${scenario.learningObjectives}

Use this vocabulary naturally where it fits: ${scenario.suggestedVocab.joinToString(", ")}
"""
                } ?: ""

                val systemPrompt = buildPrompt.buildSystemPromptForPhoto(
                    world = _uiState.value.currentWorld,
                    personality = _uiState.value.currentPersonality,
                    character = _uiState.value.currentCharacter,
                    level = _uiState.value.estimatedLevel,
                    escalation = _uiState.value.escalationLevel,
                    lorebookEntries = profile.lorebookEntries,
                    characterMemory = charMemory,
                    scenarioContext = scenarioPrompt
                )

                val chatHistory = history.map { ChatMessage(it.role, it.content) }

                val reply = GeminiClient.chatWithImage(
                    systemPrompt = systemPrompt,
                    history = chatHistory.takeLast(10),
                    userMessage = "[Kevin just showed you a photo. Look at it and react naturally. Comment on what you see, ask a question, or relate it to your conversation. Do NOT describe it like a list — just react like a real person would.]",
                    bitmap = bitmap
                )

                val assistantTurn = ConversationTurn("assistant", reply)
                history.add(assistantTurn)
                dao.insertMessage(MessageEntity(sessionId = sessionId, role = "assistant", content = reply))

                _uiState.value = _uiState.value.copy(
                    messages = history.toList(),
                    isLoading = false,
                    photoDescription = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
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
                val scenarioPrompt = _uiState.value.activeScenario?.let { scenario ->
                    """You are in a guided learning scenario: "${scenario.title}"
${scenario.systemPrompt}

Learning objectives: ${scenario.learningObjectives}

Use this vocabulary naturally where it fits: ${scenario.suggestedVocab.joinToString(", ")}

This is a ${scenario.level.label} level scenario. Adjust your language complexity accordingly."""
                } ?: ""

                val messages = buildPrompt.build(
                    history = history.dropLast(1),
                    userInput = userInput,
                    cameraContext = _uiState.value.photoDescription,
                    world = _uiState.value.currentWorld,
                    personality = _uiState.value.currentPersonality,
                    character = _uiState.value.currentCharacter,
                    estimatedLevel = newLevel,
                    escalationLevel = _uiState.value.escalationLevel,
                    lorebookEntries = profile.lorebookEntries,
                    characterMemory = charMemory,
                    scenarioContext = scenarioPrompt
                )

                // Launch grammar check in parallel with main chat call (merged: correction + natural suggestion)
                val grammarDeferred = if (userInput.trim().split(" ").size >= 3) {
                    async { checkGrammarAndSuggest(userInput) }
                } else null

                val bitmap = photoBitmap
                photoBitmap = null

                val reply = if (bitmap != null) {
                    val systemPrompt = messages.first().content
                    val chatHistory = messages.drop(1).dropLast(1)
                    GeminiClient.chatWithImage(systemPrompt, chatHistory, userInput, bitmap)
                } else {
                    val response = OpenRouterClient.api.chat(
                        auth = OpenRouterClient.authHeader,
                        request = ChatRequest(messages = messages)
                    )
                    response.body()?.choices?.firstOrNull()?.message?.content ?: "..."
                }

                val grammarResult = grammarDeferred?.await()

                // Update the user turn with correction + natural suggestion
                val userIdx = history.indexOfLast { it.role == "user" && it.content == userInput }
                if (userIdx >= 0 && grammarResult != null) {
                    history[userIdx] = history[userIdx].copy(
                        correction = grammarResult.correction,
                        phraseSuggestion = grammarResult.naturalPhrasing
                    )
                }

                val assistantTurn = ConversationTurn("assistant", reply)
                history.add(assistantTurn)

                dao.insertMessage(MessageEntity(sessionId = sessionId, role = "assistant", content = reply))

                _uiState.value = _uiState.value.copy(photoDescription = null)

                val newEscalation = updateEscalation(reply, _uiState.value.escalationLevel)

                messagesSinceMemoryUpdate++
                if (messagesSinceMemoryUpdate >= 8) {
                    messagesSinceMemoryUpdate = 0
                    val chatMessages = history.map { ChatMessage(it.role, it.content) }
                    updateMemory.update(_uiState.value.currentCharacter.name, chatMessages)
                }
                if (history.size > 30 && history.size % 25 == 0) {
                    val chatMessages = history.map { ChatMessage(it.role, it.content) }
                    updateMemory.summarizeSession(_uiState.value.currentCharacter.name, chatMessages)
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

    data class GrammarResult(val correction: String?, val naturalPhrasing: String?)

    private suspend fun checkGrammarAndSuggest(userInput: String): GrammarResult {
        return try {
            val prompt = listOf(
                ChatMessage("system", """You are an English coach for an A2 learner.
Reply with EXACTLY one or two lines:
- If GRAMMAR ERROR: "FIX: <corrected version>"
- If it could sound MORE NATURAL (even if correct): "NATURAL: <better phrasing>"
- You can output both lines if needed.
- If the sentence is perfect, reply: "OK"
Examples:
Input: "I go to gym yesterday"
Output: FIX: I went to the gym yesterday
NATURAL: I hit the gym yesterday

Input: "I went to the store"
Output: NATURAL: I stopped by the store

Input: "Where is the bathroom?"
Output: OK"""),
                ChatMessage("user", userInput)
            )
            val response = OpenRouterClient.api.chat(
                auth = OpenRouterClient.authHeader,
                request = ChatRequest(messages = prompt, maxTokens = 120)
            )
            val result = response.body()?.choices?.firstOrNull()?.message?.content?.trim() ?: return GrammarResult(null, null)
            if (result == "OK") return GrammarResult(null, null)

            var correction: String? = null
            var natural: String? = null
            result.lines().forEach { line ->
                when {
                    line.startsWith("FIX:") -> {
                        correction = line.removePrefix("FIX:").trim()
                        if (correction == userInput.trim()) correction = null
                    }
                    line.startsWith("NATURAL:") -> {
                        natural = line.removePrefix("NATURAL:").trim()
                        if (natural.equals(userInput.trim(), ignoreCase = true)) natural = null
                    }
                }
            }

            if (correction != null) {
                errorDao.insert(com.elara.app.data.local.entity.ErrorEntity(
                    original = userInput.trim(),
                    corrected = correction
                ))
            }

            GrammarResult(correction, natural)
        } catch (e: Exception) {
            GrammarResult(null, null)
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
