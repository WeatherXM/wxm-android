package com.weatherxm.ui.widgets.selectstation

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.weatherxm.R
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityWidgetSelectStationBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.widgets.currentweather.CurrentWeatherWidgetWorkerUpdate
import com.weatherxm.ui.widgets.currentweather.CurrentWeatherWidgetWorkerUpdate.Companion.UPDATE_INTERVAL_IN_MINS
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import java.util.concurrent.TimeUnit

class SelectStationActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityWidgetSelectStationBinding
    private val model: SelectStationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetSelectStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_CANCELED, resultValue)

        val adapter = SelectStationAdapter {
            model.setStationSelected(it.id)
            binding.confirmBtn.isEnabled = true
        }
        binding.recycler.adapter = adapter

        model.devices().observe(this) { devicesResource ->
            when (devicesResource.status) {
                Status.SUCCESS -> {
                    if (!devicesResource.data.isNullOrEmpty()) {
                        adapter.submitList(devicesResource.data)
                        binding.recycler.visibility = View.VISIBLE
                        binding.empty.visibility = View.GONE
                    } else {
                        binding.empty.animation(R.raw.anim_empty_devices, false)
                        binding.empty.title(getString(R.string.empty_weather_stations))
                        binding.empty.subtitle(getString(R.string.empty_select_station))
                        binding.empty.listener(null)
                        binding.empty.visibility = View.VISIBLE
                        binding.recycler.visibility = View.GONE
                    }
                }
                Status.ERROR -> {
                    binding.empty.animation(R.raw.anim_error, false)
                    binding.empty.title(getString(R.string.error_generic_message))
                    binding.empty.subtitle(devicesResource.message)
                    binding.empty.action(getString(R.string.action_retry))
                    binding.empty.listener { model.fetch() }
                    binding.empty.visibility = View.VISIBLE
                    binding.recycler.visibility = View.GONE
                }
                Status.LOADING -> {
                    binding.recycler.visibility = View.GONE
                    binding.empty.clear()
                    binding.empty.animation(R.raw.anim_loading)
                    binding.empty.visibility = View.VISIBLE
                }
            }
        }

        model.isNotLoggedIn().observe(this) {
            binding.empty.animation(R.raw.anim_error, false)
            binding.empty.title(getString(R.string.error_generic_message))
            binding.empty.subtitle(R.string.select_station_not_logged_in)
            binding.empty.listener(null)
            binding.empty.visibility = View.VISIBLE
            binding.recycler.visibility = View.GONE
        }

        binding.confirmBtn.setOnClickListener {
            onConfirmClicked(appWidgetId, resultValue)
        }

        model.checkIfLoggedInAndProceed()
    }

    private fun onConfirmClicked(appWidgetId: Int, resultValue: Intent) {
        model.saveWidgetData(appWidgetId)

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra(
            ARG_WIDGET_TYPE,
            model.getWidgetTypeById(AppWidgetManager.getInstance(this), appWidgetId)
        )
        intent.putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
        this.sendBroadcast(intent)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val widgetUpdateRequest = PeriodicWorkRequestBuilder<CurrentWeatherWidgetWorkerUpdate>(
            UPDATE_INTERVAL_IN_MINS,
            TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CURRENT_WEATHER_UPDATE_WORK",
            ExistingPeriodicWorkPolicy.KEEP,
            widgetUpdateRequest
        )
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
