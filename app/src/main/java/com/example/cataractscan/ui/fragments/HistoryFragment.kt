package com.example.cataractscan.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cataractscan.databinding.FragmentHistoryBinding
import com.example.cataractscan.ui.adapters.HistoryAdapter
import com.example.cataractscan.ui.activities.ResultActivity
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.HistoryItem
import com.example.cataractscan.api.models.AnalysisResult
import com.example.cataractscan.api.models.ConfidenceScores
import com.google.gson.Gson
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var historyAdapter: HistoryAdapter

    companion object {
        private const val TAG = "HistoryFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())
        setupRecyclerView()
        setupClickListeners()
        loadHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { historyItem ->
            openHistoryDetail(historyItem)
        }

        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            loadHistory()
        }

        binding.btnRetry.setOnClickListener {
            loadHistory()
        }
    }

    private fun loadHistory() {
        Log.d(TAG, "=== LOADING HISTORY ===")

        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No auth token available")
            showErrorState("Session expired. Please login again.")
            return
        }

        showLoadingState()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Calling history API with token: Bearer ${token.take(10)}...")

                val response = ApiClient.apiService.getHistory("Bearer $token")

                Log.d(TAG, "=== HISTORY API RESPONSE ===")
                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Is successful: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val historyResponse = response.body()!!
                    Log.d(TAG, "History loaded successfully:")
                    Log.d(TAG, "Message: ${historyResponse.message}")
                    Log.d(TAG, "History count: ${historyResponse.history.size}")

                    // Debug setiap item yang diterima dari API
                    historyResponse.history.forEachIndexed { index, item ->
                        Log.d(TAG, "=== BACKEND HISTORY ITEM $index ===")
                        Log.d(TAG, "ID: ${item.id}")
                        Log.d(TAG, "Prediction: '${item.prediction}'")
                        Log.d(TAG, "PhotoURL: '${item.photoUrl}'")
                        Log.d(TAG, "ConfidenceScores from BACKEND: ${item.confidenceScores}")

                        item.confidenceScores?.let { scores ->
                            val total = scores.normal + scores.immature + scores.mature
                            Log.d(TAG, "BACKEND Confidence Details:")
                            Log.d(TAG, "  Normal: ${scores.normal}")
                            Log.d(TAG, "  Immature: ${scores.immature}")
                            Log.d(TAG, "  Mature: ${scores.mature}")
                            Log.d(TAG, "  Total: $total")
                            Log.d(TAG, "  Format: ${if (total > 10) "Percentage (0-100)" else "Decimal (0.0-1.0)"}")

                            // Identify the highest confidence
                            val maxConfidence = maxOf(scores.normal, scores.immature, scores.mature)
                            val predictedCategory = when (maxConfidence) {
                                scores.normal -> "Normal"
                                scores.immature -> "Immature"
                                scores.mature -> "Mature"
                                else -> "Unknown"
                            }
                            Log.d(TAG, "  Highest confidence: $predictedCategory ($maxConfidence)")
                            Log.d(TAG, "  Backend prediction: ${item.prediction}")

                            if (predictedCategory.lowercase() != item.prediction.lowercase()) {
                                Log.w(TAG, "⚠️ MISMATCH: Prediction='${item.prediction}' but highest confidence is '$predictedCategory'")
                            }
                        } ?: run {
                            Log.w(TAG, "⚠️ No confidence scores from backend for item ${item.id}")
                        }
                        Log.d(TAG, "===============================")
                    }

                    if (historyResponse.history.isEmpty()) {
                        showEmptyState()
                    } else {
                        showHistoryList(historyResponse.history)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "History API error: ${response.code()} - $errorBody")

                    val errorMessage = when (response.code()) {
                        401 -> "Session expired. Please login again."
                        403 -> "Access denied. Please check your permissions."
                        500 -> "Server error. Please try again later."
                        else -> "Failed to load history. Please try again."
                    }
                    showErrorState(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading history", e)

                val errorMessage = when {
                    e.message?.contains("timeout", true) == true -> "Connection timeout. Please check your internet connection."
                    e.message?.contains("network", true) == true -> "No internet connection. Please check your network."
                    e.message?.contains("host", true) == true -> "Cannot reach server. Please try again later."
                    else -> "Error loading history: ${e.localizedMessage ?: "Unknown error"}"
                }
                showErrorState(errorMessage)
            }
        }
    }

    private fun showLoadingState() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.emptyLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showHistoryList(historyList: List<HistoryItem>) {
        Log.d(TAG, "Showing history list with ${historyList.size} items")

        historyAdapter.submitList(historyList)

        binding.rvHistory.visibility = View.VISIBLE
        binding.loadingLayout.visibility = View.GONE
        binding.emptyLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        Log.d(TAG, "Showing empty state")

        binding.emptyLayout.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.loadingLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showErrorState(errorMessage: String) {
        Log.e(TAG, "Showing error state: $errorMessage")

        binding.tvErrorMessage.text = errorMessage
        binding.errorLayout.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.loadingLayout.visibility = View.GONE
        binding.emptyLayout.visibility = View.GONE
    }

    private fun openHistoryDetail(historyItem: HistoryItem) {
        Log.d(TAG, "=== OPENING HISTORY DETAIL ===")
        Log.d(TAG, "Item ID: ${historyItem.id}")
        Log.d(TAG, "Backend prediction: '${historyItem.prediction}'")
        Log.d(TAG, "Backend confidence scores: ${historyItem.confidenceScores}")

        try {
            // PRIORITAS: Selalu gunakan confidence scores asli dari backend jika ada
            val finalConfidenceScores = if (historyItem.confidenceScores != null) {
                Log.d(TAG, "✅ USING ORIGINAL BACKEND CONFIDENCE SCORES")
                Log.d(TAG, "Backend scores breakdown:")
                Log.d(TAG, "  Normal: ${historyItem.confidenceScores.normal}")
                Log.d(TAG, "  Immature: ${historyItem.confidenceScores.immature}")
                Log.d(TAG, "  Mature: ${historyItem.confidenceScores.mature}")

                // Use original backend scores AS-IS
                historyItem.confidenceScores
            } else {
                Log.w(TAG, "⚠️ Backend didn't provide confidence scores, creating fallback")
                createMinimalFallbackScores(historyItem.prediction)
            }

            // Debug final scores yang akan digunakan
            Log.d(TAG, "=== FINAL SCORES FOR RESULT ACTIVITY ===")
            Log.d(TAG, "  Normal: ${finalConfidenceScores.normal}")
            Log.d(TAG, "  Immature: ${finalConfidenceScores.immature}")
            Log.d(TAG, "  Mature: ${finalConfidenceScores.mature}")
            Log.d(TAG, "  Total: ${finalConfidenceScores.normal + finalConfidenceScores.immature + finalConfidenceScores.mature}")

            // Convert HistoryItem to AnalysisResult
            val analysisResult = AnalysisResult(
                message = "History Analysis #${historyItem.id}",
                prediction = historyItem.prediction, // Use exact backend prediction
                explanation = historyItem.explanation,
                confidenceScores = finalConfidenceScores, // Use backend scores
                photoUrl = historyItem.photoUrl,
                id = historyItem.id,
                createdAt = historyItem.createdAt,
                updatedAt = historyItem.updatedAt
            )

            val resultJson = Gson().toJson(analysisResult)
            Log.d(TAG, "Final AnalysisResult JSON length: ${resultJson.length}")

            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                putExtra(ResultActivity.ANALYSIS_RESULT, resultJson)
                putExtra(ResultActivity.IMAGE_URI, historyItem.photoUrl)
                putExtra("IS_HISTORY_VIEW", true)
                putExtra("HISTORY_ID", historyItem.id)
            }

            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening history detail", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Failed to open history detail: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Minimal fallback hanya untuk kasus backend tidak mengirim confidence scores
     * Sebaiknya backend selalu mengirim confidence scores yang benar
     */
    private fun createMinimalFallbackScores(prediction: String): ConfidenceScores {
        Log.w(TAG, "⚠️ Creating minimal fallback scores for: $prediction")

        // Very simple fallback - just put high confidence on the prediction
        return when (prediction.lowercase()) {
            "normal" -> ConfidenceScores(normal = 0.95f, immature = 0.03f, mature = 0.02f)
            "immature", "immature cataract" -> ConfidenceScores(normal = 0.05f, immature = 0.90f, mature = 0.05f)
            "mature", "mature cataract" -> ConfidenceScores(normal = 0.03f, immature = 0.07f, mature = 0.90f)
            else -> ConfidenceScores(normal = 0.33f, immature = 0.33f, mature = 0.34f)
        }.also { scores ->
            Log.w(TAG, "Generated fallback scores: Normal=${scores.normal}, Immature=${scores.immature}, Mature=${scores.mature}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh history when fragment becomes visible again
        if (::historyAdapter.isInitialized && historyAdapter.itemCount > 0) {
            Log.d(TAG, "Fragment resumed, refreshing history")
            loadHistory()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}