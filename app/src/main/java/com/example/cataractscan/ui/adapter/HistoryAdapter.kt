package com.example.cataractscan.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ItemHistoryBinding
import com.example.cataractscan.api.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            // Load image
            Glide.with(binding.root.context)
                .load(item.photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) // Use system drawable
                .error(android.R.drawable.ic_menu_gallery) // Use system drawable
                .into(binding.ivHistoryImage)

            // Set prediction with proper formatting
            val predictionText = when(item.prediction.lowercase()) {
                "normal" -> "Normal - No Cataract"
                "immature" -> "Immature Cataract"
                "mature" -> "Mature Cataract"
                else -> item.prediction.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
            binding.tvPrediction.text = predictionText

            // Set color based on prediction
            val (colorRes, textColor) = when(item.prediction.lowercase()) {
                "normal" -> Pair(R.color.statusNormal, R.color.statusNormal)
                "immature" -> Pair(R.color.statusMild, R.color.statusMild)
                "mature" -> Pair(R.color.statusSevere, R.color.statusSevere)
                else -> Pair(R.color.statusMild, R.color.statusMild)
            }

            binding.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
            binding.tvPrediction.setTextColor(
                ContextCompat.getColor(binding.root.context, textColor)
            )

            // Set explanation
            binding.tvExplanation.text = item.explanation

            // Set analysis ID

            // Format and set date/time
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val date = inputFormat.parse(item.createdAt)

                if (date != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    binding.tvDate.text = dateFormat.format(date)
                    binding.tvTime.text = timeFormat.format(date)
                } else {
                    binding.tvDate.text = "Unknown date"
                    binding.tvTime.text = ""
                }
            } catch (e: Exception) {
                // Fallback if date parsing fails
                binding.tvDate.text = item.createdAt.substringBefore('T')
                binding.tvTime.text = item.createdAt.substringAfter('T').substringBefore('.')
            }

            // Set click listeners
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnViewDetails.setOnClickListener { onItemClick(item) }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}