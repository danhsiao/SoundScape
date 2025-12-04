package com.cs407.soundscape.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SoundEventViewModelFactory(
    private val context: Context,
    private val userId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = SoundscapeDatabase.getDatabase(context)
        val repo = SoundEventRepository(db.soundEventDao())
        return SoundEventViewModel(repo, userId) as T
    }
}
