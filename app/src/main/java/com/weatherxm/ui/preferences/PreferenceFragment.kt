package com.weatherxm.ui.preferences

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.weatherxm.R
import com.weatherxm.ui.common.AlertDialogFragment
import com.weatherxm.util.ResourcesHelper
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class PreferenceFragment : KoinComponent, PreferenceFragmentCompat() {
    private val model: PreferenceViewModel by activityViewModels()
    private val resHelper: ResourcesHelper by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        model.isLoggedIn().observe(this) { result ->
            result.mapLeft {
                Timber.d("Not logged in. Hide button.")
                val button: Preference? = findPreference(getString(R.string.action_logout))
                button?.isVisible = false
            }
                .map {
                    Timber.d("Logged in. Handle button clicks")
                    val button: Preference? = findPreference(getString(R.string.action_logout))
                    button?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        showLogoutDialog()
                        true
                    }
                }
        }
    }

    private fun showLogoutDialog() {
        AlertDialogFragment.Builder(
            title = resHelper.getString(R.string.title_dialog_logout),
            message = resHelper.getString(R.string.message_dialog_logout),
            negative = resHelper.getString(R.string.no))
            .onPositiveClick(resHelper.getString(R.string.yes)) {
                model.logout()
            }
            .build()
            .show(this)
    }
}
