package com.weatherxm.ui.home.devices

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceAdapter(private val deviceListener: DeviceListener) :
    ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()), KoinComponent {

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
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var device: Device

        init {
            binding.root.setOnClickListener {
                listener.onDeviceClicked(device)
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: Device) {
            this.device = item
            binding.name.text = item.getNameOrLabel()

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visibility = GONE
                binding.noDataLayout.visibility = VISIBLE
                binding.noDataMessage.text =
                    resHelper.getString(R.string.no_data_message_public_device)
            } else {
                setWeatherData(item)
            }

            binding.address.text = if (item.address.isNullOrEmpty()) {
                resHelper.getString(R.string.unknown_address)
            } else {
                item.address
            }

            with(binding.lastSeen) {
                text = item.attributes?.lastWeatherStationActivity?.getRelativeFormattedTime(
                    fallbackIfTooSoon = context.getString(R.string.just_now)
                )
            }

            binding.statusIcon.setImageResource(
                if (device.profile == DeviceProfile.Helium) {
                    R.drawable.ic_helium
                } else {
                    R.drawable.ic_wifi
                }
            )

            binding.status.setCardBackgroundColor(
                itemView.context.getColor(
                    when (item.attributes?.isActive) {
                        true -> {
                            R.color.device_status_online
                        }
                        false -> {
                            R.color.device_status_offline
                        }
                        null -> {
                            R.color.device_status_unknown
                        }
                    }
                )
            )

            binding.error.visibility = if (item.attributes?.isActive == false) VISIBLE else GONE
            binding.root.setStrokeColor(
                ColorStateList.valueOf(
                    itemView.context.getColor(
                        when (item.attributes?.isActive) {
                            false -> {
                                R.color.error
                            }
                            else -> {
                                R.color.transparent
                            }
                        }
                    )
                )
            )

            binding.root.strokeWidth = if (item.attributes?.isActive == false) {
                itemView.resources.getDimensionPixelSize(R.dimen.card_stroke)
            } else 0

            // TODO: Check if OTA warning should be shown
//            if (false) {
//                binding.warningBox
//                    .action(
//                        resHelper.getString(R.string.update_station_now),
//                        actionWithBorders = true
//                    ) {
//                        deviceListener.onWarningActionClicked(item)
//                    }
//                    .hideCloseButton()
//                    .show()
//                binding.deviceCardWithWarning.showIntegratedWarning()
//            }
        }

        private fun setWeatherData(item: Device) {
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
                Weather.getFormattedHumidity(
                    item.currentWeather?.humidity, includeUnit = false
                ), "%"
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
                ), Weather.getPrecipitationPreferredUnit(true)
            )
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {

        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.currentWeather?.icon == newItem.currentWeather?.icon &&
                oldItem.currentWeather?.temperature == newItem.currentWeather?.temperature &&
                oldItem.currentWeather?.timestamp == newItem.currentWeather?.timestamp &&
                oldItem.rewards?.totalRewards == newItem.rewards?.totalRewards &&
                oldItem.rewards?.actualReward == newItem.rewards?.actualReward &&
                oldItem.attributes?.friendlyName == newItem.attributes?.friendlyName &&
                oldItem.attributes?.isActive == newItem.attributes?.isActive
        }
    }
}

interface DeviceListener {
    fun onDeviceClicked(device: Device)
    fun onWarningActionClicked(device: Device)
}
