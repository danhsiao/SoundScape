package com.cs407.soundscape.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SoundEventViewModelFactory(
    private val userId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = SoundEventRepository()
        return SoundEventViewModel(repo, userId) as T
    }
}
