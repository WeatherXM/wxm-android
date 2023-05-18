package com.weatherxm.ui.preferences

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.databinding.ActivityPreferencesBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.setVisible
import com.weatherxm.util.Analytics
import com.weatherxm.util.WidgetHelper
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PreferenceActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityPreferencesBinding
    private val model: PreferenceViewModel by viewModels()
    private val navigator: Navigator by inject()
    private val widgetHelper: WidgetHelper by inject()
    private val analytics: Analytics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.surveyPrompt.action(getString(R.string.short_app_survey_prompt_desc), null) {
            model.showSurveyScreen()
        }

        binding.surveyPrompt.closeButton {
            model.dismissSurveyPrompt()
        }

        if (model.hasDismissedSurveyPrompt()) {
            binding.surveyPrompt.visibility = View.GONE
        }

        model.onDismissSurveyPrompt().observe(this) {
            if (it) binding.surveyPrompt.setVisible(true)
        }

        model.isLoggedIn().observe(this) { result ->
            result
                .mapLeft {
                    Timber.d("Not logged in. Hide survey prompt.")
                    binding.surveyPrompt.setVisible(false)
                }
        }

        model.onLogout().observe(this) { hasLoggedOut ->
            if (hasLoggedOut) {
                widgetHelper.getWidgetIds().onRight {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    val ids = it.map { id ->
                        id.toInt()
                    }
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids.toIntArray())
                    intent.putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
                    intent.putExtra(Contracts.ARG_WIDGET_SHOULD_LOGIN, true)
                    this.sendBroadcast(intent)
                }
                navigator.showStartup(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.SETTINGS,
            PreferenceActivity::class.simpleName
        )
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
