package com.weatherxm.ui.home.devices

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieDrawable.INFINITE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.InfoBanner
import com.weatherxm.databinding.FragmentDevicesBinding
import com.weatherxm.service.GlobalUploadObserverService
import com.weatherxm.ui.common.DeviceAdapter
import com.weatherxm.ui.common.DeviceListener
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.setCardRadius
import com.weatherxm.ui.common.swipeToDismiss
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseFragment
import com.weatherxm.ui.components.PhotoUploadState
import com.weatherxm.ui.home.HomeViewModel
import com.weatherxm.util.ImageFileHelper.deleteAllStationPhotos
import com.weatherxm.util.NumberUtils.formatTokens
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class DevicesFragment : BaseFragment(), DeviceListener {

    private val parentModel: HomeViewModel by activityViewModel()
    private val model: DevicesViewModel by activityViewModel()
    private lateinit var binding: FragmentDevicesBinding
    private lateinit var adapter: DeviceAdapter
    private lateinit var dialogOverlay: AlertDialog

    private val uploadObserverService: GlobalUploadObserverService by inject()
    private var removeUploadStateOnPause = false

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
            parentModel.getInfoBanner()
            model.fetch()
        }

        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            model.onScroll(scrollY - oldScrollY)
        }

        binding.sortFilterBtn.setOnClickListener {
            SortFilterDialogFragment().show(this)
        }

        model.devices().observe(viewLifecycleOwner) {
            onDevices(it)
        }

        model.onUnFollowStatus().observe(viewLifecycleOwner) {
            onUnFollowStatus(it)
        }

        model.onDevicesRewards().observe(viewLifecycleOwner) {
            onDevicesRewards(it)
        }

        parentModel.onInfoBanner().observe(viewLifecycleOwner) {
            onInfoBanner(it)
        }

        parentModel.onWalletWarnings().observe(viewLifecycleOwner) {
            onWalletMissingWarning(it.showMissingWarning)
        }

        initAndObserveUploadState()
        return binding.root
    }

    private fun onInfoBanner(infoBanner: InfoBanner?) {
        if (infoBanner != null) {
            binding.infoBanner
                .title(infoBanner.title)
                .message(infoBanner.message)
                .action(infoBanner.actionLabel, infoBanner.showActionButton) {
                    analytics.trackEventSelectContent(
                        contentType = AnalyticsService.ParamValue.ANNOUNCEMENT_BUTTON.paramValue,
                        Pair(FirebaseAnalytics.Param.ITEM_ID, infoBanner.url)
                    )
                    navigator.openWebsite(context, infoBanner.url)
                }
                .close(infoBanner.showCloseButton) {
                    parentModel.dismissInfoBanner(infoBanner.id)
                    binding.contentContainerCard.setCardRadius(0F, 0F, 0F, 0F)
                    binding.infoBanner.visible(false)
                }
                .visible(true)

            val radius = resources.getDimension(R.dimen.radius_large)
            binding.contentContainerCard.setCardRadius(radius, radius, 0F, 0F)
        } else if (binding.infoBanner.isVisible) {
            binding.infoBanner.visible(false)
            binding.contentContainerCard.setCardRadius(0F, 0F, 0F, 0F)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogOverlay = MaterialAlertDialogBuilder(requireContext()).create()
    }

    override fun onPause() {
        super.onPause()
        if (removeUploadStateOnPause) {
            binding.uploadStateContainer.visible(false)
        }
    }

    private fun initAndObserveUploadState() {
        binding.uploadStateView.setContent {
            val uploadState =
                uploadObserverService.getUploadPhotosState().collectAsState(null).value
            binding.uploadStateContainer.visible(uploadState != null)

            binding.uploadAnimation.visible(uploadState?.error == null)
            binding.uploadRetryIcon.visible(uploadState?.error != null)

            if (uploadState?.error != null) {
                binding.uploadStateCard.swipeToDismiss {
                    binding.uploadStateContainer.visible(false)
                    deleteAllStationPhotos(context, uploadState.device)
                }
                binding.uploadStateCard.setOnClickListener {
                    parentModel.retryPhotoUpload(uploadState.device.id)
                }
            } else if (uploadState?.isSuccess == true) {
                removeUploadStateOnPause = true
                binding.uploadStateCard.swipeToDismiss {
                    binding.uploadStateContainer.visible(false)
                }
                binding.uploadStateCard.setOnClickListener {
                    navigator.showStationSettings(context, uploadState.device)
                    binding.uploadStateContainer.visible(false)
                }
                binding.uploadAnimation.setAnimation(R.raw.anim_upload_success)
                binding.uploadAnimation.repeatCount = 0
            } else if (uploadState?.progress == 0) {
                binding.uploadAnimation.setAnimation(R.raw.anim_uploading)
                binding.uploadAnimation.repeatCount = INFINITE
            }

            uploadState?.let {
                PhotoUploadState(it, true)
            }
        }
    }

    private fun onUnFollowStatus(status: Resource<Unit>) {
        when (status.status) {
            Status.SUCCESS -> {
                binding.empty.visible(false)
                dialogOverlay.cancel()
            }
            Status.ERROR -> {
                context.toast(status.message ?: getString(R.string.error_reach_out_short))
                binding.empty.visible(false)
                dialogOverlay.cancel()
            }
            Status.LOADING -> {
                binding.empty.animation(R.raw.anim_loading).visible(true)
                dialogOverlay.show()
            }
        }
    }

    private fun onDevices(devices: Resource<List<UIDevice>>) {
        when (devices.status) {
            Status.SUCCESS -> {
                binding.swiperefresh.isRefreshing = false
                parentModel.setHasDevices(devices.data)
                parentModel.getWalletWarnings()
                if (!devices.data.isNullOrEmpty()) {
                    adapter.submitList(devices.data)
                    adapter.notifyDataSetChanged()
                    binding.empty.visible(false)
                    binding.recycler.visible(true)
                } else {
                    binding.empty.clear()
                        .animation(R.raw.anim_empty_devices, false)
                        .title(getString(R.string.empty_weather_stations))
                        .htmlSubtitle(getString(R.string.add_weather_station_or_browser_map))
                        .action(getString(R.string.view_explorer_map))
                        .listener {
                            parentModel.openExplorer()
                        }
                        .visible(true)
                    adapter.submitList(mutableListOf())
                    binding.recycler.visible(false)
                }
            }
            Status.ERROR -> {
                binding.swiperefresh.isRefreshing = false
                binding.totalEarnedCard.visible(true)
                binding.totalEarned.text = getString(R.string.wxm_amount, "?")
                binding.empty.animation(R.raw.anim_error, false)
                    .title(getString(R.string.error_generic_message))
                    .subtitle(devices.message)
                    .action(getString(R.string.action_retry))
                    .listener { model.fetch() }
                    .visible(true)
                binding.recycler.visible(false)
            }
            Status.LOADING -> {
                if (binding.swiperefresh.isRefreshing) {
                    binding.empty.clear().visible(false)
                } else if (adapter.currentList.isNotEmpty()) {
                    binding.empty.clear().visible(false)
                    binding.swiperefresh.isRefreshing = true
                } else {
                    binding.recycler.visible(false)
                    binding.empty.clear().animation(R.raw.anim_loading).visible(true)
                    binding.totalEarnedCard.invisible()
                }
            }
        }
    }

    private fun onDevicesRewards(rewards: DevicesRewards) {
        binding.totalEarnedCard.visible(true)
        binding.totalEarnedCard.setOnClickListener {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.TOKENS_EARNED_PRESSED.paramValue
            )
            navigator.showDevicesRewards(this, rewards)
        }
        binding.totalEarned.text = getString(R.string.wxm_amount, formatTokens(rewards.total))
        binding.totalEarnedContainer.visible(rewards.devices.isEmpty() || rewards.total > 0F)
        binding.noRewardsYet.visible(rewards.devices.isNotEmpty() && rewards.total == 0F)
    }

    private fun onWalletMissingWarning(walletMissing: Boolean) {
        if (walletMissing && parentModel.hasDevices() == true) {
            binding.walletWarning.action(getString(R.string.add_wallet_now)) {
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.WALLET_MISSING.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.ACTION.paramValue
                )
                navigator.showConnectWallet(connectWalletLauncher, this)
            }.closeButton {
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.WALLET_MISSING.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.DISMISS.paramValue
                )
                binding.walletWarning.visible(false)
                parentModel.setWalletWarningDismissTimestamp()
            }
            analytics.trackEventPrompt(
                AnalyticsService.ParamValue.WALLET_MISSING.paramValue,
                AnalyticsService.ParamValue.WARN.paramValue,
                AnalyticsService.ParamValue.VIEW.paramValue
            )
        }
        binding.walletWarning.visible(walletMissing && parentModel.hasDevices() == true)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.DEVICES_LIST, classSimpleName())
    }

    override fun onDeviceClicked(device: UIDevice) {
        navigator.showDeviceDetails(context, device = device)

        analytics.trackEventUserAction(
            actionName = AnalyticsService.ParamValue.SELECT_DEVICE.paramValue,
            contentType = AnalyticsService.ParamValue.USER_DEVICE_LIST.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_LIST_ID, device.id)
        )
    }

    override fun onFollowBtnClicked(device: UIDevice) {
        if (device.relation == DeviceRelation.FOLLOWED) {
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.DEVICE_LIST_FOLLOW.paramValue,
                AnalyticsService.ParamValue.UNFOLLOW.paramValue
            )
            navigator.showHandleFollowDialog(activity, false, device.name) {
                model.unFollowStation(device.id)
            }
        }
    }
}
