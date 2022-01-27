package com.weatherxm.ui.deviceforecast

import android.view.LayoutInflater
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

    override fun submitList(list: List<DailyForecast>?) {
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
            binding.dateOfDay.text = item.dateOfDay
            binding.nameOfDay.text = item.nameOfDay
            binding.icon.apply {
                setAnimation(Weather.getWeatherAnimation(item.icon))
                playAnimation()
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
