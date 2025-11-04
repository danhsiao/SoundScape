package com.cs407.soundscape.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cs407.soundscape.data.model.SoundEvent
import com.cs407.soundscape.data.repository.MockSoundRepository

@Composable
fun MapScreen() {
    // TODO: Replace with ViewModel when backend is integrated
    val repository = remember { MockSoundRepository() }
    var events by remember { mutableStateOf<List<SoundEvent>>(emptyList()) }
    
    // TODO: Load from ViewModel/Repository when backend is integrated
    events = remember { repository.getAllEvents() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // TODO: Replace with Google Maps Compose when backend location services are integrated
        // For now, show a placeholder
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🗺️",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Map View",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "TODO: Integrate Google Maps Compose",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Found ${events.size} sound events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                // TODO: Display markers for each sound event on the map
                // TODO: Add location permissions handling
                // TODO: Show user's current location
                // TODO: Add filtering by sound type
                // TODO: Add clustering for nearby events
            }
        }
    }
}

