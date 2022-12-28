package com.weatherxm.ui.claimdevice.helium.pair

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemScannedDeviceBinding
import com.weatherxm.ui.claimdevice.helium.pair.ScannedDevicesListAdapter.ScannedDeviceViewHolder
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.ScannedDevice

class ScannedDevicesListAdapter(
    private val scannedDeviceListener: (ScannedDevice) -> Unit
) : ListAdapter<ScannedDevice, ScannedDeviceViewHolder>(PublicDeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedDeviceViewHolder {
        val binding =
            ListItemScannedDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScannedDeviceViewHolder(binding, scannedDeviceListener)
    }

    override fun onBindViewHolder(holder: ScannedDeviceViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ScannedDeviceViewHolder(
        private val binding: ListItemScannedDeviceBinding,
        private val listener: (ScannedDevice) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var scannedDevice: ScannedDevice

        init {
            binding.root.setOnClickListener {
                listener(scannedDevice)
            }
        }

        fun bind(item: ScannedDevice, position: Int) {
            this.scannedDevice = item
            binding.name.text = item.name
            if (item.type == DeviceType.HELIUM) {
                binding.typeIcon.setImageResource(R.drawable.ic_helium)
            } else {
                binding.typeIcon.setImageResource(R.drawable.ic_wifi)
            }

            if (position == currentList.size - 1) {
                binding.bottomBorder.visibility = View.INVISIBLE
            } else {
                binding.bottomBorder.visibility = View.VISIBLE
            }
        }
    }

    class PublicDeviceDiffCallback : DiffUtil.ItemCallback<ScannedDevice>() {

        override fun areItemsTheSame(oldItem: ScannedDevice, newItem: ScannedDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: ScannedDevice, newItem: ScannedDevice): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.address == newItem.address
        }
    }
}
