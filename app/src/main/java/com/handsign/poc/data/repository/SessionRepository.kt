package com.handsign.poc.data.repository

import com.handsign.poc.data.db.dao.GestureLogDao
import com.handsign.poc.data.db.dao.SessionDao
import com.handsign.poc.data.db.entity.GestureLogEntity
import com.handsign.poc.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val gestureLogDao: GestureLogDao
) {
    suspend fun startSession(userId: Long, modelName: String): Long =
        sessionDao.insert(
            SessionEntity(
                userId      = userId,
                composedText = "",
                modelName   = modelName,
                startedAt   = System.currentTimeMillis()
            )
        )

    suspend fun closeSession(
        sessionId: Long,
        composedText: String,
        modelName: String,
        startedAt: Long,
        logs: List<GestureLogEntity>
    ) {
        val endedAt    = System.currentTimeMillis()
        val durationMs = endedAt - startedAt
        gestureLogDao.insertAll(logs)
        val avg = gestureLogDao.avgConfidenceForSession(sessionId)
        sessionDao.update(
            SessionEntity(
                id           = sessionId,
                userId       = logs.firstOrNull()?.sessionId ?: 0L,
                composedText = composedText,
                modelName    = modelName,
                accuracyAvg  = avg,
                durationMs   = durationMs,
                startedAt    = startedAt,
                endedAt      = endedAt
            )
        )
    }

    fun getSessionsByUser(userId: Long): Flow<List<SessionEntity>> =
        sessionDao.getByUser(userId)

    suspend fun deleteSession(sessionId: Long) = sessionDao.delete(sessionId)

    suspend fun getUnsynced() = sessionDao.getUnsynced()
    suspend fun markSynced(id: Long) = sessionDao.markSynced(id)
}
