package com.cs407.soundscape.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlin.math.log10
import kotlin.math.sqrt

class ScanViewModel(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val soundEventRepository: SoundEventRepository
) : ViewModel() {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _decibelLevel = MutableStateFlow(0f)
    val decibelLevel = _decibelLevel.asStateFlow()

    private val _averageDecibel = MutableStateFlow<Float?>(null)
    val averageDecibel = _averageDecibel.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(10)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    // Environment state
    private val _selectedEnvironment = MutableStateFlow<String?>(null)
    val selectedEnvironment = _selectedEnvironment.asStateFlow()

    private val _customEnvironment = MutableStateFlow("")
    val customEnvironment = _customEnvironment.asStateFlow()

    // Location state
    private val _currentLatitude = MutableStateFlow<Double?>(null)
    val currentLatitude = _currentLatitude.asStateFlow()

    private val _currentLongitude = MutableStateFlow<Double?>(null)
    val currentLongitude = _currentLongitude.asStateFlow()

    // Permission state
    private val _microphonePermissionGranted = MutableStateFlow(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    )
    val microphonePermissionGranted = _microphonePermissionGranted.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    )
    val locationPermissionGranted = _locationPermissionGranted.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied = _permissionDenied.asStateFlow()

    // Error and success messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _saveSuccessMessage = MutableStateFlow<String?>(null)
    val saveSuccessMessage = _saveSuccessMessage.asStateFlow()

    // Saving state
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    // Recording internals
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var decibelAccumulation = 0f
    private var decibelSampleCount = 0
    private var recordingStartTime: Long? = null
    private var isSavingInProgress = false // Prevent multiple saves for the same recording

    private val recordingSampleRate = 44100
    private val minBufferSize: Int by lazy {
        val size = AudioRecord.getMinBufferSize(
            recordingSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (size <= 0) recordingSampleRate else size
    }

    // Common environments list
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

    fun updateMicrophonePermission(granted: Boolean) {
        _microphonePermissionGranted.value = granted
        _permissionDenied.value = !granted
        if (granted) {
            _permissionDenied.value = false
            _errorMessage.value = null
        }
    }

    fun updateLocationPermission(granted: Boolean) {
        _locationPermissionGranted.value = granted
        if (granted) {
            viewModelScope.launch {
                getCurrentLocation()
            }
        }
    }

    fun setSelectedEnvironment(environment: String?) {
        _selectedEnvironment.value = environment
        if (environment == "Other") {
            _selectedEnvironment.value = null
            _customEnvironment.value = ""
        }
    }

    fun setCustomEnvironment(environment: String) {
        _customEnvironment.value = environment
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _saveSuccessMessage.value = null
    }

    private suspend fun getCurrentLocation() {
        if (!_locationPermissionGranted.value) return

        try {
            val cancellationTokenSource = CancellationTokenSource()
            val locationResult = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            val location: Location? = locationResult.await()
            location?.let {
                _currentLatitude.value = it.latitude
                _currentLongitude.value = it.longitude
            }
        } catch (e: Exception) {
            // Location unavailable, continue without it
            _currentLatitude.value = null
            _currentLongitude.value = null
        }
    }

    fun startRecording() {
        if (!_microphonePermissionGranted.value) {
            // Permission will be requested from UI
            return
        }
        beginRecordingSession()
    }

    private fun beginRecordingSession() {
        if (_isRecording.value) return

        _remainingSeconds.value = 10
        _averageDecibel.value = null
        decibelAccumulation = 0f
        decibelSampleCount = 0
        _decibelLevel.value = 0f
        _permissionDenied.value = false
        _errorMessage.value = null
        _saveSuccessMessage.value = null
        isSavingInProgress = false // Reset save flag when starting new recording
        recordingStartTime = System.currentTimeMillis()

        // Get location if permission is granted
        if (_locationPermissionGranted.value) {
            viewModelScope.launch {
                getCurrentLocation()
            }
        }

        val audioRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            recordingSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        if (audioRecorder.state != AudioRecord.STATE_INITIALIZED) {
            audioRecorder.release()
            _errorMessage.value = "Unable to access microphone"
            return
        }

        try {
            audioRecorder.startRecording()
        } catch (e: IllegalStateException) {
            audioRecorder.release()
            _errorMessage.value = "Failed to start recording: ${e.localizedMessage ?: "Unknown error"}"
            return
        }

        audioRecord = audioRecorder
        _isRecording.value = true

        recordingJob = viewModelScope.launch(Dispatchers.Default) {
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
                        _decibelLevel.value = decibels
                        decibelAccumulation += decibels
                        decibelSampleCount += 1
                    }
                }
            }
        }

        timerJob = viewModelScope.launch {
            for (second in 10 downTo 1) {
                _remainingSeconds.value = second
                delay(1_000)
                if (!_isRecording.value) return@launch
            }
            stopRecording(shouldCancelTimer = false)
        }
    }

    fun stopRecording(shouldCancelTimer: Boolean = true) {
        // If already stopped and not saving, just return (prevents duplicate saves)
        if (!_isRecording.value && !isSavingInProgress) {
            return
        }

        // Always perform cleanup
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

        // Calculate average decibel if we have samples
        if (decibelSampleCount > 0) {
            _averageDecibel.value = decibelAccumulation / decibelSampleCount
        }

        // Calculate duration
        val duration = recordingStartTime?.let {
            System.currentTimeMillis() - it
        } ?: 10000L

        _remainingSeconds.value = 0
        val wasRecording = _isRecording.value
        _isRecording.value = false
        
        // Only save if we were actually recording and haven't already started saving
        // This prevents duplicate saves when stopRecording is called multiple times
        if (wasRecording && !isSavingInProgress) {
            val userId = sessionManager.getUserId()
            if (userId != null && _averageDecibel.value != null) {
                saveRecordingToDatabase(userId, duration)
            } else if (userId == null) {
                // User not logged in - show message but don't block UI
                _errorMessage.value = "Please sign in to save recordings"
            }
        }
        
        // Clear recording start time after processing
        recordingStartTime = null
    }

    private fun saveRecordingToDatabase(userId: String, duration: Long) {
        // Prevent multiple simultaneous saves
        if (isSavingInProgress) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            // Set saving state
            isSavingInProgress = true
            _isSaving.value = true
            _errorMessage.value = null
            _saveSuccessMessage.value = null

            try {
                // Ensure location is fetched before saving
                if (_locationPermissionGranted.value && (_currentLatitude.value == null || _currentLongitude.value == null)) {
                    getCurrentLocation()
                    // Wait a bit for location to be fetched
                    delay(500)
                }

                val environmentName = _customEnvironment.value.takeIf { it.isNotBlank() }
                val eventLabel = environmentName ?: "Recording"

                val event = SoundEvent(
                    userId = userId,
                    label = eventLabel,
                    timestamp = System.currentTimeMillis(),
                    decibelLevel = _averageDecibel.value!!,
                    environment = environmentName,
                    latitude = _currentLatitude.value,
                    longitude = _currentLongitude.value,
                    duration = duration
                )

                // Save to database - Firestore has its own timeout handling
                val result = soundEventRepository.insert(event)
                
                // Handle Result
                if (result.isSuccess) {
                    // Success - Firestore confirmed the document was saved
                    // The event will appear in history via the snapshot listener
                    _saveSuccessMessage.value = "Recording saved successfully!"
                    // Clear success message after 3 seconds
                    viewModelScope.launch {
                        delay(3000)
                        _saveSuccessMessage.value = null
                    }
                } else {
                    // Failure case - insert failed
                    val exception = result.exceptionOrNull()
                    _errorMessage.value = "Failed to save recording: ${exception?.message ?: "Unknown error"}"
                }
                
                // CRITICAL: Always set isSaving to false after handling result
                _isSaving.value = false
                isSavingInProgress = false
            } catch (e: Exception) {
                // Handle any unexpected exceptions - ensure isSaving is always set to false
                _errorMessage.value = "Error saving recording: ${e.message ?: "Unknown error"}"
                _isSaving.value = false
                isSavingInProgress = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}

