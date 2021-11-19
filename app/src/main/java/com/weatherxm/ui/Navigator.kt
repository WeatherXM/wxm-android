package com.weatherxm.ui

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.weatherxm.data.Device
import com.weatherxm.data.Wallet
import com.weatherxm.ui.connectwallet.ConnectWalletActivity
import com.weatherxm.ui.devicedetail.DeviceDetailFragment
import com.weatherxm.ui.explorer.ExplorerActivity
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.preferences.PreferenceActivity
import com.weatherxm.ui.resetpassword.ResetPasswordActivity
import com.weatherxm.ui.signup.SignupActivity
import com.weatherxm.ui.splash.SplashActivity
import com.weatherxm.ui.userdevice.UserDeviceActivity


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
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK  or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    fun showDeviceDetails(fragmentManager: FragmentManager, device: Device) {
        val modalBottomSheet = DeviceDetailFragment.newInstance(device)
        modalBottomSheet.show(fragmentManager, DeviceDetailFragment.TAG)
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

    fun showConnectWallet(fragment: Fragment, wallet: Wallet?) {
        fragment.context?.let {
            it.startActivity(
                Intent(
                    it, ConnectWalletActivity::class.java
                )
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(ConnectWalletActivity.ARG_WALLET, wallet)
            )
        }
    }

    fun showUserDevice(fragment: Fragment, device: Device) {
        fragment.context?.let {
            it.startActivity(
                Intent(
                    it, UserDeviceActivity::class.java
                )
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

    fun showSplash(context: Context) {
        context.startActivity(
            Intent(
                context, SplashActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK  or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}
