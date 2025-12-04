package com.cs407.soundscape.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundEventDao {

    @Insert
    suspend fun insert(event: SoundEventEntity)

    @Query("SELECT * FROM sound_events WHERE userId = :userId ORDER BY id DESC")
    fun getAllByUserId(userId: Int): Flow<List<SoundEventEntity>>

    @Query("SELECT * FROM sound_events ORDER BY id DESC")
    fun getAll(): Flow<List<SoundEventEntity>>
}
