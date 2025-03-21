package com.weatherxm.ui.devicehistory

import android.os.Bundle
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityHistoryBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.devicehistory.HistoryChartsFragment.SwipeRefreshCallback
import com.weatherxm.ui.devicehistory.HistoryViewModel.Companion.DATES_BACKOFF
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.time.LocalDate

class HistoryActivity : BaseActivity(), SwipeRefreshCallback {
    private lateinit var binding: ActivityHistoryBinding

    private val model: HistoryViewModel by viewModel {
        parametersOf(intent.parcelable<UIDevice>(Contracts.ARG_DEVICE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            return@setOnMenuItemClickListener if (menuItem.itemId == R.id.select_date) {
                navigator.showDatePicker(
                    this,
                    selectedDate = model.getCurrentDateShown(),
                    dateStart = model.device.claimedAt?.toLocalDate()
                ) {
                    model.selectNewDate(it)
                }
                true
            } else {
                false
            }
        }

        model.onNewDate().observe(this) {
            binding.dateNavigator.setCurrentDate(it, true)
        }

        binding.dateNavigator.init(
            model.device.claimedAt?.toLocalDate() ?: LocalDate.now().minusDays(DATES_BACKOFF),
            LocalDate.now()
        ) {
            model.selectNewDate(it)
        }

        model.fetchWeatherHistory(true)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.HISTORY, classSimpleName())
    }

    override fun onSwipeRefresh() {
        model.fetchWeatherHistory(true)
    }
}
