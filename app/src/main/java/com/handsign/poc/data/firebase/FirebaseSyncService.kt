package com.handsign.poc.data.firebase

import com.handsign.poc.data.db.dao.GestureLogDao
import com.handsign.poc.data.db.dao.SessionDao
import com.handsign.poc.data.db.dao.UserDao
import com.handsign.poc.data.db.entity.SessionEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  FIREBASE SYNC SERVICE — STUB (NOT CONNECTED TO FIREBASE)           │
 * │                                                                     │
 * │  TO ACTIVATE:                                                       │
 * │  1. Go to https://console.firebase.google.com                       │
 * │  2. Create a project, add Android app with package "com.handsign.poc"│
 * │  3. Download google-services.json → place in /app/                  │
 * │  4. In app/build.gradle.kts: uncomment Firebase BOM deps            │
 * │  5. In build.gradle.kts (root): uncomment google-services plugin    │
 * │  6. Delete the TODO() line below and uncomment the Firestore code   │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Firestore data model (once activated):
 *   users/{uid}/
 *     profile: { displayName, platform:"android", createdAt }
 *     sessions/{sessionId}/
 *       { composedText, modelName, accuracyAvg, startedAt, endedAt, durationMs }
 *       gestureLogs/{logId}/
 *         { letter, confidence, timestamp, wasCommitted }
 */
@Singleton
class FirebaseSyncService @Inject constructor(
    private val sessionDao: SessionDao,
    private val gestureLogDao: GestureLogDao,
    private val userDao: UserDao
) {
    sealed class SyncResult {
        object NotConfigured : SyncResult()
        object NoUser        : SyncResult()
        object NoFirebaseUid : SyncResult()
        data class Success(val sessionCount: Int) : SyncResult()
        data class Error(val error: Exception) : SyncResult()
    }

    suspend fun syncPendingSessions(): SyncResult {
        // ── STUB ─────────────────────────────────────────────────────
        return SyncResult.NotConfigured
        // ── UNCOMMENT BELOW AFTER FIREBASE SETUP ─────────────────────
        //
        // val user = userDao.getActive() ?: return SyncResult.NoUser
        // val uid  = user.firebaseUid   ?: return SyncResult.NoFirebaseUid
        // return try {
        //     val db = Firebase.firestore
        //     val pending = sessionDao.getUnsynced()
        //     pending.forEach { session ->
        //         val sessionRef = db.collection("users").document(uid)
        //             .collection("sessions").document(session.id.toString())
        //         sessionRef.set(session.toFirestoreMap()).await()
        //         gestureLogDao.getBySession(session.id).forEach { log ->
        //             sessionRef.collection("gestureLogs")
        //                 .document(log.id.toString())
        //                 .set(log.toFirestoreMap()).await()
        //         }
        //         sessionDao.markSynced(session.id)
        //     }
        //     SyncResult.Success(pending.size)
        // } catch (e: Exception) { SyncResult.Error(e) }
    }
}

// Extension helpers (ready for Firebase activation)
private fun SessionEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "composedText" to composedText,
    "modelName"    to modelName,
    "accuracyAvg"  to accuracyAvg,
    "startedAt"    to startedAt,
    "endedAt"      to endedAt,
    "durationMs"   to durationMs
)
