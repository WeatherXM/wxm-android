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
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.HistoryUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.LocalDateRange
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import com.weatherxm.util.isToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.LocalDate

class HistoryChartsViewModel(
    val device: UIDevice
) : ViewModel(), KoinComponent {

    companion object {
        const val DATES_BACKOFF = 7L
    }

    private val historyUseCase: HistoryUseCase by inject()
    private val resHelper: ResourcesHelper by inject()
    private val analytics: Analytics by inject()

    private var updateWeatherHistoryJob: Job? = null
    private var currentDateShown: LocalDate = LocalDate.now()

    private val charts = MutableLiveData<Resource<HistoryCharts>>(Resource.loading())
    fun charts(): LiveData<Resource<HistoryCharts>> = charts

    private val onNewDate = MutableLiveData(currentDateShown)
    fun onNewDate(): LiveData<LocalDate> = onNewDate

    var temperatureDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    var precipDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    var windDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    var humidityDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    var pressureDataSets: MutableMap<Int, List<Float>> = mutableMapOf()
    var solarDataSets: MutableMap<Int, List<Float>> = mutableMapOf()

    fun isTodayShown(): Boolean {
        return currentDateShown.isToday()
    }

    fun getCurrentDateShown() = currentDateShown

    fun selectNewDate(newDate: LocalDate) {
        currentDateShown = newDate
        onNewDate.postValue(newDate)
        fetchWeatherHistory()
    }

    fun getLatestChartEntry(lineChartData: LineChartData): Float {
        val firstNaN = lineChartData.entries.firstOrNull { it.y.isNaN() }?.x
        return if (firstNaN != null && firstNaN > 0F) {
            firstNaN - 1
        } else {
            0F
        }
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
                    Timber.d("Returning history charts for [${it.date}]")
                    charts.postValue(Resource.success(it))
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    charts.postValue(
                        Resource.error(
                            resHelper.getString(
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
