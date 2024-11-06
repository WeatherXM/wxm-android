package com.weatherxm.ui.widgets.selectstation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemWidgetSelectStationBinding
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.handleAlerts
import com.weatherxm.ui.common.handleStroke
import com.weatherxm.ui.common.setBundleChip
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setNoDataMessage
import com.weatherxm.ui.common.setStatusChip
import com.weatherxm.ui.common.stationHealthViewsOnList
import com.weatherxm.ui.common.visible
import com.weatherxm.util.Resources
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getFormattedWindDirection
import com.weatherxm.util.Weather.getWindDirectionDrawable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SelectStationAdapter(private val stationListener: (UIDevice) -> Unit) :
    ListAdapter<UIDevice, SelectStationAdapter.SelectStationViewHolder>(
        SelectStationDiffCallback()
    ), KoinComponent {

    val resources: Resources by inject()

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectStationViewHolder {
        val binding =
            ListItemWidgetSelectStationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return SelectStationViewHolder(binding, stationListener)
    }

    override fun onBindViewHolder(holder: SelectStationViewHolder, position: Int) {
        holder.bind(getItem(position), selectedPosition == position)
    }

    inner class SelectStationViewHolder(
        private val binding: ListItemWidgetSelectStationBinding,
        private val stationListener: (UIDevice) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var device: UIDevice

        init {
            binding.root.setOnClickListener {
                stationListener.invoke(device)

                // Change the selected state
                val lastSelectedItem = selectedPosition
                selectedPosition = absoluteAdapterPosition

                // Notify unselected & selected items, to force UI update
                notifyItemChanged(lastSelectedItem)
                notifyItemChanged(selectedPosition)
            }
        }

        fun bind(item: UIDevice, isSelected: Boolean) {
            binding.root.isSelected = isSelected
            binding.selectedIcon.visible(isSelected)

            with(binding.relationIcon) {
                when (item.relation) {
                    DeviceRelation.OWNED -> {
                        setImageResource(R.drawable.ic_home)
                        setColor(R.color.colorOnSurface)
                        isEnabled = false
                    }
                    DeviceRelation.FOLLOWED -> {
                        setImageResource(R.drawable.ic_favorite)
                        setColor(R.color.follow_heart_color)
                        isEnabled = true
                    }
                    else -> visible(false)
                }
            }

            this.device = item
            binding.name.text = item.getDefaultOrFriendlyName()

            binding.address.text = if (item.address.isNullOrEmpty()) {
                resources.getString(R.string.unknown_address)
            } else {
                item.address
            }

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visible(false)
                binding.noDataLayout.visible(true)
                item.setNoDataMessage(itemView.context, binding.noDataMessage)
            } else {
                setWeatherData(item)
            }

            binding.status.setStatusChip(item)
            binding.bundle.setBundleChip(item)
            item.handleAlerts(itemView.context, binding.issueChip, null)
            item.handleStroke(binding.root)
            item.stationHealthViewsOnList(
                itemView.context,
                binding.dataQuality,
                binding.dataQualityIcon,
                binding.addressIcon
            )
        }

        private fun setWeatherData(device: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(device.currentWeather?.icon))
            binding.temperature.setData(
                Weather.getFormattedTemperature(
                    device.currentWeather?.temperature, 1, includeUnit = false
                ),
                Weather.getPreferredUnit(
                    itemView.context.getString(CacheService.KEY_TEMPERATURE),
                    itemView.context.getString(R.string.temperature_celsius)
                )
            )
            binding.humidity.setData(
                Weather.getFormattedHumidity(
                    device.currentWeather?.humidity, includeUnit = false
                ), "%"
            )
            val windValue = Weather.getFormattedWind(
                device.currentWeather?.windSpeed,
                device.currentWeather?.windDirection,
                includeUnits = false
            )
            val windUnit = Weather.getPreferredUnit(
                resources.getString(CacheService.KEY_WIND),
                resources.getString(R.string.wind_speed_ms)
            )
            binding.wind.setData(
                windValue,
                "$windUnit ${getFormattedWindDirection(device.currentWeather?.windDirection)}",
                getWindDirectionDrawable(itemView.context, device.currentWeather?.windDirection)
            )

            binding.rain.setData(
                Weather.getFormattedPrecipitation(
                    device.currentWeather?.precipitation, includeUnit = false
                ), Weather.getPrecipitationPreferredUnit()
            )
        }
    }

    class SelectStationDiffCallback : DiffUtil.ItemCallback<UIDevice>() {

        override fun areItemsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.id == newItem.id
        }

        @Suppress("MaxLineLength", "CyclomaticComplexMethod")
        override fun areContentsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.id == newItem.id &&
                oldItem.name == newItem.name &&
                oldItem.address == newItem.address
        }
    }
}
