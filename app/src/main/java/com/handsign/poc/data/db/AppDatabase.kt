package com.handsign.poc.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.handsign.poc.data.db.dao.*
import com.handsign.poc.data.db.entity.*

@Database(
    entities  = [
        UserEntity::class,
        SessionEntity::class,
        GestureLogEntity::class,
        ModelMetaEntity::class
    ],
    version   = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun gestureLogDao(): GestureLogDao
    abstract fun modelMetaDao(): ModelMetaDao
}
