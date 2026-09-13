package com.handsign.poc.data.db.dao

import androidx.room.*
import com.handsign.poc.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY startedAt DESC")
    fun getByUser(userId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE syncedToFirebase = 0 ORDER BY startedAt ASC")
    suspend fun getUnsynced(): List<SessionEntity>

    @Query("UPDATE sessions SET syncedToFirebase = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE sessions SET endedAt = :endedAt, durationMs = :durationMs, accuracyAvg = :avg WHERE id = :id")
    suspend fun closeSession(id: Long, endedAt: Long, durationMs: Long, avg: Float?)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM sessions WHERE userId = :userId")
    suspend fun countByUser(userId: Long): Int
}
