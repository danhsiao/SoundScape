//package com.cs407.soundscape.ui.screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material.icons.filled.VisibilityOff
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import androidx.compose.ui.unit.dp
//import com.cs407.noteapp.auth.EmailResult
//import com.cs407.noteapp.auth.PasswordResult
//import com.cs407.noteapp.auth.checkPassword
//import com.cs407.noteapp.auth.signIn
//import com.cs407.noteapp.auth.validateEmail
//import com.cs407.soundscape.data.model.UserState
//
//@Composable
//fun EmailField(
//    value: String,
//    onValueChange: (String) -> Unit,
//    modifier: Modifier = Modifier,
//    label: String = "Enter Email"
//) {
//    OutlinedTextField(
//        value = value,
//        onValueChange = onValueChange,
//        modifier = modifier.fillMaxWidth(),
//        label = { Text(label) },
//        singleLine = true,
//        keyboardOptions = KeyboardOptions(
//            keyboardType = KeyboardType.Email,
//            imeAction = ImeAction.Next
//        )
//    )
//}
//
//@Composable
//fun PasswordField(
//    value: String,
//    onValueChange: (String) -> Unit,
//    modifier: Modifier = Modifier,
//    label: String = "Enter Password"
//) {
//    var showPassword by remember { mutableStateOf(false) }
//
//    OutlinedTextField(
//        value = value,
//        onValueChange = onValueChange,
//        modifier = modifier.fillMaxWidth(),
//        label = { Text(label) },
//        singleLine = true,
//        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
//        keyboardOptions = KeyboardOptions(
//            keyboardType = KeyboardType.Password,
//            imeAction = ImeAction.Done
//        ),
//        trailingIcon = {
//            IconButton(onClick = { showPassword = !showPassword }) {
//                Icon(
//                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
//                    contentDescription = if (showPassword) "Hide password" else "Show password"
//                )
//            }
//        }
//    )
//}
//
//@Composable
//fun ErrorText(message: String?, modifier: Modifier = Modifier) {
//    if (!message.isNullOrBlank()) {
//        Text(
//            text = message,
//            color = MaterialTheme.colorScheme.error,
//            style = MaterialTheme.typography.bodyMedium,
//            modifier = modifier.padding(top = 8.dp)
//        )
//    }
//}
//
//@Composable
//fun LoginPage(
//    modifier: Modifier = Modifier,
//    loginButtonClick: (UserState) -> Unit
//) {
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var error by remember { mutableStateOf<String?>(null) }
//
//    Scaffold(modifier) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .padding(innerPadding)
//                .fillMaxSize(),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            EmailField(value = email, onValueChange = { email = it; error = null })
//            Spacer(Modifier.height(8.dp))
//            PasswordField(value = password, onValueChange = { password = it; error = null })
//            ErrorText(message = error)
//            Spacer(Modifier.height(16.dp))
//
//            LogInSignUpButton(
//                email = email,
//                password = password,
//                onAuthSuccess = { userState -> loginButtonClick(userState) },
//                onAuthError = { msg -> error = msg }
//            )
//        }
//    }
//}
//
//@Composable
//fun LogInSignUpButton(
//    email: String,
//    password: String,
//    onAuthSuccess: (UserState) -> Unit,
//    onAuthError: (String) -> Unit
//) {
//    Button(
//        onClick = {
//            when (val er = validateEmail(email)) {
//                is EmailResult.Error -> { onAuthError(er.message); return@Button }
//                EmailResult.Valid -> {}
//            }
//            when (val pr = checkPassword(password)) {
//                is PasswordResult.Error -> { onAuthError(pr.message); return@Button }
//                PasswordResult.Valid -> {}
//            }
//            signIn(
//                email = email,
//                password = password,
//                onSuccess = { user ->
//                    onAuthSuccess(
//                        UserState(
//                            id = 0,
//                            name = user.displayName ?: (user.email ?: ""),
//                            uid = user.uid
//                        )
//                    )
//                },
//                onError = { msg -> onAuthError(msg) }
//            )
//        },
//        modifier = Modifier
//            .fillMaxWidth(0.8f)
//            .height(50.dp)
//    ) {
//        Text("Log In or Sign Up")
//    }
//}
