package com.example.cataractscan.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ActivityForgotPasswordBinding // Pastikan import ini benar dan kelas ini tergenerasi
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.ForgotRequest
import kotlinx.coroutines.launch
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText // Penting: Tambahkan import ini untuk casting EditText

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

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
            e.printStackTrace()
        }
    }

    private fun setupClickListeners() {
        binding.resetPasswordButton.setOnClickListener {
            sendResetPasswordEmail()
        }

        // Add back button if exists
        // Perbaikan: btnBack seharusnya memiliki ID yang berbeda dari resetPasswordButton
        // Asumsi ada btnBack di layout Anda
        try {
            // Jika ada tombol kembali dengan ID 'btnBack' di layout:
            // binding.btnBack.setOnClickListener {
            //     onBackPressedDispatcher.onBackPressed()
            // }
        } catch (e: Exception) {
            // btnBack might not exist in layout, handle gracefully
        }
    }

    private fun sendResetPasswordEmail() {
        val email = binding.emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Harap masukkan email Anda", Toast.LENGTH_SHORT).show()
            binding.emailEditText.requestFocus()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
            binding.emailEditText.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val forgotRequest = ForgotRequest(email)
                val response = ApiClient.apiService.forgotPassword(forgotRequest)

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val forgotPasswordResponse = response.body()!!
                    val messageToDisplay = forgotPasswordResponse.message.takeIf { it.isNotBlank() }
                        ?: "Email reset password telah dikirim ke $email"

                    Toast.makeText(this@ForgotPasswordActivity, messageToDisplay, Toast.LENGTH_LONG).show()
                    // MULAI PERUBAHAN DI SINI
                    // Arahkan secara eksplisit ke LoginActivity dan bersihkan back stack
                    val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish() // Selesaikan ForgotPasswordActivity
                    // AKHIR PERUBAHAN DI SINI
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Permintaan tidak valid. Periksa email Anda."
                        404 -> "Email tidak terdaftar dalam sistem."
                        429 -> "Terlalu banyak permintaan. Coba lagi nanti."
                        500 -> "Server error, coba lagi nanti."
                        else -> "Gagal mengirim link reset password. Kode: ${response.code()}"
                    }
                    Toast.makeText(this@ForgotPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                val errorMessage = when {
                    e.message?.contains("timeout", true) == true -> "Koneksi timeout, coba lagi"
                    e.message?.contains("network", true) == true -> "Tidak ada koneksi internet"
                    e.message?.contains("host", true) == true -> "Server tidak dapat diakses"
                    else -> "Terjadi kesalahan: ${e.message}"
                }
                Toast.makeText(this@ForgotPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.resetPasswordButton.isEnabled = !isLoading
        binding.resetPasswordButton.text = if (isLoading) "Mengirim..." else "Confirm Email"
        binding.emailEditText.isEnabled = !isLoading
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
