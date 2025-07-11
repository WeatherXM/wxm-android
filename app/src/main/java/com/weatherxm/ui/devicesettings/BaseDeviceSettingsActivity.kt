package com.weatherxm.ui.devicesettings

import android.app.Activity
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil3.ImageLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.RewardSplitStakeholderAdapter
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.StationPhoto
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.loadImage
import com.weatherxm.ui.common.parcelable
import com.weatherxm.ui.common.parcelableList
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.MapboxUtils.getMinimap
import org.koin.android.ext.android.inject
import timber.log.Timber

@Suppress("TooManyFunctions")
abstract class BaseDeviceSettingsActivity : BaseActivity() {
    private val imageLoader: ImageLoader by inject()

    // Register the launcher for the edit location activity and wait for a possible result
    private val editLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val device = it.data?.parcelable<UIDevice>(ARG_DEVICE)
            if (it.resultCode == Activity.RESULT_OK && device != null) {
                onEditLocation(device)
            }
        }

    // Register the launcher for the photo gallery activity and wait for a possible result
    private val photoGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            /**
             * Some changes happened in the photos so we need to fetch them again or delete all of
             * them if the user left the screen with <2 photos left.
             */
            val shouldDeleteAllPhotos =
                it.data?.getBooleanExtra(Contracts.ARG_DELETE_ALL_PHOTOS, false)
            val photos = it.data?.parcelableList<StationPhoto>(Contracts.ARG_PHOTOS)
            if (it.resultCode == Activity.RESULT_OK) {
                onPhotosChanged(shouldDeleteAllPhotos, photos)
            }
        }

    protected fun handleLoading(
        swipeRefreshLayout: SwipeRefreshLayout,
        progress: LinearProgressIndicator,
        isLoading: Boolean
    ) {
        if (isLoading && !swipeRefreshLayout.isRefreshing) {
            progress.visible(true)
        } else {
            swipeRefreshLayout.isRefreshing = false
            progress.invisible()
        }
    }

    protected fun onPhotosClicked(
        devicePhotos: List<String>,
        hasAcceptedPhotosTerms: Boolean,
        device: UIDevice
    ) {
        analytics.trackEventSelectContent(
            contentType = AnalyticsService.ParamValue.GO_TO_PHOTO_VERIFICATION.paramValue,
            Pair(
                FirebaseAnalytics.Param.SOURCE,
                AnalyticsService.ParamValue.SETTINGS.paramValue
            )
        )
        val photos = arrayListOf<String>()
        devicePhotos.forEach {
            photos.add(it)
        }
        if (photos.isEmpty() || !hasAcceptedPhotosTerms) {
            navigator.showPhotoVerificationIntro(this, device, photos)
        } else {
            navigator.showPhotoGallery(photoGalleryLauncher, this, device, photos)
        }
    }

    protected fun onPhotosCancelPrompt(onCancel: () -> Unit) {
        ActionDialogFragment
            .Builder(
                title = getString(R.string.cancel_upload),
                message = getString(R.string.cancel_upload_message),
                negative = getString(R.string.action_back)
            )
            .onPositiveClick(getString(R.string.yes_cancel)) {
                analytics.trackEventUserAction(
                    AnalyticsService.ParamValue.CANCEL_UPLOADING_PHOTOS.paramValue
                )
                onCancel()
            }
            .build()
            .show(this)
    }


    protected fun onPhotosRetry(functionToRun: () -> Unit) {
        functionToRun()
        analytics.trackEventUserAction(
            AnalyticsService.ParamValue.RETRY_UPLOADING_PHOTOS.paramValue
        )
    }

    protected fun setupStationLocation(
        device: UIDevice,
        editLocationBtn: MaterialButton,
        locationDesc: MaterialTextView
    ) {
        editLocationBtn.setOnClickListener {
            navigator.showEditLocation(editLocationLauncher, this, device)
        }
        editLocationBtn.visible(device.isOwned())

        if (device.relation == DeviceRelation.FOLLOWED) {
            locationDesc.setHtml(
                R.string.station_location_favorite_desc,
                device.address ?: String.empty()
            )
        } else {
            locationDesc.setHtml(R.string.station_location_desc, device.address ?: String.empty())
        }
    }

    protected fun updateMinimap(
        device: UIDevice,
        locationLayout: ConstraintLayout,
        locationMinimap: ImageView
    ) {
        val deviceMapLocation = if (device.isOwned()) {
            device.location
        } else {
            null
        }
        getMinimap(locationLayout.width, deviceMapLocation, device.hex7)?.let {
            locationMinimap.loadImage(imageLoader, it.toString())
        } ?: locationMinimap.visible(false)
    }

    protected fun onRemoveStation(device: UIDevice, onRemoved: () -> Unit) {
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.REMOVE_DEVICE.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, device.id)
        )
        navigator.showPasswordPrompt(this, R.string.remove_station_password_message) {
            if (it) {
                Timber.d("Password confirmation success!")
                onRemoved()
            } else {
                Timber.d("Password confirmation prompt was cancelled or failed.")
            }
        }
    }

    protected fun onChangeStationName(device: UIDevice, onChanged: (String?) -> Unit) {
        FriendlyNameDialogFragment(device.friendlyName, device.id) {
            onChanged(it)
        }.show(this)
    }

    protected fun setupRemoveStationDescription(view: MaterialTextView) {
        view.movementMethod =
            me.saket.bettermovementmethod.BetterLinkMovementMethod.newInstance().apply {
                setOnLinkClickListener { _, url ->
                    navigator.openWebsite(this@BaseDeviceSettingsActivity, url)
                    return@setOnLinkClickListener true
                }
            }
        view.setHtml(
            R.string.remove_station_desc,
            getString(R.string.docs_url)
        )
    }

    protected fun trackLowBatteryWarning(deviceId: String) {
        analytics.trackEventPrompt(
            AnalyticsService.ParamValue.LOW_BATTERY.paramValue,
            AnalyticsService.ParamValue.WARN.paramValue,
            AnalyticsService.ParamValue.VIEW.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, deviceId)
        )
    }

    protected fun onShare(textToShare: String, deviceId: String) {
        navigator.openShare(this, textToShare)

        analytics.trackEventUserAction(
            actionName = AnalyticsService.ParamValue.SHARE_STATION_INFO.paramValue,
            contentType = AnalyticsService.ParamValue.STATION_INFO.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, deviceId)
        )
    }

    @Suppress("LongParameterList")
    protected fun handleRewardSplits(
        rewardSplitCard: MaterialCardView,
        rewardSplitDesc: MaterialTextView,
        recyclerRewardSplit: RecyclerView,
        data: RewardSplitsData?,
        isStakeholder: Boolean,
        isDeviceOwned: Boolean,
    ) {
        if (data?.hasSplitRewards() == true) {
            rewardSplitCard.visible(true)
            rewardSplitDesc.text = getString(R.string.reward_split_desc, data.splits.size)
            val rewardSplitAdapter = RewardSplitStakeholderAdapter(data.wallet, true)
            recyclerRewardSplit.adapter = rewardSplitAdapter
            rewardSplitAdapter.submitList(data.splits)

            val stakeHolderValue = if (isStakeholder) {
                AnalyticsService.ParamValue.STAKEHOLDER_LOWERCASE.paramValue
            } else {
                AnalyticsService.ParamValue.NON_STAKEHOLDER.paramValue
            }
            trackRewardSplitViewContent(
                AnalyticsService.ParamValue.REWARD_SPLITTING.paramValue,
                stakeHolderValue
            )
        } else {
            val stakeHolderValue = if (isDeviceOwned) {
                AnalyticsService.ParamValue.STAKEHOLDER_LOWERCASE.paramValue
            } else {
                AnalyticsService.ParamValue.NON_STAKEHOLDER.paramValue
            }
            trackRewardSplitViewContent(
                AnalyticsService.ParamValue.NO_REWARD_SPLITTING.paramValue,
                stakeHolderValue
            )
        }
    }

    protected fun trackRewardSplitViewContent(deviceState: String, userState: String) {
        analytics.trackEventViewContent(
            AnalyticsService.ParamValue.REWARD_SPLITTING_DEVICE_SETTINGS.paramValue,
            Pair(AnalyticsService.CustomParam.DEVICE_STATE.paramName, deviceState),
            Pair(AnalyticsService.CustomParam.USER_STATE.paramName, userState)
        )
    }

    abstract fun onEditLocation(device: UIDevice)
    abstract fun onPhotosChanged(shouldDeleteAllPhotos: Boolean?, photos: ArrayList<StationPhoto>?)
}
