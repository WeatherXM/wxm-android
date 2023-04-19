package com.weatherxm.ui.userdevice.forecast

import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemHourlyWeatherBinding
import com.weatherxm.ui.userdevice.forecast.HourlyAdapter.HourlyViewHolder
import com.weatherxm.util.DateTimeHelper.getHourMinutesFromISO
import com.weatherxm.util.UnitConverter
import com.weatherxm.util.Weather
import com.weatherxm.util.setHtml

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

    inner class HourlyViewHolder(
        private val binding: ListItemHourlyWeatherBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyWeather) {
            binding.time.text = getHourMinutesFromISO(
                itemView.context, item.timestamp.toString(), false
            )
            binding.icon.apply {
                setAnimation(Weather.getWeatherAnimation(item.icon))
                playAnimation()
            }
            binding.temperature.text = Weather.getFormattedTemperature(item.temperature)
            binding.feelsLike.setHtml(
                R.string.feels_like_with_value,
                Weather.getFormattedTemperature(item.temperature, fullUnit = false)
            )
            binding.precipitationProbability.setData(
                Weather.getFormattedPrecipitationProbability(item.precipProbability, false),
                "%"
            )

            binding.precipitationAccumulated.setData(
                Weather.getFormattedPrecipitation(
                    item.precipitation,
                    isRainRate = false,
                    includeUnit = false
                ),
                Weather.getPrecipitationPreferredUnit(false)
            )

            val windValue = Weather.getFormattedWind(item.windSpeed, item.windDirection, false)
            val windUnit = Weather.getPreferredUnit(
                itemView.context.getString(CacheService.KEY_WIND),
                itemView.context.getString(R.string.wind_speed_ms)
            )

            val windDirectionDrawable = ResourcesCompat.getDrawable(
                itemView.resources, R.drawable.layers_wind_direction, null
            ) as LayerDrawable

            item.windDirection?.let {
                val index = UnitConverter.getIndexOfCardinal(it)
                binding.wind.setData(windValue, windUnit, windDirectionDrawable.getDrawable(index))
            } ?: binding.wind.setData(windValue, windUnit)

            binding.humidity.setData(
                Weather.getFormattedHumidity(item.humidity, includeUnit = false), "%"
            )

            val pressureUnit = Weather.getPreferredUnit(
                itemView.context.getString(CacheService.KEY_PRESSURE),
                itemView.context.getString(R.string.pressure_hpa)
            )
            binding.pressure.setData(
                Weather.getFormattedPressure(item.pressure, includeUnit = false),
                pressureUnit
            )
            binding.uv.setData(Weather.getFormattedUV(item.uvIndex))
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
