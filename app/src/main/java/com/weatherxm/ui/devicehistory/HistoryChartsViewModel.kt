package com.weatherxm.ui.devicehistory

import android.content.Context
import android.util.SparseArray
import androidx.core.util.isNotEmpty
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.InvalidFromDate
import com.weatherxm.data.ApiError.UserError.InvalidToDate
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Failure.NetworkError
import com.weatherxm.data.Resource
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.HistoryCharts
import com.weatherxm.ui.LineChartData
import com.weatherxm.usecases.HistoryUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.getFormattedDate
import com.weatherxm.util.getLast7Days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class HistoryChartsViewModel : ViewModel(), KoinComponent {

    companion object {
        private const val DAYS_TO_FETCH = 7L
    }

    private val historyUseCase: HistoryUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    // A sparse array that maps positions of the dates tabs with their HistoryCharts
    private var posToData = SparseArray<HistoryCharts>()

    // All charts currently visible
    private val onCharts = MutableLiveData<Resource<HistoryCharts>>().apply {
        value = Resource.loading()
    }

    fun onCharts(): LiveData<Resource<HistoryCharts>> = onCharts

    fun getWeatherHistory(device: Device, context: Context) {
        onCharts.postValue(Resource.loading())

        CoroutineScope(Dispatchers.IO).launch {
            val fromDate = getFormattedDate(ZonedDateTime.now().minusDays(DAYS_TO_FETCH).toString())
            val toDate = getFormattedDate(ZonedDateTime.now().toString())
            historyUseCase.getWeatherHistory(device, fromDate, toDate, context)
                .map { historyCharts ->
                    Timber.d("Got History Charts: $historyCharts")
                    mapPositionToData(historyCharts)
                    onCharts.postValue(Resource.success(posToData.get(historyCharts.size - 1)))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    private fun handleFailure(failure: Failure) {
        onCharts.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidFromDate, is InvalidToDate -> R.string.history_invalid_dates
                        is NetworkError -> R.string.network_error
                        else -> R.string.unknown_error
                    }
                )
            )
        )
    }

    private fun mapPositionToData(allCharts: List<HistoryCharts>) {
        posToData = SparseArray(allCharts.size)
        for (i in allCharts.indices) {
            posToData.append(i, allCharts[i])
        }
    }

    fun getDataFromPosition(position: Int) {
        if (posToData.isNotEmpty()) {
            onCharts.postValue(Resource.loading())
            val data = posToData.get(position, null)
            if (data == null) {
                onCharts.postValue(Resource.error(resHelper.getString(R.string.no_charts_found)))
            } else {
                onCharts.postValue(Resource.success(data))
            }
        }
    }

    fun isDataValid(data: LineChartData): Boolean {
        return !data.timestamps.isNullOrEmpty() && !data.entries.isNullOrEmpty()
    }

    fun isDataValid(data: BarChartData): Boolean {
        return !data.timestamps.isNullOrEmpty() && !data.entries.isNullOrEmpty()
    }

    fun getDatesForTabs(): List<String> {
        return getLast7Days(resHelper)
    }
}
