package com.cs407.soundscape.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SoundEventRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Insert a new sound event for a specific user
    suspend fun insert(event: SoundEvent): Result<String> {
        return try {
            val docRef = firestore
                .collection("users")
                .document(event.userId)
                .collection("soundEvents")
                .add(
                    hashMapOf(
                        "label" to event.label,
                        "timestamp" to event.timestamp,
                        "userId" to event.userId
                    )
                )
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
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        SoundEvent(
                            id = doc.id,
                            userId = doc.getString("userId") ?: userId,
                            label = doc.getString("label") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) {
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
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { listenerRegistration.remove() }
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
