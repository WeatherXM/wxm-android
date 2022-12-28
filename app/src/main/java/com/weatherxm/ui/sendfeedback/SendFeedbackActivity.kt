package com.weatherxm.ui.sendfeedback

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.databinding.ActivityWebviewBinding
import com.weatherxm.ui.common.Contracts.ARG_IS_DELETE_ACCOUNT_FORM
import com.weatherxm.util.applyInsets
import org.koin.core.component.KoinComponent

class SendFeedbackActivity : AppCompatActivity(), KoinComponent {
    private lateinit var binding: ActivityWebviewBinding
    private val model: SendFeedbackViewModel by viewModels()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        val isDeleteAccountForm = intent?.extras?.getBoolean(ARG_IS_DELETE_ACCOUNT_FORM) ?: false

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.webview.settings.javaScriptEnabled = true

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // Hack way to make some fields hidden (like userID)
                if (isDeleteAccountForm) {
                    view.loadUrl(model.getJavascriptInjectionCodeDeleteForm())
                } else {
                    view.loadUrl(model.getJavascriptInjectionCodeSurveyForm())
                }
                if (model.isUrlFormResponse(url)) {
                    setResult(Activity.RESULT_OK)
                }
                super.onPageFinished(view, url)
            }
        }

        binding.webview.loadUrl(model.getPrefilledSurveyFormUrl(isDeleteAccountForm))
    }
}
