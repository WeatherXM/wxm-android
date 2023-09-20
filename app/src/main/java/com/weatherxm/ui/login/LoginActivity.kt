package com.weatherxm.ui.login

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import arrow.core.getOrElse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.data.User
import com.weatherxm.databinding.ActivityLoginBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_USER_MESSAGE
import com.weatherxm.ui.common.toast
import com.weatherxm.util.Analytics
import com.weatherxm.util.Validator
import com.weatherxm.util.WidgetHelper
import com.weatherxm.util.applyInsets
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.onTextChanged
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LoginActivity : AppCompatActivity(), KoinComponent {

    private val navigator: Navigator by inject()
    private val validator: Validator by inject()
    private val widgetHelper: WidgetHelper by inject()
    private val analytics: Analytics by inject()
    private val model: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        intent?.extras?.getString(ARG_USER_MESSAGE)?.let {
            showSnackbarMessage(it)
        }

        // Listen for login state change
        model.onLogin().observe(this) {
            onLoginResult(it)
        }

        // Listen for user's wallet existence
        model.user().observe(this) {
            onUserResult(it)
        }

        model.isLoggedIn().observe(this) { result ->
            if (result.getOrElse { false }) {
                Timber.d("Already Logged In. Finish the activity.")
                toast(R.string.already_logged_in)
                finish()
            }
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

        if (!validator.validateUsername(username)) {
            binding.usernameContainer.error = getString(R.string.warn_validation_invalid_email)
            return
        }

        if (!validator.validatePassword(password)) {
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
        analytics.trackScreen(
            Analytics.Screen.LOGIN,
            LoginActivity::class.simpleName
        )
    }

    private fun onLoginResult(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Login success. Get user to check if he has a wallet")
                setInputEnabled(false)
                binding.loading.visibility = View.INVISIBLE
            }
            Status.ERROR -> {
                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.LOGIN.paramValue,
                    contentId = Analytics.ParamValue.LOGIN_ID.paramValue,
                    Pair(FirebaseAnalytics.Param.METHOD, Analytics.ParamValue.EMAIL.paramValue),
                    success = 0L
                )
                setInputEnabled(true)
                binding.loading.visibility = View.INVISIBLE
                result.message?.let { showSnackbarMessage(it) }
            }
            Status.LOADING -> {
                setInputEnabled(false)
                binding.loading.visibility = View.VISIBLE
            }
        }
    }

    private fun onUserResult(result: Resource<User>) {
        when (result.status) {
            Status.SUCCESS -> {
                setInputEnabled(false)
                binding.loading.visibility = View.INVISIBLE
                val user = result.data
                Timber.d("User: $user")
                if(model.shouldShowAnalyticsOptIn()) {
                    navigator.showHome(this)
                } else {
                    navigator.showAnalyticsOptIn(this)
                }

                /*
                * We track successful login here because we chain `login`->`getUser` calls
                * and then we proceed to the next screen. So the final chained call (`getUser`)
                * should track the successful login event.
                 */
                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.LOGIN.paramValue,
                    contentId = Analytics.ParamValue.LOGIN_ID.paramValue,
                    Pair(FirebaseAnalytics.Param.METHOD, Analytics.ParamValue.EMAIL.paramValue),
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

                finish()
            }
            Status.ERROR -> {
                analytics.trackEventViewContent(
                    contentName = Analytics.ParamValue.LOGIN.paramValue,
                    contentId = Analytics.ParamValue.LOGIN_ID.paramValue,
                    Pair(FirebaseAnalytics.Param.METHOD, Analytics.ParamValue.EMAIL.paramValue),
                    success = 0L
                )
                binding.loading.visibility = View.INVISIBLE
                showSnackbarMessage("${result.message}.")
                setInputEnabled(true)
            }
            Status.LOADING -> {
                binding.loading.visibility = View.VISIBLE
                setInputEnabled(false)
            }
        }
    }

    private fun showSnackbarMessage(message: String) {
        if (snackbar?.isShown == true) {
            snackbar?.dismiss()
        }
        snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar?.show()
    }

    private fun setInputEnabled(enable: Boolean) {
        binding.username.isEnabled = enable
        binding.password.isEnabled = enable
        binding.login.isEnabled = enable
        binding.forgotPassword.isEnabled = enable
    }
}
