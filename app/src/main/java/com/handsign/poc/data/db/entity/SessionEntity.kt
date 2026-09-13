package com.handsign.poc.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity        = UserEntity::class,
        parentColumns = ["id"],
        childColumns  = ["userId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val composedText: String,
    val modelName: String,
    val accuracyAvg: Float? = null,
    val durationMs: Long?   = null,
    val startedAt: Long,
    val endedAt: Long?      = null,
    /** 0 = pending sync, 1 = synced to Firebase */
    val syncedToFirebase: Int = 0
)
