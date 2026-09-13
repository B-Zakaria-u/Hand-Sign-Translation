package com.handsign.poc.ui.model

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.handsign.poc.R
import com.handsign.poc.databinding.DialogGithubImportBinding
import com.handsign.poc.databinding.FragmentModelManagerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ModelManagerFragment : Fragment() {

    private var _binding: FragmentModelManagerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ModelManagerViewModel by viewModels()
    private lateinit var adapter: ModelListAdapter

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        showConfigInputDialog { configJson ->
            viewModel.importFromFile(uri, configJson)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModelManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = ModelListAdapter(
            onActivate = { meta -> viewModel.activateModel(meta.id) },
            onDelete   = { meta ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete model?")
                    .setMessage("'${meta.name}' will be permanently deleted.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteModel(meta.id) }
                    .show()
            }
        )
        binding.rvModels.layoutManager = LinearLayoutManager(requireContext())
        binding.rvModels.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnImportFile.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }
        binding.btnImportGithub.setOnClickListener {
            showGitHubImportDialog()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.models.collect { models ->
                        adapter.submitList(models)
                        binding.tvNoModels.isVisible = models.isEmpty()
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.isVisible = loading
                    }
                }
                launch {
                    viewModel.downloadProgress.collect { progress ->
                        if (progress != null) {
                            binding.downloadProgressBar.isVisible = true
                            binding.downloadProgressBar.progress = progress
                            binding.tvDownloadProgress.text = "Downloading… $progress%"
                        } else {
                            binding.downloadProgressBar.isVisible = false
                            binding.tvDownloadProgress.text = ""
                        }
                    }
                }
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is ModelManagerUiEvent.ShowError   -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                            is ModelManagerUiEvent.ShowSuccess -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun showConfigInputDialog(onConfirm: (String?) -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Model Config")
            .setMessage("Do you have a model_config.json for this model? Paste the JSON or press Skip to auto-detect.")
            .setNegativeButton("Skip") { _, _ -> onConfirm(null) }
            .setPositiveButton("I have it") { _, _ ->
                // Simple text input dialog for JSON
                val input = android.widget.EditText(requireContext()).apply {
                    hint = "Paste model_config.json content here"
                    minLines = 4
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Paste model_config.json")
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Use") { _, _ -> onConfirm(input.text.toString().takeIf { it.isNotBlank() }) }
                    .show()
            }
            .show()
    }

    private fun showGitHubImportDialog() {
        val dialogBinding = DialogGithubImportBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import from GitHub")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Download") { _, _ ->
                val repoUrl  = dialogBinding.etRepoUrl.text.toString().trim()
                val branch   = dialogBinding.etBranch.text.toString().trim().ifBlank { "main" }
                val filePath = dialogBinding.etFilePath.text.toString().trim()
                val sha256   = dialogBinding.etSha256.text.toString().trim().takeIf { it.isNotBlank() }

                if (repoUrl.isBlank() || filePath.isBlank()) {
                    Toast.makeText(requireContext(), "Repo URL and file path are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.importFromGitHub(repoUrl, branch, filePath, sha256)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
