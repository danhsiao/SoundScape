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
                description = "Heavy traffic on Market St",
                latitude = 37.7760,
                longitude = -122.4170,
                timestamp = Date(System.currentTimeMillis() - 3600000),
                soundType = SoundType.TRAFFIC,
                decibelLevel = 75.5f,
                duration = 300000
            ),
            SoundEvent(
                id = "2",
                title = "Birds Chirping",
                description = "Morning birds in the park",
                latitude = 37.7694,      // Dolores Park area
                longitude = -122.4862 + 0.06,
                timestamp = Date(System.currentTimeMillis() - 7200000),
                soundType = SoundType.NATURE,
                decibelLevel = 45.0f,
                duration = 600000
            ),
            SoundEvent(
                id = "3",
                title = "Construction Site",
                description = "Building construction nearby",
                latitude = 37.7890,      // closer to Embarcadero
                longitude = -122.3910,
                timestamp = Date(System.currentTimeMillis() - 10800000),
                soundType = SoundType.CONSTRUCTION,
                decibelLevel = 85.0f,
                duration = 1800000
            ),
            SoundEvent(
                id = "4",
                title = "Street Musician",
                description = "Guitarist in North Beach",
                latitude = 37.8040,      // north
                longitude = -122.4110,
                timestamp = Date(System.currentTimeMillis() - 14400000),
                soundType = SoundType.MUSIC,
                decibelLevel = 65.0f,
                duration = 900000
            ),
            SoundEvent(
                id = "5",
                title = "Dog Barking",
                description = "Dog in residential area",
                latitude = 37.7580,      // south-ish
                longitude = -122.4350,
                timestamp = Date(System.currentTimeMillis() - 18000000),
                soundType = SoundType.ANIMAL,
                decibelLevel = 60.0f,
                duration = 120000
            )
        )
    }
}

