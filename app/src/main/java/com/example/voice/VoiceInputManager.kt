package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface VoiceInputListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onPartialResults(partialText: String)
    fun onResults(recognizedText: String)
    fun onError(errorCode: Int, errorMessage: String)
    fun onEndOfSpeech()
}

class VoiceInputManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var listener: VoiceInputListener? = null

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _isAvailable = MutableStateFlow(SpeechRecognizer.isRecognitionAvailable(context))
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    fun setListener(listener: VoiceInputListener?) {
        this.listener = listener
    }

    fun startListening(languageCode: String = "auto") {
        stopListening()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener?.onError(-1, "Speech recognition is not available on this device.")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        listener?.onReadyForSpeech()
                    }

                    override fun onBeginningOfSpeech() {
                        listener?.onBeginningOfSpeech()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB to +10dB range roughly into 0..1f
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized
                        listener?.onRmsChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        _audioAmplitude.value = 0f
                        listener?.onEndOfSpeech()
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        _audioAmplitude.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        listener?.onError(error, message)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        _audioAmplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            listener?.onResults(text)
                        } else {
                            listener?.onError(SpeechRecognizer.ERROR_NO_MATCH, "No speech recognized")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            listener?.onPartialResults(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val selectedLocale = when (languageCode.lowercase()) {
                    "bn", "bn-bd", "bangla", "bengali" -> "bn-BD"
                    "hi", "hi-in", "hindi" -> "hi-IN"
                    "en", "en-us", "english" -> "en-US"
                    else -> Locale.getDefault().toLanguageTag()
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLocale)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            listener?.onError(-2, "Failed to start listening: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
        _audioAmplitude.value = 0f
    }

    fun destroy() {
        stopListening()
    }
}
