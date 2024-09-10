package com.weatherxm.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ListItemHourlyForecastBinding
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent

class HourlyForecastAdapter(
    private val onClickListener: ((HourlyWeather) -> Unit)?
) : ListAdapter<HourlyWeather, HourlyForecastAdapter.HourlyForecastViewHolder>(
    HourlyWeatherDiffCallback()
), KoinComponent {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyForecastViewHolder {
        val binding = ListItemHourlyForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HourlyForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyForecastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HourlyForecastViewHolder(private val binding: ListItemHourlyForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyWeather) {
            onClickListener?.let { listener ->
                binding.root.setOnClickListener {
                    listener.invoke(item)
                }
            }

            binding.timestamp.text = item.timestamp.getFormattedTime(itemView.context, false)
            binding.icon.setWeatherAnimation(item.icon)
            binding.temperaturePrimary.text = Weather.getFormattedTemperature(item.temperature, 1)
            binding.precipProbability.text =
                Weather.getFormattedPrecipitationProbability(item.precipProbability)
        }
    }

    class HourlyWeatherDiffCallback : DiffUtil.ItemCallback<HourlyWeather>() {

        override fun areItemsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: HourlyWeather, newItem: HourlyWeather): Boolean {
            return oldItem.timestamp == newItem.timestamp &&
                oldItem.icon == newItem.icon &&
                oldItem.temperature == newItem.temperature &&
                oldItem.precipitation == newItem.precipitation &&
                oldItem.precipAccumulated == newItem.precipAccumulated &&
                oldItem.precipProbability == newItem.precipProbability &&
                oldItem.windSpeed == newItem.windSpeed &&
                oldItem.windDirection == newItem.windDirection &&
                oldItem.windGust == newItem.windGust &&
                oldItem.humidity == newItem.humidity &&
                oldItem.feelsLike == newItem.feelsLike &&
                oldItem.pressure == newItem.pressure &&
                oldItem.uvIndex == newItem.uvIndex &&
                oldItem.solarIrradiance == newItem.solarIrradiance
        }
    }
}
