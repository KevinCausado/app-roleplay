package com.elara.app.ui.character

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.CustomCharacterEntity
import com.elara.app.databinding.ActivityCreateCharacterBinding
import kotlinx.coroutines.launch
import java.util.UUID

class CreateCharacterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCharacterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCharacterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        binding.saveButton.setOnClickListener {
            val name = binding.nameInput.text?.toString()?.trim() ?: ""
            val desc = binding.descInput.text?.toString()?.trim() ?: ""
            val greeting = binding.greetingInput.text?.toString()?.trim() ?: ""
            val prompt = binding.promptInput.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (prompt.isEmpty()) {
                Toast.makeText(this, "System prompt is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val character = CustomCharacterEntity(
                    id = "custom_${UUID.randomUUID()}",
                    displayName = name,
                    description = desc,
                    greeting = greeting.ifEmpty { "Hey! What's up?" },
                    systemPrompt = prompt
                )
                AppDatabase.get(this@CreateCharacterActivity).customCharacterDao().upsert(character)
                Toast.makeText(this@CreateCharacterActivity, "$name created!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
