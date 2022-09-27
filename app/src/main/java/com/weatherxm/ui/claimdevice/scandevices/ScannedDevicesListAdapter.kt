package com.weatherxm.ui.claimdevice.scandevices

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemScannedDeviceBinding
import com.weatherxm.ui.ScannedDevice
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScannedDevicesListAdapter(
    private val scannedDeviceListener: (ScannedDevice) -> Unit
) : ListAdapter<ScannedDevice,
    ScannedDevicesListAdapter.ScannedDeviceViewHolder>(PublicDeviceDiffCallback()), KoinComponent {

    val resHelper: ResourcesHelper by inject()

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
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var scannedDevice: ScannedDevice

        init {
            binding.root.setOnClickListener {
                listener(scannedDevice)
            }
        }

        fun bind(item: ScannedDevice, position: Int) {
            this.scannedDevice = item
            binding.name.text = item.name
            binding.deviceId.text = resHelper.getString(R.string.device_id, item.address)

            if (position == currentList.size - 1) {
                binding.bottomBorder.visibility = View.INVISIBLE
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
