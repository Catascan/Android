package com.example.cataractscan.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ActivityEditProfileBinding
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import android.util.Log

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var preferenceManager: PreferenceManager
    private var selectedImageUri: Uri? = null

    private val TAG = "EditProfileActivity"

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfile.setImageURI(uri)
                Log.d(TAG, "Image selected: $uri")
            } ?: Log.w(TAG, "Image selection data is null.")
        } else {
            Log.d(TAG, "Image selection cancelled or failed. Result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableFullScreenMode()

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        loadUserData()
        setupClickListeners()
    }

    private fun enableFullScreenMode() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    private fun loadUserData() {
        // Try to get data from PreferenceManager first
        var name = preferenceManager.getName() ?: ""
        var email = preferenceManager.getEmail() ?: ""
        val profileImage = preferenceManager.getProfileImage()

        // If data is empty, try to get from local storage
        val sharedPrefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        if (name.isEmpty()) {
            name = sharedPrefs.getString("name", "") ?: ""
        }
        if (email.isEmpty()) {
            email = sharedPrefs.getString("email", "") ?: ""
        }
        val address = sharedPrefs.getString("address", "") ?: ""

        Log.d(TAG, "Loading user data - Name: $name, Email: $email, Address: $address, Image: $profileImage")

        binding.apply {
            etName.setText(name)
            etEmail.setText(email)
            etAddress.setText(address)

            Glide.with(this@EditProfileActivity)
                .load(profileImage)
                .apply(RequestOptions
                    .circleCropTransform()
                    .placeholder(R.drawable.splash_icon)
                    .error(R.drawable.splash_icon))
                .into(ivProfile)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            saveUserData()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun saveUserData() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (validateInput(name, email)) {
            setLoading(true)
            updateProfileViaAPI(name, email, address)
        }
    }

    private fun updateProfileViaAPI(name: String, email: String, address: String) {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            showToast("Session expired, please login again")
            setLoading(false)
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // Check if image is selected
                if (selectedImageUri == null) {
                    // No image selected, just save text data locally and show success
                    saveTextDataLocally(name, email, address)
                    setLoading(false)
                    showToast("Profile updated successfully")
                    finish()
                    return@launch
                }

                // Process image upload
                val uri = selectedImageUri!!
                Log.d(TAG, "Processing selected image URI: $uri")
                val imageFile = createFileFromUri(uri)
                Log.d(TAG, "Created image file: ${imageFile.absolutePath}, size: ${imageFile.length()} bytes")

                if (imageFile.length() == 0L) {
                    Log.e(TAG, "Image file is empty after creation!")
                    showToast("Gagal mengunggah: File gambar kosong.")
                    setLoading(false)
                    return@launch
                }

                val mediaType = contentResolver.getType(uri)?.toMediaTypeOrNull()
                    ?: "image/*".toMediaTypeOrNull()

                if (mediaType == null) {
                    Log.e(TAG, "Could not determine MediaType for image URI: $uri")
                    showToast("Gagal mengunggah: Tipe gambar tidak didukung.")
                    setLoading(false)
                    return@launch
                }

                val requestFile = imageFile.asRequestBody(mediaType)
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                Log.d(TAG, "Multipart image part created. Filename: ${imageFile.name}, MediaType: $mediaType")

                // Call API to update profile image only
                val response = ApiClient.apiService.updateProfile("Bearer $token", imagePart)

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    Log.d(TAG, "Profile update successful. Message: ${responseBody.message}")

                    // Update local preferences with new image data from response
                    responseBody.user?.let { updatedUser ->
                        // Convert UserProfileUpdate to User model that PreferenceManager expects
                        // You might need to adjust this based on your PreferenceManager implementation

                        // If your PreferenceManager has a method to save image URL directly:
                        updatedUser.imageLink?.let { imageUrl ->
                            preferenceManager.saveProfileImage(imageUrl)
                        }

                        // Or if you need to convert to User model:
                        /*
                        val userModel = User(
                            id = updatedUser.id,
                            username = updatedUser.username,
                            email = updatedUser.email,
                            imageLink = updatedUser.imageLink
                        )
                        preferenceManager.saveUserProfile(userModel)
                        */
                    }

                    // Save text data locally (name, email, address)
                    saveTextDataLocally(name, email, address)

                    showToast("Profile updated successfully")
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Failed to update profile. Code: ${response.code()}, Error: $errorBody")
                    showToast("Failed to update profile: ${response.code()}")
                }
            } catch (e: Exception) {
                setLoading(false)
                Log.e(TAG, "Exception during profile update: ${e.message}", e)
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun saveTextDataLocally(name: String, email: String, address: String) {
        // Save text data to local preferences since API doesn't support updating text fields
        val sharedPrefs = getSharedPreferences("UserProfile", MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("name", name)
            putString("email", email)
            putString("address", address)
            apply()
        }

        // Also update PreferenceManager if it has methods for name/email
        // Uncomment these if your PreferenceManager has these methods:
        // preferenceManager.saveName(name)
        // preferenceManager.saveEmail(email)

        Log.d(TAG, "Text data saved locally - Name: $name, Email: $email, Address: $address")
    }

    private fun createFileFromUri(uri: Uri): File {
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return file
    }

    private fun validateInput(name: String, email: String): Boolean {
        if (name.isEmpty()) {
            showToast("Please enter your name")
            return false
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email")
            return false
        }
        return true
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading
        binding.btnSave.text = if (isLoading) "Saving..." else "Save"
        binding.btnChangePhoto.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}