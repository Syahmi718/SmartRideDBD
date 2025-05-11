package com.example.smartride_dbd

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

class SplashActivity : AppCompatActivity() {

    private lateinit var lottieAnimationView: LottieAnimationView
    private val splashTimeOut: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make the activity fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_splash)

        // Initialize the Lottie animation
        lottieAnimationView = findViewById(R.id.splashAnimation)
        
        // Configure animation settings
        lottieAnimationView.apply {
            setAnimation(R.raw.splash_animation)
            repeatCount = LottieDrawable.INFINITE
            speed = 1.0f // Normal speed
            playAnimation()
        }

        // Use a handler to delay the start of the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            // Start the main activity
            startMainActivity()
        }, splashTimeOut)
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        
        // Close this activity
        finish()
        
        // Add a fade transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    override fun onBackPressed() {
        // Disable back button during splash screen
        // Do nothing
    }
} 