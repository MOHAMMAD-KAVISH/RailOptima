package com.example.railoptima

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tagline = findViewById<TextView>(R.id.tv_tagline)
        val quote = findViewById<TextView>(R.id.tv_quote)
        val lottie = findViewById<LottieAnimationView>(R.id.lottie_animation)
        val progressBar = findViewById<ProgressBar>(R.id.pb_loading)

        // Apply animations
        tagline.alpha = 0f
        tagline.animate().alpha(1f).setDuration(1000).setStartDelay(300).start()

        quote.alpha = 0f
        quote.animate().alpha(1f).setDuration(1000).setStartDelay(600).start()

        lottie.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(1500)
            .withEndAction {
                lottie.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1500)
                    .start()
            }
            .start()

        // Mock network check (for realism, can be expanded later)
        val isNetworkAvailable = true // Replace with actual check if needed
        if (!isNetworkAvailable) {
            Snackbar.make(lottie, "No network connection", Snackbar.LENGTH_LONG).show()
            progressBar.visibility = android.view.View.GONE
            return
        }

        // Transition to LoginActivity after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, 3000)
    }
}