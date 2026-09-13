package com.handsign.poc.data.db.dao

import androidx.room.*
import com.handsign.poc.data.db.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users ORDER BY lastActive DESC LIMIT 1")
    suspend fun getActive(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("UPDATE users SET lastActive = :ts WHERE id = :id")
    suspend fun updateLastActive(id: Long, ts: Long = System.currentTimeMillis())

    @Query("UPDATE users SET firebaseUid = :uid WHERE id = :id")
    suspend fun setFirebaseUid(id: Long, uid: String)

    @Query("UPDATE users SET displayName = :name WHERE id = :id")
    suspend fun updateDisplayName(id: Long, name: String)
}
