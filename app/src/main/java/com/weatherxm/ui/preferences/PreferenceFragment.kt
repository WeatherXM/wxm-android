package com.weatherxm.ui.preferences

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.weatherxm.R
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.AlertDialogFragment
import com.weatherxm.ui.common.toast
import com.weatherxm.util.DisplayModeHelper
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class PreferenceFragment : KoinComponent, PreferenceFragmentCompat() {
    private val model: PreferenceViewModel by activityViewModels()
    private val navigator: Navigator by inject()
    private val displayModeHelper: DisplayModeHelper by inject()

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val openDocumentationButton: Preference? =
            findPreference(getString(R.string.title_open_documentation))
        val contactSupportButton: Preference? =
            findPreference(getString(R.string.title_contact_support))
        val userResearchButton: Preference? =
            findPreference(getString(R.string.user_panel_title))
        val displayPreference =
            findPreference<ListPreference>(getString(R.string.key_theme))
        val shortWxmSurvey: Preference? =
            findPreference(getString(R.string.short_app_survey))

        openDocumentationButton?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                navigator.openWebsite(context, getString(R.string.documentation_url))
                true
            }
        contactSupportButton?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                navigator.sendSupportEmail(context)
                true
            }
        userResearchButton?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
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
                    val accountCategory: PreferenceCategory? =
                        findPreference(getString(R.string.account))

                    accountCategory?.isVisible = false
                    shortWxmSurvey?.isVisible = false
                }
                .map {
                    Timber.d("Logged in. Handle button clicks")
                    val logoutButton: Preference? =
                        findPreference(getString(R.string.action_logout))
                    val resetPassButton: Preference? =
                        findPreference(getString(R.string.change_password))
                    val deleteAccountButton: Preference? =
                        findPreference(getString(R.string.delete_account))

                    logoutButton?.onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            showLogoutDialog()
                            true
                        }
                    resetPassButton?.onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            navigator.showResetPassword(this)
                            true
                        }
                    shortWxmSurvey?.onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            navigator.showSendFeedback(sendFeedbackLauncher, this)
                            true
                        }
                    deleteAccountButton?.onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            navigator.showDeleteAccount(this)
                            true
                        }
                }
        }

        model.onShowSurveyScreen().observe(this) {
            if (it) navigator.showSendFeedback(sendFeedbackLauncher, this)
        }
    }

    private fun showLogoutDialog() {
        AlertDialogFragment
            .Builder(
                title = getString(R.string.title_dialog_logout),
                message = getString(R.string.message_dialog_logout),
                negative = getString(R.string.no)
            )
            .onPositiveClick(getString(R.string.yes)) {
                model.logout()
            }
            .build()
            .show(this)
    }
}
