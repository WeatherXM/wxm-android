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
import com.weatherxm.ui.ForecastData
import com.weatherxm.ui.common.toast
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import timber.log.Timber

class ForecastActivity : AppCompatActivity(), KoinComponent {

    private lateinit var binding: ActivityForecastBinding
    private val model: ForecastViewModel by viewModels()

    private lateinit var adapter: DailyForecastAdapter
    private lateinit var deviceId: String

    companion object {
        const val ARG_DEVICE = "device"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val device = intent?.extras?.getParcelable<Device>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start ForecastActivity. Device is null.")
            toast(R.string.unknown_error)
            finish()
            return
        }

        deviceId = device.id
        binding.toolbar.subtitle = device.address ?: device.name

        // Initialize the adapter with empty data
        adapter = DailyForecastAdapter()
        binding.recycler.adapter = adapter

        model.onForecast().observe(this) {
            updateUI(it)
        }

        model.getWeatherForecast(deviceId)
    }

    private fun updateUI(resource: Resource<ForecastData>) {
        when (resource.status) {
            Status.SUCCESS -> {
                resource.data?.let { adapter.submitList(it.dailyForecasts) }
                binding.empty.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
            }
            Status.ERROR -> {
                Timber.d("Got error: $resource.message")
                binding.recycler.visibility = View.GONE
                binding.empty.animation(R.raw.anim_error)
                binding.empty.title(getString(R.string.no_forecast_data))
                binding.empty.subtitle(resource.message)
                binding.empty.action(getString(R.string.action_retry))
                binding.empty.listener { model.getWeatherForecast(deviceId) }
                binding.empty.visibility = View.VISIBLE
            }
            Status.LOADING -> {
                binding.recycler.visibility = View.GONE
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.empty.visibility = View.VISIBLE
            }
        }
    }
}

