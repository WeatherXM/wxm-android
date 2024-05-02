package com.weatherxm.ui.resetpassword

import android.os.Bundle
import android.view.View
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityResetPasswordBinding
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.util.Validator
import org.koin.androidx.viewmodel.ext.android.viewModel

class ResetPasswordActivity : BaseActivity() {
    private lateinit var binding: ActivityResetPasswordBinding
    private val model: ResetPasswordViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.email.onTextChanged {
            binding.emailContainer.error = null
            binding.sendBtn.isEnabled = !binding.email.text.isNullOrEmpty()
        }

        binding.sendBtn.setOnClickListener {
            val email = binding.email.text.toString().trim().lowercase()

            if (!Validator.validateUsername(email)) {
                binding.emailContainer.error = getString(R.string.warn_validation_invalid_email)
                return@setOnClickListener
            }

            model.resetPassword(email)
        }

        model.isEmailSent().observe(this) { result ->
            onEmailSentResult(result)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.PASSWORD_RESET, classSimpleName())
    }

    private fun onEmailSentResult(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_success, false)
                    .title(getString(R.string.success_reset_password_title))
                    .subtitle(getString(R.string.success_reset_password_text))
                binding.form.visibility = View.GONE
                binding.status.visibility = View.VISIBLE

                analytics.trackEventViewContent(
                    AnalyticsService.ParamValue.SEND_EMAIL_FORGOT_PASSWORD.paramValue,
                    AnalyticsService.ParamValue.SEND_EMAIL_FORGOT_PASSWORD_ID.paramValue,
                    success = 1L
                )
            }
            Status.ERROR -> {
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_error, false)
                    .title(getString(R.string.error_generic_message))
                    .subtitle("${result.message}")
                    .action(getString(R.string.action_retry))
                    .listener {
                        binding.form.visibility = View.VISIBLE
                        binding.status.visibility = View.GONE
                    }

                analytics.trackEventViewContent(
                    AnalyticsService.ParamValue.SEND_EMAIL_FORGOT_PASSWORD.paramValue,
                    AnalyticsService.ParamValue.SEND_EMAIL_FORGOT_PASSWORD_ID.paramValue,
                    success = 0L
                )
            }
            Status.LOADING -> {
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_loading)
                binding.form.visibility = View.GONE
                binding.status.visibility = View.VISIBLE
            }
        }
    }
}
