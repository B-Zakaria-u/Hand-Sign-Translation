package com.handsign.poc.data.db.dao

import androidx.room.*
import com.handsign.poc.data.db.entity.GestureLogEntity

@Dao
interface GestureLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: GestureLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<GestureLogEntity>)

    @Query("SELECT * FROM gesture_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: Long): List<GestureLogEntity>

    @Query("SELECT AVG(confidence) FROM gesture_logs WHERE sessionId = :sessionId AND wasCommitted = 1")
    suspend fun avgConfidenceForSession(sessionId: Long): Float?

    @Query("UPDATE gesture_logs SET wasCommitted = 1 WHERE id = :id")
    suspend fun markCommitted(id: Long)

    @Query("DELETE FROM gesture_logs WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
