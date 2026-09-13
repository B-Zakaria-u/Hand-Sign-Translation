package com.handsign.poc.di

import android.content.Context
import androidx.room.Room
import com.handsign.poc.data.db.AppDatabase
import com.handsign.poc.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "handsign_db")
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun provideGestureLogDao(db: AppDatabase): GestureLogDao = db.gestureLogDao()
    @Provides fun provideModelMetaDao(db: AppDatabase): ModelMetaDao = db.modelMetaDao()
}
