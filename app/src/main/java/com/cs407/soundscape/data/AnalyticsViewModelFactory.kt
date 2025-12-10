package com.cs407.soundscape.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AnalyticsViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = SoundEventRepository()
        return AnalyticsViewModel(repo) as T
    }
}


