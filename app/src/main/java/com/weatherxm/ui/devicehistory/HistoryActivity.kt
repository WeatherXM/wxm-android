package com.weatherxm.ui.devicehistory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ActivityHistoryBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.getLastTab
import com.weatherxm.ui.common.getSelectedTab
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.devicehistory.HistoryChartsFragment.SwipeRefreshCallback
import com.weatherxm.util.DateTimeHelper.getFormattedRelativeDay
import com.weatherxm.util.LocalDateRange
import com.weatherxm.util.applyInsets
import com.weatherxm.util.applyOnGlobalLayout
import com.weatherxm.util.onTabSelected
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.time.LocalDate

class HistoryActivity : AppCompatActivity(), KoinComponent, SwipeRefreshCallback {
    private lateinit var binding: ActivityHistoryBinding

    private val model: HistoryChartsViewModel by viewModel {
        parametersOf(intent.getParcelableExtra<Device>(Contracts.ARG_DEVICE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        if (model.device.isEmpty()) {
            Timber.d("Could not start HistoryActivity. Device is null.")
            toast(R.string.error_generic_message)
            finish()
            return
        }

        with(binding.toolbar) {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
            subtitle = model.device.address ?: model.device.name
        }

        binding.dateTabs.onTabSelected {
            val date = it.tag as LocalDate
            Timber.d("Date selected on tab: $date")
            model.onDateSelected(date)
        }

        // Listen for changes in the date range
        model.dates().observe(this) { dates ->
            Timber.d("Got new dates: $dates")
            updateDateStrip(dates)
        }
    }

    // Update dates in tab strip
    private fun updateDateStrip(dates: LocalDateRange) {
        // Delete existing tabs
        binding.dateTabs.removeAllTabs()

        // Add tabs for dates
        dates.forEach { date ->
            binding.dateTabs.addTab(
                binding.dateTabs.newTab().apply {
                    text = date.getFormattedRelativeDay(this@HistoryActivity)
                    // Save the LocalDate object as tag in the Tab, so that we can use it later
                    tag = date
                },
                false
            )
        }

        // Select last tab (TODAY) by default
        binding.dateTabs.getLastTab()?.apply {
            view.applyOnGlobalLayout { this.select() }
        }
    }

    override fun onSwipeRefresh() {
        binding.dateTabs.getSelectedTab()?.let {
            val date = it.tag as LocalDate
            model.onDateSelected(date, true)
        }
    }
}
