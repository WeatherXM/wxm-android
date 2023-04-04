package com.weatherxm.ui.stationsettings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemStationInfoBinding
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StationInfoAdapter(
    private val actionListener: (ActionType) -> Unit
) : ListAdapter<StationInfo,
    StationInfoAdapter.StationInfoViewHolder>(StationInfoDiffCallback()),
    KoinComponent {

    val resHelper: ResourcesHelper by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationInfoViewHolder {
        val binding = ListItemStationInfoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StationInfoViewHolder(binding, actionListener)
    }

    override fun onBindViewHolder(holder: StationInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StationInfoViewHolder(
        private val binding: ListItemStationInfoBinding,
        private val listener: (ActionType) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StationInfo) {
            binding.title.text = item.title
            binding.value.text = item.value

            item.action?.let { action ->
                with(binding.actionBtn) {
                    text = action.actionText
                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_update)
                    setOnClickListener {
                        listener.invoke(action.actionType)
                    }
                    visibility = View.VISIBLE
                }
            }

            item.warning?.let { warning ->
                with(binding.warningBox) {
                    htmlMessage(warning)
                    visibility = View.VISIBLE
                }
            }
        }
    }

    class StationInfoDiffCallback : DiffUtil.ItemCallback<StationInfo>() {

        override fun areItemsTheSame(oldItem: StationInfo, newItem: StationInfo): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: StationInfo, newItem: StationInfo): Boolean {
            return oldItem.title == newItem.title &&
                oldItem.value == newItem.value &&
                oldItem.action?.actionText == newItem.action?.actionText &&
                oldItem.action?.actionType == newItem.action?.actionType
        }
    }
}
