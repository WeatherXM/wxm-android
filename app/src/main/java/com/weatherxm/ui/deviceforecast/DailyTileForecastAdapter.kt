package com.weatherxm.ui.deviceforecast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemDailyTileForecastBinding
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setWeatherAnimation
import com.weatherxm.util.Weather.getFormattedTemperature
import com.weatherxm.util.getShortName
import org.koin.core.component.KoinComponent
import java.time.LocalDate

class DailyTileForecastAdapter(
    private var selectedDate: LocalDate,
    private val onNewSelectedPosition: (Int, Int) -> Unit,
    private val onClickListener: (UIForecastDay) -> Unit
) : ListAdapter<UIForecastDay, DailyTileForecastAdapter.DailyTileViewHolder>(
    UIForecastDayDiffCallback()
), KoinComponent {

    private var selectedPosition = 0

    fun getSelectedPosition(): Int = selectedPosition

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyTileViewHolder {
        val binding = ListItemDailyTileForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyTileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyTileViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class DailyTileViewHolder(private val binding: ListItemDailyTileForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UIForecastDay, position: Int) {
            binding.root.setOnClickListener {
                onClickListener.invoke(item)
                selectedDate = item.date
                checkSelectionStatus(item, position)
            }

            checkSelectionStatus(item, position)

            binding.timestamp.text = item.date.dayOfWeek.getShortName(itemView.context)
            binding.icon.setWeatherAnimation(item.icon)
            binding.temperaturePrimary.text = getFormattedTemperature(item.maxTemp)
            binding.temperatureSecondary.text = getFormattedTemperature(item.minTemp)
        }

        private fun checkSelectionStatus(item: UIForecastDay, position: Int) {
            if (selectedDate == item.date) {
                selectedPosition = position
                binding.root.setCardBackgroundColor(
                    itemView.context.getColor(R.color.daily_selected_tile)
                )
                binding.root.setCardStroke(R.color.colorPrimary, 2)
                onNewSelectedPosition.invoke(position, binding.root.width)
            } else {
                binding.root.setCardBackgroundColor(
                    itemView.context.getColor(R.color.daily_unselected_tile)
                )
                binding.root.strokeWidth = 0
            }
        }
    }

    class UIForecastDayDiffCallback : DiffUtil.ItemCallback<UIForecastDay>() {

        override fun areItemsTheSame(oldItem: UIForecastDay, newItem: UIForecastDay): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: UIForecastDay, newItem: UIForecastDay): Boolean {
            return oldItem.date == newItem.date &&
                oldItem.icon == newItem.icon &&
                oldItem.minTemp == newItem.minTemp &&
                oldItem.maxTemp == newItem.maxTemp &&
                oldItem.precip == newItem.precip &&
                oldItem.precipProbability == newItem.precipProbability &&
                oldItem.windSpeed == newItem.windSpeed &&
                oldItem.windDirection == newItem.windDirection &&
                oldItem.humidity == newItem.humidity
        }
    }
}
