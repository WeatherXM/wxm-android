package com.weatherxm.ui.preferences

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.weatherxm.R
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.AlertDialogFragment
import com.weatherxm.util.ResourcesHelper
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class PreferenceFragment : KoinComponent, PreferenceFragmentCompat() {
    private val model: PreferenceViewModel by activityViewModels()
    private val resHelper: ResourcesHelper by inject()
    private val navigator: Navigator by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        model.isLoggedIn().observe(this) { result ->
            result
                .mapLeft {
                    Timber.d("Not logged in. Hide account preferences.")
                    val accountCategory: PreferenceCategory? = findPreference(resHelper.getString(R.string.account))
                    accountCategory?.isVisible = false
                }
                .map {
                    Timber.d("Logged in. Handle button clicks")
                    val logoutButton: Preference? = findPreference(resHelper.getString(R.string.action_logout))
                    val resetPassButton: Preference? = findPreference(resHelper.getString(R.string.change_password))
                    logoutButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        showLogoutDialog()
                        true
                    }
                    resetPassButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        navigator.showResetPassword(this)
                        true
                    }
                }
        }
    }

    private fun showLogoutDialog() {
        AlertDialogFragment.Builder(
            title = resHelper.getString(R.string.title_dialog_logout),
            message = resHelper.getString(R.string.message_dialog_logout),
            negative = resHelper.getString(R.string.no)
        )
            .onPositiveClick(resHelper.getString(R.string.yes)) {
                model.logout()
            }
            .build()
            .show(this)
    }
}
