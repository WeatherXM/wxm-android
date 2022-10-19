package com.weatherxm.ui.claimdevice.location

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherxm.R
import com.weatherxm.databinding.FragmentClaimSetLocationBinding
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumViewModel
import com.weatherxm.ui.claimdevice.m5.ClaimM5ViewModel
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.toast
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.onTextChanged
import kotlinx.coroutines.launch

class ClaimLocationFragment : Fragment() {
    private val m5ParentModel: ClaimM5ViewModel by activityViewModels()
    private val heliumParentModel: ClaimHeliumViewModel by activityViewModels()
    private val model: ClaimLocationViewModel by activityViewModels()
    private lateinit var binding: FragmentClaimSetLocationBinding

    private lateinit var adapter: SearchResultsAdapter
    private lateinit var recyclerLayoutManager: LinearLayoutManager

    companion object {
        const val TAG = "ClaimLocationFragment"
        const val ARG_DEVICE_TYPE = "has_pager"

        fun newInstance(deviceType: DeviceType) = ClaimLocationFragment().apply {
            arguments = Bundle().apply { putSerializable(ARG_DEVICE_TYPE, deviceType) }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arguments?.getSerializable(ARG_DEVICE_TYPE, DeviceType::class.java)?.let {
                        model.setDeviceType(it)
                    }
                } else {
                    arguments?.getSerializable(ARG_DEVICE_TYPE)?.let {
                        model.setDeviceType(it as DeviceType)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClaimSetLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context == null) {
            // No point executing if in the meanwhile the activity is dead
            return
        }

        if (model.getDeviceType() == DeviceType.M5_WIFI) {
            m5ParentModel.nextButtonStatus(true)
            binding.navigationButtons.visibility = View.GONE
        } else {
            binding.cancel.setOnClickListener {
                heliumParentModel.cancel()
            }

            binding.confirmAndClaim.setOnClickListener {
                model.confirmLocation()
                heliumParentModel.next()
            }
        }

        model.onSelectedSearchLocation().observe(viewLifecycleOwner) {
            adapter.updateData("", mutableListOf())
            binding.recycler.visibility = View.GONE
        }

        model.onSearchResults().observe(viewLifecycleOwner) {
            if (it == null) {
                context.toast(getString(R.string.error_search_suggestions))
            } else if (it.isEmpty() || binding.searchBox.length() <= 2) {
                adapter.updateData("", mutableListOf())
                binding.recycler.visibility = View.GONE
            } else {
                adapter.updateData(binding.searchBox.text.toString(), it)
                binding.recycler.visibility = View.VISIBLE
            }
        }

        binding.searchBox.onTextChanged {
            if (it.isEmpty()) {
                adapter.updateData("", mutableListOf())
                binding.recycler.visibility = View.GONE
            } else if (it.length >= 2) {
                model.geocoding(it)
            } else {
                adapter.updateData("", mutableListOf())
                binding.recycler.visibility = View.GONE
            }
        }

        adapter = SearchResultsAdapter {
            model.getLocationFromSearchSuggestion(it)
            hideKeyboard()
        }

        binding.recycler.adapter = adapter
        recyclerLayoutManager = binding.recycler.layoutManager as LinearLayoutManager
        binding.recycler.visibility = View.GONE
    }
}
