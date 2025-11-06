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
    val topLocations: List<LocationStats>,
    val topQuietSpots: List<QuietSpot>, // Top 5 quiet spots now (user feedback requirement)
    val forecasts: List<NoiseForecast> // EWMA-based forecasts for nearby locations
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

data class QuietSpot(
    val name: String, // e.g., "Memorial Library", "Gordon Commons"
    val latitude: Double,
    val longitude: Double,
    val currentDecibel: Float,
    val environment: String? = null
)

data class NoiseForecast(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val currentDecibel: Float,
    val forecastedDecibel: Float, // EWMA prediction
    val trend: ForecastTrend, // UP, DOWN, STABLE
    val confidence: Float // 0.0 to 1.0
)

enum class ForecastTrend {
    UP, DOWN, STABLE
}

