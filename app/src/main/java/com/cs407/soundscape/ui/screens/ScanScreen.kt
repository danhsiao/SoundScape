package com.cs407.soundscape.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen() {
    var isRecording by remember { mutableStateOf(false) }
    var decibelLevel by remember { mutableStateOf(0f) }
    var selectedEnvironment by remember { mutableStateOf<String?>(null) }
    var customEnvironment by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    
    val commonEnvironments = listOf(
        "Memorial Library",
        "College Library",
        "Steenbock Library",
        "Gordon Commons",
        "Union South",
        "Memorial Union",
        "Engineering Hall",
        "Bascom Hill",
        "Café",
        "Study Room",
        "Outdoor Space",
        "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Scan Environment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.padding(8.dp))

        Card(
            modifier = Modifier.size(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) 
                    MaterialTheme.colorScheme.errorContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.padding(16.dp))
                        Text(
                            text = "${String.format("%.1f", decibelLevel)} dB",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = if (isRecording) "Recording (5-10s)..." else "Ready to Scan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Environment selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Label Environment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Box {
                    OutlinedTextField(
                        value = selectedEnvironment ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select location") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDropdown = true },
                        colors = TextFieldDefaults.outlinedTextFieldColors()
                    )
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonEnvironments.forEach { env ->
                            DropdownMenuItem(
                                text = { Text(env) },
                                onClick = {
                                    selectedEnvironment = if (env == "Other") null else env
                                    if (env == "Other") {
                                        customEnvironment = ""
                                    }
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedEnvironment == null || selectedEnvironment == "Other") {
                    Spacer(modifier = Modifier.padding(8.dp))
                    OutlinedTextField(
                        value = customEnvironment,
                        onValueChange = { customEnvironment = it },
                        label = { Text("Custom location name") },
                        placeholder = { Text("e.g., library, café") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors()
                    )
                }
            }
        }

        Button(
            onClick = {
                isRecording = !isRecording
                // TODO: Start/stop audio recording when backend is integrated
                // TODO: Request microphone permission
                // TODO: Use AudioRecord or MediaRecorder API
                // TODO: Calculate decibel levels from audio buffer
                // TODO: Capture GPS location
                // TODO: Save to backend with environment label
                if (isRecording) {
                    // Simulate decibel reading
                    decibelLevel = 65.5f
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRecording) "Stop Recording" else "Start Recording (5-10s)",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        if (!isRecording && (selectedEnvironment != null || customEnvironment.isNotBlank())) {
            Text(
                text = "📍 Location: ${selectedEnvironment ?: customEnvironment}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

