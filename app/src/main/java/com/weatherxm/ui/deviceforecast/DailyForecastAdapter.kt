package com.weatherxm.ui.deviceforecast

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.databinding.ListItemDailyForecastBinding
import com.weatherxm.ui.DailyForecast
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent

class DailyForecastAdapter :
    ListAdapter<DailyForecast, DailyForecastAdapter.DailyForecastViewHolder>(
        DailyForecastDiffCallback()
    ),
    KoinComponent {

    private var minTemperature: Float? = Float.MIN_VALUE
    private var maxTemperature: Float? = Float.MAX_VALUE

    override fun submitList(list: List<DailyForecast>?) {
        minTemperature = list?.minOfOrNull { it.minTemp ?: Float.MIN_VALUE }
        maxTemperature = list?.maxOfOrNull { it.maxTemp ?: Float.MAX_VALUE }

        // Update data
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastViewHolder {
        val binding = ListItemDailyForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyForecastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyForecastViewHolder(private val binding: ListItemDailyForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyForecast) {
            binding.date.text = item.dateOfDay
            binding.day.text = item.nameOfDay
            binding.icon.apply {
                setAnimation(Weather.getWeatherAnimation(item.icon))
                playAnimation()
            }
            binding.temperature.apply {
                valueFrom = minTemperature ?: 0F
                valueTo = maxTemperature ?: 0F
                values = listOf(item.minTemp ?: 0F, item.maxTemp ?: 0F)
            }
            binding.minTemperature.text = Weather.getFormattedTemperature(item.minTemp)
            binding.maxTemperature.text = Weather.getFormattedTemperature(item.maxTemp)

            with(binding.precipitationProbability) {
                when (item.precipProbability) {
                    null -> visibility = View.INVISIBLE
                    0 -> {
                        visibility = View.VISIBLE
                        isActivated = false
                    }
                    else -> {
                        visibility = View.VISIBLE
                        isActivated = true
                    }
                }
                text = Weather.getFormattedPrecipitationProbability(item.precipProbability)
            }
        }
    }

    class DailyForecastDiffCallback : DiffUtil.ItemCallback<DailyForecast>() {

        override fun areItemsTheSame(oldItem: DailyForecast, newItem: DailyForecast): Boolean {
            return oldItem.dateOfDay == newItem.dateOfDay
        }

        override fun areContentsTheSame(oldItem: DailyForecast, newItem: DailyForecast): Boolean {
            return oldItem.dateOfDay == newItem.dateOfDay &&
                oldItem.nameOfDay == newItem.nameOfDay &&
                oldItem.icon == newItem.icon &&
                oldItem.maxTemp == newItem.maxTemp &&
                oldItem.minTemp == newItem.minTemp
        }
    }
}
