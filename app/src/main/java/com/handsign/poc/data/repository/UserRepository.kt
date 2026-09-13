package com.handsign.poc.data.repository

import com.handsign.poc.data.db.dao.UserDao
import com.handsign.poc.data.db.entity.UserEntity
import com.handsign.poc.data.prefs.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val prefs: AppPreferences
) {
    suspend fun createUser(displayName: String, avatarSeed: String): Long {
        val id = userDao.insert(UserEntity(displayName = displayName, avatarSeed = avatarSeed))
        prefs.setActiveUserId(id)
        return id
    }

    suspend fun getActiveUser(): UserEntity? = userDao.getActive()

    suspend fun updateLastActive(userId: Long) =
        userDao.updateLastActive(userId)

    suspend fun updateDisplayName(userId: Long, name: String) =
        userDao.updateDisplayName(userId, name)
}
