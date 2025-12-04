package com.cs407.soundscape.data

data class SoundEvent(
    val id: String = "", // Firestore document ID
    val userId: String = "", // Firebase Auth UID
    val label: String = "",
    val timestamp: Long = 0L,
    val decibelLevel: Float = 0f,
    val environment: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val duration: Long = 0L // in milliseconds
)

