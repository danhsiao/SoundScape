package com.cs407.soundscape.data.repository

import android.content.Context
import android.util.Patterns
import java.io.File

data class AuthResult(
    val success: Boolean,
    val message: String? = null
)

class AuthRepository(private val context: Context) {
    private val fileName = "users.txt"

    fun register(email: String, password: String): AuthResult {
        val validation = validateCredentials(email, password)
        if (!validation.success) return validation

        val users = readUsers()
        if (users.containsKey(email.lowercase())) {
            return AuthResult(false, "Account already exists. Please sign in.")
        }

        appendUser(email, password)
        return AuthResult(true)
    }

    fun login(email: String, password: String): AuthResult {
        val users = readUsers()
        val storedPassword = users[email.lowercase()]

        return if (storedPassword != null && storedPassword == password) {
            AuthResult(true)
        } else {
            AuthResult(false, "Invalid email or password.")
        }
    }

    private fun validateCredentials(email: String, password: String): AuthResult {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult(false, "Enter a valid email address.")
        }
        if (password.length < 6) {
            return AuthResult(false, "Password must be at least 6 characters.")
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return AuthResult(false, "Password needs at least one letter and one number.")
        }
        return AuthResult(true)
    }

    private fun readUsers(): Map<String, String> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyMap()

        return file.readLines()
            .mapNotNull { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) parts[0].lowercase() to parts[1] else null
            }
            .toMap()
    }

    private fun appendUser(email: String, password: String) {
        val file = File(context.filesDir, fileName)
        file.appendText("${email.lowercase()}:$password\n")
    }
}
