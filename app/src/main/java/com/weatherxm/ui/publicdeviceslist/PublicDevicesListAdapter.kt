package com.weatherxm.ui.publicdeviceslist

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.publicdeviceslist.PublicDevicesListAdapter.PublicDeviceViewHolder
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
import com.weatherxm.util.setColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PublicDevicesListAdapter(
    private val publicDeviceListener: (UIDevice) -> Unit
) : ListAdapter<UIDevice, PublicDeviceViewHolder>(PublicDeviceDiffCallback()), KoinComponent {

    val resHelper: ResourcesHelper by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicDeviceViewHolder {
        val binding = ListItemDeviceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PublicDeviceViewHolder(binding, publicDeviceListener)
    }

    override fun onBindViewHolder(holder: PublicDeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PublicDeviceViewHolder(
        private val binding: ListItemDeviceBinding,
        private val listener: (UIDevice) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var device: UIDevice

        init {
            binding.root.setOnClickListener {
                listener(device)
            }
        }

        fun bind(item: UIDevice) {
            this.device = item
            binding.name.text = item.name

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visibility = View.GONE
                binding.noDataLayout.visibility = View.VISIBLE
            } else {
                setWeatherData(item)
            }

            binding.address.text = if (item.address.isNullOrEmpty()) {
                resHelper.getString(R.string.unknown_address)
            } else {
                item.address
            }

            with(binding.lastSeen) {
                text = item.lastWeatherStationActivity?.getRelativeFormattedTime(
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
                    when (item.isActive) {
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

            binding.error.visibility = if (item.isActive == false) View.VISIBLE else View.GONE
            binding.root.setStrokeColor(
                ColorStateList.valueOf(
                    itemView.context.getColor(
                        when (item.isActive) {
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

            binding.root.strokeWidth = if (item.isActive == false) {
                itemView.resources.getDimensionPixelSize(R.dimen.card_stroke)
            } else 0
        }

        private fun setWeatherData(device: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(device.currentWeather?.icon))
            binding.temperature.text = Weather.getFormattedTemperature(
                device.currentWeather?.temperature, 1, includeUnit = false
            )
            binding.temperatureUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
            )
            binding.feelsLike.text = Weather.getFormattedTemperature(
                device.currentWeather?.feelsLike, 1, includeUnit = false
            )
            binding.feelsLikeUnit.text = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_TEMPERATURE),
                resHelper.getString(R.string.temperature_celsius)
            )
            binding.humidity.setData(
                Weather.getFormattedHumidity(
                    device.currentWeather?.humidity, includeUnit = false
                ),
                "%"
            )
            val windValue = Weather.getFormattedWind(
                device.currentWeather?.windSpeed,
                device.currentWeather?.windDirection,
                includeUnits = false
            )
            val windUnit = Weather.getPreferredUnit(
                resHelper.getString(CacheService.KEY_WIND),
                resHelper.getString(R.string.wind_speed_ms)
            )
            val windDirectionUnit = device.currentWeather?.windDirection?.let {
                Weather.getFormattedWindDirection(it)
            } ?: ""
            binding.wind.setData(windValue, "$windUnit $windDirectionUnit")

            binding.rain.setData(
                Weather.getFormattedPrecipitation(
                    device.currentWeather?.precipitation, includeUnit = false
                ),
                Weather.getPrecipitationPreferredUnit(true)
            )
        }
    }

    class PublicDeviceDiffCallback : DiffUtil.ItemCallback<UIDevice>() {

        override fun areItemsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UIDevice, newItem: UIDevice): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.currentWeather?.icon == newItem.currentWeather?.icon &&
                oldItem.currentWeather?.temperature == newItem.currentWeather?.temperature &&
                oldItem.currentWeather?.timestamp == newItem.currentWeather?.timestamp &&
                oldItem.lastWeatherStationActivity == newItem.lastWeatherStationActivity &&
                oldItem.isActive == newItem.isActive
        }
    }
}
