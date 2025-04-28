package com.example.railoptima

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.railoptima.utils.SecurityUtils
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var emailEt: TextInputEditText
    private lateinit var passwordEt: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout // For toggle
    private lateinit var loginBtn: android.widget.Button
    private lateinit var registerBtn: android.widget.Button
    private lateinit var forgotPasswordTv: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize SharedPreferences
        sharedPref = getSharedPreferences("RailOptima", MODE_PRIVATE)

        // Check if already logged in
        if (sharedPref.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        // Bind views
        emailEt = findViewById(R.id.et_email)
        passwordEt = findViewById(R.id.et_password)
        passwordLayout = findViewById(R.id.til_password)
        loginBtn = findViewById(R.id.btn_login)
        registerBtn = findViewById(R.id.btn_register)
        forgotPasswordTv = findViewById(R.id.tv_forgot_password)

        // Password visibility toggle using TextInputLayout
        var isPasswordVisible = false
        passwordLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEt.transformationMethod = null
                passwordLayout.endIconDrawable = getDrawable(R.drawable.ic_off)
            } else {
                passwordEt.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordLayout.endIconDrawable = getDrawable(R.drawable.ic_eye)
            }
            passwordEt.setSelection(passwordEt.text?.length ?: 0)
        }

        // Login button
        loginBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            if (!validateInputs(email, password)) return@setOnClickListener

            // Check credentials
            val storedEmail = sharedPref.getString("email", null)
            val storedPassword = sharedPref.getString("password", null)
            val hashedPassword = SecurityUtils.hashPassword(password)

            if (email == storedEmail && hashedPassword == storedPassword) {
                // Save login state
                sharedPref.edit().putBoolean("isLoggedIn", true).apply()
                Snackbar.make(it, "Login successful!", Snackbar.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Snackbar.make(it, "Invalid email or password", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Register button
        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot password
        forgotPasswordTv.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        val emailLayout = findViewById<TextInputLayout>(R.id.til_email)
        passwordLayout.error = null
        emailLayout.error = null

        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Enter a valid email"
            return false
        }
        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }
}