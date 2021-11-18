package com.weatherxm.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.databinding.ActivityPreferencesBinding
import com.weatherxm.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PreferenceActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityPreferencesBinding
    private val model: PreferenceViewModel by viewModels()
    private val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.preference_root, PreferenceFragment())
            .commit()

        model.onLogout().observe(this, { hasLoggedOut ->
            if (hasLoggedOut) {
                navigator.showSplash(this)
                finish()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
