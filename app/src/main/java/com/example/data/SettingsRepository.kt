package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ASSISTANT_NAME = "key_assistant_name"
        private const val KEY_WAKE_WORD = "key_wake_word"
        private const val KEY_PITCH = "key_pitch"
        private const val KEY_SPEED = "key_speed"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_CONTINUOUS_WAKE = "key_continuous_wake"
    }

    fun loadSettings(): AssistantSettings {
        return AssistantSettings(
            assistantName = prefs.getString(KEY_ASSISTANT_NAME, "Nova") ?: "Nova",
            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD, true),
            voicePitch = prefs.getFloat(KEY_PITCH, 1.05f),
            voiceSpeed = prefs.getFloat(KEY_SPEED, 1.0f),
            selectedLanguage = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto",
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            autoSpeakResponse = prefs.getBoolean(KEY_AUTO_SPEAK, true),
            continuousWakeMode = prefs.getBoolean(KEY_CONTINUOUS_WAKE, false)
        )
    }

    fun saveSettings(settings: AssistantSettings) {
        prefs.edit()
            .putString(KEY_ASSISTANT_NAME, settings.assistantName)
            .putBoolean(KEY_WAKE_WORD, settings.wakeWordEnabled)
            .putFloat(KEY_PITCH, settings.voicePitch)
            .putFloat(KEY_SPEED, settings.voiceSpeed)
            .putString(KEY_LANGUAGE, settings.selectedLanguage)
            .putBoolean(KEY_HAPTIC, settings.hapticFeedback)
            .putBoolean(KEY_AUTO_SPEAK, settings.autoSpeakResponse)
            .putBoolean(KEY_CONTINUOUS_WAKE, settings.continuousWakeMode)
            .apply()
    }
}
