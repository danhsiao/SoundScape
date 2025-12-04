package com.cs407.soundscape.data

import kotlinx.coroutines.flow.Flow

class SoundEventRepository(private val dao: SoundEventDao) {

    suspend fun insert(event: SoundEventEntity) {
        dao.insert(event)
    }

    fun getAllByUserId(userId: Int): Flow<List<SoundEventEntity>> {
        return dao.getAllByUserId(userId)
    }

    fun getAll(): Flow<List<SoundEventEntity>> {
        return dao.getAll()
    }
}
