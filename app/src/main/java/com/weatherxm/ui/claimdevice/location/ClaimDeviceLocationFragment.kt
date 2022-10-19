package com.weatherxm.ui.claimdevice.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherxm.databinding.FragmentClaimDeviceSetLocationBinding
import com.weatherxm.ui.claimdevice.ClaimDeviceViewModel
import com.weatherxm.ui.common.toast
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.onTextChanged

class ClaimDeviceLocationFragment : Fragment() {
    private val model: ClaimDeviceViewModel by activityViewModels()
    private val locationModel: ClaimDeviceLocationViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimDeviceSetLocationBinding

    private lateinit var adapter: SearchResultsAdapter
    private lateinit var recyclerLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimDeviceSetLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        model.nextButtonStatus(true)

        binding.cancel.setOnClickListener {
            model.cancel()
        }

        binding.confirmAndClaim.setOnClickListener {
            locationModel.confirmLocation()
            model.nextButtonClick()
        }

        locationModel.onError().observe(viewLifecycleOwner) {
            context.toast(it.errorMessage)
        }

        locationModel.onClearSearchBox().observe(viewLifecycleOwner) {
            if (it) {
                adapter.updateData("", mutableListOf())
                binding.recycler.visibility = View.GONE
            }
        }

        locationModel.onSearchResults().observe(viewLifecycleOwner) {
            if (it.isNotEmpty() && binding.searchBox.length() > 2) {
                adapter.updateData(binding.searchBox.text.toString(), it)
                binding.recycler.visibility = View.VISIBLE
            } else {
                adapter.updateData("", mutableListOf())
                binding.recycler.visibility = View.GONE
            }
        }

        binding.searchBox.onTextChanged {
            if (it.isEmpty()) {
                adapter.updateData("", mutableListOf())
                binding.recycler.visibility = View.GONE
            } else if (it.length >= 2) {
                locationModel.geocoding(it)
            } else {
                adapter.updateData("", mutableListOf())
                binding.recycler.visibility = View.GONE
            }
        }

        adapter = SearchResultsAdapter {
            locationModel.getLocationFromSearchSuggestion(it)
            hideKeyboard()
        }

        binding.recycler.adapter = adapter
        recyclerLayoutManager = binding.recycler.layoutManager as LinearLayoutManager
        binding.recycler.visibility = View.GONE
    }
}
