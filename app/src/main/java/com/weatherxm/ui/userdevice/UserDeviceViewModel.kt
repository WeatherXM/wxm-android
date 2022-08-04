package com.weatherxm.ui.userdevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import com.weatherxm.ui.TokenInfo
import com.weatherxm.ui.UIError
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.DateTimeHelper.isTomorrow
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime


@Suppress("TooManyFunctions")
class UserDeviceViewModel : ViewModel(), KoinComponent {

    private val resHelper: ResourcesHelper by inject()
    private val userDeviceUseCase: UserDeviceUseCase by inject()

    private lateinit var device: Device

    private val onDeviceSet = MutableLiveData<Device>()

    private val onEditNameChange = MutableLiveData<Boolean>()

    private val onLoading = MutableLiveData<Boolean>()

    private val onError = MutableLiveData<UIError>()

    private val onForecast = MutableLiveData<List<HourlyWeather>>()

    private val onTokens = MutableLiveData<TokenInfo>()

    private val onUnitPreferenceChanged = MutableLiveData(false)

    fun onDeviceSet(): LiveData<Device> = onDeviceSet

    fun onEditNameChange(): LiveData<Boolean> = onEditNameChange

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun onForecast(): LiveData<List<HourlyWeather>> = onForecast

    fun onTokens(): LiveData<TokenInfo> = onTokens

    fun onUnitPreferenceChanged(): LiveData<Boolean> = onUnitPreferenceChanged

    fun setDevice(device: Device) {
        this.device = device
        onDeviceSet.postValue(this.device)
    }

    fun getDevice(): Device {
        return device
    }

    fun fetchUserDeviceAllData(forceRefresh: Boolean = false) {
        onLoading.postValue(true)

        viewModelScope.launch(Dispatchers.IO) {
            val userDevice = async {
                userDeviceUseCase.getUserDevice(device.id)
            }

            val tokensDeferred = async {
                userDeviceUseCase.getTokenInfoLast30D(device.id)
            }

            val forecastDeferred = async {
                userDeviceUseCase.getTodayAndTomorrowForecast(device, forceRefresh)
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
                    if (it == NoConnectionError || it == ConnectionTimeoutError) {
                        shouldRetry = true
                    }
                    errorOnUserDevice = true
                }

            val tokens = tokensDeferred.await()
            tokens
                .map {
                    onTokens.postValue(it)
                }
                .mapLeft {
                    if (it == NoConnectionError || it == ConnectionTimeoutError) {
                        shouldRetry = true
                    }
                    errorOnTokens = true
                }

            val forecast = forecastDeferred.await()
            forecast
                .map {
                    onForecast.postValue(it)
                }
                .mapLeft {
                    if (it == NoConnectionError || it == ConnectionTimeoutError) {
                        shouldRetry = true
                    }
                    errorOnForecast = true
                }

            handleErrors(errorOnUserDevice, errorOnTokens, errorOnForecast, shouldRetry)
            onLoading.postValue(false)
        }
    }

