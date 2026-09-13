package com.handsign.poc.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.handsign.poc.data.db.dao.ModelMetaDao
import com.handsign.poc.data.db.entity.ModelMetaEntity
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelMetaDao: ModelMetaDao,
    private val prefs: AppPreferences,
    private val gson: Gson
) {
    private val modelsDir = File(context.filesDir, "models").also { it.mkdirs() }
    private val configsDir = File(context.filesDir, "model_configs").also { it.mkdirs() }

    fun getAllModels(): Flow<List<ModelMetaEntity>> = modelMetaDao.getAll()

    suspend fun getActiveModel(): ModelMetaEntity? = modelMetaDao.getActive()

    suspend fun activateModel(id: Long) {
        modelMetaDao.activateModel(id)
        prefs.setActiveModelId(id)
    }

    suspend fun saveModel(
        modelFile: File,
        config: ModelConfig,
        sourceUrl: String? = null
    ): Long {
        val configFile = File(configsDir, "${modelFile.nameWithoutExtension}_config.json")
        configFile.writeText(gson.toJson(config))

        val id = modelMetaDao.insert(
            ModelMetaEntity(
                name       = config.name,
                format     = config.format.name,
                filePath   = modelFile.absolutePath,
                configPath = configFile.absolutePath,
                sourceUrl  = sourceUrl,
                sha256     = config.sha256
            )
        )
        // Auto-activate if this is the first model
        if (modelMetaDao.count() == 1) activateModel(id)
        return id
    }

    suspend fun deleteModel(id: Long) {
        val meta = modelMetaDao.getById(id) ?: return
        File(meta.filePath).delete()
        File(meta.configPath).delete()
        modelMetaDao.delete(id)
    }

    fun readConfig(meta: ModelMetaEntity): ModelConfig =
        gson.fromJson(File(meta.configPath).readText(), ModelConfig::class.java)
}
