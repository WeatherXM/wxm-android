package com.weatherxm.ui.publicdeviceslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentPublicDevicesListBinding
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.explorer.ExplorerViewModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import timber.log.Timber

class PublicDevicesListFragment : BottomSheetDialogFragment(), KoinComponent {
    private val explorerModel: ExplorerViewModel by activityViewModels()
    private val model: PublicDevicesListViewModel by viewModels()
    private lateinit var binding: FragmentPublicDevicesListBinding
    private var selectedHexIndex: String? = null
    private lateinit var adapter: PublicDevicesListAdapter

    companion object {
        const val TAG = "PublicDevicesListFragment"
        private const val ARG_HEX_SELECTED_INDEX = "hex_index"

        fun newInstance(hexIndex: String?) = PublicDevicesListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_HEX_SELECTED_INDEX, hexIndex)
            }
        }
    }

    init {
        lifecycleScope.launch {
            whenCreated {
                selectedHexIndex = arguments?.getString(ARG_HEX_SELECTED_INDEX)
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_WeatherXM_BottomSheetDialog_Explorer_Devices
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPublicDevicesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PublicDevicesListAdapter {
            explorerModel.onDeviceClicked(it)
            dismiss()
        }

        binding.recycler.adapter = adapter

        model.devices().observe(this) {
            updateUI(it)
        }

        model.address().observe(this) {
            binding.location.text = it
            binding.location.visibility = View.VISIBLE
        }

        model.fetchDevices(selectedHexIndex)
    }

    private fun updateUI(devices: Resource<List<Device>>) {
        when (devices.status) {
            Status.SUCCESS -> {
                binding.title.text = getString(R.string.weather_stations)
                if (!devices.data.isNullOrEmpty()) {
                    adapter.submitList(devices.data)
                    binding.recycler.visibility = View.VISIBLE
                } else {
                    toast(getString(R.string.oops_something_wrong), Toast.LENGTH_LONG)
                }
            }
            Status.ERROR -> {
                Timber.d(devices.message, devices.message)
                devices.message?.let { toast(it, Toast.LENGTH_LONG) }
            }
            Status.LOADING -> {}
        }
    }
}
