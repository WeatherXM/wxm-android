package com.weatherxm.ui.networkstats

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.databinding.ListItemNetworkStationStatsBinding
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NetworkStationStatsAdapter(
    private val onStationClicked: (NetworkStationStats) -> Unit
) : ListAdapter<NetworkStationStats,
    NetworkStationStatsAdapter.StationInfoViewHolder>(StationInfoDiffCallback()),
    KoinComponent {

    val resHelper: ResourcesHelper by inject()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationInfoViewHolder {
        val binding = ListItemNetworkStationStatsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StationInfoViewHolder(binding, onStationClicked)
    }

    override fun onBindViewHolder(holder: StationInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StationInfoViewHolder(
        private val binding: ListItemNetworkStationStatsBinding,
        private val listener: (NetworkStationStats) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @Suppress("MagicNumber")
        @SuppressLint("SetTextI18n")
        fun bind(item: NetworkStationStats) {
            binding.root.setOnClickListener {
                listener.invoke(item)
            }

            binding.stationName.text = item.name
            binding.stationAmount.text = item.amount

            val percentage = item.percentage.coerceIn(0.0, 100.0)
            binding.stationPercentage.text = "${percentage.toInt()}%"
            binding.stationSlider.values = listOf(percentage.toFloat())
        }
    }

    class StationInfoDiffCallback : DiffUtil.ItemCallback<NetworkStationStats>() {

        override fun areItemsTheSame(
            oldItem: NetworkStationStats,
            newItem: NetworkStationStats
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: NetworkStationStats,
            newItem: NetworkStationStats
        ): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.amount == newItem.amount &&
                oldItem.percentage == newItem.percentage
        }
    }
}
