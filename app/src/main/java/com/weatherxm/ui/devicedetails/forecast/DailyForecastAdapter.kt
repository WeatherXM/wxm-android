package com.weatherxm.ui.devicedetails.forecast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.databinding.ListItemForecastBinding
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.util.DateTimeHelper.getRelativeDayAndMonthDay
import com.weatherxm.util.Resources
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getWindDirectionDrawable
import com.weatherxm.util.NumberUtils.roundToDecimals
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DailyForecastAdapter(private val onClickListener: (UIForecastDay) -> Unit) :
    ListAdapter<UIForecastDay, DailyForecastAdapter.DailyForecastViewHolder>(
        UIForecastDiffCallback()
    ), KoinComponent {

    private var minTemperature: Float = Float.MAX_VALUE
    private var maxTemperature: Float = Float.MIN_VALUE

    val resources: Resources by inject()

    override fun submitList(list: List<UIForecastDay>?) {
        /*
        Consider the following case:
        Day X: minimum value: 10.01, max value 15.99
        Day Y: minimum value 10.49, max value 15.51
        In the above examples, min and max temperature are 10 and 16 on both days,
        but the slider will be drawn according to the floats, not the min/max as ints.
        Therefore we round all values to 0 decimals here before drawing the sliders.
        */
        minTemperature = list
            ?.minOfOrNull { it.minTemp ?: Float.MAX_VALUE }
            ?.let { roundToDecimals(it, 0) }
            ?: 0F
        maxTemperature = list
            ?.maxOfOrNull { it.maxTemp ?: Float.MIN_VALUE }
            ?.let { roundToDecimals(it, 0) }
            ?: 0F

        // Update data
        super.submitList(list?.map {
            it.apply {
                this.minTemp = roundToDecimals(this.minTemp ?: 0F, 0)
                this.maxTemp = roundToDecimals(this.maxTemp ?: 0F, 0)
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyForecastViewHolder {
        val binding = ListItemForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyForecastViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyForecastViewHolder(private val binding: ListItemForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UIForecastDay) {
            binding.root.setOnClickListener {
                onClickListener(item)
            }

            binding.date.text = item.date.getRelativeDayAndMonthDay(itemView.context)
            binding.icon.setWeatherAnimation(item.icon)
            if (minTemperature == Float.MAX_VALUE || maxTemperature == Float.MIN_VALUE) {
                binding.temperature.invisible()
            } else {
                binding.temperature.apply {
                    valueFrom = minTemperature
                    valueTo = maxTemperature
                    values = listOf(item.minTemp, item.maxTemp)
                }
            }
            binding.minTemperature.text = Weather.getFormattedTemperature(item.minTemp)
            binding.maxTemperature.text = Weather.getFormattedTemperature(item.maxTemp)

            binding.precipProbability.text =
                Weather.getFormattedPrecipitationProbability(item.precipProbability)
            binding.precip.text = Weather.getFormattedPrecipitation(item.precip, isRainRate = false)

            binding.wind.text = Weather.getFormattedWind(item.windSpeed, item.windDirection)
            binding.windIcon.setImageDrawable(
                getWindDirectionDrawable(itemView.context, item.windDirection)
            )
            binding.humidity.text = Weather.getFormattedHumidity(item.humidity)
        }
    }

    class UIForecastDiffCallback : DiffUtil.ItemCallback<UIForecastDay>() {

        override fun areItemsTheSame(oldItem: UIForecastDay, newItem: UIForecastDay): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: UIForecastDay, newItem: UIForecastDay): Boolean {
            return oldItem.date == newItem.date &&
                oldItem.icon == newItem.icon &&
                oldItem.maxTemp == newItem.maxTemp &&
                oldItem.minTemp == newItem.minTemp &&
                oldItem.precipProbability == newItem.precipProbability &&
                oldItem.windSpeed == newItem.windSpeed &&
                oldItem.windDirection == newItem.windDirection &&
                oldItem.humidity == newItem.humidity &&
                oldItem.hourlyWeather?.size == newItem.hourlyWeather?.size
        }
    }
}
