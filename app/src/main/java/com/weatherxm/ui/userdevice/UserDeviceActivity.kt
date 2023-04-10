package com.weatherxm.ui.userdevice

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceProfile
import com.weatherxm.databinding.ActivityUserDeviceBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.getParcelableExtra
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.userdevice.current.CurrentFragment
import com.weatherxm.ui.userdevice.forecast.ForecastFragment
import com.weatherxm.ui.userdevice.rewards.RewardsFragment
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setColor
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class UserDeviceActivity : AppCompatActivity(), KoinComponent {
    private val model: UserDeviceViewModel by viewModel {
        parametersOf(getParcelableExtra(ARG_DEVICE, Device.empty()))
    }
    private lateinit var binding: ActivityUserDeviceBinding
    private val navigator: Navigator by inject()

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
        binding = ActivityUserDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        if (model.device.isEmpty()) {
            Timber.d("Could not start UserDeviceActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.settingsBtn.setOnClickListener {
            navigator.showStationSettings(this, model.device)
        }

        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

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

    private fun updateDeviceInfo(device: Device = model.device) {
        binding.name.text = device.getNameOrLabel()

        with(binding.lastSeen) {
            text = device.attributes?.lastWeatherStationActivity?.getRelativeFormattedTime(
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

        binding.status.setCardBackgroundColor(
            getColor(
                when (device.attributes?.isActive) {
                    true -> {
                        binding.statusIcon.setColor(R.color.success)
                        binding.status.strokeColor = getColor(R.color.success)
                        R.color.successTint
                    }
                    false -> {
                        binding.statusIcon.setColor(R.color.error)
                        binding.status.strokeColor = getColor(R.color.error)
                        R.color.errorTint
                    }
                    null -> {
                        R.color.midGrey
                    }
                }
            )
        )

        binding.address.text = if (device.address.isNullOrEmpty()) {
            getString(R.string.unknown_address)
        } else {
            device.address
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
