package com.weatherxm.ui.home.devices

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDevicesBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.getParcelableExtra
import com.weatherxm.ui.deviceheliumota.DeviceHeliumOTAActivity.Companion.ARG_DEVICE
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DevicesFragment : Fragment(), KoinComponent, DeviceListener {

    private val parentModel: HomeViewModel by activityViewModels()
    private val model: DevicesViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private lateinit var binding: FragmentDevicesBinding

    private val userDeviceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                model.fetch()
            }
        }

    private val updateOTADeviceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val device = result.data?.getParcelableExtra(ARG_DEVICE, Device.empty())
                if (device != null && device != Device.empty()) {
                    navigator.showUserDevice(userDeviceLauncher, this, device)
                }
            }
        }

    // Register the launcher for the connect wallet activity and wait for a possible result
    private val connectWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                parentModel.setWalletNotMissing()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDevicesBinding.inflate(inflater, container, false)

        binding.root.applyInsets()

        val adapter = DeviceAdapter(this)
        binding.recycler.adapter = adapter

        binding.swiperefresh.setOnRefreshListener {
            model.fetch()
        }

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                model.onScroll(dy)
            }
        })

        model.devices().observe(viewLifecycleOwner) { devicesResource ->
            when (devicesResource.status) {
                Status.SUCCESS -> {
                    binding.swiperefresh.isRefreshing = false
                    if (!devicesResource.data.isNullOrEmpty()) {
                        adapter.submitList(devicesResource.data)
                        adapter.notifyDataSetChanged()
                        binding.recycler.visibility = View.VISIBLE
                        binding.empty.visibility = View.GONE
                        parentModel.getWalletMissing()
                    } else {
                        binding.empty.animation(R.raw.anim_empty_devices, false)
                        binding.empty.title(getString(R.string.empty_weather_stations))
                        binding.empty.subtitle(getString(R.string.add_weather_station))
                        binding.empty.listener(null)
                        binding.empty.visibility = View.VISIBLE
                        binding.recycler.visibility = View.GONE
                    }
                }
                Status.ERROR -> {
                    binding.swiperefresh.isRefreshing = false
                    binding.empty.animation(R.raw.anim_error, false)
                    binding.empty.title(getString(R.string.error_generic_message))
                    binding.empty.subtitle(devicesResource.message)
                    binding.empty.action(getString(R.string.action_retry))
                    binding.empty.listener { model.fetch() }
                    binding.empty.visibility = View.VISIBLE
                    binding.recycler.visibility = View.GONE
                }
                Status.LOADING -> {
                    if (binding.swiperefresh.isRefreshing) {
                        binding.empty.clear()
                        binding.empty.visibility = View.GONE
                    } else if (adapter.currentList.isNotEmpty()) {
                        binding.empty.clear()
                        binding.empty.visibility = View.GONE
                        binding.swiperefresh.isRefreshing = true
                    } else {
                        binding.recycler.visibility = View.GONE
                        binding.empty.clear()
                        binding.empty.animation(R.raw.anim_loading)
                        binding.empty.visibility = View.VISIBLE
                    }
                }
            }
        }

        model.preferenceChanged().observe(viewLifecycleOwner) {
            if (it) adapter.notifyDataSetChanged()
        }

        parentModel.onWalletMissing().observe(viewLifecycleOwner) {
            if (it) {
                binding.walletWarning.action(getString(R.string.add_wallet_now)) {
                    navigator.showConnectWallet(connectWalletLauncher, this)
                }.show()
            } else {
                binding.walletWarning.hide()
            }
        }

        parentModel.onDeviceClaimed().observe(viewLifecycleOwner) {
            it?.let {
                onDeviceClicked(it)
            }
        }

        return binding.root
    }

    override fun onDeviceClicked(device: Device) {
        navigator.showUserDevice(userDeviceLauncher, this, device)
    }

    override fun onWarningActionClicked(device: Device) {
        navigator.showDeviceHeliumOTA(updateOTADeviceLauncher, this, device)
    }
}
