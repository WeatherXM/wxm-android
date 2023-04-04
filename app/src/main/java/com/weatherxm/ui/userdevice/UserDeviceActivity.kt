package com.weatherxm.ui.userdevice

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ActivityUserDeviceBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.getParcelableExtra
import com.weatherxm.ui.common.toast
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setColor
import com.weatherxm.util.setHtml
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
    private lateinit var hourlyAdapter: HourlyAdapter
    private lateinit var layoutManagerOfRecycler: LinearLayoutManager
    private val navigator: Navigator by inject()
    private var snackbar: Snackbar? = null

    companion object {
        const val TAB_TODAY = 0
        const val TAB_TOMORROW = 1
    }

    init {
        lifecycleScope.launch {
            // Launch the block in a new coroutine every time the lifecycle
            // is in the RESUMED state (or above) and cancel it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                Timber.d("Starting device polling")
                // Trigger the flow for refreshing device data in the background
                model.deviceAutoRefresh().collect {
                    it.tap { device ->
                        onDeviceUpdated(device)
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

        // Initialize the adapter with empty data
        hourlyAdapter = HourlyAdapter()
        binding.recycler.adapter = hourlyAdapter

        // Initialize the layout manager
        layoutManagerOfRecycler = binding.recycler.layoutManager as LinearLayoutManager

        binding.recycler.setOnScrollChangeListener { _, _, _, _, _ ->
            onHourlyForecastScroll()
        }

        // Fix flickering on item selection
        (binding.recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        binding.dateTabs.addOnTabSelectedListener(onForecastDateSelectedListener)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.settingsBtn.setOnClickListener {
            navigator.showStationSettings(this, model.device)
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchTokensForecastData(forceRefresh = true)
            model.fetchUserDevice()
            setResult(Activity.RESULT_OK)
        }

        binding.historicalCharts.setOnClickListener {
            navigator.showHistoryActivity(this, model.device)
        }

        binding.forecastNextDays.setOnClickListener {
            navigator.showForecast(this, model.device)
        }

        binding.tokenRewards.setOnClickListener {
            navigator.showTokenScreen(this, model.device)
        }

        binding.tokenNotice.setHtml(R.string.device_detail_token_notice)

        // onCreate was too big. Handle the initialization of the observers in a separate function
        initObservers()
        onDeviceUpdated(model.device)

        // Fetch data
        model.fetchTokensForecastData()
    }

    private fun initObservers() {
        model.onDeviceSet().observe(this) {
            onDeviceUpdated(it)
        }

        model.onForecast().observe(this) {
            hourlyAdapter.submitList(it)
        }

        model.onTokens().observe(this) {
            binding.tokenCard.setTokenInfo(it, model.device.rewards?.totalRewards)
        }

        model.onUnitPreferenceChanged().observe(this) {
            if (it) {
                binding.currentWeatherCard.updateCurrentWeatherUI(model.device.timezone)
                hourlyAdapter.notifyDataSetChanged()
            }
        }

        model.onLoading().observe(this) {
            if (it && binding.swiperefresh.isRefreshing) {
                binding.progress.visibility = View.INVISIBLE
            } else if (it) {
                binding.progress.visibility = View.VISIBLE
            } else {
                binding.swiperefresh.isRefreshing = false
                binding.progress.visibility = View.INVISIBLE
            }
        }

        model.onError().observe(this) {
            showSnackbarMessage(it.errorMessage, it.retryFunction)
        }
    }

    private fun onDeviceUpdated(device: Device) {
        updateDeviceInfo(device)
        binding.currentWeatherCard.setData(device.currentWeather, device.timezone)
    }

    private fun onForecastDateSelected(dateTabSelected: TabLayout.Tab) {
        when (dateTabSelected.position) {
            TAB_TODAY -> {
                layoutManagerOfRecycler.scrollToPosition(0)
            }
            TAB_TOMORROW -> {
                layoutManagerOfRecycler.scrollToPositionWithOffset(
                    model.getPositionOfTomorrowFirstItem(hourlyAdapter.currentList), 0
                )
            }
        }
    }

    private fun onHourlyForecastScroll() {
        val dateTabs = binding.dateTabs
        val firstItemVisiblePosition = layoutManagerOfRecycler.findFirstVisibleItemPosition()
        val firstItemHourlyWeather = hourlyAdapter.getItemFromPosition(firstItemVisiblePosition)
        val isFirstItemTomorrow = model.isHourlyWeatherTomorrow(firstItemHourlyWeather)

        // Disable tab listener first
        binding.dateTabs.removeOnTabSelectedListener(onForecastDateSelectedListener)

        if (isFirstItemTomorrow && dateTabs.selectedTabPosition == TAB_TODAY) {
            dateTabs.getTabAt(TAB_TOMORROW)?.select()
        } else if (!isFirstItemTomorrow && dateTabs.selectedTabPosition == TAB_TOMORROW) {
            dateTabs.getTabAt(TAB_TODAY)?.select()
        }

        // Re-enable listener
        binding.dateTabs.addOnTabSelectedListener(onForecastDateSelectedListener)
    }

    private fun updateDeviceInfo(device: Device) {
        binding.name.text = device.getNameOrLabel()
        binding.collapsingToolbar.title = device.getNameOrLabel()

        binding.statusIcon.setColor(
            when (device.attributes?.isActive) {
                true -> {
                    binding.errorCard.hide()
                    R.color.success
                }
                false -> {
                    binding.errorCard.setErrorMessageWithUrl(
                        R.string.error_user_device_offline,
                        device.profile
                    )
                    R.color.error
                }
                null -> {
                    R.color.midGrey
                }
            }
        )

        val lastActive = device.attributes?.lastWeatherStationActivity?.let {
            getString(
                R.string.last_active,
                it.getRelativeFormattedTime(getString(R.string.last_active_just_now))
            )
        }

        binding.address.text = device.address
        binding.lastActive.text = lastActive
        binding.statusLabel.text = getString(
            when (device.attributes?.isActive) {
                true -> R.string.online
                false -> R.string.offline
                null -> R.string.unknown
            }
        )
    }

    private fun showSnackbarMessage(message: String, callback: (() -> Unit)? = null) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }

        if (callback != null) {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            snackbar?.setAction(R.string.action_retry) {
                callback()
            }
        } else {
            snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        }
        snackbar?.show()
    }

    private val onForecastDateSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            tab?.let { onForecastDateSelected(it) }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            // No-op
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            // No-op
        }
    }
}
