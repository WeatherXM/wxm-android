package com.weatherxm.ui.devicesettings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemDeviceInfoBinding
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.visible
import com.weatherxm.util.Resources
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceInfoItemAdapter(
    private val actionListener: ((DeviceAlert) -> Unit)?
) : ListAdapter<UIDeviceInfoItem,
    DeviceInfoItemAdapter.DeviceInfoViewHolder>(DeviceInfoDiffCallback()),
    KoinComponent {

    val resources: Resources by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceInfoViewHolder {
        val binding = ListItemDeviceInfoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceInfoViewHolder(binding, actionListener)
    }

    override fun onBindViewHolder(holder: DeviceInfoViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class DeviceInfoViewHolder(
        private val binding: ListItemDeviceInfoBinding,
        private val listener: ((DeviceAlert) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UIDeviceInfoItem, position: Int) {
            if (position == itemCount - 1) {
                binding.bottomBorder.visible(false)
            }

            binding.title.text = item.title
            binding.value.text = item.value

            item.deviceAlert?.let { deviceAlert ->
                if (deviceAlert.alert == DeviceAlertType.NEEDS_UPDATE) {
                    with(binding.actionBtn) {
                        text = resources.getString(R.string.action_update_firmware)
                        icon = AppCompatResources.getDrawable(context, R.drawable.ic_update)
                        setOnClickListener { listener?.invoke(deviceAlert) }
                        visible(true)
                    }
                } else {
                    binding.infoBox.setContent {
                        DeviceInfoItemAlertView(deviceAlert) {
                            listener?.invoke(deviceAlert)
                        }
                    }
                    binding.infoBox.visible(true)
                }
            }
        }
    }

    class DeviceInfoDiffCallback : DiffUtil.ItemCallback<UIDeviceInfoItem>() {

        override fun areItemsTheSame(
            oldItem: UIDeviceInfoItem,
            newItem: UIDeviceInfoItem
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: UIDeviceInfoItem,
            newItem: UIDeviceInfoItem
        ): Boolean {
            return oldItem.title == newItem.title &&
                oldItem.value == newItem.value &&
                oldItem.deviceAlert == newItem.deviceAlert
        }
    }
}
