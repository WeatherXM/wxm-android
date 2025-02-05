package com.weatherxm.ui

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BoostReward
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.Reward
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.WXMRemoteMessage
import com.weatherxm.ui.analytics.AnalyticsOptInActivity
import com.weatherxm.ui.cellinfo.CellInfoActivity
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity
import com.weatherxm.ui.claimdevice.pulse.ClaimPulseActivity
import com.weatherxm.ui.claimdevice.selectstation.SelectStationTypeActivity
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity
import com.weatherxm.ui.common.Contracts.ARG_BLE_DEVICE_CONNECTED
import com.weatherxm.ui.common.Contracts.ARG_BOOST_REWARD
import com.weatherxm.ui.common.Contracts.ARG_CELL_CENTER
import com.weatherxm.ui.common.Contracts.ARG_DATE
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_DEVICES_REWARDS
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_TYPE
import com.weatherxm.ui.common.Contracts.ARG_EXPLORER_CELL
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_DAY
import com.weatherxm.ui.common.Contracts.ARG_INSTRUCTIONS_ONLY
import com.weatherxm.ui.common.Contracts.ARG_NEEDS_PHOTO_VERIFICATION
import com.weatherxm.ui.common.Contracts.ARG_NEW_PHOTO_VERIFICATION
import com.weatherxm.ui.common.Contracts.ARG_OPEN_EXPLORER_ON_BACK
import com.weatherxm.ui.common.Contracts.ARG_PHOTOS
import com.weatherxm.ui.common.Contracts.ARG_REMOTE_MESSAGE
import com.weatherxm.ui.common.Contracts.ARG_REWARD
import com.weatherxm.ui.common.Contracts.ARG_REWARD_DETAILS
import com.weatherxm.ui.common.Contracts.ARG_USER_MESSAGE
import com.weatherxm.ui.common.Contracts.ARG_WALLET_REWARDS
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.DatePicker
import com.weatherxm.ui.components.LoginPromptDialogFragment
import com.weatherxm.ui.components.MessageDialogFragment
import com.weatherxm.ui.connectwallet.ConnectWalletActivity
import com.weatherxm.ui.deeplinkrouter.DeepLinkRouterActivity
import com.weatherxm.ui.deleteaccount.DeleteAccountActivity
import com.weatherxm.ui.deleteaccountsurvey.DeleteAccountSurveyActivity
import com.weatherxm.ui.devicealerts.DeviceAlertsActivity
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.ui.deviceeditlocation.DeviceEditLocationActivity
import com.weatherxm.ui.deviceforecast.ForecastDetailsActivity
import com.weatherxm.ui.deviceheliumota.DeviceHeliumOTAActivity
import com.weatherxm.ui.devicehistory.HistoryActivity
import com.weatherxm.ui.devicesettings.helium.DeviceSettingsHeliumActivity
import com.weatherxm.ui.devicesettings.helium.changefrequency.ChangeFrequencyActivity
import com.weatherxm.ui.devicesettings.helium.reboot.RebootActivity
import com.weatherxm.ui.devicesettings.wifi.DeviceSettingsWifiActivity
import com.weatherxm.ui.devicesrewards.DevicesRewardsActivity
import com.weatherxm.ui.explorer.ExplorerActivity
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.networkstats.NetworkStatsActivity
import com.weatherxm.ui.passwordprompt.PasswordPromptFragment
import com.weatherxm.ui.photoverification.gallery.PhotoGalleryActivity
import com.weatherxm.ui.photoverification.intro.PhotoVerificationIntroActivity
import com.weatherxm.ui.photoverification.upload.PhotoUploadActivity
import com.weatherxm.ui.preferences.PreferenceActivity
import com.weatherxm.ui.resetpassword.ResetPasswordActivity
import com.weatherxm.ui.rewardboosts.RewardBoostActivity
import com.weatherxm.ui.rewarddetails.RewardDetailsActivity
import com.weatherxm.ui.rewardissues.RewardIssuesActivity
import com.weatherxm.ui.rewardsclaim.RewardsClaimActivity
import com.weatherxm.ui.rewardslist.RewardsListActivity
import com.weatherxm.ui.signup.SignupActivity
import com.weatherxm.ui.startup.StartupActivity
import com.weatherxm.ui.updateprompt.UpdatePromptActivity
import com.weatherxm.util.ImageFileHelper.getUriForFile
import timber.log.Timber
import java.io.File
import java.time.LocalDate

@Suppress("TooManyFunctions")
class Navigator(private val analytics: AnalyticsWrapper) {

