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
            onBackPressed()
        }

        val device = intent?.extras?.getParcelable<Device>(ARG_DEVICE)
        if (device == null) {
            Timber.d("Could not start HistoryActivity. Device is null.")
            toast(R.string.unknown_error)
            finish()
            return
        }

        binding.toolbar.subtitle = device.address ?: device.name

        binding.dateTabs.onTabSelected {
            model.getDataFromPosition(it.position)
        }

        updateDates()

        navigator.showHistoryCharts(supportFragmentManager, device)
    }

    private fun updateDates() {
        val lastDays = model.getDatesForTabs()

        lastDays.forEach { date ->
            val newDateTab = binding.dateTabs.newTab()
            newDateTab.text = date
            binding.dateTabs.addTab(newDateTab)
        }
        val tab = binding.dateTabs.getTabAt(binding.dateTabs.tabCount - 1)
        tab?.view?.viewTreeObserver?.addOnGlobalLayoutListener {
            tab.select()
        }
    }
}

