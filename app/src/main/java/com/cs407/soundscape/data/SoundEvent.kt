package com.cs407.soundscape.data

data class SoundEvent(
    val id: String = "", // Firestore document ID
    val userId: String = "", // Firebase Auth UID
    val label: String = "",
    val timestamp: Long = 0L
)

