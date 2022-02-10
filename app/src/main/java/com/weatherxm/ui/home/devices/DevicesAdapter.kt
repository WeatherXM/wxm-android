package com.weatherxm.ui.home.devices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.util.Weather
import com.weatherxm.util.getRelativeTimeFromISO
import com.weatherxm.util.setTextAndColor
import org.koin.core.component.KoinComponent

class DeviceAdapter(private val deviceListener: DeviceListener) :
    ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()), KoinComponent {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding =
            ListItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding, deviceListener)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
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
            binding.name.text = item.name
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.currentWeather?.icon))
            binding.temperature.text =
                Weather.getFormattedTemperature(item.currentWeather?.temperature)

            item.currentWeather?.timestamp?.let {
                binding.lastSeen.text = getRelativeTimeFromISO(it)
            }

            when {
                item.attributes?.isActive == null -> {
                    binding.statusChip.setTextAndColor(R.string.unknown, R.color.grey)
                }
                item.attributes.isActive -> {
                    binding.statusChip.setTextAndColor(R.string.online, R.color.green)
                }
                else -> {
                    binding.statusChip.setTextAndColor(R.string.offline, R.color.red)
                }
            }
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
                oldItem.attributes?.isActive == newItem.attributes?.isActive
        }
    }
}

interface DeviceListener {
    fun onDeviceClicked(device: Device)
}
