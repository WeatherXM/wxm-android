package com.weatherxm.ui.deleteaccountsurvey

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import com.weatherxm.R
import com.weatherxm.databinding.ActivityWebviewBinding
import com.weatherxm.ui.components.BaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeleteAccountSurveyActivity : BaseActivity() {
    private lateinit var binding: ActivityWebviewBinding
    private val model: DeleteAccountSurveyViewModel by viewModel()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.webview.settings.javaScriptEnabled = true

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // Hack way to make some fields hidden (like userID)
                view.loadUrl(model.getJavascriptInjectionCode())

                if (model.isUrlFormResponse(url)) {
                    setResult(Activity.RESULT_OK)
                }
                super.onPageFinished(view, url)
            }
        }

        val surveyUrl = getString(R.string.delete_account_survey_url)
        binding.webview.loadUrl(model.getPrefilledSurveyFormUrl(surveyUrl))
    }
}
