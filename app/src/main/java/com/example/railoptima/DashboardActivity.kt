package com.example.railoptima

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.railoptima.DashboardFragment
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Enable transitions
        window.sharedElementEnterTransition = android.transition.TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Navigation Drawer
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Load Dashboard Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .commit()
        }

        // Navigation Item Clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment())
                        .commit()
                }
                R.id.nav_forecast -> startActivity(Intent(this, ForecastActivity::class.java))
                R.id.nav_schedule -> startActivity(Intent(this, ScheduleActivity::class.java))
                R.id.nav_delay -> startActivity(Intent(this, DelayActivity::class.java))
                R.id.nav_resource -> startActivity(Intent(this, ResourceActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.logout)
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes") { _, _ ->
                            // Clear SharedPreferences
                            val sharedPref = getSharedPreferences("RailOptima", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                remove("isLoggedIn")
                                remove("email")
                                remove("password")
                                remove("name")
                                apply()
                            }
                            // Feedback
                            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            // Navigate to LoginActivity, clear stack
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}