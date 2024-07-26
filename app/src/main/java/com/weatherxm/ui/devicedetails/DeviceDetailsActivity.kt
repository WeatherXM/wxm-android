package com.weatherxm.ui.devicedetails

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Resource
import com.weatherxm.data.SeverityLevel
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityDeviceDetailsBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.setBundleChip
import com.weatherxm.ui.common.setColor
import com.weatherxm.ui.common.setErrorChip
import com.weatherxm.ui.common.setStatusChip
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.devicedetails.current.CurrentFragment
import com.weatherxm.ui.devicedetails.forecast.ForecastFragment
import com.weatherxm.ui.devicedetails.rewards.RewardsFragment
import com.weatherxm.ui.explorer.UICell
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceDetailsActivity : BaseActivity() {
    private val model: DeviceDetailsViewModel by viewModel {
        parametersOf(
            intent.parcelable<UIDevice>(Contracts.ARG_DEVICE) ?: UIDevice.empty(),
            intent.getStringExtra(ARG_DEVICE_ID) ?: String.empty(),
            intent.getBooleanExtra(Contracts.ARG_OPEN_EXPLORER_ON_BACK, false)
        )
    }
    private lateinit var binding: ActivityDeviceDetailsBinding

    companion object {
        private const val OBSERVATIONS = 0
        private const val FORECAST_TAB_POSITION = 1
        private const val REWARDS_TAB_POSITION = 2
    }

    init {
        lifecycleScope.launch {
            // Launch the block in a new coroutine every time the lifecycle
            // is in the RESUMED state (or above) and cancel it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                Timber.d("Starting device polling")
                // Trigger the flow for refreshing device data in the background
                model.deviceAutoRefresh().collect {
                    it.onRight { device ->
                        updateDeviceInfo(device)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dialogOverlay = MaterialAlertDialogBuilder(this).create()

        if (model.device.isEmpty() && model.deviceId.isEmpty()) {
            Timber.d("Could not start DeviceDetailsActivity. Device and DeviceID are null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        onBackPressedDispatcher.addCallback {
            if (!model.openExplorerOnBack || model.isLoggedIn() == null) {
                finish()
                return@addCallback
            }
            if (model.isLoggedIn() == true) {
                navigator.showHome(this@DeviceDetailsActivity, model.device.cellCenter)
            } else {
                navigator.showExplorer(this@DeviceDetailsActivity, model.device.cellCenter)
            }
            finish()
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener {
            onMenuItem(it)
        }

        binding.address.setOnClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.REGION.paramValue,
                customParams = arrayOf(
                    Pair(
                        AnalyticsService.CustomParam.CONTENT_NAME.paramName,
                        AnalyticsService.ParamValue.STATION_DETAILS_CHIP.paramValue
                    ),
                    Pair(
                        FirebaseAnalytics.Param.ITEM_ID,
                        AnalyticsService.ParamValue.STATION_REGION_ID.paramValue
                    )
                )
            )
            model.device.cellCenter?.let { location ->
                navigator.showCellInfo(this, UICell(model.device.cellIndex, location))
            }
        }

        model.onFollowStatus().observe(this) {
            onFollowStatus(it, dialogOverlay)
        }

        model.onUpdatedDevice().observe(this) {
            updateDeviceInfo(it)
        }

        model.address().observe(this) {
            binding.address.text = it ?: getString(R.string.unknown_address)
        }

        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = adapter.itemCount - 1

        @Suppress("UseCheckOrError")
        TabLayoutMediator(binding.navigatorGroup, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                OBSERVATIONS -> getString(R.string.observations)
                FORECAST_TAB_POSITION -> resources.getString(R.string.forecast)
                REWARDS_TAB_POSITION -> resources.getString(R.string.rewards)
                else -> throw IllegalStateException("Oops! You forgot to add a tab here.")
            }
        }.attach()

        binding.follow.setOnClickListener {
            handleFollowClick()
        }

        updateDeviceInfo()
    }

    override fun onResume() {
        super.onResume()
        if (model.device.relation != DeviceRelation.OWNED) {
            analytics.trackScreen(
                AnalyticsService.Screen.EXPLORER_DEVICE, classSimpleName(), model.device.id
            )
        }
    }

    private fun onFollowStatus(followStatus: Resource<Unit>, dialogOverlay: AlertDialog) {
        model.onFollowStatus().observe(this) {
            binding.loadingAnimation.visible(followStatus.status == Status.LOADING)
            when (followStatus.status) {
                Status.SUCCESS -> dialogOverlay.cancel()
                Status.ERROR -> {
                    toast(it.message ?: getString(R.string.error_reach_out_short))
                    dialogOverlay.cancel()
                }

                Status.LOADING -> dialogOverlay.show()
            }
        }
    }

    private fun onMenuItem(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.share_station -> {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.DEVICE_DETAILS_SHARE.paramValue
                )
                navigator.openShare(
                    this,
                    getString(R.string.share_station_url, model.createNormalizedName())
                )
                true
            }
            R.id.settings -> {
                navigator.showStationSettings(this, model.device)
                true
            }
            else -> false
        }
    }

    private fun updateDeviceInfo(device: UIDevice = model.device) {
        with(device.getDefaultOrFriendlyName()) {
            binding.collapsingToolbar.title = this
            binding.name.text = this
            binding.publicName.text = device.name
            binding.publicName.visibility = if (this != device.name) VISIBLE else INVISIBLE
        }

        with(binding.follow) {
            when (device.relation) {
                DeviceRelation.OWNED -> {
                    setOnClickListener {
                        // NO-OP
                    }
                    setImageResource(R.drawable.ic_home)
                    setColor(R.color.colorOnSurface)
                    isEnabled = false
                }

                DeviceRelation.FOLLOWED -> {
                    setOnClickListener {
                        handleFollowClick()
                    }
                    setImageResource(R.drawable.ic_favorite)
                    setColor(R.color.follow_heart_color)
                    isEnabled = true
                }

                DeviceRelation.UNFOLLOWED -> {
                    setOnClickListener {
                        handleFollowClick()
                    }
                    setImageResource(R.drawable.ic_favorite_outline)
                    setColor(R.color.follow_heart_color)
                    isEnabled = true
                }

                null -> visible(false)
            }
        }

        with(binding.toolbar) {
            if (device.isFollowed()) {
                if (menu.findItem(R.id.settings) == null) {
                    menu.add(Menu.NONE, R.id.settings, 1, R.string.station_settings)
                }
            } else if (device.isUnfollowed()) {
                menu.removeItem(R.id.settings)
            }
        }

        binding.status.setStatusChip(device)
        binding.bundle.setBundleChip(device)
        if (!model.device.isOnline()) {
            binding.status.setOnClickListener {
                navigator.showDeviceAlerts(this, model.device)
            }
        } else {
            binding.status.setOnClickListener(null)
            binding.status.isClickable = false
        }

        if (device.address.isNullOrEmpty()) {
            model.fetchAddressFromCell()
        } else {
            binding.address.text = device.address
        }

        setAlerts(device.alerts)
    }

    private fun setAlerts(alerts: List<DeviceAlert>) {
        val alertsWithoutOffline = alerts.dropWhile {
            it.alert == DeviceAlertType.OFFLINE
        }
        val hasErrorSeverity = alerts.firstOrNull {
            it.severity == SeverityLevel.ERROR
        } != null

        if (alerts.size > 1) {
            if (hasErrorSeverity) {
                binding.alertChip.setErrorChip()
            }
            binding.alertChip.text = getString(R.string.issues, alerts.size)
            setupAlertChipClickListener(null)
        } else if (alertsWithoutOffline.size == 1) {
            if (alertsWithoutOffline[0].alert == DeviceAlertType.NEEDS_UPDATE) {
                binding.alertChip.text = getString(R.string.update_required)
                analytics.trackEventPrompt(
                    AnalyticsService.ParamValue.OTA_AVAILABLE.paramValue,
                    AnalyticsService.ParamValue.WARN.paramValue,
                    AnalyticsService.ParamValue.VIEW.paramValue
                )
                setupAlertChipClickListener(AnalyticsService.ParamValue.OTA_UPDATE_ID.paramValue)
            } else if (alertsWithoutOffline[0].alert == DeviceAlertType.LOW_BATTERY) {
                binding.alertChip.text = getString(R.string.low_battery)
                setupAlertChipClickListener(AnalyticsService.ParamValue.LOW_BATTERY_ID.paramValue)
            }
        }
        binding.alertChip.visible(alertsWithoutOffline.isNotEmpty())
    }

    private fun setupAlertChipClickListener(analyticsItemId: String?) {
        analyticsItemId?.let {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.WARNINGS.paramValue,
                customParams = arrayOf(
                    Pair(
                        AnalyticsService.CustomParam.CONTENT_NAME.paramName,
                        AnalyticsService.ParamValue.STATION_DETAILS_CHIP.paramValue
                    ),
                    Pair(FirebaseAnalytics.Param.ITEM_ID, it)
                )
            )
        }
        binding.alertChip.setOnClickListener {
            navigator.showDeviceAlerts(this, model.device)
        }
    }

    private fun handleFollowClick() {
        if (model.isLoggedIn() == false) {
            navigator.showLoginDialog(
                fragmentActivity = this,
                title = getString(R.string.add_favorites),
                htmlMessage = getString(R.string.hidden_content_login_prompt, model.device.name)
            )
            return
        }

        if (model.device.isFollowed()) {
            navigator.showHandleFollowDialog(this, false, model.device.name) {
                model.unFollowStation()
            }
        } else if (model.device.isUnfollowed() && !model.device.isOnline()) {
            navigator.showHandleFollowDialog(this, true, model.device.name) {
                model.followStation()
            }
        } else if (model.device.isUnfollowed()) {
            model.followStation()
        }
    }

    class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int {
            return 3
        }

        @Suppress("UseCheckOrError")
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                OBSERVATIONS -> CurrentFragment()
                FORECAST_TAB_POSITION -> ForecastFragment()
                REWARDS_TAB_POSITION -> RewardsFragment()
                else -> throw IllegalStateException("Oops! You forgot to add a fragment here.")
            }
        }
    }
}
