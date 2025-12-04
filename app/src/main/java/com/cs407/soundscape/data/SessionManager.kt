package com.cs407.soundscape.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SessionManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Get current user
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Get user ID
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    // Get username (display name)
    fun getUsername(): String? {
        return auth.currentUser?.displayName
    }

    // Get email
    fun getEmail(): String? {
        return auth.currentUser?.email
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Clear session (sign out)
    fun clearSession() {
        auth.signOut()
    }

    // Add auth state listener
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    // Remove auth state listener
    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
