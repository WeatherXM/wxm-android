package com.weatherxm.ui

import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
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
import com.weatherxm.data.BoostReward
import com.weatherxm.data.Location
import com.weatherxm.data.Reward
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.WXMRemoteMessage
import com.weatherxm.ui.analytics.AnalyticsOptInActivity
import com.weatherxm.ui.cellinfo.CellInfoActivity
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity
import com.weatherxm.ui.claimdevice.selectstation.SelectStationTypeActivity
import com.weatherxm.ui.claimdevice.wifi.ClaimWifiActivity
import com.weatherxm.ui.common.Contracts.ARG_BLE_DEVICE_CONNECTED
import com.weatherxm.ui.common.Contracts.ARG_BOOST_REWARD
import com.weatherxm.ui.common.Contracts.ARG_CELL_CENTER
import com.weatherxm.ui.common.Contracts.ARG_DATE
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_ID
import com.weatherxm.ui.common.Contracts.ARG_DEVICE_TYPE
import com.weatherxm.ui.common.Contracts.ARG_EXPLORER_CELL
import com.weatherxm.ui.common.Contracts.ARG_FORECAST_SELECTED_DAY
import com.weatherxm.ui.common.Contracts.ARG_IS_DELETE_ACCOUNT_FORM
import com.weatherxm.ui.common.Contracts.ARG_OPEN_EXPLORER_ON_BACK
import com.weatherxm.ui.common.Contracts.ARG_REMOTE_MESSAGE
import com.weatherxm.ui.common.Contracts.ARG_REWARD
import com.weatherxm.ui.common.Contracts.ARG_REWARD_DETAILS
import com.weatherxm.ui.common.Contracts.ARG_USER_MESSAGE
import com.weatherxm.ui.common.Contracts.ARG_WALLET_REWARDS
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.ui.components.DatePicker
import com.weatherxm.ui.components.LoginPromptDialogFragment
import com.weatherxm.ui.components.MessageDialogFragment
import com.weatherxm.ui.connectwallet.ConnectWalletActivity
import com.weatherxm.ui.deleteaccount.DeleteAccountActivity
import com.weatherxm.ui.devicealerts.DeviceAlertsActivity
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.ui.deviceeditlocation.DeviceEditLocationActivity
import com.weatherxm.ui.deviceforecast.ForecastDetailsActivity
import com.weatherxm.ui.deviceheliumota.DeviceHeliumOTAActivity
import com.weatherxm.ui.devicehistory.HistoryActivity
import com.weatherxm.ui.devicesettings.DeviceSettingsActivity
import com.weatherxm.ui.devicesettings.changefrequency.ChangeFrequencyActivity
import com.weatherxm.ui.devicesettings.reboot.RebootActivity
import com.weatherxm.ui.explorer.ExplorerActivity
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.networkstats.NetworkStatsActivity
import com.weatherxm.ui.passwordprompt.PasswordPromptFragment
import com.weatherxm.ui.preferences.PreferenceActivity
import com.weatherxm.ui.resetpassword.ResetPasswordActivity
import com.weatherxm.ui.rewardboosts.RewardBoostActivity
import com.weatherxm.ui.rewarddetails.RewardDetailsActivity
import com.weatherxm.ui.rewardissues.RewardIssuesActivity
import com.weatherxm.ui.rewardsclaim.RewardsClaimActivity
import com.weatherxm.ui.rewardslist.RewardsListActivity
import com.weatherxm.ui.sendfeedback.SendFeedbackActivity
import com.weatherxm.ui.signup.SignupActivity
import com.weatherxm.ui.startup.StartupActivity
import com.weatherxm.ui.updateprompt.UpdatePromptActivity
import com.weatherxm.ui.urlrouteractivity.UrlRouterActivity
import timber.log.Timber
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
        device: UIDevice = UIDevice.empty(),
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

    fun showStationSettings(context: Context?, device: UIDevice) {
        context?.let {
            it.startActivity(
                Intent(it, DeviceSettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_DEVICE, device)
            )
        }
    }

    fun openShare(context: Context, text: String) {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }

    fun showSendFeedback(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        context: Context,
        isDeleteAccountForm: Boolean = false
    ) {
        val intent = Intent(context, SendFeedbackActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(ARG_IS_DELETE_ACCOUNT_FORM, isDeleteAccountForm)
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

    fun showSendFeedback(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        fragment: Fragment
    ) {
        fragment.context?.let {
            activityResultLauncher.launch(
                Intent(it, SendFeedbackActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showClaimSelectStationType(context: Context) {
        context.startActivity(
            Intent(context, SelectStationTypeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showClaimHeliumFlow(context: Context) {
        // Launch claim activity
        context.startActivity(
            Intent(context, ClaimHeliumActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showClaimWifiFlow(context: Context, deviceType: DeviceType) {
        // Launch claim activity
        context.startActivity(
            Intent(context, ClaimWifiActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE_TYPE, deviceType as Parcelable)
        )
    }

    fun showDeviceHeliumOTA(fragment: Fragment, device: UIDevice?, deviceIsBleConnected: Boolean) {
        fragment.context?.let {
            it.startActivity(
                Intent(it, DeviceHeliumOTAActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_DEVICE, device)
                    .putExtra(ARG_BLE_DEVICE_CONNECTED, deviceIsBleConnected)
            )
        }
    }

    fun showDeviceHeliumOTA(context: Context, device: UIDevice?, deviceIsBleConnected: Boolean) {
        context.startActivity(
            Intent(context, DeviceHeliumOTAActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
                .putExtra(ARG_BLE_DEVICE_CONNECTED, deviceIsBleConnected)
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

    fun showUrlRouter(context: Context, wxmRemoteMessage: WXMRemoteMessage? = null) {
        context.startActivity(
            Intent(context, UrlRouterActivity::class.java)
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

    fun showMessageDialog(
        fragmentManager: FragmentManager,
        title: String?,
        message: String?,
        readMoreUrl: String? = null,
        analyticsScreen: AnalyticsService.Screen? = null
    ) {
        MessageDialogFragment.newInstance(title, message, readMoreUrl, analyticsScreen)
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
