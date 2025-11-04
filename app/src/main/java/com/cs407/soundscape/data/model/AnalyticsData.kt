package com.cs407.soundscape.data.model

/**
 * Data model for analytics.
 * TODO: Replace with backend model when integrating Firebase/Supabase
 */
data class AnalyticsData(
    val totalEvents: Int,
    val averageDecibel: Float,
    val eventsByType: Map<SoundType, Int>,
    val eventsByDay: List<DailyEventCount>,
    val peakHours: List<Int>, // Hours of day with most events
    val topLocations: List<LocationStats>
)

data class DailyEventCount(
    val date: String,
    val count: Int
)

data class LocationStats(
    val latitude: Double,
    val longitude: Double,
    val eventCount: Int,
    val averageDecibel: Float
)

