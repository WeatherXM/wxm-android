package com.weatherxm.ui.userdevice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.databinding.ListItemHourlyWeatherBinding
import org.koin.core.component.KoinComponent

class HourlyAdapter(private val hourlyCards: Array<HourlyCard>) :
    RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder>(),  KoinComponent {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding =
            ListItemHourlyWeatherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HourlyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        holder.bind(hourlyCards[position])
    }

    class HourlyViewHolder(private val binding: ListItemHourlyWeatherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyCard) {
            binding.hour.text = item.time
            binding.icon.setAnimation(item.weatherAnimation)
            binding.temperature.text = item.temp
        }
    }

    override fun getItemCount() = hourlyCards.size
}
