//package com.cs407.noteapp.auth
//
//import com.google.firebase.auth.FirebaseUser
//import com.google.firebase.Firebase
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.auth
//
//sealed class EmailResult {
//    object Valid : EmailResult()
//    data class Error(val message: String) : EmailResult()
//}
//
//sealed class PasswordResult {
//    object Valid : PasswordResult()
//    data class Error(val message: String) : PasswordResult()
//}
//
//fun validateEmail(email: String): EmailResult {
//    if (email.isEmpty()) return EmailResult.Error("Email is empty")
//
//    val pattern = Regex("^[\\w.]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")
//    return if (pattern.matches(email)) {
//        EmailResult.Valid
//    } else {
//        EmailResult.Error("Invalid Email Format")
//    }
//}
//
//fun checkPassword(password: String): PasswordResult {
//    if (password.isEmpty()) return PasswordResult.Error("Password is empty")
//    if (password.length < 6) return PasswordResult.Error("Password is too short")
//
//    val hasDigit = Regex("\\d")
//    val hasLower = Regex("[a-z]")
//    val hasUpper = Regex("[A-Z]")
//
//    return if (
//        hasDigit.containsMatchIn(password) &&
//        hasLower.containsMatchIn(password) &&
//        hasUpper.containsMatchIn(password)
//    ) {
//        PasswordResult.Valid
//    } else {
//        PasswordResult.Error(
//            "Password should contain at least one lowercase letter, one uppercase letter, and one digit"
//        )
//    }
//}
//
//fun createAccount(
//    email: String,
//    password: String,
//    onSuccess: (FirebaseUser) -> Unit,
//    onError: (String) -> Unit
//) {
//    val auth: FirebaseAuth = FirebaseAuth.getInstance()
//    auth.createUserWithEmailAndPassword(email, password)
//        .addOnSuccessListener {
//            val user = auth.currentUser
//            if (user != null) onSuccess(user) else onError("Authentication failed.")
//        }
//        .addOnFailureListener { e ->
//            onError(e.localizedMessage ?: "Authentication failed.")
//        }
//}
//
//fun signIn(
//    email: String,
//    password: String,
//    onSuccess: (FirebaseUser) -> Unit,
//    onError: (String) -> Unit
//) {
//    val auth = Firebase.auth
//    auth.signInWithEmailAndPassword(email, password)
//        .addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val user = auth.currentUser
//                if (user != null) onSuccess(user) else onError("Authentication failed.")
//            } else {
//                createAccount(
//                    email = email,
//                    password = password,
//                    onSuccess = onSuccess,
//                    onError = onError
//                )
//            }
//        }
//}
