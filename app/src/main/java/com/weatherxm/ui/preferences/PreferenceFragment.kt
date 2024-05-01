package com.weatherxm.ui.preferences

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.weatherxm.R
import com.weatherxm.analytics.Analytics
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.components.ActionDialogFragment
import com.weatherxm.analytics.AnalyticsImpl
import com.weatherxm.util.DisplayModeHelper
import com.weatherxm.util.hasPermission
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class PreferenceFragment : PreferenceFragmentCompat() {
    private val model: PreferenceViewModel by activityViewModel()
    private val navigator: Navigator by inject()
    private val displayModeHelper: DisplayModeHelper by inject()
    private val analytics: AnalyticsImpl by inject()

    companion object {
        const val TAG = "PreferenceFragment"
    }

    // Register the launcher for the claim device activity and wait for a possible result
    private val sendFeedbackLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                context.toast(getString(R.string.thank_you_feedback))
                model.dismissSurveyPrompt()
            }
        }

    override fun onResume() {
        handleNotificationsPreference()
        super.onResume()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val openDocumentationButton: Preference? =
            findPreference(getString(R.string.documentation_title))
        val announcementsButton: Preference? =
            findPreference(getString(R.string.announcements_title))
        val contactSupportButton: Preference? =
            findPreference(getString(R.string.contact_support_title))
        val userResearchButton: Preference? =
            findPreference(getString(R.string.user_panel_title))
        val displayPreference =
            findPreference<ListPreference>(getString(R.string.key_theme))
        val shortWxmSurvey: Preference? =
            findPreference(getString(R.string.short_app_survey))
        val analyticsPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.key_google_analytics))
        val notificationsPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.notifications_preference_key))
        val logoutBtn: Preference? = findPreference(getString(R.string.action_logout))
        val resetPassBtn: Preference? =
            findPreference(getString(R.string.change_password))
        val deleteAccountButton: Preference? =
            findPreference(getString(R.string.delete_account))

        /*
         * Disable switching of notifications toggle as we prompt user to do it via the settings
         */
        notificationsPreference?.setOnPreferenceChangeListener { _, _ ->
            false
        }
        openDocumentationButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            analytics.trackEventSelectContent(Analytics.ParamValue.DOCUMENTATION.paramValue)
            navigator.openWebsite(context, getString(R.string.docs_url))
            true
        }
        announcementsButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            analytics.trackEventSelectContent(Analytics.ParamValue.ANNOUNCEMENTS.paramValue)
            navigator.openWebsite(context, getString(R.string.announcements_url))
            true
        }
        contactSupportButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            navigator.openSupportCenter(context, source = Analytics.ParamValue.SETTINGS.paramValue)
            true
        }
        userResearchButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            analytics.trackEventSelectContent(Analytics.ParamValue.USER_RESEARCH_PANEL.paramValue)
            navigator.openWebsite(this.context, getString(R.string.user_panel_url))
            true
        }

        displayPreference?.setOnPreferenceChangeListener { _, newValue ->
            displayModeHelper.setDisplayMode(newValue.toString())
            true
        }

        model.isLoggedIn().observe(this) { result ->
            result
                .mapLeft {
                    Timber.d("Not logged in. Hide account preferences.")
                    onLoggedOut(
                        shortWxmSurvey,
                        logoutBtn,
                        resetPassBtn,
                        deleteAccountButton,
                        analyticsPreference
                    )
                }
                .map {
                    Timber.d("Logged in. Handle button clicks")
                    onLoggedIn(
                        shortWxmSurvey,
                        logoutBtn,
                        resetPassBtn,
                        deleteAccountButton,
                        analyticsPreference
                    )
                }
        }

        model.onShowSurveyScreen().observe(this) {
            if (it) navigator.showSendFeedback(sendFeedbackLauncher, this)
        }
    }

    private fun onLoggedOut(
        shortWxmSurvey: Preference?,
        logoutBtn: Preference?,
        resetPassBtn: Preference?,
        deleteAccountButton: Preference?,
        analyticsPreference: SwitchPreferenceCompat?
    ) {
        logoutBtn?.isVisible = false
        resetPassBtn?.isVisible = false
        deleteAccountButton?.isVisible = false
        shortWxmSurvey?.isVisible = false
        analyticsPreference?.isVisible = false
    }

    private fun onLoggedIn(
        shortWxmSurvey: Preference?,
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
        shortWxmSurvey?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            analytics.trackEventSelectContent(Analytics.ParamValue.APP_SURVEY.paramValue)
            navigator.showSendFeedback(sendFeedbackLauncher, this)
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

    private fun handleNotificationsPreference() {
        val notificationsPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.notifications_preference_key))

        notificationsPreference?.isChecked =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context?.hasPermission(POST_NOTIFICATIONS) == true
            } else {
                NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
            }

        notificationsPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val notificationsStatus = if (notificationsPreference?.isChecked == true) {
                Analytics.ParamValue.ON
            } else {
                Analytics.ParamValue.OFF
            }
            analytics.trackEventUserAction(
                Analytics.ParamValue.NOTIFICATIONS.paramValue,
                customParams = arrayOf(
                    Pair(Analytics.CustomParam.STATUS.paramName, notificationsStatus.paramValue)
                )
            )

            navigator.openAppSettings(context)
            true
        }
    }
}
