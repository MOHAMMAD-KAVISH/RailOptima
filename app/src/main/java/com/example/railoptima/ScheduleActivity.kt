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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.railoptima.adapter.ScheduleAdapter
import com.example.railoptima.model.Schedule
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ScheduleActivity : AppCompatActivity() {
    private lateinit var adapter: ScheduleAdapter
    private val schedules = mutableListOf<Schedule>()
    private val TAG = "ScheduleActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_schedule)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set content view", e)
            Snackbar.make(findViewById(android.R.id.content), "Error loading schedule", Snackbar.LENGTH_LONG).show()
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

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.rv_schedules) ?: run {
            Log.e(TAG, "RecyclerView not found")
            return
        }
        adapter = ScheduleAdapter(schedules) { schedule ->
            val fragmentContainer = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragment_container)
            if (fragmentContainer == null) {
                Log.e(TAG, "Fragment container not found")
                Snackbar.make(recyclerView, "Error displaying forecast", Snackbar.LENGTH_SHORT).show()
                return@ScheduleAdapter
            }
            val args = Bundle().apply {
                putString("rake_id", schedule.rakeId)
                putString("destination", schedule.platform)
                putString("eta", schedule.time)
            }
            try {
                val fragment = ForecastResultFragment().apply { arguments = args }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ForecastResultFragment", e)
                Snackbar.make(recyclerView, "Error displaying forecast", Snackbar.LENGTH_SHORT).show()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Form setup
        val rakeIdEt = findViewById<TextInputEditText>(R.id.et_rake_id)
        val platformSpinner = findViewById<Spinner>(R.id.sp_platform)
        val timeEt = findViewById<TextInputEditText>(R.id.et_time)
        val addBtn = findViewById<Button>(R.id.btn_add_schedule)
        if (rakeIdEt == null || platformSpinner == null || timeEt == null || addBtn == null) {
            Log.e(TAG, "Form views not found")
            return
        }

        // Map platforms to stations
        val platforms = WeatherUtils.stations.keys.toList()
        platformSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, platforms)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        addBtn.setOnClickListener {
            val rakeId = rakeIdEt.text.toString().trim()
            val platform = platformSpinner.selectedItem?.toString() ?: ""
            val time = timeEt.text.toString().trim()

            if (rakeId.isEmpty() || time.isEmpty() || platform.isEmpty()) {
                Snackbar.make(it, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Fetch weather for platform
            val latLng = WeatherUtils.stations[platform]
            if (latLng != null && sharedPref.getBoolean("notifications_enabled", true)) {
                WeatherUtils.fetchWeather(this, latLng, platform) { weatherData, error ->
                    runOnUiThread {
                        val weatherMessage = when {
                            error != null -> "Weather unavailable: $error"
                            weatherData != null -> "${weatherData.description.replaceFirstChar { it.uppercase() }} at $platform, ${weatherData.temp}°C\n${weatherData.delayRisk}"
                            else -> "Weather unavailable"
                        }

                        // Confirmation dialog
                        try {
                            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_schedule, null)
                            dialogView.findViewById<TextView>(R.id.tv_dialog_content)?.setText(
                                "Rake #$rakeId\nPlatform: $platform\nTime: $time\nWeather: $weatherMessage"
                            )
                            val dialog = AlertDialog.Builder(this)
                                .setView(dialogView)
                                .create()
                            dialogView.findViewById<Button>(R.id.btn_dialog_cancel)?.setOnClickListener { dialog.dismiss() }
                            dialogView.findViewById<Button>(R.id.btn_dialog_confirm)?.setOnClickListener {
                                val schedule = Schedule(rakeId, platform, time)
                                adapter.addSchedule(schedule)
                                rakeIdEt.text?.clear()
                                timeEt.text?.clear()
                                dialog.dismiss()
                                Snackbar.make(it, "Schedule added", Snackbar.LENGTH_SHORT).show()

                                // Send notification
                                if (sharedPref.getBoolean("notifications_enabled", true)) {
                                    sendScheduleNotification(rakeId, platform, time, weatherData?.delayRisk)
                                }
                            }
                            dialog.show()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to show confirm dialog", e)
                            Snackbar.make(it, "Error adding schedule", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // No weather
                runOnUiThread {
                    try {
                        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_schedule, null)
                        dialogView.findViewById<TextView>(R.id.tv_dialog_content)?.setText(
                            "Rake #$rakeId\nPlatform: $platform\nTime: $time"
                        )
                        val dialog = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .create()
                        dialogView.findViewById<Button>(R.id.btn_dialog_cancel)?.setOnClickListener { dialog.dismiss() }
                        dialogView.findViewById<Button>(R.id.btn_dialog_confirm)?.setOnClickListener {
                            val schedule = Schedule(rakeId, platform, time)
                            adapter.addSchedule(schedule)
                            rakeIdEt.text?.clear()
                            timeEt.text?.clear()
                            dialog.dismiss()
                            Snackbar.make(it, "Schedule added", Snackbar.LENGTH_SHORT).show()

                            // Send notification
                            if (sharedPref.getBoolean("notifications_enabled", true)) {
                                sendScheduleNotification(rakeId, platform, time, null)
                            }
                        }
                        dialog.show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to show confirm dialog (no weather)", e)
                        Snackbar.make(it, "Error adding schedule", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun sendScheduleNotification(rakeId: String, platform: String, time: String, delayRisk: String?) {
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

            val contentText = buildString {
                append("Rake #$rakeId on $platform at $time")
                if (delayRisk != null) append("\n$delayRisk")
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Schedule Confirmed")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
            Snackbar.make(findViewById(android.R.id.content), "Error sending notification", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Retry notification after permission granted
            val sharedPref = getSharedPreferences("RailOptima", MODE_PRIVATE)
            if (sharedPref.getBoolean("notifications_enabled", true)) {
                // Assuming last schedule for retry
                if (schedules.isNotEmpty()) {
                    val lastSchedule = schedules.last()
                    sendScheduleNotification(lastSchedule.rakeId, lastSchedule.platform, lastSchedule.time, null)
                }
            }
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Notifications blocked. Please enable in settings.",
                Snackbar.LENGTH_LONG
            ).setAction("Settings") {
                startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                })
            }.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}