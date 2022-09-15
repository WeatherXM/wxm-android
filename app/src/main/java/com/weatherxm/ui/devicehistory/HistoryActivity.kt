package com.weatherxm.ui.devicehistory

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ActivityHistoryBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.util.DateTimeHelper
import com.weatherxm.util.DateTimeHelper.LocalDateRange
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.applyInsets
import com.weatherxm.util.applyOnGlobalLayout
import com.weatherxm.util.createAndAddTab
import com.weatherxm.util.onTabSelected
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HistoryActivity : AppCompatActivity(), KoinComponent {

    private lateinit var binding: ActivityHistoryBinding
    private val model: HistoryChartsViewModel by viewModels()
    private val navigator: Navigator by inject()
    private val resHelper: ResourcesHelper by inject()

    companion object {
        const val ARG_DEVICE = "device"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val device = intent?.extras?.getParcelable<Device>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start HistoryActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        binding.toolbar.subtitle = device.address ?: device.name

        binding.dateTabs.onTabSelected {
            model.setSelectedDay(it.text.toString())
        }

        navigator.showHistoryCharts(supportFragmentManager, device)

        // Listen for changes in the date range
        model.dates().observe(this) { dates ->
            updateDateStrip(dates)
        }
    }

    // Update dates in tab strip
    private fun updateDateStrip(dates: LocalDateRange) {
        // Delete existing tabs
        binding.dateTabs.removeAllTabs()
        // Add tabs for dates
        dates.forEach { date ->
            binding.dateTabs.createAndAddTab(
                DateTimeHelper.getRelativeDayFromLocalDate(resHelper, date)
            )
        }
        // Select last tab (TODAY) by default
        binding.dateTabs.getTabAt(binding.dateTabs.tabCount - 1)?.apply {
            view.applyOnGlobalLayout { this.select() }
        }
    }
}

