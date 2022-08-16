package com.weatherxm.ui.devicehistory

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.databinding.ActivityHistoryBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
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
            model.setSelectedTab(it.position)
        }

        model.onUpdateDates().observe(this) {
            if (it) {
                updateDates()
            }
        }

        updateDates()

        navigator.showHistoryCharts(supportFragmentManager, device)
    }

    private fun updateDates() {
        val currentEarliestDate = model.getEarliestDate()
        val tabsFromModel = model.getDatesForTabs()

        /*
        * If the date tabs are not being initialized, create and add all of them and
        * select the latest tab.
        *
        * If the date tabs exist, then check the current earliest date  if it is the same
        * as the new earliest date.
        * If it isn't, then that means we have moved one day forward so we remove all date tabs
        * and recreate them and select again the latest tab to make it visible that the new day's
        * data just arrived and the dates at the top changed.
        */
        if (binding.dateTabs.tabCount == 0) {
            createDatesAndSelectLatest(tabsFromModel)
        } else if (tabsFromModel[0] != currentEarliestDate) {
            binding.dateTabs.removeAllTabs()
            createDatesAndSelectLatest(tabsFromModel)
        }
    }

    private fun createDatesAndSelectLatest(newTabs: List<String>) {
        newTabs.forEach { date ->
            binding.dateTabs.createAndAddTab(date)
        }

        // Select last tab (TODAY) by default
        binding.dateTabs.getTabAt(binding.dateTabs.tabCount - 1)?.apply {
            view.applyOnGlobalLayout { this.select() }
        }
    }
}

