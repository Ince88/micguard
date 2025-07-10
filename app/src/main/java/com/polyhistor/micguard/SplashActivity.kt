package com.polyhistor.micguard

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Set status bar color to match splash background
        window.statusBarColor = ContextCompat.getColor(this, R.color.splash_background)
        
        val appIcon = findViewById<ImageView>(R.id.appIcon)
        val appName = findViewById<TextView>(R.id.appName)
        
        // Start the animation
        startSplashAnimation(appIcon, appName)
    }
    
    private fun startSplashAnimation(appIcon: ImageView, appName: TextView) {
        // Create scale animation for the icon
        val scaleX = ObjectAnimator.ofFloat(appIcon, View.SCALE_X, 0.5f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(appIcon, View.SCALE_Y, 0.5f, 1.0f)
        
        // Create alpha animation for the text
        val textAlpha = ObjectAnimator.ofFloat(appName, View.ALPHA, 0f, 1f)
        
        // Create rotation animation for the icon
        val rotation = ObjectAnimator.ofFloat(appIcon, View.ROTATION, -30f, 0f)
        
        // Combine animations
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, rotation)
        animatorSet.play(textAlpha).after(200) // Text appears after 200ms
        
        // Set duration and interpolator
        animatorSet.duration = 1000
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        
        // Start animation and navigate to main activity when done
        animatorSet.start()
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Wait a bit more then navigate
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    // Add fade transition
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }, 500)
            }
        })
    }
} 