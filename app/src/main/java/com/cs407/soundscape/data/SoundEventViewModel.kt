package com.cs407.soundscape.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SoundEventViewModel(
    private val repo: SoundEventRepository,
    private val userId: String
) : ViewModel() {

    val events = repo.getAllByUserId(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSampleEvent() {
        viewModelScope.launch {
            repo.insert(
                SoundEvent(
                    userId = userId,
                    label = "Test Event",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertEvent(label: String, timestamp: Long) {
        repo.insert(
            SoundEvent(
                userId = userId,
                label = label,
                timestamp = timestamp
            )
        )
    }
}
