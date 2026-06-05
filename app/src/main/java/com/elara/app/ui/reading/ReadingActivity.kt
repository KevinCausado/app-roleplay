package com.elara.app.ui.reading

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.elara.app.R
import com.elara.app.data.local.AppDatabase
import com.elara.app.data.local.entity.WordEntity
import com.elara.app.data.remote.TranslatorClient
import com.elara.app.databinding.ActivityReadingBinding
import com.elara.app.ui.conversation.VoiceManager
import kotlinx.coroutines.launch

class ReadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadingBinding
    private lateinit var voiceManager: VoiceManager
    private var isReading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        voiceManager = VoiceManager(this)
        binding.backButton.setOnClickListener { finish() }

        binding.readingLoadBtn.setOnClickListener {
            val text = binding.readingInput.text.toString().trim()
            if (text.isBlank()) {
                Toast.makeText(this, "Paste some text first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.readingLoadBtn.visibility = View.GONE
            binding.readingInput.visibility = View.GONE
            binding.readingScroll.visibility = View.VISIBLE
            makeWordsTappable(binding.readingContent, text)
        }

        binding.readingSpeakBtn.setOnClickListener {
            if (isReading) {
                voiceManager.stopSpeaking()
                binding.readingSpeakBtn.text = "🔊 Read"
                isReading = false
            } else {
                val text = binding.readingContent.text.toString()
                if (text.isNotBlank()) {
                    voiceManager.speak(text)
                    binding.readingSpeakBtn.text = "⏹ Stop"
                    isReading = true
                    voiceManager.onTtsDone = {
                        runOnUiThread {
                            binding.readingSpeakBtn.text = "🔊 Read"
                            isReading = false
                        }
                    }
                }
            }
        }
    }

    private fun makeWordsTappable(tv: TextView, text: String) {
        val spannable = SpannableString(text)
        val wordRegex = Regex("[a-zA-Z']+")
        wordRegex.findAll(text).forEach { match ->
            val word = match.value
            val span = object : ClickableSpan() {
                override fun onClick(view: View) {
                    showTranslatePopup(view, word, text)
                }
                override fun updateDrawState(ds: android.text.TextPaint) {
                    ds.color = 0xFFE2DFF5.toInt()
                    ds.isUnderlineText = false
                }
            }
            spannable.setSpan(span, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        tv.text = spannable
        tv.movementMethod = LinkMovementMethod.getInstance()
        tv.highlightColor = 0x33A78BFA
    }

    private fun showTranslatePopup(anchor: View, word: String, context: String) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_translate, null)
        val popup = PopupWindow(popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 16f

        val wordTv = popupView.findViewById<TextView>(R.id.popupWord)
        val translationTv = popupView.findViewById<TextView>(R.id.popupTranslation)
        val saveBtn = popupView.findViewById<Button>(R.id.popupSaveButton)

        wordTv.text = word
        translationTv.text = "..."

        lifecycleScope.launch {
            val translation = TranslatorClient.translate(word)
            translationTv.text = translation
            saveBtn.setOnClickListener {
                lifecycleScope.launch {
                    val db = AppDatabase.get(this@ReadingActivity)
                    if (db.wordDao().getByWord(word.lowercase()) == null) {
                        db.wordDao().insert(WordEntity(
                            word = word.lowercase(),
                            translation = translation,
                            context = context.take(120)
                        ))
                        saveBtn.text = "Saved!"
                        saveBtn.isEnabled = false
                    } else {
                        saveBtn.text = "Already saved"
                        saveBtn.isEnabled = false
                    }
                }
            }
        }
        popup.showAtLocation(anchor, Gravity.CENTER, 0, -200)
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}
