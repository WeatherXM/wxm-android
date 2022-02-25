package com.weatherxm.ui.userdevice

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ActivityUserDeviceBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.widget.TokenCardView
import com.weatherxm.util.applyInsets
import com.weatherxm.util.getRelativeTimeFromISO
import com.weatherxm.util.onTabSelected
import com.weatherxm.util.setHtml
import com.weatherxm.util.setTextAndColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UserDeviceActivity : AppCompatActivity(), KoinComponent, TokenCardView.TokenOptionListener {

    private val model: UserDeviceViewModel by viewModels()
    private lateinit var binding: ActivityUserDeviceBinding
    private lateinit var hourlyAdapter: HourlyAdapter
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
            toast(R.string.unknown_error)
            finish()
            return
        }

        // Initialize the adapter with empty data and its listener when an item is clicked
        hourlyAdapter = HourlyAdapter {
            binding.currentWeatherCard.setWeatherData(it)
        }
        binding.recycler.adapter = hourlyAdapter

        // Fix flickering on item selection
        (binding.recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.swiperefresh.setOnRefreshListener {
            model.fetchUserDeviceAllData()
        }

        binding.historicalCharts.setOnClickListener {
            navigator.showHistoryActivity(this, device)
        }

        binding.forecastNextDays.setOnClickListener {
            navigator.showForecast(this, device)
        }

        binding.tokenCard.optionListener = this

        binding.tokenNotice.setHtml(R.string.device_detail_token_notice)

        binding.dateTabs.onTabSelected {
            when (it.position) {
                TAB_TODAY -> {
                    model.fetchForecast(UserDeviceViewModel.ForecastState.TODAY)
                }
                TAB_TOMORROW -> {
                    model.fetchForecast(UserDeviceViewModel.ForecastState.TOMORROW)
                }
            }
        }

        model.onDeviceSet().observe(this) {
            updateToolbar(it)
            binding.currentWeatherCard.setWeatherData(it.currentWeather)
        }

        model.onForecast().observe(this) {
            hourlyAdapter.submitList(it)
        }

        model.onTokens().observe(this) {
            binding.tokenCard.setTokenData(it)
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

        // Fetch data
        model.setDevice(device)
        model.fetchUserDeviceAllData()
    }

    private fun updateToolbar(device: Device) {
        binding.title.text = device.name

        binding.statusChip.apply {
            when (device.attributes?.isActive) {
                true -> setTextAndColor(R.string.online, R.color.green)
                false -> setTextAndColor(R.string.offline, R.color.red)
                null -> setTextAndColor(R.string.unknown, R.color.grey)
            }
        }

        val lastActive = device.attributes?.lastActiveAt?.let {
            getString(
                R.string.last_active,
                getRelativeTimeFromISO(it, getString(R.string.last_active_just_now))
            )
        }
        binding.subtitle.text = listOf(device.address, lastActive)
            .joinToString(" · ")
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

    override fun onOptionClick(tokenOption: UserDeviceViewModel.TokensState) {
        model.fetchTokenDetails(tokenOption)
    }
}
