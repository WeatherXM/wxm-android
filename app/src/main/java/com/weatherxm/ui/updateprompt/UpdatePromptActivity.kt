package com.weatherxm.ui.updateprompt

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.databinding.ActivityUpdatePromptBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.Analytics
import com.weatherxm.util.applyInsets
import com.weatherxm.util.setHtml
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UpdatePromptActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityUpdatePromptBinding
    private val navigator: Navigator by inject()
    private val analytics: Analytics by inject()
    private val model: UpdatePromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback {
            if (!model.isUpdateMandatory()) {
                navigator.showStartup(this@UpdatePromptActivity)
            }
            finish()
        }

        binding.updateDescription.setHtml(R.string.desc_update_prompt)

        if (model.isUpdateMandatory()) {
            binding.toolbar.navigationIcon = null
            binding.continueWithoutUpdatingBtn.visibility = View.GONE
        }

        binding.updateBtn.setOnClickListener {
            navigator.openPlayStore(this, getString(R.string.market_url, packageName))
        }

        binding.continueWithoutUpdatingBtn.setOnClickListener {
            navigator.showStartup(this)
            finish()
        }

        binding.changelog.text = model.getChangelog()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(
            Analytics.Screen.APP_UPDATE_PROMPT,
            UpdatePromptActivity::class.simpleName
        )
    }
}
