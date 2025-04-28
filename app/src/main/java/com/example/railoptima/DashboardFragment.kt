package com.example.railoptima

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DashboardFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize MapView
        mapView = view.findViewById(R.id.map_view)
        var mapViewBundle: Bundle? = savedInstanceState?.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
        }
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync { googleMap ->
            MapsInitializer.initialize(requireContext())

            // Set to Satellite View
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

            // Initialize FusedLocationProviderClient
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

            // Check and request permissions
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get current location
                getCurrentLocation(googleMap)
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
            }

            // FAB Bottom Sheet
           val fab = view.findViewById<FloatingActionButton>(R.id.fab_actions)


            fab.setOnClickListener {
                try {
                    val dialog = BottomSheetDialog(requireContext())
                    val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_actions, null)
                    dialog.setContentView(sheetView)

                    // Forecast button
                    sheetView.findViewById<View>(R.id.btn_forecast)?.setOnClickListener {
                        try {
                            startActivity(Intent(requireContext(), ForecastActivity::class.java))
                            dialog.dismiss()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening ForecastActivity", e)
                        }
                    }

                    // Schedule button
                    sheetView.findViewById<View>(R.id.btn_schedule)?.setOnClickListener {
                        try {
                            startActivity(Intent(requireContext(), ScheduleActivity::class.java))
                            dialog.dismiss()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening ScheduleActivity", e)
                        }
                    }

                    // Delay button
                    sheetView.findViewById<View>(R.id.btn_delay)?.setOnClickListener {
                        try {
                            startActivity(Intent(requireContext(), DelayActivity::class.java))
                            dialog.dismiss()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening DelayActivity", e)
                        }
                    }

                    dialog.show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing BottomSheetDialog", e)
                }
            }




            // ViewPager and Tabs
            val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
            val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
            viewPager.adapter = DashboardPagerAdapter(this)
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Rakes"
                    1 -> "Delays"
                    2 -> "Resources"
                    else -> ""
                }
            }.attach()

            // Swipe to Refresh
            val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
            swipeRefresh.setOnRefreshListener {
                swipeRefresh.isRefreshing = false // Mock refresh
            }
        }

        return view
    }

    private fun getCurrentLocation(googleMap: GoogleMap) {
        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                googleMap.addMarker(MarkerOptions().position(userLocation).title("You are here"))

                // Start continuous location updates if needed
                startLocationUpdates(googleMap)
            }
        }
    }

    private fun startLocationUpdates(googleMap: GoogleMap) {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Update every 10 seconds
            fastestInterval = 5000 // Update every 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Define the LocationCallback here
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult?.let {
                    for (location in it.locations) {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        googleMap.addMarker(MarkerOptions().position(userLocation).title("You are here"))
                    }
                }
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // Save and restore the map view state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapViewBundle = Bundle()
        mapView.onSaveInstanceState(mapViewBundle)
        outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        // Stop location updates to save resources
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

class DashboardPagerAdapter(fragment: Fragment) : androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RakeFragment()
            1 -> DelayFragment()
            2 -> ResourceFragment()
            else -> RakeFragment()
        }
    }
}

class RakeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(android.R.layout.list_content, container, false)
        val listView = view.findViewById<ListView>(android.R.id.list)
        val rakes = listOf("Rake #123 - Mumbai, ETA: 14:00", "Rake #456 - Delhi, ETA: 15:30")
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, rakes)
        return view
    }
}

class DelayFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(android.R.layout.list_content, container, false)
        val listView = view.findViewById<ListView>(android.R.id.list)
        val delays = listOf("Rake #123 - 30 mins", "Rake #456 - 45 mins")
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, delays)
        return view
    }
}

class ResourceFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(android.R.layout.list_content, container, false)
        val listView = view.findViewById<ListView>(android.R.id.list)
        val resources = listOf("Platform 1 - Occupied", "Crew A - Available")
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, resources)
        return view
    }
}
