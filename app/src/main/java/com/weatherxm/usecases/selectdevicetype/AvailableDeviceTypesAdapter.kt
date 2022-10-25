package com.weatherxm.usecases.selectdevicetype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemSelectDeviceTypeBinding
import com.weatherxm.ui.claimdevice.AvailableDeviceType
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AvailableDeviceTypesAdapter(
    private val deviceTypeListener: (AvailableDeviceType) -> Unit
) : ListAdapter<AvailableDeviceType,
        AvailableDeviceTypesAdapter.AvailableDeviceTypeViewHolder>(PublicDeviceDiffCallback()),
    KoinComponent {

    val resHelper: ResourcesHelper by inject()

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
        return AvailableDeviceTypeViewHolder(binding, deviceTypeListener)
    }

    override fun onBindViewHolder(holder: AvailableDeviceTypeViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class AvailableDeviceTypeViewHolder(
        private val binding: ListItemSelectDeviceTypeBinding,
        private val listener: (AvailableDeviceType) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var deviceType: AvailableDeviceType

        init {
            binding.root.setOnClickListener {
                listener(deviceType)
            }
        }

        fun bind(item: AvailableDeviceType, position: Int) {
            this.deviceType = item
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
