package com.weatherxm.ui.devicealerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemAlertBinding
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.setVisible

class DeviceAlertsAdapter(private val deviceAlertListener: DeviceAlertListener) :
    ListAdapter<DeviceAlert, DeviceAlertsAdapter.DailyDeviceAlertsViewHolder>(
        DeviceAlertsDiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyDeviceAlertsViewHolder {
        val binding = ListItemAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailyDeviceAlertsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyDeviceAlertsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyDeviceAlertsViewHolder(private val binding: ListItemAlertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeviceAlert) {
            if (item == DeviceAlert.NEEDS_UPDATE) {
                binding.warning
                    .title(R.string.updated_needed_title)
                    .message(R.string.updated_needed_desc)
                    .action(itemView.context.getString(R.string.update_station_now)) {
                        deviceAlertListener.onUpdateStationClicked()
                    }
                showWarningCard()
            } else if (item == DeviceAlert.OFFLINE) {
                binding.error
                    .title(R.string.station_offline)
                    .message(R.string.station_offline_alert_message)
                    .action(itemView.context.getString(R.string.title_contact_support)) {
                        deviceAlertListener.onContactSupportClicked()
                    }

                showErrorCard()
            }
        }

        private fun showWarningCard() {
            binding.error.setVisible(false)
            binding.warning.setVisible(true)
        }

        private fun showErrorCard() {
            binding.warning.setVisible(false)
            binding.error.setVisible(true)
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
}
