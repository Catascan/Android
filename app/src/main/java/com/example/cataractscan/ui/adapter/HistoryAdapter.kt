package com.example.cataractscan.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ItemHistoryBinding
import com.example.cataractscan.api.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val TAG = "HistoryAdapter"
    }

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
            // Load image with proper URL handling
            loadImage(item.photoUrl)

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

            // Format and set date/time
            formatDateTime(item.createdAt)

            // Set click listeners
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnViewDetails.setOnClickListener { onItemClick(item) }
        }

        private fun loadImage(photoUrl: String?) {
            Log.d(TAG, "Loading image from URL: $photoUrl")

            if (photoUrl.isNullOrEmpty()) {
                Log.w(TAG, "Photo URL is null or empty")
                binding.ivHistoryImage.setImageResource(R.drawable.ic_place_holder)
                return
            }

            // Fix the malformed URL - remove the duplicate domain part
            val cleanUrl = cleanPhotoUrl(photoUrl)
            Log.d(TAG, "Cleaned URL: $cleanUrl")

            val requestOptions = RequestOptions()
                .placeholder(android.R.drawable.ic_menu_gallery) // Use system drawable
                .error(android.R.drawable.ic_menu_gallery) // Use system drawable
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(15000) // 15 second timeout

            Glide.with(binding.root.context)
                .load(cleanUrl)
                .apply(requestOptions)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "Failed to load image: $cleanUrl", e)
                        e?.logRootCauses(TAG)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "Image loaded successfully: $cleanUrl")
                        return false
                    }
                })
                .into(binding.ivHistoryImage)
        }

        private fun cleanPhotoUrl(photoUrl: String): String {
            // Handle malformed URLs like:
            // "http://catascan-api-544673494956.asia-southeast1.run.app/https://storage.googleapis.com/..."
            return when {
                photoUrl.contains("run.app/https://") -> {
                    // Extract the actual Google Storage URL
                    val httpsIndex = photoUrl.indexOf("https://", photoUrl.indexOf("run.app/"))
                    if (httpsIndex != -1) {
                        photoUrl.substring(httpsIndex)
                    } else {
                        photoUrl
                    }
                }
                photoUrl.contains("run.app/http://") -> {
                    // Extract the actual URL (though http should be https for Google Storage)
                    val httpIndex = photoUrl.indexOf("http://", photoUrl.indexOf("run.app/"))
                    if (httpIndex != -1) {
                        val extractedUrl = photoUrl.substring(httpIndex)
                        // Convert http to https for Google Cloud Storage
                        if (extractedUrl.contains("googleapis.com") || extractedUrl.contains("googleusercontent.com")) {
                            extractedUrl.replace("http://", "https://")
                        } else {
                            extractedUrl
                        }
                    } else {
                        photoUrl
                    }
                }
                photoUrl.startsWith("//") -> {
                    "https:$photoUrl"
                }
                photoUrl.startsWith("/") -> {
                    "https://storage.googleapis.com$photoUrl"
                }
                else -> photoUrl
            }
        }

        private fun formatDateTime(createdAt: String) {
            try {
                // Try multiple date formats
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                    "yyyy-MM-dd'T'HH:mm:ss"
                )

                var date: Date? = null
                for (format in formats) {
                    try {
                        val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                        date = inputFormat.parse(createdAt)
                        if (date != null) break
                    } catch (e: Exception) {
                        // Try next format
                        continue
                    }
                }

                if (date != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    binding.tvDate.text = dateFormat.format(date)
                    binding.tvTime.text = timeFormat.format(date)
                } else {
                    // Fallback parsing
                    binding.tvDate.text = createdAt.substringBefore('T').replace("-", "/")
                    binding.tvTime.text = createdAt.substringAfter('T').substringBefore('.').substring(0, 5)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting date: $createdAt", e)
                // Final fallback
                binding.tvDate.text = createdAt.substringBefore('T')
                binding.tvTime.text = createdAt.substringAfter('T').substringBefore('.')
            }
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