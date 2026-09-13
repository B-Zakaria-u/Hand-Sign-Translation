package com.handsign.poc.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.handsign.poc.data.model.ModelConfig
import com.handsign.poc.data.repository.ModelRepository
import com.handsign.poc.inference.ModelLoader
import com.handsign.poc.inference.ModelValidator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Downloads a model from a GitHub raw URL in the background.
 * Reports progress via WorkManager setProgress() and a foreground notification.
 *
 * Input data keys:
 *   - "repo_url"   e.g. "https://github.com/user/repo"
 *   - "branch"     e.g. "main"
 *   - "file_path"  e.g. "models/asl.tflite"
 *   - "sha256"     optional hex digest for integrity check
 *
 * Output data keys (on success):
 *   - "model_id"   Long — the newly saved model's Room ID
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val modelLoader: ModelLoader,
    private val modelRepository: ModelRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_REPO_URL   = "repo_url"
        const val KEY_BRANCH     = "branch"
        const val KEY_FILE_PATH  = "file_path"
        const val KEY_SHA256     = "sha256"
        const val KEY_PROGRESS   = "progress"
        const val KEY_ERROR      = "error"
        const val KEY_MODEL_ID   = "model_id"

        private const val CHANNEL_ID     = "model_download"
        private const val NOTIFICATION_ID = 1001

        fun buildRequest(
            repoUrl: String,
            branch: String,
            filePath: String,
            sha256: String? = null
        ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(
                workDataOf(
                    KEY_REPO_URL  to repoUrl,
                    KEY_BRANCH    to branch,
                    KEY_FILE_PATH to filePath,
                    KEY_SHA256    to sha256
                )
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("model_download")
            .build()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading model…")
            .setProgress(100, 0, true)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        val repoUrl  = inputData.getString(KEY_REPO_URL)  ?: return failure("Missing repo_url")
        val branch   = inputData.getString(KEY_BRANCH)    ?: "main"
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return failure("Missing file_path")
        val sha256   = inputData.getString(KEY_SHA256)

        setForeground(getForegroundInfo())

        return try {
            val (file, config) = modelLoader.downloadFromGitHub(
                repoUrl        = repoUrl,
                branch         = branch,
                filePath       = filePath,
                expectedSha256 = sha256
            ) { progress ->
                setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                updateNotification(progress)
            }
            val modelId = modelRepository.saveModel(
                modelFile = file,
                config    = config,
                sourceUrl = "$repoUrl/blob/$branch/$filePath"
            )
            Result.success(workDataOf(KEY_MODEL_ID to modelId))
        } catch (e: Exception) {
            failure(e.message ?: "Unknown error")
        }
    }

    private fun failure(msg: String): Result =
        Result.failure(workDataOf(KEY_ERROR to msg))

    private fun updateNotification(progress: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                 as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading model… $progress%")
            .setProgress(100, progress, false)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Model Downloads", NotificationManager.IMPORTANCE_LOW
            )
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager).createNotificationChannel(channel)
        }
    }
}
