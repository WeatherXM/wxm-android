package com.weatherxm.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.util.UnitSelector
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getFormattedWindDirection
import com.weatherxm.util.Weather.getWindDirectionDrawable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceAdapter(private val deviceListener: DeviceListener) :
    ListAdapter<UIDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()), KoinComponent {

    val analytics: AnalyticsWrapper by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding =
            ListItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding, deviceListener)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(
        private val binding: ListItemDeviceBinding,
        private val listener: DeviceListener,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var device: UIDevice

        init {
            binding.root.setOnClickListener {
                listener.onDeviceClicked(device)
            }
        }

        fun bind(item: UIDevice) {
            this.device = item

            with(binding.follow) {
                isEnabled = item.relation != DeviceRelation.OWNED
                when (item.relation) {
                    DeviceRelation.OWNED -> {
                        setImageResource(R.drawable.ic_home)
                        setColor(R.color.colorOnSurface)
                    }
                    DeviceRelation.FOLLOWED -> {
                        setOnClickListener {
                            listener.onFollowBtnClicked(device)
                        }
                        setImageResource(R.drawable.ic_favorite)
                        setColor(R.color.follow_heart_color)
                    }
                    DeviceRelation.UNFOLLOWED -> {
                        setOnClickListener {
                            listener.onFollowBtnClicked(device)
                        }
                        setImageResource(R.drawable.ic_favorite_outline)
                        setColor(R.color.follow_heart_color)
                    }
                    null -> visible(false)
                }
            }

            binding.name.text = item.getDefaultOrFriendlyName()

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visible(false)
                binding.noDataLayout.visible(true)
                item.setNoDataMessage(itemView.context, binding.noDataMessage)
            } else {
                setWeatherData(item)
            }

            binding.address.text = if (item.address.isNullOrEmpty()) {
                itemView.context.getString(R.string.unknown_address)
            } else {
                item.address
            }

            binding.status.setStatusChip(item)
            binding.bundle.setBundleChip(item)
            item.handleAlerts(itemView.context, binding.issueChip, analytics)
            item.handleStroke(binding.root)
            item.stationHealthViewsOnList(
                itemView.context,
                binding.dataQuality,
                binding.dataQualityIcon,
                binding.addressIcon
            )
        }

        private fun setWeatherData(item: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.currentWeather?.icon))
            binding.temperature.setData(
                Weather.getFormattedTemperature(
                    context = itemView.context,
                    value = device.currentWeather?.temperature,
                    decimals = 1,
                    includeUnit = false
                ),
                UnitSelector.getTemperatureUnit(itemView.context).unit
            )
            binding.humidity.setData(
                Weather.getFormattedHumidity(item.currentWeather?.humidity, includeUnit = false),
                "%"
            )
            val windValue = Weather.getFormattedWind(
                context = itemView.context,
                windSpeed = item.currentWeather?.windSpeed,
                windDirection = item.currentWeather?.windDirection,
                includeUnits = false
            )
            val windUnit = UnitSelector.getWindUnit(itemView.context).unit
            val windDirection =
                getFormattedWindDirection(itemView.context, item.currentWeather?.windDirection)
            binding.wind.setData(
                windValue,
                "$windUnit $windDirection",
                getWindDirectionDrawable(itemView.context, item.currentWeather?.windDirection)
            )

            binding.rain.setData(
                Weather.getFormattedPrecipitation(
                    context = itemView.context,
                    value = item.currentWeather?.precipitation,
                    includeUnit = false
                ),
                UnitSelector.getPrecipitationUnit(itemView.context, true).unit
            )
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<UIDevice>() {

        override fun areItemsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.id == newItem.id
        }

        @Suppress("CyclomaticComplexMethod")
        override fun areContentsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.currentWeather?.icon == newItem.currentWeather?.icon &&
                oldItem.currentWeather?.temperature == newItem.currentWeather?.temperature &&
                oldItem.currentWeather?.humidity == newItem.currentWeather?.humidity &&
                oldItem.currentWeather?.precipitation == newItem.currentWeather?.precipitation &&
                oldItem.currentWeather?.windSpeed == newItem.currentWeather?.windSpeed &&
                oldItem.currentWeather?.windDirection == newItem.currentWeather?.windDirection &&
                oldItem.currentWeather?.feelsLike == newItem.currentWeather?.feelsLike &&
                oldItem.currentWeather?.timestamp == newItem.currentWeather?.timestamp &&
                oldItem.bundleName == newItem.bundleName &&
                oldItem.bundleTitle == newItem.bundleTitle &&
                oldItem.connectivity == newItem.connectivity &&
                oldItem.wsModel == newItem.wsModel &&
                oldItem.gwModel == newItem.gwModel &&
                oldItem.hwClass == newItem.hwClass &&
                oldItem.alerts.size == newItem.alerts.size &&
                oldItem.currentFirmware == newItem.currentFirmware &&
                oldItem.assignedFirmware == newItem.assignedFirmware &&
                oldItem.friendlyName == newItem.friendlyName &&
                oldItem.relation == newItem.relation &&
                oldItem.lastWeatherStationActivity == newItem.lastWeatherStationActivity &&
                oldItem.isActive == newItem.isActive
        }
    }
}

interface DeviceListener {
    fun onDeviceClicked(device: UIDevice)
    fun onFollowBtnClicked(device: UIDevice)
}
