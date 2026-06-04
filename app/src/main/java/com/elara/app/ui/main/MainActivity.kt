package com.elara.app.ui.main

import android.Manifest
import android.accounts.AccountManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.elara.app.R
import com.elara.app.data.remote.drive.ElaraDriveSync
import com.elara.app.databinding.ActivityMainBinding
import com.elara.app.domain.model.Character
import com.elara.app.domain.model.Personality
import com.elara.app.domain.model.World
import com.elara.app.ui.conversation.ConversationViewModel
import com.elara.app.ui.conversation.VoiceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var driveSync: ElaraDriveSync

    private val accountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                driveSync.driveService.setAccount(accountName)
                Toast.makeText(this, "Connected: $accountName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val viewModel: ConversationViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private lateinit var adapter: MessageAdapter
    private lateinit var voiceManager: VoiceManager
    private var isListening = false
    private var isImmersive = false
    private var immersiveLoopActive = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        driveSync = ElaraDriveSync(this)
        setupRecycler()
        setupInput()
        setupVoice()
        setupObservers()
        loadVoiceSettings()
        updateStreakDisplay()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setupRecycler() {
        adapter = MessageAdapter(lifecycleScope) { text ->
            voiceManager.speak(text)
            binding.immersiveStatusText.text = "Repeat after me..."
        }
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatRecycler.layoutManager = layoutManager
        binding.chatRecycler.adapter = adapter
    }

    private fun setupInput() {
        binding.sendButton.setOnClickListener { sendText() }
        binding.micButton.setOnClickListener { toggleMic() }

        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendText(); true } else false
        }

        binding.avatarImage.setOnClickListener { showCharacterSheet() }
        binding.characterNameText.setOnClickListener { showCharacterSheet() }
        binding.worldLabel.setOnClickListener { showWorldsSheet() }
        binding.personalityLabel.setOnClickListener { showPersonalitySheet() }
        binding.clearButton.setOnClickListener { viewModel.clearHistory() }
        binding.settingsButton.setOnClickListener { showChatSettings() }
        binding.wordsButton.setOnClickListener {
            startActivity(android.content.Intent(this, com.elara.app.ui.words.WordsActivity::class.java))
        }

        // Immersive toggle
        binding.immersiveToggle.setOnClickListener { enterImmersive() }
        binding.exitImmersive.setOnClickListener { exitImmersive() }
        binding.immersiveMic.setOnClickListener { toggleImmersiveMic() }
    }

    private fun showCharacterSheet() {
        val current = viewModel.uiState.value.currentCharacter
        CharacterBottomSheet(current) { character ->
            viewModel.setCharacter(character)
        }.show(supportFragmentManager, "character")
    }

    private fun showWorldsSheet() {
        val current = viewModel.uiState.value.currentWorld
        WorldsBottomSheet(current) { world ->
            viewModel.setWorld(world)
        }.show(supportFragmentManager, "worlds")
    }

    private fun showPersonalitySheet() {
        val current = viewModel.uiState.value.currentPersonality
        PersonalityBottomSheet(current) { personality ->
            viewModel.setPersonality(personality)
        }.show(supportFragmentManager, "personality")
    }

    private fun sendText() {
        val text = binding.messageInput.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return
        binding.messageInput.text?.clear()
        voiceManager.stopSpeaking()
        viewModel.sendMessage(text)
        StreakManager.recordToday(this)
        updateStreakDisplay()
    }

    private fun updateStreakDisplay() {
        val streak = StreakManager.getStreak(this)
        binding.streakText.text = if (streak > 1) "🔥$streak" else ""
    }

    // Normal mode mic
    private fun toggleMic() {
        if (isListening) {
            voiceManager.stopListening()
            isListening = false
            binding.micButton.setImageResource(android.R.drawable.ic_btn_speak_now)
        } else {
            voiceManager.stopSpeaking()
            voiceManager.startDictation(binding.messageInput.text?.toString() ?: "")
            isListening = true
            binding.micButton.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    // Immersive mode
    private fun enterImmersive() {
        isImmersive = true
        binding.immersiveOverlay.visibility = android.view.View.VISIBLE
        binding.chatRecycler.visibility = android.view.View.GONE
        binding.inputContainer.visibility = android.view.View.GONE
        binding.header.visibility = android.view.View.GONE
        binding.immersiveHint.text = "tap to speak"
        binding.immersiveStatusText.text = "Elara"
        binding.pulseRing.visibility = android.view.View.INVISIBLE
        binding.immersiveMic.setImageResource(android.R.drawable.ic_btn_speak_now)
    }

    private fun exitImmersive() {
        immersiveLoopActive = false
        isImmersive = false
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
        stopPulse()
        binding.immersiveOverlay.visibility = android.view.View.GONE
        binding.chatRecycler.visibility = android.view.View.VISIBLE
        binding.inputContainer.visibility = android.view.View.VISIBLE
        binding.header.visibility = android.view.View.VISIBLE
        binding.immersiveMic.setImageResource(android.R.drawable.ic_btn_speak_now)
    }

    private fun toggleImmersiveMic() {
        if (immersiveLoopActive) stopImmersiveLoop() else startImmersiveLoop()
    }

    private fun startImmersiveLoop() {
        immersiveLoopActive = true
        binding.immersiveHint.text = "listening..."
        binding.immersiveStatusText.text = "I'm listening"
        startPulse()
        voiceManager.stopSpeaking()
        voiceManager.startListening()
        binding.immersiveMic.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun stopImmersiveLoop() {
        immersiveLoopActive = false
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
        stopPulse()
        binding.immersiveHint.text = "tap to speak"
        binding.immersiveStatusText.text = "Elara"
        binding.immersiveMic.setImageResource(android.R.drawable.ic_btn_speak_now)
    }

    private fun startPulse() {
        binding.pulseRing.visibility = android.view.View.VISIBLE
        val anim = android.view.animation.AnimationUtils.loadAnimation(this, com.elara.app.R.anim.pulse)
        binding.pulseRing.startAnimation(anim)
    }

    private fun stopPulse() {
        binding.pulseRing.clearAnimation()
        binding.pulseRing.visibility = android.view.View.INVISIBLE
    }

    private fun setupVoice() {
        voiceManager = VoiceManager(this)

        // Dictation fills input in normal mode
        voiceManager.onDictationResult = { text ->
            if (!isImmersive) {
                binding.messageInput.setText(text)
                binding.messageInput.setSelection(text.length)
            }
        }

        // Single-shot result: auto-send in immersive mode
        voiceManager.onSpeechResult = { text ->
            if (isImmersive && immersiveLoopActive) {
                if (text.isNotBlank()) {
                    viewModel.sendMessage(text)
                }
            }
        }

        voiceManager.onTtsDone = {
            runOnUiThread {
                if (isImmersive && immersiveLoopActive) {
                    binding.immersiveHint.text = "Listening..."
                    voiceManager.startListening()
                }
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.messages)
                if (adapter.itemCount > 0) {
                    binding.chatRecycler.scrollToPosition(adapter.itemCount - 1)
                }

                binding.statusText.text = if (state.isLoading) "● typing..." else "● online"
                binding.statusText.setTextColor(
                    if (state.isLoading) 0xFFFFAA33.toInt() else 0xFF4CAF50.toInt()
                )
                binding.worldLabel.text = "${state.currentWorld.emoji} ${state.currentWorld.displayName}"
                binding.personalityLabel.text = "${state.currentPersonality.emoji} ${state.currentPersonality.displayName}"
                binding.escalationText.text = "❤".repeat(state.escalationLevel.coerceAtMost(9))
                binding.characterNameText.text = state.currentCharacter.displayName
                binding.messageInput.hint = "Message ${state.currentCharacter.displayName}..."
                updateWorldTheme(state.currentWorld)

                // Immersive status
                if (isImmersive) {
                    val charName = state.currentCharacter.displayName
                    when {
                        state.isLoading -> {
                            binding.immersiveHint.text = "thinking..."
                            binding.immersiveStatusText.text = "..."
                            stopPulse()
                        }
                        !state.isLoading && state.messages.lastOrNull()?.role == "assistant" -> {
                            binding.immersiveHint.text = "speaking"
                            binding.immersiveStatusText.text = charName
                            stopPulse()
                        }
                        immersiveLoopActive -> {
                            binding.immersiveHint.text = "listening..."
                            binding.immersiveStatusText.text = "I'm listening"
                            startPulse()
                        }
                    }
                }

                // Auto-speak last assistant message
                if (!state.isLoading && state.messages.isNotEmpty()) {
                    val last = state.messages.last()
                    if (last.role == "assistant") {
                        voiceManager.speak(last.content)
                    }
                }

                state.error?.let {
                    Toast.makeText(this@MainActivity, "Error: $it", Toast.LENGTH_SHORT).show()
                    if (isImmersive) stopImmersiveLoop()
                }
            }
        }
    }

    private var currentFontSize = 15f

    private fun loadVoiceSettings() {
        voiceManager.pitch = VoiceSettings.getPitch(this)
        voiceManager.speechRate = VoiceSettings.getRate(this)
        voiceManager.applyVoiceSettings()
    }

    private fun showChatSettings() {
        val worldName = viewModel.uiState.value.currentWorld.name
        ChatSettingsSheet(
            currentFontSize = currentFontSize,
            currentWorldName = worldName,
            currentPitch = VoiceSettings.getPitch(this),
            currentRate = VoiceSettings.getRate(this),
            driveAccountName = driveSync.driveService.getSavedAccountName(),
            onFontSizeChanged = { size ->
                currentFontSize = size
                adapter.setFontSize(size)
            },
            onBackgroundPicked = { uri ->
                BackgroundManager.save(this, worldName, uri)
                applyBackground(worldName)
            },
            onVoiceChanged = { pitch, rate ->
                VoiceSettings.savePitch(this, pitch)
                VoiceSettings.saveRate(this, rate)
                voiceManager.pitch = pitch
                voiceManager.speechRate = rate
                voiceManager.applyVoiceSettings()
            },
            onDriveConnect = { launchAccountPicker() },
            onDrivePush = {
                lifecycleScope.launch {
                    val ok = driveSync.pushAll()
                    Toast.makeText(this@MainActivity, if (ok) "Backup done" else "Backup failed", Toast.LENGTH_SHORT).show()
                }
            },
            onDrivePull = {
                lifecycleScope.launch {
                    val ok = driveSync.pullAll()
                    Toast.makeText(this@MainActivity, if (ok) "Restored from Drive" else "Restore failed", Toast.LENGTH_SHORT).show()
                }
            }
        ).show(supportFragmentManager, "settings")
    }

    private fun launchAccountPicker() {
        val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
            .usingOAuth2(this, listOf(DriveScopes.DRIVE_FILE))
        accountPickerLauncher.launch(credential.newChooseAccountIntent())
    }

    private fun applyBackground(worldName: String) {
        val uri = BackgroundManager.get(this, worldName)
        if (uri != null) {
            binding.root.background = null
            binding.chatBgImage.setImageURI(uri)
            binding.chatBgImage.visibility = android.view.View.VISIBLE
            binding.immersiveBgImage.setImageURI(uri)
            binding.immersiveBgImage.visibility = android.view.View.VISIBLE
        } else {
            binding.chatBgImage.visibility = android.view.View.GONE
            binding.immersiveBgImage.visibility = android.view.View.GONE
        }
    }

    private fun updateWorldTheme(world: World) {
        val uri = BackgroundManager.get(this, world.name)
        if (uri != null) {
            binding.root.background = null
            binding.chatBgImage.setImageURI(uri)
            binding.chatBgImage.visibility = android.view.View.VISIBLE
            binding.immersiveBgImage.setImageURI(uri)
            binding.immersiveBgImage.visibility = android.view.View.VISIBLE
        } else {
            binding.chatBgImage.visibility = android.view.View.GONE
            binding.immersiveBgImage.visibility = android.view.View.GONE
            val bgRes = when (world) {
                World.MODERN -> R.drawable.bg_modern
                World.MEDIEVAL -> R.drawable.bg_medieval
                World.SCIFI -> R.drawable.bg_scifi
                World.WESTERN -> R.drawable.bg_western
                World.BEACH -> R.drawable.bg_beach
                World.CYBERPUNK -> R.drawable.bg_cyberpunk
            }
            binding.root.setBackgroundResource(bgRes)
            binding.immersiveOverlay.setBackgroundResource(bgRes)
        }

        val accent = when (world) {
            World.MODERN -> 0xFF7B5EA7.toInt()
            World.MEDIEVAL -> 0xFF8B4513.toInt()
            World.SCIFI -> 0xFF00B4D8.toInt()
            World.WESTERN -> 0xFFB8730A.toInt()
            World.BEACH -> 0xFF00897B.toInt()
            World.CYBERPUNK -> 0xFFE040FB.toInt()
        }
        binding.sendButton.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
        binding.worldLabel.setTextColor(accent)
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}
