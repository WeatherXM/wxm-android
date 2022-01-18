package com.weatherxm.ui.userdevice

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ListItemHourlyWeatherBinding
import com.weatherxm.util.Weather
import com.weatherxm.util.getHourMinutesFromISO
import org.koin.core.component.KoinComponent

class HourlyAdapter(
    private val context: Context,
    private val onForecastClick: (HourlyWeather) -> Unit,
) :
    ListAdapter<HourlyWeather, HourlyAdapter.HourlyViewHolder>(HourlyDiffCallback()),
    KoinComponent {
    private var selectedPos = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding =
            ListItemHourlyWeatherBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val holder = HourlyViewHolder(binding)

        holder.itemView.setOnClickListener {
            val lastSelectedItem = selectedPos
            selectedPos = holder.adapterPosition
            holder.itemView.isActivated = true
            notifyItemChanged(lastSelectedItem)
            notifyItemChanged(selectedPos)
        }
        return holder
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.isSelected = isSelected(position)

        if (holder.itemView.isSelected) {
            selectedPos = holder.adapterPosition
            holder.itemView.isActivated = true
            /*holder.getBinding().card.cardElevation =
                context.resources.getDimension(R.dimen.card_selected_elevation)*/
            onForecastClick.invoke(getItem(selectedPos))
        } else {
            holder.itemView.isActivated = false
            /*holder.getBinding().card.cardElevation =
                context.resources.getDimension(R.dimen.card_elevation)*/
        }
    }

    fun resetSelected() {
        selectedPos = RecyclerView.NO_POSITION
    }

    private fun isSelected(position: Int): Boolean {
        return selectedPos == position || (selectedPos == RecyclerView.NO_POSITION && position == 0)
    }

    inner class HourlyViewHolder(private val binding: ListItemHourlyWeatherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyWeather) {
            item.timestamp?.let {
                binding.time.text = getHourMinutesFromISO(context, it)
                binding.icon.setAnimation(Weather.getWeatherAnimation(item.icon))
                // TODO binding.icon.playAnimation()
                binding.temperature.text = Weather.getFormattedTemperature(item.temperature)
            }
        }

        fun getBinding(): ListItemHourlyWeatherBinding {
            return binding
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
