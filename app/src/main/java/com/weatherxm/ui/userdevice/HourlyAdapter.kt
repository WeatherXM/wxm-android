package com.weatherxm.ui.userdevice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ListItemHourlyWeatherBinding
import com.weatherxm.ui.userdevice.HourlyAdapter.HourlyViewHolder
import com.weatherxm.util.Weather
import com.weatherxm.util.getHourMinutesFromISO
import org.koin.core.component.KoinComponent

class HourlyAdapter(
    private val onHourlyForecastSelected: (HourlyWeather) -> Unit
) : ListAdapter<HourlyWeather, HourlyViewHolder>(HourlyDiffCallback()), KoinComponent {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun submitList(list: List<HourlyWeather>?) {
        // Reset selected position
        selectedPosition = if (list.isNullOrEmpty()) RecyclerView.NO_POSITION else 0
        // Update data
        super.submitList(list)
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
        private val onHourlyForecastSelected: (HourlyWeather) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                // Invoke listener
                onHourlyForecastSelected(getItem(adapterPosition))

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
            binding.temperature.text = Weather.getFormattedTemperature(item.temperature)
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
                oldItem.cloudCover == newItem.cloudCover &&
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
