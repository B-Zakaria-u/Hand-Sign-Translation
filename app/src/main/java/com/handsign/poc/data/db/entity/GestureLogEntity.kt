package com.handsign.poc.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gesture_logs",
    foreignKeys = [ForeignKey(
        entity        = SessionEntity::class,
        parentColumns = ["id"],
        childColumns  = ["sessionId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class GestureLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val letter: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    /** 0 = candidate (not yet accepted), 1 = user accepted (committed) */
    val wasCommitted: Int = 0
)
