package com.handsign.poc.ui.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.handsign.poc.data.db.entity.ModelMetaEntity
import com.handsign.poc.data.repository.ModelRepository
import com.handsign.poc.inference.ModelLoader
import com.handsign.poc.worker.ModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ModelManagerUiEvent {
    data class ShowError(val message: String) : ModelManagerUiEvent()
    data class ShowSuccess(val message: String) : ModelManagerUiEvent()
}

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val modelLoader: ModelLoader,
    private val workManager: WorkManager
) : ViewModel() {

    val models: StateFlow<List<ModelMetaEntity>> = modelRepository.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _event = MutableSharedFlow<ModelManagerUiEvent>()
    val event: SharedFlow<ModelManagerUiEvent> = _event.asSharedFlow()

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    fun importFromFile(uri: Uri, configJson: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (file, config) = modelLoader.loadFromUri(uri, configJson)
                modelRepository.saveModel(file, config)
                _event.emit(ModelManagerUiEvent.ShowSuccess("Model '${config.name}' imported"))
            } catch (e: Exception) {
                _event.emit(ModelManagerUiEvent.ShowError(e.message ?: "Import failed"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importFromGitHub(repoUrl: String, branch: String, filePath: String, sha256: String?) {
        val request = ModelDownloadWorker.buildRequest(repoUrl, branch, filePath, sha256)
        workManager.enqueue(request)
        // Observe progress
        workManager.getWorkInfoByIdLiveData(request.id).observeForever { info ->
            val progress = info?.progress?.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
            _downloadProgress.value = progress
            if (info?.state?.isFinished == true) {
                _downloadProgress.value = null
                val error = info.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                viewModelScope.launch {
                    if (error != null) _event.emit(ModelManagerUiEvent.ShowError(error))
                    else _event.emit(ModelManagerUiEvent.ShowSuccess("Model downloaded and ready"))
                }
            }
        }
    }

    fun activateModel(id: Long) {
        viewModelScope.launch {
            modelRepository.activateModel(id)
            _event.emit(ModelManagerUiEvent.ShowSuccess("Model activated"))
        }
    }

    fun deleteModel(id: Long) {
        viewModelScope.launch {
            modelRepository.deleteModel(id)
            _event.emit(ModelManagerUiEvent.ShowSuccess("Model deleted"))
        }
    }
}
