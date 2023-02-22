package com.weatherxm.ui.home.devices

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.ui.common.UserDevice
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
import com.weatherxm.util.setColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceAdapter(private val deviceListener: DeviceListener) :
    ListAdapter<UserDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()), KoinComponent {

    val resHelper: ResourcesHelper by inject()

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

        private lateinit var userDevice: UserDevice

        init {
            binding.root.setOnClickListener {
                listener.onDeviceClicked(userDevice)
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: UserDevice) {
            this.userDevice = item
            binding.name.text = item.device.getNameOrLabel()

            if (item.device.currentWeather == null || item.device.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visibility = GONE
                binding.noDataLayout.visibility = VISIBLE
            } else {
                setWeatherData(item)
            }

            binding.address.text = if (item.device.address.isNullOrEmpty()) {
                resHelper.getString(R.string.unknown_address)
            } else {
                item.device.address
            }

            setStatus(item)
            setAlerts(item)
        }

        private fun setStatus(item: UserDevice) {
            with(binding.lastSeen) {
                text = item.device.attributes?.lastWeatherStationActivity?.getRelativeFormattedTime(
                    fallbackIfTooSoon = context.getString(R.string.just_now)
                )
            }

            binding.statusIcon.setImageResource(
                if (item.device.profile == DeviceProfile.Helium) {
                    R.drawable.ic_helium
                } else {
                    R.drawable.ic_wifi
                }
            )

            binding.status.setCardBackgroundColor(
                itemView.context.getColor(
                    when (item.device.attributes?.isActive) {
                        true -> {
                            binding.statusIcon.setColor(R.color.success)
                            R.color.successTint
                        }
                        false -> {
                            binding.statusIcon.setColor(R.color.error)
                            R.color.errorTint
                        }
                        null -> {
                            R.color.midGrey
                        }
                    }
                )
            )
        }

        private fun setAlerts(item: UserDevice) {
            binding.error.visibility =
                if (item.device.attributes?.isActive == false) VISIBLE else GONE

            if (deviceNeedsOTAPrompt(item)) {
                binding.warning.action(
                    resHelper.getString(R.string.update_station_now), actionWithBorders = true
                ) {
                    deviceListener.onUpdateStationClicked(item)
                }.show()
            } else {
                binding.warning.hide()
            }

            with(binding.root) {
                if (item.device.attributes?.isActive == false) {
                    strokeColor = itemView.context.getColor(R.color.error)
                    strokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_stroke)
                } else if (deviceNeedsOTAPrompt(item)) {
                    strokeColor = itemView.context.getColor(R.color.warning)
                    strokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_stroke)
                } else {
                    strokeColor = itemView.context.getColor(R.color.transparent)
                    strokeWidth = 0
                }
            }
        }

        private fun deviceNeedsOTAPrompt(userDevice: UserDevice): Boolean {
            return userDevice.shouldShowOTAPrompt &&
                userDevice.device.attributes?.isActive == true &&
                userDevice.device.profile == DeviceProfile.Helium &&
                userDevice.device.needsUpdate()
        }

        private fun setWeatherData(item: UserDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.device.currentWeather?.icon))
            binding.temperature.text = Weather.getFormattedTemperature(
                item.device.currentWeather?.temperature, 1, includeUnit = false
            )
            binding.temperatureUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
            )
            binding.feelsLike.text = Weather.getFormattedTemperature(
                item.device.currentWeather?.feelsLike, 1, includeUnit = false
            )
            binding.feelsLikeUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
            )
            binding.humidity.setData(
                Weather.getFormattedHumidity(
                    item.device.currentWeather?.humidity, includeUnit = false
                ), "%"
            )
            val windValue = Weather.getFormattedWind(
                item.device.currentWeather?.windSpeed,
                item.device.currentWeather?.windDirection,
                includeUnits = false
            )
            val windUnit = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_WIND),
                resHelper.getString(R.string.wind_speed_ms)
            )
            val windDirectionUnit = item.device.currentWeather?.windDirection?.let {
                Weather.getFormattedWindDirection(it)
            } ?: ""
            binding.wind.setData(windValue, "$windUnit $windDirectionUnit")

            binding.rain.setData(
                Weather.getFormattedPrecipitation(
                    item.device.currentWeather?.precipitation, includeUnit = false
                ), Weather.getPrecipitationPreferredUnit()
            )
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<UserDevice>() {

        override fun areItemsTheSame(oldItem: UserDevice, newItem: UserDevice): Boolean {
            return oldItem.device.id == newItem.device.id
        }

        @Suppress("MaxLineLength", "CyclomaticComplexMethod")
        override fun areContentsTheSame(oldItem: UserDevice, newItem: UserDevice): Boolean {
            val oldDevice = oldItem.device
            val newDevice = newItem.device
            return oldDevice.name == newDevice.name &&
                oldDevice.currentWeather?.icon == newDevice.currentWeather?.icon &&
                oldDevice.currentWeather?.temperature == newDevice.currentWeather?.temperature &&
                oldDevice.currentWeather?.humidity == newDevice.currentWeather?.humidity &&
                oldDevice.currentWeather?.precipitation == newDevice.currentWeather?.precipitation &&
                oldDevice.currentWeather?.windSpeed == newDevice.currentWeather?.windSpeed &&
                oldDevice.currentWeather?.windDirection == newDevice.currentWeather?.windDirection &&
                oldDevice.currentWeather?.feelsLike == newDevice.currentWeather?.feelsLike &&
                oldDevice.currentWeather?.timestamp == newDevice.currentWeather?.timestamp &&
                oldDevice.profile == newDevice.profile &&
                oldDevice.needsUpdate() == newDevice.needsUpdate() &&
                oldItem.shouldShowOTAPrompt == newItem.shouldShowOTAPrompt &&
                oldDevice.attributes?.firmware?.current == newDevice.attributes?.firmware?.current &&
                oldDevice.attributes?.firmware?.assigned == newDevice.attributes?.firmware?.assigned &&
                oldDevice.attributes?.friendlyName == newDevice.attributes?.friendlyName &&
                oldDevice.attributes?.isActive == newDevice.attributes?.isActive
        }
    }
}

interface DeviceListener {
    fun onDeviceClicked(userDevice: UserDevice)
    fun onUpdateStationClicked(userDevice: UserDevice)
}
