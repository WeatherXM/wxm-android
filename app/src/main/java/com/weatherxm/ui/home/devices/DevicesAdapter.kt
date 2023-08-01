package com.weatherxm.ui.home.devices

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceOwnershipStatus
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceAdapter(private val deviceListener: DeviceListener) :
    ListAdapter<UIDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()), KoinComponent {

    val resHelper: ResourcesHelper by inject()
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
        listener: DeviceListener,
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var device: UIDevice

        init {
            binding.root.setOnClickListener {
                listener.onDeviceClicked(device)
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: UIDevice) {
            this.device = item
            binding.name.text = item.getDefaultOrFriendlyName()

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visibility = GONE
                binding.noDataLayout.visibility = VISIBLE
            } else {
                setWeatherData(item)
            }

            binding.address.text = if (item.address.isNullOrEmpty()) {
                resHelper.getString(R.string.unknown_address)
            } else {
                item.address
            }

            @Suppress("UseCheckOrError")
            binding.stationFollowHomeIcon.setImageResource(
                when (item.ownershipStatus) {
                    DeviceOwnershipStatus.OWNED -> R.drawable.ic_home
                    DeviceOwnershipStatus.FOLLOWED -> R.drawable.ic_favorite
                    DeviceOwnershipStatus.UNFOLLOWED -> R.drawable.ic_favorite_outline
                    null -> throw IllegalStateException("Oops! No ownership status here.")
                }
            )

            setStatus(item)
            setAlerts(item)
        }

        private fun setStatus(item: UIDevice) {
            with(binding.lastSeen) {
                text = item.lastWeatherStationActivity?.getRelativeFormattedTime(
                    fallbackIfTooSoon = context.getString(R.string.just_now)
                )
            }

            binding.statusIcon.setImageResource(
                if (item.profile == DeviceProfile.Helium) {
                    R.drawable.ic_helium
                } else {
                    R.drawable.ic_wifi
                }
            )
            binding.statusCard.setCardBackgroundColor(
                itemView.context.getColor(
                    when (item.isActive) {
                        true -> R.color.successTint
                        false -> R.color.errorTint
                        else -> R.color.midGrey
                    }
                )
            )
        }

        private fun setAlerts(item: UIDevice) {
            binding.alertsError.setVisible(false)
            binding.error.setVisible(false)
            binding.warning.setVisible(false)
            setCardStroke(R.color.transparent, 0)
            if (item.alerts.size > 1) {
                setCardStroke(R.color.error, 1)
                binding.alertsError.title(
                    itemView.context.getString(R.string.issues, item.alerts.size.toString())
                ).action {
                    deviceListener.onAlertsClicked(item)
                }.setVisible(true)
            } else if (item.alerts.contains(DeviceAlert.OFFLINE)) {
                setCardStroke(R.color.error, 1)
                binding.error.setVisible(true)
            } else if (item.alerts.contains(DeviceAlert.NEEDS_UPDATE)) {
                setCardStroke(R.color.warning, 1)
                binding.warning.action(resHelper.getString(R.string.update_station_now)) {
                    deviceListener.onUpdateStationClicked(item)
                }.setVisible(true)
                analytics.trackEventPrompt(
                    Analytics.ParamValue.OTA_AVAILABLE.paramValue,
                    Analytics.ParamValue.WARN.paramValue,
                    Analytics.ParamValue.VIEW.paramValue
                )
            }
        }

        private fun setCardStroke(@ColorRes colorResId: Int, width: Int) {
            binding.root.strokeColor = itemView.context.getColor(colorResId)
            binding.root.strokeWidth = width
        }

        private fun setWeatherData(item: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.currentWeather?.icon))
            binding.temperature.text = Weather.getFormattedTemperature(
                item.currentWeather?.temperature, 1, includeUnit = false
            )
            binding.temperatureUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
            )
            binding.feelsLike.text = Weather.getFormattedTemperature(
                item.currentWeather?.feelsLike, 1, includeUnit = false
            )
            binding.feelsLikeUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
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
                resHelper.getString(CacheService.KEY_WIND),
                resHelper.getString(R.string.wind_speed_ms)
            )
            val windDirectionUnit = item.currentWeather?.windDirection?.let {
                Weather.getFormattedWindDirection(it)
            } ?: ""
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
                oldItem.alerts == newItem.alerts &&
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
}
