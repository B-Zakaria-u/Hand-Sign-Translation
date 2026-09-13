package com.handsign.poc.ui.camera

import android.app.Application
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.handsign.poc.data.db.entity.GestureLogEntity
import com.handsign.poc.data.prefs.AppPreferences
import com.handsign.poc.data.repository.ModelRepository
import com.handsign.poc.data.repository.SessionRepository
import com.handsign.poc.data.repository.UserRepository
import com.handsign.poc.inference.GestureRecognizer
import com.handsign.poc.inference.ModelAdapterFactory
import com.handsign.poc.inference.ModelLoader
import com.handsign.poc.inference.NormalizedLandmark
import com.handsign.poc.inference.pipeline.FrameDebouncer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class CameraUiState {
    object Loading   : CameraUiState()
    object NoModel   : CameraUiState()
    object NoHand    : CameraUiState()
    object Inferring : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val modelRepository: ModelRepository,
    private val modelLoader: ModelLoader,
    private val modelAdapterFactory: ModelAdapterFactory,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val prefs: AppPreferences
) : AndroidViewModel(application) {

    // ── Exposed StateFlows ─────────────────────────────────────────────
    private val _uiState        = MutableStateFlow<CameraUiState>(CameraUiState.Loading)
    private val _currentLetter  = MutableStateFlow("")
    private val _confidence     = MutableStateFlow(0f)
    private val _composedText   = MutableStateFlow("")
    private val _landmarks      = MutableStateFlow<List<List<NormalizedLandmark>>>(emptyList())
    private val _imageWidth     = MutableStateFlow(640)
    private val _imageHeight    = MutableStateFlow(480)
    private val _inferenceMs    = MutableStateFlow(0L)
    private val _modelName      = MutableStateFlow("—")
    private val _sessionSaved   = MutableStateFlow(false)

    val uiState:       StateFlow<CameraUiState>         = _uiState.asStateFlow()
    val currentLetter: StateFlow<String>                = _currentLetter.asStateFlow()
    val confidence:    StateFlow<Float>                 = _confidence.asStateFlow()
    val composedText:  StateFlow<String>                = _composedText.asStateFlow()
    val landmarks:     StateFlow<List<List<NormalizedLandmark>>> = _landmarks.asStateFlow()
    val imageWidth:    StateFlow<Int>                   = _imageWidth.asStateFlow()
    val imageHeight:   StateFlow<Int>                   = _imageHeight.asStateFlow()
    val inferenceMs:   StateFlow<Long>                  = _inferenceMs.asStateFlow()
    val modelName:     StateFlow<String>                = _modelName.asStateFlow()
    val sessionSaved:  StateFlow<Boolean>               = _sessionSaved.asStateFlow()

    // ── Private state ──────────────────────────────────────────────────
    private var activeRecognizer: GestureRecognizer? = null
    private val debouncer = FrameDebouncer()
    private val gestureLogs = mutableListOf<GestureLogEntity>()
    private val sessionStartTime = System.currentTimeMillis()
    private var activeSessionId  = -1L

    init {
        loadActiveModel()
    }

    private fun loadActiveModel() {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Loading
            try {
                val meta = modelRepository.getActiveModel()
                val metaFile = meta?.let { java.io.File(it.filePath) }

                val (file, config) = if (meta != null && metaFile != null && metaFile.exists() && com.handsign.poc.inference.ModelValidator.validate(metaFile).isSuccess) {
                    Pair(metaFile, modelRepository.readConfig(meta))
                } else {
                    if (meta != null) {
                        runCatching { modelRepository.deleteModel(meta.id) }
                    }
                    val bundled = modelLoader.loadBundled()
                    runCatching { modelRepository.saveModel(bundled.first, bundled.second) }
                    bundled
                }

                val recognizer = modelAdapterFactory.create(config, file)
                withContext(Dispatchers.Default) { recognizer.initialize() }
                swapRecognizer(recognizer)
                _modelName.value = config.name
                _uiState.value = CameraUiState.NoHand
            } catch (e: Exception) {
                android.util.Log.e("CameraViewModel", "Error in loadActiveModel", e)
                _uiState.value = CameraUiState.Error(e.message ?: "Failed to load model")
            }
        }
    }

    private fun swapRecognizer(new: GestureRecognizer) {
        activeRecognizer?.close()
        activeRecognizer = new
        debouncer.reset()
    }

    /** Called from CameraFragment for each camera frame */
    fun onNewFrame(imageProxy: ImageProxy) {
        val recognizer = activeRecognizer ?: run { imageProxy.close(); return }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = recognizer.recognize(imageProxy)
                if (result == null || result.landmarks.isNullOrEmpty()) {
                    _uiState.value = CameraUiState.NoHand
                    _landmarks.value = emptyList()
                    return@launch
                }
                _uiState.value  = CameraUiState.Inferring
                _confidence.value = result.confidence
                _inferenceMs.value = result.inferenceMs
                _landmarks.value = result.landmarks
                _imageWidth.value = result.imageWidth
                _imageHeight.value = result.imageHeight

                if (result.letter.isNotEmpty() && result.letter != "—") {
                    val committed = debouncer.submit(result.letter)
                    if (committed != null) {
                        _currentLetter.value = committed
                        val char = if (committed == "SPACE") " " else committed
                        _composedText.update { it + char }
                        gestureLogs += GestureLogEntity(
                            sessionId    = activeSessionId,
                            letter       = committed,
                            confidence   = result.confidence,
                            wasCommitted = 1
                        )
                    }
                } else {
                    debouncer.reset()
                }
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(e.message ?: "Inference error")
            }
        }
    }

    fun onBackspace() {
        _composedText.update { if (it.isNotEmpty()) it.dropLast(1) else it }
        debouncer.reset()
        _currentLetter.value = ""
    }

    fun onClearText() {
        _composedText.value = ""
        debouncer.reset()
        _currentLetter.value = ""
    }

    fun onSaveSession() {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            val text = _composedText.value
            if (text.isBlank()) return@launch
            val sessionId = sessionRepository.startSession(user.id, _modelName.value)
            sessionRepository.closeSession(
                sessionId    = sessionId,
                composedText = text,
                modelName    = _modelName.value,
                startedAt    = sessionStartTime,
                logs         = gestureLogs.map { it.copy(sessionId = sessionId) }
            )
            _sessionSaved.value = true
            _sessionSaved.value = false
        }
    }

    /** Hot-swap the active model without restarting the Fragment */
    fun reloadModel() = loadActiveModel()

    override fun onCleared() {
        super.onCleared()
        activeRecognizer?.close()
    }
}
