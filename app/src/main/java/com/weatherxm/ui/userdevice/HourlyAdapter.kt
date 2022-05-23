package com.weatherxm.ui.userdevice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ListItemHourlyWeatherBinding
import com.weatherxm.ui.SelectedHourlyForecast
import com.weatherxm.ui.userdevice.HourlyAdapter.HourlyViewHolder
import com.weatherxm.ui.userdevice.UserDeviceViewModel.ForecastState
import com.weatherxm.util.DateTimeHelper.getHourMinutesFromISO
import com.weatherxm.util.Weather

class HourlyAdapter(
    private val onHourlyForecastSelected: (SelectedHourlyForecast) -> Unit
) : ListAdapter<HourlyWeather, HourlyViewHolder>(HourlyDiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION
    private var forecastState: ForecastState = ForecastState.TODAY

    fun setForecastState(newState: ForecastState) {
        forecastState = newState
    }

    override fun submitList(list: List<HourlyWeather>?) {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition != 0) {
            // Reset list as we need to reset the selected item also
            super.submitList(null)
        }

        // Reset selected position
        selectedPosition = if (list.isNullOrEmpty()) RecyclerView.NO_POSITION else 0

        // Update data
        super.submitList(list)

        // Invoke callback for newly selected position
        list?.let {
            onHourlyForecastSelected(SelectedHourlyForecast(it[selectedPosition], selectedPosition))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding = ListItemHourlyWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HourlyViewHolder(binding, onHourlyForecastSelected)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, isSelected(position))
    }

    private fun isSelected(position: Int): Boolean {
        return selectedPosition == position
            || (selectedPosition == RecyclerView.NO_POSITION && position == 0)
    }

    inner class HourlyViewHolder(
        private val binding: ListItemHourlyWeatherBinding,
        private val onHourlyForecastSelected: (SelectedHourlyForecast) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                // Invoke listener
                onHourlyForecastSelected(
                    SelectedHourlyForecast(getItem(adapterPosition), adapterPosition)
                )

                // Change the selected state
                val lastSelectedItem = selectedPosition
                selectedPosition = adapterPosition

                // Notify unselected & selected items, to force UI update
                notifyItemChanged(lastSelectedItem)
                notifyItemChanged(selectedPosition)
            }
        }

        fun bind(item: HourlyWeather, isSelected: Boolean) {
            binding.root.isActivated = isSelected
            binding.time.text = getHourMinutesFromISO(itemView.context, item.timestamp)
            if (forecastState == ForecastState.TODAY && adapterPosition == 0) {
                binding.temperature.text = Weather.getFormattedTemperature(item.temperature, 1)
            } else {
                binding.temperature.text = Weather.getFormattedTemperature(item.temperature)
            }
            binding.precipitation.text = if (item.precipitation != null) {
                Weather.getFormattedPrecipitation(item.precipitation)
            } else {
                Weather.getFormattedPrecipitationProbability(item.precipProbability)
            }
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
