package com.weatherxm.ui.signup

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.databinding.ActivitySignupBinding
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.hideKeyboard
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Validator
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SignupActivity : BaseActivity() {
    private val model: SignupViewModel by viewModel()
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.username.onTextChanged {
            binding.usernameContainer.error = null
            binding.signup.isEnabled = !binding.username.text.isNullOrEmpty()
        }

        binding.done.setOnClickListener {
            navigator.showLogin(this)
            finish()
        }

        binding.signup.setOnClickListener {
            val username = binding.username.text.toString().trim().lowercase()
            val firstName = binding.firstName.text.toString().trim()
            val lastName = binding.lastName.text.toString().trim()

            // Validate input
            if (!Validator.validateUsername(username)) {
                binding.usernameContainer.error = getString(R.string.warn_validation_invalid_email)
                return@setOnClickListener
            }

            // Hide keyboard, if showing
            hideKeyboard()

            // Perform signup
            model.signup(username, firstName, lastName)
        }

        binding.contactSupport.setOnClickListener {
            navigator.openSupportCenter(this)
        }

        // Listen for signup state change
        model.isSignedUp().observe(this) { result ->
            onSignupResult(result)
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.SIGNUP, classSimpleName())
    }

    private fun onSignupResult(result: Resource<String>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Signup success. Email Sent.")
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_success, false)
                    .title(getString(R.string.success))
                    .subtitle(result.data)
                binding.form.visible(false)
                binding.status.visible(true)
                binding.contactSupport.visible(true)
                binding.done.visible(true)

                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.SIGNUP.paramValue,
                    contentId = AnalyticsService.ParamValue.SIGNUP_ID.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.METHOD, AnalyticsService.ParamValue.EMAIL.paramValue
                    ),
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
                        binding.form.visible(true)
                        binding.status.visible(false)
                        binding.done.invisible()
                    }
                binding.form.visible(false)
                binding.status.visible(true)
                binding.contactSupport.invisible()
                binding.done.invisible()

                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.SIGNUP.paramValue,
                    contentId = AnalyticsService.ParamValue.SIGNUP_ID.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.METHOD,
                        AnalyticsService.ParamValue.EMAIL.paramValue
                    ),
                    success = 0L
                )
            }
            Status.LOADING -> {
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_loading)
                binding.form.visible(false)
                binding.status.visible(true)
                binding.contactSupport.invisible()
                binding.done.invisible()
            }
        }
    }
}
