package com.weatherxm.ui.cellinfo

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
import com.weatherxm.ui.common.DeviceOwnershipStatus
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CellDeviceListAdapter(
    private val listener: (UIDevice) -> Unit
) : ListAdapter<UIDevice, CellDeviceListAdapter.CellDeviceViewHolder>(CellDeviceDiffCallback()),
    KoinComponent {

    val resHelper: ResourcesHelper by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellDeviceViewHolder {
        val binding = ListItemDeviceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return CellDeviceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: CellDeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CellDeviceViewHolder(
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

            @Suppress("UseCheckOrError")
            binding.stationFollowHomeIcon.setImageResource(
                when (item.ownershipStatus) {
                    DeviceOwnershipStatus.OWNED -> R.drawable.ic_home
                    DeviceOwnershipStatus.FOLLOWED -> R.drawable.ic_favorite
                    DeviceOwnershipStatus.UNFOLLOWED -> R.drawable.ic_favorite_outline
                    null -> throw IllegalStateException("Oops! No ownership status here.")
                }
            )

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visibility = View.GONE
                binding.noDataLayout.visibility = View.VISIBLE
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

            binding.statusCard.setCardBackgroundColor(
                itemView.context.getColor(
                    when (item.isActive) {
                        true -> R.color.successTint
                        false -> R.color.errorTint
                        null -> R.color.midGrey
                    }
                )
            )
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
                Weather.getPrecipitationPreferredUnit()
            )
        }
    }

    class CellDeviceDiffCallback : DiffUtil.ItemCallback<UIDevice>() {

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
