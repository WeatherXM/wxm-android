package com.weatherxm.ui.userdevice

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ActivityUserDeviceBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.util.DateTimeHelper.getRelativeTimeFromISO
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setColor
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UserDeviceActivity : AppCompatActivity(), KoinComponent, OnMenuItemClickListener {

    private val model: UserDeviceViewModel by viewModels()
    private lateinit var binding: ActivityUserDeviceBinding
    private lateinit var hourlyAdapter: HourlyAdapter
    private lateinit var layoutManagerOfRecycler: LinearLayoutManager
    private val navigator: Navigator by inject()
    private var snackbar: Snackbar? = null

    companion object {
        const val ARG_DEVICE = "device"
        const val TAB_TODAY = 0
        const val TAB_TOMORROW = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val device = intent?.extras?.getParcelable<Device>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start UserDeviceActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.setOnMenuItemClickListener(this)

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

        binding.swiperefresh.setOnRefreshListener {
            model.fetchUserDeviceAllData(forceRefresh = true)
            setResult(Activity.RESULT_OK)
        }

        binding.historicalCharts.setOnClickListener {
            navigator.showHistoryActivity(this, model.getDevice())
        }

        binding.forecastNextDays.setOnClickListener {
            navigator.showForecast(this, model.getDevice())
        }

        binding.tokenRewards.setOnClickListener {
            navigator.showTokenScreen(this, model.getDevice())
        }

        binding.tokenNotice.setHtml(R.string.device_detail_token_notice)

        // onCreate was too big. Handle the initialization of the observers in a separate function
        initObservers()

        // Fetch data
        model.setDevice(device)
        model.fetchUserDeviceAllData()
    }

    private fun initObservers() {
        model.onDeviceSet().observe(this) {
            updateDeviceInfo(it)
            binding.currentWeatherCard.setData(it.currentWeather, it.timezone, 1)
        }

        model.onForecast().observe(this) {
            hourlyAdapter.submitList(it)
        }

        model.onTokens().observe(this) {
            binding.tokenCard.setTokenInfo(it, model.getDevice().rewards?.totalRewards)
        }

        model.onEditNameChange().observe(this) {
            if (it) {
                model.fetchUserDevice()
                setResult(Activity.RESULT_OK)
            }
        }

        model.onUnitPreferenceChanged().observe(this) {
            if (it) {
                binding.currentWeatherCard.updateCurrentWeatherUI(1, model.getDevice().timezone)
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

    override fun onMenuItemClick(menuItem: MenuItem?): Boolean {
        return when (menuItem?.itemId) {
            R.id.settings -> {
                navigator.showPreferences(this)
                true
            }
            R.id.edit_name -> {
                model.canChangeFriendlyName()
                    .fold({
                        toast(it.errorMessage)
                    }, {
                        // This cannot be false, by design
                        FriendlyNameDialogFragment(model.getDevice().attributes?.friendlyName) {
                            model.setOrClearFriendlyName(it)
                        }.show(this)
                    })
                true
            }
            else -> false
        }
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
                    R.color.device_status_online
                }
                false -> {
                    binding.errorCard.setErrorMessageWithUrl(R.string.error_user_device_offline)
                    R.color.device_status_offline
                }
                null -> {
                    R.color.device_status_unknown
                }
            }
        )

        val lastActive = device.attributes?.lastWeatherStationActivity?.let {
            getString(
                R.string.last_active,
                getRelativeTimeFromISO(it, getString(R.string.last_active_just_now))
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
