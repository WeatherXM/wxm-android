package com.weatherxm.ui.home.explorer.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.databinding.ListItemNetworkSearchResultBinding
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.highlightText
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.home.explorer.SearchResult

class NetworkSearchResultsListAdapter(
    private val listener: (SearchResult) -> Unit
) : ListAdapter<SearchResult, NetworkSearchResultsListAdapter.NetworkSearchResultViewHolder>(
    NetworkSearchResultDiffCallback()
) {
    private var query: String = String.empty()

    fun updateData(newQuery: String, newData: List<SearchResult> = mutableListOf()) {
        /*
        * Clear the list before updating to let the "highlighting" work in case of a searched result
        * is already in the recents list
         */
        submitList(null)
        query = newQuery
        submitList(newData)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NetworkSearchResultViewHolder {
        val binding =
            ListItemNetworkSearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        return NetworkSearchResultViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: NetworkSearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NetworkSearchResultViewHolder(
        private val binding: ListItemNetworkSearchResultBinding,
        private val listener: (SearchResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchResult) {
            binding.root.setOnClickListener {
                listener(item)
            }

            when (item.stationBundle?.connectivity) {
                "helium" -> binding.typeIcon.setImageResource(R.drawable.ic_helium)
                "wifi" -> binding.typeIcon.setImageResource(R.drawable.ic_wifi)
                "cellular" -> binding.typeIcon.setImageResource(R.drawable.ic_cellular)
                null -> binding.typeIcon.setImageResource(R.drawable.ic_address_marker)
            }

            val resultName = item.name ?: String.empty()
            if (query.isEmpty()) {
                binding.resultName.text = resultName
                binding.resultName.setTextColor(itemView.context.getColor(R.color.colorOnSurface))
            } else {
                var currentQueryToCheck = query
                while (currentQueryToCheck.isNotEmpty()) {
                    val queryStartIndex = resultName.indexOf(currentQueryToCheck, ignoreCase = true)
                    if (queryStartIndex >= 0) {
                        binding.resultName.highlightText(
                            resultName,
                            queryStartIndex,
                            queryStartIndex.plus(currentQueryToCheck.length),
                            R.color.darkGrey,
                            R.color.colorOnSurface
                        )
                        break
                    }
                    currentQueryToCheck = currentQueryToCheck.dropLast(1)
                }
                if (currentQueryToCheck.isEmpty()) {
                    binding.resultName.text = resultName
                    binding.resultName.setTextColor(itemView.context.getColor(R.color.darkGrey))
                }
            }

            item.addressPlace?.let {
                binding.resultDesc.text = it
            }
            binding.resultDesc.visible(!item.addressPlace.isNullOrEmpty())
        }
    }

    class NetworkSearchResultDiffCallback : DiffUtil.ItemCallback<SearchResult>() {

        override fun areItemsTheSame(
            oldItem: SearchResult,
            newItem: SearchResult
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: SearchResult,
            newItem: SearchResult
        ): Boolean {
            return oldItem.name == newItem.name &&
                oldItem.center == newItem.center &&
                oldItem.addressPlace == newItem.addressPlace &&
                oldItem.stationBundle == newItem.stationBundle &&
                oldItem.stationId == newItem.stationId &&
                oldItem.stationCellIndex == newItem.stationCellIndex
        }
    }
}
