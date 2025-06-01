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
import com.example.cataractscan.api.ApiClient
import com.example.cataractscan.api.ChangePasswordRequest // Pastikan ini mengarah ke data class yang benar
import com.example.cataractscan.api.ChangePasswordResponse // Import ChangePasswordResponse
import com.example.cataractscan.utils.PreferenceManager // Import PreferenceManager
import kotlinx.coroutines.launch
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import android.util.Log
import com.example.cataractscan.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var preferenceManager: PreferenceManager
    private var isNewPasswordVisible = false
    private var isConfirmNewPasswordVisible = false

    private val TAG = "ChangePasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        setupFullScreen()

        preferenceManager = PreferenceManager(this)

        // Validasi apakah user sudah login
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sesi telah berakhir, silakan login kembali.", Toast.LENGTH_LONG).show()
            redirectToLogin()
            return
        }

        setupClickListeners()
        setupPasswordToggles()
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
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up fullscreen: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // PERHATIAN: Pastikan ID tombol di XML adalah btnSavePassword
        binding.btnSetNewPassword.setOnClickListener { // Ini harus btnSavePassword dari layout
            handleChangePassword()
        }
    }

    private fun setupPasswordToggles() {
        // Hanya untuk new password dan confirm new password
        binding.etNewPassword.setOnTouchListener { _, event ->
            handlePasswordToggle(binding.etNewPassword, event, isNewPasswordVisible) {
                isNewPasswordVisible = it
            }
        }

        binding.etConfirmNewPassword.setOnTouchListener { _, event ->
            handlePasswordToggle(binding.etConfirmNewPassword, event, isConfirmNewPasswordVisible) {
                isConfirmNewPasswordVisible = it
            }
        }
    }

    private fun handlePasswordToggle(
        editText: EditText,
        event: MotionEvent,
        isVisible: Boolean,
        updateState: (Boolean) -> Unit
    ): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val drawableEnd = editText.compoundDrawables[2]
            if (drawableEnd != null) {
                val drawableWidth = drawableEnd.bounds.width()
                if (event.rawX >= (editText.right - drawableWidth - editText.paddingEnd)) {
                    togglePasswordVisibility(editText, isVisible, updateState)
                    return true
                }
            }
        }
        return false
    }

    private fun togglePasswordVisibility(
        editText: EditText,
        isVisible: Boolean,
        updateState: (Boolean) -> Unit
    ) {
        val newVisibility = !isVisible
        val cursorPosition = editText.selectionEnd

        editText.inputType = if (newVisibility) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val lockIcon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_lock_24)
        val visibilityIcon = ContextCompat.getDrawable(
            this,
            if (newVisibility) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        )

        lockIcon?.setBounds(0, 0, 70, 70)
        visibilityIcon?.setBounds(0, 0, 70, 70)

        editText.setCompoundDrawables(lockIcon, null, visibilityIcon, null)
        editText.setSelection(cursorPosition.coerceAtMost(editText.text.length))

        updateState(newVisibility)
    }

    private fun handleChangePassword() {
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmNewPassword = binding.etConfirmNewPassword.text.toString().trim()

        // Validasi input
        when {
            newPassword.isEmpty() -> {
                showToast("Harap masukkan password baru Anda")
                binding.etNewPassword.requestFocus()
                return
            }
            confirmNewPassword.isEmpty() -> {
                showToast("Harap konfirmasi password baru Anda")
                binding.etConfirmNewPassword.requestFocus()
                return
            }
            newPassword != confirmNewPassword -> {
                showToast("Password baru dan konfirmasi password tidak cocok")
                binding.etConfirmNewPassword.requestFocus()
                return
            }
            newPassword.length < 6 -> {
                showToast("Password baru minimal 6 karakter")
                binding.etNewPassword.requestFocus()
                return
            }
        }

        // Token otorisasi diambil dari PreferenceManager
        val token = preferenceManager.getToken()
        if (token.isNullOrEmpty()) {
            showToast("Sesi telah berakhir, silakan login kembali")
            redirectToLogin()
            return
        }

        performChangePassword(newPassword, confirmNewPassword, token)
    }

    private fun performChangePassword(newPassword: String, retypePassword: String, token: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                // KOREKSI: Membuat ChangePasswordRequest yang benar tanpa oldPassword
                val request = ChangePasswordRequest(
                    newPassword = newPassword,
                    retypePassword = retypePassword
                )

                Log.d(TAG, "Sending change password request with newPassword and retypePassword")

                val response = ApiClient.apiService.changePassword("Bearer $token", request)

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val changePasswordResponse = response.body()!!
                    Log.d(TAG, "Change password successful: ${changePasswordResponse.message}")
                    showToast(changePasswordResponse.message ?: "Password berhasil diubah")
                    finish() // Kembali ke ProfileFragment setelah berhasil
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Change password failed. Code: ${response.code()}, Error: $errorBody")

                    val errorMessage = when (response.code()) {
                        400 -> "Data tidak valid atau format permintaan salah"
                        401 -> "Sesi telah berakhir, silakan login kembali"
                        403 -> "Akses ditolak. Periksa kredensial Anda."
                        500 -> "Server error, coba lagi nanti."
                        else -> "Gagal mengubah password. Kode: ${response.code()}"
                    }
                    showToast(errorMessage)

                    if (response.code() == 401) {
                        redirectToLogin()
                    }
                }
            } catch (e: Exception) {
                setLoading(false)
                Log.e(TAG, "Exception during change password: ${e.message}", e)
                val errorMessage = when {
                    e.message?.contains("timeout", true) == true -> "Koneksi timeout, coba lagi"
                    e.message?.contains("network", true) == true -> "Tidak ada koneksi internet"
                    e.message?.contains("host", true) == true -> "Server tidak dapat diakses"
                    else -> "Terjadi kesalahan: ${e.message}"
                }
                Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSetNewPassword.isEnabled = !isLoading
        binding.btnSetNewPassword.text = if (isLoading) "Menyimpan..." else "Simpan Password"

        try {
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.w(TAG, "progressIndicator not found in layout: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun redirectToLogin() {
        preferenceManager.logout()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}