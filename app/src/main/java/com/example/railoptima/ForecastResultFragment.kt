package com.example.railoptima

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ForecastResultFragment : Fragment(), OnMapReadyCallback {
    private var mapView: MapView? = null
    private var googleMap: com.google.android.gms.maps.GoogleMap? = null
    private var weatherText: TextView? = null
    private var locationSpinner: Spinner? = null
    private val TAG = "ForecastResultFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View
        try {
            view = inflater.inflate(R.layout.fragment_forecast_result, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inflate fragment layout", e)
            Toast.makeText(context, "Error loading forecast", Toast.LENGTH_LONG).show()
            return null
        }

        // Initialize views
        mapView = view.findViewById(R.id.map_view)
        weatherText = view.findViewById(R.id.tv_weather)
        locationSpinner = view.findViewById(R.id.sp_location)
        if (mapView == null || weatherText == null || locationSpinner == null) {
            Log.e(TAG, "Required views not found")
            Toast.makeText(context, "Error initializing views", Toast.LENGTH_LONG).show()
            return view
        }

        // Map setup
        try {
            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MapView", e)
            Toast.makeText(context, "Error loading map", Toast.LENGTH_SHORT).show()
        }

        // Result display
        val rakeId = arguments?.getString("rake_id") ?: ""
        val dest = arguments?.getString("destination") ?: "Mumbai"
        val eta = arguments?.getString("eta") ?: ""
        view.findViewById<TextView>(R.id.tv_result)?.setText(
            "Rake #$rakeId to $dest${if (eta.isNotEmpty()) ", ETA: $eta" else ""}"
        ) ?: Log.e(TAG, "tv_result not found")

        // Setup location spinner
        val stationNames = WeatherUtils.stations.keys.toList()
        locationSpinner?.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            stationNames
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val destIndex = stationNames.indexOf(dest).takeIf { it >= 0 } ?: 0
        locationSpinner?.setSelection(destIndex)

        // Spinner listener
        locationSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedStation = stationNames[position]
                val latLng = WeatherUtils.stations[selectedStation] ?: WeatherUtils.stations["Mumbai"]!!
                updateMapLocation(latLng, selectedStation, rakeId)
                fetchWeather(latLng, selectedStation, rakeId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Details dialog
        view.findViewById<Button>(R.id.btn_details)?.setOnClickListener {
            try {
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_forecast_details, null)
                dialogView.findViewById<TextView>(R.id.tv_dialog_content)?.setText(
                    "Rake #$rakeId\nType: Passenger\nDestination: $dest${if (eta.isNotEmpty()) "\nETA: $eta" else ""}"
                )
                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                dialogView.findViewById<Button>(R.id.btn_dialog_close)?.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show details dialog", e)
                Toast.makeText(context, "Error showing details", Toast.LENGTH_SHORT).show()
            }
        }

        // Check location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }

        return view
    }

    override fun onMapReady(map: com.google.android.gms.maps.GoogleMap) {
        googleMap = map
        val dest = arguments?.getString("destination") ?: "Mumbai"
        val rakeId = arguments?.getString("rake_id") ?: ""
        val latLng = WeatherUtils.stations[dest] ?: WeatherUtils.stations["Mumbai"]!!
        updateMapLocation(latLng, dest, rakeId)
        fetchWeather(latLng, dest, rakeId)
    }

    private fun updateMapLocation(latLng: LatLng, station: String, rakeId: String) {
        googleMap?.clear()
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Rake #$rakeId")
                .snippet("Station: $station")
        )
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    private fun fetchWeather(latLng: LatLng, station: String, rakeId: String) {
        val sharedPref = context?.getSharedPreferences("RailOptima", Context.MODE_PRIVATE) ?: return
        if (!sharedPref.getBoolean("notifications_enabled", true)) {
            weatherText?.setText(getString(R.string.weather_disabled))
            return
        }

        weatherText?.setText(getString(R.string.loading_weather))
        WeatherUtils.fetchWeather(requireContext(), latLng, station) { weatherData, error ->
            activity?.runOnUiThread {
                when {
                    error != null -> {
                        weatherText?.setText(error)
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                    weatherData != null -> {
                        weatherText?.setText(
                            getString(
                                R.string.weather_info,
                                station,
                                weatherData.temp,
                                weatherData.description,
                                weatherData.delayRisk
                            )
                        )
                        Toast.makeText(context, "Weather updated for $station", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        weatherText?.setText(getString(R.string.weather_error))
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            googleMap?.let { onMapReady(it) }
        } else {
            Toast.makeText(context, R.string.location_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}