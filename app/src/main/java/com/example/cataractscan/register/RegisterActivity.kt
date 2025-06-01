package com.example.cataractscan.register

import android.text.InputType
import android.view.MotionEvent
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cataractscan.R
import com.example.cataractscan.databinding.ActivityRegisterBinding
import com.example.cataractscan.api.ApiClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var animatorSet: AnimatorSet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request full screen before setting content view
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        setupAnimation()
        setupClickListeners()
        setupPasswordToggle()
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
            handleRegistration()
        }

        binding.loginTextView.setOnClickListener {
            finish()
        }
    }

    private fun setupPasswordToggle() {
        // Setup password toggle for main password field
        binding.passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2 // Index for drawableEnd
                if (event.rawX >= (binding.passwordEditText.right - binding.passwordEditText.compoundDrawables[drawableEnd].bounds.width())) {
                    togglePasswordVisibility(binding.passwordEditText)
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }

        // Setup password toggle for confirm password field
        binding.confirmPasswordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2 // Index for drawableEnd
                if (event.rawX >= (binding.confirmPasswordEditText.right - binding.confirmPasswordEditText.compoundDrawables[drawableEnd].bounds.width())) {
                    togglePasswordVisibility(binding.confirmPasswordEditText)
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun togglePasswordVisibility(editText: android.widget.EditText) {
        val isVisible = editText.inputType != (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        val inputType = if (isVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        editText.inputType = inputType

        val drawable = if (isVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_baseline_lock_24,
            0,
            drawable,
            0
        )

        editText.setSelection(editText.text.length)
    }

    private fun handleRegistration() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        when {
            name.isEmpty() -> {
                showToast("Harap masukkan nama lengkap")
                binding.nameEditText.requestFocus()
                return
            }
            name.length < 2 -> {
                showToast("Nama minimal 2 karakter")
                binding.nameEditText.requestFocus()
                return
            }
            email.isEmpty() -> {
                showToast("Harap masukkan email")
                binding.emailEditText.requestFocus()
                return
            }
            !isValidEmail(email) -> {
                showToast("Format email tidak valid")
                binding.emailEditText.requestFocus()
                return
            }
            password.isEmpty() -> {
                showToast("Harap masukkan password")
                binding.passwordEditText.requestFocus()
                return
            }
            password.length < 3 -> {
                showToast("Password minimal 3 karakter")
                binding.passwordEditText.requestFocus()
                return
            }
            confirmPassword.isEmpty() -> {
                showToast("Harap konfirmasi password")
                binding.confirmPasswordEditText.requestFocus()
                return
            }
            password != confirmPassword -> {
                showToast("Password tidak cocok")
                binding.confirmPasswordEditText.requestFocus()
                return
            }
        }

        // Show loading
        setLoading(true)

        // Call API - sesuai dengan dokumentasi API
        lifecycleScope.launch {
            try {
                val registerRequest = com.example.cataractscan.api.RegisterRequest(
                    username = name,
                    email = email,
                    password = password,
                    retype_password = confirmPassword
                )
                val response = ApiClient.apiService.register(registerRequest)
                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val registerResponse = response.body()!!
                    showToast("Register berhasil! Silakan login")
                    finish()
                } else {
                    // Handle different error codes
                    when (response.code()) {
                        400 -> showToast("Data tidak valid, periksa kembali")
                        409 -> showToast("Email sudah terdaftar")
                        422 -> showToast("Email sudah digunakan")
                        500 -> showToast("Server error, coba lagi nanti")
                        else -> showToast("Registrasi gagal, coba lagi")
                    }
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

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.loginButton.text = if (isLoading) "Loading..." else "Register"
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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