package com.weatherxm.ui

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.weatherxm.data.PublicDevice
import com.weatherxm.ui.devicedetail.DeviceDetailFragment
import com.weatherxm.ui.explorer.ExplorerActivity
import com.weatherxm.ui.home.HomeActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.preferences.PreferenceActivity
import com.weatherxm.ui.signup.SignupActivity
import com.weatherxm.ui.splash.SplashActivity

class Navigator {

    fun showSplash(context: Context) {
        context.startActivity(
            Intent(
                context, SplashActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

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
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    fun showDeviceDetails(fragmentManager: FragmentManager, publicDevice: PublicDevice) {
        val modalBottomSheet = DeviceDetailFragment.newInstance(publicDevice)
        modalBottomSheet.show(fragmentManager, DeviceDetailFragment.TAG)
    }

    fun showPreferences(context: Context) {
        context.startActivity(
            Intent(
                context, PreferenceActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}
