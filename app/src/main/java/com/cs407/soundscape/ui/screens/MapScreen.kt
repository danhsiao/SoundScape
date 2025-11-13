package com.cs407.soundscape.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cs407.soundscape.data.model.SoundEvent
import com.cs407.soundscape.data.repository.MockSoundRepository
import com.cs407.soundscape.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.graphics.toArgb

@SuppressLint("MissingPermission") // we gate calls on hasLocationPermission
@Composable
fun MapScreen() {
    val ctx = LocalContext.current
    val repository = remember { MockSoundRepository() }
    val allEvents = remember { repository.getAllEvents() }

    // selected bottom-sheet event
    var selectedEvent by remember { mutableStateOf<SoundEvent?>(null) }

    // location permission state
    var hasLocationPermission by remember { mutableStateOf(false) }

    // device location (if we get it)
    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }

    // filters
    var showQuiet by remember { mutableStateOf(true) }     // < 50
    var showModerate by remember { mutableStateOf(true) }  // 50–75
    var showLoud by remember { mutableStateOf(true) }      // > 75

    val filteredEvents = remember(allEvents, showQuiet, showModerate, showLoud) {
        allEvents.filter { e ->
            val dB = e.decibelLevel
            when {
                dB < 50f -> showQuiet
                dB in 50f..75f -> showModerate
                else -> showLoud
            }
        }
    }

    // start camera: use current location if we get it; otherwise first event or SF
    val fallbackLatLng = if (allEvents.isNotEmpty()) {
        LatLng(allEvents.first().latitude, allEvents.first().longitude)
    } else LatLng(37.7749, -122.4194)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fallbackLatLng, 12f)
    }
    val scope = rememberCoroutineScope()

    // request permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse
    }

    // kick off permission request once
    LaunchedEffect(Unit) {
        permLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // once we have permission, fetch last known location and animate
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fused = LocationServices.getFusedLocationProviderClient(ctx)
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    loc?.let {
                        val here = LatLng(it.latitude, it.longitude)
                        currentLatLng = here
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(here, 14f)
                            )
                        }
                    }
                }
        }
    }

    // map properties: enable My Location (blue dot) when allowed
    val mapProperties by remember(hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasLocationPermission
            )
        )
    }
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = true

        )
    }

    // heatmap provider from filtered events
    val heatmapProvider = remember(filteredEvents) {
        val minDb = 30f
        val maxDb = 100f
        val weighted = filteredEvents.map { e ->
            val normalized = ((e.decibelLevel - minDb) / (maxDb - minDb))
                .coerceIn(0f, 1f)
            val weight = 1.0 + normalized * 9.0
            WeightedLatLng(LatLng(e.latitude, e.longitude), weight)
        }

        val colors = intArrayOf(
            HeatmapGradientGreen.toArgb(),
            HeatmapGradientOrange.toArgb(),
            HeatmapGradientRed.toArgb()
        )
        val startPoints = floatArrayOf(0.1f, 0.5f, 1.0f)
        val gradient = Gradient(colors, startPoints)

        HeatmapTileProvider.Builder()
            .weightedData(weighted)
            .gradient(gradient)
            .radius(50)
            .opacity(1.0)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = { tapLatLng ->
                selectedEvent = findNearestEvent(
                    tapLatLng = tapLatLng,
                    events = filteredEvents,
                    maxDistanceMeters = 200.0
                )
            }
        ) {
            TileOverlay(tileProvider = heatmapProvider, zIndex = 1f)
        }

        // Legend + filters
        LegendWithFilters(
            showQuiet = showQuiet,
            onQuietChange = { showQuiet = it },
            showModerate = showModerate,
            onModerateChange = { showModerate = it },
            showLoud = showLoud,
            onLoudChange = { showLoud = it },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        // Recenter FAB (uses current location if available; else fallback)
        FloatingActionButton(
            onClick = {
                val target = currentLatLng ?: fallbackLatLng
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(target, if (currentLatLng != null) 14f else 12f)
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 3.5.dp, bottom = 100.dp)
        ) {
            Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Recenter map")
        }

        // Bottom info panel
        selectedEvent?.let { event ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                    .padding(16.dp)
            ) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${event.decibelLevel} dB • ${event.soundType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "📍 ${"%.4f".format(event.latitude)}, ${"%.4f".format(event.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendWithFilters(
    showQuiet: Boolean,
    onQuietChange: (Boolean) -> Unit,
    showModerate: Boolean,
    onModerateChange: (Boolean) -> Unit,
    showLoud: Boolean,
    onLoudChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) { /* ... keep your existing code ... */ }

@Composable
private fun LegendFilterRow(
    color: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) { /* ... keep your existing code ... */ }

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val sinLat = sin(dLat / 2)
    val sinLon = sin(dLon / 2)
    val a = sinLat * sinLat +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sinLon * sinLon
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

private fun findNearestEvent(
    tapLatLng: LatLng,
    events: List<SoundEvent>,
    maxDistanceMeters: Double
    ): SoundEvent? {
        var nearest: SoundEvent? = null
        var nearestDist = Double.MAX_VALUE
        for (e in events) {
            val d = haversineMeters(tapLatLng.latitude, tapLatLng.longitude, e.latitude, e.longitude)
            if (d < nearestDist) { nearestDist = d; nearest = e }
        }
        return if (nearestDist <= maxDistanceMeters) nearest else null
    }
