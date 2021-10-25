package com.weatherxm.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.data.Device
import com.weatherxm.databinding.ListItemDeviceBinding

class DeviceAdapter(private val listener: OnDeviceSelectedListener) :
    ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding =
            ListItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ListItemDeviceBinding,
        listener: OnDeviceSelectedListener,
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var field: Device

        init {
            binding.root.setOnClickListener {
                listener.onDeviceSelected(field)
            }
        }

        fun bind(item: Device) {
            this.field = item
            binding.name.text = item.name
            //binding.crop.text = item.currentCrop?.name ?: "No Crop Information"
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {

        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}
