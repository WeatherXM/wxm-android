package com.weatherxm.ui.claimdevice.selectdevicetype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemSelectDeviceTypeBinding
import com.weatherxm.ui.claimdevice.AvailableDeviceType
import com.weatherxm.ui.claimdevice.selectdevicetype.AvailableDeviceTypesAdapter.AvailableDeviceTypeViewHolder
import com.weatherxm.ui.common.DeviceType

class AvailableDeviceTypesAdapter(
    private val listener: (DeviceType) -> Unit
) : ListAdapter<AvailableDeviceType, AvailableDeviceTypeViewHolder>(PublicDeviceDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AvailableDeviceTypeViewHolder {
        val binding =
            ListItemSelectDeviceTypeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return AvailableDeviceTypeViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: AvailableDeviceTypeViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class AvailableDeviceTypeViewHolder(
        private val binding: ListItemSelectDeviceTypeBinding,
        private val listener: (DeviceType) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val deviceType = it.tag as DeviceType
                listener(deviceType)
            }
        }

        fun bind(item: AvailableDeviceType, position: Int) {
            // Set the device type as tag in the root view
            itemView.tag = item.type

            binding.title.text = item.title
            binding.desc.text = item.desc

            if (item.type == DeviceType.HELIUM) {
                binding.typeIcon.setImageResource(R.drawable.ic_helium)
            } else {
                binding.typeIcon.setImageResource(R.drawable.ic_wifi)
            }

            if (position == currentList.size - 1) {
                binding.bottomBorder.visibility = View.INVISIBLE
            }
        }
    }

    class PublicDeviceDiffCallback : DiffUtil.ItemCallback<AvailableDeviceType>() {

        override fun areItemsTheSame(
            oldItem: AvailableDeviceType,
            newItem: AvailableDeviceType
        ): Boolean {
            return oldItem.title != newItem.title
        }

        override fun areContentsTheSame(
            oldItem: AvailableDeviceType,
            newItem: AvailableDeviceType
        ): Boolean {
            return oldItem.title == newItem.title &&
                oldItem.desc == newItem.desc &&
                oldItem.type == newItem.type
        }
    }
}
