package com.weatherxm.ui

import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.data.ClientIdentificationHelper
import com.weatherxm.data.Location
import com.weatherxm.ui.analytics.AnalyticsOptInActivity
import com.weatherxm.ui.cellinfo.CellInfoActivity
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity
import com.weatherxm.ui.common.Contracts.ARG_BLE_DEVICE_CONNECTED
import com.weatherxm.ui.common.Contracts.ARG_CELL_CENTER
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_EXPLORER_CELL
import com.weatherxm.ui.common.Contracts.ARG_IS_DELETE_ACCOUNT_FORM
import com.weatherxm.ui.common.Contracts.ARG_OPEN_EXPLORER_ON_BACK
import com.weatherxm.ui.common.Contracts.ARG_USER_MESSAGE
import com.weatherxm.ui.common.MessageDialogFragment
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.connectwallet.ConnectWalletActivity
import com.weatherxm.ui.deleteaccount.DeleteAccountActivity
import com.weatherxm.ui.devicealerts.DeviceAlertsActivity
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.ui.deviceheliumota.DeviceHeliumOTAActivity
import com.weatherxm.ui.devicehistory.HistoryActivity
import com.weatherxm.ui.explorer.ExplorerActivity
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.networkstats.NetworkStatsActivity
import com.weatherxm.ui.passwordprompt.PasswordPromptFragment
import com.weatherxm.ui.preferences.PreferenceActivity
import com.weatherxm.ui.resetpassword.ResetPasswordActivity
import com.weatherxm.ui.sendfeedback.SendFeedbackActivity
import com.weatherxm.ui.signup.SignupActivity
import com.weatherxm.ui.startup.StartupActivity
import com.weatherxm.ui.stationsettings.StationSettingsActivity
import com.weatherxm.ui.stationsettings.changefrequency.ChangeFrequencyActivity
import com.weatherxm.ui.stationsettings.reboot.RebootActivity
import com.weatherxm.ui.token.TokenActivity
import com.weatherxm.ui.updateprompt.UpdatePromptActivity
import com.weatherxm.util.Analytics
import timber.log.Timber


@Suppress("TooManyFunctions")
class Navigator(
    private val clientIdentificationHelper: ClientIdentificationHelper,
    private val analytics: Analytics
) {

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

    fun showCellInfo(context: Context?, cell: UICell) {
        context?.let {
            it.startActivity(
                Intent(it, CellInfoActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_EXPLORER_CELL, cell)
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
                Intent(it, StationSettingsActivity::class.java)
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

    fun showQRScanner(activityResultLauncher: ActivityResultLauncher<ScanOptions>) {
        activityResultLauncher.launch(ScanOptions().setBeepEnabled(false))
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

    fun showTokenScreen(context: Context, device: UIDevice?) {
        context.startActivity(
            Intent(context, TokenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ARG_DEVICE, device)
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

    fun showClaimHeliumFlow(context: Context) {
        // Launch claim activity
        context.startActivity(
            Intent(context, ClaimHeliumActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showClaimM5Flow(context: Context) {
        // Launch claim activity
        context.startActivity(
            Intent(context, ClaimM5Activity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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

    fun showDeviceAlerts(fragment: Fragment, device: UIDevice?) {
        fragment.context?.let {
            it.startActivity(
                Intent(it, DeviceAlertsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ARG_DEVICE, device)
            )
        }
    }

    fun showMessageDialog(
        fragmentManager: FragmentManager,
        title: String?,
        message: String?
    ) {
        MessageDialogFragment.newInstance(title, message)
            .show(fragmentManager, MessageDialogFragment.TAG)
    }

    fun sendSupportEmail(
        context: Context?,
        subject: String? = null,
        body: String? = null,
        source: String = Analytics.ParamValue.ERROR.paramValue,
        withClientIdentifier: Boolean = true
    ) {
        analytics.trackEventSelectContent(
            Analytics.ParamValue.CONTACT_SUPPORT.paramValue,
            Pair(FirebaseAnalytics.Param.SOURCE, source)
        )

        context?.let {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // Only email apps should handle this
            intent.putExtra(
                Intent.EXTRA_EMAIL, arrayOf(it.getString(R.string.support_email_recipient))
            )
            subject?.let { subject -> intent.putExtra(Intent.EXTRA_SUBJECT, subject) }

            val clientIdentifier = clientIdentificationHelper.getDeviceSupportEmailBody()
            if (body.isNullOrEmpty() && withClientIdentifier) {
                intent.putExtra(Intent.EXTRA_TEXT, clientIdentifier)
            } else if (withClientIdentifier) {
                intent.putExtra(Intent.EXTRA_TEXT, "$body\n$clientIdentifier")
            }
            try {
                it.startActivity(
                    Intent.createChooser(
                        intent,
                        it.getString(R.string.support_email_intent_title)
                    )
                )
            } catch (e: ActivityNotFoundException) {
                Timber.d("Email client not found: $e")
                it.toast(R.string.error_support_cannot_send_email)
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
}
