package com.weatherxm.ui.login

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivityLoginBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Validator
import com.weatherxm.util.onTextChanged
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LoginActivity : AppCompatActivity(), KoinComponent {

    private val navigator: Navigator by inject()
    private val validator: Validator by inject()
    private val model: LoginViewModel by viewModels()
    private val resourcesHelper: ResourcesHelper by inject()
    private lateinit var binding: ActivityLoginBinding

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.username.onTextChanged {
            binding.usernameContainer.error = null
            binding.login.isEnabled =
                !binding.username.text.isNullOrEmpty() && !binding.password.text.isNullOrEmpty()
        }

        binding.password.onTextChanged {
            binding.passwordContainer.error = null
            binding.login.isEnabled =
                !binding.username.text.isNullOrEmpty() && !binding.password.text.isNullOrEmpty()
        }

        binding.forgotPassword.setOnClickListener {
            navigator.showResetPassword(this)
        }

        binding.signupPrompt.text = HtmlCompat.fromHtml(
            resourcesHelper.getString(R.string.prompt_signup),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.signupPrompt.setOnClickListener {
            navigator.showSignup(this)
        }

        binding.login.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()

            if (validator.validateUsername(username)) {
                binding.usernameContainer.error =
                    resourcesHelper.getString(R.string.invalid_username)
                return@setOnClickListener
            }

            if (validator.validatePassword(password)) {
                binding.passwordContainer.error =
                    resourcesHelper.getString(R.string.invalid_password)
                return@setOnClickListener
            }

            binding.username.isEnabled = false
            binding.password.isEnabled = false
            binding.loading.visibility = View.VISIBLE

            model.login(username, password)
        }

        // Listen for login state change
        model.isLoggedIn().observe(this) { result ->
            onLoginResult(result)
        }
    }

    private fun onLoginResult(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Login success. Starting main app flow.")
                navigator.showHome(this)
                finish()
            }
            Status.ERROR -> {
                binding.username.isEnabled = true
                binding.password.isEnabled = true
                binding.loading.visibility = View.INVISIBLE
                showSnackbarMessage(
                    "${resourcesHelper.getString(R.string.login_failed)} ${result.message}."
                )
            }
            Status.LOADING -> {
                binding.username.isEnabled = false
                binding.password.isEnabled = false
                binding.loading.visibility = View.VISIBLE
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
}
