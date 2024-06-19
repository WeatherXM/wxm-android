package com.weatherxm.ui.cellinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setBundleChip
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setStatusChip
import com.weatherxm.ui.common.visible
import com.weatherxm.util.Resources
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.getFormattedWindDirection
import com.weatherxm.util.Weather.getWindDirectionDrawable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CellDeviceListAdapter(
    private val cellDeviceListener: CellDeviceListener
) : ListAdapter<UIDevice, CellDeviceListAdapter.CellDeviceViewHolder>(CellDeviceDiffCallback()),
    KoinComponent {

    val resources: Resources by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellDeviceViewHolder {
        val binding = ListItemDeviceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return CellDeviceViewHolder(binding, cellDeviceListener)
    }

    override fun onBindViewHolder(holder: CellDeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CellDeviceViewHolder(
        private val binding: ListItemDeviceBinding,
        private val listener: CellDeviceListener
    ) :
        RecyclerView.ViewHolder(binding.root) {

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

            binding.name.text = item.name

            if (item.currentWeather == null || item.currentWeather.isEmpty()) {
                binding.weatherDataLayout.visible(false)
                binding.noDataLayout.visible(true)
                binding.noDataMessage.text =
                    resources.getString(R.string.no_data_message_public_device)
            } else {
                setWeatherData(item)
            }

            binding.address.text = if (item.address.isNullOrEmpty()) {
                resources.getString(R.string.unknown_address)
            } else {
                item.address
            }

            binding.status.setStatusChip(item)
            binding.bundle.setBundleChip(item)
        }

        private fun setWeatherData(device: UIDevice) {
            binding.icon.setAnimation(Weather.getWeatherAnimation(device.currentWeather?.icon))
            binding.temperature.text = Weather.getFormattedTemperature(
                device.currentWeather?.temperature, 1, includeUnit = false
            )
            binding.temperatureUnit.text = Weather.getPreferredUnit(
                resources.getString(CacheService.KEY_TEMPERATURE),
                resources.getString(R.string.temperature_celsius)
            )
            binding.feelsLike.text = Weather.getFormattedTemperature(
                device.currentWeather?.feelsLike, 1, includeUnit = false
            )
            binding.feelsLikeUnit.text = Weather.getPreferredUnit(
                resources.getString(CacheService.KEY_TEMPERATURE),
                resources.getString(R.string.temperature_celsius)
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
                oldItem.relation == newItem.relation &&
                oldItem.address == newItem.address &&
                oldItem.isActive == newItem.isActive
        }
    }
}

interface CellDeviceListener {
    fun onDeviceClicked(device: UIDevice)
    fun onFollowBtnClicked(device: UIDevice)
}
