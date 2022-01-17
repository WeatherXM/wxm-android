package com.weatherxm.ui.userdevice

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.HourlyWeather
import com.weatherxm.databinding.ActivityUserDeviceBinding
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.widget.TokenCardView
import com.weatherxm.util.applyTopBottomInsets
import com.weatherxm.util.getRelativeTimeFromISO
import com.weatherxm.util.onTabSelected
import com.weatherxm.util.setTextAndColor
import org.koin.core.component.KoinComponent
import timber.log.Timber

class UserDeviceActivity : AppCompatActivity(), KoinComponent, TokenCardView.TokenOptionListener {

    private val model: UserDeviceViewModel by viewModels()
    private lateinit var binding: ActivityUserDeviceBinding
    private var snackbar: Snackbar? = null
    private var hourlyAdapter: HourlyAdapter? = null

    companion object {
        const val ARG_DEVICE = "device"
        const val TAB_TODAY = 0
        const val TAB_TOMORROW = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyTopBottomInsets()

        val device = intent?.extras?.getParcelable<Device>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start UserDeviceActivity. Device is null.")
            toast(R.string.unknown_error)
            finish()
            return
        }

        // Initialize the adapter with empty data and its listener when an item is clicked
        hourlyAdapter = HourlyAdapter(this) {
            binding.currentWeatherCard.setWeatherData(it)
        }
        binding.recycler.adapter = hourlyAdapter

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tokenCard.optionListener = this

        binding.dateTabs.onTabSelected {
            when (it.position) {
                TAB_TODAY -> {
                    model.setForecastCurrentState(UserDeviceViewModel.ForecastState.TODAY)
                }
                TAB_TOMORROW -> {
                    model.setForecastCurrentState(UserDeviceViewModel.ForecastState.TOMORROW)
                }
            }
        }

        model.onDeviceSet().observe(this) {
            updateToolbar(it)
            binding.currentWeatherCard.setWeatherData(it.currentWeather)
        }

        model.onForecast().observe(this) {
            updateAdapter(device.currentWeather, it)
        }

        model.onTokens().observe(this) {
            binding.tokenCard.setTokenData(it)
        }

        model.onLoading().observe(this) {
            binding.progress.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        model.onError().observe(this) {
            showSnackbarMessage(it.errorMessage, it.retryFunction)
        }

        // Fetch data
        model.setDevice(device)
        model.getUserDeviceData()
    }

    private fun updateAdapter(currentWeather: HourlyWeather?, forecast: List<HourlyWeather>?) {
        val listAdapter = model.addCurrentToForecast(currentWeather, forecast)
        hourlyAdapter?.resetSelected()
        hourlyAdapter?.submitList(listAdapter)
    }

    private fun updateToolbar(device: Device) {
        binding.toolbar.title = device.name
        device.address?.let {
            binding.toolbar.subtitle = it
        }

        when (device.attributes?.isActive) {
            null -> {
                binding.statusChip.setTextAndColor(R.string.unknown, R.color.grey)
            }
            true -> {
                binding.statusChip.setTextAndColor(R.string.online, R.color.green)
            }
            else -> {
                binding.statusChip.setTextAndColor(R.string.offline, R.color.red)
            }
        }

        val lastActive = device.attributes?.lastActiveAt?.let {
            getRelativeTimeFromISO(it)
        } ?: ""
        if (lastActive.isNotEmpty()) {
            binding.lastActive.text = getString(R.string.last_active, lastActive)
            binding.lastActive.visibility = View.VISIBLE
        } else {
            binding.lastActive.visibility = View.GONE
        }
    }

    // TODO: Explore weird animation of snackbar when coming in and out of the screen
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
        model.setTokenCurrentState(tokenOption)
    }
}
