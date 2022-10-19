package com.weatherxm.ui.claimdevice.location

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.databinding.ListItemLocationBinding
import com.weatherxm.ui.claimdevice.location.SearchResultsAdapter.LocationSearchResultViewHolder
import com.weatherxm.util.MapboxUtils

class SearchResultsAdapter(
    private val listener: (SearchSuggestion) -> Unit
) : ListAdapter<SearchSuggestion, LocationSearchResultViewHolder>(
    LocationSearchResultDiffCallback()
) {
    private var query: String = ""

    fun updateData(newQuery: String, newData: List<SearchSuggestion> = mutableListOf()) {
        query = newQuery
        submitList(newData)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LocationSearchResultViewHolder {
        val binding = ListItemLocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationSearchResultViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: LocationSearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LocationSearchResultViewHolder(
        private val binding: ListItemLocationBinding,
        private val listener: (SearchSuggestion) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.location.setOnClickListener {
                listener(getItem(absoluteAdapterPosition))
            }
        }

        fun bind(item: SearchSuggestion) {
            val suggestion = MapboxUtils.parseSearchSuggestion(item)
            val formattedSuggestion = SpannableStringBuilder(suggestion)
            val boldToStart = suggestion.indexOf(query, ignoreCase = true)
            if (boldToStart < 0) {
                binding.location.text = suggestion
            } else {
                formattedSuggestion.setSpan(
                    StyleSpan(Typeface.BOLD),
                    boldToStart,
                    boldToStart.plus(query.length),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.location.text = formattedSuggestion
            }
        }
    }

    class LocationSearchResultDiffCallback : DiffUtil.ItemCallback<SearchSuggestion>() {

        override fun areItemsTheSame(
            oldItem: SearchSuggestion,
            newItem: SearchSuggestion
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: SearchSuggestion,
            newItem: SearchSuggestion
        ): Boolean {
            val oldAddress = oldItem.address
            val newAddress = newItem.address
            return listOf(
                oldAddress?.street == newAddress?.street,
                oldAddress?.country == newAddress?.country,
                oldAddress?.place == newAddress?.place,
                oldAddress?.region == newAddress?.region,
                oldAddress?.postcode == newAddress?.postcode
            ).all { it }
        }
    }
}