    private fun fetchForecast() {
        onLoading.postValue(true)
        viewModelScope.launch {
            userDeviceUseCase.getTodayAndTomorrowForecast(device)
                .map {
                    Timber.d("Got short term forecast for TODAY & TOMORROW")
                    if (it.isEmpty()) {
                        onError.postValue(
                            UIError(resHelper.getString(R.string.forecast_empty), null)
                        )
                    }
                    onForecast.postValue(it)
                }
                .mapLeft {
                    handleForecastFailure(it)
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
            is NoConnectionError, is ConnectionTimeoutError -> {
                uiError.errorMessage = failure.getDefaultMessage(R.string.error_reach_out_short)
                uiError.retryFunction = { fetchForecast() }
            }
            else -> {
                uiError.errorMessage = resHelper.getString(R.string.error_reach_out_short)
            }
        }
        onError.postValue(uiError)
    }

    private fun fetchTokenDetails() {
        onLoading.postValue(true)
        viewModelScope.launch {
            userDeviceUseCase.getTokenInfoLast30D(device.id)
                .map { onTokens.postValue(it) }
                .mapLeft { handleTokenFailure(it) }
            onLoading.postValue(false)
        }
    }

    fun fetchUserDevice() {
        onLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
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
                        is NoConnectionError, is ConnectionTimeoutError -> {
                            uiError.errorMessage = it.getDefaultMessage(
                                R.string.error_reach_out_short
                            )
                            uiError.retryFunction = ::fetchUserDevice
                        }
                        else -> {
                            uiError.errorMessage =
                                resHelper.getString(R.string.error_reach_out_short)
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
                uiError.retryFunction = { fetchUserDeviceAllData() }
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
                uiError.retryFunction = ::fetchTokenDetails
            }
        } else if (errorForecast) {
            uiError.errorMessage = resHelper.getString(R.string.error_user_device_forecast_failed)

            if (shouldRetry) {
                uiError.retryFunction = { fetchForecast() }
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
                    failure.message ?: resHelper.getString(R.string.error_reach_out_short)
            }
            is NoConnectionError, is ConnectionTimeoutError -> {
                uiError.errorMessage = failure.getDefaultMessage(R.string.error_reach_out_short)
                uiError.retryFunction = ::fetchTokenDetails
            }
            else -> {
                uiError.errorMessage = resHelper.getString(R.string.error_reach_out_short)
            }
        }
        onError.postValue(uiError)
    }

    fun isHourlyWeatherTomorrow(hourlyWeather: HourlyWeather?): Boolean {
        return ZonedDateTime.parse(hourlyWeather?.timestamp).isTomorrow()
    }

    fun getPositionOfTomorrowFirstItem(currentForecasts: List<HourlyWeather>): Int {
        var position = 0
        currentForecasts.forEach {
            if (isHourlyWeatherTomorrow(it)) {
                return position
            }
            position++
        }
        return position
    }

    fun setOrClearFriendlyName(friendlyName: String?) {
        if (friendlyName == null) {
            clearFriendlyName()
        } else {
            setFriendlyName(friendlyName)
        }
    }

    fun canChangeFriendlyName(): Either<UIError, Boolean> {
        return userDeviceUseCase.canChangeFriendlyName(device.id)
            .mapLeft {
                Timber.d(it.message)
                UIError(
                    resHelper.getString(R.string.error_friendly_name_change_rate_limit),
                    null
                )
            }
    }

    private fun setFriendlyName(friendlyName: String) {
        if (friendlyName.isNotEmpty() && friendlyName != device.attributes?.friendlyName) {
            onLoading.postValue(true)
            viewModelScope.launch {
                userDeviceUseCase.setFriendlyName(device.id, friendlyName)
                    .map {
                        onEditNameChange.postValue(true)
                    }
                    .mapLeft {
                        onError.postValue(
                            UIError(
                                resHelper.getString(R.string.error_reach_out_short),
                                null
                            )
                        )
                    }
                onLoading.postValue(false)
            }
        }
    }

    private fun clearFriendlyName() {
        if (device.attributes?.friendlyName?.isNotEmpty() == true) {
            onLoading.postValue(true)
            viewModelScope.launch {
                userDeviceUseCase.clearFriendlyName(device.id)
                    .map {
                        onEditNameChange.postValue(true)
                    }
                    .mapLeft {
                        onError.postValue(
                            UIError(
                                resHelper.getString(R.string.error_reach_out_short),
                                null
                            )
                        )
                    }
                onLoading.postValue(false)
            }
        }
    }

    init {
        viewModelScope.launch {
            userDeviceUseCase.getUnitPreferenceChangedFlow()
                .collect {
                    Timber.d("Unit preference key changed: $it. Triggering data update.")
                    onUnitPreferenceChanged.postValue(true)
                }
        }
    }
}
