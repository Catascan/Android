package com.example.cataractscan.splashscreen
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.cataractscan.R
import com.example.cataractscan.login.LoginActivity
import com.example.cataractscan.ui.activities.MainActivity
import com.example.cataractscan.utils.PreferenceManager

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set fullscreen
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_spalsh_screen)
        supportActionBar?.hide()

        // Initialize PreferenceManager
        preferenceManager = PreferenceManager(this)

        // Initialize views
        val splashImage = findViewById<ImageView>(R.id.iv_logo)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Start animations
        splashImage.startAnimation(fadeIn)

        // Check token and navigate after delay
        Handler().postDelayed({
            checkTokenAndNavigate()
        }, 3000) // Reduced delay untuk better UX
    }

    private fun checkTokenAndNavigate() {
        if (preferenceManager.isLoggedIn()) {
            // Token exists, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // No token, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}

