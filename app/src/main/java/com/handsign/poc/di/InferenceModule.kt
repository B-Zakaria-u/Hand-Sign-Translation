package com.handsign.poc.di

import android.content.Context
import com.handsign.poc.inference.ModelAdapterFactory
import com.handsign.poc.inference.ModelLoader
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InferenceModule {

    @Provides
    @Singleton
    fun provideModelAdapterFactory(
        @ApplicationContext context: Context
    ): ModelAdapterFactory = ModelAdapterFactory(context)

    @Provides
    @Singleton
    fun provideModelLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        gson: Gson
    ): ModelLoader = ModelLoader(context, okHttpClient, gson)
}
