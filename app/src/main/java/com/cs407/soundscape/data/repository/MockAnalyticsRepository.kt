package com.cs407.soundscape.data.repository

import com.cs407.soundscape.data.model.AnalyticsData
import com.cs407.soundscape.data.model.DailyEventCount
import com.cs407.soundscape.data.model.LocationStats
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
        
        return AnalyticsData(
            totalEvents = totalEvents,
            averageDecibel = averageDecibel,
            eventsByType = eventsByType,
            eventsByDay = eventsByDay,
            peakHours = peakHours,
            topLocations = topLocations
        )
    }
}

