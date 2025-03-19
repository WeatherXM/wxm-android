package com.weatherxm.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.models.Survey
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.WalletWarnings
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.usecases.RemoteBannersUseCase
import com.weatherxm.usecases.UserUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class HomeViewModel(
    private val userUseCase: UserUseCase,
    private val remoteBannersUseCase: RemoteBannersUseCase,
    private val photoUseCase: DevicePhotoUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private var hasDevices: Boolean? = null

    // Needed for passing info to show the wallet missing warning card and badges
    private val onWalletWarnings = MutableLiveData<WalletWarnings>()
    private val onSurvey = SingleLiveEvent<Survey>()
    private val onInfoBanner = SingleLiveEvent<RemoteBanner?>()

    // Needed for passing info to the activity to show/hide elements when scrolling on the list
    private val showOverlayViews = MutableLiveData(true)
    val shouldShowTerms = mutableStateOf(userUseCase.shouldShowTermsPrompt())

    fun onWalletWarnings(): LiveData<WalletWarnings> = onWalletWarnings
    fun onSurvey(): LiveData<Survey> = onSurvey
    fun onInfoBanner(): LiveData<RemoteBanner?> = onInfoBanner
    fun showOverlayViews() = showOverlayViews

    fun hasDevices() = hasDevices

    fun setHasDevices(devices: List<UIDevice>?) {
        hasDevices = devices?.firstOrNull { it.isOwned() } != null
    }

    fun onScroll(dy: Int) {
        showOverlayViews.postValue(dy <= 0)
    }

    fun getWalletWarnings() {
        viewModelScope.launch(dispatcher) {
            userUseCase.getWalletAddress().onRight {
                onWalletWarnings.postValue(
                    WalletWarnings(
                        showMissingBadge = it.isEmpty(),
                        showMissingWarning = userUseCase.shouldShowWalletMissingWarning(it)
                    )
                )
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onWalletWarnings.postValue(
                    WalletWarnings(
                        showMissingBadge = false,
                        showMissingWarning = false
                    )
                )
            }
        }
    }

    fun setWalletWarningDismissTimestamp() {
        userUseCase.setWalletWarningDismissTimestamp()
    }

    fun setWalletNotMissing() {
        onWalletWarnings.postValue(
            WalletWarnings(
                showMissingBadge = false,
                showMissingWarning = false
            )
        )
    }

    fun getSurvey() {
        remoteBannersUseCase.getSurvey().apply {
            if (this != null) {
                onSurvey.postValue(this)
            }
        }
    }

    fun dismissSurvey(surveyId: String) {
        remoteBannersUseCase.dismissSurvey(surveyId)
    }

    fun getInfoBanner() {
        remoteBannersUseCase.getRemoteBanner(RemoteBannerType.INFO_BANNER).apply {
            onInfoBanner.postValue(this)
        }
    }

    fun dismissInfoBanner(infoBannerId: String) {
        remoteBannersUseCase.dismissRemoteBanner(RemoteBannerType.INFO_BANNER, infoBannerId)
    }

    fun setAcceptTerms() {
        shouldShowTerms.value = false
        userUseCase.setAcceptTerms()
    }

    fun retryPhotoUpload(deviceId: String) = photoUseCase.retryUpload(deviceId)

    fun getClaimingBadgeShouldShow(): Boolean {
        return userUseCase.getClaimingBadgeShouldShow()
    }

    fun setClaimingBadgeShouldShow(shouldShow: Boolean) {
        userUseCase.setClaimingBadgeShouldShow(shouldShow)
    }
}
