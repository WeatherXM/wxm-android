package com.weatherxm.ui.devicedetails

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.removeItemAt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.databinding.ActivityDeviceDetailsBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DeviceOwnershipStatus
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.devicedetails.current.CurrentFragment
import com.weatherxm.ui.devicedetails.forecast.ForecastFragment
import com.weatherxm.ui.devicedetails.rewards.RewardsFragment
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.util.Analytics
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.applyInsets
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class DeviceDetailsActivity : AppCompatActivity(), KoinComponent {
    private val model: DeviceDetailsViewModel by viewModel {
        parametersOf(
            intent.getParcelableExtra<UIDevice>(Contracts.ARG_DEVICE),
            intent.getBooleanExtra(Contracts.ARG_OPEN_EXPLORER_ON_BACK, false)
        )
    }
    private lateinit var binding: ActivityDeviceDetailsBinding
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()

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

        binding.root.applyInsets()

        if (model.device.isEmpty()) {
            Timber.d("Could not start DeviceDetailsActivity. Device is null.")
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

        if (model.device.ownershipStatus == DeviceOwnershipStatus.UNFOLLOWED) {
            binding.toolbar.menu.removeItemAt(1)
        }

        binding.toolbar.setOnMenuItemClickListener {
            onMenuItem(it)
        }

        binding.address.setOnClickListener {
            model.device.cellCenter?.let { location ->
                navigator.showCellInfo(this, UICell(model.device.cellIndex, location))
            }
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

        updateDeviceInfo()
    }

    override fun onResume() {
        super.onResume()
        if (model.device.ownershipStatus != DeviceOwnershipStatus.OWNED) {
            analytics.trackScreen(
                Analytics.Screen.EXPLORER_DEVICE,
                DeviceDetailsActivity::class.simpleName,
                model.device.id
            )
        }
    }

    private fun onMenuItem(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.share_station -> {
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
        binding.name.text = device.getDefaultOrFriendlyName()
        binding.publicName.text = device.name
        binding.collapsingToolbar.title = device.getDefaultOrFriendlyName()

        @Suppress("UseCheckOrError")
        binding.stationFollowHomeIcon.setImageResource(
            when (device.ownershipStatus) {
                DeviceOwnershipStatus.OWNED -> R.drawable.ic_home
                DeviceOwnershipStatus.FOLLOWED -> R.drawable.ic_favorite
                DeviceOwnershipStatus.UNFOLLOWED -> R.drawable.ic_favorite_outline
                null -> throw IllegalStateException("Oops! No ownership status here.")
            }
        )

        with(binding.lastSeen) {
            text = device.lastWeatherStationActivity?.getRelativeFormattedTime(
                fallbackIfTooSoon = context.getString(R.string.just_now)
            )
        }

        binding.statusIcon.setImageResource(
            if (device.profile == DeviceProfile.Helium) {
                R.drawable.ic_helium
            } else {
                R.drawable.ic_wifi
            }
        )

        binding.statusCard.setCardBackgroundColor(
            getColor(
                when (device.isActive) {
                    true -> R.color.successTint
                    false -> R.color.errorTint
                    else -> R.color.midGrey
                }
            )
        )

        if (device.address.isNullOrEmpty()) {
            model.fetchAddressFromCell()
        } else {
            binding.address.text = device.address
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
