package com.weatherxm.ui.login

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.models.User
import com.weatherxm.databinding.ActivityLoginBinding
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_FROM_ONBOARDING
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.classSimpleName
import com.weatherxm.ui.common.hideKeyboard
import com.weatherxm.ui.common.invisible
import com.weatherxm.ui.common.onTextChanged
import com.weatherxm.ui.common.toast
import com.weatherxm.ui.common.visible
import com.weatherxm.ui.components.BaseActivity
import com.weatherxm.util.Validator
import com.weatherxm.util.WidgetHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginActivity : BaseActivity() {
    private val widgetHelper: WidgetHelper by inject()
    private val model: LoginViewModel by viewModel()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Listen for login state change
        model.onLogin().observe(this) {
            onLoginResult(it)
        }

        // Listen for user's wallet existence
        model.user().observe(this) {
            onUserResult(it)
        }

        if (model.isLoggedIn()) {
            Timber.d("Already Logged In. Finish the activity.")
            toast(R.string.already_logged_in)
            finish()
        }

        binding.username.onTextChanged {
            binding.usernameContainer.error = null
            binding.login.isEnabled =
                !binding.username.text.isNullOrEmpty() && !binding.password.text.isNullOrEmpty()
        }

        binding.username.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.password.requestFocus()
            }
            true
        }

        binding.password.onTextChanged {
            binding.passwordContainer.error = null
            binding.login.isEnabled =
                !binding.username.text.isNullOrEmpty() && !binding.password.text.isNullOrEmpty()
        }

        binding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateAndLogin()
            }
            true
        }

        binding.forgotPassword.setOnClickListener {
            navigator.showResetPassword(this)
        }

        binding.login.setOnClickListener {
            validateAndLogin()
        }
    }

    private fun validateAndLogin() {
        val username = binding.username.text.toString().trim().lowercase()
        val password = binding.password.text.toString().trim()

        if (!Validator.validateUsername(username)) {
            binding.usernameContainer.error = getString(R.string.warn_validation_invalid_email)
            return
        }

        if (!Validator.validatePassword(password)) {
            binding.passwordContainer.error =
                getString(R.string.warn_validation_invalid_password)
            return
        }

        // Hide keyboard, if showing
        hideKeyboard()

        // Disable input
        setInputEnabled(false)

        // Perform login
        model.login(username, password)
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(AnalyticsService.Screen.LOGIN, classSimpleName())
    }

    private fun onLoginResult(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                setInputEnabled(false)
                binding.loading.invisible()
            }
            Status.ERROR -> {
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.LOGIN.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.METHOD, AnalyticsService.ParamValue.EMAIL.paramValue
                    ),
                    success = 0L
                )
                setInputEnabled(true)
                binding.loading.invisible()
                result.message?.let { showSnackbarMessage(binding.root, it) }
            }
            Status.LOADING -> {
                setInputEnabled(false)
                binding.loading.visible(true)
            }
        }
    }

    private fun onUserResult(result: Resource<User>) {
        when (result.status) {
            Status.SUCCESS -> {
                setInputEnabled(false)
                binding.loading.invisible()
                val user = result.data
                Timber.d("User: $user")

                /*
                * We track successful login here because we chain `login`->`getUser` calls
                * and then we proceed to the next screen. So the final chained call (`getUser`)
                * should track the successful login event.
                 */
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.LOGIN.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.METHOD,
                        AnalyticsService.ParamValue.EMAIL.paramValue
                    ),
                    success = 1L
                )

                widgetHelper.getWidgetIds().onRight {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    val ids = it.map { id ->
                        id.toInt()
                    }
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids.toIntArray())
                    intent.putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
                    intent.putExtra(Contracts.ARG_WIDGET_ON_LOGGED_IN, true)
                    this.sendBroadcast(intent)
                }

                val fromOnboarding = intent.getBooleanExtra(ARG_FROM_ONBOARDING, false)
                if (fromOnboarding) {
                    navigator.showAnalyticsOptIn(this)
                } else {
                    finish()
                }
            }
            Status.ERROR -> {
                analytics.trackEventViewContent(
                    contentName = AnalyticsService.ParamValue.LOGIN.paramValue,
                    Pair(
                        FirebaseAnalytics.Param.METHOD,
                        AnalyticsService.ParamValue.EMAIL.paramValue
                    ),
                    success = 0L
                )
                binding.loading.invisible()
                showSnackbarMessage(binding.root, "${result.message}.")
                setInputEnabled(true)
            }
            Status.LOADING -> {
                binding.loading.visible(true)
                setInputEnabled(false)
            }
        }
    }

    private fun setInputEnabled(enable: Boolean) {
        binding.username.isEnabled = enable
        binding.password.isEnabled = enable
        binding.login.isEnabled = enable
        binding.forgotPassword.isEnabled = enable
    }
}
