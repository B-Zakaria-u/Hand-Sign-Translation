package com.handsign.poc.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val avatarSeed: String,
    val createdAt: Long  = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    /** Set when Firebase Auth is enabled and user has signed in */
    val firebaseUid: String? = null
)
