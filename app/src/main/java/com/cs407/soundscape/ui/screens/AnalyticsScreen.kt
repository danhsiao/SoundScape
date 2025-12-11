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
import androidx.compose.runtime.LaunchedEffect
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
import com.cs407.soundscape.data.EWMAForecaster
import com.cs407.soundscape.data.ForecastTrend
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
    
    // Track current hour for hourly refresh
    var currentHour by remember { mutableStateOf(System.currentTimeMillis() / (60 * 60 * 1000)) }
    
    // Update current hour every hour
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60 * 60 * 1000) // Wait 1 hour
            currentHour = System.currentTimeMillis() / (60 * 60 * 1000)
        }
    }
    
    // Calculate analytics from current user's data (for display)
    // These will automatically update when userEvents changes (real-time from Firestore)
    val totalEvents = userEvents.size
    val eventsByLabel = userEvents.groupBy { it.label }.mapValues { it.value.size }
    val recentEvents = userEvents.take(10)
    
    // Calculate location stats with forecasts for all user's locations
    // This will recompute whenever userEvents, allEvents, or currentHour changes
    val allLocationStats = remember(userEvents, allEvents, currentHour) {
        val allStats = mutableListOf<LocationStats>()
        val labels = userEvents.groupBy { it.label }.keys
        labels.forEach { label ->
            val stats = calculateLocationStats(userEvents, allEvents, label)
            allStats.addAll(stats)
        }
        // Group nearby locations together (within 0.05 degrees lat/lng) and merge them
        // Pass allEvents so we can recalculate merged statistics properly
        val thresholdDegrees = 0.05 // 0.05 degrees ≈ 5.5 km (between 0.01-0.1 as requested)
        groupNearbyLocations(allStats, thresholdDegrees, allEvents)
    }

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
            
            // Show forecasts for all unique locations (already grouped by proximity)
            if (allLocationStats.isNotEmpty() && allLocationStats.any { it.forecastedDecibel != null }) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Noise Forecasts (15-30 min)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(allLocationStats.filter { it.forecastedDecibel != null }) { stats ->
                    ForecastCard(stats = stats)
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

@Composable
fun ForecastCard(stats: LocationStats) {
    val trendText = when (stats.forecastTrend) {
        ForecastTrend.UP -> "↑"
        ForecastTrend.DOWN -> "↓"
        ForecastTrend.STABLE -> "→"
        null -> ""
    }
    val trendColor = when (stats.forecastTrend) {
        ForecastTrend.UP -> MaterialTheme.colorScheme.error
        ForecastTrend.DOWN -> MaterialTheme.colorScheme.primary
        ForecastTrend.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.4f", stats.latitude)}, ${String.format(Locale.US, "%.4f", stats.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (stats.forecastedDecibel != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", stats.forecastedDecibel)} dB $trendText",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                        if (stats.forecastConfidence != null) {
                            Text(
                                text = "${String.format(Locale.US, "%.0f", stats.forecastConfidence * 100)}% confidence",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: ${String.format(Locale.US, "%.1f", stats.averageDecibel)} dB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Forecast: ${String.format(Locale.US, "%.1f", stats.forecastedDecibel ?: 0f)} dB",
                    style = MaterialTheme.typography.bodySmall,
                    color = trendColor
                )
            }
        }
    }
}

