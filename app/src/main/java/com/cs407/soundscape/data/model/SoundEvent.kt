package com.cs407.soundscape.data.model

import java.util.Date

/**
 * Data model for a sound event.
 * TODO: Replace with backend model when integrating Firebase/Supabase
 */
data class SoundEvent(
    val id: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Date,
    val soundType: SoundType,
    val decibelLevel: Float,
    val duration: Long, // in milliseconds
    val environment: String? = null, // e.g., "library", "café", "Memorial Library", "Gordon Commons"
    val userId: String? = null // TODO: Get from authenticated user when backend is integrated
)

enum class SoundType {
    TRAFFIC,
    NATURE,
    CONSTRUCTION,
    MUSIC,
    VOICE,
    ANIMAL,
    OTHER
}

