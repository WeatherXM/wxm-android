package com.weatherxm.ui.preferences

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.databinding.ActivityPreferencesBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PreferenceActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityPreferencesBinding
    private val model: PreferenceViewModel by viewModels()
    private val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.showSurveyButton.setOnClickListener {
            model.showSurveyScreen()
        }

        binding.closeSurveyPromptButton.setOnClickListener {
            model.dismissSurveyPrompt()
        }

        if (model.hasDismissedSurveyPrompt()) {
            binding.surveyPrompt.visibility = View.GONE
        }

        model.onDismissSurveyPrompt().observe(this) {
            if (it) {
                binding.surveyPrompt.visibility = View.GONE
            }
        }

        model.isLoggedIn().observe(this) { result ->
            result
                .mapLeft {
                    Timber.d("Not logged in. Hide survey prompt.")
                    binding.surveyPrompt.visibility = View.GONE
                }
        }

        model.onLogout().observe(this) { hasLoggedOut ->
            if (hasLoggedOut) {
                navigator.showStartup(this)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
