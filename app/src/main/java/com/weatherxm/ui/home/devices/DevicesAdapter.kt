package com.weatherxm.ui.home.devices

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.setCardStroke
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setStatusChip
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.Resources
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceAdapter(private val deviceListener: DeviceListener) :
    ListAdapter<UIDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()), KoinComponent {

    val resources: Resources by inject()
    val analytics: Analytics by inject()

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
                when (item.relation) {
                    DeviceRelation.OWNED -> {
                        setImageResource(R.drawable.ic_home)
                        setColor(R.color.colorOnSurface)
                        isEnabled = false
                    }
                    DeviceRelation.FOLLOWED -> {
                        setOnClickListener {
                            listener.onFollowBtnClicked(device)
                        }
                        setImageResource(R.drawable.ic_favorite)
                        setColor(R.color.follow_heart_color)
                        isEnabled = true
                    }
                    DeviceRelation.UNFOLLOWED -> {
                        setOnClickListener {
                            listener.onFollowBtnClicked(device)
                        }
                        setImageResource(R.drawable.ic_favorite_outline)
                        setColor(R.color.follow_heart_color)
                        isEnabled = true
                    }
                    null -> setVisible(false)
                }
            }

            binding.name.text = item.getDefaultOrFriendlyName()

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visibility = GONE
                binding.noDataLayout.visibility = VISIBLE
            } else {
                setWeatherData(item)
            }

            binding.address.text = if (item.address.isNullOrEmpty()) {
                resources.getString(R.string.unknown_address)
            } else {
                item.address
            }

            with(binding.status) {
                setStatusChip(
                    item.lastWeatherStationActivity?.getRelativeFormattedTime(
                        fallbackIfTooSoon = context.getString(R.string.just_now)
                    ),
                    item.profile,
                    item.isActive,
                )
            }
            setAlerts(item)
        }

        private fun setAlerts(item: UIDevice) {
            binding.alertsError.setVisible(false)
            binding.error.setVisible(false)
            binding.warning.setVisible(false)
            binding.root.setCardStroke(R.color.transparent, 0)
            if (item.alerts.size > 1) {
                binding.root.setCardStroke(R.color.error, 2)
                binding.alertsError.title(
                    itemView.context.getString(R.string.issues, item.alerts.size.toString())
                ).action {
                    deviceListener.onAlertsClicked(item)
                }.setVisible(true)
            } else if (item.alerts.contains(DeviceAlert.OFFLINE)) {
                binding.root.setCardStroke(R.color.error, 2)
                binding.error.setVisible(true)
            } else if (item.alerts.contains(DeviceAlert.NEEDS_UPDATE)) {
                binding.root.setCardStroke(R.color.warning, 2)
                binding.warning.action(resources.getString(R.string.update_station_now)) {
                    deviceListener.onUpdateStationClicked(item)
                }.setVisible(true)
                analytics.trackEventPrompt(
                    Analytics.ParamValue.OTA_AVAILABLE.paramValue,
                    Analytics.ParamValue.WARN.paramValue,
                    Analytics.ParamValue.VIEW.paramValue
                )
            }
        }

        private fun setWeatherData(item: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.currentWeather?.icon))
            binding.temperature.text = Weather.getFormattedTemperature(
                item.currentWeather?.temperature, 1, includeUnit = false
            )
            binding.temperatureUnit.text = Weather.getPreferredUnit(
                resources.getString(CacheService.KEY_TEMPERATURE),
                resources.getString(R.string.temperature_celsius)
            )
            binding.feelsLike.text = Weather.getFormattedTemperature(
                item.currentWeather?.feelsLike, 1, includeUnit = false
            )
            binding.feelsLikeUnit.text = Weather.getPreferredUnit(
                resources.getString(CacheService.KEY_TEMPERATURE),
                resources.getString(R.string.temperature_celsius)
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
                resources.getString(CacheService.KEY_WIND),
                resources.getString(R.string.wind_speed_ms)
            )
            val windDirectionUnit = item.currentWeather?.windDirection?.let {
                Weather.getFormattedWindDirection(it)
            } ?: String.empty()
            binding.wind.setData(windValue, "$windUnit $windDirectionUnit")

            binding.rain.setData(
                Weather.getFormattedPrecipitation(
                    item.currentWeather?.precipitation, includeUnit = false
                ), Weather.getPrecipitationPreferredUnit()
            )
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
                oldItem.profile == newItem.profile &&
                oldItem.needsUpdate() == newItem.needsUpdate() &&
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
    fun onUpdateStationClicked(device: UIDevice)
    fun onAlertsClicked(device: UIDevice)
    fun onFollowBtnClicked(device: UIDevice)
}
