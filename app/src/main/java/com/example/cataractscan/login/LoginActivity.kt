package com.example.cataractscan.login

import androidx.core.content.ContextCompat
import android.text.InputType
import android.view.MotionEvent
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ActivityLoginBinding
import com.example.cataractscan.register.RegisterActivity
import com.example.cataractscan.ui.activities.MainActivity
import com.example.cataractscan.utils.PreferenceManager
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.LoginResponse
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var animatorSet: AnimatorSet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup fullscreen
        setupFullScreen()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Initialize PreferenceManager
        preferenceManager = PreferenceManager(this)

        // Check if already logged in
        checkLoginStatus()

        setupAnimation()
        setupClickListeners()
        setupPasswordToggle()
    }

    private fun setupFullScreen() {
        // Make the activity full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set window flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Hide system bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Make content appear behind system bars
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    private fun checkLoginStatus() {
        if (preferenceManager.isLoggedIn()) {
            navigateToMain()
        }
    }

    private fun setupAnimation() {
        val imageView = binding.imgIllustration

        val upAnimation = ObjectAnimator.ofFloat(imageView, "translationY", 0f, -50f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
        }

        val rotateAnimation = ObjectAnimator.ofFloat(imageView, "rotation", -5f, 5f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
        }

        animatorSet = AnimatorSet().apply {
            playTogether(upAnimation, rotateAnimation)
            start()
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            handleLogin()
        }

        binding.labelSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Tambahkan click listener untuk tombol Lupa Password
        // MULAI PERUBAHAN DI SINI
        binding.forgotPasswordTextView.setOnClickListener {
            // Hapus baris Toast di bawah ini
            // Toast.makeText(this, "Fitur Lupa Password akan segera hadir!", Toast.LENGTH_SHORT).show()
            // Aktifkan baris di bawah ini untuk mengarahkan ke ForgotPasswordActivity
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        // AKHIR PERUBAHAN DI SINI
    }

    private fun setupPasswordToggle() {
        binding.passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2 // Index for drawableEnd
                if (event.rawX >= (binding.passwordEditText.right - binding.passwordEditText.compoundDrawables[drawableEnd].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun togglePasswordVisibility() {
        // Create drawable with custom size
        val lockIcon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_lock_24)
        val visibilityIcon = ContextCompat.getDrawable(
            this,
            if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility
        )

        // Set bounds for icons (width, height)
        lockIcon?.setBounds(0, 0, 70, 70)  // Lock icon size
        visibilityIcon?.setBounds(0, 0, 70, 50)  // Smaller eye icon size

        if (isPasswordVisible) {
            // Hide password
            binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            // Show password
            binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }

        // Set the drawables with custom sizes
        binding.passwordEditText.setCompoundDrawables(
            lockIcon,
            null,
            visibilityIcon,
            null
        )

        isPasswordVisible = !isPasswordVisible
        binding.passwordEditText.setSelection(binding.passwordEditText.text.length)
    }

    private fun handleLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        when {
            email.isEmpty() -> {
                showToast("Harap masukkan email")
                return
            }
            password.isEmpty() -> {
                showToast("Harap masukkan password")
                return
            }
        }

        // Show loading
        setLoading(true)

        // Call API
        lifecycleScope.launch {
            try {
                val loginRequest = com.example.cataractscan.api.LoginRequest(email, password)
                val response = ApiClient.apiService.login(loginRequest)
                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Call the new handleLoginSuccess method
                    handleLoginSuccess(loginResponse, email)
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Data login tidak valid"
                        401 -> "Email atau password salah"
                        404 -> "User tidak ditemukan"
                        500 -> "Server error, coba lagi nanti"
                        else -> "Login gagal"
                    }
                    showToast(errorMessage)
                }
            } catch (e: Exception) {
                setLoading(false)
                val errorMessage = when {
                    e.message?.contains("timeout", true) == true -> "Koneksi timeout, coba lagi"
                    e.message?.contains("network", true) == true -> "Tidak ada koneksi internet"
                    e.message?.contains("host", true) == true -> "Server tidak dapat diakses"
                    else -> "Terjadi kesalahan: ${e.message}"
                }
                showToast(errorMessage)
            }
        }
    }

    // NEW METHOD: Handle login success with profile loading
    private fun handleLoginSuccess(loginResponse: LoginResponse, email: String) {
        // Simpan token
        preferenceManager.saveToken(loginResponse.token)

        // Simpan email yang digunakan untuk login
        preferenceManager.saveEmail(email)

        // Setelah login berhasil, ambil data profil lengkap dari API
        loadUserProfileAfterLogin()

        showToast("Login berhasil")

        // Navigate to main activity
        navigateToMain()
    }

    // NEW METHOD: Load user profile after successful login
    private fun loadUserProfileAfterLogin() {
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                // Panggil endpoint getUserProfile untuk mendapatkan data profil
                val response = ApiClient.apiService.getUserProfile("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val profileResponse = response.body()!!
                    val userProfileData = profileResponse.user

                    // Simpan data profil ke preferences
                    preferenceManager.saveUserProfileData(userProfileData)

                    // Optional: Atau bisa juga panggil endpoint getProfileEdit untuk data lebih lengkap
                    // val profileEditResponse = ApiClient.apiService.getProfileEdit("Bearer $token")
                    // if (profileEditResponse.isSuccessful && profileEditResponse.body() != null) {
                    //     val profileEditData = profileEditResponse.body()!!.user
                    //     preferenceManager.saveUserProfile(profileEditData)
                    // }
                }
            } catch (e: Exception) {
                // Handle error silently, user can still proceed to main screen
                // Profile data will be loaded again when they access home fragment
                // Log error for debugging purposes if needed
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.loginButton.text = if (isLoading) "Loading..." else "Login"
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::animatorSet.isInitialized) {
            animatorSet.cancel()
        }
    }
}