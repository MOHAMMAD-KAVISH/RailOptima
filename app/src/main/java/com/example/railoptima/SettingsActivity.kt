package com.example.railoptima

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private var isDialogShowing = false
    private val TAG = "SettingsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_settings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set content view", e)
            Toast.makeText(this, "Error loading settings", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar) ?: run {
            Log.e(TAG, "Toolbar not found")
            return
        }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // SharedPreferences
        val sharedPref = getSharedPreferences("RailOptima", MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Check notifications enabled
        val notificationsEnabled = sharedPref.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) {
            Toast.makeText(this, "Notifications are disabled in app settings", Toast.LENGTH_SHORT).show()
        }

        // Battery optimization check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "Please disable battery optimization for notifications", Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open battery optimization settings", e)
                }
            }
        }

        // Language Spinner
        val languageSpinner = findViewById<Spinner>(R.id.sp_language)
        val languages = arrayOf("English", "Hindi")
        languageSpinner?.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val savedLanguage = sharedPref.getString("language", "English")
        languageSpinner?.setSelection(languages.indexOf(savedLanguage))
        var isFirstLanguageSelection = true
        languageSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (isFirstLanguageSelection) {
                    isFirstLanguageSelection = false
                    return
                }
                val selectedLanguage = languages[position]
                editor.putString("language", selectedLanguage).apply()
                val locale = if (selectedLanguage == "Hindi") Locale("hi") else Locale.ENGLISH
                val config = resources.configuration
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
                Toast.makeText(this@SettingsActivity, "Language set to $selectedLanguage", Toast.LENGTH_SHORT).show()
                // Delay recreate to avoid rapid UI changes
                android.os.Handler(mainLooper).postDelayed({
                    if (!isFinishing) recreate()
                }, 500)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Theme Switch
        val themeSwitch = findViewById<Switch>(R.id.sw_theme)
        themeSwitch?.isChecked = sharedPref.getBoolean("dark_mode", false)
        themeSwitch?.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            Toast.makeText(this, "Theme set to ${if (isChecked) "Dark" else "Light"}", Toast.LENGTH_SHORT).show()
            // Delay recreate to avoid rapid UI changes
            android.os.Handler(mainLooper).postDelayed({
                if (!isFinishing) recreate()
            }, 500)
        }

        // Font Size Spinner
        val fontSizeSpinner = findViewById<Spinner>(R.id.sp_font_size)
        val fontSizes = arrayOf("Small", "Medium", "Large")
        fontSizeSpinner?.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            fontSizes
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val savedFontSize = sharedPref.getString("font_size", "Medium")
        fontSizeSpinner?.setSelection(fontSizes.indexOf(savedFontSize))
        var isFirstFontSelection = true
        fontSizeSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (isFirstFontSelection) {
                    isFirstFontSelection = false
                    return
                }
                val selectedFontSize = fontSizes[position]
                editor.putString("font_size", selectedFontSize).apply()
                Toast.makeText(this@SettingsActivity, "Font size set to $selectedFontSize", Toast.LENGTH_SHORT).show()
                // TODO: Implement font size scaling (e.g., update textAppearance)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Notifications Switch
        val notificationSwitch = findViewById<Switch>(R.id.sw_notifications)
        notificationSwitch?.isChecked = sharedPref.getBoolean("notifications_enabled", true) &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        notificationSwitch?.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                } else {
                    Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
                    sendTestNotification()
                }
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Auto-Refresh Weather Switch
        val autoRefreshSwitch = findViewById<Switch>(R.id.sw_auto_refresh)
        autoRefreshSwitch?.isChecked = sharedPref.getBoolean("auto_refresh", false)
        autoRefreshSwitch?.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("auto_refresh", isChecked).apply()
            Toast.makeText(
                this,
                "Auto-refresh weather ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: Hook into WeatherUtils for auto-refresh
        }

        // Test Notification Button
        findViewById<Button>(R.id.btn_test_notification)?.setOnClickListener {
            sendTestNotification()
        }

        // Reset Data Button
        val resetBtn = findViewById<Button>(R.id.btn_reset_data)
        resetBtn?.setOnClickListener {
            if (isDialogShowing) return@setOnClickListener
            isDialogShowing = true
            try {
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_reset, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setOnDismissListener { isDialogShowing = false }
                    .create()
                dialogView.findViewById<Button>(R.id.btn_dialog_cancel)?.setOnClickListener {
                    dialog.dismiss()
                }
                dialogView.findViewById<Button>(R.id.btn_dialog_reset)?.setOnClickListener {
                    editor.clear().apply()
                    Toast.makeText(this, "Data reset successful", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    android.os.Handler(mainLooper).postDelayed({
                        if (!isFinishing) {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finishAffinity()
                        }
                    }, 1000)
                }
                dialog.show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show reset dialog", e)
                isDialogShowing = false
                Toast.makeText(this, "Error showing reset dialog", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendTestNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "railoptima_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "RailOptima Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for RailOptima notifications"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 500, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("RailOptima Test Alert")
                .setContentText("This is a test notification from RailOptima!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            } else {
                Toast.makeText(this, "Notifications blocked. Please enable in settings.", Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send test notification", e)
            Toast.makeText(this, "Error sending notification", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val notificationSwitch = findViewById<Switch>(R.id.sw_notifications)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
                notificationSwitch?.isChecked = true
                sendTestNotification()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                notificationSwitch?.isChecked = false
                val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}