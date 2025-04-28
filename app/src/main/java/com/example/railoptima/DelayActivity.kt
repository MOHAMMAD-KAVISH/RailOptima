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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.railoptima.adapter.DelayAdapter
import com.example.railoptima.model.Delay
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class DelayActivity : AppCompatActivity() {
    private lateinit var adapter: DelayAdapter
    private val delays = mutableListOf<Delay>()
    private val TAG = "DelayActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_delay)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set content view", e)
            Snackbar.make(findViewById(android.R.id.content), "Error loading delays", Snackbar.LENGTH_LONG).show()
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
        val recyclerView = findViewById<RecyclerView>(R.id.rv_delays) ?: run {
            Log.e(TAG, "RecyclerView not found")
            return
        }
        adapter = DelayAdapter(delays) { delay ->
            val fragmentContainer = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.fragment_container)
            if (fragmentContainer == null) {
                Log.e(TAG, "Fragment container not found")
                Snackbar.make(recyclerView, "Error displaying forecast", Snackbar.LENGTH_SHORT).show()
                return@DelayAdapter
            }
            val args = Bundle().apply {
                putString("rake_id", delay.rakeId)
                putString("destination", delay.station ?: "Mumbai")
                putString("eta", "")
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
        val delayMinutesEt = findViewById<TextInputEditText>(R.id.et_delay_minutes)
        val stationSpinner = findViewById<Spinner>(R.id.sp_station)
        val reportBtn = findViewById<Button>(R.id.btn_report_delay)
        if (rakeIdEt == null || delayMinutesEt == null || stationSpinner == null || reportBtn == null) {
            Log.e(TAG, "Form views not found")
            return
        }

        // Station spinner
        val stations = WeatherUtils.stations.keys.toList()
        stationSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stations)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        reportBtn.setOnClickListener {
            val rakeId = rakeIdEt.text.toString().trim()
            val minutesStr = delayMinutesEt.text.toString().trim()
            val station = stationSpinner.selectedItem?.toString() ?: ""

            if (rakeId.isEmpty() || minutesStr.isEmpty() || station.isEmpty()) {
                Snackbar.make(it, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minutes = minutesStr.toIntOrNull()
            if (minutes == null || minutes <= 0) {
                Snackbar.make(it, "Enter a valid delay in minutes", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Fetch weather for station
            val latLng = WeatherUtils.stations[station]
            if (latLng != null && sharedPref.getBoolean("notifications_enabled", true)) {
                WeatherUtils.fetchWeather(this, latLng, station) { weatherData, error ->
                    runOnUiThread {
                        val weatherMessage = when {
                            error != null -> "Weather unavailable: $error"
                            weatherData != null -> "${weatherData.description.replaceFirstChar { it.uppercase() }} at $station, ${weatherData.temp}°C\n${weatherData.delayRisk}"
                            else -> "Weather unavailable"
                        }

                        try {
                            val delay = Delay(rakeId, minutes, station)
                            adapter.addDelay(delay)
                            rakeIdEt.text?.clear()
                            delayMinutesEt.text?.clear()
                            Snackbar.make(it, "Delay reported\n$weatherMessage", Snackbar.LENGTH_LONG).show()

                            // Send notification
                            if (sharedPref.getBoolean("notifications_enabled", true)) {
                                sendDelayNotification(rakeId, minutes, station, weatherData?.delayRisk)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to report delay", e)
                            Snackbar.make(it, "Error reporting delay", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // No weather
                runOnUiThread {
                    try {
                        val delay = Delay(rakeId, minutes, station)
                        adapter.addDelay(delay)
                        rakeIdEt.text?.clear()
                        delayMinutesEt.text?.clear()
                        Snackbar.make(it, "Delay reported", Snackbar.LENGTH_SHORT).show()

                        // Send notification
                        if (sharedPref.getBoolean("notifications_enabled", true)) {
                            sendDelayNotification(rakeId, minutes, station, null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to report delay (no weather)", e)
                        Snackbar.make(it, "Error reporting delay", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun sendDelayNotification(rakeId: String, minutes: Int, station: String, delayRisk: String?) {
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
                append("Rake #$rakeId delayed by $minutes minutes at $station")
                if (delayRisk != null) append("\n$delayRisk")
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Delay Reported")
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
                // Assuming last delay for retry
                if (delays.isNotEmpty()) {
                    val lastDelay = delays.last()
                    sendDelayNotification(lastDelay.rakeId, lastDelay.minutes, lastDelay.station ?: "Mumbai", null)
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