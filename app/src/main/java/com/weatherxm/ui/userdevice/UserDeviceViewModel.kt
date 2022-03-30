package com.weatherxm.ui.userdevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.ui.TokenSummary
import com.weatherxm.ui.UIError
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

@Suppress("TooManyFunctions")
class UserDeviceViewModel : ViewModel(), KoinComponent {

    private val resHelper: ResourcesHelper by inject()
    private val userDeviceUseCase: UserDeviceUseCase by inject()

    private lateinit var device: Device
    private var tokensCurrentState = TokensState.HOUR24
    private var forecastCurrentState = ForecastState.TODAY

    enum class TokensState {
        HOUR24,
        DAYS7,
        DAYS30
    }

    enum class ForecastState {
        TODAY,
        TOMORROW
    }

    private val onDeviceSet = MutableLiveData<Device>()

    private val onLoading = MutableLiveData<Boolean>()

    private val onError = MutableLiveData<UIError>()

    private val onForecast = MutableLiveData<List<HourlyWeather>>()

    private val onTokens = MutableLiveData<TokenSummary>()

    private val onLastRewardTokens = MutableLiveData<Float>()

    fun onDeviceSet(): LiveData<Device> = onDeviceSet

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun onForecast(): LiveData<List<HourlyWeather>> = onForecast

    fun onTokens(): LiveData<TokenSummary> = onTokens

    fun onLastRewardTokens(): LiveData<Float> = onLastRewardTokens

    fun setDevice(device: Device) {
        this.device = device
        onDeviceSet.postValue(this.device)
    }

