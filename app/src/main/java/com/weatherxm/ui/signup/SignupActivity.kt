package com.weatherxm.ui.signup

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.databinding.ActivitySignupBinding
import com.weatherxm.ui.Navigator
import com.weatherxm.util.Validator
import com.weatherxm.util.applyInsets
import com.weatherxm.util.hideKeyboard
import com.weatherxm.util.onTextChanged
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class SignupActivity : AppCompatActivity(), KoinComponent {

    private val model: SignupViewModel by viewModels()
    private val navigator: Navigator by inject()
    private val validator: Validator by inject()
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyInsets()

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
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
            if (!validator.validateUsername(username)) {
                binding.usernameContainer.error = getString(R.string.warn_validation_invalid_email)
                return@setOnClickListener
            }

            // Hide keyboard, if showing
            hideKeyboard()

            // Perform signup
            model.signup(username, firstName, lastName)
        }

        binding.contactSupport.setOnClickListener {
            navigator.sendSupportEmail(
                context = this,
                subject = getString(R.string.support_email_subject_no_activation),
                body = getString(
                    R.string.support_email_body_user,
                    binding.username.text.toString().trim()
                )
            )
        }

        // Listen for login state change
        model.isSignedUp().observe(this) { result ->
            onSignupResult(result)
        }
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
