package com.example.cataractscan.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ActivityForgotPasswordBinding
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.ForgotPasswordRequest
import kotlinx.coroutines.launch
import android.util.Log
import androidx.activity.addCallback

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val apiService = ApiClient.apiService

    companion object {
        private const val TAG = "ForgotPasswordActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi binding terlebih dahulu
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        // Setup fullscreen setelah binding
        setupFullScreen()

        setupClickListeners()
    }

    private fun setupFullScreen() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up fullscreen", e)
        }
    }

    private fun setupClickListeners() {
        // Reset password button
        binding.resetPasswordButton.setOnClickListener {
            sendResetPasswordEmail()
        }

        // Back button - pastikan ID ini ada di layout Anda
        try {
            // Jika ada btnBack di layout
            findViewById<View>(R.id.btnBack)?.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.w(TAG, "btnBack not found in layout", e)
        }

        // Alternative: Handle sistem back button
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun sendResetPasswordEmail() {
        val email = binding.emailEditText.text.toString().trim()

        // Validasi input
        if (!validateEmail(email)) {
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Sending reset password email to: $email")

                // Gunakan ForgotPasswordRequest sesuai dengan ApiService
                val request = ForgotPasswordRequest(email)
                val response = apiService.forgotPassword(request)

                setLoading(false)

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "Response: ${result?.message}")

                    // Check success field dari response
                    if (result?.success == true) {
                        showSuccessMessage(email, result.message)
                    } else {
                        // Jika tidak ada success field, anggap berhasil jika ada message
                        if (!result?.message.isNullOrBlank()) {
                            showSuccessMessage(email, result!!.message)
                        } else {
                            showError("Gagal mengirim email reset")
                        }
                    }
                } else {
                    handleErrorResponse(response.code())
                }

            } catch (e: Exception) {
                setLoading(false)
                handleNetworkError(e)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.emailEditText.error = "Email tidak boleh kosong"
                binding.emailEditText.requestFocus()
                false
            }
            !isValidEmail(email) -> {
                binding.emailEditText.error = "Format email tidak valid"
                binding.emailEditText.requestFocus()
                false
            }
            else -> {
                binding.emailEditText.error = null
                true
            }
        }
    }

    private fun showSuccessMessage(email: String, message: String) {
        val displayMessage = message.takeIf { it.isNotBlank() }
            ?: "Email reset password telah dikirim ke $email"

        // Show success dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Email Terkirim")
            .setMessage("$displayMessage\n\nSilakan periksa email Anda dan ikuti instruksi untuk reset password.")
            .setPositiveButton("OK") { _, _ ->
                navigateToLogin()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to LoginActivity", e)
            // Fallback: just finish this activity
            finish()
        }
    }

    private fun handleErrorResponse(code: Int) {
        val errorMessage = when (code) {
            400 -> "Permintaan tidak valid. Periksa email Anda."
            404 -> "Email tidak terdaftar dalam sistem."
            429 -> "Terlalu banyak permintaan. Coba lagi nanti."
            500 -> "Server error, coba lagi nanti."
            else -> "Gagal mengirim link reset password. Kode: $code"
        }
        showError(errorMessage)
    }

    private fun handleNetworkError(e: Exception) {
        Log.e(TAG, "Network error", e)
        val errorMessage = when {
            e.message?.contains("timeout", true) == true -> "Koneksi timeout, coba lagi"
            e.message?.contains("network", true) == true -> "Tidak ada koneksi internet"
            e.message?.contains("host", true) == true -> "Server tidak dapat diakses"
            e.message?.contains("ConnectException", true) == true -> "Tidak dapat terhubung ke server"
            e.message?.contains("UnknownHostException", true) == true -> "Server tidak ditemukan"
            e.message?.contains("Failed to connect", true) == true -> "Gagal terhubung ke server"
            else -> "Terjadi kesalahan jaringan: ${e.message}"
        }
        showError(errorMessage)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.apply {
            resetPasswordButton.isEnabled = !isLoading
            resetPasswordButton.text = if (isLoading) "Mengirim..." else "Confirm Email"
            emailEditText.isEnabled = !isLoading

            // Show/hide progress bar if exists
            try {
                // Jika ada progressBar di layout
                findViewById<View>(R.id.progressBar)?.visibility =
                    if (isLoading) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                // progressBar tidak ada di layout
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}