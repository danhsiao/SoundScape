package com.cs407.soundscape.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SoundEventRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Insert a new sound event for a specific user
    suspend fun insert(event: SoundEvent): Result<String> {
        return try {
            val data = hashMapOf(
                "label" to event.label,
                "timestamp" to event.timestamp,
                "userId" to event.userId,
                "decibelLevel" to event.decibelLevel,
                "duration" to event.duration
            )
            
            // Add optional fields if they exist
            event.environment?.let { data["environment"] = it }
            event.latitude?.let { data["latitude"] = it }
            event.longitude?.let { data["longitude"] = it }
            
            val docRef = firestore
                .collection("users")
                .document(event.userId)
                .collection("soundEvents")
                .add(data)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all sound events for a specific user as a Flow
    fun getAllByUserId(userId: String): Flow<List<SoundEvent>> = callbackFlow {
        val listenerRegistration = firestore
            .collection("users")
            .document(userId)
            .collection("soundEvents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log error for debugging
                    Log.e("SoundEventRepository", "Error loading events: ${error.message}", error)
                    // If it's a missing index error, provide helpful info
                    if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        Log.w("SoundEventRepository", "Firestore index may be missing. Check Firebase Console for index creation link.")
                    }
                    // Emit empty list instead of closing the flow
                    // This allows the UI to continue working even if there's a query error
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        SoundEvent(
                            id = doc.id,
                            userId = doc.getString("userId") ?: userId,
                            label = doc.getString("label") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            decibelLevel = (doc.getDouble("decibelLevel")?.toFloat()) ?: 0f,
                            environment = doc.getString("environment"),
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude"),
                            duration = doc.getLong("duration") ?: 0L
                        )
                    } catch (e: Exception) {
                        // Skip malformed documents
                        null
                    }
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Get all sound events from all users (for admin/analytics purposes)
    fun getAll(): Flow<List<SoundEvent>> = callbackFlow {
        val listenerRegistration = firestore
            .collectionGroup("soundEvents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        SoundEvent(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            label = doc.getString("label") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            decibelLevel = (doc.getDouble("decibelLevel")?.toFloat()) ?: 0f,
                            environment = doc.getString("environment"),
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude"),
                            duration = doc.getLong("duration") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Verify that an event exists in the database by checking if it appears in the user's events
    suspend fun verifyEventExists(userId: String, eventId: String, maxWaitSeconds: Int = 10): Boolean {
        return try {
            var found = false
            var attempts = 0
            val maxAttempts = maxWaitSeconds * 2 // Check every 500ms
            
            while (!found && attempts < maxAttempts) {
                val doc = firestore
                    .collection("users")
                    .document(userId)
                    .collection("soundEvents")
                    .document(eventId)
                    .get()
                    .await()
                
                found = doc.exists()
                if (found) break
                
                delay(500) // Wait 500ms between checks
                attempts++
            }
            found
        } catch (e: Exception) {
            false
        }
    }

    // Delete a sound event
    suspend fun delete(userId: String, eventId: String): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("soundEvents")
                .document(eventId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
