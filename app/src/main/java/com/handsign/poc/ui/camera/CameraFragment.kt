package com.handsign.poc.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.handsign.poc.R
import com.handsign.poc.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var useFrontCamera = true

    // ── Permission ─────────────────────────────────────────────────────
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else showPermissionDenied()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> startCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(requireContext(),
                    "Camera permission is required for hand gesture detection",
                    Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val cameraSelector = if (useFrontCamera)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also { it.surfaceProvider = binding.previewView.surfaceProvider }

        var frameCount = 0
        val frameInterval = 3

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    frameCount++
                    if (frameCount % frameInterval == 0) {
                        viewModel.onNewFrame(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is CameraUiState.Loading   -> binding.chipStatus.text = "Loading model…"
                            is CameraUiState.NoHand    -> binding.chipStatus.text = "No hand detected"
                            is CameraUiState.NoModel   -> binding.chipStatus.text = "No model loaded"
                            is CameraUiState.Inferring -> binding.chipStatus.text = "Detecting…"
                            is CameraUiState.Error     -> {
                                binding.chipStatus.text = "Error"
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.currentLetter.collect { letter ->
                        binding.tvLetter.text = letter.ifBlank { "—" }
                    }
                }

                launch {
                    viewModel.confidence.collect { conf ->
                        val pct = (conf * 100).toInt()
                        binding.tvConfidence.text = "$pct%"
                        binding.confidenceIndicator.progress = pct
                    }
                }

                launch {
                    viewModel.composedText.collect { text ->
                        binding.tvComposedText.text = text
                    }
                }

                launch {
                    viewModel.landmarks.collect { landmarks ->
                        binding.overlayView.setResults(
                            landmarks   = landmarks,
                            imageWidth  = 640,
                            imageHeight = 480,
                            isFrontCamera = useFrontCamera
                        )
                    }
                }

                launch {
                    viewModel.inferenceMs.collect { ms ->
                        binding.tvInferenceMs.text = "${ms}ms"
                    }
                }

                launch {
                    viewModel.modelName.collect { name ->
                        binding.tvModelName.text = name
                    }
                }

                launch {
                    viewModel.sessionSaved.collect { saved ->
                        if (saved) Toast.makeText(requireContext(),
                            "Session saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBackspace.setOnClickListener { viewModel.onBackspace() }
        binding.btnClear.setOnClickListener { viewModel.onClearText() }
        binding.btnSave.setOnClickListener { viewModel.onSaveSession() }
        binding.btnFlipCamera.setOnClickListener {
            useFrontCamera = !useFrontCamera
            bindCameraUseCases()
        }
    }

    private fun showPermissionDenied() {
        binding.previewView.visibility = View.GONE
        binding.tvPermissionDenied.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
