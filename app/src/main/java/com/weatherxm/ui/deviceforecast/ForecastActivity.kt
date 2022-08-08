package com.weatherxm.ui.deviceforecast

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityForecastBinding
import com.weatherxm.ui.DailyForecast
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import timber.log.Timber

class ForecastActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForecastBinding
    private val model: ForecastViewModel by viewModels()

    private lateinit var adapter: DailyForecastAdapter

    companion object {
        const val ARG_DEVICE = "device"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val device = intent?.extras?.getParcelable<Device>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start ForecastActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        model.setDevice(device)

        binding.toolbar.subtitle = device.address ?: device.getNameOrLabel()

        // Initialize the adapter with empty data
        adapter = DailyForecastAdapter()
        binding.recycler.adapter = adapter

        binding.swiperefresh.setOnRefreshListener {
            model.getWeatherForecast(forceRefresh = true)
        }

        model.onForecast().observe(this) {
            updateUI(it)
        }

        model.getWeatherForecast()
    }

    private fun updateUI(resource: Resource<List<DailyForecast>>) {
        when (resource.status) {
            Status.SUCCESS -> {
                binding.swiperefresh.isRefreshing = false
                resource.data?.let { adapter.submitList(it) }
                binding.empty.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.swiperefresh.isRefreshing = false
                binding.recycler.visibility = View.GONE
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(getString(R.string.error_forecast_no_data))
                binding.empty.subtitle(resource.message)
                binding.empty.action(getString(R.string.action_retry))
                binding.empty.listener { model.getWeatherForecast() }
                binding.empty.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                if (binding.swiperefresh.isRefreshing) {
                    binding.empty.clear()
                    binding.empty.visibility = View.GONE
                } else {
                    binding.recycler.visibility = View.GONE
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_loading)
                    binding.empty.visibility = View.VISIBLE
                }
            }
        }
    }
}

