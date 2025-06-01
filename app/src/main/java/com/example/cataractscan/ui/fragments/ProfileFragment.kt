package com.example.cataractscan.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cataractscan.R
import com.example.cataractscan.databinding.FragmentProfileBinding
import com.example.cataractscan.login.LoginActivity
import com.example.cataractscan.profile.EditProfileActivity
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.ProfileEditUser
import com.example.cataractscan.login.ChangePasswordActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())

        setupUI() // Display cached data first
        setupListeners()
        loadUserProfile() // Load fresh data from API
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this fragment
        loadUserProfile()
    }

    private fun setupUI() {
        // Display cached user information from preferences
        val username = preferenceManager.getUsername() ?: "User"
        val email = preferenceManager.getEmail() ?: "email@example.com"
        val photoUrl = preferenceManager.getProfileImage()

        Log.d(TAG, "Setting up UI - username: $username, email: $email, photoUrl: $photoUrl")

        // Set CataScan label - memastikan label muncul
        binding.tvCatascanLabel.text = "CataScan"
        binding.tvCatascanLabel.visibility = View.VISIBLE

        // Set username and email
        binding.tvUsernameDetail.text = username
        binding.tvEmailDetail.text = email // Make sure you have this TextView in your layout

        // Load profile image with Glide
        loadProfileImage(photoUrl)
    }

    private fun loadProfileImage(imageUrl: String?) {
        Log.d(TAG, "Loading profile image: $imageUrl")
        Glide.with(this)
            .load(imageUrl)
            .apply(RequestOptions
                .circleCropTransform()
                .placeholder(R.drawable.splash_icon)
                .error(R.drawable.splash_icon))
            .into(binding.ivProfileImage)
    }

    private fun loadUserProfile() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token is null or empty")
            return
        }

        Log.d(TAG, "Loading user profile from API...")

        lifecycleScope.launch {
            try {
                // Use getProfileEdit endpoint to get complete user data including email
                val response = ApiClient.apiService.getProfileEdit("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    val userProfile = profileResponse.user

                    Log.d(TAG, "Profile loaded successfully from API: username=${userProfile.username}, email=${userProfile.email}, imageLink=${userProfile.imageLink}")

                    // Update UI with fresh data from API
                    updateUIWithProfileData(userProfile)

                    // Save profile to preferences
                    saveProfileToPreferences(userProfile)

                } else {
                    Log.e(TAG, "Failed to load profile: ${response.code()}")
                    Toast.makeText(requireContext(), "Gagal memuat profil: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}")
                Toast.makeText(requireContext(), "Error memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIWithProfileData(userProfile: ProfileEditUser) {
        Log.d(TAG, "Updating UI with profile data: username=${userProfile.username}, email=${userProfile.email}")

        // Update CataScan label - refresh saat data profile di-update
        binding.tvCatascanLabel.text = "CataScan"
        binding.tvCatascanLabel.visibility = View.VISIBLE

        // Update username
        binding.tvUsernameDetail.text = userProfile.username

        // Update email - display email from API response
        binding.tvEmailDetail.text = userProfile.email

        // Load profile image
        loadProfileImage(userProfile.imageLink)
    }

    private fun saveProfileToPreferences(userProfile: ProfileEditUser) {
        Log.d(TAG, "Attempting to save profile to preferences...")

        try {
            // Save profile data to preferences
            preferenceManager.saveUserProfile(userProfile)
            Log.d(TAG, "Profile saved successfully using saveUserProfile method")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile using saveUserProfile method: ${e.message}")

            // Fallback: Save manually if there's an issue with the overload method
            Log.d(TAG, "Saving profile manually using individual methods as fallback...")
            try {
                preferenceManager.saveUsername(userProfile.username)
                preferenceManager.saveEmail(userProfile.email)
                if (!userProfile.imageLink.isNullOrEmpty()) {
                    preferenceManager.saveProfileImage(userProfile.imageLink)
                } else {
                    Log.d(TAG, "No image link to save manually")
                }
                Log.d(TAG, "Profile saved manually successfully")
            } catch (manualException: Exception) {
                Log.e(TAG, "Error saving profile manually: ${manualException.message}")
            }
        }

        // Verify that data has been saved
        verifyProfileSaved()
    }

    private fun verifyProfileSaved() {
        val savedUsername = preferenceManager.getUsername()
        val savedEmail = preferenceManager.getEmail()
        val savedImageUrl = preferenceManager.getProfileImage()
        Log.d(TAG, "Verification - saved username: $savedUsername, saved email: $savedEmail, saved image: $savedImageUrl")
    }

    private fun setupListeners() {
        // Edit profile button
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Change password button
        binding.btnChangePassword.setOnClickListener {
            val token = preferenceManager.getToken()

            if (token.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_LONG).show()
                redirectToLogin()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }

        // Optional: Tambahkan click listener untuk CataScan label jika diperlukan
        binding.tvCatascanLabel.setOnClickListener {
            Toast.makeText(requireContext(), "CataScan - Aplikasi Deteksi Katarak", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLogout() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            clearSessionAndNavigateToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.logout("Bearer $token")
                clearSessionAndNavigateToLogin()
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                clearSessionAndNavigateToLogin()
            }
        }
    }

    private fun clearSessionAndNavigateToLogin() {
        preferenceManager.logout()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}