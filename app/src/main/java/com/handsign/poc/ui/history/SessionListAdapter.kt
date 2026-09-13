package com.handsign.poc.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.handsign.poc.data.db.entity.SessionEntity
import com.handsign.poc.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.*

class SessionListAdapter(
    private val onDelete: (SessionEntity) -> Unit
) : ListAdapter<SessionEntity, SessionListAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SessionEntity>() {
            override fun areItemsTheSame(a: SessionEntity, b: SessionEntity) = a.id == b.id
            override fun areContentsTheSame(a: SessionEntity, b: SessionEntity) = a == b
        }
        private val DATE_FMT = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    }

    inner class ViewHolder(private val b: ItemSessionBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(session: SessionEntity) {
            b.tvSessionText.text = session.composedText.ifBlank { "(empty)" }
            b.tvSessionDate.text = DATE_FMT.format(Date(session.startedAt))
            b.tvSessionModel.text = session.modelName
            b.tvLetterCount.text = "${session.composedText.trim().length} letters"
            val acc = session.accuracyAvg?.let { "Avg: ${"%.0f".format(it * 100)}%" } ?: ""
            b.tvAccuracy.text = acc
            b.ivSyncStatus.setImageResource(
                if (session.syncedToFirebase == 1) android.R.drawable.ic_menu_upload
                else android.R.drawable.ic_menu_save
            )
            b.btnDelete.setOnClickListener { onDelete(session) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}
