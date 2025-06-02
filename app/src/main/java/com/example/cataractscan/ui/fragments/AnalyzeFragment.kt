package com.example.cataractscan.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cataractscan.databinding.FragmentAnalyzeBinding
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.models.AnalysisResult
import com.example.cataractscan.ui.activities.ResultActivity
import com.example.cataractscan.utils.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import android.content.Intent

class AnalyzeFragment : Fragment() {
    private var _binding: FragmentAnalyzeBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private var imageUri: Uri? = null

    companion object {
        private const val TAG = "AnalyzeFragment"
    }

    // Modern way to handle activity result
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            android.util.Log.d(TAG, "Image selected: $it")
            imageUri = it
            // Show the selected image in the preview
            binding.imagePreview.setImageURI(it)
            // Enable the analyze button
            binding.analyzeButton.isEnabled = true
            binding.analyzeButton.alpha = 1.0f
            // Show selected state
            binding.galleryButton.text = "Image Selected ✓"
            // Hide hint text if it exists
            try {
                binding.tvImageHint.visibility = View.GONE
            } catch (e: Exception) {
                // tvImageHint might not exist in layout
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyzeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Initially disable analyze button
        binding.analyzeButton.isEnabled = false
        binding.analyzeButton.alpha = 0.5f
    }

    private fun setupClickListeners() {
        binding.galleryButton.setOnClickListener {
            openGallery()
        }

        binding.analyzeButton.setOnClickListener {
            imageUri?.let { uri ->
                uploadAndAnalyzeImage(uri)
            } ?: run {
                Snackbar.make(binding.root, "Please select an image first", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        android.util.Log.d(TAG, "Opening gallery")
        pickImageLauncher.launch("image/*")
    }

    private fun uploadAndAnalyzeImage(uri: Uri) {
        android.util.Log.d(TAG, "Starting image analysis for URI: $uri")

        // Show loading indicator
        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert URI to file for upload
                val imageFile = createFileFromUri(uri)
                android.util.Log.d(TAG, "Image file created: ${imageFile.name}, size: ${imageFile.length()} bytes")

                // Create multipart request
                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                // Get authorization token
                val token = preferenceManager.getToken()
                if (token.isNullOrEmpty()) {
                    android.util.Log.e(TAG, "No auth token available")
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Snackbar.make(binding.root, "Session expired, please login again", Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }

                android.util.Log.d(TAG, "Calling API with token: Bearer ${token.take(10)}...")

                // Call the API to analyze the image - Updated endpoint
                val response = ApiClient.apiService.analyzeImage("Bearer $token", imagePart)

                android.util.Log.d(TAG, "API Response received:")
                android.util.Log.d(TAG, "- Response code: ${response.code()}")
                android.util.Log.d(TAG, "- Is successful: ${response.isSuccessful}")

                withContext(Dispatchers.Main) {
                    setLoading(false)

                    if (response.isSuccessful && response.body() != null) {
                        val analysisResult = response.body()!!

                        android.util.Log.d(TAG, "Analysis successful!")
                        android.util.Log.d(TAG, "- Message: ${analysisResult.message}")
                        android.util.Log.d(TAG, "- Prediction: ${analysisResult.prediction}")
                        android.util.Log.d(TAG, "- Explanation: ${analysisResult.explanation}")

                        // Save analysis result to local storage
                        saveAnalysisToLocal(analysisResult, uri)

                        // Update statistics
                        updateStatistics(analysisResult)

                        // Show success message briefly
                        Snackbar.make(binding.root, "Analysis completed successfully!", Snackbar.LENGTH_SHORT).show()

                        // Convert the response to JSON string
                        val resultJson = Gson().toJson(analysisResult)
                        android.util.Log.d(TAG, "Result JSON: $resultJson")

                        // Navigate to result screen with the result data
                        val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                            putExtra(ResultActivity.ANALYSIS_RESULT, resultJson)
                            putExtra(ResultActivity.IMAGE_URI, uri.toString())
                        }

                        android.util.Log.d(TAG, "Navigating to ResultActivity")
                        startActivity(intent)

                        // Reset UI after successful analysis
                        resetUI()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e(TAG, "API Error: ${response.code()} - $errorBody")

                        val errorMessage = when (response.code()) {
                            400 -> "Invalid image format. Please select a clear eye image."
                            401 -> "Session expired. Please login again."
                            413 -> "Image too large. Please select a smaller image."
                            422 -> "Unable to process image. Please try another image."
                            500 -> "Server error. Please try again later."
                            else -> "Failed to analyze image. Please try again."
                        }
                        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception during image analysis", e)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    val errorMessage = when {
                        e.message?.contains("timeout", true) == true -> "Connection timeout. Please check your internet connection."
                        e.message?.contains("network", true) == true -> "No internet connection. Please check your network."
                        e.message?.contains("host", true) == true -> "Cannot reach server. Please try again later."
                        else -> "Error: ${e.localizedMessage ?: "Unknown error occurred"}"
                    }
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Menyimpan hasil analisis ke local storage
     */
    private fun saveAnalysisToLocal(analysisResult: AnalysisResult, imageUri: Uri) {
        try {
            // Save to SharedPreferences with timestamp as key
            val timestamp = System.currentTimeMillis().toString()
            val resultJson = Gson().toJson(analysisResult)

            preferenceManager.saveAnalysisResult(timestamp, resultJson)
            android.util.Log.d(TAG, "Analysis result saved locally with timestamp: $timestamp")

            // Optional: Batasi jumlah hasil analisis yang tersimpan (maksimal 50)
            preferenceManager.limitAnalysisResults(50)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save analysis locally", e)
        }
    }

    /**
     * Update statistik setelah analisis berhasil
     */
    private fun updateStatistics(analysisResult: AnalysisResult) {
        try {
            // Increment total scans
            preferenceManager.incrementTotalScans()

            // Increment detected cases if cataract is detected
            val prediction = analysisResult.prediction.lowercase()
            if (prediction.contains("cataract") || prediction.contains("immature") || prediction.contains("mature")) {
                preferenceManager.incrementDetectedCases()
                android.util.Log.d(TAG, "Cataract detected, incrementing detected cases")
            }

            android.util.Log.d(TAG, "Statistics updated - Total scans: ${preferenceManager.getTotalScans()}, Detected cases: ${preferenceManager.getDetectedCases()}")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update statistics", e)
        }
    }

    private fun createFileFromUri(uri: Uri): File {
        android.util.Log.d(TAG, "Converting URI to file: $uri")

        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
        val file = File(requireContext().cacheDir, "cataract_analysis_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.flush()
        outputStream.close()

        android.util.Log.d(TAG, "File created: ${file.name}, size: ${file.length()} bytes")
        return file
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.analyzeButton.isEnabled = !isLoading && imageUri != null
        binding.galleryButton.isEnabled = !isLoading

        if (isLoading) {
            binding.analyzeButton.text = "Analyzing..."
            binding.analyzeButton.alpha = 0.7f
        } else {
            binding.analyzeButton.text = "Analyze Image"
            binding.analyzeButton.alpha = if (imageUri != null) 1.0f else 0.5f
        }
    }

    private fun resetUI() {
        android.util.Log.d(TAG, "Resetting UI")
        imageUri = null
        binding.imagePreview.setImageResource(android.R.drawable.ic_menu_gallery)
        binding.analyzeButton.isEnabled = false
        binding.analyzeButton.alpha = 0.5f
        binding.galleryButton.text = "Select Image from Gallery"

        // Show hint text again if it exists
        try {
            binding.tvImageHint.visibility = View.VISIBLE
            binding.tvImageHint.text = "No image selected"
        } catch (e: Exception) {
            // tvImageHint might not exist
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}