package com.omersusin.sealora.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AiConfig(
    val provider: AiProvider = AiProvider.OPENROUTER,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = "",
    val isActive: Boolean = false
)

@Serializable
enum class AiProvider(val displayName: String, val defaultBaseUrl: String) {
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta"),
    OPENAI("OpenAI", "https://api.openai.com/v1")
}

fun AiProvider.defaultModels(): List<String> = when (this) {
    AiProvider.OPENROUTER -> listOf(
        "meta-llama/llama-3.1-8b-instruct:free",
        "google/gemma-2-9b-it:free",
        "mistralai/mistral-7b-instruct:free",
        "deepseek/deepseek-chat",
        "anthropic/claude-3.5-sonnet"
    )
    AiProvider.GROQ -> listOf(
        "llama-3.1-8b-instant",
        "llama-3.1-70b-versatile",
        "mixtral-8x7b-32768",
        "gemma-7b-it"
    )
    AiProvider.GEMINI -> listOf(
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-pro"
    )
    AiProvider.OPENAI -> listOf(
        "gpt-4o-mini",
        "gpt-4o",
        "gpt-3.5-turbo"
    )
}

@Serializable
data class AiChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AiChatSession(
    val id: String,
    val messages: List<AiChatMessage> = emptyList(),
    val city: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
