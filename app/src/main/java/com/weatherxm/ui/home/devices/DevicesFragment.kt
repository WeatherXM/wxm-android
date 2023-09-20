package com.weatherxm.ui.home.devices

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.FragmentDevicesBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.common.toast
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
    private lateinit var adapter: DeviceAdapter
    private lateinit var dialogOverlay: AlertDialog

    // Register the launcher for the connect wallet activity and wait for a possible result
    private val connectWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                parentModel.setWalletNotMissing()
            }
        }

    @com.google.android.material.badge.ExperimentalBadgeUtils
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDevicesBinding.inflate(inflater, container, false)

        binding.root.applyInsets()

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            return@setOnMenuItemClickListener if (menuItem.itemId == R.id.sort_filter) {
                SortFilterDialogFragment.newInstance().show(this)
                true
            } else {
                false
            }
        }

        adapter = DeviceAdapter(this)
        binding.recycler.adapter = adapter
        val filtersBadge = BadgeDrawable.create(requireContext()).apply {
            context?.let {
                backgroundColor = it.getColor(R.color.follow_heart_color)
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetch()
        }

        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            model.onScroll(scrollY - oldScrollY)
        }

        model.devices().observe(viewLifecycleOwner) {
            if (model.getDevicesSortFilterOptions().areDefaultFiltersOn()) {
                BadgeUtils.detachBadgeDrawable(filtersBadge, binding.toolbar, R.id.sort_filter)
            } else {
                BadgeUtils.attachBadgeDrawable(filtersBadge, binding.toolbar, R.id.sort_filter)
            }
            onDevices(it)
        }

        model.onFollowStatus().observe(viewLifecycleOwner) {
            onFollowStatus(it)
        }

        parentModel.onWalletMissingWarning().observe(viewLifecycleOwner) {
            onWalletMissingWarning(it)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogOverlay = MaterialAlertDialogBuilder(requireContext()).create()
    }

    private fun onFollowStatus(status: Resource<Unit>) {
        when (status.status) {
            Status.SUCCESS -> {
                binding.empty.setVisible(false)
                dialogOverlay.cancel()
            }
            Status.ERROR -> {
                context.toast(status.message ?: getString(R.string.error_reach_out_short))
                binding.empty.setVisible(false)
                dialogOverlay.cancel()
            }
            Status.LOADING -> {
                binding.empty.animation(R.raw.anim_loading).setVisible(true)
                dialogOverlay.show()
            }
        }
    }

    private fun onDevices(devices: Resource<List<UIDevice>>) {
        when (devices.status) {
            Status.SUCCESS -> {
                binding.swiperefresh.isRefreshing = false
                if (!devices.data.isNullOrEmpty()) {
                    adapter.submitList(devices.data)
                    adapter.notifyDataSetChanged()
                    binding.empty.setVisible(false)
                    binding.recycler.setVisible(true)
                    parentModel.getWalletMissing(devices.data)
                } else {
                    with(binding.empty) {
                        clear()
                        animation(R.raw.anim_empty_devices, false)
                        listener(null)
                        title(getString(R.string.empty_weather_stations))
                        htmlSubtitle(getString(R.string.add_weather_station_or_browser_map))
                        action(getString(R.string.view_explorer_map))
                        listener {
                            parentModel.openExplorer()
                        }
                        visibility = View.VISIBLE
                    }
                    adapter.submitList(mutableListOf())
                    binding.recycler.setVisible(false)
                }
            }
            Status.ERROR -> {
                binding.swiperefresh.isRefreshing = false
                binding.empty.animation(R.raw.anim_error, false)
                binding.empty.title(getString(R.string.error_generic_message))
                binding.empty.subtitle(devices.message)
                binding.empty.action(getString(R.string.action_retry))
                binding.empty.listener { model.fetch() }
                binding.empty.setVisible(true)
                binding.recycler.setVisible(false)
            }
            Status.LOADING -> {
                if (binding.swiperefresh.isRefreshing) {
                    binding.empty.clear()
                    binding.empty.setVisible(false)
                } else if (adapter.currentList.isNotEmpty()) {
                    binding.empty.clear()
                    binding.empty.setVisible(false)
                    binding.swiperefresh.isRefreshing = true
                } else {
                    binding.recycler.setVisible(false)
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_loading)
                    binding.empty.setVisible(true)
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
        analytics.trackScreen(Analytics.Screen.DEVICES_LIST, DevicesFragment::class.simpleName)
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

    override fun onFollowBtnClicked(device: UIDevice) {
        if (device.relation == DeviceRelation.FOLLOWED) {
            analytics.trackEventUserAction(
                Analytics.ParamValue.DEVICE_LIST_FOLLOW.paramValue,
                Analytics.ParamValue.UNFOLLOW.paramValue
            )
            navigator.showHandleFollowDialog(activity, false, device.name) {
                model.unFollowStation(device.id)
            }
        }
    }
}
