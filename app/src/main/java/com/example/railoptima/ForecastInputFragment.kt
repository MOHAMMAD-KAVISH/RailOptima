package com.example.railoptima

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ForecastInputFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_forecast_input, container, false)

        val rakeIdEt = view.findViewById<TextInputEditText>(R.id.et_rake_id)
        val rakeTypeSpinner = view.findViewById<Spinner>(R.id.sp_rake_type)
        val destinationEt = view.findViewById<TextInputEditText>(R.id.et_destination)
        val forecastBtn = view.findViewById<Button>(R.id.btn_forecast)
        val progressBar = view.findViewById<ProgressBar>(R.id.pb_forecast)

        // Spinner setup
        val rakeTypes = arrayOf("Passenger", "Freight", "Goods")
        rakeTypeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            rakeTypes
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        forecastBtn.setOnClickListener {
            val id = rakeIdEt.text.toString().trim()
            val type = rakeTypeSpinner.selectedItem.toString()
            val dest = destinationEt.text.toString().trim()

            if (id.isEmpty() || dest.isEmpty()) {
                Snackbar.make(view, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            forecastBtn.isEnabled = false

            // Save inputs
            val sharedPref = requireActivity().getSharedPreferences("RailOptima", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("lastRakeId", id) // Consistent naming
                putString("lastRakeType", type)
                putString("lastDestination", dest)
                apply()
            }

            // Mock forecast
            view.postDelayed({
                progressBar.visibility = View.GONE
                forecastBtn.isEnabled = true

                val resultFragment = ForecastResultFragment().apply {
                    arguments = Bundle().apply {
                        putString("rake_id", id)
                        putString("destination", dest)
                        putString("eta", "14:00")
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, resultFragment)
                    .addToBackStack(null)
                    .commit()
            }, 2000)
        }

        return view
    }
}