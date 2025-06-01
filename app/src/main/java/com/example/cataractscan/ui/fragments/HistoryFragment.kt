package com.example.cataractscan.ui.fragments

import android.content.Intent
import android.os.Bundle
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
        android.util.Log.d(TAG, "Loading history...")

        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            android.util.Log.e(TAG, "No auth token available")
            showErrorState("Session expired. Please login again.")
            return
        }

        showLoadingState()

        lifecycleScope.launch {
            try {
                android.util.Log.d(TAG, "Calling history API with token: Bearer ${token.take(10)}...")

                val response = ApiClient.apiService.getHistory("Bearer $token")

                android.util.Log.d(TAG, "History API response:")
                android.util.Log.d(TAG, "- Response code: ${response.code()}")
                android.util.Log.d(TAG, "- Is successful: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val historyResponse = response.body()!!
                    android.util.Log.d(TAG, "History loaded successfully:")
                    android.util.Log.d(TAG, "- Message: ${historyResponse.message}")
                    android.util.Log.d(TAG, "- History count: ${historyResponse.history.size}")

                    if (historyResponse.history.isEmpty()) {
                        showEmptyState()
                    } else {
                        showHistoryList(historyResponse.history)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e(TAG, "History API error: ${response.code()} - $errorBody")

                    val errorMessage = when (response.code()) {
                        401 -> "Session expired. Please login again."
                        403 -> "Access denied. Please check your permissions."
                        500 -> "Server error. Please try again later."
                        else -> "Failed to load history. Please try again."
                    }
                    showErrorState(errorMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception loading history", e)

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
        android.util.Log.d(TAG, "Showing history list with ${historyList.size} items")

        historyAdapter.submitList(historyList)

        binding.rvHistory.visibility = View.VISIBLE
        binding.loadingLayout.visibility = View.GONE
        binding.emptyLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        android.util.Log.d(TAG, "Showing empty state")

        binding.emptyLayout.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.loadingLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showErrorState(errorMessage: String) {
        android.util.Log.e(TAG, "Showing error state: $errorMessage")

        binding.tvErrorMessage.text = errorMessage
        binding.errorLayout.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.loadingLayout.visibility = View.GONE
        binding.emptyLayout.visibility = View.GONE
    }

    private fun openHistoryDetail(historyItem: HistoryItem) {
        android.util.Log.d(TAG, "Opening history detail for item #${historyItem.id}")

        try {
            // Convert HistoryItem to AnalysisResult for compatibility with ResultActivity
            val analysisResult = AnalysisResult(
                message = "History item #${historyItem.id}",
                prediction = historyItem.prediction,
                explanation = historyItem.explanation,
                confidence_scores = ConfidenceScores(
                    // Default confidence scores since history doesn't include them
                    normal = if (historyItem.prediction.lowercase() == "normal") 0.95f else 0.1f,
                    immature = if (historyItem.prediction.lowercase() == "immature") 0.85f else 0.1f,
                    mature = if (historyItem.prediction.lowercase() == "mature") 0.90f else 0.1f
                ),
                photoUrl = historyItem.photoUrl
            )

            val resultJson = Gson().toJson(analysisResult)

            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                putExtra(ResultActivity.ANALYSIS_RESULT, resultJson)
                putExtra(ResultActivity.IMAGE_URI, historyItem.photoUrl) // Use photo URL as image URI
                putExtra("IS_HISTORY_VIEW", true) // Flag to indicate this is from history
            }

            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error opening history detail", e)
            // Show error feedback to user
            android.widget.Toast.makeText(
                requireContext(),
                "Failed to open history detail",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh history when fragment becomes visible again
        if (::historyAdapter.isInitialized && historyAdapter.itemCount > 0) {
            android.util.Log.d(TAG, "Fragment resumed, refreshing history")
            loadHistory()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}