data class LocationStats(
    val latitude: Double,
    val longitude: Double,
    val averageDecibel: Float, // All-time average
    val peakMaxDecibelTime: Long, // From last hour
    val peakMinDecibelTime: Long, // From last hour
    val forecastedDecibel: Float? = null, // EWMA prediction for 15-30 min
    val forecastTrend: ForecastTrend? = null, // UP/DOWN/STABLE
    val forecastConfidence: Float? = null // 0.0 to 1.0
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

/**
 * Group nearby locations together and merge their statistics
 * Locations within thresholdDegrees (lat/lng) are considered the same location
 * This function needs access to allEvents to properly recalculate merged statistics
 */
private fun groupNearbyLocations(
    stats: List<LocationStats>, 
    thresholdDegrees: Double,
    allEvents: List<SoundEvent> = emptyList()
): List<LocationStats> {
    if (stats.isEmpty()) return emptyList()
    
    val groups = mutableListOf<MutableList<LocationStats>>()
    
    for (stat in stats) {
        // Find if this location is close to any existing group (using coordinate-based check)
        val nearbyGroupIndex = groups.indexOfFirst { group ->
            val groupCenter = group.first()
            Math.abs(stat.latitude - groupCenter.latitude) <= thresholdDegrees &&
            Math.abs(stat.longitude - groupCenter.longitude) <= thresholdDegrees
        }
        
        if (nearbyGroupIndex >= 0) {
            groups[nearbyGroupIndex].add(stat)
        } else {
            groups.add(mutableListOf(stat))
        }
    }
    
    // Merge each group into a single LocationStats
    return groups.map { group ->
        if (group.size == 1) {
            group.first()
        } else {
            // Merge multiple stats into one
            val avgLat = group.map { it.latitude }.average()
            val avgLng = group.map { it.longitude }.average()
            
            // If we have allEvents, recalculate stats from all events at this merged location
            if (allEvents.isNotEmpty()) {
                val allEventsAtMergedLocation = allEvents.filter { event ->
                    event.latitude != null && event.longitude != null &&
                    Math.abs(event.latitude!! - avgLat) <= thresholdDegrees &&
                    Math.abs(event.longitude!! - avgLng) <= thresholdDegrees
                }
                
                if (allEventsAtMergedLocation.isNotEmpty()) {
                    // Recalculate average decibel from all events
                    val mergedAvgDecibel = allEventsAtMergedLocation.map { it.decibelLevel }.average().toFloat()
                    
                    // Peak max/min times from last hour
                    val now = System.currentTimeMillis()
                    val oneHourAgo = now - (60 * 60 * 1000)
                    val lastHourEvents = allEventsAtMergedLocation.filter { it.timestamp >= oneHourAgo }
                    
                    val maxDecibelEvent = if (lastHourEvents.isNotEmpty()) {
                        lastHourEvents.maxByOrNull { it.decibelLevel }
                    } else null
                    val minDecibelEvent = if (lastHourEvents.isNotEmpty()) {
                        lastHourEvents.minByOrNull { it.decibelLevel }
                    } else null
                    
                    // Recalculate EWMA forecast from all events (pass averageDecibel for accurate trend)
                    val forecaster = EWMAForecaster(alpha = 0.3f)
                    val sortedEvents = allEventsAtMergedLocation.sortedBy { it.timestamp }
                    val forecast = forecaster.calculateForecast(sortedEvents, mergedAvgDecibel)
                    
                    return@map LocationStats(
                        latitude = avgLat,
                        longitude = avgLng,
                        averageDecibel = mergedAvgDecibel,
                        peakMaxDecibelTime = maxDecibelEvent?.timestamp ?: 0L,
                        peakMinDecibelTime = minDecibelEvent?.timestamp ?: 0L,
                        forecastedDecibel = forecast?.first,
                        forecastTrend = forecast?.second,
                        forecastConfidence = forecast?.third
                    )
                }
            }
            
            // Fallback: merge without recalculating (use averages)
            val mergedAvgDecibel = group.map { it.averageDecibel }.average().toFloat()
            val allMaxTimes = group.mapNotNull { if (it.peakMaxDecibelTime > 0) it.peakMaxDecibelTime else null }
            val allMinTimes = group.mapNotNull { if (it.peakMinDecibelTime > 0) it.peakMinDecibelTime else null }
            val mergedMaxTime = allMaxTimes.maxOrNull() ?: 0L
            val mergedMinTime = allMinTimes.minOrNull() ?: 0L
            val bestForecast = group.maxByOrNull { it.forecastConfidence ?: 0f }
            
            LocationStats(
                latitude = avgLat,
                longitude = avgLng,
                averageDecibel = mergedAvgDecibel,
                peakMaxDecibelTime = mergedMaxTime,
                peakMinDecibelTime = mergedMinTime,
                forecastedDecibel = bestForecast?.forecastedDecibel,
                forecastTrend = bestForecast?.forecastTrend,
                forecastConfidence = bestForecast?.forecastConfidence
            )
        }
    }
}

fun calculateLocationStats(userEvents: List<SoundEvent>, allEvents: List<SoundEvent>, selectedLabel: String): List<LocationStats> {
    // Step 1: Find events with the selected label from CURRENT USER that have location data
    val selectedLabelEvents = userEvents.filter { 
        it.label == selectedLabel && it.latitude != null && it.longitude != null 
    }
    
    if (selectedLabelEvents.isEmpty()) return emptyList()
    
    // Step 2: Group nearby coordinates together (within 0.05 degrees lat/lng)
    // Use coordinate-based threshold: 0.05 degrees ≈ 5.5 km
    val thresholdDegrees = 0.05 // Group locations within 0.05 degrees (between 0.01-0.1 as requested)
    val eventGroups = mutableListOf<MutableList<SoundEvent>>()
    
    for (event in selectedLabelEvents) {
        val lat = event.latitude!!
        val lng = event.longitude!!
        
        // Find if this event is close to any existing group
        // Check against all events in each group to find the closest match
        val nearbyGroupIndex = eventGroups.indexOfFirst { group ->
            group.any { existingEvent ->
                val existingLat = existingEvent.latitude!!
                val existingLng = existingEvent.longitude!!
                // Check if within threshold degrees for both lat and lng
                Math.abs(lat - existingLat) <= thresholdDegrees && 
                Math.abs(lng - existingLng) <= thresholdDegrees
            }
        }
        
        if (nearbyGroupIndex >= 0) {
            // Add to existing group
            eventGroups[nearbyGroupIndex].add(event)
        } else {
            // Create a new group
            eventGroups.add(mutableListOf(event))
        }
    }
    
    // Step 3: Calculate group centers (average of all events in each group)
    val locationGroups = eventGroups.map { group ->
        val avgLat = group.map { it.latitude!! }.average()
        val avgLng = group.map { it.longitude!! }.average()
        Pair(avgLat, avgLng)
    }
    
    // Step 4: For each location group, find ALL events within threshold (all labels, all users)
    return locationGroups.map { groupCenter ->
        val (centerLat, centerLng) = groupCenter
        
        // Find all events within threshold from allEvents (using coordinate-based check)
        val allEventsAtLocation = allEvents.filter { event ->
            event.latitude != null && event.longitude != null && 
            Math.abs(event.latitude!! - centerLat) <= thresholdDegrees &&
            Math.abs(event.longitude!! - centerLng) <= thresholdDegrees
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
        
        // Step 4: Calculate statistics
        // Average decibel from ALL events (all-time)
        val avgDecibel = allEventsAtLocation.map { it.decibelLevel }.average().toFloat()
        
        // Peak max/min decibel times from LAST HOUR only
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val lastHourEvents = allEventsAtLocation.filter { it.timestamp >= oneHourAgo }
        
        val maxDecibelEvent = if (lastHourEvents.isNotEmpty()) {
            lastHourEvents.maxByOrNull { it.decibelLevel }
        } else {
            null
        }
        val minDecibelEvent = if (lastHourEvents.isNotEmpty()) {
            lastHourEvents.minByOrNull { it.decibelLevel }
        } else {
            null
        }
        
        // Step 5: Calculate EWMA forecast (pass averageDecibel for accurate trend determination)
        val forecaster = EWMAForecaster(alpha = 0.3f)
        val sortedEvents = allEventsAtLocation.sortedBy { it.timestamp }
        val forecast = forecaster.calculateForecast(sortedEvents, avgDecibel)
        
        LocationStats(
            latitude = centerLat,
            longitude = centerLng,
            averageDecibel = avgDecibel,
            peakMaxDecibelTime = maxDecibelEvent?.timestamp ?: 0L,
            peakMinDecibelTime = minDecibelEvent?.timestamp ?: 0L,
            forecastedDecibel = forecast?.first,
            forecastTrend = forecast?.second,
            forecastConfidence = forecast?.third
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
    // Track current hour for hourly refresh
    var currentHour by remember { mutableStateOf(System.currentTimeMillis() / (60 * 60 * 1000)) }
    
    // Update current hour every hour
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60 * 60 * 1000) // Wait 1 hour
            currentHour = System.currentTimeMillis() / (60 * 60 * 1000)
        }
    }
    
    val locationStats = remember(userEvents, allEvents, label, currentHour) { 
        calculateLocationStats(userEvents, allEvents, label) 
    }
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
                                
                                // EWMA Forecast
                                if (stats.forecastedDecibel != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val trendText = when (stats.forecastTrend) {
                                        ForecastTrend.UP -> "↑"
                                        ForecastTrend.DOWN -> "↓"
                                        ForecastTrend.STABLE -> "→"
                                        null -> ""
                                    }
                                    val trendColor = when (stats.forecastTrend) {
                                        ForecastTrend.UP -> MaterialTheme.colorScheme.error
                                        ForecastTrend.DOWN -> MaterialTheme.colorScheme.primary
                                        ForecastTrend.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(
                                        text = "Forecast (15-30 min): ${String.format(Locale.US, "%.1f", stats.forecastedDecibel)} dB $trendText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = trendColor
                                    )
                                    if (stats.forecastConfidence != null) {
                                        Text(
                                            text = "Confidence: ${String.format(Locale.US, "%.0f", stats.forecastConfidence * 100)}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                if (stats.peakMaxDecibelTime > 0) {
                                    Text(
                                        text = "Peak Max Decibel Time (Last Hour): ${dateFormat.format(Date(stats.peakMaxDecibelTime))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (stats.peakMinDecibelTime > 0) {
                                    Text(
                                        text = "Peak Min Decibel Time (Last Hour): ${dateFormat.format(Date(stats.peakMinDecibelTime))}",
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


