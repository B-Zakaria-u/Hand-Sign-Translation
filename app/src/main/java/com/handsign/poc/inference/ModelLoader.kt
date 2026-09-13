package com.handsign.poc.inference

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.data.model.ModelFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a model from any source (bundled assets / local URI / GitHub URL)
 * into a pair of (model File, ModelConfig) ready for the adapter factory.
 */
@Singleton
class ModelLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val modelsDir = File(context.filesDir, "models").also { it.mkdirs() }

    // ── Bundled ────────────────────────────────────────────────────────────────

    suspend fun loadBundled(): Pair<File, ModelConfig> = withContext(Dispatchers.IO) {
        val taskDest = File(modelsDir, "bundled_gesture_recognizer.task")
        val hasTaskAsset = try {
            context.assets.open("gesture_recognizer.task").close()
            true
        } catch (_: Exception) {
            false
        }

        if (hasTaskAsset) {
            if (!taskDest.exists() || taskDest.length() < 1000 || ModelValidator.validate(taskDest).isFailure) {
                context.assets.open("gesture_recognizer.task").use { input ->
                    taskDest.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val validation = ModelValidator.validate(taskDest)
            if (validation.isSuccess) {
                val config = ModelConfig(
                    name = "MediaPipe Gestures (Bundled)",
                    format = ModelFormat.MEDIAPIPE_TASK,
                    labels = listOf("Thumb_Up", "Thumb_Down", "Victory", "Pointing_Up", "Open_Palm", "Closed_Fist", "ILoveYou"),
                    confidenceThreshold = 0.5f
                )
                return@withContext Pair(taskDest, config)
            }
        }

        val dest = File(modelsDir, "bundled_model.tflite")
        if (!dest.exists()) {
            context.assets.open("model.tflite").use { it.copyTo(dest.outputStream()) }
        }
        ModelValidator.validate(dest).getOrThrow()
        val configJson = context.assets.open("model_config.json").bufferedReader().readText()
        val config = parseConfig(configJson)
        Pair(dest, config)
    }

    // ── Local File URI ─────────────────────────────────────────────────────────

    suspend fun loadFromUri(uri: Uri, configJson: String? = null): Pair<File, ModelConfig> =
        withContext(Dispatchers.IO) {
            val ext = getFileExtension(uri)
            val dest = File(modelsDir, "import_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)!!.use { it.copyTo(dest.outputStream()) }

            ModelValidator.validate(dest).getOrThrow()

            val targetFile = if (ext == "keras" || ext == "h5") {
                convertKerasOnDevice(dest)
            } else {
                dest
            }

            val config = if (configJson != null) parseConfig(configJson)
                         else inferConfig(targetFile)
            Pair(targetFile, config)
        }

    private fun getFileExtension(uri: Uri): String {
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
        } ?: uri.path ?: ""
        return fileName.substringAfterLast('.', "tflite").lowercase()
    }

    private suspend fun convertKerasOnDevice(kerasFile: File): File = withContext(Dispatchers.IO) {
        val outputFile = File(modelsDir, "${kerasFile.nameWithoutExtension}_converted.tflite")
        try {
            val pythonClass = Class.forName("com.chaquo.python.Python")
            val isStartedMethod = pythonClass.getMethod("isStarted")
            val getInstanceMethod = pythonClass.getMethod("getInstance")
            if (!(isStartedMethod.invoke(null) as Boolean)) {
                val platformClass = Class.forName("com.chaquo.python.android.AndroidPlatform")
                val platformConst = platformClass.getConstructor(Context::class.java).newInstance(context)
                pythonClass.getMethod("start", Class.forName("com.chaquo.python.PythonPlatform")).invoke(null, platformConst)
            }
            val pythonObj = getInstanceMethod.invoke(null)
            val getModuleMethod = pythonClass.getMethod("getModule", String::class.java)
            val moduleObj = getModuleMethod.invoke(pythonObj, "keras_converter")
            val callAttrMethod = moduleObj.javaClass.getMethod("callAttr", String::class.java, Array<Any>::class.java)
            callAttrMethod.invoke(moduleObj, "convert_keras_to_tflite", arrayOf<Any>(kerasFile.absolutePath, outputFile.absolutePath))

            if (outputFile.exists() && outputFile.length() > 32) {
                return@withContext outputFile
            }
        } catch (e: Exception) {
            android.util.Log.w("ModelLoader", "On-device Python conversion unavailable: ${e.message}")
        }

        throw IllegalArgumentException(
            "To load .${kerasFile.extension} files on-device, please convert them to .tflite using tf.lite.TFLiteConverter in Python before importing."
        )
    }

    // ── GitHub URL ─────────────────────────────────────────────────────────────

    suspend fun downloadFromGitHub(
        repoUrl: String,
        branch: String,
        filePath: String,
        expectedSha256: String? = null,
        onProgress: (Int) -> Unit = {}
    ): Pair<File, ModelConfig> = withContext(Dispatchers.IO) {

        // Security: validate GitHub URL format
        val repoRegex = Regex("^https://github\\.com/[\\w.\\-]+/[\\w.\\-]+$")
        require(repoRegex.matches(repoUrl)) { "Invalid GitHub URL: must match https://github.com/user/repo" }
        require(!filePath.contains("..")) { "Path traversal attempt detected in filePath" }

        val rawUrl = repoUrl.replace("github.com", "raw.githubusercontent.com") +
                     "/$branch/$filePath"

        val request = Request.Builder().url(rawUrl).build()
        val response = okHttpClient.newCall(request).execute()
        require(response.isSuccessful) { "Download failed with HTTP ${response.code}: $rawUrl" }

        val ext  = filePath.substringAfterLast(".", "tflite")
        val dest = File(modelsDir, "github_${System.currentTimeMillis()}.$ext")
        val body = response.body ?: error("Empty response body")
        val totalBytes = body.contentLength()
        var bytesRead  = 0L

        body.byteStream().use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(8192)
                var n: Int
                while (input.read(buf).also { n = it } != -1) {
                    output.write(buf, 0, n)
                    bytesRead += n
                    if (totalBytes > 0) onProgress((bytesRead * 100 / totalBytes).toInt())
                }
            }
        }

        ModelValidator.validate(dest).getOrThrow()

        val targetFile = if (ext == "keras" || ext == "h5") {
            convertKerasOnDevice(dest)
        } else {
            dest
        }

        // Integrity check
        if (expectedSha256 != null) {
            require(ModelValidator.verifySha256(dest, expectedSha256)) {
                "SHA-256 mismatch — file may be corrupted or tampered with"
            }
        }

        // Try to fetch sidecar config
        val configUrl = rawUrl.substringBeforeLast(".") + "_config.json"
        val configJson = try {
            val cfgResp = okHttpClient.newCall(Request.Builder().url(configUrl).build()).execute()
            if (cfgResp.isSuccessful) cfgResp.body?.string() else null
        } catch (_: Exception) { null }

        val config = if (configJson != null) parseConfig(configJson)
                     else inferConfig(targetFile)
        Pair(targetFile, config)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun parseConfig(json: String): ModelConfig = gson.fromJson(json, ModelConfig::class.java)

    private fun inferConfig(file: File): ModelConfig {
        val format = when (file.extension.lowercase()) {
            "tflite"      -> ModelFormat.TFLITE_LANDMARK
            "task"        -> ModelFormat.MEDIAPIPE_TASK
            "onnx"        -> ModelFormat.ONNX
            "ptl"         -> ModelFormat.TORCH_MOBILE
            "keras", "h5" -> ModelFormat.TFLITE_LANDMARK
            else          -> ModelFormat.TFLITE_LANDMARK
        }
        return ModelConfig(
            name   = file.nameWithoutExtension,
            format = format,
            labels = ('A'..'Z').map { it.toString() } + listOf("SPACE")
        )
    }
}
