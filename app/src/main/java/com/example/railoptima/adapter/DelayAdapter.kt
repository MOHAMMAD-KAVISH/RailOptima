package com.example.railoptima.adapter

import android.content.ContentValues.TAG
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.railoptima.R
import com.example.railoptima.WeatherUtils
import com.example.railoptima.model.Delay

class DelayAdapter(
    private val delays: MutableList<Delay>,
    private val onMapClick: (Delay) -> Unit
) : RecyclerView.Adapter<DelayAdapter.DelayViewHolder>() {

    private val TAG = "DelayAdapter"

    class DelayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDelay: TextView? = itemView.findViewById(R.id.tv_delay)
        val ivWeather: ImageView? = itemView.findViewById(R.id.iv_weather)
        val btnMap: ImageView? = itemView.findViewById(R.id.btn_map)

        fun bind(delay: Delay, onMapClick: (Delay) -> Unit) {
            try {
                if (tvDelay == null) {
                    Log.e(TAG, "tvDelay is null for delay: ${delay.rakeId}")
                    return
                }
                tvDelay.text = buildString {
                    append("Rake #${delay.rakeId}, ${delay.minutes} min delay")
                    if (delay.station != null) append(" at ${delay.station}")
                }
                btnMap?.setOnClickListener { onMapClick(delay) }
            } catch (e: Exception) {
                Log.e(TAG, "Error binding delay: ${delay.rakeId}", e)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelayViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_delay, parent, false)
            DelayViewHolder(view).apply {
                Log.d(TAG, "Created ViewHolder, tvDelay: ${tvDelay?.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inflating item_delay layout", e)
            DelayViewHolder(View(parent.context)) // Fallback empty view
        }
    }

    override fun onBindViewHolder(holder: DelayViewHolder, position: Int) {
        if (position >= delays.size) {
            Log.w(TAG, "Invalid position: $position, size: ${delays.size}")
            return
        }
        val delay = delays[position]
        holder.bind(delay, onMapClick)

        // Fetch weather for icon
        val station = delay.station
        if (station != null && WeatherUtils.stations.containsKey(station) && holder.ivWeather != null) {
            WeatherUtils.fetchWeather(holder.itemView.context, WeatherUtils.stations[station]!!, station) { weatherData, error ->
                holder.itemView.post {
                    try {
                        if (error != null) {
                            holder.ivWeather.setImageResource(R.drawable.img_4)
                            Log.w(TAG, "Weather fetch error for $station: $error")
                            return@post
                        }
                        val iconRes = when {
                            weatherData?.description?.lowercase()?.contains("rain") == true -> R.drawable.ic_rain
                            weatherData?.description?.lowercase()?.contains("clear") == true -> R.drawable.img_5
                            else -> R.drawable.img_4
                        }
                        holder.ivWeather.setImageResource(iconRes)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating weather icon for $station", e)
                        holder.ivWeather.setImageResource(R.drawable.img_4)
                    }
                }
            }
        } else {
            holder.ivWeather?.setImageResource(R.drawable.img_4)
        }
    }

    override fun getItemCount(): Int = delays.size

    fun addDelay(delay: Delay) {
        delays.add(delay)
        notifyItemInserted(delays.size - 1)
        Log.d(TAG, "Added delay: Rake #${delay.rakeId}, ${delay.minutes} min at ${delay.station}")
    }
}