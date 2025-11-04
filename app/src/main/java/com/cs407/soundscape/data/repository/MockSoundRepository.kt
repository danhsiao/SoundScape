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
        private var mockEvents = listOf(
            SoundEvent(
                id = "1",
                title = "Traffic Noise",
                description = "Heavy traffic on Main Street",
                latitude = 37.7749,
                longitude = -122.4194,
                timestamp = Date(System.currentTimeMillis() - 3600000), // 1 hour ago
                soundType = SoundType.TRAFFIC,
                decibelLevel = 75.5f,
                duration = 300000 // 5 minutes
            ),
            SoundEvent(
                id = "2",
                title = "Birds Chirping",
                description = "Morning birds in the park",
                latitude = 37.7849,
                longitude = -122.4094,
                timestamp = Date(System.currentTimeMillis() - 7200000), // 2 hours ago
                soundType = SoundType.NATURE,
                decibelLevel = 45.0f,
                duration = 600000 // 10 minutes
            ),
            SoundEvent(
                id = "3",
                title = "Construction Site",
                description = "Building construction nearby",
                latitude = 37.7649,
                longitude = -122.4294,
                timestamp = Date(System.currentTimeMillis() - 10800000), // 3 hours ago
                soundType = SoundType.CONSTRUCTION,
                decibelLevel = 85.0f,
                duration = 1800000 // 30 minutes
            ),
            SoundEvent(
                id = "4",
                title = "Street Musician",
                description = "Guitarist playing on the corner",
                latitude = 37.7549,
                longitude = -122.4394,
                timestamp = Date(System.currentTimeMillis() - 14400000), // 4 hours ago
                soundType = SoundType.MUSIC,
                decibelLevel = 65.0f,
                duration = 900000 // 15 minutes
            ),
            SoundEvent(
                id = "5",
                title = "Dog Barking",
                description = "Dog in nearby yard",
                latitude = 37.7949,
                longitude = -122.3994,
                timestamp = Date(System.currentTimeMillis() - 18000000), // 5 hours ago
                soundType = SoundType.ANIMAL,
                decibelLevel = 60.0f,
                duration = 120000 // 2 minutes
            )
        )
    }
}

