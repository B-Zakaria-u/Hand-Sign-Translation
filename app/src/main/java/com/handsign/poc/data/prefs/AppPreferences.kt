package com.handsign.poc.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ACTIVE_USER_ID       = longPreferencesKey("active_user_id")
        private val KEY_ACTIVE_MODEL_ID      = longPreferencesKey("active_model_id")
        private val KEY_CONFIDENCE_THRESHOLD = floatPreferencesKey("confidence_threshold")
        private val KEY_STABLE_FRAMES        = intPreferencesKey("stable_frames")
        private val KEY_ONBOARDING_DONE      = booleanPreferencesKey("onboarding_done")
        private val KEY_FRONT_CAMERA         = booleanPreferencesKey("front_camera")

        const val DEFAULT_CONFIDENCE = 0.75f
        const val DEFAULT_STABLE_FRAMES = 10
    }

    val activeUserId: Flow<Long?> = context.dataStore.data.map {
        if (it.contains(KEY_ACTIVE_USER_ID)) it[KEY_ACTIVE_USER_ID] else null
    }
    val activeModelId: Flow<Long?> = context.dataStore.data.map {
        if (it.contains(KEY_ACTIVE_MODEL_ID)) it[KEY_ACTIVE_MODEL_ID] else null
    }
    val confidenceThreshold: Flow<Float> = context.dataStore.data.map {
        it[KEY_CONFIDENCE_THRESHOLD] ?: DEFAULT_CONFIDENCE
    }
    val stableFrames: Flow<Int> = context.dataStore.data.map {
        it[KEY_STABLE_FRAMES] ?: DEFAULT_STABLE_FRAMES
    }
    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ONBOARDING_DONE] ?: false
    }
    val useFrontCamera: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_FRONT_CAMERA] ?: true
    }

    suspend fun setActiveUserId(id: Long) =
        context.dataStore.edit { it[KEY_ACTIVE_USER_ID] = id }
    suspend fun setActiveModelId(id: Long) =
        context.dataStore.edit { it[KEY_ACTIVE_MODEL_ID] = id }
    suspend fun setConfidenceThreshold(v: Float) =
        context.dataStore.edit { it[KEY_CONFIDENCE_THRESHOLD] = v }
    suspend fun setStableFrames(v: Int) =
        context.dataStore.edit { it[KEY_STABLE_FRAMES] = v }
    suspend fun setOnboardingDone(done: Boolean) =
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    suspend fun setFrontCamera(front: Boolean) =
        context.dataStore.edit { it[KEY_FRONT_CAMERA] = front }
}
