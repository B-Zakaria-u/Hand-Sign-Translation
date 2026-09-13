package com.handsign.poc.data.model

import android.net.Uri

/** Represents where a model originates from */
sealed class ModelSource {
    /** The default model shipped inside assets/ */
    object Bundled : ModelSource()

    /**
     * A model the user imported from the device file system.
     * [uri] is the content URI from the file picker.
     * [configJson] is the raw JSON string of model_config.json (optional — auto-inferred if null).
     */
    data class LocalFile(val uri: Uri, val configJson: String? = null) : ModelSource()

    /**
     * A model to be downloaded from a GitHub repository.
     * [repoUrl]  e.g. "https://github.com/user/repo"
     * [branch]   e.g. "main"
     * [filePath] e.g. "models/asl_model.tflite"
     * [sha256]   optional hex digest for integrity check
     */
    data class GitHub(
        val repoUrl: String,
        val branch: String  = "main",
        val filePath: String,
        val sha256: String? = null
    ) : ModelSource()
}
