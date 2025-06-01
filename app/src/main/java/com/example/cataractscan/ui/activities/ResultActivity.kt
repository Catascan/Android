package com.example.cataractscan.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ActivityResultBinding
import com.example.cataractscan.api.models.AnalysisResult
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private var imageUri: Uri? = null
    private var analysisResult: AnalysisResult? = null

    companion object {
        const val IMAGE_URI = "image_uri"
        const val ANALYSIS_RESULT = "analysis_result"
        private const val TAG = "ResultActivity"

        // Define possible cataract status values
        private const val STATUS_NORMAL = "normal"
        private const val STATUS_IMMATURE = "immature"
        private const val STATUS_MATURE = "mature"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d(TAG, "ResultActivity started")

        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        imageUri = intent.getStringExtra(IMAGE_URI)?.let { Uri.parse(it) }
        val resultJson = intent.getStringExtra(ANALYSIS_RESULT)

        android.util.Log.d(TAG, "Image URI: $imageUri")
        android.util.Log.d(TAG, "Result JSON: $resultJson")

        if (resultJson != null) {
            try {
                analysisResult = Gson().fromJson(resultJson, AnalysisResult::class.java)
                android.util.Log.d(TAG, "Analysis result parsed: ${analysisResult?.prediction}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error parsing analysis result", e)
                showErrorState("Failed to parse analysis result")
                return
            }
        } else {
            android.util.Log.e(TAG, "No result JSON received")
            showErrorState("No analysis data received")
            return
        }

        setupToolbar()

        // Immediately display results since we already have them
        displayResults()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            android.util.Log.d(TAG, "Back button pressed")
            finish()
        }
    }

    private fun displayResults() {
        android.util.Log.d(TAG, "Displaying results...")

        // Load the image first
        imageUri?.let { uri ->
            android.util.Log.d(TAG, "Loading image from URI: $uri")
            Glide.with(this)
                .load(uri)
                .into(binding.ivEyeImage)
        } ?: run {
            // If no local URI, try to load from photoUrl in response
            analysisResult?.photoUrl?.let { url ->
                android.util.Log.d(TAG, "Loading image from API URL: $url")
                Glide.with(this)
                    .load(url)
                    .into(binding.ivEyeImage)
            }
        }

        // Display the results with animation
        analysisResult?.let { result ->
            android.util.Log.d(TAG, "Processing analysis result:")
            android.util.Log.d(TAG, "- Message: ${result.message}")
            android.util.Log.d(TAG, "- Prediction: ${result.prediction}")
            android.util.Log.d(TAG, "- Normal: ${result.confidence_scores.normal}")
            android.util.Log.d(TAG, "- Immature: ${result.confidence_scores.immature}")
            android.util.Log.d(TAG, "- Mature: ${result.confidence_scores.mature}")

            try {
                // Show result layout with fade in animation
                binding.resultLayout.visibility = View.VISIBLE
                binding.resultLayout.alpha = 0f
                binding.resultLayout.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start()

                // Set prediction text with better formatting
                val predictionText = when(result.prediction.lowercase()) {
                    STATUS_NORMAL -> "Normal Tidak Terdeteksi Katarak"
                    STATUS_IMMATURE -> "Katarak Terdeteksi Immature"
                    STATUS_MATURE -> "Katarak Terdeteksi Mature"
                    else -> result.prediction.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                }
                binding.tvDiagnosis.text = predictionText
                android.util.Log.d(TAG, "Diagnosis text set: $predictionText")

                // Set color based on prediction
                val colorRes = when(result.prediction.lowercase()) {
                    STATUS_NORMAL -> R.color.statusNormal
                    STATUS_IMMATURE -> R.color.statusMild
                    STATUS_MATURE -> R.color.statusSevere
                    else -> R.color.statusMild
                }
                binding.tvDiagnosis.setTextColor(getColor(colorRes))

                // Check if severity indicator exists
                try {
                    binding.severityIndicator.setBackgroundColor(getColor(colorRes))
                    android.util.Log.d(TAG, "Severity indicator color set")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Severity indicator not found in layout")
                }

                // Set explanation with icon
                binding.tvExplanation.text = result.explanation
                android.util.Log.d(TAG, "Explanation set: ${result.explanation}")

                // Set explanation icon
                try {
                    binding.ivExplanation.setImageResource(R.drawable.ic_explanation)
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Explanation icon not set: ${e.message}")
                }

                // Display confidence scores with animation delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val confidenceScores = result.confidence_scores
                    setupConfidenceScores(confidenceScores.normal, confidenceScores.immature, confidenceScores.mature)
                }, 200)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error displaying results", e)
                showErrorState("Error displaying analysis results")
            }
        } ?: run {
            android.util.Log.e(TAG, "No analysis result data")
            showErrorState("No analysis result data available")
        }
    }

    private fun setupConfidenceScores(normal: Float, immature: Float, mature: Float) {
        // Convert to percentages
        val normalPercent = (normal * 100).toInt()
        val immaturePercent = (immature * 100).toInt()
        val maturePercent = (mature * 100).toInt()

        android.util.Log.d(TAG, "Setting confidence scores: Normal=$normalPercent%, Immature=$immaturePercent%, Mature=$maturePercent%")

        // Update text percentages first
        binding.tvNormalPercent.text = "$normalPercent%"
        binding.tvImmaturePercent.text = "$immaturePercent%"
        binding.tvMaturePercent.text = "$maturePercent%"

        // Animate progress bars from 0 to target value
        animateProgressBar(binding.progressNormal, normalPercent)
        animateProgressBar(binding.progressImmature, immaturePercent, 100)
        animateProgressBar(binding.progressMature, maturePercent, 200)
    }

    private fun animateProgressBar(progressBar: com.google.android.material.progressindicator.LinearProgressIndicator, targetProgress: Int, delay: Long = 0) {
        progressBar.progress = 0

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val animator = android.animation.ValueAnimator.ofInt(0, targetProgress)
            animator.duration = 1000
            animator.interpolator = android.view.animation.DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
            animator.start()
        }, delay)
    }

    private fun showErrorState(errorMessage: String) {
        android.util.Log.e(TAG, "Showing error state: $errorMessage")
        binding.resultLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.tvErrorMessage.text = errorMessage

        // Setup retry button if exists
        try {
            binding.btnRetry.setOnClickListener {
                android.util.Log.d(TAG, "Retry button clicked")
                finish()
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Retry button not found in layout")
        }
    }

    private fun shareResults() {
        android.util.Log.d(TAG, "Sharing results...")

        if (binding.resultLayout.visibility != View.VISIBLE) {
            android.util.Log.w(TAG, "Cannot share - results not visible")
            return
        }

        val confidenceText = analysisResult?.let { result ->
            val normal = (result.confidence_scores.normal * 100).toInt()
            val immature = (result.confidence_scores.immature * 100).toInt()
            val mature = (result.confidence_scores.mature * 100).toInt()

            "Confidence Scores:\n" +
                    "Normal: $normal%\n" +
                    "Mild Cataract: $immature%\n" +
                    "Severe Cataract: $mature%\n"
        } ?: ""

        val shareText = "${binding.tvDiagnosis.text}\n\n" +
                confidenceText + "\n" +
                "Explanation: ${binding.tvExplanation.text}\n\n" +
                "Analysis performed on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"

            // If we have an image URI, share that too
            imageUri?.let { uri ->
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(Intent.createChooser(shareIntent, "Share Analysis Results"))
    }
}