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
import arrow.core.Either
import com.journeyapps.barcodescanner.ScanOptions
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.ui.claimdevice.getClaimResultHandler
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumActivity
import com.weatherxm.ui.claimdevice.m5.ClaimM5Activity
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.connectwallet.ConnectWalletActivity
import com.weatherxm.ui.deleteaccount.DeleteAccountActivity
import com.weatherxm.ui.deviceforecast.ForecastActivity
import com.weatherxm.ui.deviceheliumota.DeviceHeliumOTAActivity
import com.weatherxm.ui.devicehistory.HistoryActivity
import com.weatherxm.ui.explorer.ExplorerActivity
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.login.LoginActivity.Companion.ARG_USER_MESSAGE
import com.weatherxm.ui.passwordprompt.PasswordPromptFragment
import com.weatherxm.ui.preferences.PreferenceActivity
import com.weatherxm.ui.publicdevicedetail.PublicDeviceDetailFragment
import com.weatherxm.ui.publicdeviceslist.PublicDevicesListFragment
import com.weatherxm.ui.resetpassword.ResetPasswordActivity
import com.weatherxm.ui.sendfeedback.SendFeedbackActivity
import com.weatherxm.ui.sendfeedback.SendFeedbackActivity.Companion.ARG_IS_DELETE_ACCOUNT_FORM
import com.weatherxm.ui.signup.SignupActivity
import com.weatherxm.ui.startup.StartupActivity
import com.weatherxm.ui.token.TokenActivity
import com.weatherxm.ui.updateprompt.UpdatePromptActivity
import com.weatherxm.ui.userdevice.UserDeviceActivity
import timber.log.Timber


@Suppress("TooManyFunctions")
class Navigator {

    fun showExplorer(context: Context) {
        context.startActivity(
            Intent(
                context, ExplorerActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    fun showLogin(
        context: Context,
        isForcedLogout: Boolean = false,
        userMessage: String? = null
    ) {
        val intentFlags = if (isForcedLogout) {
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
            Intent(
                context, SignupActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    fun showHome(context: Context) {
        context.startActivity(
            Intent(
                context, HomeActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    fun showUpdatePrompt(context: Context) {
        context.startActivity(
            Intent(
                context, UpdatePromptActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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

    fun showPublicDevicesList(fragmentManager: FragmentManager) {
        val modalBottomSheet = PublicDevicesListFragment()
        modalBottomSheet.show(fragmentManager, PublicDevicesListFragment.TAG)
    }

    fun showDeviceDetails(fragmentManager: FragmentManager, device: UIDevice) {
        val modalBottomSheet = PublicDeviceDetailFragment.newInstance(device)
        modalBottomSheet.show(fragmentManager, PublicDeviceDetailFragment.TAG)
    }

    fun showPreferences(context: Context) {
        context.startActivity(
            Intent(
                context, PreferenceActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showPreferences(fragment: Fragment) {
        fragment.context?.let {
            it.startActivity(
                Intent(
                    it, PreferenceActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showUserDevice(context: Context?, device: Device) {
        context?.let {
            it.startActivity(
                Intent(it, UserDeviceActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(UserDeviceActivity.ARG_DEVICE, device)
            )
        }
    }

    fun showUserDevice(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        fragment: Fragment,
        device: Device
    ) {
        fragment.context?.let {
            activityResultLauncher.launch(
                Intent(it, UserDeviceActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(UserDeviceActivity.ARG_DEVICE, device)
            )
        }
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
            Intent(
                context, ResetPasswordActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showDeleteAccount(fragment: Fragment) {
        fragment.context?.let {
            it.startActivity(
                Intent(
                    it, DeleteAccountActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showResetPassword(fragment: Fragment) {
        fragment.context?.let {
            it.startActivity(
                Intent(
                    it, ResetPasswordActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showStartup(context: Context) {
        context.startActivity(
            Intent(
                context, StartupActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    fun showHistoryActivity(context: Context, device: Device?) {
        context.startActivity(
            Intent(context, HistoryActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(HistoryActivity.ARG_DEVICE, device)
        )
    }

    fun showForecast(context: Context, device: Device?) {
        context.startActivity(
            Intent(context, ForecastActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ForecastActivity.ARG_DEVICE, device)
        )
    }

    fun showTokenScreen(context: Context, device: Device?) {
        context.startActivity(
            Intent(context, TokenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(TokenActivity.ARG_DEVICE, device)
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
                Intent(
                    it, SendFeedbackActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    fun showClaimHeliumFlow(
        activity: FragmentActivity,
        callback: (Either<Failure, Device>) -> Unit
    ) {
        // Launch claim activity
        getClaimResultHandler(activity, callback).launch(
            Intent(activity, ClaimHeliumActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showClaimM5Flow(
        activity: FragmentActivity,
        callback: (Either<Failure, Device>) -> Unit
    ) {
        // Launch claim activity
        getClaimResultHandler(activity, callback).launch(
            Intent(activity, ClaimM5Activity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showDeviceHeliumOTA(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        fragment: Fragment,
        device: Device?
    ) {
        fragment.context?.let {
            activityResultLauncher.launch(
                Intent(it, DeviceHeliumOTAActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(DeviceHeliumOTAActivity.ARG_DEVICE, device)
            )
        }
    }

    fun sendSupportEmail(
        context: Context?,
        recipient: String? = null,
        subject: String? = null,
        body: String? = null
    ) {
        context?.let {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // Only email apps should handle this
            intent.putExtra(
                Intent.EXTRA_EMAIL, arrayOf(
                    if (recipient.isNullOrEmpty()) {
                        it.getString(R.string.support_email_recipient)
                    } else recipient
                )
            )
            subject?.let { subject -> intent.putExtra(Intent.EXTRA_SUBJECT, subject) }
            body?.let { body -> intent.putExtra(Intent.EXTRA_TEXT, body) }
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
