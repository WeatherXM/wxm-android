package com.weatherxm.ui.updateprompt

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.databinding.ActivityUpdatePromptBinding
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.setHtml
import com.weatherxm.ui.components.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class UpdatePromptActivity : BaseActivity() {
    private lateinit var binding: ActivityUpdatePromptBinding
    private val model: UpdatePromptViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback {
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.APP_UPDATE_PROMPT_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.APP_UPDATE_PROMPT.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.DISCARD.paramValue
                )
            )
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
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.APP_UPDATE_PROMPT_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.APP_UPDATE_PROMPT.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.UPDATE.paramValue
                )
            )
            navigator.openPlayStore(this, getString(R.string.market_url, packageName))
        }

        binding.continueWithoutUpdatingBtn.setOnClickListener {
            analytics.trackEventUserAction(
                actionName = AnalyticsService.ParamValue.APP_UPDATE_PROMPT_RESULT.paramValue,
                contentType = AnalyticsService.ParamValue.APP_UPDATE_PROMPT.paramValue,
                Pair(
                    AnalyticsService.CustomParam.ACTION.paramName,
                    AnalyticsService.ParamValue.DISCARD.paramValue
                )
            )
            navigator.showStartup(this)
            finish()
        }

        binding.changelog.text = model.getChangelog()
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.APP_UPDATE_PROMPT, classSimpleName())
    }
}