    fun fetchUserDeviceAllData() {
        onLoading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val userDevice = async {
                userDeviceUseCase.getUserDevice(device.id)
            }

            // This function runs only on onCreate/onSwipeRefresh so we forceRefresh the getTokens
            val tokensDeferred = async {
                userDeviceUseCase.getTokens24H(device.id, true)
            }

            val forecastDeferred = async {
                if (forecastCurrentState == ForecastState.TODAY) {
                    userDeviceUseCase.getTodayForecast(device)
                } else {
                    userDeviceUseCase.getTomorrowForecast(device)
                }
            }

            var errorOnUserDevice = false
            var errorOnTokens = false
            var errorOnForecast = false
            var shouldRetry = false

            val deviceResponse = userDevice.await()
            deviceResponse
                .map {
                    setDevice(it)
                }
                .mapLeft {
                    if (it == Failure.NetworkError) {
                        shouldRetry = true
                    }
                    errorOnUserDevice = true
                }

            val tokens = tokensDeferred.await()
            tokens
                .map {
                    onLastRewardTokens.postValue(it)
                }
                .mapLeft {
                    if (it == Failure.NetworkError) {
                        shouldRetry = true
                    }
                    errorOnTokens = true
                }

            val forecast = forecastDeferred.await()
            forecast
                .map {
                    onForecast.postValue(addCurrentToForecast(device.currentWeather, it))
                }
                .mapLeft {
                    if (it == Failure.NetworkError) {
                        shouldRetry = true
                    }
                    errorOnForecast = true
                }

            handleErrors(errorOnUserDevice, errorOnTokens, errorOnForecast, shouldRetry)
            onLoading.postValue(false)
        }
    }

    fun fetchForecast(newState: ForecastState) {
        onLoading.postValue(true)
        forecastCurrentState = newState
        CoroutineScope(Dispatchers.IO).launch {
            when (forecastCurrentState) {
                ForecastState.TODAY -> {
                    userDeviceUseCase.getTodayForecast(device)
                        .map {
                            Timber.d("Got Short Term Forecast: $it")
                            onForecast.postValue(addCurrentToForecast(device.currentWeather, it))
                        }
                        .mapLeft {
                            handleForecastFailure(it)
                        }
                }
                ForecastState.TOMORROW -> {
                    userDeviceUseCase.getTomorrowForecast(device)
                        .map {
                            Timber.d("Got Short Term Forecast: $it")
                            if (it.isEmpty()) {
                                onError.postValue(
                                    UIError(resHelper.getString(R.string.forecast_empty), null)
                                )
                                onForecast.postValue(it)
                            } else {
                                onForecast.postValue(
                                    addCurrentToForecast(device.currentWeather, it)
                                )
                            }
                        }
                        .mapLeft {
                            handleForecastFailure(it)
                        }
                }
            }
            onLoading.postValue(false)
        }
    }

    private fun handleForecastFailure(failure: Failure) {
        val uiError = UIError("", null)
        when (failure) {
            is ApiError.UserError.InvalidFromDate, is ApiError.UserError.InvalidToDate -> {
                uiError.errorMessage = resHelper.getString(R.string.error_forecast_generic_message)
            }
            is Failure.NetworkError -> {
                uiError.errorMessage = resHelper.getString(R.string.error_network)
                uiError.retryFunction = { (::fetchForecast)(forecastCurrentState) }
            }
            else -> {
                uiError.errorMessage = resHelper.getString(R.string.error_unknown)
            }
        }
        onError.postValue(uiError)
    }

    fun fetchTokenDetails(newState: TokensState) {
        onLoading.postValue(true)
        tokensCurrentState = newState
        CoroutineScope(Dispatchers.IO).launch {
            when (tokensCurrentState) {
                TokensState.HOUR24 -> {
                    userDeviceUseCase.getTokens24H(device.id)
                        .map { onLastRewardTokens.postValue(it) }
                        .mapLeft { handleTokenFailure(it) }
                }
                TokensState.DAYS7 -> {
                    userDeviceUseCase.getTokens7D(device.id)
                        .map { onTokens.postValue(it) }
                        .mapLeft { handleTokenFailure(it) }
                }
                TokensState.DAYS30 -> {
                    userDeviceUseCase.getTokens30D(device.id)
                        .map { onTokens.postValue(it) }
                        .mapLeft { handleTokenFailure(it) }
                }
            }
            onLoading.postValue(false)
        }
    }

    /*
    * Needed to show 1 decimal point at temperature only if the first tile on the "Today" state
    * is selected - which means only on current weather.
    * On forecast tiles show 0 decimal points.
     */
    fun temperatureDecimalsToShow(selectedPosition: Int): Int {
        return if (selectedPosition == 0 && forecastCurrentState == ForecastState.TODAY) {
            1
        } else {
            0
        }
    }

    private fun fetchUserDevice() {
        onLoading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            userDeviceUseCase.getUserDevice(device.id)
                .map {
                    Timber.d("Got User Device: $it")
                    setDevice(it)
                }
                .mapLeft {
                    val uiError = UIError("", null)
                    when (it) {
                        is ApiError.DeviceNotFound -> {
                            uiError.errorMessage =
                                resHelper.getString(R.string.error_user_device_not_found)
                        }
                        is Failure.NetworkError -> {
                            uiError.errorMessage = resHelper.getString(R.string.error_network)
                            uiError.retryFunction = ::fetchUserDevice
                        }
                        else -> {
                            uiError.errorMessage = resHelper.getString(R.string.error_unknown)
                        }
                    }
                    onError.postValue(uiError)
                }
            onLoading.postValue(false)
        }
    }

    @Suppress("ComplexMethod")
    private fun handleErrors(
        errorDevice: Boolean,
        errorToken: Boolean,
        errorForecast: Boolean,
        shouldRetry: Boolean
    ) {
        val uiError = UIError("", null)

        // This if checks if 2/3 error states are true, so we fetch all the data again
        // Otherwise we have either 0/3 or 1/3 error states so just check them one by one
        @Suppress("ComplexCondition")
        if ((errorDevice && (errorToken || errorForecast)) || (errorToken && errorForecast)) {
            uiError.errorMessage = resHelper.getString(R.string.error_user_device_data_failed)

            if (shouldRetry) {
                uiError.retryFunction = ::fetchUserDeviceAllData
            }
        } else if (errorDevice) {
            uiError.errorMessage =
                resHelper.getString(R.string.error_user_device_current_weather_failed)

            if (shouldRetry) {
                uiError.retryFunction = ::fetchUserDevice
            }
        } else if (errorToken) {
            uiError.errorMessage = resHelper.getString(R.string.error_user_device_token_failed)

            if (shouldRetry) {
                uiError.retryFunction = { (::fetchTokenDetails)(tokensCurrentState) }
            }
        } else if (errorForecast) {
            uiError.errorMessage = resHelper.getString(R.string.error_user_device_forecast_failed)

            if (shouldRetry) {
                uiError.retryFunction = { (::fetchForecast)(forecastCurrentState) }
            }
        }

        if (errorToken || errorForecast || errorDevice) {
            onError.postValue(uiError)
        }
    }

    private fun handleTokenFailure(failure: Failure) {
        val uiError = UIError("", null)
        when (failure) {
            is ApiError.GenericError -> {
                uiError.errorMessage =
                    failure.message ?: resHelper.getString(R.string.error_unknown)
            }
            is Failure.NetworkError -> {
                uiError.errorMessage = resHelper.getString(R.string.error_network)
                uiError.retryFunction = { (::fetchTokenDetails)(tokensCurrentState) }
            }
            else -> {
                uiError.errorMessage = resHelper.getString(R.string.error_unknown)
            }
        }
        onError.postValue(uiError)
    }

    private fun addCurrentToForecast(
        currentWeather: HourlyWeather?,
        forecastTimeseries: List<HourlyWeather>?
    ): List<HourlyWeather> {
        forecastTimeseries?.let {
            val currentAndForecast =
                if (currentWeather == null || forecastCurrentState != ForecastState.TODAY) {
                    forecastTimeseries
                } else {
                    val listToReturn = forecastTimeseries.toMutableList()
                    listToReturn.add(0, currentWeather)
                    listToReturn.toList()
                }
            return currentAndForecast
        }

        return if (currentWeather != null) {
            listOf(currentWeather)
        } else {
            listOf()
        }
    }
}
