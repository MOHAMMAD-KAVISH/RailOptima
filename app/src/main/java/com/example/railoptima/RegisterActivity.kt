package com.example.railoptima

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.railoptima.utils.SecurityUtils
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Bind views
        val nameEt = findViewById<TextInputEditText>(R.id.et_name)
        val emailEt = findViewById<TextInputEditText>(R.id.et_email)
        val passwordEt = findViewById<TextInputEditText>(R.id.et_password)
        val confirmPasswordEt = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val registerBtn = findViewById<Button>(R.id.btn_register)
        val loginTv = findViewById<Button>(R.id.btn_login)

        // Register button
        registerBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val confirmPassword = confirmPasswordEt.text.toString().trim()

            if (!validateInputs(name, email, password, confirmPassword)) return@setOnClickListener

            // Save user
            val sharedPref = getSharedPreferences("RailOptima", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("name", name)
                putString("email", email)
                putString("password", SecurityUtils.hashPassword(password))
                apply()
            }

            Snackbar.make(it, "Registration successful! Please log in.", Snackbar.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Back to login
        loginTv.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        val nameLayout = findViewById<TextInputLayout>(R.id.til_name)
        val emailLayout = findViewById<TextInputLayout>(R.id.til_email)
        val passwordLayout = findViewById<TextInputLayout>(R.id.til_password)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.til_confirm_password)

        nameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null

        if (name.isEmpty()) {
            nameLayout.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
        if (password != confirmPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
            return false
        }
        return true
    }
}