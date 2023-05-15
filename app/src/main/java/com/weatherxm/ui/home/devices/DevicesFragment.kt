package com.weatherxm.ui.home.devices

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDevicesBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.UserDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DevicesFragment : Fragment(), KoinComponent, DeviceListener {

    private val parentModel: HomeViewModel by activityViewModels()
    private val model: DevicesViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private lateinit var binding: FragmentDevicesBinding

    private val userDeviceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                model.fetch()
            }
        }

    // Register the launcher for the connect wallet activity and wait for a possible result
    private val connectWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                parentModel.setWalletNotMissing()
            }
        }

    @SuppressLint("NotifyDataSetChanged")
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

        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            model.onScroll(scrollY - oldScrollY)
        }

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

        parentModel.onWalletMissingWarning().observe(viewLifecycleOwner) {
            if (it) {
                binding.walletWarning.action(getString(R.string.add_wallet_now)) {
                    navigator.showConnectWallet(connectWalletLauncher, this)
                }.closeButton {
                    binding.walletWarning.setVisible(false)
                    parentModel.setWalletWarningDismissTimestamp()
                }
            }
            binding.walletWarning.setVisible(it)
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.DEVICES_LIST,
            DevicesFragment::class.simpleName
        )
    }

    override fun onDeviceClicked(userDevice: UserDevice) {
        navigator.showUserDevice(userDeviceLauncher, this, userDevice.device)
    }

    override fun onUpdateStationClicked(userDevice: UserDevice) {
        navigator.showDeviceHeliumOTA(this, userDevice.device, false)
    }

    override fun onAlertsClicked(userDevice: UserDevice) {
        navigator.showDeviceAlerts(this, userDevice)
    }
}
