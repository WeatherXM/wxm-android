package com.weatherxm.ui.home.locations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.databinding.ListItemLocationWeatherBinding
import com.weatherxm.ui.common.LocationWeather
import org.koin.core.component.KoinComponent

class LocationsAdapter(private val onClickListener: (LocationWeather) -> Unit) :
    ListAdapter<LocationWeather, LocationsAdapter.DailyLocationWeatherViewHolder>
        (LocationWeatherDiffCallback()),
    KoinComponent {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DailyLocationWeatherViewHolder {
        val binding = ListItemLocationWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyLocationWeatherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyLocationWeatherViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyLocationWeatherViewHolder(
        private val binding: ListItemLocationWeatherBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocationWeather) {
            binding.root.setData(item) {
                onClickListener(item)
            }
        }
    }

    class LocationWeatherDiffCallback : DiffUtil.ItemCallback<LocationWeather>() {

        override fun areItemsTheSame(oldItem: LocationWeather, newItem: LocationWeather): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: LocationWeather,
            newItem: LocationWeather
        ): Boolean {
            return oldItem.coordinates == newItem.coordinates &&
                    oldItem.icon == newItem.icon &&
                    oldItem.address == newItem.address &&
                    oldItem.dailyMaxTemp == newItem.dailyMaxTemp &&
                    oldItem.dailyMinTemp == newItem.dailyMinTemp &&
                    oldItem.currentTemp == newItem.currentTemp &&
                    oldItem.currentWeatherSummaryResId == newItem.currentWeatherSummaryResId
        }
    }
}
