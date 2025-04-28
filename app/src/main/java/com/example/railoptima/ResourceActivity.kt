package com.example.railoptima

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.railoptima.adapter.ResourceAdapter
import com.example.railoptima.model.Resource
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ResourceActivity : AppCompatActivity() {
    private lateinit var adapter: ResourceAdapter
    private val resources = mutableListOf<Resource>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resource)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.rv_resources)
        adapter = ResourceAdapter(resources)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Form setup
        val typeSpinner = findViewById<Spinner>(R.id.sp_resource_type)
        val resourceIdEt = findViewById<TextInputEditText>(R.id.et_resource_id)
        val statusSpinner = findViewById<Spinner>(R.id.sp_status)
        val assignBtn = findViewById<Button>(R.id.btn_assign_resource)

        val types = arrayOf("Crew", "Platform")
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val statuses = arrayOf("Available", "Occupied", "Maintenance")
        statusSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        assignBtn.setOnClickListener {
            val type = typeSpinner.selectedItem.toString()
            val id = resourceIdEt.text.toString().trim()
            val status = statusSpinner.selectedItem.toString()

            if (id.isEmpty()) {
                Snackbar.make(it, "Please enter a resource ID", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resource = Resource(type, id, status)
            adapter.addResource(resource)
            resourceIdEt.text?.clear()
            Snackbar.make(it, "Resource assigned", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}