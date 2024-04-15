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
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Weather
import com.weatherxm.util.getShortName
import org.koin.core.component.KoinComponent
import java.time.LocalDate

class DailyTileForecastAdapter(
    private var selectedDate: LocalDate,
    private val onClickListener: (UIForecastDay) -> Unit
) : ListAdapter<UIForecastDay, DailyTileForecastAdapter.DailyTileViewHolder>(
    UIForecastDayDiffCallback()
), KoinComponent {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyTileViewHolder {
        val binding = ListItemDailyTileForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyTileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyTileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyTileViewHolder(private val binding: ListItemDailyTileForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UIForecastDay) {
            binding.root.setOnClickListener {
                selectedDate = item.date
                binding.root.setCardStroke(R.color.colorPrimary, 2)
                onClickListener.invoke(item)
            }
            // TODO: Clear previous selected daily card 

            if (selectedDate == item.date) {
//                binding.root.setCardBackgroundColor(
//                    itemView.context.getColor(R.color.daily_tile_selected_background)
//                )
                binding.root.setCardStroke(R.color.colorPrimary, 2)
            } else {
//                binding.root.setCardBackgroundColor(
//                    itemView.context.getColor(R.color.daily_tile_unselected_background)
//                )
            }

            binding.timestamp.text = item.date.dayOfWeek.getShortName(itemView.context)
            binding.icon.apply {
                setAnimation(Weather.getWeatherAnimation(item.icon))
                playAnimation()
            }
            binding.temperaturePrimary.text = Weather.getFormattedTemperature(item.maxTemp)
            binding.temperatureSecondary.text = Weather.getFormattedTemperature(item.minTemp)
            binding.temperatureSecondary.setVisible(true)
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
