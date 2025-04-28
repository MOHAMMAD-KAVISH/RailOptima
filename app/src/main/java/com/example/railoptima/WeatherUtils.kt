package com.example.railoptima

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object WeatherUtils {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val apiKey = "a9c0a0616d2a23ad52f01037b9c48511" // Your valid key
    private val cache = mutableMapOf<String, Pair<Long, WeatherData>>()
    private const val CACHE_DURATION_MS = 30 * 60 * 1000 // 30 minutes

    data class WeatherData(
        val temp: Double,
        val description: String,
        val delayRisk: String
    )

    fun fetchWeather(
        context: Context,
        latLng: LatLng,
        station: String,
        callback: (WeatherData?, String?) -> Unit
    ) {
        // Check cache
        val cacheKey = "${latLng.latitude}_${latLng.longitude}"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.first < CACHE_DURATION_MS) {
            println("Using cached weather for $station")
            callback(cached.second, null)
            return
        }

        val url = "https://api.openweathermap.org/data/2.5/weather?lat=${latLng.latitude}&lon=${latLng.longitude}&appid=$apiKey&units=metric"
        println("Fetching weather: $url")

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Weather fetch failed for $station: ${e.message}")
                callback(null, context.getString(R.string.weather_error_network))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    println("Weather fetch failed for $station: HTTP ${response.code}")
                    callback(null, context.getString(R.string.weather_error_api))
                    return
                }

                val body = response.body?.string() ?: run {
                    println("Weather fetch failed for $station: Empty response")
                    callback(null, context.getString(R.string.weather_error))
                    return
                }

                try {
                    val json = JSONObject(body)
                    val temp = json.getJSONObject("main").getDouble("temp")
                    val description = json.getJSONArray("weather").getJSONObject(0).getString("description")
                    val delayRisk = when {
                        description.contains("rain", true) -> "High risk of delays due to rain"
                        description.contains("fog", true) -> "Possible delays due to low visibility"
                        temp > 35 -> "Heat may affect operations"
                        else -> "No significant delay risk"
                    }
                    val weatherData = WeatherData(temp, description, delayRisk)
                    cache[cacheKey] = System.currentTimeMillis() to weatherData
                    println("Weather for $station: $temp°C, $description, $delayRisk")
                    callback(weatherData, null)
                } catch (e: Exception) {
                    println("Weather parse error for $station: ${e.message}")
                    callback(null, context.getString(R.string.weather_error))
                }
            }
        })
    }

    // Station coordinates (shared across app)
    val stations = mapOf(
        "Mumbai" to LatLng(19.0760, 72.8777),
        "Delhi" to LatLng(28.7041, 77.1025),
        "Pune" to LatLng(18.5204, 73.8567),
        "Ahmedabad" to LatLng(23.0225, 72.5714),
        "Chennai" to LatLng(13.0827, 80.2707)
    )
}