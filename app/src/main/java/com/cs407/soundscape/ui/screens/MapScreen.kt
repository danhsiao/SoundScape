package com.cs407.soundscape.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cs407.soundscape.data.model.SoundEvent
import com.cs407.soundscape.data.repository.MockSoundRepository
import com.cs407.soundscape.ui.theme.HeatmapGradientGreen
import com.cs407.soundscape.ui.theme.HeatmapGradientOrange
import com.cs407.soundscape.ui.theme.HeatmapGradientRed
import com.cs407.soundscape.ui.theme.HeatmapLoud
import com.cs407.soundscape.ui.theme.HeatmapModerate
import com.cs407.soundscape.ui.theme.HeatmapQuiet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.graphics.toArgb


@Composable
fun MapScreen() {
    val repository = remember { MockSoundRepository() }
    val allEvents = remember { repository.getAllEvents() }

    var selectedEvent by remember { mutableStateOf<SoundEvent?>(null) }

    // sound-range filters (default: all on)
    var showQuiet by remember { mutableStateOf(true) }     // < 50
    var showModerate by remember { mutableStateOf(true) }  // 50–75
    var showLoud by remember { mutableStateOf(true) }      // > 75

    // apply filters
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

    val startLatLng = if (allEvents.isNotEmpty()) {
        LatLng(allEvents.first().latitude, allEvents.first().longitude)
    } else {
        LatLng(37.7749, -122.4194)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 12f)
    }
    val scope = rememberCoroutineScope()

    // build heatmap from filtered events
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
            onMapClick = { tapLatLng ->
                // when filtered, only allow clicking filtered events
                selectedEvent = findNearestEvent(
                    tapLatLng = tapLatLng,
                    events = filteredEvents,
                    maxDistanceMeters = 200.0
                )
            }
        ) {
            TileOverlay(
                tileProvider = heatmapProvider,
                zIndex = 1f
            )
        }

        // legend + filters
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

        // recenter FAB
        FloatingActionButton(
            onClick = {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(startLatLng, 12f)
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 3.5.dp, bottom = 100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter map"
            )
        }

        // bottom info panel
        selectedEvent?.let { event ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                    .padding(16.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium
                )
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
) {
    Card(
        modifier = modifier.widthIn(max = 220.dp),
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = "Sound levels",
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.height(5.dp))

            LegendFilterRow(
                color = HeatmapQuiet,
                label = "< 50 dB (quiet)",
                checked = showQuiet,
                onCheckedChange = onQuietChange
            )
            Spacer(Modifier.height(5.dp))
            LegendFilterRow(
                color = HeatmapModerate,
                label = "50–75 dB (moderate)",
                checked = showModerate,
                onCheckedChange = onModerateChange
            )
            Spacer(Modifier.height(5.dp))
            LegendFilterRow(
                color = HeatmapLoud,
                label = "> 75 dB (loud)",
                checked = showLoud,
                onCheckedChange = onLoudChange
            )
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(18.dp)
        )
    }
}


// Calculates the distance in meters between two lat/lng points on Earth using the haversine formula.
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

/**
 * Pick the closest event to the place the user tapped.
 * If it's farther than maxDistanceMeters, return null.
 */
private fun findNearestEvent(
    tapLatLng: LatLng,
    events: List<SoundEvent>,
    maxDistanceMeters: Double
): SoundEvent? {
    var nearest: SoundEvent? = null
    var nearestDist = Double.MAX_VALUE

    for (e in events) {
        val d = haversineMeters(
            tapLatLng.latitude, tapLatLng.longitude,
            e.latitude, e.longitude
        )
        if (d < nearestDist) {
            nearestDist = d
            nearest = e
        }
    }

    return if (nearestDist <= maxDistanceMeters) nearest else null
}
