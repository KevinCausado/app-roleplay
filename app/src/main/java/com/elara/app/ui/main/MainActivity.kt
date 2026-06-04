package com.elara.app.ui.main

import android.Manifest
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
import com.elara.app.databinding.ActivityMainBinding
import com.elara.app.domain.model.Personality
import com.elara.app.ui.conversation.ConversationViewModel
import com.elara.app.ui.conversation.VoiceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ConversationViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    private lateinit var adapter: MessageAdapter
    private lateinit var voiceManager: VoiceManager
    private var isListening = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupInput()
        setupVoice()
        setupObservers()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setupRecycler() {
        adapter = MessageAdapter()
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

        // Tap avatar or world label to open worlds selector
        binding.avatarImage.setOnClickListener { showWorldsSheet() }
        binding.worldLabel.setOnClickListener { showWorldsSheet() }

        // Tap personality label to open personality selector
        binding.personalityLabel.setOnClickListener { showPersonalitySheet() }

        // Long press clear button to reset
        binding.clearButton.setOnClickListener {
            viewModel.clearHistory()
        }
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
    }

    private fun toggleMic() {
        if (isListening) {
            val text = voiceManager.stopListening()
            isListening = false
            binding.micButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            if (text.isNotEmpty()) {
                voiceManager.stopSpeaking()
                viewModel.sendMessage(text)
            }
        } else {
            voiceManager.stopSpeaking()
            voiceManager.startContinuous()
            isListening = true
            binding.micButton.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun setupVoice() {
        voiceManager = VoiceManager(this)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.messages)
                if (adapter.itemCount > 0) {
                    binding.chatRecycler.scrollToPosition(adapter.itemCount - 1)
                }

                // Status
                binding.statusText.text = if (state.isLoading) "● typing..." else "● online"
                binding.statusText.setTextColor(
                    if (state.isLoading) 0xFFFFAA33.toInt() else 0xFF4CAF50.toInt()
                )

                // World label
                binding.worldLabel.text = "${state.currentWorld.emoji} ${state.currentWorld.displayName}"

                // Personality label
                binding.personalityLabel.text = "${state.currentPersonality.emoji} ${state.currentPersonality.displayName}"

                // Escalation dots
                binding.escalationText.text = "❤".repeat(state.escalationLevel.coerceAtMost(9))

                // Auto-speak last assistant message
                if (!state.isLoading && state.messages.isNotEmpty()) {
                    val last = state.messages.last()
                    if (last.role == "assistant") {
                        voiceManager.speak(last.content)
                    }
                }

                state.error?.let {
                    Toast.makeText(this@MainActivity, "Error: $it", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}
