package com.cs407.soundscape.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val repo: UserRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        // Initialize with current user if logged in
        _currentUser.value = repo.getCurrentUser()
    }

    fun signIn(email: String, password: String, onSuccess: (FirebaseUser) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                val result = repo.signIn(email, password)
                result.onSuccess { user ->
                    _currentUser.value = user
                    onSuccess(user)
                }.onFailure { exception ->
                    _authError.value = exception.message ?: "Sign in failed"
                }
            } catch (e: Exception) {
                _authError.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, username: String, onSuccess: (FirebaseUser) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                val result = repo.signUp(email, password, username)
                result.onSuccess { user ->
                    _currentUser.value = user
                    onSuccess(user)
                }.onFailure { exception ->
                    _authError.value = exception.message ?: "Sign up failed"
                }
            } catch (e: Exception) {
                _authError.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        repo.signOut()
        _currentUser.value = null
        _authError.value = null
    }

    fun clearError() {
        _authError.value = null
    }
}
