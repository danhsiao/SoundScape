package com.cs407.soundscape.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.soundscape.data.AnalyticsViewModel
import com.cs407.soundscape.data.AnalyticsViewModelFactory
import com.cs407.soundscape.data.SessionManager
import com.cs407.soundscape.data.SoundEvent
import com.cs407.soundscape.data.SoundEventViewModel
import com.cs407.soundscape.data.SoundEventViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen() {
    val sessionManager = remember { SessionManager() }
    val userId = sessionManager.getUserId()
    
    if (userId == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Please sign in to view analytics",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    // Get current user's events for displaying locations
    val userViewModel: SoundEventViewModel = viewModel(
        factory = SoundEventViewModelFactory(userId)
    )
    val userEvents by userViewModel.events.collectAsState()
    
    // Get all events for calculating stats from all users
    val allEventsViewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModelFactory()
    )
    val allEvents by allEventsViewModel.allEvents.collectAsState()
    
    // State for showing location details dialog
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    
    // Calculate analytics from current user's data (for display)
    val totalEvents = userEvents.size
    val eventsByLabel = userEvents.groupBy { it.label }.mapValues { it.value.size }
    val recentEvents = userEvents.take(10)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (userEvents.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No data yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start recording sounds to see analytics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                StatCard(
                    title = "Total Events",
                    value = totalEvents.toString()
                )
            }

            if (eventsByLabel.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Events by Type",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(eventsByLabel.entries.sortedByDescending { it.value }) { (label, count) ->
                    LabelStatCard(
                        label = label,
                        count = count,
                        onClick = { selectedLabel = label }
                    )
                }
            }

            if (recentEvents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recent Events",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(recentEvents) { event ->
                    RecentEventCard(event = event)
                }
            }
        }
    }
    
    // Show location details dialog when a label is selected
    selectedLabel?.let { label ->
        LocationDetailsDialog(
            label = label,
            userEvents = userEvents,
            allEvents = allEvents,
            onDismiss = { selectedLabel = null }
        )
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun LabelStatCard(label: String, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.ifEmpty { "Unknown" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RecentEventCard(event: SoundEvent) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(event.timestamp))
    
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
                text = event.label.ifEmpty { "Sound Event" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class LocationStats(
    val latitude: Double,
    val longitude: Double,
    val averageDecibel: Float,
    val peakMaxDecibelTime: Long,
    val peakMinDecibelTime: Long
)

// Calculate distance between two coordinates in meters using Haversine formula
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

fun calculateLocationStats(userEvents: List<SoundEvent>, allEvents: List<SoundEvent>, selectedLabel: String): List<LocationStats> {
    // Step 1: Find events with the selected label from CURRENT USER that have location data
    val selectedLabelEvents = userEvents.filter { 
        it.label == selectedLabel && it.latitude != null && it.longitude != null 
    }
    
    if (selectedLabelEvents.isEmpty()) return emptyList()
    
    // Step 2: Group nearby coordinates together (within 20 meters)
    val locationGroups = mutableListOf<Pair<Double, Double>>()
    val thresholdMeters = 20.0 // Group locations within 20 meters
    
    for (event in selectedLabelEvents) {
        val lat = event.latitude!!
        val lng = event.longitude!!
        
        // Find if this location is close to any existing group
        val nearbyGroup = locationGroups.firstOrNull { group ->
            val (groupLat, groupLng) = group
            calculateDistance(lat, lng, groupLat, groupLng) <= thresholdMeters
        }
        
        if (nearbyGroup == null) {
            // Create a new group with this location as the center
            locationGroups.add(Pair(lat, lng))
        }
    }
    
    // Step 3: For each location group, find ALL events within threshold (all labels, all users)
    return locationGroups.map { groupCenter ->
        val (centerLat, centerLng) = groupCenter
        
        // Find all events within threshold from allEvents
        val allEventsAtLocation = allEvents.filter { event ->
            event.latitude != null && event.longitude != null && 
            calculateDistance(centerLat, centerLng, event.latitude!!, event.longitude!!) <= thresholdMeters
        }
        
        if (allEventsAtLocation.isEmpty()) {
            // Should not happen, but handle gracefully
            return@map LocationStats(
                latitude = centerLat,
                longitude = centerLng,
                averageDecibel = 0f,
                peakMaxDecibelTime = 0L,
                peakMinDecibelTime = 0L
            )
        }
        
        // Step 4: Calculate statistics from ALL events at this location
        val avgDecibel = allEventsAtLocation.map { it.decibelLevel }.average().toFloat()
        
        val maxDecibelEvent = allEventsAtLocation.maxByOrNull { it.decibelLevel }
        val minDecibelEvent = allEventsAtLocation.minByOrNull { it.decibelLevel }
        
        LocationStats(
            latitude = centerLat,
            longitude = centerLng,
            averageDecibel = avgDecibel,
            peakMaxDecibelTime = maxDecibelEvent?.timestamp ?: 0L,
            peakMinDecibelTime = minDecibelEvent?.timestamp ?: 0L
        )
    }
}

@Composable
fun LocationDetailsDialog(
    label: String,
    userEvents: List<SoundEvent>,
    allEvents: List<SoundEvent>,
    onDismiss: () -> Unit
) {
    val locationStats = remember(userEvents, allEvents, label) { calculateLocationStats(userEvents, allEvents, label) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Location Details: $label",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (locationStats.isEmpty()) {
                Text(
                    text = "No location data available for this event type.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(locationStats) { stats ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Location",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Lat: ${stats.latitude}, Lng: ${stats.longitude}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Average Decibel: ${String.format(Locale.US, "%.1f", stats.averageDecibel)} dB",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                if (stats.peakMaxDecibelTime > 0) {
                                    Text(
                                        text = "Peak Max Decibel Time: ${dateFormat.format(Date(stats.peakMaxDecibelTime))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (stats.peakMinDecibelTime > 0) {
                                    Text(
                                        text = "Peak Min Decibel Time: ${dateFormat.format(Date(stats.peakMinDecibelTime))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

