package com.example.railoptima.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.railoptima.R
import com.example.railoptima.WeatherUtils
import com.example.railoptima.model.Schedule

class ScheduleAdapter(
    private val schedules: MutableList<Schedule>,
    private val onMapClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSchedule: TextView = itemView.findViewById(R.id.tv_schedule)
        val ivWeather: ImageView = itemView.findViewById(R.id.iv_weather)
        val btnMap: ImageView = itemView.findViewById(R.id.btn_map)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.tvSchedule.text = "Rake #${schedule.rakeId}, ${schedule.platform}, ${schedule.time}"

        val platform = schedule.platform
        if (WeatherUtils.stations.containsKey(platform)) {
            WeatherUtils.fetchWeather(holder.itemView.context, WeatherUtils.stations[platform]!!, platform) { weatherData, _ ->
                val iconRes = when {
                    weatherData?.description?.lowercase()?.contains("rain") == true -> R.drawable.ic_rain
                    weatherData?.description?.lowercase()?.contains("clear") == true -> R.drawable.img_5
                    else -> R.drawable.img_4
                }
                holder.ivWeather.setImageResource(iconRes)
            }
        } else {
            holder.ivWeather.setImageResource(R.drawable.img_4)
        }

        holder.btnMap.setOnClickListener { onMapClick(schedule) }
    }

    override fun getItemCount(): Int = schedules.size

    fun addSchedule(schedule: Schedule) {
        schedules.add(schedule)
        notifyItemInserted(schedules.size - 1)
    }
}