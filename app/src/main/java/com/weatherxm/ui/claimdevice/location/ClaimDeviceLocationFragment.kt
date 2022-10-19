package com.weatherxm.ui.claimdevice.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherxm.databinding.FragmentClaimDeviceSetLocationBinding
import com.weatherxm.ui.claimdevice.ClaimDeviceViewModel
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceViewModel
import com.weatherxm.ui.common.toast
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.onTextChanged
import kotlinx.coroutines.launch

class ClaimDeviceLocationFragment : Fragment() {
    private val m5ParentModel: ClaimDeviceViewModel by activityViewModels()
    private val heliumParentModel: ClaimHeliumDeviceViewModel by activityViewModels()
    private val locationModel: ClaimDeviceLocationViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimDeviceSetLocationBinding

    private lateinit var adapter: SearchResultsAdapter
    private lateinit var recyclerLayoutManager: LinearLayoutManager

    private var hasBottomNavigationButtons: Boolean = false

    companion object {
        const val TAG = "ClaimDeviceLocationFragment"
        private const val ARG_HAS_PAGER = "has_pager"

        fun newInstance(hasBottomNavigationButtons: Boolean) = ClaimDeviceLocationFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_HAS_PAGER, hasBottomNavigationButtons) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                arguments?.getBoolean(ARG_HAS_PAGER)?.let {
                    hasBottomNavigationButtons = it
                }
            }
        }
    }

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

        if (hasBottomNavigationButtons) {
            m5ParentModel.nextButtonStatus(true)
            binding.navigationButtons.visibility = View.GONE
        } else {
            binding.cancel.setOnClickListener {
                heliumParentModel.cancel()
            }

            binding.confirmAndClaim.setOnClickListener {
                locationModel.confirmLocation()
                heliumParentModel.next()
            }
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
