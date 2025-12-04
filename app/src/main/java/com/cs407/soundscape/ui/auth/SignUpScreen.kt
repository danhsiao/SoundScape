package com.cs407.soundscape.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.soundscape.data.UserViewModel
import com.cs407.soundscape.data.UserViewModelFactory

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory())

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authError by userViewModel.authError.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    var passwordMismatchError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                passwordMismatchError = false
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it
                passwordMismatchError = false
            },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            enabled = !isLoading
        )

        if (passwordMismatchError) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        authError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                    if (password != confirmPassword) {
                        passwordMismatchError = true
                        return@Button
                    }
                    userViewModel.signUp(email, password, username) { _ ->
                        onSignUpSuccess()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToSignIn) {
            Text("Already have an account? Sign In")
        }
    }
}
