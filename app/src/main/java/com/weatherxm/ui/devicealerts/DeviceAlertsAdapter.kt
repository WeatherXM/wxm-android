package com.weatherxm.ui.devicealerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.databinding.ListItemAlertBinding
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.UIDevice

class DeviceAlertsAdapter(
    private val deviceAlertListener: DeviceAlertListener,
    private val device: UIDevice?
) : ListAdapter<DeviceAlert, DeviceAlertsAdapter.DeviceAlertsViewHolder>(
    DeviceAlertsDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceAlertsViewHolder {
        val binding = ListItemAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceAlertsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceAlertsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceAlertsViewHolder(
        private val binding: ListItemAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeviceAlert) {
            if (item.severity == SeverityLevel.ERROR) {
                binding.alert.error(true)
            } else {
                binding.alert.warning(true)
            }

            when (item.alert) {
                DeviceAlertType.NEEDS_UPDATE -> {
                    binding.alert
                        .icon(R.drawable.ic_update_alt)
                        .title(R.string.updated_needed_title)
                        .message(R.string.updated_needed_desc)
                        .action(itemView.context.getString(R.string.update_station_now)) {
                            deviceAlertListener.onUpdateStationClicked()
                        }
                }
                DeviceAlertType.OFFLINE -> {
                    binding.alert.title(R.string.inactive)
                    if (device?.isOwned() == true) {
                        binding.alert.message(R.string.station_inactive_alert_message)
                            .action(itemView.context.getString(R.string.contact_support_title)) {
                                deviceAlertListener.onContactSupportClicked()
                            }
                    } else {
                        binding.alert.message(R.string.station_inactive_alert_message_public)
                    }
                }
                DeviceAlertType.LOW_BATTERY -> {
                    binding.alert
                        .icon(R.drawable.ic_low_battery)
                        .title(R.string.low_battery)
                        .message(R.string.low_battery_desc)
                        .action(itemView.context.getString(R.string.read_more)) {
                            deviceAlertListener.onLowBatteryReadMoreClicked()
                        }
                }
                DeviceAlertType.LOW_STATION_RSSI -> {
                    // TODO: Currently do nothing. Maybe we'll handle it in the future. 
                }
            }
        }
    }

    class DeviceAlertsDiffCallback : DiffUtil.ItemCallback<DeviceAlert>() {

        override fun areItemsTheSame(oldItem: DeviceAlert, newItem: DeviceAlert): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: DeviceAlert, newItem: DeviceAlert): Boolean {
            return oldItem == newItem
        }
    }
}

interface DeviceAlertListener {
    fun onUpdateStationClicked()
    fun onContactSupportClicked()
    fun onLowBatteryReadMoreClicked()
}
