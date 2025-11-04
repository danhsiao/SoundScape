package com.cs407.soundscape.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cs407.soundscape.data.model.SoundEvent
import com.cs407.soundscape.data.repository.MockSoundRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.heatmaps.HeatmapTileProvider

@Composable
fun MapScreen() {
    val repository = remember { MockSoundRepository() }
    val events: List<SoundEvent> = remember { repository.getAllEvents() }

    val startLatLng = if (events.isNotEmpty()) {
        LatLng(events.first().latitude, events.first().longitude)
    } else {
        LatLng(37.7749, -122.4194)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 12f)
    }

    val heatmapProvider = remember(events) {
        val points = events.map { LatLng(it.latitude, it.longitude) }
        HeatmapTileProvider.Builder()
            .data(points)
            .radius(50)
            .opacity(0.8)
            .build()
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        TileOverlay(
            tileProvider = heatmapProvider,
            zIndex = 1f
        )

        events.forEach { event ->
            val markerState = rememberMarkerState(
                position = LatLng(event.latitude, event.longitude)
            )
            Marker(
                state = markerState,
                title = event.title,
                snippet = "${event.decibelLevel} dB • ${event.soundType.name}"
            )
        }
    }
}
