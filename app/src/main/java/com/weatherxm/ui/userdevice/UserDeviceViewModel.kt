package com.weatherxm.ui.userdevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.ServerError
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

    fun onDeviceSet(): LiveData<Device> = onDeviceSet

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun onForecast(): LiveData<List<HourlyWeather>> = onForecast

    fun onTokens(): LiveData<TokenSummary> = onTokens

    fun setDevice(device: Device) {
        this.device = device
        onDeviceSet.postValue(this.device)
    }

    fun getUserDeviceData() {
        onLoading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            val tokensDeferred = async {
                userDeviceUseCase.getTokensSummary24H(device.id)
            }

            val forecastDeferred = async {
                userDeviceUseCase.getTodayForecast(device)
            }

            var errorOnTokens = false
            var errorOnForecast = false
            var shouldRetry = false

            val tokens = tokensDeferred.await()
            tokens
                .map {
                    onTokens.postValue(it)
                }
                .mapLeft {
                    Timber.d("Got error when fetching tokens: $it")
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
                    Timber.d("Got error when fetching weather forecast: $it")
                    if (it == Failure.NetworkError) {
                        shouldRetry = true
                    }
                    errorOnForecast = true
                }

            // TODO: Optimize/Beautify this code
            val uiError = UIError("", null)
            if (errorOnTokens && errorOnForecast) {
                val tokenTitle = resHelper.getString(R.string.token_failed)
                val forecastTitle = resHelper.getString(R.string.forecast_failed)

                uiError.errorMessage = "$tokenTitle\n$forecastTitle"

                if (shouldRetry) {
                    uiError.retryFunction = ::getUserDeviceData
                }
            } else if (errorOnTokens) {
                uiError.errorMessage = resHelper.getString(R.string.token_failed)

                if (shouldRetry) {
                    uiError.retryFunction = ::fetchTokenDetails
                }
            } else if (errorOnForecast) {
                uiError.errorMessage = resHelper.getString(R.string.forecast_failed)

                if (shouldRetry) {
                    uiError.retryFunction = ::getForecast
                }
            }

            if (errorOnTokens || errorOnForecast) {
                onError.postValue(uiError)
            }
            onLoading.postValue(false)
        }
    }

    fun setForecastCurrentState(newState: ForecastState) {
        forecastCurrentState = newState
        getForecast()
    }

    private fun getForecast() {
        onLoading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            when (forecastCurrentState) {
                ForecastState.TODAY -> {
                    userDeviceUseCase.getTodayForecast(device)
                        .map {
                            Timber.d("Got Short Term Forecast: $it")
                            onForecast.postValue(addCurrentToForecast(device.currentWeather, it))
                        }
                        .mapLeft {
                            Timber.d("Got error: $it")
                            val uiError = UIError("", null)
                            when (it) {
                                is Failure.NetworkError -> {
                                    uiError.errorMessage =
                                        resHelper.getString(R.string.network_error)
                                    uiError.retryFunction = ::getForecast
                                }
                                is ServerError -> {
                                    uiError.errorMessage =
                                        resHelper.getString(R.string.server_error)
                                }
                                is Failure.UnknownError -> {
                                    uiError.errorMessage =
                                        resHelper.getString(R.string.unknown_error)
                                }
                            }
                            onError.postValue(uiError)
                        }
                }
                ForecastState.TOMORROW -> {
                    userDeviceUseCase.getTomorrowForecast(device)
                        .map {
                            Timber.d("Got Short Term Forecast: $it")
                            onForecast.postValue(addCurrentToForecast(device.currentWeather, it))
                        }
                        .mapLeft {
                            Timber.d("Got error: $it")
                            val uiError = UIError("", null)
                            when (it) {
                                is Failure.NetworkError -> {
                                    uiError.errorMessage =
                                        resHelper.getString(R.string.network_error)
                                    uiError.retryFunction = ::getForecast
                                }
                                is ServerError -> {
                                    uiError.errorMessage =
                                        resHelper.getString(R.string.server_error)
                                }
                                is Failure.UnknownError -> {
                                    uiError.errorMessage =
                                        resHelper.getString(R.string.unknown_error)
                                }
                            }
                            onError.postValue(uiError)
                        }
                }
            }
            onLoading.postValue(false)
        }
    }

    fun setTokenCurrentState(newState: TokensState) {
        tokensCurrentState = newState
        fetchTokenDetails()
    }

    private fun fetchTokenDetails() {
        onLoading.postValue(true)
        CoroutineScope(Dispatchers.IO).launch {
            when (tokensCurrentState) {
                TokensState.HOUR24 -> {
                    userDeviceUseCase.getTokensSummary24H(device.id)
                        .map { handleTokenSuccess(it) }
                        .mapLeft { handleTokenFailure(it) }
                }
                TokensState.DAYS7 -> {
                    userDeviceUseCase.getTokensSummary7D(device.id)
                        .map { handleTokenSuccess(it) }
                        .mapLeft { handleTokenFailure(it) }
                }
                TokensState.DAYS30 -> {
                    userDeviceUseCase.getTokensSummary30D(device.id)
                        .map { handleTokenSuccess(it) }
                        .mapLeft { handleTokenFailure(it) }
                }
            }
            onLoading.postValue(false)
        }
    }

    private fun handleTokenSuccess(tokenSummary: TokenSummary) {
        Timber.d("Got Tokens: $tokenSummary")
        onTokens.postValue(tokenSummary)
    }

    private fun handleTokenFailure(failure: Failure) {
        Timber.d("Got error: $failure")
        val uiError = UIError("", null)
        when (failure) {
            is Failure.NetworkError -> {
                uiError.errorMessage = resHelper.getString(R.string.network_error)
                uiError.retryFunction = ::fetchTokenDetails
            }
            is ServerError -> {
                uiError.errorMessage = resHelper.getString(R.string.server_error)
            }
            is Failure.UnknownError -> {
                uiError.errorMessage = resHelper.getString(R.string.unknown_error)
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
