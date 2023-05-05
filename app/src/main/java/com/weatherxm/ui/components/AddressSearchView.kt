package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.databinding.ViewAddressSearchBinding
import com.weatherxm.ui.claimdevice.location.SearchResultsAdapter
import com.weatherxm.util.onTextChanged

class AddressSearchView : LinearLayout {

    private lateinit var binding: ViewAddressSearchBinding
    private lateinit var adapter: SearchResultsAdapter

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        binding = ViewAddressSearchBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setAdapter(
        adapter: SearchResultsAdapter,
        onTextChanged: (String) -> Unit,
        onMyLocationClicked: () -> Unit
    ) {
        this.adapter = adapter
        binding.recycler.adapter = adapter
        binding.recycler.visibility = GONE
        binding.searchBox.onTextChanged {
            if (it.isEmpty() || it.length <= 2) {
                clear()
            } else {
                onTextChanged.invoke(it)
            }
        }
        binding.myLocationButton.setOnClickListener {
            onMyLocationClicked.invoke()
        }
    }

    fun getQueryLength(): Int {
        return binding.searchBox.length()
    }

    fun setData(newData: List<SearchSuggestion>) {
        adapter.updateData(binding.searchBox.text.toString(), newData)
        binding.recycler.visibility = VISIBLE
    }

    fun clear() {
        adapter.updateData("", mutableListOf())
        binding.recycler.visibility = GONE
    }
}
