package com.weatherxm.ui.publicdeviceslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemDeviceBinding
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.publicdeviceslist.PublicDevicesListAdapter.PublicDeviceViewHolder
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.Weather
import com.weatherxm.util.setTextAndColor

class PublicDevicesListAdapter(
    private val publicDeviceListener: (UIDevice) -> Unit
) : ListAdapter<UIDevice, PublicDeviceViewHolder>(PublicDeviceDiffCallback()) {

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
            binding.icon.setAnimation(Weather.getWeatherAnimation(item.currentWeather?.icon))
            binding.temperature.text =
                Weather.getFormattedTemperature(item.currentWeather?.temperature, 1)

            device.lastWeatherStationActivity?.let {
                with(binding.lastSeen) {
                    context.getString(
                        R.string.last_active,
                        it.getRelativeFormattedTime(
                            fallbackIfTooSoon = context.getString(R.string.last_active_just_now)
                        )
                    )
                }
            }

            // Hide them as we are not showing token-related data in public devices list for now
            binding.tokensLastDayTitle.visibility = View.INVISIBLE
            binding.tokensTotalTitle.visibility = View.INVISIBLE

            binding.statusChip.setTextAndColor(
                when (item.isActive) {
                    true -> R.string.online
                    false -> R.string.offline
                    null -> R.string.unknown
                },
                when (item.isActive) {
                    true -> R.color.device_status_online
                    false -> R.color.device_status_offline
                    null -> R.color.device_status_unknown
                }
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
