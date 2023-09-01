package com.weatherxm.ui.widgets.selectstation

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityWidgetSelectStationBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UserDevices
import com.weatherxm.ui.common.setVisible
import com.weatherxm.ui.widgets.currentweather.CurrentWeatherWidgetWorkerUpdate
import com.weatherxm.util.Analytics
import com.weatherxm.util.WidgetHelper
import com.weatherxm.util.applyInsets
import com.weatherxm.util.onTabSelected
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SelectStationActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityWidgetSelectStationBinding
    private val model: SelectStationViewModel by viewModels()
    private val analytics: Analytics by inject()
    private val widgetHelper: WidgetHelper by inject()

    private lateinit var adapter: SelectStationAdapter

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

        adapter = SelectStationAdapter {
            model.setStationSelected(it)
            binding.confirmBtn.isEnabled = true
        }
        binding.recycler.adapter = adapter

        initTabs()

        binding.navigationTabs.onTabSelected {
            if (model.getUserDevices() == null) return@onTabSelected
            onTabSelected(
                when (it.position) {
                    0 -> model.getUserDevices()?.devices
                    1 -> model.getUserDevices()?.getOwnedDevices()
                    2 -> model.getUserDevices()?.getFollowedDevices()
                    else -> mutableListOf()
                }
            )
        }

        model.devices().observe(this) {
            onDevices(it)
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

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.WIDGET_SELECT_STATION,
            SelectStationActivity::class.simpleName
        )
    }

    private fun onTabSelected(devicesOfTab: List<UIDevice>?) {
        if (devicesOfTab.isNullOrEmpty()) {
            onEmptyState()
        } else {
            adapter.submitList(devicesOfTab)
            binding.empty.setVisible(false)
            binding.recycler.setVisible(true)
        }
    }

    private fun onDevices(userDevices: Resource<UserDevices>) {
        when (userDevices.status) {
            Status.SUCCESS -> {
                if (!userDevices.data?.devices.isNullOrEmpty()) {
                    binding.navigationTabs.getTabAt(0)?.text = getString(
                        R.string.total_with_placeholder,
                        userDevices.data?.totalDevices?.toString() ?: "?"
                    )
                    binding.navigationTabs.getTabAt(1)?.text = getString(
                        R.string.owned_with_placeholder,
                        userDevices.data?.ownedDevices?.toString() ?: "?"
                    )
                    binding.navigationTabs.getTabAt(2)?.text = getString(
                        R.string.favorites_with_placeholder,
                        userDevices.data?.followedDevices?.toString() ?: "?"
                    )

                    when (binding.navigationTabs.selectedTabPosition) {
                        1 -> onTabSelected(model.getUserDevices()?.getOwnedDevices())
                        2 -> onTabSelected(model.getUserDevices()?.getFollowedDevices())
                        else -> {
                            adapter.submitList(userDevices.data?.devices)
                            adapter.notifyDataSetChanged()
                            binding.empty.setVisible(false)
                            binding.recycler.setVisible(true)
                        }
                    }
                } else {
                    onEmptyState()
                }
            }
            Status.ERROR -> {
                binding.empty.animation(R.raw.anim_error, false)
                binding.empty.title(getString(R.string.error_generic_message))
                binding.empty.subtitle(userDevices.message)
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

    private fun onEmptyState() {
        binding.empty.animation(R.raw.anim_empty_devices, false)
        binding.empty.title(getString(R.string.empty_weather_stations))
        binding.empty.subtitle(getString(R.string.empty_select_station))
        binding.empty.listener(null)
        binding.empty.visibility = View.VISIBLE
        binding.recycler.visibility = View.GONE
    }

    private fun initTabs() {
        with(binding.navigationTabs) {
            addTab(
                newTab().apply {
                    text = getString(R.string.total)
                },
                true
            )
            addTab(
                newTab().apply {
                    text = getString(R.string.owned)
                }
            )
            addTab(
                newTab().apply {
                    text = getString(R.string.favorites)
                }
            )
        }
    }

    private fun onConfirmClicked(appWidgetId: Int, resultValue: Intent) {
        model.saveWidgetData(appWidgetId)

        /**
         * Broadcast a custom intent in order to update the widget that was just added.
         */
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra(
            ARG_WIDGET_TYPE,
            widgetHelper.getWidgetTypeById(AppWidgetManager.getInstance(this), appWidgetId)
        )
        intent.putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
        intent.putExtra(Contracts.ARG_DEVICE, model.getStationSelected())

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

