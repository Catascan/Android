package com.example.cataractscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cataractscan.R
import com.example.cataractscan.databinding.FragmentHomeBinding
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Set fullscreen flag
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        // Setup initial UI with cached data
        setupInitialUI()

        // Load fresh user profile data from API
        loadUserProfile()

        // Setup card click listeners
        setupCardClickListeners()

        // Setup scrolling text animations
        setupScrollingText()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this fragment
        loadUserProfile()

        // Restart scrolling text animations
        restartScrollingAnimations()
    }

    private fun setupInitialUI() {
        // Display cached user information from preferences
        val email = preferenceManager.getEmail() ?: "email@example.com"
        val username = preferenceManager.getUsername() ?: preferenceManager.getName() ?: "User"
        val profileImage = preferenceManager.getProfileImage()

        // Set email
        binding.tvEmail.text = email

        // Set welcome text with username
        binding.tvWelcome.text = "Selamat datang, $username!"

        // Activate marquee for welcome text
        binding.tvWelcome.isSelected = true
        binding.tvWelcome.requestFocus()

        // Load profile image with Glide
        profileImage?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions
                    .circleCropTransform()
                    .placeholder(R.drawable.splash_icon)
                    .error(R.drawable.splash_icon))
                .into(binding.profileImage)
        }

        // BARIS DI BAWAH INI TELAH DIHAPUS: setupInitialStats()
    }

    // FUNGSI DI BAWAH INI TELAH DIHAPUS KARENA TIDAK DIGUNAKAN LAGI
    /*
    private fun setupInitialStats() {
        // Set default or cached statistics
        // You can modify these values based on actual user data from API
        binding.tvTotalScans.text = preferenceManager.getTotalScans()?.toString() ?: "0"
        binding.tvDetectedCases.text = preferenceManager.getDetectedCases()?.toString() ?: "0"
        binding.tvAccuracy.text = "${preferenceManager.getAccuracy() ?: 95}%"
    }
    */

    private fun setupScrollingText() {
        // Setup scrolling text for information card
        binding.tvScrollingText.isSelected = true
        binding.tvScrollingText.requestFocus()

        // Setup scrolling banner
        binding.tvScrollingBanner.isSelected = true
        binding.tvScrollingBanner.requestFocus()
    }

    private fun restartScrollingAnimations() {
        // Restart marquee animations when fragment resumes
        binding.tvWelcome.isSelected = true
        binding.tvScrollingText.isSelected = true
        binding.tvScrollingBanner.isSelected = true
    }

    private fun loadUserProfile() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            return
        }

        lifecycleScope.launch {
            try {
                // Call getUserProfile endpoint to get fresh data
                val response = ApiClient.apiService.getUserProfile("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    val userProfileData = profileResponse.user

                    // Update UI with fresh data from API
                    val currentEmail = preferenceManager.getEmail()
                    if (currentEmail != null) {
                        binding.tvEmail.text = currentEmail
                    }

                    // Update welcome message with username
                    binding.tvWelcome.text = "Selamat datang, ${userProfileData.username}!"

                    // Load profile image with Glide
                    userProfileData.imageLink?.let { imageUrl ->
                        Glide.with(this@HomeFragment)
                            .load(imageUrl)
                            .apply(RequestOptions
                                .circleCropTransform()
                                .placeholder(R.drawable.splash_icon)
                                .error(R.drawable.splash_icon))
                            .into(binding.profileImage)
                    }

                    // Update preferences with latest username and image
                    preferenceManager.saveUserProfileData(
                        userProfileData.id,
                        userProfileData.username,
                        userProfileData.imageLink
                    )

                    // Load and update user statistics if available from API
                    // loadUserStatistics(token) // BARIS INI JUGA DIKOMENTARI/DIHAPUS JIKA TIDAK ADA STATISTIK LAGI

                } else {
                    // Handle API error silently
                }
            } catch (e: Exception) {
                // Handle network error silently
            }
        }
    }

    private fun loadUserStatistics(token: String) {
        // FUNGSI INI JUGA DIHAPUS KARENA TIDAK DIGUNAKAN LAGI
        /*
        lifecycleScope.launch {
            try {
                // Example API call for statistics (implement according to your API)
                // val statsResponse = ApiClient.apiService.getUserStatistics("Bearer $token")

                // For now, we'll use mock data or cached data
                // You can replace this with actual API call

                // Update stats in UI
                // binding.tvTotalScans.text = statsResponse.totalScans.toString()
                // binding.tvDetectedCases.text = statsResponse.detectedCases.toString()
                // binding.tvAccuracy.text = "${statsResponse.accuracy}%"

            } catch (e: Exception) {
                // Handle error silently
            }
        }
        */
    }

    private fun setupCardClickListeners() {
        // Existing card click listeners for information cards
        binding.cardPengertian.setOnClickListener {
            findNavController().navigate(R.id.infoPengertianFragment)
        }

        binding.cardMature.setOnClickListener {
            findNavController().navigate(R.id.infoMatureFragment)
        }

        binding.cardImmature.setOnClickListener {
            findNavController().navigate(R.id.infoImmatureFragment)
        }

        // Optional: Add click listeners for new scrolling text cards
        binding.cardScrollingText.setOnClickListener {
            // Navigate to information or tips page
            // findNavController().navigate(R.id.informationFragment)
        }

        binding.cardScrollingBanner.setOnClickListener {
            // Navigate to about app or features page
            // findNavController().navigate(R.id.aboutFragment)
        }

        // Jika kamu menambahkan cardImportantInfo yang baru, kamu bisa menambahkan listener di sini
        binding.cardImportantInfo.setOnClickListener {
            // Misalnya, navigasi ke halaman detail informasi penting
            // findNavController().navigate(R.id.importantInfoDetailFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}