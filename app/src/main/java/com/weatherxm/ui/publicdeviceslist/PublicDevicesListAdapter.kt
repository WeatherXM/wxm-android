package com.weatherxm.ui.publicdeviceslist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ListItemPublicDeviceBinding
import com.weatherxm.util.DateTimeHelper.getRelativeTimeFromISO
import com.weatherxm.util.setTextAndColor
import org.koin.core.component.KoinComponent

class PublicDevicesListAdapter(
    private val publicDeviceListener: (Device) -> Unit
) : ListAdapter<Device,
    PublicDevicesListAdapter.PublicDeviceViewHolder>(PublicDeviceDiffCallback()),
    KoinComponent {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicDeviceViewHolder {
        val binding = ListItemPublicDeviceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PublicDeviceViewHolder(binding, publicDeviceListener)
    }

    override fun onBindViewHolder(holder: PublicDeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PublicDeviceViewHolder(
        private val binding: ListItemPublicDeviceBinding,
        private val listener: (Device) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var device: Device

        init {
            binding.root.setOnClickListener {
                listener(device)
            }
        }

        fun bind(item: Device) {
            this.device = item
            binding.name.text = item.name
            device.attributes?.lastActiveAt?.let {
                binding.lastSeen.text = itemView.resources.getString(
                    R.string.last_active,
                    getRelativeTimeFromISO(
                        it,
                        itemView.resources.getString(R.string.last_active_just_now)
                    )
                )
            }
            when (item.attributes?.isActive) {
                true -> binding.statusChip.setTextAndColor(R.string.online, R.color.green)
                false -> binding.statusChip.setTextAndColor(R.string.offline, R.color.red)
                null -> binding.statusChip.setTextAndColor(R.string.unknown, R.color.grey)
            }
        }
    }

    class PublicDeviceDiffCallback : DiffUtil.ItemCallback<Device>() {

        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.attributes?.isActive == newItem.attributes?.isActive
        }
    }
}
