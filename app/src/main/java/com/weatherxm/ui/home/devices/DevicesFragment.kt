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
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDevicesBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import com.weatherxm.util.onTabSelected
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DevicesFragment : Fragment(), KoinComponent, DeviceListener {

    private val parentModel: HomeViewModel by activityViewModels()
    private val model: DevicesViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private lateinit var binding: FragmentDevicesBinding
    private lateinit var adapter: DeviceAdapter

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

        adapter = DeviceAdapter(this)
        binding.recycler.adapter = adapter

        binding.swiperefresh.setOnRefreshListener {
            model.fetch()
        }

        initTabs()

        binding.navigationTabs.onTabSelected {
            when (it.position) {
                0 -> {
                    // TODO: Implement this
                }
                1 -> {
                    // TODO: Implement this
                }
                2 -> {
                    // TODO: Implement this
                }
            }
        }

        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            model.onScroll(scrollY - oldScrollY)
        }

        model.devices().observe(viewLifecycleOwner) { devicesResource ->
            onDevices(devicesResource)
        }

        model.preferenceChanged().observe(viewLifecycleOwner) {
            if (it) adapter.notifyDataSetChanged()
        }

        parentModel.onWalletMissingWarning().observe(viewLifecycleOwner) {
            onWalletMissingWarning(it)
        }
        return binding.root
    }

    private fun initTabs() {
        with(binding.navigationTabs) {
            addTab(
                newTab().apply {
                    text = getString(R.string.total_with_placeholder, "?")
                },
                true
            )
            addTab(
                newTab().apply {
                    text = getString(R.string.owned_with_placeholder, "?")
                }
            )
            addTab(
                newTab().apply {
                    text = getString(R.string.following_with_placeholder, "?")
                }
            )
        }
    }

    private fun onDevices(devicesResource: Resource<List<UIDevice>>) {
        when (devicesResource.status) {
            Status.SUCCESS -> {
                binding.swiperefresh.isRefreshing = false
                if (!devicesResource.data.isNullOrEmpty()) {
                    // TODO: Change the numbers in all different tabs
                    binding.navigationTabs.getTabAt(0)?.text = getString(
                        R.string.total_with_placeholder,
                        devicesResource.data.size.toString()
                    )
                    binding.navigationTabs.getTabAt(1)?.text = getString(
                        R.string.owned_with_placeholder,
                        devicesResource.data.size.toString()
                    )
                    binding.navigationTabs.getTabAt(2)?.text = getString(
                        R.string.following_with_placeholder,
                        devicesResource.data.size.toString()
                    )

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

    private fun onWalletMissingWarning(walletMissing: Boolean) {
        if (walletMissing) {
            binding.walletWarning.action(getString(R.string.add_wallet_now)) {
                analytics.trackEventPrompt(
                    Analytics.ParamValue.WALLET_MISSING.paramValue,
                    Analytics.ParamValue.WARN.paramValue,
                    Analytics.ParamValue.ACTION.paramValue
                )
                navigator.showConnectWallet(connectWalletLauncher, this)
            }.closeButton {
                analytics.trackEventPrompt(
                    Analytics.ParamValue.WALLET_MISSING.paramValue,
                    Analytics.ParamValue.WARN.paramValue,
                    Analytics.ParamValue.DISMISS.paramValue
                )
                binding.walletWarning.setVisible(false)
                parentModel.setWalletWarningDismissTimestamp()
            }
            analytics.trackEventPrompt(
                Analytics.ParamValue.WALLET_MISSING.paramValue,
                Analytics.ParamValue.WARN.paramValue,
                Analytics.ParamValue.VIEW.paramValue
            )
        }
        binding.walletWarning.setVisible(walletMissing)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.DEVICES_LIST,
            DevicesFragment::class.simpleName
        )
    }

    override fun onDeviceClicked(device: UIDevice) {
        navigator.showDeviceDetails(context, device = device)

        analytics.trackEventUserAction(
            actionName = Analytics.ParamValue.SELECT_DEVICE.paramValue,
            contentType = Analytics.ParamValue.USER_DEVICE_LIST.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_LIST_ID, device.id)
        )
    }

    override fun onUpdateStationClicked(device: UIDevice) {
        analytics.trackEventPrompt(
            Analytics.ParamValue.OTA_AVAILABLE.paramValue,
            Analytics.ParamValue.WARN.paramValue,
            Analytics.ParamValue.ACTION.paramValue
        )
        navigator.showDeviceHeliumOTA(this, device, false)
    }

    override fun onAlertsClicked(device: UIDevice) {
        navigator.showDeviceAlerts(this, device)
    }
}
