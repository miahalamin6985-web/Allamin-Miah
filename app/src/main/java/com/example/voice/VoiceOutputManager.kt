package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface VoiceOutputListener {
    fun onSpeechStarted()
    fun onSpeechCompleted()
    fun onSpeechError(error: String)
}

class VoiceOutputManager(
    context: Context,
    private val listener: VoiceOutputListener? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch = 1.05f
    private var currentSpeed = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    listener?.onSpeechStarted()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    listener?.onSpeechCompleted()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    listener?.onSpeechError("TTS speech playback failed")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    listener?.onSpeechError("TTS error code: $errorCode")
                }
            })
            applyVoiceAttributes()
        }
    }

    fun setVoiceAttributes(pitch: Float, speed: Float) {
        this.currentPitch = pitch
        this.currentSpeed = speed
        applyVoiceAttributes()
    }

    private fun applyVoiceAttributes() {
        if (!isInitialized) return
        tts?.setPitch(currentPitch)
        tts?.setSpeechRate(currentSpeed)
    }

    /**
     * Speaks the given text, detecting Bengali/Hindi script to configure natural locale voice
     */
    fun speak(text: String, preferredLanguage: String = "auto") {
        if (!isInitialized || text.isBlank()) return

        stop()

        // Detect script
        val locale = resolveLocale(text, preferredLanguage)
        tts?.language = locale
        applyVoiceAttributes()

        val utteranceId = "nova_tts_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    private fun resolveLocale(text: String, preferred: String): Locale {
        when (preferred.lowercase()) {
            "bn", "bengali", "bangla" -> return Locale.forLanguageTag("bn-BD")
            "hi", "hindi" -> return Locale.forLanguageTag("hi-IN")
            "en", "english" -> return Locale.US
        }

        // Auto-detect based on unicode blocks
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        if (hasBengali) {
            return Locale.forLanguageTag("bn-BD")
        }

        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        if (hasHindi) {
            return Locale.forLanguageTag("hi-IN")
        }

        return Locale.US
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
