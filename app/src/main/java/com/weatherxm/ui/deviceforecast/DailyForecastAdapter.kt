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
import com.weatherxm.util.Weather.roundToDecimals

class DailyForecastAdapter :
    ListAdapter<DailyForecast, DailyForecastAdapter.DailyForecastViewHolder>(
        DailyForecastDiffCallback()
    ) {

    private var minTemperature: Float = Float.MAX_VALUE
    private var maxTemperature: Float = Float.MIN_VALUE

    override fun submitList(list: List<DailyForecast>?) {
        /*
        Consider the following case:
        Day X: minimum value: 10.01, max value 15.99
        Day Y: minimum value 10.49, max value 15.51
        In the above examples, min and max temperature are 10 and 16 on both days,
        but the slider will be drawn according to the floats, not the min/max as ints.
        Therefore we round all values to 0 decimals here before drawing the slider.
        */
        minTemperature = list
            ?.minOfOrNull { it.minTemp ?: Float.MAX_VALUE }
            ?.let { roundToDecimals(it, 0) } ?: 0F
        maxTemperature = list
            ?.maxOfOrNull { it.maxTemp ?: Float.MIN_VALUE }
            ?.let { roundToDecimals(it, 0) } ?: 0F

        // Update data
        super.submitList(list?.map {
            it.apply {
                this.minTemp = roundToDecimals(this.minTemp ?: 0F, 0)
                this.maxTemp = roundToDecimals(this.maxTemp ?: 0F, 0)
            }
        })
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
            if (minTemperature == Float.MAX_VALUE || maxTemperature == Float.MIN_VALUE) {
                binding.temperature.visibility = View.INVISIBLE
            } else {
                binding.temperature.apply {
                    valueFrom = minTemperature
                    valueTo = maxTemperature
                    values = listOf(item.minTemp, item.maxTemp)
                }
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
                isActivated = true
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
