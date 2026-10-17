package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiBrain {

    private val systemPrompt = """
        You are NOVA, a friendly, intelligent, fast, and helpful personal voice assistant.
        You support Bengali (বাংলা), English, Hindi (हिन्दी), and Banglish (Bengali written in English letters).
        Guidelines:
        1. Because your responses are spoken out loud via Android Text-To-Speech, keep answers natural, conversational, and direct (1-3 sentences for regular Q&A).
        2. If the user asks for code, summaries, or study help, provide clear and structured help.
        3. Match the user's language (if spoken in Bengali, reply in Bengali; if in Banglish, reply in friendly Banglish or Bengali; if in Hindi, reply in Hindi; if in English, reply in English).
        4. Personality: Warm, intelligent, respectful, fast, and delightful.
    """.trimIndent()

    suspend fun answerQuestion(
        userMessage: String,
        assistantName: String = "Nova",
        conversationHistory: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local fallback when API key is not yet configured by user
            return@withContext getLocalFallbackResponse(userMessage, assistantName)
        }

        try {
            // Take up to 6 recent messages for conversational context
            val recentMessages = conversationHistory.takeLast(6).map { msg ->
                GeminiContent(
                    role = if (msg.sender == MessageSender.USER) "user" else "model",
                    parts = listOf(GeminiPart(text = msg.text))
                )
            }.toMutableList()

            // Append current message
            recentMessages.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userMessage))
                )
            )

            val request = GeminiRequest(
                contents = recentMessages,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt.replace("NOVA", assistantName)))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 400
                )
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!reply.isNullOrBlank()) {
                reply.trim()
            } else {
                getLocalFallbackResponse(userMessage, assistantName)
            }
        } catch (e: Exception) {
            Log.e("GeminiBrain", "API call failed: ${e.message}", e)
            getLocalFallbackResponse(userMessage, assistantName)
        }
    }

    private fun getLocalFallbackResponse(query: String, name: String): String {
        val lower = query.lowercase().trim()

        // Multilingual greetings & personality
        return when {
            lower.contains("who are you") || lower.contains("what is your name") ->
                "I am $name, your personal AI voice assistant. I'm here to help you control your phone, answer questions, and make your day smoother."

            lower.contains("তোমার নাম কি") || lower.contains("কে তুমি") ->
                "আমি $name, আপনার ব্যক্তিগত কৃত্রিম বুদ্ধিমত্তা সম্পন্ন ভয়েস অ্যাসিস্ট্যান্ট। আমি আপনাকে কীভাবে সাহায্য করতে পারি?"

            lower.contains("tumhara naam kya hai") || lower.contains("aap kaun ho") || lower.contains("तुम्हारा नाम क्या है") ->
                "मेरा नाम $name है। मैं आपकी व्यक्तिगत एआई वॉयस असिस्टेंट हूँ।"

            lower.contains("kemon acho") || lower.contains("কেমন আছো") || lower.contains("how are you") ->
                "আমি দারুণ আছি! আশা করি আপনার দিনটিও খুব সুন্দর কাটছে। বলুন, কী সাহায্য করতে পারি?"

            lower.contains("kaise ho") || lower.contains("कैसे हो") ->
                "मैं बहुत अच्छी हूँ! बताइए मैं आपकी क्या सहायता कर सकती हूँ?"

            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello! How can I help you today?"

            lower.contains("thank you") || lower.contains("thanks") || lower.contains("ধন্যবাদ") || lower.contains("shukriya") ->
                "You're very welcome! Always happy to assist."

            lower.contains("joke") || lower.contains("কৌতুক") || lower.contains("হাসাও") ->
                "Why don't scientists trust atoms? Because they make up everything!"

            lower.contains("weather") || lower.contains("আবহাওয়া") || lower.contains("mausam") ->
                "I can open your weather app or search local forecasts for you. Just say: 'search weather today'!"

            else ->
                "I heard you! To answer complex questions or search in-depth, please connect your Gemini API key in the Secrets panel, or ask me to control your phone settings, flashlight, volume, alarms, timers, and apps!"
        }
    }
}
