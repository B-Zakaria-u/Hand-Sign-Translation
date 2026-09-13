package com.handsign.poc.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_meta")
data class ModelMetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ModelFormat.name() string, e.g. "TFLITE_LANDMARK" */
    val format: String,
    /** Absolute path to the .tflite / .onnx / .ptl / .task file */
    val filePath: String,
    /** Absolute path to the sidecar model_config.json */
    val configPath: String,
    /** Original source URL (GitHub or null for local imports) */
    val sourceUrl: String? = null,
    /** SHA-256 hex digest for integrity verification */
    val sha256: String? = null,
    /** 0 = inactive, 1 = currently loaded */
    val isActive: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
