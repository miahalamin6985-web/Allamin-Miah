package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionType
import com.example.data.AssistantSettings
import com.example.data.AssistantState
import com.example.data.ChatMessage
import com.example.data.GeminiBrain
import com.example.data.MessageSender
import com.example.data.PendingConfirmation
import com.example.data.SettingsRepository
import com.example.engine.ActionEngine
import com.example.engine.ExecutionResult
import com.example.service.NovaVoiceService
import com.example.voice.VoiceInputListener
import com.example.voice.VoiceInputManager
import com.example.voice.VoiceOutputListener
import com.example.voice.VoiceOutputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RestrictionDialogState(
    val title: String,
    val explanation: String,
    val primaryButtonText: String = "Enable Permission",
    val onPrimaryAction: (() -> Unit)? = null
)

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    val actionEngine = ActionEngine(application)
    val voiceInputManager = VoiceInputManager(application)
    private val geminiBrain = GeminiBrain()

    private val _settings = MutableStateFlow(settingsRepo.loadSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _restrictionDialog = MutableStateFlow<RestrictionDialogState?>(null)
    val restrictionDialog: StateFlow<RestrictionDialogState?> = _restrictionDialog.asStateFlow()

    private val _isDeviceAdminActive = MutableStateFlow(actionEngine.deviceAdminHelper.isDeviceAdminActive())
    val isDeviceAdminActive: StateFlow<Boolean> = _isDeviceAdminActive.asStateFlow()

    val audioAmplitude = voiceInputManager.audioAmplitude

    private val voiceOutputManager = VoiceOutputManager(
        context = application,
        listener = object : VoiceOutputListener {
            override fun onSpeechStarted() {
                _assistantState.value = AssistantState.SPEAKING
            }

            override fun onSpeechCompleted() {
                if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }

            override fun onSpeechError(error: String) {
                if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }
    )

    init {
        // Apply saved voice attributes
        voiceOutputManager.setVoiceAttributes(
            _settings.value.voicePitch,
            _settings.value.voiceSpeed
        )

        // Initialize welcome message
        val welcomeMessage = ChatMessage(
            sender = MessageSender.NOVA,
            text = "Hi! I'm Nova, your AI Voice Assistant. Try saying \"Hey Nova, open YouTube\", \"turn on flashlight\", or ask me anything in English, বাংলা, or हिन्दी!",
            actionDescription = "Assistant Ready"
        )
        _messages.value = listOf(welcomeMessage)

        setupVoiceInputListener()
    }

    private fun setupVoiceInputListener() {
        voiceInputManager.setListener(object : VoiceInputListener {
            override fun onReadyForSpeech() {
                _assistantState.value = AssistantState.LISTENING
                _partialTranscript.value = ""
            }

            override fun onBeginningOfSpeech() {
                _assistantState.value = AssistantState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onPartialResults(partialText: String) {
                _partialTranscript.value = partialText
            }

            override fun onResults(recognizedText: String) {
                _partialTranscript.value = ""
                processUserQuery(recognizedText)
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                _partialTranscript.value = ""
                if (_assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }

            override fun onEndOfSpeech() {
                if (_assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.THINKING
                }
            }
        })
    }

    fun toggleListening() {
        if (_assistantState.value == AssistantState.LISTENING) {
            voiceInputManager.stopListening()
            _assistantState.value = AssistantState.IDLE
        } else if (_assistantState.value == AssistantState.SPEAKING) {
            voiceOutputManager.stop()
            _assistantState.value = AssistantState.IDLE
        } else {
            voiceOutputManager.stop()
            _assistantState.value = AssistantState.LISTENING
            voiceInputManager.startListening(_settings.value.selectedLanguage)
        }
    }

    fun stopSpeaking() {
        voiceOutputManager.stop()
        _assistantState.value = AssistantState.IDLE
    }

    fun processUserQuery(query: String) {
        if (query.isBlank()) return

        // 1. Add User Message
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = query
        )
        _messages.value = _messages.value + userMsg
        _assistantState.value = AssistantState.THINKING

        viewModelScope.launch {
            // 2. Check Action Engine for phone controls
            val executionResult: ExecutionResult? = actionEngine.processCommand(
                rawCommand = query,
                assistantName = _settings.value.assistantName,
                onExecuteDirectCall = { number ->
                    actionEngine.directCallNumber(number)
                }
            )

            if (executionResult != null) {
                handleExecutionResult(executionResult)
            } else {
                // 3. Delegate to Gemini AI Brain
                val answer = geminiBrain.answerQuestion(
                    userMessage = query,
                    assistantName = _settings.value.assistantName,
                    conversationHistory = _messages.value
                )

                val assistantMsg = ChatMessage(
                    sender = MessageSender.NOVA,
                    text = answer,
                    actionType = ActionType.GENERAL_AI
                )
                _messages.value = _messages.value + assistantMsg

                if (_settings.value.autoSpeakResponse) {
                    voiceOutputManager.speak(answer, _settings.value.selectedLanguage)
                } else {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }
    }

    private fun handleExecutionResult(res: ExecutionResult) {
        _assistantState.value = AssistantState.EXECUTING

        if (res.pendingConfirmation != null) {
            _pendingConfirmation.value = res.pendingConfirmation
            val promptMsg = ChatMessage(
                sender = MessageSender.NOVA,
                text = res.spokenResponse,
                actionType = res.actionType,
                actionDescription = "Awaiting Confirmation"
            )
            _messages.value = _messages.value + promptMsg
            if (_settings.value.autoSpeakResponse) {
                voiceOutputManager.speak(res.spokenResponse, _settings.value.selectedLanguage)
            } else {
                _assistantState.value = AssistantState.IDLE
            }
            return
        }

        if (res.triggerAdminSetup) {
            _restrictionDialog.value = RestrictionDialogState(
                title = "Device Admin Required",
                explanation = "Android security requires Device Administrator authorization for any app to lock the screen. Tap below to enable Nova AI as a device administrator.",
                primaryButtonText = "Activate in Settings",
                onPrimaryAction = {
                    val intent = actionEngine.deviceAdminHelper.getActivateAdminIntent()
                    getApplication<Application>().startActivity(intent)
                }
            )
        }

        val assistantMsg = ChatMessage(
            sender = MessageSender.NOVA,
            text = res.spokenResponse,
            actionType = res.actionType,
            actionDescription = res.details
        )
        _messages.value = _messages.value + assistantMsg

        if (_settings.value.autoSpeakResponse) {
            voiceOutputManager.speak(res.spokenResponse, _settings.value.selectedLanguage)
        } else {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }

    fun dismissRestrictionDialog() {
        _restrictionDialog.value = null
    }

    fun replaySpeech(text: String) {
        voiceOutputManager.speak(text, _settings.value.selectedLanguage)
    }

    fun testVoice(pitch: Float, speed: Float) {
        voiceOutputManager.setVoiceAttributes(pitch, speed)
        voiceOutputManager.speak(
            "Hello! I am ${_settings.value.assistantName}. How does this voice sound?",
            _settings.value.selectedLanguage
        )
    }

    fun updateSettings(newSettings: AssistantSettings) {
        _settings.value = newSettings
        settingsRepo.saveSettings(newSettings)
        voiceOutputManager.setVoiceAttributes(newSettings.voicePitch, newSettings.voiceSpeed)

        if (newSettings.continuousWakeMode) {
            NovaVoiceService.start(getApplication())
        } else {
            NovaVoiceService.stop(getApplication())
        }
    }

    fun refreshDeviceAdminStatus() {
        _isDeviceAdminActive.value = actionEngine.deviceAdminHelper.isDeviceAdminActive()
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        voiceInputManager.destroy()
        voiceOutputManager.shutdown()
    }
}
