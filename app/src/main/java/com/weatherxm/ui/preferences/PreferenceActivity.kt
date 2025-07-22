package com.weatherxm.ui.preferences

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityPreferencesBinding
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.components.BaseActivity
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class PreferenceActivity : BaseActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private val model: PreferenceViewModel by viewModel()
    private val sharedPreferences: SharedPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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
