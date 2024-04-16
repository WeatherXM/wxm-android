package com.weatherxm.ui.devicehistory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.InvalidFromDate
import com.weatherxm.data.ApiError.UserError.InvalidToDate
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.ChartsUseCase
import com.weatherxm.usecases.HistoryUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import com.weatherxm.util.isToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

class HistoryChartsViewModel(
    val device: UIDevice,
    private val historyUseCase: HistoryUseCase,
    private val chartsUseCase: ChartsUseCase,
    private val resources: Resources,
    private val analytics: Analytics
) : ViewModel() {

    companion object {
        const val DATES_BACKOFF = 7L
    }

    private var updateWeatherHistoryJob: Job? = null
    private var currentDateShown: LocalDate = LocalDate.now()

    private val charts = MutableLiveData<Resource<Charts>>(Resource.loading())
    fun charts(): LiveData<Resource<Charts>> = charts

    private val onNewDate = MutableLiveData(currentDateShown)
    fun onNewDate(): LiveData<LocalDate> = onNewDate

    fun isTodayShown(): Boolean {
        return currentDateShown.isToday()
    }

    fun getCurrentDateShown() = currentDateShown

    fun selectNewDate(newDate: LocalDate) {
        currentDateShown = newDate
        onNewDate.postValue(newDate)
        fetchWeatherHistory()
    }

    fun fetchWeatherHistory(forceUpdate: Boolean = false) {
        // If this the first data update, force a network update
        val shouldForceUpdate = forceUpdate || updateWeatherHistoryJob == null

        updateWeatherHistoryJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running history job.")
            }
        }

        updateWeatherHistoryJob = viewModelScope.launch(Dispatchers.IO) {
            // Post loading status
            charts.postValue(Resource.loading())

            Timber.d("Fetching data for $currentDateShown [forced=$shouldForceUpdate]")

            analytics.trackEventSelectContent(
                Analytics.ParamValue.HISTORY_DAY.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, device.id),
                Pair(Analytics.CustomParam.DATE.paramName, currentDateShown.toString())
            )

            // Fetch fresh data
            historyUseCase.getWeatherHistory(device, currentDateShown, shouldForceUpdate)
                .onRight {
                    val historyCharts = chartsUseCase.createHourlyCharts(currentDateShown, it)
                    Timber.d("Returning history charts for [${historyCharts.date}]")
                    charts.postValue(Resource.success(historyCharts))
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    charts.postValue(
                        Resource.error(
                            resources.getString(
                                when (it) {
                                    is InvalidFromDate, is InvalidToDate -> {
                                        R.string.error_history_generic_message
                                    }

                                    else -> it.getDefaultMessageResId()
                                }
                            )
                        )
                    )
                }
        }

        updateWeatherHistoryJob?.invokeOnCompletion {
            if (it is CancellationException) {
                Timber.d("Cancelled running history job.")
            }
        }
    }

    fun getDataSetIndexForHighlight(
        x: Float,
        dataSet: MutableMap<Int, List<Float>>,
        fallback: Int
    ): Int {
        return dataSet.filterValues {
            it.contains(x)
        }.keys.let {
            if (it.isNotEmpty()) {
                it.first()
            } else {
                fallback
            }
        }
    }
}
