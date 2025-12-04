package com.cs407.soundscape.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UserViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = SoundscapeDatabase.getDatabase(context)
        val repo = UserRepository(db.userDao())
        return UserViewModel(repo) as T
    }
}

