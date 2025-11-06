package com.cs407.soundscape.data.repository

import com.cs407.soundscape.data.model.AnalyticsData
import com.cs407.soundscape.data.model.DailyEventCount
import com.cs407.soundscape.data.model.ForecastTrend
import com.cs407.soundscape.data.model.LocationStats
import com.cs407.soundscape.data.model.NoiseForecast
import com.cs407.soundscape.data.model.QuietSpot
import com.cs407.soundscape.data.model.SoundType

/**
 * Mock repository for analytics data.
 * TODO: Replace with Firebase/Supabase analytics queries when backend is integrated
 */
class MockAnalyticsRepository(private val soundRepository: MockSoundRepository) {
    
    fun getAnalyticsData(): AnalyticsData {
        // TODO: Replace with backend aggregation queries
        val events = soundRepository.getAllEvents()
        
        val totalEvents = events.size
        val averageDecibel = events.map { it.decibelLevel }.average().toFloat()
        
        val eventsByType = events.groupingBy { it.soundType }.eachCount()
        
        val eventsByDay = listOf(
            DailyEventCount("Mon", 12),
            DailyEventCount("Tue", 8),
            DailyEventCount("Wed", 15),
            DailyEventCount("Thu", 10),
            DailyEventCount("Fri", 18),
            DailyEventCount("Sat", 22),
            DailyEventCount("Sun", 14)
        )
        
        val peakHours = listOf(8, 9, 12, 13, 17, 18)
        
        val topLocations = events.groupBy { "${it.latitude},${it.longitude}" }
            .map { (location, locationEvents) ->
                val coords = location.split(",")
                LocationStats(
                    latitude = coords[0].toDouble(),
                    longitude = coords[1].toDouble(),
                    eventCount = locationEvents.size,
                    averageDecibel = locationEvents.map { it.decibelLevel }.average().toFloat()
                )
            }
            .sortedByDescending { it.eventCount }
            .take(5)
        
        // Top 5 Quiet Spots Now (user feedback requirement)
        // Get recent events (within last hour) grouped by environment, sorted by decibel
        val recentEvents = events.filter { 
            System.currentTimeMillis() - it.timestamp.time < 3600000 // Last hour
        }
        
        val topQuietSpots = recentEvents
            .groupBy { it.environment ?: "Unknown" }
            .map { (env, envEvents) ->
                val avgDecibel = envEvents.map { it.decibelLevel }.average().toFloat()
                val latestEvent = envEvents.maxByOrNull { it.timestamp.time }!!
                QuietSpot(
                    name = env,
                    latitude = latestEvent.latitude,
                    longitude = latestEvent.longitude,
                    currentDecibel = avgDecibel,
                    environment = env
                )
            }
            .sortedBy { it.currentDecibel } // Sort by quietest first
            .take(5)
        
        // EWMA-based forecasts (mock implementation)
        // TODO: Replace with actual EWMA calculation when backend is integrated
        val forecasts = listOf(
            NoiseForecast(
                locationName = "Memorial Library",
                latitude = 43.0756,
                longitude = -89.4042,
                currentDecibel = 32.0f,
                forecastedDecibel = 34.0f, // EWMA prediction
                trend = ForecastTrend.UP,
                confidence = 0.75f
            ),
            NoiseForecast(
                locationName = "Gordon Commons",
                latitude = 43.0738,
                longitude = -89.4012,
                currentDecibel = 68.0f,
                forecastedDecibel = 65.0f,
                trend = ForecastTrend.DOWN,
                confidence = 0.82f
            ),
            NoiseForecast(
                locationName = "College Library",
                latitude = 43.0745,
                longitude = -89.4035,
                currentDecibel = 28.0f,
                forecastedDecibel = 29.0f,
                trend = ForecastTrend.STABLE,
                confidence = 0.88f
            ),
            NoiseForecast(
                locationName = "Union South",
                latitude = 43.0708,
                longitude = -89.4065,
                currentDecibel = 55.0f,
                forecastedDecibel = 58.0f,
                trend = ForecastTrend.UP,
                confidence = 0.70f
            )
        )
        
        return AnalyticsData(
            totalEvents = totalEvents,
            averageDecibel = averageDecibel,
            eventsByType = eventsByType,
            eventsByDay = eventsByDay,
            peakHours = peakHours,
            topLocations = topLocations,
            topQuietSpots = topQuietSpots,
            forecasts = forecasts
        )
    }
}

