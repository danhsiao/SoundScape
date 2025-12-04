package com.cs407.soundscape.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sound_events",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SoundEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int, // Foreign key to users table
    val label: String,
    val timestamp: Long
)
