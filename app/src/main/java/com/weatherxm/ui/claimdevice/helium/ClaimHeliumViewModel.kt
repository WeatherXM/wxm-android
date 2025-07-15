package com.weatherxm.ui.claimdevice.helium

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError.DeviceNotFound
import com.weatherxm.data.models.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.models.ApiError.UserError.ClaimError.DeviceClaiming
import com.weatherxm.data.models.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.models.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

// Suppress this as almost all functions are get/set methods
@Suppress("TooManyFunctions")
class ClaimHeliumViewModel(
    private val claimDeviceUseCase: ClaimDeviceUseCase,
    private val photoUseCase: DevicePhotoUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<UIDevice>>()
    private val onPhotosMetadata = MutableLiveData<Pair<UIDevice, List<PhotoPresignedMetadata>>>()

    fun onCancel() = onCancel
    fun onNext() = onNext
    fun onClaimResult() = onClaimResult
    fun onPhotosMetadata(): LiveData<Pair<UIDevice, List<PhotoPresignedMetadata>>> =
        onPhotosMetadata

    private var devEUI: String = String.empty()
    private var deviceKey: String = String.empty()
    private var frequency: Frequency = Frequency.US915

    fun setFrequency(frequency: Frequency) {
        this.frequency = frequency
    }

    fun setDeviceEUI(devEUI: String) {
        this.devEUI = devEUI
    }

    fun setDeviceKey(key: String) {
        deviceKey = key
    }

    fun getFrequency(): Frequency {
        return frequency
    }

    fun getDevEUI(): String {
        return devEUI
    }

    fun getDeviceKey(): String {
        return deviceKey
    }

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
    }

    fun claimDevice(location: Location, photos: List<StationPhoto>) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            claimDeviceUseCase.claimDevice(devEUI, location.lat, location.lon, deviceKey).onRight {
                Timber.d("Claimed device: $it")
                prepareUpload(it, photos)
                onClaimResult.postValue(Resource.success(it))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onClaimResult.postValue(
                    Resource.error(
                        resources.getString(
                            when (it) {
                                is InvalidClaimId -> R.string.error_claim_invalid_dev_eui
                                is InvalidClaimLocation -> R.string.error_claim_invalid_location
                                is DeviceAlreadyClaimed -> R.string.error_claim_already_claimed
                                is DeviceNotFound -> R.string.error_claim_not_found_helium
                                is DeviceClaiming -> R.string.error_claim_device_claiming_error
                                else -> it.getDefaultMessageResId()
                            }
                        )
                    )
                )
            }
        }
    }

    suspend fun prepareUpload(device: UIDevice, photos: List<StationPhoto>) {
        photoUseCase.getPhotosMetadataForUpload(device.id, photos.mapNotNull { it.localPath })
            .onRight {
                onPhotosMetadata.postValue(Pair(device, it))
            }
    }

    fun setClaimedDevice(device: UIDevice?) {
        device?.let { onClaimResult.postValue(Resource.success(device)) }
    }
}
