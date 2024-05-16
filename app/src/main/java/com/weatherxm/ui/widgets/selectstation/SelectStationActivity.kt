package com.weatherxm.ui.widgets.selectstation

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityWidgetSelectStationBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.widgets.currentweather.CurrentWeatherWidgetWorkerUpdate
import com.weatherxm.util.WidgetHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectStationActivity : BaseActivity() {
    private lateinit var binding: ActivityWidgetSelectStationBinding
    private val model: SelectStationViewModel by viewModel()
    private val widgetHelper: WidgetHelper by inject()

    private lateinit var adapter: SelectStationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetSelectStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_CANCELED, resultValue)

        adapter = SelectStationAdapter {
            model.setStationSelected(it)
            binding.confirmBtn.isEnabled = true
        }
        binding.recycler.adapter = adapter

        model.devices().observe(this) {
            binding.signInBtn.setVisible(false)
            binding.confirmBtn.setVisible(true)
            onDevices(it)
        }

        model.isNotLoggedIn().observe(this) {
            binding.empty.clear()
                .animation(R.raw.anim_warning)
                .title(getString(R.string.action_sign_in))
                .subtitle(R.string.select_station_not_logged_in)
                .listener(null)
            binding.confirmBtn.setVisible(false)
            binding.recycler.setVisible(false)
            binding.empty.setVisible(true)
            binding.signInBtn.setVisible(true)
        }

        binding.confirmBtn.setOnClickListener {
            onConfirmClicked(appWidgetId, resultValue)
        }

        binding.signInBtn.setOnClickListener {
            navigator.showLogin(this, true)
            finish()
        }

        model.checkIfLoggedInAndProceed()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.WIDGET_SELECT_STATION, classSimpleName())
    }

    private fun onDevices(devices: Resource<List<UIDevice>>) {
        when (devices.status) {
            Status.SUCCESS -> {
                if (!devices.data.isNullOrEmpty()) {
                    adapter.submitList(devices.data)
                    binding.empty.setVisible(false)
                    binding.recycler.setVisible(true)
                } else {
                    binding.empty.animation(R.raw.anim_empty_devices, false)
                    binding.empty.title(getString(R.string.empty_weather_stations))
                    binding.empty.subtitle(getString(R.string.empty_select_station))
                    binding.empty.listener(null)
                    binding.empty.setVisible(true)
                    binding.recycler.setVisible(false)
                }
            }
            Status.ERROR -> {
                binding.empty.animation(R.raw.anim_error, false)
                binding.empty.title(getString(R.string.error_generic_message))
                binding.empty.subtitle(devices.message)
                binding.empty.action(getString(R.string.action_retry))
                binding.empty.listener { model.fetch() }
                binding.empty.setVisible(true)
                binding.recycler.setVisible(false)
            }
            Status.LOADING -> {
                binding.recycler.setVisible(false)
                binding.empty.clear()
                binding.empty.animation(R.raw.anim_loading)
                binding.empty.setVisible(true)
            }
        }
    }

    private fun onConfirmClicked(appWidgetId: Int, resultValue: Intent) {
        model.saveWidgetData(appWidgetId)

        /**
         * Broadcast a custom intent in order to update the widget that was just added.
         */
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(
                ARG_WIDGET_TYPE,
                widgetHelper.getWidgetTypeById(
                    AppWidgetManager.getInstance(this@SelectStationActivity), appWidgetId
                ) as Parcelable
            )
            putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
            putExtra(Contracts.ARG_DEVICE, model.getStationSelected())
        }

        this.sendBroadcast(intent)

        /**
         * Initialize the WorkManager.
         */
        CurrentWeatherWidgetWorkerUpdate.initAndStart(
            this,
            appWidgetId,
            model.getStationSelected().id
        )

        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

