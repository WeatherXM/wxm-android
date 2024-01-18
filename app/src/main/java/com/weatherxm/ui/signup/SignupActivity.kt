package com.weatherxm.ui.signup

import android.os.Bundle
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivitySignupBinding
import com.weatherxm.ui.common.applyInsets
import com.weatherxm.ui.common.hideKeyboard
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Analytics
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

        binding.root.applyInsets()

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
        analytics.trackScreen(Analytics.Screen.SIGNUP, this::class.simpleName)
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
                binding.form.visibility = View.GONE
                binding.status.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.VISIBLE
                binding.done.visibility = View.VISIBLE

                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.SIGNUP.paramValue,
                    contentId = Analytics.ParamValue.SIGNUP_ID.paramValue,
                    Pair(FirebaseAnalytics.Param.METHOD, Analytics.ParamValue.EMAIL.paramValue),
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
                        binding.done.visibility = View.INVISIBLE
                    }
                binding.form.visibility = View.GONE
                binding.status.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.INVISIBLE
                binding.done.visibility = View.INVISIBLE

                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.SIGNUP.paramValue,
                    contentId = Analytics.ParamValue.SIGNUP_ID.paramValue,
                    Pair(FirebaseAnalytics.Param.METHOD, Analytics.ParamValue.EMAIL.paramValue),
                    success = 0L
                )
            }
            Status.LOADING -> {
                binding.statusView
                    .clear()
                    .animation(R.raw.anim_loading)
                binding.form.visibility = View.GONE
                binding.status.visibility = View.VISIBLE
                binding.contactSupport.visibility = View.INVISIBLE
                binding.done.visibility = View.INVISIBLE
            }
        }
    }
}
