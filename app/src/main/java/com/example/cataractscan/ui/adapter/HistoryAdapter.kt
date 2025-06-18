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
        // BASE URL sesuai dengan backend Anda
        private const val BASE_URL = "https://hctqpvn8-3000.asse.devtunnels.ms"
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
            Log.d(TAG, "=== BINDING HISTORY ITEM ===")
            Log.d(TAG, "Item ID: ${item.id}")
            Log.d(TAG, "Prediction: ${item.prediction}")
            Log.d(TAG, "Original photoUrl: '${item.photoUrl}'")

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
            Log.d(TAG, "=== IMAGE LOADING DEBUG ===")
            Log.d(TAG, "Raw photoUrl from API: '$photoUrl'")

            if (photoUrl.isNullOrEmpty()) {
                Log.w(TAG, "Photo URL is null or empty")
                binding.ivHistoryImage.setImageResource(R.drawable.ic_place_holder)
                return
            }

            // Clean the URL based on what backend sends
            val finalUrl = buildImageUrl(photoUrl)
            Log.d(TAG, "Final image URL: '$finalUrl'")

            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_place_holder)
                .error(R.drawable.ic_place_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(15000)

            try {
                Glide.with(binding.root.context)
                    .load(finalUrl)
                    .apply(requestOptions)
                    .into(binding.ivHistoryImage)

                Log.d(TAG, "Glide load initiated for: $finalUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading image with Glide: ${e.message}", e)
                binding.ivHistoryImage.setImageResource(R.drawable.ic_place_holder)
            }
        }

        private fun buildImageUrl(photoUrl: String): String {
            Log.d(TAG, "Building image URL from: $photoUrl")

            // Jika URL sudah lengkap (absolute URL), gunakan langsung
            if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
                Log.d(TAG, "URL is already absolute: $photoUrl")
                return photoUrl
            }

            // Jika URL relatif, tambahkan base URL
            val cleanedUrl = when {
                // Jika dimulai dengan "/", langsung tambahkan ke base URL
                photoUrl.startsWith("/") -> {
                    "$BASE_URL$photoUrl"
                }

                // Jika dimulai dengan "uploads/" atau folder lain, tambahkan "/"
                photoUrl.startsWith("uploads/") ||
                        photoUrl.startsWith("images/") ||
                        photoUrl.startsWith("static/") ||
                        photoUrl.startsWith("media/") -> {
                    "$BASE_URL/$photoUrl"
                }

                // Jika hanya nama file, asumsikan ada di folder uploads
                !photoUrl.contains("/") &&
                        (photoUrl.contains(".jpg") || photoUrl.contains(".png") ||
                                photoUrl.contains(".jpeg") || photoUrl.contains(".webp") ||
                                photoUrl.contains(".gif") || photoUrl.contains(".bmp")) -> {
                    "$BASE_URL/uploads/$photoUrl"
                }

                // Default: langsung tambahkan ke base URL dengan "/"
                else -> {
                    "$BASE_URL/$photoUrl"
                }
            }

            Log.d(TAG, "Built URL: $cleanedUrl")
            return cleanedUrl
        }

        private fun formatDateTime(createdAt: String) {
            try {
                // Multiple date formats untuk compatibility
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                    "yyyy-MM-dd HH:mm:ss"
                )

                var parsedDate: Date? = null
                for (format in formats) {
                    try {
                        val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                        parsedDate = inputFormat.parse(createdAt)
                        if (parsedDate != null) {
                            Log.d(TAG, "Successfully parsed date with format: $format")
                            break
                        }
                    } catch (e: Exception) {
                        // Try next format
                        continue
                    }
                }

                if (parsedDate != null) {
                    // Format untuk display
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    binding.tvDate.text = dateFormat.format(parsedDate)
                    binding.tvTime.text = timeFormat.format(parsedDate)

                    Log.d(TAG, "Formatted date: ${binding.tvDate.text}, time: ${binding.tvTime.text}")
                } else {
                    // Fallback parsing untuk format yang tidak standard
                    Log.w(TAG, "Could not parse date with standard formats, using fallback")

                    val datePart = createdAt.substringBefore('T')
                    val timePart = createdAt.substringAfter('T').substringBefore('.')

                    binding.tvDate.text = datePart.replace("-", "/")
                    binding.tvTime.text = if (timePart.length >= 5) {
                        timePart.substring(0, 5)
                    } else {
                        timePart
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting date: $createdAt", e)

                // Final fallback - just show the raw strings
                try {
                    binding.tvDate.text = createdAt.substringBefore('T')
                    val timePart = createdAt.substringAfter('T').substringBefore('.')
                    binding.tvTime.text = if (timePart.length >= 5) {
                        timePart.substring(0, 5)
                    } else {
                        timePart
                    }
                } catch (ex: Exception) {
                    binding.tvDate.text = "Unknown date"
                    binding.tvTime.text = "Unknown time"
                }
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