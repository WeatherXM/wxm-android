package com.weatherxm.ui.login

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.data.User
import com.weatherxm.databinding.ActivityLoginBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.Validator
import com.weatherxm.util.applyInsets
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.onTextChanged
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LoginActivity : AppCompatActivity(), KoinComponent {

    private val navigator: Navigator by inject()
    private val validator: Validator by inject()
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

        binding.login.setOnClickListener {
            val username = binding.username.text.toString().trim().lowercase()
            val password = binding.password.text.toString().trim()

            if (!validator.validateUsername(username)) {
                binding.usernameContainer.error = getString(R.string.warn_validation_invalid_email)
                return@setOnClickListener
            }

            if (!validator.validatePassword(password)) {
                binding.passwordContainer.error =
                    getString(R.string.warn_validation_invalid_password)
                return@setOnClickListener
            }

            // Hide keyboard, if showing
            hideKeyboard()

            // Disable input
            setInputEnabled(false)

            // Perform login
            model.login(username, password)
        }

        // Listen for login state change
        model.isLoggedIn().observe(this) {
            onLoginResult(it)
        }

        // Listen for user's wallet existence
        model.user().observe(this) {
            onUserResult(it)
        }
    }

    private fun onLoginResult(result: Resource<Unit>) {
        when (result.status) {
            Status.SUCCESS -> {
                Timber.d("Login success. Get user to check if he has a wallet")
                setInputEnabled(false)
                binding.loading.visibility = View.INVISIBLE
            }
            Status.ERROR -> {
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
                if (user?.hasWallet() == true) {
                    navigator.showHome(this)
                } else {
                    navigator.showConnectWallet(this,true)
                }
                finish()
            }
            Status.ERROR -> {
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
