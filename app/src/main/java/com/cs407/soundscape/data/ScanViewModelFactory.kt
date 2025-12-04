package com.cs407.soundscape.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ScanViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val sessionManager = SessionManager()
        val soundEventRepository = SoundEventRepository()
        return ScanViewModel(context, sessionManager, soundEventRepository) as T
    }
}

