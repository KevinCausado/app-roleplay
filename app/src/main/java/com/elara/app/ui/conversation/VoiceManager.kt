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

    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechEnded: (() -> Unit)? = null
    var onTtsDone: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(1.1f)
                tts?.setSpeechRate(0.95f)
                isTtsReady = true
            }
        }
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
        startInternal()
    }

    fun startContinuous() {
        continuous = true
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
            override fun onEndOfSpeech() { if (!continuous) onSpeechEnded?.invoke(); restartIfContinuous() }
            override fun onError(error: Int) { if (!continuous) onSpeechEnded?.invoke(); restartIfContinuous() }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (text != null) onSpeechResult?.invoke(text)
                if (!continuous) onSpeechEnded?.invoke()
                restartIfContinuous()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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
        stopInternal()
    }

    fun destroy() {
        continuous = false
        tts?.shutdown()
        stopInternal()
    }
}
