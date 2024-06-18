package com.weatherxm.ui.devicesettings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemDeviceInfoBinding
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Resources
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceInfoAdapter(
    private val actionListener: (ActionType) -> Unit
) : ListAdapter<UIDeviceInfo,
    DeviceInfoAdapter.DeviceInfoViewHolder>(DeviceInfoDiffCallback()),
    KoinComponent {

    val resources: Resources by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceInfoViewHolder {
        val binding = ListItemDeviceInfoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceInfoViewHolder(binding, actionListener)
    }

    override fun onBindViewHolder(holder: DeviceInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceInfoViewHolder(
        private val binding: ListItemDeviceInfoBinding,
        private val listener: (ActionType) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UIDeviceInfo) {
            binding.title.text = item.title
            binding.value.text = item.value

            item.action?.let { action ->
                with(binding.actionBtn) {
                    text = action.actionText
                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_update)
                    setOnClickListener {
                        listener.invoke(action.actionType)
                    }
                    setVisible(true)
                }
            }

            item.warning?.let { warning ->
                with(binding.warningBox) {
                    htmlMessage(warning)
                    setVisible(true)
                }
            }
        }
    }

    class DeviceInfoDiffCallback : DiffUtil.ItemCallback<UIDeviceInfo>() {

        override fun areItemsTheSame(oldItem: UIDeviceInfo, newItem: UIDeviceInfo): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: UIDeviceInfo, newItem: UIDeviceInfo): Boolean {
            return oldItem.title == newItem.title &&
                oldItem.value == newItem.value &&
                oldItem.action?.actionText == newItem.action?.actionText &&
                oldItem.action?.actionType == newItem.action?.actionType
        }
    }
}
