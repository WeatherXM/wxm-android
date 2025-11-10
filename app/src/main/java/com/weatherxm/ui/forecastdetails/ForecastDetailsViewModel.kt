package com.weatherxm.ui.forecastdetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.datasource.LocationsDataSource.Companion.MAX_AUTH_LOCATIONS
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.ui.common.UILocation
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.ChartsUseCase
import com.weatherxm.usecases.ForecastUseCase
import com.weatherxm.usecases.LocationsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

@Suppress("LongParameterList")
class ForecastDetailsViewModel(
    val device: UIDevice,
    val location: UILocation,
    val hasFreeTrialAvailable: Boolean,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    private val authUseCase: AuthUseCase,
    private val chartsUseCase: ChartsUseCase,
    private val forecastUseCase: ForecastUseCase,
    private val locationsUseCase: LocationsUseCase,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val onForecastLoaded = MutableLiveData<Resource<Unit>>()

    fun onForecastLoaded(): LiveData<Resource<Unit>> = onForecastLoaded

    private var forecast: UIForecast = UIForecast.empty()

    fun forecast() = forecast

    fun fetchDeviceForecast() {
        onForecastLoaded.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            forecastUseCase.getDeviceForecast(device).onRight {
                Timber.d("Got forecast details for device forecast")
                forecast = it
                if (it.isEmpty()) {
                    onForecastLoaded.postValue(
                        Resource.error(resources.getString(R.string.forecast_empty))
                    )
                } else {
                    onForecastLoaded.postValue(Resource.success(Unit))
                }
            }.onLeft {
                forecast = UIForecast.empty()
                analytics.trackEventFailure(it.code)
                handleForecastFailure(it)
            }
        }
    }

    fun fetchLocationForecast() {
        onForecastLoaded.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            forecastUseCase.getLocationForecast(location.coordinates)
                .onRight {
                    Timber.d("Got forecast details for location forecast")
                    forecast = it
                    if (it.isEmpty()) {
                        onForecastLoaded.postValue(
                            Resource.error(resources.getString(R.string.forecast_empty))
                        )
                    } else {
                        onForecastLoaded.postValue(Resource.success(Unit))
                    }
                }
                .onLeft {
                    forecast = UIForecast.empty()
                    analytics.trackEventFailure(it.code)
                    handleForecastFailure(it)
                }
        }
    }

    private fun handleForecastFailure(failure: Failure) {
        onForecastLoaded.postValue(
            Resource.error(
                when (failure) {
                    is ApiError.UserError.InvalidFromDate, is ApiError.UserError.InvalidToDate -> {
                        resources.getString(R.string.error_forecast_generic_message)
                    }
                    is ApiError.UserError.InvalidTimezone -> {
                        resources.getString(R.string.error_forecast_invalid_timezone)
                    }
                    else -> failure.getDefaultMessage(R.string.error_reach_out_short)
                }
            )
        )
    }

    fun getSelectedDayPosition(selectedISODate: String?): Int {
        if (selectedISODate == null) {
            return 0
        }

        val selectedLocalDate = LocalDate.parse(selectedISODate)
        val position = forecast.forecastDays.indexOfFirst {
            it.date == selectedLocalDate
        }

        return if (position == -1) {
            /**
             * Temporary code so we can figure out why some crashes occur, in which cases/dates
             */
            val allDates = forecast.forecastDays.joinToString(" : ") { it.date.toString() }
            Timber.e("Could not find ($selectedISODate - $selectedLocalDate) in: $allDates")
            0
        } else {
            position
        }
    }

    /**
     * 2. Show first the 07:00am hour or
     * 3. Show first the first available hour
     */
    @Suppress("MagicNumber")
    fun getDefaultHourPosition(hourlies: List<HourlyWeather>): Int {
        return hourlies.indexOf(
            hourlies.firstOrNull {
                it.timestamp.hour == 7
            } ?: hourlies[0]
        )
    }

    fun getCharts(forecastDay: UIForecastDay): Charts {
        Timber.d("Returning forecast charts for [${forecastDay.date}]")
        return chartsUseCase.createHourlyCharts(
            forecastDay.date, forecastDay.hourlyWeather ?: mutableListOf()
        )
    }

    fun canSaveMoreLocations(): Boolean {
        val isLoggedIn = authUseCase.isLoggedIn()
        val savedLocations = locationsUseCase.getSavedLocations()
        return (isLoggedIn && savedLocations.size < MAX_AUTH_LOCATIONS) ||
            (!isLoggedIn && savedLocations.isEmpty())
    }

    fun isLoggedIn() = authUseCase.isLoggedIn()

    fun addSavedLocation() {
        location.isSaved = true
        locationsUseCase.addSavedLocation(location.coordinates)
    }

    fun removeSavedLocation() {
        location.isSaved = false
        locationsUseCase.removeSavedLocation(location.coordinates)
    }
}
