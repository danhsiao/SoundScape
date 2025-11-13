package com.cs407.soundscape.data.repository

import com.cs407.soundscape.data.model.SoundEvent
import com.cs407.soundscape.data.model.SoundType
import java.util.Date

/**
 * Mock repository for sound events.
 * TODO: Replace with Firebase/Supabase repository when backend is integrated
 */
class MockSoundRepository {
    
    fun getAllEvents(): List<SoundEvent> {
        // TODO: Replace with backend call: FirebaseFirestore.collection("soundEvents").get()
        return mockEvents
    }
    
    fun getEventsByLocation(latitude: Double, longitude: Double, radiusKm: Double): List<SoundEvent> {
        // TODO: Replace with backend query filtering by location
        return mockEvents.filter { event ->
            val distance = calculateDistance(
                latitude, longitude,
                event.latitude, event.longitude
            )
            distance <= radiusKm
        }
    }
    
    fun getEventsByType(type: SoundType): List<SoundEvent> {
        // TODO: Replace with backend query filtering by type
        return mockEvents.filter { it.soundType == type }
    }
    
    fun saveEvent(event: SoundEvent) {
        // TODO: Replace with backend save: FirebaseFirestore.collection("soundEvents").add(event)
        mockEvents = mockEvents + event
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    companion object {
        // Mountain View / Shoreline area points
        private var mockEvents = listOf(
            SoundEvent(
                id = "1",
                title = "Traffic Noise",
                description = "Heavy traffic on Market St",
                // US-101 by Computer History Museum
                latitude = 37.4149,
                longitude = -122.0770,
                timestamp = Date(System.currentTimeMillis() - 3600000),
                soundType = SoundType.TRAFFIC,
                decibelLevel = 78.0f,
                duration = 300000
            ),
            SoundEvent(
                id = "2",
                title = "Birds Chirping",
                description = "Morning birds in the park",
                // Shoreline Lake / Baylands edge
                latitude = 37.4305,
                longitude = -122.0858,
                timestamp = Date(System.currentTimeMillis() - 7200000),
                soundType = SoundType.NATURE,
                decibelLevel = 35.0f,
                duration = 600000
            ),
            SoundEvent(
                id = "3",
                title = "Construction Site",
                description = "Building construction nearby",
                // North Whisman / tech campus area
                latitude = 37.4055,
                longitude = -122.0635,
                timestamp = Date(System.currentTimeMillis() - 10800000),
                soundType = SoundType.CONSTRUCTION,
                decibelLevel = 95.0f,
                duration = 1800000
            ),
            SoundEvent(
                id = "4",
                title = "Street Musician",
                description = "Guitarist in North Beach",
                // Castro St (Downtown Mountain View)
                latitude = 37.3929,
                longitude = -122.0784,
                timestamp = Date(System.currentTimeMillis() - 14400000),
                soundType = SoundType.MUSIC,
                decibelLevel = 68.0f,
                duration = 900000
            ),
            SoundEvent(
                id = "5",
                title = "Dog Barking",
                description = "Dog in residential area",
                // Monta Loma neighborhood
                latitude = 37.4078,
                longitude = -122.0956,
                timestamp = Date(System.currentTimeMillis() - 18000000),
                soundType = SoundType.ANIMAL,
                decibelLevel = 55.0f,
                duration = 120000
            )
        )
    }
}

