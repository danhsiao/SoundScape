package com.cs407.soundscape.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val repo: UserRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun signIn(username: String, password: String, onSuccess: (UserEntity) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                val user = repo.getUserByCredentials(username, password)
                if (user != null) {
                    _currentUser.value = user
                    onSuccess(user)
                } else {
                    _authError.value = "Invalid username or password"
                }
            } catch (e: Exception) {
                _authError.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(username: String, password: String, email: String? = null, onSuccess: (UserEntity) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            try {
                // Check if username already exists
                val existingUser = repo.getUserByUsername(username)
                if (existingUser != null) {
                    _authError.value = "Username already exists"
                    return@launch
                }

                // Create new user
                val userIdLong = repo.insertUser(
                    UserEntity(
                        username = username,
                        password = password,
                        email = email
                    )
                )

                // Get the created user (insert returns the row ID)
                val newUser = repo.getUserByIdSync(userIdLong.toInt())
                if (newUser != null) {
                    _currentUser.value = newUser
                    onSuccess(newUser)
                } else {
                    _authError.value = "Failed to create user"
                }
            } catch (e: Exception) {
                _authError.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        _currentUser.value = null
        _authError.value = null
    }

    fun clearError() {
        _authError.value = null
    }
}

