package com.cs407.soundscape.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen() {
    var isRecording by remember { mutableStateOf(false) }
    var decibelLevel by remember { mutableStateOf(0f) }
    var averageDecibel by remember { mutableStateOf<Float?>(null) }
    var selectedEnvironment by remember { mutableStateOf<String?>(null) }
    var customEnvironment by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(10) }
    var permissionDenied by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var decibelAccumulation by remember { mutableStateOf(0f) }
    var decibelSampleCount by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val microphonePermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val recordingSampleRate = 44100
    val minBufferSize = remember {
        val size = AudioRecord.getMinBufferSize(
            recordingSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (size <= 0) recordingSampleRate else size
    }

    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    fun stopRecording(shouldCancelTimer: Boolean = true) {
        recordingJob?.cancel()
        recordingJob = null

        audioRecord?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        audioRecord = null

        if (shouldCancelTimer) {
            timerJob?.cancel()
        }
        timerJob = null

        if (decibelSampleCount > 0) {
            averageDecibel = decibelAccumulation / decibelSampleCount
        }

        remainingSeconds = 0
        isRecording = false
    }

    fun beginRecordingSession() {
        if (isRecording) return

        remainingSeconds = 10
        averageDecibel = null
        decibelAccumulation = 0f
        decibelSampleCount = 0
        decibelLevel = 0f
        permissionDenied = false
        errorMessage = null

        val audioRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            recordingSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        if (audioRecorder.state != AudioRecord.STATE_INITIALIZED) {
            audioRecorder.release()
            errorMessage = "Unable to access microphone"
            return
        }

        try {
            audioRecorder.startRecording()
        } catch (e: IllegalStateException) {
            audioRecorder.release()
            errorMessage = "Failed to start recording: ${e.localizedMessage ?: "Unknown error"}"
            return
        }

        audioRecord = audioRecorder
        isRecording = true

        recordingJob = coroutineScope.launch(Dispatchers.Default) {
            val buffer = ShortArray(minBufferSize)
            while (isActive) {
                val read = audioRecorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var sumSquares = 0.0
                    for (i in 0 until read) {
                        val normalized = buffer[i] / 32768.0
                        sumSquares += normalized * normalized
                    }
                    val meanSquare = sumSquares / read
                    val amplitude = sqrt(meanSquare)
                    val decibels = if (amplitude <= 0.00001) {
                        0f
                    } else {
                        (20 * log10(amplitude) + 94).toFloat().coerceIn(0f, 120f)
                    }

                    withContext(Dispatchers.Main) {
                        decibelLevel = decibels
                        decibelAccumulation += decibels
                        decibelSampleCount += 1
                    }
                }
            }
        }

        timerJob = coroutineScope.launch {
            for (second in 10 downTo 1) {
                remainingSeconds = second
                delay(1_000)
                if (!isRecording) return@launch
            }
            stopRecording(shouldCancelTimer = false)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            microphonePermissionGranted.value = granted
            permissionDenied = !granted
            if (granted) {
                permissionDenied = false
                errorMessage = null
                beginRecordingSession()
            }
        }
    )

    fun startRecording() {
        if (!microphonePermissionGranted.value) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            beginRecordingSession()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopRecording()
        }
    }
    
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
            text = when {
                isRecording -> "Recording... ${remainingSeconds}s left"
                averageDecibel != null -> "Recording complete"
                else -> "Ready to Scan"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (averageDecibel != null && !isRecording) {
            Text(
                text = "Average noise level: ${String.format("%.1f", averageDecibel)} dB",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

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
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
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

        if (permissionDenied) {
            Text(
                text = "Microphone permission is required to record noise levels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (!microphonePermissionGranted.value && !permissionDenied && !isRecording) {
            Text(
                text = "Tap start to allow microphone access for recording.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
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