    fun showExplorer(context: Context, cellCenter: Location? = null) {
        context.startActivity(
            Intent(context, ExplorerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(ARG_CELL_CENTER, cellCenter)
        )
    }

    fun showLogin(
        context: Context,
        newTask: Boolean = false,
        userMessage: String? = null
    ) {
        val intentFlags = if (newTask) {
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        } else {
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(
            Intent(context, LoginActivity::class.java).addFlags(intentFlags)
                .putExtra(ARG_USER_MESSAGE, userMessage)
        )
    }

    fun showSignup(context: Context) {
        context.startActivity(
            Intent(context, SignupActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    fun showHome(context: Context, cellCenter: Location? = null) {
        context.startActivity(
            Intent(context, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(ARG_CELL_CENTER, cellCenter)
        )
    }

    fun showUpdatePrompt(context: Context) {
        context.startActivity(
            Intent(context, UpdatePromptActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    fun showAnalyticsOptIn(context: Context) {
        context.startActivity(
            Intent(context, AnalyticsOptInActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    fun showNetworkStats(context: Context) {
        context.startActivity(
            Intent(context, NetworkStatsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showPasswordPrompt(
        activity: FragmentActivity,
        @StringRes message: Int,
        listener: PasswordPromptFragment.OnPasswordConfirmedListener
    ) {
        PasswordPromptFragment.newInstance(message).show(activity) {
            listener.onPasswordConfirmed(it)
        }
    }

    fun showCellInfo(context: Context?, cell: UICell, openExplorerOnBack: Boolean = false) {
        context?.let {
            it.startActivity(
                Intent(it, CellInfoActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_EXPLORER_CELL, cell)
                    .putExtra(ARG_OPEN_EXPLORER_ON_BACK, openExplorerOnBack)
            )
        }
    }

    fun showPreferences(fragment: Fragment) {
        fragment.context?.let {
            it.startActivity(
                Intent(it, PreferenceActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showDeviceDetails(
        context: Context?,
        device: UIDevice,
        openExplorerOnBack: Boolean = false
    ) {
        context?.let {
            it.startActivity(
                Intent(it, DeviceDetailsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(ARG_DEVICE, device)
                    .putExtra(ARG_OPEN_EXPLORER_ON_BACK, openExplorerOnBack)
            )
        }
    }

    fun showDeviceDetailsWithBackStack(context: Context?, device: UIDevice) {
        val deviceDetailsActivity = Intent(context, DeviceDetailsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(ARG_DEVICE, device)

        val pendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deviceDetailsActivity)
            this.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        pendingIntent.send()
    }

    fun showStationSettings(context: Context?, device: UIDevice) {
        val intent = if (device.isHelium()) {
            Intent(context, DeviceSettingsHeliumActivity::class.java)
        } else {
            Intent(context, DeviceSettingsWifiActivity::class.java)
        }
        context?.startActivity(
            intent
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
        )
    }

    fun openShare(context: Context, text: String) {
        with(Intent()) {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(this, context.getString(R.string.share)))
        }
    }

    fun openShareImages(context: Context, photoUris: ArrayList<Uri>) {
        with(Intent()) {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoUris)
            context.startActivity(Intent.createChooser(this, context.getString(R.string.share)))
        }
    }

    fun showDeleteAccountSurvey(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        context: Context
    ) {
        val intent = Intent(context, DeleteAccountSurveyActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activityResultLauncher.launch(intent)
    }

    fun showBluetoothEnablePrompt(bluetoothLauncher: ActivityResultLauncher<Intent>) {
        bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    fun showResetPassword(context: Context) {
        context.startActivity(
            Intent(context, ResetPasswordActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showDeleteAccount(fragment: Fragment) {
        fragment.context?.let {
            it.startActivity(
                Intent(it, DeleteAccountActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showResetPassword(fragment: Fragment) {
        fragment.context?.let {
            it.startActivity(
                Intent(it, ResetPasswordActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showStartup(context: Context) {
        context.startActivity(
            Intent(context, StartupActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    fun showHistoryActivity(context: Context, device: UIDevice?) {
        context.startActivity(
            Intent(context, HistoryActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
        )
    }

    fun showRewardsList(context: Context, device: UIDevice?) {
        context.startActivity(
            Intent(context, RewardsListActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
        )
    }

    fun showRewardDetails(context: Context, device: UIDevice?, reward: Reward?) {
        context.startActivity(
            Intent(context, RewardDetailsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
                .putExtra(ARG_REWARD, reward)
        )
    }

    fun showConnectWallet(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        fragment: Fragment
    ) {
        fragment.context?.let {
            activityResultLauncher.launch(
                Intent(it, ConnectWalletActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showConnectWallet(context: Context) {
        context.startActivity(
            Intent(context, ConnectWalletActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showClaimSelectStationType(context: Context) {
        context.startActivity(
            Intent(context, SelectStationTypeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showClaimFlow(
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        context: Context,
        deviceType: DeviceType
    ) {
        val activity = when (deviceType) {
            DeviceType.M5_WIFI, DeviceType.D1_WIFI -> ClaimWifiActivity::class.java
            DeviceType.PULSE_4G -> ClaimPulseActivity::class.java
            DeviceType.HELIUM -> ClaimHeliumActivity::class.java
        }
        val intent = Intent(context, activity)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ARG_DEVICE_TYPE, deviceType as Parcelable)
        if (activityResultLauncher == null) {
            context.startActivity(intent)
        } else {
            activityResultLauncher.launch(intent)
        }
    }

    fun showDeviceHeliumOTA(
        context: Context?,
        device: UIDevice?,
        deviceIsBleConnected: Boolean = false,
        needsPhotoVerification: Boolean = false
    ) {
        context?.startActivity(
            Intent(context, DeviceHeliumOTAActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
                .putExtra(ARG_BLE_DEVICE_CONNECTED, deviceIsBleConnected)
                .putExtra(ARG_NEEDS_PHOTO_VERIFICATION, needsPhotoVerification)
        )
    }

    fun showRebootStation(context: Context, device: UIDevice?) {
        context.startActivity(
            Intent(context, RebootActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
        )
    }

    fun showChangeFrequency(context: Context, device: UIDevice?) {
        context.startActivity(
            Intent(context, ChangeFrequencyActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
        )
    }

    fun showDeepLinkRouter(context: Context, wxmRemoteMessage: WXMRemoteMessage? = null) {
        context.startActivity(
            Intent(context, DeepLinkRouterActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(ARG_REMOTE_MESSAGE, wxmRemoteMessage)
        )
    }

    fun showEditLocation(
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        context: Context,
        device: UIDevice?
    ) {
        val intent = Intent(context, DeviceEditLocationActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ARG_DEVICE, device)
        if (activityResultLauncher == null) {
            context.startActivity(intent)
        } else {
            activityResultLauncher.launch(intent)
        }
    }

    fun showDeviceAlerts(context: Context?, device: UIDevice?) {
        context?.let {
            it.startActivity(
                Intent(it, DeviceAlertsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_DEVICE, device)
            )
        }
    }

    fun showRewardsClaiming(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        context: Context,
        rewardsData: UIWalletRewards
    ) {
        val intent = Intent(context, RewardsClaimActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ARG_WALLET_REWARDS, rewardsData)
        activityResultLauncher.launch(intent)
    }

    fun showRewardIssues(context: Context, device: UIDevice?, reward: RewardDetails?) {
        context.startActivity(
            Intent(context, RewardIssuesActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
                .putExtra(ARG_REWARD_DETAILS, reward)
        )
    }

    fun showRewardBoost(context: Context, reward: BoostReward?, deviceId: String?, date: String?) {
        context.startActivity(
            Intent(context, RewardBoostActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_BOOST_REWARD, reward)
                .putExtra(ARG_DEVICE_ID, deviceId)
                .putExtra(ARG_DATE, date)
        )
    }

    fun showForecastDetails(
        context: Context?,
        device: UIDevice,
        forecastSelectedISODate: String? = null
    ) {
        context?.startActivity(
            Intent(context, ForecastDetailsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
                .putExtra(ARG_FORECAST_SELECTED_DAY, forecastSelectedISODate)
        )
    }

    fun showDevicesRewards(fragment: Fragment, devicesRewards: DevicesRewards) {
        fragment.context?.let {
            it.startActivity(
                Intent(it, DevicesRewardsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_DEVICES_REWARDS, devicesRewards)
            )
        }
    }

    fun showPhotoVerificationIntro(
        context: Context?,
        device: UIDevice,
        photos: ArrayList<String> = arrayListOf(),
        instructionsOnly: Boolean = false
    ) {
        context?.let {
            it.startActivity(
                Intent(it, PhotoVerificationIntroActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_DEVICE, device)
                    .putStringArrayListExtra(ARG_PHOTOS, photos)
                    .putExtra(ARG_INSTRUCTIONS_ONLY, instructionsOnly)
            )
        }
    }

    fun showPhotoGallery(
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        context: Context,
        device: UIDevice,
        photos: ArrayList<String>,
        newPhotoVerification: Boolean
    ) {
        val intent = Intent(context, PhotoGalleryActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ARG_DEVICE, device)
            .putStringArrayListExtra(ARG_PHOTOS, photos)
            .putExtra(ARG_NEW_PHOTO_VERIFICATION, newPhotoVerification)
        if (activityResultLauncher == null) {
            context.startActivity(intent)
        } else {
            activityResultLauncher.launch(intent)
        }
    }

    fun showPhotoUpload(
        context: Context?,
        device: UIDevice,
        photos: ArrayList<String>
    ) {
        context?.let {
            it.startActivity(
                Intent(it, PhotoUploadActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_DEVICE, device)
                    .putStringArrayListExtra(ARG_PHOTOS, photos)
            )
        }
    }

    @Suppress("LongParameterList")
    fun showMessageDialog(
        fragmentManager: FragmentManager,
        title: String?,
        message: String? = null,
        htmlMessage: String? = null,
        readMoreUrl: String? = null,
        analyticsScreen: AnalyticsService.Screen? = null
    ) {
        MessageDialogFragment.newInstance(title, message, htmlMessage, readMoreUrl, analyticsScreen)
            .show(fragmentManager, MessageDialogFragment.TAG)
    }

    fun showHandleFollowDialog(
        fragmentActivity: FragmentActivity?,
        isFollowDialog: Boolean,
        deviceName: String,
        onConfirm: () -> Unit
    ) {
        var title = R.string.add_favorites
        var message = R.string.add_favorites_desc
        if (!isFollowDialog) {
            title = R.string.remove_favorites
            message = R.string.remove_favorites_desc
        }
        fragmentActivity?.let {
            ActionDialogFragment
                .Builder(
                    title = it.getString(title),
                    htmlMessage = it.getString(message, deviceName),
                    negative = it.getString(R.string.action_cancel),
                )
                .onPositiveClick(it.getString(R.string.action_confirm)) {
                    onConfirm.invoke()
                }
                .build()
                .show(it)
        }
    }

    fun showLoginDialog(
        fragmentActivity: FragmentActivity?,
        title: String,
        message: String? = null,
        htmlMessage: String? = null,
    ) {
        fragmentActivity?.let {
            LoginPromptDialogFragment(title,
                message,
                htmlMessage,
                onLogin = {
                    showLogin(it)
                },
                onSignup = {
                    showSignup(it)
                }
            ).show(it)
        }
    }

    fun showDatePicker(
        context: Context,
        selectedDate: LocalDate? = null,
        dateStart: LocalDate? = null,
        dateEnd: LocalDate? = null,
        listener: DatePicker.OnDateSelectedListener
    ) {
        DatePicker.show(context, selectedDate, dateStart, dateEnd, listener)
    }

    fun openSupportCenter(
        context: Context?,
        source: String = AnalyticsService.ParamValue.ERROR.paramValue
    ) {
        analytics.trackEventSelectContent(
            AnalyticsService.ParamValue.CONTACT_SUPPORT.paramValue,
            Pair(FirebaseAnalytics.Param.SOURCE, source)
        )

        context?.let {
            try {
                it.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(context.getString(R.string.support_center_url))
                    )
                )
            } catch (e: ActivityNotFoundException) {
                Timber.d(e, "Could not open support center.")
                it.toast(R.string.error_cannot_open_support_center)
            }
        }
    }

    fun openCamera(launcher: ActivityResultLauncher<Intent>, context: Context, destFile: File) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, destFile.getUriForFile(context))
            launcher.launch(takePictureIntent)
        }
    }

    fun openWebsite(context: Context?, url: String) {
        context?.let {
            try {
                CustomTabsIntent.Builder()
                    .build()
                    .launchUrl(it, Uri.parse(url))
            } catch (e: ActivityNotFoundException) {
                Timber.d(e, "Could not load url: $url")
                it.toast(R.string.error_open_website_support_cannot_open_url, url)
            }
        }
    }

    fun openPlayStore(context: Context?, url: String) {
        context?.let {
            try {
                it.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                Timber.d(e, "Could not open play store.")
                it.toast(R.string.error_cannot_open_play_store)
            }
        }
    }

    fun openAppSettings(context: Context?) {
        context?.let {
            try {
                Timber.d("Going to application settings")
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", it.packageName, null)
                    it.startActivity(this)
                }
            } catch (e: ActivityNotFoundException) {
                Timber.d(e, "Could not open app settings.")
                it.toast(R.string.error_cannot_open_app_settings)
            }
        }
    }
}
