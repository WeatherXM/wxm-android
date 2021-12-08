package com.weatherxm.ui.home.devices

import android.text.format.DateUtils.getRelativeTimeSpanString
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Weather
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceAdapter(private val deviceListener: DeviceListener) :
    ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()), KoinComponent {
    private val resHelper: ResourcesHelper by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding =
            ListItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding, resHelper, deviceListener)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ListItemDeviceBinding,
        private val resHelper: ResourcesHelper,
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
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.timeseries?.hourlyIcon))
            binding.temperature.text =
                Weather.getFormattedTemperature(item.timeseries?.hourlyTemperature)

            item.timeseries?.timestamp?.let {
                binding.lastSeen.text = getRelativeTimeSpanString(it, System.currentTimeMillis(), 0)
            }

            when {
                item.attributes?.isActive == null -> {
                    setStatusChip(
                        resHelper.getString(R.string.unknown),
                        R.color.grey
                    )
                }
                item.attributes.isActive -> {
                    setStatusChip(
                        resHelper.getString(R.string.online),
                        R.color.green
                    )
                }
                else -> {
                    setStatusChip(
                        resHelper.getString(R.string.offline),
                        R.color.red
                    )
                }
            }
        }

        private fun setStatusChip(text: String, color: Int) {
            binding.statusChip.setChipBackgroundColorResource(color)
            binding.statusChip.text = text
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {

        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.timeseries?.hourlyIcon == newItem.timeseries?.hourlyIcon &&
                oldItem.timeseries?.hourlyTemperature == newItem.timeseries?.hourlyTemperature &&
                oldItem.timeseries?.timestamp == newItem.timeseries?.timestamp &&
                oldItem.attributes?.isActive == newItem.attributes?.isActive
        }
    }
}

interface DeviceListener {
    fun onDeviceClicked(device: Device)
}
