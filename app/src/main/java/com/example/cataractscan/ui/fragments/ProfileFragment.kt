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
import com.example.cataractscan.api.UserProfileData
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
        // Display cached user information from preferences first
        val username = preferenceManager.getUsername() ?: "User"
        val email = preferenceManager.getEmail() ?: "email@example.com"
        val photoUrl = preferenceManager.getProfileImage()

        // Also try to get from local storage as fallback
        val sharedPrefs = requireContext().getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE)
        val localName = sharedPrefs.getString("name", "") ?: ""
        val localEmail = sharedPrefs.getString("email", "") ?: ""

        val displayName = if (localName.isNotEmpty()) localName else username
        val displayEmail = if (localEmail.isNotEmpty()) localEmail else email

        Log.d(TAG, "Setting up UI - username: $displayName, email: $displayEmail, photoUrl: $photoUrl")

        // Set CataScan label - memastikan label muncul
        binding.tvCatascanLabel.text = "CataScan"
        binding.tvCatascanLabel.visibility = View.VISIBLE

        // Set username and email
        binding.tvUsernameDetail.text = displayName
        binding.tvEmailDetail.text = displayEmail

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
                // Try multiple endpoints in order of preference
                var response = try {
                    Log.d(TAG, "Trying endpoint: auth/user")
                    ApiClient.apiService.getUserProfile("Bearer $token")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed with auth/user endpoint: ${e.message}")
                    null
                }

                // If first endpoint fails, try the alternative
                if (response == null || !response.isSuccessful) {
                    try {
                        Log.d(TAG, "Trying alternative endpoint: auth/profile/edit")
                        val altResponse = ApiClient.apiService.getProfileEdit("Bearer $token")
                        if (altResponse.isSuccessful && altResponse.body() != null) {
                            val profileResponse = altResponse.body()!!
                            val userProfile = profileResponse.user
                            updateUIWithProfileEditData(userProfile)
                            saveProfileEditToPreferences(userProfile)
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed with auth/profile/edit endpoint: ${e.message}")
                    }
                }

                // Handle successful response from getUserProfile
                if (response != null && response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    val userProfile = profileResponse.user

                    Log.d(TAG, "Profile loaded successfully from auth/user: username=${userProfile.username}, imageLink=${userProfile.imageLink}")

                    // Update UI with data from getUserProfile
                    updateUIWithUserProfileData(userProfile)
                    saveUserProfileToPreferences(userProfile)

                } else {
                    // If all endpoints fail, show user cached data and inform about network issues
                    val errorCode = response?.code() ?: 0
                    Log.e(TAG, "All profile endpoints failed. Last error code: $errorCode")

                    when (errorCode) {
                        401 -> {
                            Toast.makeText(requireContext(), "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_LONG).show()
                            redirectToLogin()
                        }
                        404 -> {
                            // Don't show error for 404, just use cached data
                            Log.d(TAG, "Profile endpoint not found, using cached data")
                        }
                        else -> {
                            Toast.makeText(requireContext(), "Gagal memuat profil terbaru, menampilkan data tersimpan", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}")
                // Don't show error toast, just use cached data
                Log.d(TAG, "Using cached profile data due to network error")
            }
        }
    }

    private fun updateUIWithUserProfileData(userProfile: UserProfileData) {
        Log.d(TAG, "Updating UI with UserProfileData: username=${userProfile.username}")

        // Update CataScan label
        binding.tvCatascanLabel.text = "CataScan"
        binding.tvCatascanLabel.visibility = View.VISIBLE

        // Update username
        binding.tvUsernameDetail.text = userProfile.username

        // For email, use cached data since getUserProfile doesn't return email
        val cachedEmail = preferenceManager.getEmail() ?: "email@example.com"
        binding.tvEmailDetail.text = cachedEmail

        // Load profile image
        loadProfileImage(userProfile.imageLink)
    }

    private fun updateUIWithProfileEditData(userProfile: ProfileEditUser) {
        Log.d(TAG, "Updating UI with ProfileEditUser: username=${userProfile.username}, email=${userProfile.email}")

        // Update CataScan label
        binding.tvCatascanLabel.text = "CataScan"
        binding.tvCatascanLabel.visibility = View.VISIBLE

        // Update username and email
        binding.tvUsernameDetail.text = userProfile.username
        binding.tvEmailDetail.text = userProfile.email

        // Load profile image
        loadProfileImage(userProfile.imageLink)
    }

    private fun saveUserProfileToPreferences(userProfile: UserProfileData) {
        Log.d(TAG, "Saving UserProfileData to preferences...")
        try {
            preferenceManager.saveUsername(userProfile.username)
            if (!userProfile.imageLink.isNullOrEmpty()) {
                preferenceManager.saveProfileImage(userProfile.imageLink)
            }
            Log.d(TAG, "UserProfileData saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving UserProfileData: ${e.message}")
        }
    }

    private fun saveProfileEditToPreferences(userProfile: ProfileEditUser) {
        Log.d(TAG, "Saving ProfileEditUser to preferences...")
        try {
            preferenceManager.saveUsername(userProfile.username)
            preferenceManager.saveEmail(userProfile.email)
            if (!userProfile.imageLink.isNullOrEmpty()) {
                preferenceManager.saveProfileImage(userProfile.imageLink)
            }
            Log.d(TAG, "ProfileEditUser saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ProfileEditUser: ${e.message}")
        }
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