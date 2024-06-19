package com.weatherxm.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.databinding.ViewAddressSearchBinding
import com.weatherxm.ui.common.SearchResultsAdapter
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.visible

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
        binding.recycler.visible(false)
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
        binding.recycler.visible(true)
    }

    fun clear() {
        adapter.updateData(String.empty(), mutableListOf())
        binding.recycler.visible(false)
    }
}
