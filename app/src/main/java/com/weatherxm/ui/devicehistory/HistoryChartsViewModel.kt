package com.weatherxm.ui.devicehistory

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.InvalidFromDate
import com.weatherxm.data.ApiError.UserError.InvalidToDate
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.HistoryCharts
import com.weatherxm.usecases.HistoryUseCase
import com.weatherxm.util.DateTimeHelper.getDateRangeFromToday
import com.weatherxm.util.DateTimeHelper.getRelativeDayFromLocalDate
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HistoryChartsViewModel : ViewModel(), KoinComponent {

    companion object {
        // 8 days in the past (including today)
        private const val DAYS_TO_FETCH = -8
    }

    private val historyUseCase: HistoryUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    /*
     * The selected day.
     * Need to save it in case the user picks another day before the data is fetched
     * so we have this info to show him the correct charts
     */
    private var selectedDay: String = ""

    private val dates = MutableLiveData(getDateRangeFromToday(DAYS_TO_FETCH))
    fun dates() = dates

    /*
    * A hashmap that contains as keys the relative days shown in the UI
    * and as values the historical charts for that day
    *
    * IMPORTANT: As said before the keys are the same relative days as shown in the UI.
    *
    * Explanation:
    *  - We use relative days as keys produced by getRelativeDayFromLocalDate()
    *  - The currentDates has relative days
    *       by using getLast7Days() which uses internally getRelativeDayFromLocalDate()
    *  - The tabs of the UI are the values of the currentDates
    *  - The selectedDay is the text of the selected tab
    *  - So we use dataForDates[selectedDay] in our code to get the charts for this day
     */
    private var dataForDates: HashMap<String, HistoryCharts?> = HashMap()

    // All charts currently visible
    private val onCharts = MutableLiveData<Resource<HistoryCharts>>().apply {
        value = Resource.loading()
    }

    fun onCharts(): LiveData<Resource<HistoryCharts>> = onCharts

    fun getWeatherHistory(device: Device, context: Context, isSwipeRefresh: Boolean = false) {
        onCharts.postValue(Resource.loading())

        // Get new date range
        val newDates = getDateRangeFromToday(DAYS_TO_FETCH)

        viewModelScope.launch(Dispatchers.IO) {
            val fromDate = newDates.first().toString()
            val toDate = newDates.last().toString()

            // Fetch fresh data
            historyUseCase.getWeatherHistory(device, fromDate, toDate, context)
                .map { historyCharts ->
                    Timber.d("Got History Charts")
                    mapPositionToData(historyCharts)
                    getDataForSelectedDay()
                }
                .mapLeft {
                    handleFailure(it)
                }

            // If this is a forced refresh and date range has changed, post updated dates
            if (isSwipeRefresh && newDates != dates.value) {
                Timber.d("Dates have changed. Posting update.")
                dates.postValue(newDates)
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        onCharts.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidFromDate, is InvalidToDate -> {
                            R.string.error_history_generic_message
                        }
                        else -> failure.getDefaultMessageResId()
                    }
                )
            )
        )
    }

    private fun mapPositionToData(allCharts: List<HistoryCharts>) {
        allCharts.forEach {
            val relativeDate = getRelativeDayFromLocalDate(resHelper, it.date, false)
            dataForDates[relativeDate] = it
        }
    }

    fun setSelectedDay(day: String) {
        selectedDay = day
        getDataForSelectedDay()
    }

    private fun getDataForSelectedDay() {
        if (dataForDates.isNotEmpty()) {
            onCharts.postValue(Resource.loading())
            val data = dataForDates[selectedDay]
            onCharts.postValue(Resource.success(data))
        }
    }
}
