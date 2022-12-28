package com.weatherxm.ui.deviceforecast

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityForecastBinding
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.getParcelableExtra
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ForecastActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForecastBinding

    private val model: ForecastViewModel by viewModel {
        parametersOf(getParcelableExtra(ARG_DEVICE, Device.empty()))
    }

    private lateinit var adapter: DailyForecastAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (model.device.isEmpty()) {
            Timber.d("Could not start ForecastActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.subtitle = model.device.address ?: model.device.getNameOrLabel()

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

