package com.weatherxm.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.weatherxm.R

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
