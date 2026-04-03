package com.omersusin.sealora.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val baseUrl: String,
    val urlTemplate: String,
    val type: String,
    val isActive: Boolean,
    val requiresApiKey: Boolean,
    val apiKey: String,
    val language: String,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "ai_configs")
data class AiConfigEntity(
    @PrimaryKey
    val provider: String,
    val apiKey: String,
    val model: String,
    val baseUrl: String,
    val isActive: Boolean
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val city: String,
    val messagesJson: String,
    val createdAt: Long
)

@Entity(tableName = "cached_weather")
data class CachedWeatherEntity(
    @PrimaryKey
    val id: String,
    val city: String,
    val providerId: String,
    val dataJson: String,
    val fetchedAt: Long
)
