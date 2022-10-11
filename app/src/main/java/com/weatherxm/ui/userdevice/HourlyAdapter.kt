package com.weatherxm.ui.userdevice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ListItemHourlyWeatherBinding
import com.weatherxm.ui.userdevice.HourlyAdapter.HourlyViewHolder
import com.weatherxm.util.DateTimeHelper.getHourMinutesFromISO
import com.weatherxm.util.Weather

class HourlyAdapter : ListAdapter<HourlyWeather, HourlyViewHolder>(HourlyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding = ListItemHourlyWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HourlyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    fun getItemFromPosition(position: Int): HourlyWeather? {
        return if (currentList.isNotEmpty()) {
            currentList[position]
        } else {
            null
        }
    }

    inner class HourlyViewHolder(
        private val binding: ListItemHourlyWeatherBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyWeather) {
            binding.time.text = getHourMinutesFromISO(
                itemView.context, item.timestamp.toString(), false
            )
            binding.temperature.text = Weather.getFormattedTemperature(item.temperature)
            binding.precipitation.text =
                Weather.getFormattedPrecipitationProbability(item.precipProbability)
            binding.icon.apply {
                setAnimation(Weather.getWeatherAnimation(item.icon))
                playAnimation()
            }
        }
    }

    class HourlyDiffCallback : DiffUtil.ItemCallback<HourlyWeather>() {

        override fun areItemsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather): Boolean {
            return oldItem.timestamp == newItem.timestamp &&
                oldItem.icon == newItem.icon &&
                oldItem.temperature == newItem.temperature &&
                oldItem.humidity == newItem.humidity &&
                oldItem.precipProbability == newItem.precipProbability &&
                oldItem.precipitation == newItem.precipitation &&
                oldItem.uvIndex == newItem.uvIndex &&
                oldItem.windDirection == newItem.windDirection &&
                oldItem.windSpeed == newItem.windSpeed &&
                oldItem.windGust == newItem.windGust &&
                oldItem.pressure == newItem.pressure
        }
    }
}
