package com.elara.app.ui.conversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var stt: SpeechRecognizer? = null
    private var isTtsReady = false
    private var continuous = false
    private val dictationBuffer = StringBuilder()

    var pitch: Float = 1.1f
    var speechRate: Float = 0.95f
    private var charPitch: Float? = null
    private var charRate: Float? = null

    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechEnded: (() -> Unit)? = null
    var onTtsDone: (() -> Unit)? = null
    var onDictationResult: ((String) -> Unit)? = null
    var onListeningStateChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(pitch)
                tts?.setSpeechRate(speechRate)
                isTtsReady = true
            }
        }
    }

    fun setCharacterVoice(pitch: Float, rate: Float) {
        charPitch = pitch
        charRate = rate
        applyVoiceSettings()
    }

    fun applyVoiceSettings() {
        if (!isTtsReady) return
        tts?.setPitch(charPitch ?: pitch)
        tts?.setSpeechRate(charRate ?: speechRate)
    }

    fun speak(text: String) {
        if (!isTtsReady) return
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onTtsDone?.invoke() }
            override fun onError(utteranceId: String?) {}
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "elara_utterance")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening() {
        continuous = false
        onListeningStateChanged?.invoke(true)
        startInternal()
    }

    fun startDictation(existingText: String = "") {
        continuous = true
        dictationBuffer.clear()
        val trimmed = existingText.trim()
        if (trimmed.isNotEmpty()) dictationBuffer.append(trimmed).append(" ")
        onListeningStateChanged?.invoke(true)
        startInternal()
    }

    private fun startInternal() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        stopInternal()

        stt = SpeechRecognizer.createSpeechRecognizer(context)
        stt?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onSpeechStarted?.invoke() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { restartIfContinuous() }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (text != null) {
                    if (continuous) {
                        dictationBuffer.append(text).append(" ")
                        onDictationResult?.invoke(dictationBuffer.toString().trim())
                    } else {
                        onSpeechResult?.invoke(text)
                        onSpeechEnded?.invoke()
                    }
                }
                restartIfContinuous()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (continuous) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (text != null) {
                        val full = dictationBuffer.toString().trimEnd()
                        val space = if (full.isEmpty() || full.endsWith(" ")) "" else " "
                        onDictationResult?.invoke("$full$space$text")
                    }
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 300000L)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 300000L)
        }
        stt?.startListening(intent)
    }

    private fun restartIfContinuous() {
        if (continuous) startInternal()
    }

    private fun stopInternal() {
        stt?.stopListening()
        stt?.destroy()
        stt = null
    }

    fun stopListening() {
        continuous = false
        // stopListening (not destroy) triggers onResults with what was captured so far
        stt?.stopListening()
        stt?.destroy()
        stt = null
        dictationBuffer.clear()
        onListeningStateChanged?.invoke(false)
    }

    fun destroy() {
        continuous = false
        tts?.shutdown()
        stopInternal()
    }
}
