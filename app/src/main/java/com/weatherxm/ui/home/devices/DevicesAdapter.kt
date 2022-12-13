package com.weatherxm.ui.home.devices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Tokens.formatTokens
import com.weatherxm.util.Weather
import com.weatherxm.util.setTextAndColor
import com.weatherxm.util.showIntegratedWarning
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

        fun bind(item: Device) {
            this.device = item
            binding.name.text = item.getNameOrLabel()
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.currentWeather?.icon))
            binding.temperature.text =
                Weather.getFormattedTemperature(item.currentWeather?.temperature, 1)

            device.attributes?.lastWeatherStationActivity?.let {
                with(binding.lastSeen) {
                    text = context.getString(
                        R.string.last_active,
                        it.getRelativeFormattedTime(
                            fallbackIfTooSoon = context.getString(R.string.last_active_just_now)
                        )
                    )
                }
            }

            device.rewards?.totalRewards?.let {
                val total = resHelper.getString(R.string.wxm_amount, formatTokens(it))
                binding.tokensTotal.text = total
            }

            device.rewards?.actualReward?.let {
                val lastReward = resHelper.getString(R.string.wxm_amount, formatTokens(it))
                binding.tokensLastDay.text = lastReward
            }

            binding.statusChip.setTextAndColor(
                when (item.attributes?.isActive) {
                    true -> R.string.online
                    false -> R.string.offline
                    null -> R.string.unknown
                },
                when (item.attributes?.isActive) {
                    true -> R.color.device_status_online
                    false -> R.color.device_status_offline
                    null -> R.color.device_status_unknown
                }
            )

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
