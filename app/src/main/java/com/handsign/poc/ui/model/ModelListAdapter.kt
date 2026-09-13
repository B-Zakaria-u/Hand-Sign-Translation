package com.handsign.poc.ui.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.handsign.poc.data.db.entity.ModelMetaEntity
import com.handsign.poc.databinding.ItemModelBinding
import java.text.SimpleDateFormat
import java.util.*

class ModelListAdapter(
    private val onActivate: (ModelMetaEntity) -> Unit,
    private val onDelete: (ModelMetaEntity) -> Unit
) : ListAdapter<ModelMetaEntity, ModelListAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ModelMetaEntity>() {
            override fun areItemsTheSame(a: ModelMetaEntity, b: ModelMetaEntity) = a.id == b.id
            override fun areContentsTheSame(a: ModelMetaEntity, b: ModelMetaEntity) = a == b
        }
        private val DATE_FMT = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }

    inner class ViewHolder(private val binding: ItemModelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meta: ModelMetaEntity) {
            binding.tvModelName.text   = meta.name
            binding.tvFormat.text      = meta.format
            binding.tvSource.text      = if (meta.sourceUrl != null) "GitHub" else "Local file"
            binding.tvDate.text        = DATE_FMT.format(Date(meta.createdAt))
            binding.chipActive.isVisible = meta.isActive == 1

            binding.btnActivate.isEnabled = meta.isActive == 0
            binding.btnActivate.setOnClickListener { onActivate(meta) }
            binding.btnDelete.setOnClickListener { onDelete(meta) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
