package com.weatherxm.ui.preferences

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.weatherxm.R
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.AlertDialogFragment
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class PreferenceFragment : KoinComponent, PreferenceFragmentCompat() {
    private val model: PreferenceViewModel by activityViewModels()
    private val navigator: Navigator by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        model.isLoggedIn().observe(this) { result ->
            result
                .mapLeft {
                    Timber.d("Not logged in. Hide account preferences.")
                    val accountCategory: PreferenceCategory? =
                        findPreference(getString(R.string.account))
                    accountCategory?.isVisible = false
                }
                .map {
                    Timber.d("Logged in. Handle button clicks")
                    val logoutButton: Preference? =
                        findPreference(getString(R.string.action_logout))
                    val resetPassButton: Preference? =
                        findPreference(getString(R.string.change_password))
                    val openDocumentationButton: Preference? =
                        findPreference(getString(R.string.title_open_documentation))
                    val contactSupportButton: Preference? =
                        findPreference(getString(R.string.title_contact_support))
                    logoutButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        showLogoutDialog()
                        true
                    }
                    resetPassButton?.onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            navigator.showResetPassword(this)
                            true
                        }
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
                }
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
