package com.weatherxm.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.util.Rewards.getRewardScoreColor
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
                if (!item.isOwned()) {
                    binding.noDataMessage.text =
                        itemView.context.getString(R.string.no_data_message_public_device)
                }
            } else {
                setWeatherData(item)
            }

            /**
             * STOPSHIP:
             * TODO: Is this OK? Confirm it.
             */
            binding.address.text = if (item.address.isNullOrEmpty()) {
                itemView.context.getString(R.string.unknown_address)
            } else {
                item.address
            }

            binding.status.setStatusChip(item)
            binding.bundle.setBundleChip(item)

            setAlerts(item)
            setStationHealth(item)
        }

        private fun setWeatherData(item: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.currentWeather?.icon))
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
                Weather.getFormattedHumidity(item.currentWeather?.humidity, includeUnit = false),
                "%"
            )
            val windValue = Weather.getFormattedWind(
                item.currentWeather?.windSpeed,
                item.currentWeather?.windDirection,
                includeUnits = false
            )
            val windUnit = Weather.getPreferredUnit(
                itemView.context.getString(CacheService.KEY_WIND),
                itemView.context.getString(R.string.wind_speed_ms)
            )
            binding.wind.setData(
                windValue,
                "$windUnit ${getFormattedWindDirection(item.currentWeather?.windDirection)}",
                getWindDirectionDrawable(itemView.context, item.currentWeather?.windDirection)
            )

            binding.rain.setData(
                Weather.getFormattedPrecipitation(
                    item.currentWeather?.precipitation, includeUnit = false
                ),
                Weather.getPrecipitationPreferredUnit()
            )
        }

        private fun setAlerts(item: UIDevice) {
            if (item.alerts.isEmpty()) {
                binding.root.strokeWidth = 0
                binding.issueChip.visible(false)
                return
            }

            /**
             * STOPSHIP:
             * TODO: Include here if Data Quality is error/warning or location has error for stroke purposes
             */
            val hasErrorSeverity = item.hasErrors()

            if (hasErrorSeverity) {
                binding.root.setCardStroke(R.color.error, 2)
            } else {
                binding.root.setCardStroke(R.color.warning, 2)
            }

            if (item.alerts.size > 1) {
                if (hasErrorSeverity) {
                    binding.issueChip.errorChip()
                } else {
                    binding.issueChip.warningChip()
                }
                binding.issueChip.text =
                    itemView.context.getString(R.string.issues, item.alerts.size)
            } else {
                when (item.alerts[0]) {
                    DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY) -> {
                        binding.issueChip.lowBatteryChip()
                    }
                    DeviceAlert.createError(DeviceAlertType.OFFLINE) -> {
                        binding.issueChip.offlineChip()
                    }
                    DeviceAlert.createWarning(DeviceAlertType.NEEDS_UPDATE) -> {
                        binding.issueChip.updateRequiredChip()
                        analytics.trackEventPrompt(
                            AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                            AnalyticsService.ParamValue.WARN.paramValue,
                            AnalyticsService.ParamValue.VIEW.paramValue
                        )
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
            binding.issueChip.visible(true)
        }

        private fun setStationHealth(item: UIDevice) {
            /**
             * STOPSHIP:
             * TODO: Handle it properly based on the actual UIDevice parameters
             */
            binding.dataQuality.text = itemView.context.getString(R.string.data_quality_value, 100)
            binding.dataQualityIcon.setColor(getRewardScoreColor(100))
            binding.addressIcon.setColor(R.color.success)
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<UIDevice>() {

        override fun areItemsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.id == newItem.id
        }

        @Suppress("MaxLineLength", "CyclomaticComplexMethod")
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
                oldItem.lastWeatherStationActivity == newItem.lastWeatherStationActivity &&
                oldItem.isActive == newItem.isActive
        }
    }
}

interface DeviceListener {
    fun onDeviceClicked(device: UIDevice)
    fun onFollowBtnClicked(device: UIDevice)
}
