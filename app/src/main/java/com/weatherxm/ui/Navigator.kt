package com.weatherxm.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.ui.claimdevice.ClaimDeviceActivity
import com.weatherxm.ui.claimdevice.helium.ClaimHeliumDeviceActivity
import com.weatherxm.ui.claimdevice.helium.pairingstatus.PairingStatusFragment
import com.weatherxm.ui.claimdevice.selectdevicetype.SelectDeviceTypeFragment
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.connectwallet.ConnectWalletActivity
import com.weatherxm.ui.deviceforecast.ForecastActivity
import com.weatherxm.ui.devicehistory.HistoryActivity
import com.weatherxm.ui.explorer.ExplorerActivity
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.preferences.PreferenceActivity
import com.weatherxm.ui.publicdevicedetail.PublicDeviceDetailFragment
import com.weatherxm.ui.publicdeviceslist.PublicDevicesListFragment
import com.weatherxm.ui.resetpassword.ResetPasswordActivity
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

    fun showLogin(context: Context) {
        context.startActivity(
            Intent(
                context, LoginActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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

    fun showConnectWallet(context: Context, onBackGoHome: Boolean) {
        context.startActivity(
            Intent(context, ConnectWalletActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ConnectWalletActivity.ARG_ON_BACK_GO_HOME, onBackGoHome)
        )
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

    fun showResetPassword(context: Context) {
        context.startActivity(
            Intent(
                context, ResetPasswordActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
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

    fun showSelectDeviceTypeDialog(fragmentManager: FragmentManager) {
        val modalBottomSheet = SelectDeviceTypeFragment()
        modalBottomSheet.show(fragmentManager, SelectDeviceTypeFragment.TAG)
    }

    fun showClaimM5(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        context: Context
    ) {
        activityResultLauncher.launch(
            Intent(context, ClaimDeviceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showClaimHelium(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        context: Context
    ) {
        activityResultLauncher.launch(
            Intent(context, ClaimHeliumDeviceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showHeliumPairingStatus(fragmentManager: FragmentManager) {
        val modalBottomSheet = PairingStatusFragment()
        modalBottomSheet.show(fragmentManager, PairingStatusFragment.TAG)
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
