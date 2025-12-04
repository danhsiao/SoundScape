package com.cs407.soundscape.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Get current logged-in user
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Sign up with email and password
    suspend fun signUp(email: String, password: String, username: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            // Update user profile with display name
            user?.let {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                it.updateProfile(profileUpdates).await()
            }
            
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign in with email and password
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Get user ID
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    // Get username (display name)
    fun getUsername(): String? {
        return auth.currentUser?.displayName
    }

    // Get user email
    fun getEmail(): String? {
        return auth.currentUser?.email
    }
}
