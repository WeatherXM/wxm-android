package com.weatherxm.ui.preferences

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.DisplayModeHelper
import com.weatherxm.util.hasPermission
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class PreferenceFragment : PreferenceFragmentCompat() {
    private val model: PreferenceViewModel by activityViewModel()
    private val navigator: Navigator by inject()
    private val displayModeHelper: DisplayModeHelper by inject()
    private val analytics: AnalyticsWrapper by inject()

    companion object {
        const val TAG = "PreferenceFragment"
    }

    override fun onResume() {
        handleNotificationsPreference()
        super.onResume()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val openDocsButton: Preference? = findPreference(getString(R.string.documentation_title))
        val announcementsButton: Preference? =
            findPreference(getString(R.string.announcements_title))
        val contactSupportButton: Preference? =
            findPreference(getString(R.string.contact_support_title))
        val userResearchButton: Preference? = findPreference(getString(R.string.user_panel_title))
        val displayPreference = findPreference<ListPreference>(getString(R.string.key_theme))
        val analyticsPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.key_google_analytics))
        val notificationsPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.notifications_preference_key))
        val logoutBtn: Preference? = findPreference(getString(R.string.action_logout))
        val resetPassBtn: Preference? = findPreference(getString(R.string.change_password))
        val deleteAccountButton: Preference? = findPreference(getString(R.string.delete_account))
        val appVersionPref: Preference? = findPreference(getString(R.string.title_app_version))

        /*
         * Disable switching of notifications toggle as we prompt user to do it via the settings
         */
        notificationsPreference?.setOnPreferenceChangeListener { _, _ ->
            false
        }
        openDocsButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.DOCUMENTATION.paramValue)
            navigator.openWebsite(context, getString(R.string.docs_url))
            true
        }
        announcementsButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            analytics.trackEventSelectContent(AnalyticsService.ParamValue.ANNOUNCEMENTS.paramValue)
            navigator.openWebsite(context, getString(R.string.announcements_url))
            true
        }
        contactSupportButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            navigator.openSupportCenter(context, AnalyticsService.ParamValue.SETTINGS.paramValue)
            true
        }
        userResearchButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            analytics.trackEventSelectContent(
                AnalyticsService.ParamValue.USER_RESEARCH_PANEL.paramValue
            )
            navigator.openWebsite(this.context, getString(R.string.user_panel_url))
            true
        }
        displayPreference?.setOnPreferenceChangeListener { _, newValue ->
            displayModeHelper.setDisplayMode(newValue.toString())
            true
        }
        /**
         * If `installationId` is available, append it at the end of the app version
         */
        model.getInstallationId()?.let {
            appVersionPref?.summary = getString(R.string.app_version) + "-$it"
        }

        if (model.isLoggedIn()) {
            Timber.d("Logged in. Handle button clicks")
            onLoggedIn(logoutBtn, resetPassBtn, deleteAccountButton, analyticsPreference)
        } else {
            Timber.d("Not logged in. Hide account preferences.")
            onLoggedOut(logoutBtn, resetPassBtn, deleteAccountButton, analyticsPreference)
        }
    }

    private fun onLoggedOut(
        logoutBtn: Preference?,
        resetPassBtn: Preference?,
        deleteAccountButton: Preference?,
        analyticsPreference: SwitchPreferenceCompat?
    ) {
        logoutBtn?.isVisible = false
        resetPassBtn?.isVisible = false
        deleteAccountButton?.isVisible = false
        analyticsPreference?.isVisible = false
    }

    private fun onLoggedIn(
        logoutBtn: Preference?,
        resetPassBtn: Preference?,
        deleteAccountButton: Preference?,
        analyticsPreference: SwitchPreferenceCompat?
    ) {
        analyticsPreference?.setOnPreferenceChangeListener { _, newValue ->
            model.setAnalyticsEnabled(newValue as Boolean)
            true
        }
        logoutBtn?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showLogoutDialog()
            true
        }
        resetPassBtn?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            navigator.showResetPassword(this)
            true
        }
        deleteAccountButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            navigator.showDeleteAccount(this)
            true
        }
    }

    private fun showLogoutDialog() {
        ActionDialogFragment
            .Builder(
                title = getString(R.string.title_dialog_logout),
                message = getString(R.string.message_dialog_logout),
                negative = getString(R.string.no),
            )
            .onPositiveClick(getString(R.string.yes)) {
                model.logout()
            }
            .build()
            .show(this)
    }

    /**
     * Suppress InlinedApi as we check for API level before using it through AndroidBuildInfo.sdkInt
     */
    @SuppressLint("InlinedApi")
    private fun handleNotificationsPreference() {
        val notificationsPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.notifications_preference_key))

        notificationsPreference?.isChecked =
            if (AndroidBuildInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                context?.hasPermission(POST_NOTIFICATIONS) == true
            } else {
                NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
            }

        notificationsPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val notificationsStatus = if (notificationsPreference?.isChecked == true) {
                AnalyticsService.ParamValue.ON
            } else {
                AnalyticsService.ParamValue.OFF
            }
            analytics.trackEventUserAction(
                AnalyticsService.ParamValue.NOTIFICATIONS.paramValue,
                customParams = arrayOf(
                    Pair(
                        AnalyticsService.CustomParam.STATUS.paramName,
                        notificationsStatus.paramValue
                    )
                )
            )

            navigator.openAppSettings(context)
            true
        }
    }
}
