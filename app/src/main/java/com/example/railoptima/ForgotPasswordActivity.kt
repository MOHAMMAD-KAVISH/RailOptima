package com.example.railoptima

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Bind views
        val emailEt = findViewById<TextInputEditText>(R.id.et_email)
        val submitBtn = findViewById<Button>(R.id.btn_submit)
        val backBtn = findViewById<Button>(R.id.btn_back)

        // Submit button
        submitBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val emailLayout = findViewById<TextInputLayout>(R.id.til_email)
            emailLayout.error = null

            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Enter a valid email"
                return@setOnClickListener
            }

            // Simulate sending reset email
            Log.d("ForgotPassword", "Reset link sent to $email")
            Snackbar.make(it, "Password reset link sent to $email", Snackbar.LENGTH_LONG).show()
            emailEt.text?.clear()
        }

        // Back to login
        backBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}