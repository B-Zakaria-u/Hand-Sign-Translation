package com.handsign.poc.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.handsign.poc.data.model.ModelFormat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)   // large model downloads
            .addInterceptor { chain ->
                val request = chain.request()
                // SECURITY: block non-HTTPS requests at the network layer
                check(request.url.scheme == "https") {
                    "Only HTTPS connections are allowed. Got: ${request.url.scheme}://"
                }
                chain.proceed(request)
            }
            .build()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .registerTypeAdapter(ModelFormat::class.java,
            com.google.gson.JsonDeserializer { json, _, _ ->
                runCatching { ModelFormat.valueOf(json.asString.uppercase()) }
                    .getOrDefault(ModelFormat.TFLITE_LANDMARK)
            })
        .create()
}
