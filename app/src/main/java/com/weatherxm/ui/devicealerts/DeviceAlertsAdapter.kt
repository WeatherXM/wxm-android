package com.weatherxm.ui.devicealerts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemAlertBinding
import com.weatherxm.ui.common.ActionForMessageView
import com.weatherxm.ui.common.DataForMessageView
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.SubtitleForMessageView
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.components.compose.MessageCardView

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

        @Suppress("LongMethod")
        fun bind(item: DeviceAlert) {
            when (item.alert) {
                DeviceAlertType.OFFLINE -> {
                    val message = if (device?.isOwned() == true) {
                        R.string.station_inactive_alert_message
                    } else {
                        R.string.station_inactive_alert_message_public
                    }
                    binding.alert.setContent {
                        MessageCardView(
                            data = DataForMessageView(
                                title = R.string.inactive,
                                subtitle = SubtitleForMessageView(message = message),
                                drawable = R.drawable.ic_error_hex_filled,
                                action = if (device?.isOwned() == true) {
                                    ActionForMessageView(label = R.string.contact_support_title) {
                                        deviceAlertListener.onContactSupportClicked()
                                    }
                                } else null,
                                useStroke = true,
                                severityLevel = item.severity,
                            )
                        )
                    }
                }
                DeviceAlertType.LOW_BATTERY -> {
                    var title = R.string.low_battery
                    if (device?.isCellular() == true) {
                        title = R.string.low_ws_battery
                    }
                    binding.alert.setContent {
                        MessageCardView(
                            data = DataForMessageView(
                                title = title,
                                subtitle = SubtitleForMessageView(
                                    message = R.string.low_battery_desc
                                ),
                                drawable = R.drawable.ic_low_battery,
                                action = ActionForMessageView(label = R.string.read_more) {
                                    deviceAlertListener.onLowBatteryReadMoreClicked()
                                },
                                useStroke = true,
                                severityLevel = item.severity,
                            )
                        )
                    }
                }
                DeviceAlertType.LOW_GATEWAY_BATTERY -> {
                    binding.alert.setContent {
                        MessageCardView(
                            data = DataForMessageView(
                                title = R.string.low_gw_battery,
                                subtitle = SubtitleForMessageView(
                                    message = R.string.low_gw_battery_desc
                                ),
                                drawable = R.drawable.ic_low_battery,
                                action = ActionForMessageView(label = R.string.read_more) {
                                    deviceAlertListener.onLowBatteryReadMoreClicked()
                                },
                                useStroke = true,
                                severityLevel = item.severity,
                            )
                        )
                    }
                }
                DeviceAlertType.NEEDS_UPDATE -> {
                    binding.alert.setContent {
                        MessageCardView(
                            data = DataForMessageView(
                                title = R.string.updated_needed_title,
                                subtitle = SubtitleForMessageView(
                                    message = R.string.updated_needed_desc
                                ),
                                drawable = R.drawable.ic_update_alt,
                                action = ActionForMessageView(label = R.string.update_station_now) {
                                    deviceAlertListener.onUpdateStationClicked()
                                },
                                useStroke = true,
                                severityLevel = item.severity,
                            )
                        )
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
