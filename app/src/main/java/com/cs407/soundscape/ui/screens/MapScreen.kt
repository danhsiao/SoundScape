package com.cs407.soundscape.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

@Composable
fun MapScreen() {
    // Repository and base data
    val repository = remember { MockSoundRepository() }
    val allEvents = remember { repository.getAllEvents() }

    // Selected event shown in bottom sheet
    var selectedEvent by remember { mutableStateOf<SoundEvent?>(null) }

    // Legend filter toggles
    var showQuiet by remember { mutableStateOf(true) }
    var showModerate by remember { mutableStateOf(true) }
    var showLoud by remember { mutableStateOf(true) }

    // Apply filters to events used by heatmap and tap selection
    val filteredEvents = remember(allEvents, showQuiet, showModerate, showLoud) {
        allEvents.filter { e ->
            when (e.decibelLevel) {
                in Float.NEGATIVE_INFINITY..50f -> showQuiet
                in 50f..75f -> showModerate
                else -> showLoud
            }
        }
    }

    // Initial map target (fallback if no location yet)
    val startLatLng = if (allEvents.isNotEmpty())
        LatLng(allEvents.first().latitude, allEvents.first().longitude)
    else LatLng(37.7749, -122.4194)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 12f)
    }
    val scope = rememberCoroutineScope()

    // Permission check and "blue dot" setup
    val context = LocalContext.current
    val hasFine = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val locationEnabled = hasFine || hasCoarse

    // Last-known device location (when available)
    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }

    // Map UI controls and properties
    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = true, compassEnabled = true) }
    val mapProperties = remember(locationEnabled) { MapProperties(isMyLocationEnabled = locationEnabled) }

    // On first load, pan camera to device location if permission granted
    LaunchedEffect(locationEnabled) {
        if (locationEnabled) {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val here = LatLng(loc.latitude, loc.longitude)
                    currentLatLng = here
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(here, 13.5f)
                        )
                    }
                }
            }
        }
    }

    // Heatmap provider: weight by decibel + custom gradient
    val heatmapProvider: HeatmapTileProvider? = remember(filteredEvents) {
        if (filteredEvents.isEmpty()) return@remember null
        val minDb = 30f
        val maxDb = 100f
        val weighted = filteredEvents.map { e ->
            val normalized = ((e.decibelLevel - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
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
        // Map with heatmap overlay and tap-to-select behavior
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapClick = { tapLatLng ->
                selectedEvent = findNearestEvent(tapLatLng, filteredEvents, 200.0)
            }
        ) {
            heatmapProvider?.let { TileOverlay(tileProvider = it, zIndex = 1f) }
        }

        // Small legend card with three sound ranges and checkboxes
        LegendWithFilters(
            showQuiet = showQuiet, onQuietChange = { showQuiet = it },
            showModerate = showModerate, onModerateChange = { showModerate = it },
            showLoud = showLoud, onLoudChange = { showLoud = it },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )

        // FAB to recenter on current location (falls back to startLatLng)
        FloatingActionButton(
            onClick = {
                val target = currentLatLng ?: startLatLng
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(target, 13.5f)
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.5.dp, bottom = 100.dp)
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Recenter map")
        }

        // Bottom info panel for the selected event
        selectedEvent?.let { event ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                    .padding(16.dp)
            ) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${event.decibelLevel} dB • ${event.soundType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "📍 ${"%.4f".format(event.latitude)}, ${"%.4f".format(event.longitude)}",
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
) {
    // Legend container pinned to top-left
    Card(modifier = modifier.widthIn(max = 220.dp)) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(text = "Sound levels", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(5.dp))
            LegendFilterRow(HeatmapQuiet, "< 50 dB (quiet)", showQuiet, onQuietChange)
            Spacer(Modifier.height(5.dp))
            LegendFilterRow(HeatmapModerate, "50–75 dB (moderate)", showModerate, onModerateChange)
            Spacer(Modifier.height(5.dp))
            LegendFilterRow(HeatmapLoud, "> 75 dB (loud)", showLoud, onLoudChange)
        }
    }
}

@Composable
private fun LegendFilterRow(
    color: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // One legend row: swatch + label + checkbox
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.size(12.dp).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.size(18.dp))
    }
}

// Nearest-event lookup by distance to tap point (meters)
private fun findNearestEvent(tapLatLng: LatLng, events: List<SoundEvent>, maxDistanceMeters: Double): SoundEvent? {
    var nearest: SoundEvent? = null
    var nearestDist = Double.MAX_VALUE
    for (e in events) {
        val d = haversineMeters(tapLatLng.latitude, tapLatLng.longitude, e.latitude, e.longitude)
        if (d < nearestDist) {
            nearestDist = d
            nearest = e
        }
    }
    return if (nearestDist <= maxDistanceMeters) nearest else null
}

// Great-circle distance between two lat/lng points (haversine formula)
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
