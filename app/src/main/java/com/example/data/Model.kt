package com.example.data

import java.util.UUID

enum class AssistantState(val label: String) {
    IDLE("Idle"),
    LISTENING("Listening..."),
    THINKING("Thinking..."),
    EXECUTING("Executing action..."),
    SPEAKING("Speaking...")
}

enum class MessageSender {
    USER,
    NOVA
}

enum class ActionType {
    OPEN_APP,
    PHONE_CALL,
    SEND_SMS,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    SET_ALARM,
    SET_TIMER,
    DO_NOT_DISTURB,
    BATTERY_SAVER,
    LOCK_SCREEN,
    SCREEN_ON,
    WEB_SEARCH,
    WEATHER,
    GENERAL_AI,
    RESTRICTION_EXPLANATION
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType? = null,
    val actionDescription: String? = null,
    val detectedLanguage: String? = null
)

data class AssistantSettings(
    val assistantName: String = "Nova",
    val wakeWordEnabled: Boolean = true,
    val voicePitch: Float = 1.05f,
    val voiceSpeed: Float = 1.0f,
    val selectedLanguage: String = "auto", // "auto", "en", "bn", "hi"
    val hapticFeedback: Boolean = true,
    val autoSpeakResponse: Boolean = true,
    val isDeviceAdminActive: Boolean = false,
    val continuousWakeMode: Boolean = false
)

data class PendingConfirmation(
    val title: String,
    val message: String,
    val actionType: ActionType,
    val onConfirm: () -> Unit
)
