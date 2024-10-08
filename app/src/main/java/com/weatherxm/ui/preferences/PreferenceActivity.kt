package com.weatherxm.ui.preferences

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityPreferencesBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.WidgetHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class PreferenceActivity : BaseActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private val model: PreferenceViewModel by viewModel()
    private val widgetHelper: WidgetHelper by inject()
    private val sharedPreferences: SharedPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        model.onLogout().observe(this) { hasLoggedOut ->
            if (hasLoggedOut) {
                analytics.trackEventSelectContent(AnalyticsService.ParamValue.LOGOUT.paramValue)
                widgetHelper.getWidgetIds().onRight {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    val ids = it.map { id ->
                        id.toInt()
                    }
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids.toIntArray())
                    intent.putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
                    intent.putExtra(Contracts.ARG_WIDGET_SHOULD_LOGIN, true)
                    this.sendBroadcast(intent)
                }
                navigator.showStartup(this)
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(model.onPreferencesChanged)
    }

    override fun onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(model.onPreferencesChanged)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.SETTINGS, classSimpleName())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
